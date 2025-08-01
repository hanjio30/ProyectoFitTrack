package com.example.fittrack.View.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fittrack.R
import com.example.fittrack.ViewModel.DashboardViewModel

class DashboardFragment : Fragment() {

    // ViewModel
    private lateinit var dashboardViewModel: DashboardViewModel

    // Views
    private var tvGreeting: TextView? = null
    private var tvSubtitle: TextView? = null
    private var tvCaloriesValue: TextView? = null
    private var tvActivityValue: TextView? = null
    private var tvStepsValue: TextView? = null
    private var tvHydrationValue: TextView? = null
    private var tvStreakValue: TextView? = null
    private var tvDistanceValue: TextView? = null
    private var tvGoalValue: TextView? = null

    // Cards clickeables
    private var cardDistance: CardView? = null
    private var cardHidratacion: CardView? = null

    companion object {
        private const val TAG = "DashboardFragment"
        private const val ARG_USER_NAME = "user_name"

        // Factory method para crear el fragment con el nombre del usuario
        fun newInstance(userName: String): DashboardFragment {
            val fragment = DashboardFragment()
            val args = Bundle()
            args.putString(ARG_USER_NAME, userName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d(TAG, "=== CREANDO DashboardFragment ===")

            // Inflar el layout
            val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
            Log.d(TAG, "Layout inflado exitosamente")

            view
        } catch (e: Exception) {
            Log.e(TAG, "Error al inflar layout: ${e.message}", e)

            // Si hay error con el layout personalizado, crear una vista básica
            val fallbackView = TextView(requireContext()).apply {
                text = "Dashboard - Vista de respaldo\n\nEl layout personalizado tiene un problema.\nRevisa los recursos y drawables."
                textSize = 16f
                setPadding(32, 32, 32, 32)
            }
            fallbackView
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d(TAG, "=== CONFIGURANDO VISTAS ===")

            // Solo continuar si es una vista válida (no la de respaldo)
            if (view is TextView) {
                Log.d(TAG, "Usando vista de respaldo")
                return
            }

            initializeViewModel()
            initializeViews(view)
            setupClickListeners()
            setupObservers()
            loadData()

            Log.d(TAG, "=== DashboardFragment CONFIGURADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onViewCreated: ${e.message}", e)
            showError("Error al configurar dashboard")
        }
    }

    private fun initializeViewModel() {
        try {
            Log.d(TAG, "Inicializando ViewModel")
            dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar ViewModel: ${e.message}", e)
        }
    }

    private fun initializeViews(view: View) {
        try {
            Log.d(TAG, "Inicializando vistas...")

            // Buscar vistas de forma segura
            tvGreeting = view.findViewById(R.id.tvGreeting)
            tvSubtitle = view.findViewById(R.id.tvSubtitle)
            tvCaloriesValue = view.findViewById(R.id.tvCaloriesValue)
            tvActivityValue = view.findViewById(R.id.tvActivityValue)
            tvStepsValue = view.findViewById(R.id.tvStepsValue)
            tvHydrationValue = view.findViewById(R.id.tvHydrationValue)
            tvStreakValue = view.findViewById(R.id.tvStreakValue)
            tvDistanceValue = view.findViewById(R.id.tvDistanceValue)
            tvGoalValue = view.findViewById(R.id.tvGoalValue)

            // Inicializar cards clickeables
            cardDistance = view.findViewById(R.id.cardDistance)
            cardHidratacion = view.findViewById(R.id.cardHidratacion)

            Log.d(TAG, "Vistas inicializadas")
            Log.d(TAG, "cardDistance: ${if (cardDistance != null) "✓" else "✗"}")
            Log.d(TAG, "cardHidratacion: ${if (cardHidratacion != null) "✓" else "✗"}")

        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
        }
    }

    // ✅ CONFIGURAR CLICKS CON NAVIGATION COMPONENT
    private fun setupClickListeners() {
        try {
            Log.d(TAG, "Configurando click listeners")

            // Click listener para el card de distancia
            cardDistance?.setOnClickListener {
                Log.d(TAG, "Click en card de distancia - Navegando...")
                navigateToDistanceFragment()
            }

            // Click listener para el card de distancia
            cardHidratacion?.setOnClickListener {
                Log.d(TAG, "Click en card de hidratacion - Navegando...")
                navigateToHidratacionFragment()
            }

            // Puedes agregar más cards aquí
            /*
            cardCalories?.setOnClickListener {
                navigateToCaloriesFragment()
            }
            */

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar click listeners: ${e.message}", e)
        }
    }

    // ✅ NAVEGACIÓN - DISTANCIA RECORRIDA  CON NAVIGATION COMPONENT
    private fun navigateToDistanceFragment() {
        try {
            Log.d(TAG, "Click en card de distancia - Navegando...")

            // Usar Navigation Component para navegar al fragment de distancia
            findNavController().navigate(R.id.action_dashboard_to_distance)

            Log.d(TAG, "Navegación iniciada a DistRecorridaFragment")

        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar: ${e.message}", e)
            showError("Error al abrir estadísticas de distancia")
        }
    }

    // NAVEGACIÓN - HIDRATACIÓN
    private fun navigateToHidratacionFragment() {
        try {
            Log.d(TAG, "Click en card de hidratación - Navegando...")
            findNavController().navigate(R.id.action_dashboard_to_hidratacion)
            Log.d(TAG, "Navegación iniciada a HidratacionFragment")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar a hidratación: ${e.message}", e)
            showError("Error al abrir estadísticas de hidratación")
        }
    }

    // ✅ FUNCIÓN PARA NAVEGAR CON DATOS (ejemplo)
    private fun navigateToDistanceWithData(userName: String) {
        try {
            // Si quieres pasar datos, usa Bundle
            val bundle = Bundle().apply {
                putString("userName", userName)
            }

            findNavController().navigate(R.id.action_dashboard_to_distance, bundle)
            Log.d(TAG, "Navegación con datos iniciada")

        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar con datos: ${e.message}", e)
        }
    }

    private fun setupObservers() {
        try {
            Log.d(TAG, "Configurando observers")

            // Observer para el saludo
            dashboardViewModel.greeting.observe(viewLifecycleOwner) { greeting ->
                tvGreeting?.text = greeting
            }

            // Observer para el subtítulo
            dashboardViewModel.subtitle.observe(viewLifecycleOwner) { subtitle ->
                tvSubtitle?.text = subtitle
            }

            // Observers para las estadísticas
            dashboardViewModel.caloriesValue.observe(viewLifecycleOwner) { value ->
                tvCaloriesValue?.text = value
            }

            dashboardViewModel.activityValue.observe(viewLifecycleOwner) { value ->
                tvActivityValue?.text = value
            }

            dashboardViewModel.stepsValue.observe(viewLifecycleOwner) { value ->
                tvStepsValue?.text = value
            }

            dashboardViewModel.hydrationValue.observe(viewLifecycleOwner) { value ->
                tvHydrationValue?.text = value
            }

            dashboardViewModel.streakValue.observe(viewLifecycleOwner) { value ->
                tvStreakValue?.text = value
            }

            dashboardViewModel.distanceValue.observe(viewLifecycleOwner) { value ->
                tvDistanceValue?.text = value
            }

            dashboardViewModel.goalValue.observe(viewLifecycleOwner) { value ->
                tvGoalValue?.text = value
            }

            // Observer para el estado de carga
            dashboardViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                handleLoadingState(isLoading)
            }

            // Observer para errores
            dashboardViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
                if (!errorMessage.isNullOrEmpty()) {
                    showError(errorMessage)
                    dashboardViewModel.clearError()
                }
            }

            Log.d(TAG, "Observers configurados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar observers: ${e.message}", e)
        }
    }

    private fun handleLoadingState(isLoading: Boolean) {
        try {
            if (isLoading) {
                Log.d(TAG, "Cargando datos...")
                // Aquí puedes mostrar un ProgressBar o deshabilitar cards
                cardDistance?.isEnabled = false
            } else {
                Log.d(TAG, "Carga completada")
                cardDistance?.isEnabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al manejar estado de carga: ${e.message}", e)
        }
    }

    private fun loadData() {
        try {
            // Obtener el nombre del usuario desde los argumentos
            val userName = arguments?.getString(ARG_USER_NAME)
            Log.d(TAG, "Cargando datos con usuario: $userName")

            dashboardViewModel.initializeDashboard(userName)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            showError("Error al cargar datos")
        }
    }

    private fun showError(message: String) {
        try {
            context?.let {
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar mensaje: ${e.message}", e)
        }
    }

    // Método público para actualizar estadísticas
    fun updateStats(stats: DashboardViewModel.UserStats) {
        try {
            if (::dashboardViewModel.isInitialized) {
                dashboardViewModel.updateUserStats(stats)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar estadísticas: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "DashboardFragment onResume")

        try {
            if (::dashboardViewModel.isInitialized) {
                dashboardViewModel.refreshData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "DashboardFragment onPause")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "DashboardFragment onDestroyView")

        // Limpiar referencias para evitar memory leaks
        tvGreeting = null
        tvSubtitle = null
        tvCaloriesValue = null
        tvActivityValue = null
        tvStepsValue = null
        tvHydrationValue = null
        tvStreakValue = null
        tvDistanceValue = null
        tvGoalValue = null
        cardDistance = null
        cardHidratacion = null
    }
}