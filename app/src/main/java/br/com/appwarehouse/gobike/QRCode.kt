package br.com.appwarehouse.gobike

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import android.annotation.SuppressLint
import java.io.IOException
import com.google.android.gms.vision.Detector
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Vibrator

class QRCode : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setLogo(R.drawable.qrcode)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "   QRCode"
        setContentView(R.layout.activity_qrcode)

        val surfaceView = findViewById<SurfaceView>(R.id.camerapreview)
        val barcodeDetector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        val cameraSource = CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(640, 480).build()

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                try {
                    cameraSource.start(surfaceHolder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {

            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                cameraSource.stop()
            }
        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {

            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val qrCodes = detections.detectedItems
                if (qrCodes.size() != 0) readQRCode(qrCodes.valueAt(0).displayValue)
            }
        })
    }

    private fun readQRCode(qrcode: String) {
        val tralha = qrcode.indexOf('#')
        if (tralha > 0) {
            val goBike = qrcode.substring(0, tralha)
            if (goBike == "goBike") {
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(1000)
                val bikeId = qrcode.substring(tralha + 1, qrcode.length)
                val intent = Intent()
                intent.putExtra("qrcode", bikeId)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

}
