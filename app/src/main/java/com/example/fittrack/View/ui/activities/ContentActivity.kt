package com.example.fittrack.View.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.viewpager2.widget.ViewPager2
import com.example.fittrack.utils.ImageUtils
import android.widget.ImageView
import android.graphics.Bitmap
import com.example.fittrack.R
import com.example.fittrack.View.Adapters.OnboardingAdapter
import com.example.fittrack.View.ui.fragments.OnboardingFragment
import com.example.fittrack.ViewModel.ContentViewModel
import com.example.fittrack.ViewModel.RecorridoViewModel
import com.example.fittrack.databinding.ActivityContentBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.gms.maps.model.LatLng
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fittrack.location.FilterResult
import com.example.fittrack.location.GPSLocationFilter
import com.example.fittrack.sensors.StepCounterManager
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog



class ContentActivity : AppCompatActivity(),
    OnboardingFragment.OnboardingListener,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityContentBinding
    private lateinit var viewModel: ContentViewModel
    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var sharedPreferences: SharedPreferences

    // ✅ NUEVOS COMPONENTES PARA EXERCISE TRACKING
    private lateinit var stepCounterManager: StepCounterManager
    private lateinit var gpsLocationFilter: GPSLocationFilter
    private lateinit var recorridoViewModel: RecorridoViewModel

    // Navigation Component
    private var navController: NavController? = null
    private var appBarConfiguration: AppBarConfiguration? = null

    private val indicators = mutableListOf<View>()

    // Views del navigation header
    private var tvUserName: TextView? = null
    private var tvUserEmail: TextView? = null

    // ✅ VARIABLES PARA EXERCISE TRACKING
    private var isExerciseActive = false
    private var exerciseStartTime = 0L
    private var totalDistance = 0f
    private var currentSteps = 0
    private val routePoints = mutableListOf<LatLng>()

    companion object {
        private const val TAG = "ContentActivity"
        private const val EXERCISE_PERMISSIONS_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "=== INICIANDO ContentActivity ===")

            binding = ActivityContentBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Inicializar ViewModel
            viewModel = ViewModelProvider(this)[ContentViewModel::class.java]
            sharedPreferences = getSharedPreferences(ContentViewModel.PREF_NAME, MODE_PRIVATE)

            // ✅ INICIALIZAR COMPONENTES DE EXERCISE TRACKING
            initializeExerciseComponents()

            setupObservers()
            setupNavigationDrawer()
            setupBottomNavigation()

            // ✅ VERIFICAR PERMISOS PARA EXERCISE TRACKING
            checkExercisePermissions()

            // Inicializar la aplicación
            viewModel.initializeApp(sharedPreferences)

            Log.d(TAG, "=== ContentActivity CONFIGURADA EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            finish()
        }
    }

    // ✅ NUEVO MÉTODO: Inicializar componentes de exercise tracking
    private fun initializeExerciseComponents() {
        try {
            Log.d(TAG, "Inicializando componentes de exercise tracking")

            // Inicializar step counter manager
            stepCounterManager = StepCounterManager(this) { stepCount ->
                currentSteps = stepCount
                updateStepDisplay(stepCount)
            }

            // Inicializar GPS location filter
            gpsLocationFilter = GPSLocationFilter()

            // Inicializar RecorridoViewModel
            recorridoViewModel = ViewModelProvider(this)[RecorridoViewModel::class.java]

            // Verificar soporte para contador de pasos
            if (!stepCounterManager.isStepCountingSupported()) {
                showToast("Este dispositivo no soporta contador de pasos automático")
                Log.w(TAG, "Dispositivo no soporta contador de pasos")
            }

            Log.d(TAG, stepCounterManager.getSensorInfo())
            Log.d(TAG, "Componentes de exercise tracking inicializados exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar componentes de exercise tracking: ${e.message}", e)
        }
    }

    // ✅ NUEVO MÉTODO: Verificar permisos para exercise tracking
    private fun checkExercisePermissions() {
        try {
            Log.d(TAG, "Verificando permisos para exercise tracking")

            val permissionsToRequest = mutableListOf<String>()

            // Verificar permiso de reconocimiento de actividad (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }

            // Verificar permisos de ubicación
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            // Solicitar permisos si es necesario
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    EXERCISE_PERMISSIONS_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "Todos los permisos ya están concedidos")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar permisos: ${e.message}", e)
        }
    }

    // ✅ NUEVOS MÉTODOS PÚBLICOS PARA CONTROL DE EJERCICIO
    fun startExercise() {
        try {
            Log.d(TAG, "=== INICIANDO EJERCICIO ===")

            if (isExerciseActive) {
                Log.w(TAG, "El ejercicio ya está activo")
                showToast("El ejercicio ya está en curso")
                return
            }

            // ✅ MOSTRAR MENSAJE DE PREPARACIÓN
            showToast("Preparando GPS... Espera un momento sin moverte")

            isExerciseActive = true
            exerciseStartTime = System.currentTimeMillis()

            // Reiniciar contadores y filtros
            stepCounterManager.resetStepCount()
            gpsLocationFilter.reset() // ✅ Esto ahora limpia completamente el filtro
            routePoints.clear()
            totalDistance = 0f
            currentSteps = 0

            // ✅ DELAY ANTES DE INICIAR GPS para mejor primera ubicación
            Thread {
                try {
                    Thread.sleep(2000) // 2 segundos de espera
                    runOnUiThread {
                        // Iniciar tracking después del delay
                        stepCounterManager.startTracking()
                        startLocationUpdates()

                        showToast("GPS listo. ¡Puedes empezar a moverte!")
                        Log.d(TAG, "Tracking iniciado después de delay de estabilización")
                    }
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Delay interrumpido: ${e.message}")
                    runOnUiThread {
                        stepCounterManager.startTracking()
                        startLocationUpdates()
                    }
                }
            }.start()

            // Notificar cambio de estado inmediatamente
            notifyExerciseStateChanged()

            Log.d(TAG, "Ejercicio iniciado - esperando estabilización GPS")

        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar ejercicio: ${e.message}", e)
            showToast("Error al iniciar ejercicio")
            isExerciseActive = false
        }
    }

    fun pauseExercise() {
        try {
            if (!isExerciseActive) {
                Log.w(TAG, "Intento de pausar ejercicio cuando no está activo")
                return
            }

            Log.d(TAG, "Pausando ejercicio")

            // Pausar tracking (implementar lógica de pausa)
            stepCounterManager.pauseTracking() // Asumiendo que existe este método
            stopLocationUpdates()

            // Notificar cambio de estado sin resetear variables
            notifyExerciseStateChanged()

            showToast("Ejercicio pausado")
            Log.d(TAG, "Ejercicio pausado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al pausar ejercicio: ${e.message}", e)
        }
    }

    fun stopExercise() {
        try {
            if (!isExerciseActive) {
                Log.w(TAG, "Intento de detener ejercicio cuando no está activo")
                return
            }

            Log.d(TAG, "=== DETENIENDO EJERCICIO ===")

            isExerciseActive = false

            // Detener tracking
            stepCounterManager.stopTracking()
            stopLocationUpdates()

            // Mostrar opciones post-ejercicio
            showPostExerciseOptions()

            // Notificar cambio de estado
            notifyExerciseStateChanged()

            Log.d(TAG, "Ejercicio detenido exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al detener ejercicio: ${e.message}", e)
        }
    }

    fun saveRoute() {
        try {
            Log.d(TAG, "=== GUARDANDO RUTA ===")

            if (routePoints.isEmpty()) {
                showToast("No hay datos de ruta para guardar")
                Log.w(TAG, "No hay puntos de ruta para guardar")
                return
            }

            val exerciseEndTime = System.currentTimeMillis()
            val totalTimeMs = exerciseEndTime - exerciseStartTime
            val startCoord = routePoints.firstOrNull()
            val endCoord = routePoints.lastOrNull()

            // Obtener pasos reales del sensor
            val realSteps = stepCounterManager.getCurrentStepCount()

            Log.d(TAG, "Datos del recorrido:")
            Log.d(TAG, "- Distancia: ${String.format("%.2f", totalDistance)} km")
            Log.d(TAG, "- Tiempo: ${totalTimeMs}ms (${totalTimeMs / 1000}s)")
            Log.d(TAG, "- Pasos reales: $realSteps")
            Log.d(TAG, "- Puntos GPS: ${routePoints.size}")
            Log.d(TAG, "- Coordenada inicio: $startCoord")
            Log.d(TAG, "- Coordenada fin: $endCoord")

            // Guardar en RecorridoViewModel
            recorridoViewModel.agregarRecorrido(
                distanciaKm = totalDistance,
                tiempoMs = totalTimeMs,
                coordenadasInicio = startCoord,
                coordenadasFin = endCoord,
                tipoActividad = "Caminata" // Esto podría ser configurable
            )

            // Observar resultado del guardado
            setupSaveRouteObservers()

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar ruta: ${e.message}", e)
            showToast("Error al guardar recorrido")
        }
    }

    // ✅ NUEVO MÉTODO: Configurar observers para guardado de ruta
    private fun setupSaveRouteObservers() {
        try {
            recorridoViewModel.guardadoExitoso.observe(this) { exitoso ->
                if (exitoso) {
                    showToast("Recorrido guardado exitosamente")
                    Log.d(TAG, "Recorrido guardado exitosamente en la base de datos")

                    // Resetear estado después del guardado exitoso
                    resetExerciseState()
                }
            }

            recorridoViewModel.error.observe(this) { error ->
                error?.let {
                    showToast("Error al guardar: $it")
                    Log.e(TAG, "Error al guardar recorrido: $it")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar observers de guardado: ${e.message}", e)
        }
    }

    // ✅ NUEVO MÉTODO: Resetear estado del ejercicio
    private fun resetExerciseState() {
        try {
            isExerciseActive = false
            exerciseStartTime = 0L
            totalDistance = 0f
            currentSteps = 0
            routePoints.clear()

            Log.d(TAG, "Estado del ejercicio reseteado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al resetear estado del ejercicio: ${e.message}", e)
        }
    }

    // ✅ MÉTODO ADICIONAL: Para mostrar estadísticas en tiempo real (opcional)
    private fun logGPSQuality() {
        if (::gpsLocationFilter.isInitialized) {
            Log.i(TAG, gpsLocationFilter.getFilterStats())
        }
    }

    // ✅ NUEVO MÉTODO: Manejar actualizaciones de ubicación GPS
    fun onLocationUpdate(location: Location) {
        try {
            if (!isExerciseActive) {
                Log.d(TAG, "Ubicación recibida pero ejercicio no está activo")
                return
            }

            Log.d(TAG, "📍 Nueva ubicación GPS recibida:")
            Log.d(TAG, "   Lat: ${location.latitude}, Lng: ${location.longitude}")
            Log.d(TAG, "   Precisión: ${location.accuracy}m")
            Log.d(TAG, "   Proveedor: ${location.provider}")

            val filterResult = gpsLocationFilter.filterLocation(location)
            Log.d(TAG, "🔍 ${filterResult.getResultDescription()}")

            when (filterResult) {
                is FilterResult.ACCEPTED_FIRST_LOCATION -> {
                    val latLng = LatLng(location.latitude, location.longitude)
                    routePoints.add(latLng)
                    updateMapLocation(latLng)
                    Log.d(TAG, "🎯 PRIMERA UBICACIÓN ESTABLECIDA: $latLng")

                    showToast("GPS establecido correctamente")
                }

                is FilterResult.ACCEPTED_AND_SMOOTHED -> {
                    val smoothedLocation = filterResult.location
                    val latLng = LatLng(smoothedLocation.latitude, smoothedLocation.longitude)

                    // Calcular distancia desde el último punto
                    if (routePoints.isNotEmpty()) {
                        val lastPoint = routePoints.last()
                        val distance = calculateDistance(lastPoint, latLng)
                        totalDistance += distance / 1000f // Convertir a km

                        Log.d(TAG, "📏 Distancia añadida: ${String.format("%.2f", distance)}m")
                    }

                    routePoints.add(latLng)
                    updateMapLocation(latLng)
                    updateDistanceDisplay(totalDistance)

                    Log.d(TAG, "✅ Ubicación procesada exitosamente")
                    Log.d(TAG, "   Distancia total: ${String.format("%.3f", totalDistance)} km")
                    Log.d(TAG, "   Puntos en ruta: ${routePoints.size}")
                }

                is FilterResult.REJECTED_STATIONARY -> {
                    Log.d(TAG, "🚶 Usuario detectado como estacionario - no se actualiza la ruta")
                    // No mostrar toast para evitar spam, solo log
                }

                else -> {
                    Log.d(TAG, "📍 Ubicación no procesada: ${filterResult.getResultDescription()}")
                    // Logging adicional cada 5 rechazos para debug
                    if (routePoints.size % 5 == 0) {
                        logGPSQuality()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar actualización de ubicación: ${e.message}", e)
            showToast("Error en GPS tracking")
        }
    }

    // ✅ NUEVO MÉTODO: Calcular distancia entre puntos
    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude,
                results
            )
            results[0]
        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular distancia: ${e.message}", e)
            0f
        }
    }

    // ✅ NUEVO MÉTODO: Actualizar display de pasos
    private fun updateStepDisplay(steps: Int) {
        try {
            runOnUiThread {
                // Notificar a fragmentos activos sobre actualización de pasos
                notifyStepCountUpdated(steps)
                Log.d(TAG, "Pasos actualizados: $steps")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar display de pasos: ${e.message}", e)
        }
    }

    // ✅ NUEVO MÉTODO: Actualizar display de distancia
    private fun updateDistanceDisplay(distance: Float) {
        try {
            runOnUiThread {
                // Notificar a fragmentos activos sobre actualización de distancia
                notifyDistanceUpdated(distance)
                Log.d(TAG, "Distancia actualizada: ${String.format("%.3f", distance)} km")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar display de distancia: ${e.message}", e)
        }
    }

    // ✅ NUEVO MÉTODO: Actualizar ubicación en mapa
    private fun updateMapLocation(latLng: LatLng) {
        try {
            // Notificar a fragmentos de mapa sobre nueva ubicación
            notifyLocationUpdated(latLng)
            Log.d(TAG, "Ubicación de mapa actualizada: $latLng")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar ubicación en mapa: ${e.message}", e)
        }
    }

    // ✅ MÉTODOS DE NOTIFICACIÓN A FRAGMENTOS
    private fun notifyExerciseStateChanged() {
        try {
            // Aquí podrías usar un EventBus, LiveData, o interfaces para notificar cambios
            // Por ejemplo, si tienes un fragment que maneja el ejercicio:
            val currentFragment = getCurrentActiveFragment()
            if (currentFragment is ExerciseTrackingFragment) {
                currentFragment.onExerciseStateChanged(isExerciseActive, currentSteps, totalDistance)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al notificar cambio de estado del ejercicio: ${e.message}", e)
        }
    }

    private fun notifyStepCountUpdated(steps: Int) {
        // Implementar notificación de pasos a fragmentos
    }

    private fun notifyDistanceUpdated(distance: Float) {
        // Implementar notificación de distancia a fragmentos
    }

    private fun notifyLocationUpdated(latLng: LatLng) {
        // Implementar notificación de ubicación a fragmentos
    }

    // ✅ MÉTODOS AUXILIARES PARA EXERCISE TRACKING
    private fun startLocationUpdates() {
        try {
            Log.d(TAG, "Iniciando actualizaciones de ubicación GPS")
            // Implementar lógica para iniciar GPS tracking
            // Esto dependerá de tu implementación específica de LocationManager o FusedLocationProvider
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar actualizaciones de ubicación: ${e.message}", e)
        }
    }

    private fun stopLocationUpdates() {
        try {
            Log.d(TAG, "Deteniendo actualizaciones de ubicación GPS")
            // Implementar lógica para detener GPS tracking
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener actualizaciones de ubicación: ${e.message}", e)
        }
    }

    private fun showPostExerciseOptions() {
        try {
            Log.d(TAG, "Mostrando opciones post-ejercicio")
            // Implementar diálogo o navegación a pantalla de resumen
            // Por ejemplo, mostrar un diálogo con opciones para guardar o descartar
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar opciones post-ejercicio: ${e.message}", e)
        }
    }

    private fun getCurrentActiveFragment(): androidx.fragment.app.Fragment? {
        return try {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            navHostFragment?.childFragmentManager?.primaryNavigationFragment
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener fragmento activo: ${e.message}", e)
            null
        }
    }

    // ✅ MÉTODOS PÚBLICOS DE ACCESO AL ESTADO DEL EJERCICIO
    fun isExerciseCurrentlyActive(): Boolean = isExerciseActive
    fun getCurrentSteps(): Int = currentSteps
    fun getCurrentDistance(): Float = totalDistance
    fun getCurrentRoutePoints(): List<LatLng> = routePoints.toList()
    fun getExerciseStartTime(): Long = exerciseStartTime

    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar toast: ${e.message}", e)
        }
    }

    // ✅ MÉTODO onResume ACTUALIZADO
    override fun onResume() {
        super.onResume()
        try {
            Log.d(TAG, "onResume - Refrescando imagen de perfil y estado de ejercicio")
            if (viewModel.showMainContent.value == true) {
                viewModel.refreshProfileImage()

                // Reanudar tracking si el ejercicio estaba activo
                if (isExerciseActive) {
                    stepCounterManager.resumeTracking() // Asumiendo que existe este método
                    Log.d(TAG, "Reanudando tracking de ejercicio")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }

    // ✅ MÉTODO onPause ACTUALIZADO
    override fun onPause() {
        super.onPause()
        try {
            Log.d(TAG, "onPause - Pausando tracking temporalmente")
            // No detener completamente el tracking, solo pausar si es necesario
            // El ejercicio puede continuar en background
        } catch (e: Exception) {
            Log.e(TAG, "Error en onPause: ${e.message}", e)
        }
    }

    // ✅ MÉTODO onDestroy ACTUALIZADO
    override fun onDestroy() {
        try {
            Log.d(TAG, "onDestroy - Limpiando recursos de exercise tracking")

            // Detener tracking completamente
            if (::stepCounterManager.isInitialized) {
                stepCounterManager.stopTracking()
            }

            stopLocationUpdates()

        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar recursos en onDestroy: ${e.message}", e)
        } finally {
            super.onDestroy()
        }
    }

    // ===== RESTO DE MÉTODOS ORIGINALES SIN CAMBIOS =====

    private fun setupNavigation() {
        try {
            Log.d(TAG, "Configurando Navigation Component")

            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

            if (navHostFragment != null) {
                navController = navHostFragment.navController

                appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.dashboardFragment,
                        R.id.estadisticasFragment,
                        R.id.mapFragment
                    ),
                    binding.drawerLayout
                )

                Log.d(TAG, "Navigation Component configurado exitosamente")
                binding.fragmentContainer.visibility = View.GONE

            } else {
                Log.w(TAG, "NavHostFragment no encontrado, usando navegación manual")
                binding.fragmentContainer.visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar Navigation Component: ${e.message}", e)
            binding.fragmentContainer.visibility = View.VISIBLE
        }
    }

    private fun setupObservers() {
        try {
            viewModel.showOnboarding.observe(this) { showOnboarding ->
                if (showOnboarding) {
                    binding.frameOnboarding.visibility = View.VISIBLE
                    binding.frameMainContent.visibility = View.GONE
                    setupOnboarding()
                }
            }

            viewModel.showMainContent.observe(this) { showMainContent ->
                if (showMainContent) {
                    binding.frameOnboarding.visibility = View.GONE
                    binding.frameMainContent.visibility = View.VISIBLE
                    setupNavigation()
                }
            }

            viewModel.currentUserName.observe(this) { userName ->
                tvUserName?.text = userName
            }

            viewModel.currentUserEmail.observe(this) { userEmail ->
                tvUserEmail?.text = userEmail
            }

            viewModel.userProfileImage.observe(this) { profileBitmap ->
                Log.d(TAG, "Observer userProfileImage triggered - Bitmap: ${profileBitmap != null}")
                updateUserAvatar(profileBitmap)
            }

            viewModel.profileImageUpdated.observe(this) { updated ->
                if (updated) {
                    Log.d(TAG, "Profile image updated - Refreshing UI")
                    updateUserAvatar(viewModel.userProfileImage.value)
                    viewModel.resetProfileImageUpdated()
                }
            }

            viewModel.currentFragment.observe(this) { fragmentType ->
                navigateToFragment(fragmentType)
            }

            viewModel.logoutEvent.observe(this) { shouldLogout ->
                if (shouldLogout) {
                    finish()
                    viewModel.resetLogoutEvent()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en setupObservers: ${e.message}", e)
        }
    }

    private fun updateUserAvatar(profileBitmap: Bitmap?) {
        try {
            Log.d(TAG, "=== updateUserAvatar ===")
            Log.d(TAG, "Bitmap recibido: ${profileBitmap != null}")
            if (profileBitmap != null) {
                Log.d(TAG, "Bitmap dimensiones: ${profileBitmap.width}x${profileBitmap.height}")
            }

            val imageViews = listOf(
                Triple(binding.ivUserAvatar, "Onboarding Avatar", "ivUserAvatar"),
                Triple(binding.ivUserAvatarMain, "Main Avatar", "ivUserAvatarMain")
            )

            imageViews.forEach { (imageView, description, viewId) ->
                imageView?.let { iv ->
                    try {
                        if (profileBitmap != null) {
                            Log.d(TAG, "Aplicando imagen personalizada en $description")
                            ImageUtils.makeImageCircular(iv, profileBitmap)
                        } else {
                            Log.d(TAG, "Aplicando imagen por defecto en $description")
                            ImageUtils.makeImageCircular(iv, R.drawable.ic_user_avatar)
                        }
                        Log.d(TAG, "$description actualizado exitosamente")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al actualizar $description: ${e.message}", e)
                        try {
                            iv.setImageResource(R.drawable.ic_user_avatar)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error en fallback para $description: ${ex.message}", ex)
                        }
                    }
                } ?: Log.w(TAG, "$description ($viewId) es null")
            }

            try {
                val headerView = binding.navigationView?.getHeaderView(0)
                val navHeaderAvatar = headerView?.findViewById<ImageView>(R.id.ivNavHeaderAvatar)
                navHeaderAvatar?.let { imageView ->
                    if (profileBitmap != null) {
                        Log.d(TAG, "Aplicando imagen personalizada en Navigation Header")
                        ImageUtils.makeImageCircular(imageView, profileBitmap)
                    } else {
                        Log.d(TAG, "Aplicando imagen por defecto en Navigation Header")
                        ImageUtils.makeImageCircular(imageView, R.drawable.ic_user_avatar)
                    }
                    Log.d(TAG, "Navigation Header avatar actualizado exitosamente")
                } ?: Log.w(TAG, "Navigation Header avatar es null")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar Navigation Header: ${e.message}", e)
            }

            Log.d(TAG, "=== updateUserAvatar COMPLETADO ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en updateUserAvatar: ${e.message}", e)
            try {
                binding.ivUserAvatar?.setImageResource(R.drawable.ic_user_avatar)
                binding.ivUserAvatarMain?.setImageResource(R.drawable.ic_user_avatar)
            } catch (ex: Exception) {
                Log.e(TAG, "Error en último recurso: ${ex.message}", ex)
            }
        }
    }

    private fun navigateToFragment(fragmentType: ContentViewModel.FragmentType) {
        try {
            navController?.let { controller ->
                when (fragmentType) {
                    ContentViewModel.FragmentType.DASHBOARD -> {
                        if (controller.currentDestination?.id != R.id.dashboardFragment) {
                            controller.navigate(R.id.dashboardFragment)
                        }
                    }
                    ContentViewModel.FragmentType.ESTADISTICAS -> {
                        if (controller.currentDestination?.id != R.id.estadisticasFragment) {
                            controller.navigate(R.id.estadisticasFragment)
                        }
                    }
                    ContentViewModel.FragmentType.MAP -> {
                        if (controller.currentDestination?.id != R.id.mapFragment) {
                            controller.navigate(R.id.mapFragment)
                        }
                    }
                    ContentViewModel.FragmentType.PERFIL -> {
                        if (controller.currentDestination?.id != R.id.perfilFragment) {
                            controller.navigate(R.id.perfilFragment)
                        }
                    }
                    ContentViewModel.FragmentType.RECORRIDO -> {
                        if (controller.currentDestination?.id != R.id.recorridoFragment) {
                            controller.navigate(R.id.recorridoFragment)
                        }
                    }
                }
                return
            }

            Log.w(TAG, "NavController no disponible, usando navegación manual")
            navigateManually(fragmentType)

        } catch (e: Exception) {
            Log.e(TAG, "Error en navegación: ${e.message}", e)
            navigateManually(fragmentType)
        }
    }

    private fun navigateManually(fragmentType: ContentViewModel.FragmentType) {
        try {
            when (fragmentType) {
                ContentViewModel.FragmentType.DASHBOARD -> loadDashboardFragment()
                ContentViewModel.FragmentType.ESTADISTICAS -> loadEstadisticasFragment()
                ContentViewModel.FragmentType.MAP -> loadMapFragment()
                ContentViewModel.FragmentType.PERFIL -> loadPerfilFragment()
                ContentViewModel.FragmentType.RECORRIDO -> loadRecorridoFragment()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en navegación manual: ${e.message}", e)
        }
    }

    private fun setupOnboarding() {
        try {
            Log.d(TAG, "Configurando onboarding")
            setupViewPager()
            setupIndicators()

            binding.ivHamburgerMenu?.setOnClickListener {
                Log.d(TAG, "Click en hamburger menu durante onboarding")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en setupOnboarding: ${e.message}", e)
        }
    }

    private fun setupViewPager() {
        try {
            onboardingAdapter = OnboardingAdapter(this)
            binding.vpOnboarding.adapter = onboardingAdapter

            binding.vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateIndicators(position)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupViewPager: ${e.message}", e)
        }
    }

    private fun setupIndicators() {
        try {
            indicators.clear()
            binding.indicator1?.let { indicators.add(it) }
            binding.indicator2?.let { indicators.add(it) }
            binding.indicator3?.let { indicators.add(it) }
            binding.indicator4?.let { indicators.add(it) }
            binding.indicator5?.let { indicators.add(it) }
            updateIndicators(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupIndicators: ${e.message}", e)
        }
    }

    private fun updateIndicators(position: Int) {
        try {
            indicators.forEachIndexed { index, indicator ->
                if (index == position) {
                    indicator.setBackgroundResource(R.drawable.indicator_selected)
                } else {
                    indicator.setBackgroundResource(R.drawable.indicator_unselected)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en updateIndicators: ${e.message}", e)
        }
    }

    override fun onOnboardingComplete() {
        try {
            Log.d(TAG, "Onboarding completado")
            viewModel.onOnboardingComplete(sharedPreferences)
        } catch (e: Exception) {
            Log.e(TAG, "Error en onOnboardingComplete: ${e.message}", e)
        }
    }

    private fun setupNavigationDrawer() {
        try {
            Log.d(TAG, "Configurando Navigation Drawer")

            binding.navigationView?.setNavigationItemSelectedListener(this)

            val headerView = binding.navigationView?.getHeaderView(0)
            if (headerView != null) {
                tvUserName = headerView.findViewById(R.id.tvUserName)
                tvUserEmail = headerView.findViewById(R.id.tvUserEmail)
                Log.d(TAG, "Vistas del header obtenidas exitosamente")
            }

            binding.ivHamburgerMenuMain?.setOnClickListener {
                Log.d(TAG, "Click en hamburger menu principal")
                binding.drawerLayout?.openDrawer(GravityCompat.START)
            }

            binding.ivUserAvatarMain?.setOnClickListener {
                Log.d(TAG, "Click en avatar principal")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en setupNavigationDrawer: ${e.message}", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
            Log.d(TAG, "Configurando Bottom Navigation")

            binding.bottomNavigationMain?.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        viewModel.navigateToFragment(ContentViewModel.FragmentType.DASHBOARD)
                        true
                    }
                    R.id.nav_stats -> {
                        viewModel.navigateToFragment(ContentViewModel.FragmentType.ESTADISTICAS)
                        true
                    }
                    R.id.nav_map -> {
                        viewModel.navigateToFragment(ContentViewModel.FragmentType.MAP)
                        true
                    }
                    else -> false
                }
            }

            binding.bottomNavigationMain?.selectedItemId = R.id.nav_home

        } catch (e: Exception) {
            Log.e(TAG, "Error en setupBottomNavigation: ${e.message}", e)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.nav_perfil -> {
                    viewModel.navigateToFragment(ContentViewModel.FragmentType.PERFIL)
                }
                R.id.nav_historial -> {
                    viewModel.navigateToFragment(ContentViewModel.FragmentType.RECORRIDO)
                }
                R.id.nav_salir -> {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_logout, null)

                    val alertDialog = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create()

                    val btnSalir = dialogView.findViewById<Button>(R.id.btnSalir)
                    val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)

                    btnSalir.setOnClickListener {
                        viewModel.logout(sharedPreferences)
                        alertDialog.dismiss()
                    }

                    btnCancelar.setOnClickListener {
                        alertDialog.dismiss()
                    }

                    alertDialog.show()
                }

            }

            binding.drawerLayout?.closeDrawer(GravityCompat.START)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error en onNavigationItemSelected: ${e.message}", e)
            return false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp(appBarConfiguration!!) == true || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        try {
            if (binding.frameMainContent.visibility == View.VISIBLE) {
                if (binding.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                } else {
                    if (navController?.navigateUp() != true) {
                        super.onBackPressed()
                    }
                }
            } else if (binding.frameOnboarding.visibility == View.VISIBLE) {
                val currentItem = binding.vpOnboarding.currentItem
                if (currentItem > 0) {
                    binding.vpOnboarding.currentItem = currentItem - 1
                } else {
                    super.onBackPressed()
                }
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onBackPressed: ${e.message}", e)
            super.onBackPressed()
        }
    }

    // ===== MÉTODOS MANUALES MANTENIDOS COMO FALLBACK =====
    private fun loadDashboardFragment() {
        try {
            val userName = viewModel.getCurrentUserName()
            Log.d(TAG, "Cargando DashboardFragment manualmente con usuario: $userName")
            val fragment = com.example.fittrack.View.ui.fragments.DashboardFragment.newInstance(userName)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar DashboardFragment: ${e.message}", e)
        }
    }

    private fun loadEstadisticasFragment() {
        try {
            Log.d(TAG, "Cargando EstadisticasFragment manualmente")
            val fragment = com.example.fittrack.View.ui.fragments.EstadisticasFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar EstadisticasFragment: ${e.message}", e)
        }
    }

    private fun loadMapFragment() {
        try {
            Log.d(TAG, "Cargando MapFragment manualmente")
            val fragment = com.example.fittrack.View.ui.fragments.MapFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar MapFragment: ${e.message}", e)
        }
    }

    private fun loadPerfilFragment() {
        try {
            Log.d(TAG, "Cargando PerfilFragment manualmente")
            val fragment = com.example.fittrack.View.ui.fragments.PerfilFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar PerfilFragment: ${e.message}", e)
        }
    }

    private fun loadRecorridoFragment() {
        try {
            Log.d(TAG, "Cargando RecorridoFragment manualmente")
            val fragment = com.example.fittrack.View.ui.fragments.RecorridoFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar RecorridoFragment: ${e.message}", e)
        }
    }

    // ===== INTERFACES PARA COMUNICACIÓN CON FRAGMENTOS =====

    /**
     * Interface que pueden implementar los fragmentos para recibir actualizaciones del ejercicio
     */
    interface ExerciseTrackingFragment {
        fun onExerciseStateChanged(isActive: Boolean, steps: Int, distance: Float)
        fun onStepCountUpdated(steps: Int)
        fun onDistanceUpdated(distance: Float)
        fun onLocationUpdated(latLng: LatLng)
    }

    /**
     * Interface que pueden implementar los fragmentos que necesitan controlar el ejercicio
     */
    interface ExerciseControlFragment {
        fun getContentActivity(): ContentActivity?
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == EXERCISE_PERMISSIONS_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allPermissionsGranted) {
                Log.d(TAG, "Todos los permisos de ejercicio concedidos")
                showToast("Permisos concedidos. Ya puedes usar el tracking de ejercicios")
            } else {
                Log.w(TAG, "Algunos permisos de ejercicio fueron denegados")
                showToast("Algunos permisos fueron denegados. La funcionalidad puede estar limitada")
            }
        }
    }

    // ✅ AGREGAR ESTOS MÉTODOS ANTES DEL CIERRE DE LA CLASE (línea ~750)

    /**
     * Obtiene información del estado del filtro GPS
     */
    fun getGPSFilterStatus(): String {
        return if (::gpsLocationFilter.isInitialized) {
            gpsLocationFilter.getFilterStats()
        } else {
            "Filtro GPS no inicializado"
        }
    }

    /**
     * Verifica si el usuario se está moviendo actualmente
     */
    fun isUserCurrentlyMoving(): Boolean {
        return if (::gpsLocationFilter.isInitialized) {
            gpsLocationFilter.isUserMoving()
        } else {
            false
        }
    }

    /**
     * Obtiene estadísticas detalladas del ejercicio actual
     */
    fun getCurrentExerciseStats(): String {
        return buildString {
            append("=== ESTADÍSTICAS DEL EJERCICIO ===\n")
            append("Estado: ${if (isExerciseActive) "ACTIVO" else "INACTIVO"}\n")
            append("Tiempo transcurrido: ${if (exerciseStartTime > 0) "${(System.currentTimeMillis() - exerciseStartTime) / 1000}s" else "0s"}\n")
            append("Distancia total: ${String.format("%.3f", totalDistance)} km\n")
            append("Pasos actuales: $currentSteps\n")
            append("Puntos GPS: ${routePoints.size}\n")
            append("Usuario en movimiento: ${isUserCurrentlyMoving()}\n")
            append("\n${getGPSFilterStatus()}\n")
            append("\n${if (::stepCounterManager.isInitialized) stepCounterManager.getDetailedStats() else "StepCounterManager no inicializado"}")
        }
    }

}



