package br.com.appwarehouse.gobike

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import khttp.structures.authorization.BasicAuthorization
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Home : AppCompatActivity() {
    private var myLocation: Location? = null
    private var stations = "[]"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setLogo(R.drawable.leaf_logo)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "   goBike"
        setContentView(R.layout.activity_home)

        Prefs.setContext(this) // Inicializa Singleton Prefs
    }

    override fun onStart() {
        reloadMyLocation()

        if (Prefs.id > 0) {
            findViewById<ImageView>(R.id.iv_login).setImageResource(R.drawable.user2)
            if (Prefs.nome.isNotEmpty()) findViewById<TextView>(R.id.tv_login).text = Prefs.nome
            if (Prefs.last4.isNotEmpty()) {
                findViewById<TextView>(R.id.tv_card).text = Prefs.last4
                findViewById<ImageView>(R.id.iv_card).setImageResource(R.drawable.creditcard2)
            }
            if (Prefs.nome.isEmpty() || Prefs.last4.isEmpty()) getMoipInfo()
            setBtnGo()
        }

        super.onStart()
    }

    private fun getMoipInfo() {
        Toast.makeText(this, R.string.toast_Checando, Toast.LENGTH_SHORT).show()
        val USER = resources.getString(R.string.moip_user)
        val PASSWORD = resources.getString(R.string.moip_password)
        val url = "https://sandbox.moip.com.br/v2/customers/${Prefs.moip}"

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.get(url = url,
                                     auth = BasicAuthorization(USER, PASSWORD)).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respMoip = deferred.await()

            if (respMoip.has("fullname")) {
                val nome = respMoip.getString("fullname")
                val space = nome.indexOf(' ')
                Prefs.nome = if (space > 0) nome.substring(0, space) else nome
                findViewById<TextView>(R.id.tv_login).text = Prefs.nome
            }

            if (respMoip.has("fundingInstrument")) {
                val fundingInstrument = respMoip.getJSONObject("fundingInstrument")
                val creditCard = fundingInstrument.getJSONObject("creditCard")

                val crc = creditCard.getString("id")
                val last4 = creditCard.getString("last4")

                val intent = Intent(applicationContext, Card::class.java)
                intent.putExtra("crc", crc)
                intent.putExtra("last4", last4)
                startActivity(intent)
            }
        }
    }

    private fun confGPS() {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(R.string.alertTitle_confGPS)
            .setMessage(R.string.alertMsg_confGPS)
            .setPositiveButton(R.string.alertBtn_Sim) { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                finish()
            }
            .setNegativeButton(R.string.alertBtn_Nao) { dialog, _ ->
                dialog.cancel()
            }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun reloadMyLocation() {
        val locationPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        if (!locationPermission) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 60)

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsProvider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsProvider) confGPS()

        if (locationPermission && gpsProvider) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                myLocation = location
                getStations()
            }
        }
    }

    private fun getStations() {
        val url = "http://gobike2.jelasticlw.com.br/station/near" +
                  "?lat=" + myLocation!!.latitude +
                  "&lon=" + myLocation!!.longitude +
                  "&raio=" + Prefs.raio

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                khttp.get(url = url).text
            }
            val respStations = deferred.await()

            try {
                if (JSONArray(respStations).length() > 0) {
                    findViewById<ImageView>(R.id.iv_point).setImageResource(R.drawable.point2)
                    stations = respStations
                    setBtnGo()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, R.string.toast_ErroServidor, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun goLogin(v: View?) {
        if (Prefs.id > 0) popupLogin()
        else {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    private fun popupLogin() {
        val item0 = resources.getString(R.string.alertItem_Logout)
        val item1 = resources.getString(R.string.alertItem_Excluir_Conta)
        val item2 = resources.getString(R.string.alertItem_Cancelar)
        val items = arrayOf(item0, item1, item2)
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(R.string.toastHome_goLogin_title).setItems(items) { dialog, which ->
            when(which) {
                0 -> logout()
                1 -> deleteUser()
                2 -> dialog.cancel()
            }
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun logout() {
        Prefs.clear()
        findViewById<ImageView>(R.id.iv_login).setImageResource(R.drawable.user1)
        findViewById<TextView>(R.id.tv_login).text = ""
        findViewById<ImageView>(R.id.iv_card).setImageResource(R.drawable.creditcard1)
        findViewById<TextView>(R.id.tv_card).text = ""
        setBtnGo()
    }

    private fun deleteUser() {
        Toast.makeText(this, R.string.toast_Excluindo, Toast.LENGTH_SHORT).show()
        val url = "http://gobike2.jelasticlw.com.br/cliente/user?id=${Prefs.id}&code=${Prefs.code}"

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
                Toast.makeText(applicationContext, R.string.toastHome_TaskUser_contaExcluida, Toast.LENGTH_LONG).show()
                logout()
            } else Toast.makeText(applicationContext, R.string.toastHome_TaskUser_erroConta, Toast.LENGTH_LONG).show()
        }
    }

    fun goCard(v: View?) {
        if (Prefs.id > 0) {
            if (Prefs.card.isEmpty()) {
                val intent = Intent(this, Card::class.java)
                startActivity(intent)
            } else popupCard()
        } else alert(R.string.toastHome_goCard_error)
    }

    private fun popupCard() {
        val item0 = resources.getString(R.string.alertItem_Excluir_Cartao)
        val item1 = resources.getString(R.string.alertItem_Cancelar)
        val items = arrayOf(item0, item1)
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(R.string.toastHome_goCard_title).setItems(items) { dialog, which ->
            when(which) {
                0 -> deleteCard()
                1 -> dialog.cancel()
            }
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun deleteCard() {
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
                findViewById<ImageView>(R.id.iv_card).setImageResource(R.drawable.creditcard1)
                findViewById<TextView>(R.id.tv_card).text = ""
                setBtnGo()
            } else Toast.makeText(applicationContext, R.string.toastHome_TaskUser_erroCartao, Toast.LENGTH_LONG).show()
        }
    }

    private fun alert(msg: Int) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(R.string.alertTitle_Atencao)
            .setMessage(msg)
            .setNeutralButton("Ok") {
                dialog, _ -> dialog.cancel()
            }
        val alert = alertBuilder.create()
        alert.show()
    }

    fun goConf(v: View?) {
        val intent = Intent(this, Conf::class.java)
        startActivity(intent)
    }

    fun goMaps(v: View?) {
        val intent = Intent(this, Maps::class.java)
        intent.putExtra("myLocation", myLocation)
        intent.putExtra("stations", stations)
        startActivity(intent)
    }

    private fun setBtnGo() {
        val btnGo = findViewById<Button>(R.id.go)
        if ((Prefs.id > 0) && Prefs.last4.isNotEmpty() && (JSONArray(stations).length() > 0)) {
            btnGo.isEnabled = true
            btnGo.setBackgroundResource(R.drawable.bg_round_green)
        } else {
            btnGo.isEnabled = false
            btnGo.setBackgroundResource(R.drawable.bg_round_black)
        }
    }

    fun doBtnGo(v: View?) {
        goQRCode()
    }

    private fun goQRCode() {
        val cameraPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        if (!cameraPermission) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 61)
        else {
            val intent = Intent(this, QRCode::class.java)
            startActivityForResult(intent, 62)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 61 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, QRCode::class.java)
            startActivityForResult(intent, 62)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 62 && resultCode == Activity.RESULT_OK) {
            val qrcode = data?.getStringExtra("qrcode")
            Toast.makeText(this, qrcode, Toast.LENGTH_LONG).show()
        }
    }
}
