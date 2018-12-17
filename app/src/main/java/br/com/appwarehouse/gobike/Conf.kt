package br.com.appwarehouse.gobike

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.Toast

class Conf : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setLogo(R.drawable.settings3)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "   Conf"
        setContentView(R.layout.activity_conf)

        val etRaio = findViewById<EditText>(R.id.raio)
        etRaio.hint = resources.getString(R.string.hint_raio) + " " + Prefs.raio
    }

    fun doSave(v: View?) {
        val etRaio = findViewById<EditText>(R.id.raio)
        if ((etRaio.text.isNotEmpty()) && (etRaio.text.toString().toInt() > 5)) {
            Prefs.raio = etRaio.text.toString().toInt()
            finish()
        } else Toast.makeText(this, R.string.toastConf_Erro, Toast.LENGTH_LONG).show()
    }
}
