package br.com.appwarehouse.gobike

import com.google.android.gms.common.util.Base64Utils
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

abstract class Criptografia {
    companion object {
        fun crypt(msg: String): String? {
            val PUBLIC_KEY = Base64Utils.decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCpI4xCFgpQy6mgjdFQipG/9/lWhC9Lg7Rr4/Y/3CzS11LX1b2LRTisGobnfIJm2zIz+p64YaBpGS35oSFv/6Rx+5kjqUloX0Sn5cNFsqUGIZWAMw7OQmigsira+4f6EcOgt/JbooM6OixqU+vD+EI9fW5GKpvS1tYwC/mxgVcBIwIDAQAB")
            var crypt: String? = null

            try {
                val kf = KeyFactory.getInstance("RSA")
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")

                val spec = X509EncodedKeySpec(PUBLIC_KEY)
                val publicKey = kf.generatePublic(spec)

                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val bCrypt = cipher.doFinal(msg.toByteArray(Charsets.UTF_8))
                crypt = Base64Utils.encode(bCrypt).replace("\n", "")

            } catch (e: InvalidKeySpecException) {
                e.printStackTrace()
            } catch (e: BadPaddingException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: IllegalBlockSizeException) {
                e.printStackTrace()
            } catch (e: NoSuchPaddingException) {
                e.printStackTrace()
            } catch (e: InvalidKeyException) {
                e.printStackTrace()
            }

            return crypt
        }
    }
}
