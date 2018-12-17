package br.com.appwarehouse.gobike

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder

class Login : AppCompatActivity() {
    private var f: Int = 1
    private var id: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setLogo(R.drawable.user3)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "   Login"
        setContentView(R.layout.activity_login)

        findViewById<EditText>(R.id.nome).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.tel).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.cpf).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.nasc).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.code).visibility = View.INVISIBLE
    }

    fun doFun(v: View?) {
        when(f) {
            1 -> checkEmail()
            2 -> save()
            4 -> checkCode()
        }
    }

    private fun checkEmail() {
        val etEmail = findViewById<EditText>(R.id.email)
        val email = etEmail.text.toString().trim()
        if (Utils.validaEmail(email, this)) {
            etEmail.isEnabled = false
            Toast.makeText(this, R.string.toast_Checando, Toast.LENGTH_SHORT).show()
            val url = "http://gobike2.jelasticlw.com.br/cliente/checkEmail?email=$email"

            GlobalScope.launch(Dispatchers.Main) {
                val deferred = async(Dispatchers.Default) {
                    try {
                        khttp.get(url = url).jsonObject
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        JSONObject()
                    }
                }
                val respEmail = deferred.await()

                if (respEmail.has("ok")) {
                    if (respEmail.getBoolean("ok")) {
                        id = respEmail.getInt("id")
                        askCode()
                    } else showForm()
                } else {
                    Toast.makeText(applicationContext, R.string.toast_ErroServidor, Toast.LENGTH_LONG).show()
                    etEmail.isEnabled = true
                }
            }

        }
    }

    private fun save() {
        val email = findViewById<EditText>(R.id.email).text.toString().trim()
        val nome = findViewById<EditText>(R.id.nome).text.toString().trim()
        val tel = findViewById<EditText>(R.id.tel).text.toString().trim()
        val cpf = findViewById<EditText>(R.id.cpf).text.toString().trim()
        val nasc = findViewById<EditText>(R.id.nasc).text.toString().trim()

        if (Utils.validaNome(nome, this) &&
            Utils.validaTel(tel, this) &&
            Utils.validaCPF(cpf, this) &&
            Utils.validaNasc(nasc, this)) {

            val cpfM = Utils.unmask(cpf)
            val nascM = Utils.unmask(nasc)
            val space1 = tel.indexOf(' ')
            val space2 = tel.indexOf(' ', space1+1)

            val nascF = nascM.substring(4, 8) + "-" + nascM.substring(2, 4) + "-" + nascM.substring(0, 2)
            val ddi = tel.substring(tel.indexOf('+')+1, tel.indexOf(')'))
            val ddd = tel.substring(space1+1, space2)
            val telF  = tel.substring(space2+1, tel.length)

            val jsonNewUser = JSONObject()
            jsonNewUser.put("email", email)
            jsonNewUser.put("nome", nome)
            jsonNewUser.put("cpf", cpfM)
            jsonNewUser.put("nasc", nascF)
            jsonNewUser.put("ddi", ddi)
            jsonNewUser.put("ddd", ddd)
            jsonNewUser.put("tel", telF)

            val msg = "\n" + resources.getString(R.string.hint_email) + ": " + email +
                      "\n" + resources.getString(R.string.hint_nome) + ": " + nome +
                      "\n" + resources.getString(R.string.hint_tel) + ": " + tel +
                      "\n" + resources.getString(R.string.hint_cpf) + ": " + cpf +
                      "\n" + resources.getString(R.string.hint_nasc) + ": " + nasc
            confirm(resources.getString(R.string.alertLogin_Confira), msg, true, jsonNewUser)
        }
    }

    private fun showForm() {
        val etNome = findViewById<EditText>(R.id.nome)
        etNome.visibility = View.VISIBLE
        etNome.requestFocus()
        val tel = findViewById<EditText>(R.id.tel)
        tel.visibility = View.VISIBLE
        tel.addTextChangedListener(Utils.maskInsert(Utils.TEL_MASK, tel))
        val cpf = findViewById<EditText>(R.id.cpf)
        cpf.visibility = View.VISIBLE
        cpf.addTextChangedListener(Utils.maskInsert(Utils.CPF_MASK, cpf))
        val nasc = findViewById<EditText>(R.id.nasc)
        nasc.visibility = View.VISIBLE
        nasc.addTextChangedListener(Utils.maskInsert(Utils.DATA_MASK, nasc))
        findViewById<Button>(R.id.btn_login).setText(R.string.btnCad)

        f = 2
    }

    private fun disableForm() {
        findViewById<EditText>(R.id.nome).isEnabled = false
        findViewById<EditText>(R.id.tel).isEnabled = false
        findViewById<EditText>(R.id.cpf).isEnabled = false
        findViewById<EditText>(R.id.nasc).isEnabled = false
    }

    private fun checkCode() {
        Toast.makeText(this, R.string.toast_Enviando, Toast.LENGTH_SHORT).show()
        val etCode = findViewById<EditText>(R.id.code).text.toString().trim().toUpperCase()
        val code = Criptografia.crypt(etCode)?: ""
        val codeEncoded = URLEncoder.encode(code, "UTF-8")
        val url = "http://gobike2.jelasticlw.com.br/cliente/checkCode?id=$id&code=$codeEncoded"

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.get(url = url).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respCheck = deferred.await()

            if (respCheck.has("ok")) {
                if (respCheck.getBoolean("ok")) {
                    val moip = respCheck.getString("moip")
                    Prefs.id = id
                    Prefs.moip = moip
                    Prefs.code = codeEncoded
                    finish()
                } else confirm(resources.getString(R.string.alertCodeErrado), resources.getString(R.string.alertMsg_NovoCode), false, JSONObject())
            } else Toast.makeText(applicationContext, R.string.toast_ErroServidor, Toast.LENGTH_LONG).show()
        }
    }

    private fun askCode() {
        alert(resources.getString(R.string.alertTitle_Atencao), resources.getString(R.string.alertMsg_EntreCode))
        val etCode = findViewById<EditText>(R.id.code)
        etCode.visibility = View.VISIBLE
        etCode.requestFocus()
        findViewById<Button>(R.id.btn_login).setText(R.string.btnLogin)
        f = 4
    }

    private fun alert(title: String, msg: String) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(title)
            .setMessage(msg)
            .setNeutralButton("Ok") {
                dialog, _ -> dialog.cancel()
            }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun confirm(title: String, msg: String, new: Boolean, json: JSONObject) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(title)
            .setMessage(msg)
            .setNegativeButton(R.string.alertItem_Cancelar) {
                dialog, _ -> dialog.cancel()
            }
            .setPositiveButton("Ok") {
                dialog, _ -> dialog.cancel()
                if (new) postNewUser(json)
                else setCode()
            }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun postNewUser(json: JSONObject) {
        Toast.makeText(this, R.string.toastLogin_Cadastrando, Toast.LENGTH_SHORT).show()
        val url = "http://gobike2.jelasticlw.com.br/cliente/save"

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.post(url = url,
                                      json = json).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respNewUser = deferred.await()

            if (respNewUser.getBoolean("ok")) {
                id = respNewUser.getInt("id")
                disableForm()
                askCode()
            } else Toast.makeText(applicationContext, R.string.toastLogin_ErroCad, Toast.LENGTH_LONG).show()
        }
    }

    private fun setCode() {
        Toast.makeText(this, R.string.toast_Enviando, Toast.LENGTH_SHORT).show()
        val url = "http://gobike2.jelasticlw.com.br/cliente/setCode?id=$id"

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.post(url = url).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respSetCode = deferred.await()

            if (respSetCode.has("ok")) {
                if (respSetCode.getBoolean("ok")) {
                    askCode()
                } else {
                    val err = respSetCode.getInt("err")
                    val msg = resources.getString(R.string.msgLogin1) + " $err " + resources.getString(R.string.msgLogin2) + "\n" +
                            resources.getString(R.string.msgLogin3) + " ${5 - err} " + resources.getString(R.string.msgLogin4) + "\n" +
                            resources.getString(R.string.msgLogin5)
                    alert(resources.getString(R.string.alertAguarde), msg)
                }
            } else Toast.makeText(applicationContext, R.string.toast_ErroServidor, Toast.LENGTH_LONG).show()
        }
    }

}
