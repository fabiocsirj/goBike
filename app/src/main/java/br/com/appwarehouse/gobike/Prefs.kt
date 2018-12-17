package br.com.appwarehouse.gobike

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

object Prefs {
    private var sp: SharedPreferences? = null

    fun setContext(context: Context) {
        sp = context.getSharedPreferences("goBike", MODE_PRIVATE)
    }

    fun clear() {
        sp?.edit()?.clear()?.apply()
    }

    fun removeCard() {
        sp?.edit()?.remove("card")?.apply()
        sp?.edit()?.remove("cvc")?.apply()
        sp?.edit()?.remove("last4")?.apply()
    }

    var raio: Int
        @Synchronized
        get() = sp?.getInt("raio", 1600)?: 1600
        @Synchronized
        set(valor) = sp?.edit()?.putInt("raio", valor)?.apply()?: Unit

    var id: Int
        @Synchronized
        get() = sp?.getInt("id", 0)?: 0
        @Synchronized
        set(valor) = sp?.edit()?.putInt("id", valor)?.apply()?: Unit

    var moip: String
        @Synchronized
        get() = sp?.getString("moip", "")?: ""
        @Synchronized
        set(valor) = sp?.edit()?.putString("moip", valor)?.apply()?: Unit

    var nome: String
        @Synchronized
        get() = sp?.getString("nome", "")?: ""
        @Synchronized
        set(valor) = sp?.edit()?.putString("nome", valor)?.apply()?: Unit

    var code: String
        @Synchronized
        get() = sp?.getString("code", "")?: ""
        @Synchronized
        set(valor) = sp?.edit()?.putString("code", valor)?.apply()?: Unit

    var card: String
        @Synchronized
        get() = sp?.getString("card", "")?: ""
        @Synchronized
        set(valor) = sp?.edit()?.putString("card", valor)?.apply()?: Unit

    var cvc: String
        @Synchronized
        get() = sp?.getString("cvc", "")?: ""
        @Synchronized
        set(valor) = sp?.edit()?.putString("cvc", valor)?.apply()?: Unit

    var last4: String
        @Synchronized
        get() = sp?.getString("last4", "")?: ""
        @Synchronized
        set(valor) = sp?.edit()?.putString("last4", valor)?.apply()?: Unit
}
