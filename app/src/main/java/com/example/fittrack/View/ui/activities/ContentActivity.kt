package com.example.fittrack.View.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
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
import com.example.fittrack.databinding.ActivityContentBinding
import com.google.android.material.navigation.NavigationView

class ContentActivity : AppCompatActivity(),
    OnboardingFragment.OnboardingListener,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityContentBinding
    private lateinit var viewModel: ContentViewModel
    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var sharedPreferences: SharedPreferences

    // Navigation Component
    private var navController: NavController? = null
    private var appBarConfiguration: AppBarConfiguration? = null

    private val indicators = mutableListOf<View>()

    // Views del navigation header
    private var tvUserName: TextView? = null
    private var tvUserEmail: TextView? = null

    companion object {
        private const val TAG = "ContentActivity"
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

            setupObservers()
            setupNavigationDrawer()
            setupBottomNavigation()

            // Inicializar la aplicación
            viewModel.initializeApp(sharedPreferences)

            Log.d(TAG, "=== ContentActivity CONFIGURADA EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            finish()
        }
    }

    // ✅ MÉTODO onResume AGREGADO para refrescar imagen al volver de PerfilFragment
    override fun onResume() {
        super.onResume()
        try {
            Log.d(TAG, "onResume - Refrescando imagen de perfil")
            if (viewModel.showMainContent.value == true) {
                viewModel.refreshProfileImage()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }

    private fun setupNavigation() {
        try {
            Log.d(TAG, "Configurando Navigation Component")

            // Obtener el NavHostFragment
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

            if (navHostFragment != null) {
                navController = navHostFragment.navController

                // Configurar AppBar con Navigation
                appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.dashboardFragment,
                        R.id.estadisticasFragment,
                        R.id.mapFragment
                    ),
                    binding.drawerLayout
                )

                Log.d(TAG, "Navigation Component configurado exitosamente")

                // ✅ OCULTAR EL FRAGMENT CONTAINER MANUAL
                binding.fragmentContainer.visibility = View.GONE

            } else {
                Log.w(TAG, "NavHostFragment no encontrado, usando navegación manual")
                // ✅ MOSTRAR EL FRAGMENT CONTAINER COMO FALLBACK
                binding.fragmentContainer.visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar Navigation Component: ${e.message}", e)
            // En caso de error, usar navegación manual
            binding.fragmentContainer.visibility = View.VISIBLE
        }
    }

    // ✅ OBSERVERS MEJORADOS
    private fun setupObservers() {
        try {
            // Observer para mostrar/ocultar onboarding
            viewModel.showOnboarding.observe(this) { showOnboarding ->
                if (showOnboarding) {
                    binding.frameOnboarding.visibility = View.VISIBLE
                    binding.frameMainContent.visibility = View.GONE
                    setupOnboarding()
                }
            }

            // Observer para mostrar/ocultar contenido principal
            viewModel.showMainContent.observe(this) { showMainContent ->
                if (showMainContent) {
                    binding.frameOnboarding.visibility = View.GONE
                    binding.frameMainContent.visibility = View.VISIBLE
                    setupNavigation() // Configurar Navigation cuando se muestra el contenido principal
                }
            }

            // Observer para el nombre del usuario
            viewModel.currentUserName.observe(this) { userName ->
                tvUserName?.text = userName
            }

            // Observer para el email del usuario
            viewModel.currentUserEmail.observe(this) { userEmail ->
                tvUserEmail?.text = userEmail
            }

            // ✅ OBSERVER MEJORADO: Para la imagen de perfil del usuario
            viewModel.userProfileImage.observe(this) { profileBitmap ->
                Log.d(TAG, "Observer userProfileImage triggered - Bitmap: ${profileBitmap != null}")
                updateUserAvatar(profileBitmap)
            }

            // ✅ NUEVO OBSERVER: Para notificar cuando la imagen se actualiza
            viewModel.profileImageUpdated.observe(this) { updated ->
                if (updated) {
                    Log.d(TAG, "Profile image updated - Refreshing UI")
                    updateUserAvatar(viewModel.userProfileImage.value)
                    viewModel.resetProfileImageUpdated()
                }
            }

            // Observer para navegación de fragmentos
            viewModel.currentFragment.observe(this) { fragmentType ->
                navigateToFragment(fragmentType)
            }

            // Observer para logout
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

    // ✅ MÉTODO MEJORADO: Actualizar avatar del usuario
    private fun updateUserAvatar(profileBitmap: Bitmap?) {
        try {
            Log.d(TAG, "=== updateUserAvatar ===")
            Log.d(TAG, "Bitmap recibido: ${profileBitmap != null}")
            if (profileBitmap != null) {
                Log.d(TAG, "Bitmap dimensiones: ${profileBitmap.width}x${profileBitmap.height}")
            }

            // ✅ MEJORADO: Actualizar todos los ImageView con mejor manejo de errores
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
                        // Fallback: imagen por defecto sin circular
                        try {
                            iv.setImageResource(R.drawable.ic_user_avatar)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error en fallback para $description: ${ex.message}", ex)
                        }
                    }
                } ?: Log.w(TAG, "$description ($viewId) es null")
            }

            // ✅ MEJORADO: Actualizar Navigation Header con mejor manejo
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
            // Último recurso: intentar mostrar imagen por defecto en los ImageView principales
            try {
                binding.ivUserAvatar?.setImageResource(R.drawable.ic_user_avatar)
                binding.ivUserAvatarMain?.setImageResource(R.drawable.ic_user_avatar)
            } catch (ex: Exception) {
                Log.e(TAG, "Error en último recurso: ${ex.message}", ex)
            }
        }
    }

    // ✅ MÉTODO UNIFICADO DE NAVEGACIÓN
    private fun navigateToFragment(fragmentType: ContentViewModel.FragmentType) {
        try {
            // Intentar usar Navigation Component primero
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

            // ✅ FALLBACK: Si Navigation Component no está disponible, usar método manual
            Log.w(TAG, "NavController no disponible, usando navegación manual")
            navigateManually(fragmentType)

        } catch (e: Exception) {
            Log.e(TAG, "Error en navegación: ${e.message}", e)
            // En caso de error, usar navegación manual
            navigateManually(fragmentType)
        }
    }

    // ✅ NAVEGACIÓN MANUAL COMO FALLBACK
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
                    viewModel.logout(sharedPreferences)
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

    // ✅ MÉTODOS MANUALES MANTENIDOS COMO FALLBACK
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
}