package br.com.appwarehouse.gobike

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.gms.location.LocationServices
import khttp.structures.authorization.BasicAuthorization
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat

class Home : AppCompatActivity() {
    private var myLocation: Location? = null
    private var stations = "[]"
    private var status = Status.NO_RENT
    private var idRent = 0

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
        getStatus()

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
        Log.i("goBike", "Send Server: $url")

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                khttp.get(url = url).text
            }
            val respStations = deferred.await()
            Log.i("goBike", "Receive Server: $respStations")

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
        Log.i("goBike", "Send Server: $url")

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
            Log.i("goBike", "Receive Server: $respDel")

            if (respDel.has("ok") && respDel.getBoolean("ok")) {
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
        Log.i("goBike", "Send Server: $url")

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
            Log.i("goBike", "Receive Server: $respDel")

            if (respDel.has("ok") && respDel.getBoolean("ok")) {
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
        val stationsNear = (JSONArray(stations).length() > 0)
        val togetherStation = if (stationsNear) isTogetherStation() else false
        val btnGo = findViewById<Button>(R.id.go)
        btnGo.isEnabled = false

        if (status == Status.PAID) { // Está com bike alugada
            btnGo.text = resources.getText(R.string.btnDevolver)
            if (togetherStation) btnGo.setBackgroundResource(R.drawable.bg_round_red)
            else btnGo.setBackgroundResource(R.drawable.bg_round_grey)
            btnGo.isEnabled = true
        } else {
            if (status == Status.CREATED || status == Status.WAITING) { // Está tentando alugar
                btnGo.text = resources.getText(R.string.btnCancel)
                btnGo.setBackgroundResource(R.drawable.bg_round_red)
                btnGo.isEnabled = true
            } else { // Está NO_RENT
                btnGo.text = "GO"
                if ((Prefs.id > 0) && Prefs.last4.isNotEmpty() && stationsNear) {
                    btnGo.setBackgroundResource(R.drawable.bg_round_green)
                    btnGo.isEnabled = true
                } else btnGo.setBackgroundResource(R.drawable.bg_round_black)
            }
        }
    }

    private fun isTogetherStation(): Boolean {
        var ret = false
        for (i in 0 until JSONArray(stations).length()) {
            val station = JSONArray(stations).getJSONObject(i)
            val locStation = Location("")
            locStation.latitude = station.getDouble("lat")
            locStation.longitude = station.getDouble("lon")
            if (myLocation!!.distanceTo(locStation) < 10) {
                ret = true
                break
            }
        }
        return ret
    }

    fun doBtnGo(v: View?) {
        if (status == Status.PAID) stopRent() // Devolvendo bike
        else {
            if (status == Status.CREATED || status == Status.WAITING) { // Está tentando alugar
                Log.i("goBike", "Cancelando id_rent: $idRent")
            } else { // Está NO_RENT
                val cameraPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                if (!cameraPermission) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 61)
                else goQRCode()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 61 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) goQRCode()
    }

    private fun goQRCode() {
        val intent = Intent(this, QRCode::class.java)
        startActivityForResult(intent, 62)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 62 && resultCode == Activity.RESULT_OK) {
            val qrcode = data?.getStringExtra("qrcode")
            Toast.makeText(this, "Bike: $qrcode", Toast.LENGTH_LONG).show()
            rentBike(qrcode!!.toInt())
        }
    }

    private fun rentBike(id_bik: Int) {
        val url = "http://gobike2.jelasticlw.com.br/rent/rent?" +
                  "id_cli=${Prefs.id}&code=${Prefs.code}&id_bik=$id_bik&moip=${Prefs.moip}&crc=${Prefs.card}&cvc=${Prefs.cvc}"
        Log.i("goBike", "Send Server: $url")

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.post(url = url).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respRent = deferred.await()
            Log.i("goBike", "Receive Server: $respRent")

            if (respRent.has("ok") && respRent.getBoolean("ok")) getStatus()
        }
    }

    private fun getStatus() {
        val url = "http://gobike2.jelasticlw.com.br/rent/status?id_cli=${Prefs.id}&code=${Prefs.code}"
        Log.i("goBike", "Send Server: $url")

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.get(url = url).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respStatus = deferred.await()
            Log.i("goBike", "Receive Server: $respStatus")

            if (respStatus.has("ok") && respStatus.getBoolean("ok")) {
                status = Status.values()[respStatus.getInt("status")]
                idRent = respStatus.getInt("id_rent")

                val tv_status = findViewById<TextView>(R.id.tv_status)
                if (status == Status.NO_RENT || status == Status.PAID) tv_status.visibility = View.INVISIBLE
                else tv_status.visibility = View.VISIBLE

                when(status) {
                    Status.NO_RENT -> Log.i("goBike", "Status: NO_RENT")
                    Status.CREATED -> tv_status.text = resources.getText(R.string.status_created)
                    Status.WAITING -> tv_status.text = resources.getText(R.string.status_waiting)
                    Status.PAID -> tictac(respStatus.getString("check_in"))
                    Status.NOT_PAID -> tv_status.text = resources.getText(R.string.status_not_paid)
                    Status.REVERTED -> tv_status.text = resources.getText(R.string.status_not_paid)
                    Status.ERR_STATUS -> Log.i("goBike", "Status: ERR_STATUS")
                    Status.PEND_PAID -> Log.i("goBike", "Status: PEND_PAID")
                }

                if (status == Status.CREATED || status == Status.WAITING) waitAndGetStatus(10)

                setBtnGo()
            }
        }
    }

    private fun waitAndGetStatus(seconds: Long) {
        GlobalScope.launch {
            Log.i("goBike", "Espera $seconds segundos para novo status...")
            delay(seconds * 1000)
            getStatus()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun tictac(check_in: String) {
        val clock = findViewById<Chronometer>(R.id.clock)
        if (clock.visibility == View.INVISIBLE) {
            val format = SimpleDateFormat("yyyy-M-dd HH:mm:ss")
            val check_in_date = format.parse(check_in)
            findViewById<ImageView>(R.id.iv_clock).visibility = View.VISIBLE
            clock.base = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - check_in_date.time)
            clock.start()
            clock.visibility = View.VISIBLE
        }
    }

    private fun stopRent() {
        val url = "http://gobike2.jelasticlw.com.br/rent/stop?" +
                  "id_cli=${Prefs.id}&code=${Prefs.code}&crc=${Prefs.card}&cvc=${Prefs.cvc}&id_rent=$idRent"
        Log.i("goBike", "Send Server: $url")

        GlobalScope.launch(Dispatchers.Main) {
            val deferred = async(Dispatchers.Default) {
                try {
                    khttp.post(url = url).jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                    JSONObject()
                }
            }
            val respStop = deferred.await()
            Log.i("goBike", "Receive Server: $respStop")

            if (respStop.has("ok") && respStop.getBoolean("ok")) {
                hiddenChron()
                getStatus()
            }
        }
    }

    private fun hiddenChron() {
        val clock = findViewById<Chronometer>(R.id.clock)
        if (clock.visibility == View.VISIBLE) {
            findViewById<ImageView>(R.id.iv_clock).visibility = View.INVISIBLE
            clock.stop()
            clock.visibility = View.INVISIBLE
        }
    }

}

private enum class Status {
    NO_RENT,
    CREATED,
    WAITING,
    PAID,
    NOT_PAID,
    REVERTED,
    ERR_STATUS,
    PEND_PAID
}
