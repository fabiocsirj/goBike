package br.com.appwarehouse.gobike

import android.content.Context
import android.text.TextWatcher
import android.text.Editable
import android.widget.EditText
import android.widget.Toast

abstract class Utils {
    companion object {
        fun validaEmail(email: String, context: Context): Boolean {
            val regex = "^[a-z0-9_]([a-z0-9_.-]+)?@[a-z0-9_]+\\.[a-z]{2,3}(\\.[a-z]{2})?".toRegex()
            if (!regex.matches(email)) {
                Toast.makeText(context, R.string.toastValida_ErroEmail, Toast.LENGTH_LONG).show()
                return false
            } else return true
        }

        fun validaCard(card: String, context: Context): Boolean = if (unmask(card).length != 16) {
            Toast.makeText(context, R.string.toastValida_ErroCartao, Toast.LENGTH_LONG).show()
            false
        } else true

        fun validaExpire(expire: String, context: Context): Boolean = if (expire.length != 5) {
            Toast.makeText(context, R.string.toastValida_ErroExpira, Toast.LENGTH_LONG).show()
            false
        } else true

        fun validaCVC(cvc: String, context: Context): Boolean = if (cvc.isEmpty()) {
            Toast.makeText(context, R.string.toastValida_ErroCVC, Toast.LENGTH_LONG).show()
            false
        } else true

        fun validaNome(nome: String, context: Context): Boolean = if (nome.isEmpty()) {
            Toast.makeText(context, R.string.toastValida_ErroNome, Toast.LENGTH_LONG).show()
            false
        } else true

        fun validaCPF(cpf: String, context: Context): Boolean = if (cpf.length != 14 || !cpfValido(unmask(cpf))) {
            Toast.makeText(context, R.string.toastValida_ErroCPF, Toast.LENGTH_LONG).show()
            false
        } else true

        fun validaTel(tel: String, context: Context): Boolean = if (tel.length < 18) {
            Toast.makeText(context, R.string.toastValida_ErroTel, Toast.LENGTH_LONG).show()
            false
        } else true

        fun validaNasc(nasc: String, context: Context): Boolean = if (nasc.length != 10) {
            Toast.makeText(context, R.string.toastValida_ErroData, Toast.LENGTH_LONG).show()
            false
        } else true

        private fun cpfValido(cpf: String): Boolean {
            val d = IntArray(11)
            for (i in 0..10) d[i] = cpf.substring(i, i + 1).toInt()
            var df = digitoVerificador(d, 9)
            if (df != d[9])
                return false
            else {
                df = digitoVerificador(d, 10)
                return df == d[10]
            }
        }

        private fun digitoVerificador(d: IntArray, t: Int): Int {
            var df = 0
            var i = 0
            var v = t + 1
            while (i < t) {
                df += d[i] * v
                i++
                v--
            }
            df %= 11
            df = if (df < 2) 0 else 11 - df
            return df
        }

        const val CPF_MASK = "###.###.###-##"
        const val DATA_MASK = "##/##/####"
        const val EXPIRE_MASK = "##/##"
        const val CARD_MASK = "#### #### #### ####"
        const val TEL_MASK = "(+##) ## #####-####"

        fun unmask(s: String): String {
            return s.replace("[-./()+ ]".toRegex(), "")
        }

        private fun isASign(c: Char): Boolean {
            return c == '.' || c == '-' || c == '/'
        }

        fun maskInsert(mask: String, ediTxt: EditText): TextWatcher {
            return object: TextWatcher {
                var isUpdating: Boolean = false
                var old = ""

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val str = Utils.unmask(s.toString())
                    var mascara = ""
                    if (isUpdating) {
                        old = str
                        isUpdating = false
                        return
                    }

                    var index = 0
                    for (i in 0 until mask.length) {
                        val m = mask[i]
                        if (m != '#') {
                            if (index == str.length && str.length < old.length) continue
                            mascara += m
                            continue
                        }

                        if (index < str.length) mascara += str[index]
                        else break

                        index++
                    }

                    if (mascara.isNotEmpty()) {
                        var lastChar = mascara[mascara.length - 1]
                        var hadSign = false
                        while (isASign(lastChar) && str.length == old.length) {
                            mascara = mascara.substring(0, mascara.length - 1)
                            lastChar = mascara[mascara.length - 1]
                            hadSign = true
                        }

                        if (mascara.isNotEmpty() && hadSign) mascara = mascara.substring(0, mascara.length - 1)
                    }

                    isUpdating = true
                    ediTxt.setText(mascara)
                    ediTxt.setSelection(mascara.length)
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun afterTextChanged(s: Editable) {}
            }
        }
    }
}
