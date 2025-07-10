package com.example.fittrack

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.fittrack.adapters.OnboardingAdapter
import com.example.fittrack.databinding.ActivityContentBinding
import com.example.fittrack.fragments.OnboardingFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ContentActivity : AppCompatActivity(), OnboardingFragment.OnboardingListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityContentBinding
    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val indicators = mutableListOf<View>()

    // Firebase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    // Views del navigation header
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView

    companion object {
        private const val PREF_NAME = "FitTrackPrefs"
        private const val KEY_FIRST_TIME = "is_first_time"
    }

    data class UserData(
        val fullName: String = "",
        val email: String = "",
        val uid: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("users")

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        // Verificar si es la primera vez que se abre la app
        val isFirstTime = sharedPreferences.getBoolean(KEY_FIRST_TIME, true)

        if (isFirstTime) {
            setupOnboarding()
        } else {
            showMainContent()
        }
    }

    private fun setupOnboarding() {
        binding.frameOnboarding.visibility = View.VISIBLE
        binding.frameMainContent.visibility = View.GONE

        setupViewPager()
        setupIndicators()

        // Configurar el menú hamburguesa durante onboarding (opcional)
        binding.ivHamburgerMenu.setOnClickListener {
            // Aquí puedes agregar lógica si necesitas el menú durante onboarding
        }
    }

    private fun setupViewPager() {
        onboardingAdapter = OnboardingAdapter(this)
        binding.vpOnboarding.adapter = onboardingAdapter

        binding.vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
            }
        })
    }

    private fun setupIndicators() {
        indicators.clear()
        indicators.add(binding.indicator1)
        indicators.add(binding.indicator2)
        indicators.add(binding.indicator3)
        indicators.add(binding.indicator4)
        indicators.add(binding.indicator5)

        updateIndicators(0)
    }

    private fun updateIndicators(position: Int) {
        indicators.forEachIndexed { index, indicator ->
            if (index == position) {
                indicator.setBackgroundResource(R.drawable.indicator_selected)
            } else {
                indicator.setBackgroundResource(R.drawable.indicator_unselected)
            }
        }
    }

    override fun onOnboardingComplete() {
        // Marcar que ya no es la primera vez
        sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, false).apply()

        // Mostrar contenido principal
        showMainContent()
    }

    private fun showMainContent() {
        binding.frameOnboarding.visibility = View.GONE
        binding.frameMainContent.visibility = View.VISIBLE

        setupNavigationDrawer()
        loadUserData()
    }

    private fun setupNavigationDrawer() {
        // Configurar el NavigationView
        binding.navigationView.setNavigationItemSelectedListener(this)

        // Obtener las vistas del header
        val headerView = binding.navigationView.getHeaderView(0)
        tvUserName = headerView.findViewById(R.id.tvUserName)
        tvUserEmail = headerView.findViewById(R.id.tvUserEmail)

        // Configurar el click del hamburger menu
        binding.ivHamburgerMenuMain.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Configurar el click del avatar (opcional)
        binding.ivUserAvatarMain.setOnClickListener {
            // Aquí puedes agregar lógica para mostrar opciones del usuario
        }
    }

    private fun loadUserData() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Obtener datos del usuario desde Firebase Database
            databaseReference.child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userData = snapshot.getValue(UserData::class.java)
                            userData?.let {
                                // Actualizar las vistas con los datos del usuario
                                tvUserName.text = it.fullName
                                tvUserEmail.text = it.email
                            }
                        } else {
                            // Si no hay datos en la base de datos, usar los datos de Firebase Auth
                            tvUserName.text = currentUser.displayName ?: "Usuario"
                            tvUserEmail.text = currentUser.email ?: "usuario@ejemplo.com"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // En caso de error, usar los datos de Firebase Auth
                        tvUserName.text = currentUser.displayName ?: "Usuario"
                        tvUserEmail.text = currentUser.email ?: "usuario@ejemplo.com"
                    }
                })
        } else {
            // Si no hay usuario logueado, mostrar valores por defecto
            tvUserName.text = "Usuario"
            tvUserEmail.text = "usuario@ejemplo.com"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_perfil -> {
                // Navegar a perfil
                // TODO: Implementar navegación a perfil
                handlePerfilNavigation()
            }
            R.id.nav_historial -> {
                // Navegar a historial
                // TODO: Implementar navegación a historial
                handleHistorialNavigation()
            }
            R.id.nav_salir -> {
                // Cerrar sesión o salir
                handleSalirNavigation()
            }
        }

        // Cerrar el drawer después de seleccionar
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handlePerfilNavigation() {
        // TODO: Implementar navegación a fragment de perfil
        // Por ejemplo: supportFragmentManager.beginTransaction()
        //   .replace(R.id.fragmentContainer, PerfilFragment())
        //   .commit()
    }

    private fun handleHistorialNavigation() {
        // TODO: Implementar navegación a fragment de historial
    }

    private fun handleSalirNavigation() {
        // Cerrar sesión de Firebase
        firebaseAuth.signOut()

        // Resetear SharedPreferences para volver a mostrar onboarding
        sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, true).apply()

        // Reiniciar la actividad o cerrar la app
        finish()
    }

    override fun onBackPressed() {
        if (binding.frameMainContent.visibility == View.VISIBLE) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
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
    }
}