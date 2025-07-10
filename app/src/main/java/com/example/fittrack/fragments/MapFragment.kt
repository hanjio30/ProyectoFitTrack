package com.example.fittrack.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fittrack.R
import com.example.fittrack.databinding.FragmentMapBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    // Variables para el ejercicio
    private var isExerciseActive = false
    private var isPaused = false
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var totalDistance: Float = 0f
    private var lastLocation: Location? = null

    // Lista de puntos para la ruta
    private val routePoints = ArrayList<LatLng>()
    private var routePolyline: Polyline? = null

    // Handler para actualizar el cronómetro
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    // Estados de la UI
    private enum class UIState {
        IDLE, ACTIVE, PAUSED, STOPPED
    }
    private var currentState = UIState.IDLE

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val LOCATION_UPDATE_INTERVAL = 5000L
        private const val LOCATION_UPDATE_FASTEST_INTERVAL = 2000L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupLocationServices()
        setupClickListeners()
        setupTimer()
        updateUIState()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_UPDATE_FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                if (isExerciseActive && !isPaused) {
                    locationResult.lastLocation?.let { location ->
                        updateLocation(location)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Botón iniciar ejercicio
        binding.fabStartExercise.setOnClickListener {
            if (checkLocationPermission()) {
                startExercise()
            }
        }

        // Botón pausar
        binding.btnPause.setOnClickListener {
            if (isPaused) {
                resumeExercise()
            } else {
                pauseExercise()
            }
        }

        // Botón detener
        binding.btnStop.setOnClickListener {
            stopExercise()
        }

        // Opciones del overlay
        binding.tvSaveRoute.setOnClickListener {
            saveRoute()
        }

        binding.tvEditRoute.setOnClickListener {
            editRoute()
        }

        binding.tvCancelRoute.setOnClickListener {
            cancelRoute()
        }

        // Botón volver del programador
        binding.btnBack.setOnClickListener {
            hideScheduleOverlay()
        }

        // Botón de imagen del programador
        binding.ivScheduleImage.setOnClickListener {
            // TODO: Implementar selección de imagen
            Toast.makeText(context, "Seleccionar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                if (isExerciseActive && !isPaused) {
                    updateTimer()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configurar el mapa
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false

        // Verificar permisos y configurar ubicación
        if (checkLocationPermission()) {
            googleMap.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            false
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
                    )
                    lastLocation = it
                }
            }
        }
    }

    private fun startExercise() {
        isExerciseActive = true
        isPaused = false
        startTime = System.currentTimeMillis()
        pausedTime = 0
        totalDistance = 0f
        routePoints.clear()

        currentState = UIState.ACTIVE
        updateUIState()

        // Iniciar actualizaciones de ubicación
        startLocationUpdates()

        // Iniciar cronómetro
        timerHandler.post(timerRunnable)

        Toast.makeText(context, "Ejercicio iniciado", Toast.LENGTH_SHORT).show()
    }

    private fun pauseExercise() {
        isPaused = true
        pausedTime = System.currentTimeMillis()
        currentState = UIState.PAUSED
        updateUIState()

        Toast.makeText(context, "Ejercicio pausado", Toast.LENGTH_SHORT).show()
    }

    private fun resumeExercise() {
        isPaused = false
        // Ajustar el tiempo de inicio considerando la pausa
        val pauseDuration = System.currentTimeMillis() - pausedTime
        startTime += pauseDuration

        currentState = UIState.ACTIVE
        updateUIState()

        // Reiniciar cronómetro
        timerHandler.post(timerRunnable)

        Toast.makeText(context, "Ejercicio reanudado", Toast.LENGTH_SHORT).show()
    }

    private fun stopExercise() {
        isExerciseActive = false
        isPaused = false
        currentState = UIState.STOPPED
        updateUIState()

        // Detener actualizaciones de ubicación
        stopLocationUpdates()

        // Mostrar overlay de opciones
        binding.overlayOptions.visibility = View.VISIBLE

        Toast.makeText(context, "Ejercicio detenido", Toast.LENGTH_SHORT).show()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocation(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)

        // Agregar punto a la ruta
        routePoints.add(currentLatLng)

        // Calcular distancia si no es el primer punto
        lastLocation?.let { last ->
            val distance = last.distanceTo(location)
            totalDistance += distance
            updateDistanceDisplay()
        }

        // Actualizar la línea de la ruta
        updateRoutePolyline()

        // Centrar cámara en la ubicación actual
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))

        lastLocation = location
    }

    private fun updateRoutePolyline() {
        // Remover la línea anterior
        routePolyline?.remove()

        // Crear nueva línea si hay suficientes puntos
        if (routePoints.size >= 2) {
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .color(Color.parseColor("#FF6B35"))
                .width(8f)

            routePolyline = googleMap.addPolyline(polylineOptions)
        }
    }

    private fun updateTimer() {
        val elapsedTime = System.currentTimeMillis() - startTime
        val hours = elapsedTime / 3600000
        val minutes = (elapsedTime % 3600000) / 60000
        val seconds = (elapsedTime % 60000) / 1000

        val timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        binding.tvCurrentDuration.text = timeText
    }

    private fun updateDistanceDisplay() {
        val distanceInKm = totalDistance / 1000f
        binding.tvCurrentDistance.text = String.format("%.2f km", distanceInKm)

        // Calcular velocidad
        val elapsedTimeInHours = (System.currentTimeMillis() - startTime) / 3600000f
        val speed = if (elapsedTimeInHours > 0) distanceInKm / elapsedTimeInHours else 0f
        binding.tvCurrentSpeed.text = String.format("%.1f km/h", speed)
    }

    private fun updateUIState() {
        when (currentState) {
            UIState.IDLE -> {
                binding.fabStartExercise.visibility = View.VISIBLE
                binding.cardRunningInfo.visibility = View.GONE
                binding.buttonContainer.visibility = View.GONE
                binding.overlayOptions.visibility = View.GONE
                binding.overlaySchedule.visibility = View.GONE
                binding.runningInfoOverlay.visibility = View.VISIBLE
            }
            UIState.ACTIVE -> {
                binding.fabStartExercise.visibility = View.GONE
                binding.cardRunningInfo.visibility = View.VISIBLE
                binding.buttonContainer.visibility = View.VISIBLE
                binding.btnPause.text = "PAUSAR"
                binding.overlayOptions.visibility = View.GONE
                binding.overlaySchedule.visibility = View.GONE
                binding.runningInfoOverlay.visibility = View.GONE
            }
            UIState.PAUSED -> {
                binding.btnPause.text = "REANUDAR"
            }
            UIState.STOPPED -> {
                binding.fabStartExercise.visibility = View.VISIBLE
                binding.cardRunningInfo.visibility = View.GONE
                binding.buttonContainer.visibility = View.GONE
                binding.runningInfoOverlay.visibility = View.VISIBLE
            }
        }
    }

    private fun saveRoute() {
        // TODO: Implementar guardado de ruta
        Toast.makeText(context, "Ruta guardada", Toast.LENGTH_SHORT).show()
        resetExercise()
    }

    private fun editRoute() {
        // TODO: Implementar edición de ruta
        Toast.makeText(context, "Editar ruta", Toast.LENGTH_SHORT).show()
        hideOptionsOverlay()
    }

    private fun cancelRoute() {
        Toast.makeText(context, "Ruta cancelada", Toast.LENGTH_SHORT).show()
        resetExercise()
    }

    private fun resetExercise() {
        currentState = UIState.IDLE
        updateUIState()

        // Limpiar la ruta del mapa
        routePolyline?.remove()
        routePoints.clear()
        totalDistance = 0f

        // Resetear valores
        binding.tvCurrentDistance.text = "0.00 km"
        binding.tvCurrentDuration.text = "00:00:00"
        binding.tvCurrentSpeed.text = "0.0 km/h"

        hideOptionsOverlay()
    }

    private fun hideOptionsOverlay() {
        binding.overlayOptions.visibility = View.GONE
    }

    private fun showScheduleOverlay() {
        binding.overlaySchedule.visibility = View.VISIBLE
    }

    private fun hideScheduleOverlay() {
        binding.overlaySchedule.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (::googleMap.isInitialized) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        googleMap.isMyLocationEnabled = true
                        getCurrentLocation()
                    }
                }
            } else {
                Toast.makeText(
                    context,
                    "Se requieren permisos de ubicación para usar esta función",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isExerciseActive) {
            stopLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isExerciseActive && !isPaused) {
            startLocationUpdates()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(timerRunnable)
        }
        if (isExerciseActive) {
            stopLocationUpdates()
        }
        _binding = null
    }
}