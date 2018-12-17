package br.com.appwarehouse.gobike

import android.annotation.SuppressLint
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray

class Maps : AppCompatActivity(), OnMapReadyCallback {
    private var myLocation: Location? = null
    private var stations: JSONArray = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        myLocation = intent?.extras?.get("myLocation") as Location?
        stations = JSONArray(intent?.extras?.getString("stations"))

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.clear()

        val builder = LatLngBounds.builder()
        if (myLocation != null) {
            val gps = LatLng(myLocation!!.latitude, myLocation!!.longitude)
            builder.include(gps)
            googleMap?.isMyLocationEnabled = true
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(gps, 17F))
        }

        if (stations.length() > 0) {
            for (i in 0 until stations.length()) {
                val station = stations.getJSONObject(i)
                val nome = station.getString("nome")
                val lat = station.getDouble("lat")
                val lon = station.getDouble("lon")

                val marker = MarkerOptions()
                marker.position(LatLng(lat, lon))
                    .title(nome)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                googleMap?.addMarker(marker)
                builder.include(marker.position)
            }
            val bounds = builder.build()
            val cu = CameraUpdateFactory.newLatLngBounds(bounds, 150)
            GoogleMap.OnMapLoadedCallback { googleMap?.animateCamera(cu) }
        }
    }
}
