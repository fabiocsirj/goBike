package br.com.appwarehouse.gobike

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import khttp.structures.authorization.BasicAuthorization
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject

class Card : AppCompatActivity() {
    private var newCard: Boolean = true
    private var crc: String = ""
    private var last4: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setLogo(R.drawable.creditcard3)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "   Card"
        setContentView(R.layout.activity_card)

        val numero = findViewById<EditText>(R.id.et_numero)
        numero.addTextChangedListener(Utils.maskInsert(Utils.CARD_MASK, numero))
        val cpf = findViewById<EditText>(R.id.et_cpf)
        cpf.addTextChangedListener(Utils.maskInsert(Utils.CPF_MASK, cpf))
        val expire = findViewById<EditText>(R.id.et_expire)
        expire.addTextChangedListener(Utils.maskInsert(Utils.EXPIRE_MASK, expire))

        if (intent.hasExtra("crc")) {
            newCard = false
            crc = intent.getStringExtra("crc")
            last4 = intent.getStringExtra("last4")

            numero.setText("XXXX XXXX XXXX $last4")
            numero.isEnabled = false
            cpf.visibility = View.INVISIBLE
            expire.visibility = View.INVISIBLE
            findViewById<EditText>(R.id.et_nome).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.tv_nome).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.tv_cpf).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.tv_expire).visibility = View.INVISIBLE

            alert(resources.getString(R.string.alertMsg_EntreCVC) + last4)
        }
    }

    private fun alert(msg: String) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(R.string.alertTitle_Atencao)
            .setMessage(msg)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.cancel()
                findViewById<EditText>(R.id.et_cvc).isFocusable = true
            }
            .setNeutralButton("Delete") { dialog, _ ->
                dialog.cancel()
                deleteCard()
            }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun deleteCard() {
        findViewById<Button>(R.id.btn_saveCard).isEnabled = false
        Toast.makeText(this, R.string.toast_Excluindo, Toast.LENGTH_SHORT).show()
        val url = "http://gobike2.jelasticlw.com.br/cliente/card?id=${Prefs.id}&code=${Prefs.code}"

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.delete(url = url).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respDel = deferred.await()

            if (respDel.getBoolean("ok")) {
                Toast.makeText(applicationContext, R.string.toastHome_TaskUser_cartaoExcluido, Toast.LENGTH_LONG).show()
                Prefs.removeCard()
                finish()
            } else Toast.makeText(applicationContext, R.string.toastHome_TaskUser_erroCartao, Toast.LENGTH_LONG).show()
        }
    }

    fun doSave(v: View?) {
        val cvc = findViewById<EditText>(R.id.et_cvc).text.toString().trim()
        if (newCard) {
            val numero = findViewById<EditText>(R.id.et_numero).text.toString().trim()
            val nome = findViewById<EditText>(R.id.et_nome).text.toString().trim()
            val cpf = findViewById<EditText>(R.id.et_cpf).text.toString().trim()
            val expire = findViewById<EditText>(R.id.et_expire).text.toString().trim()

            if (Utils.validaCard(numero, this) &&
                Utils.validaNome(nome, this) &&
                Utils.validaCPF(cpf, this) &&
                Utils.validaExpire(expire, this) &&
                Utils.validaCVC(cvc, this)
            ) {

                val fundingInstrument = JSONObject()
                val creditCard = JSONObject()
                creditCard.put("expirationMonth", expire.substring(0, 2))
                creditCard.put("expirationYear", expire.substring(3, 5))
                creditCard.put("number", Utils.unmask(numero))
                creditCard.put("cvc", cvc)
                val holder = JSONObject()
                holder.put("fullname", nome)
                val taxDocument = JSONObject()
                taxDocument.put("type", "CPF")
                taxDocument.put("number", Utils.unmask(cpf))
                holder.put("taxDocument", taxDocument)
                creditCard.put("holder", holder)
                fundingInstrument.put("method", "CREDIT_CARD")
                fundingInstrument.put("creditCard", creditCard)

                setMoipCard(fundingInstrument, cvc)
            }
        } else if (Utils.validaCVC(cvc, this)) saveCardPrefs(cvc)
    }

    private fun setMoipCard(fundingInstrument: JSONObject, cvc: String) {
        Toast.makeText(this, R.string.toast_Enviando, Toast.LENGTH_SHORT).show()
        val USER = resources.getString(R.string.moip_user)
        val PASSWORD = resources.getString(R.string.moip_password)
        val url = "https://sandbox.moip.com.br/v2/customers/" + Prefs.moip + "/fundinginstruments"

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.post(url = url,
                                      auth = BasicAuthorization(USER, PASSWORD),
                                      json = fundingInstrument).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respMoip = deferred.await()

            if (respMoip.has("creditCard")) {
                val creditCard = respMoip.getJSONObject("creditCard")
                crc = creditCard.getString("id")
                last4 = creditCard.getString("last4")
                saveCardPrefs(cvc)
            } else Toast.makeText(applicationContext, R.string.toastCard_Erro, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveCardPrefs(cvc: String) {
        Prefs.card = crc
        Prefs.last4 = last4
        Prefs.cvc = Criptografia.crypt(cvc)?: ""
        Toast.makeText(applicationContext, R.string.toastCard_Ok, Toast.LENGTH_LONG).show()
        finish()
    }

}
