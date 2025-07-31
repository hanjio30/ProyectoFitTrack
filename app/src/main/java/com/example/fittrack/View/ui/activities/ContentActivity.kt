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
import androidx.viewpager2.widget.ViewPager2
import com.example.fittrack.R
import com.example.fittrack.View.Adapters.OnboardingAdapter
import com.example.fittrack.View.ui.fragments.*
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

            // Observer para cambios de fragmento
            viewModel.currentFragment.observe(this) { fragmentType ->
                when (fragmentType) {
                    ContentViewModel.FragmentType.DASHBOARD -> loadDashboardFragment()
                    ContentViewModel.FragmentType.ESTADISTICAS -> loadEstadisticasFragment()
                    ContentViewModel.FragmentType.MAP -> loadMapFragment()
                    ContentViewModel.FragmentType.PERFIL -> loadPerfilFragment()
                    ContentViewModel.FragmentType.RECORRIDO -> loadRecorridoFragment()
                }
            }

            // Observer para refrescar dashboard
            viewModel.refreshDashboard.observe(this) { shouldRefresh ->
                if (shouldRefresh) {
                    refreshDashboardIfVisible()
                    viewModel.resetRefreshDashboard()
                }
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

    private fun setupOnboarding() {
        try {
            Log.d(TAG, "Configurando onboarding")

            setupViewPager()
            setupIndicators()

            // Configurar el menú hamburguesa durante onboarding (opcional)
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

            // Configurar el NavigationView
            binding.navigationView?.setNavigationItemSelectedListener(this)

            // Obtener las vistas del header de forma segura
            val headerView = binding.navigationView?.getHeaderView(0)
            if (headerView != null) {
                tvUserName = headerView.findViewById(R.id.tvUserName)
                tvUserEmail = headerView.findViewById(R.id.tvUserEmail)
                Log.d(TAG, "Vistas del header obtenidas exitosamente")
            } else {
                Log.w(TAG, "HeaderView es null")
            }

            // Configurar el click del hamburger menu
            binding.ivHamburgerMenuMain?.setOnClickListener {
                Log.d(TAG, "Click en hamburger menu principal")
                binding.drawerLayout?.openDrawer(GravityCompat.START)
            }

            // Configurar el click del avatar (opcional)
            binding.ivUserAvatarMain?.setOnClickListener {
                Log.d(TAG, "Click en avatar principal")
                // Aquí puedes agregar lógica para mostrar opciones del usuario
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

            // Seleccionar el item de inicio por defecto
            binding.bottomNavigationMain?.selectedItemId = R.id.nav_home

        } catch (e: Exception) {
            Log.e(TAG, "Error en setupBottomNavigation: ${e.message}", e)
        }
    }

    private fun refreshDashboardIfVisible() {
        try {
            // Verificar si el DashboardFragment está actualmente visible
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is DashboardFragment) {
                Log.d(TAG, "Recargando DashboardFragment con nuevo nombre de usuario")
                loadDashboardFragment()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en refreshDashboardIfVisible: ${e.message}", e)
        }
    }

    private fun loadDashboardFragment() {
        try {
            val userName = viewModel.getCurrentUserName()
            Log.d(TAG, "Cargando DashboardFragment con usuario: $userName")
            val fragment = DashboardFragment.newInstance(userName)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
            Log.d(TAG, "DashboardFragment cargado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar DashboardFragment: ${e.message}", e)
        }
    }

    private fun loadEstadisticasFragment() {
        try {
            Log.d(TAG, "Cargando EstadisticasFragment")
            val fragment = EstadisticasFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar EstadisticasFragment: ${e.message}", e)
        }
    }

    private fun loadMapFragment() {
        try {
            Log.d(TAG, "Cargando MapFragment")
            val fragment = MapFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar MapFragment: ${e.message}", e)
        }
    }

    private fun loadPerfilFragment() {
        try {
            Log.d(TAG, "Cargando PerfilFragment")
            val fragment = PerfilFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar PerfilFragment: ${e.message}", e)
        }
    }

    private fun loadRecorridoFragment() {
        try {
            Log.d(TAG, "Cargando RecorridoFragment")
            val fragment = RecorridoFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar RecorridoFragment: ${e.message}", e)
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

    override fun onBackPressed() {
        try {
            if (binding.frameMainContent.visibility == View.VISIBLE) {
                if (binding.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                } else {
                    super.onBackPressed()
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
}