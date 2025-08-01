package com.example.fittrack.View.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import com.example.fittrack.View.ui.activities.ContentActivity
import com.example.fittrack.view.ui.components.HeaderCardView
import com.example.fittrack.ViewModel.HidratacionViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.fittrack.View.ui.adapters.HydrationRemindersAdapter

class HidratacionFragment : Fragment() {

    private lateinit var viewModel: HidratacionViewModel

    // Views principales
    private var tvCurrentHydration: TextView? = null
    private var tvDailyGoal: TextView? = null
    private var ivWaterGlass: ImageView? = null
    private var rvHydrationReminders: RecyclerView? = null
    private var tvTipDescription: TextView? = null
    private var ivBackArrow: ImageView? = null
    private var tvCardTitle: TextView? = null

    // Adapter para RecyclerView
    private var hydrationAdapter: HydrationRemindersAdapter? = null

    companion object {
        private const val TAG = "HidratacionFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d(TAG, "=== CREANDO HidratacionFragment ===")
            inflater.inflate(R.layout.hidratacion, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error al inflar layout: ${e.message}", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d(TAG, "=== CONFIGURANDO VISTAS ===")

            // ✅ OCULTAR ELEMENTOS DE NAVEGACIÓN DEL CONTENTACTIVITY
            hideContentActivityNavigation()

            initializeViewModel()
            initializeViews(view)
            setupHeader()
            setupRecyclerView()
            setupObservers()
            loadData()

            Log.d(TAG, "=== HidratacionFragment CONFIGURADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onViewCreated: ${e.message}", e)
        }
    }

    private fun initializeViewModel() {
        try {
            viewModel = ViewModelProvider(this)[HidratacionViewModel::class.java]
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar ViewModel: ${e.message}", e)
        }
    }

    private fun initializeViews(view: View) {
        try {
            // Buscar vistas en el layout
            tvCurrentHydration = view.findViewById(R.id.tv_current_hydration)
            tvDailyGoal = view.findViewById(R.id.tv_daily_goal)
            ivWaterGlass = view.findViewById(R.id.iv_water_glass)
            rvHydrationReminders = view.findViewById(R.id.rv_hydration_reminders)
            tvTipDescription = view.findViewById(R.id.tv_tip_description)

            // Buscar vistas del header incluido
            ivBackArrow = view.findViewById(R.id.ivBackArrow)
            tvCardTitle = view.findViewById(R.id.tvCardTitle)

            Log.d(TAG, "Vistas inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
        }
    }

    private fun setupHeader() {
        try {
            Log.d(TAG, "BackArrow encontrado: ${ivBackArrow != null}")
            Log.d(TAG, "TitleView encontrado: ${tvCardTitle != null}")

            tvCardTitle?.text = "Hidratación"

            ivBackArrow?.setOnClickListener {
                Log.d(TAG, "Click en flecha detectado!")
                try {
                    findNavController().popBackStack()
                    Log.d(TAG, "PopBackStack ejecutado")
                } catch (e: Exception) {
                    Log.e(TAG, "Error en popBackStack: ${e.message}", e)
                }
            }

            Log.d(TAG, "Header configurado directamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar header: ${e.message}", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            rvHydrationReminders?.layoutManager = LinearLayoutManager(context)

            // Crear adapter con callback para cuando se complete un recordatorio
            hydrationAdapter = HydrationRemindersAdapter { reminderId ->
                // Callback cuando se completa un recordatorio
                viewModel.completeReminder(reminderId)
            }

            rvHydrationReminders?.adapter = hydrationAdapter
            Log.d(TAG, "RecyclerView y Adapter configurados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar RecyclerView: ${e.message}", e)
        }
    }

    private fun setupObservers() {
        try {
            viewModel.currentHydration.observe(viewLifecycleOwner) { hydration ->
                tvCurrentHydration?.text = hydration
            }

            viewModel.dailyGoal.observe(viewLifecycleOwner) { goal ->
                tvDailyGoal?.text = goal
            }

            viewModel.dailyTip.observe(viewLifecycleOwner) { tip ->
                tvTipDescription?.text = tip
            }

            viewModel.hydrationReminders.observe(viewLifecycleOwner) { reminders ->
                // Actualizar el adapter del RecyclerView
                hydrationAdapter?.updateReminders(reminders)
                Log.d(TAG, "Recordatorios recibidos: ${reminders.size}")
            }

            viewModel.waterGlassLevel.observe(viewLifecycleOwner) { level ->
                // Aquí podrías cambiar la imagen del vaso según el nivel
                Log.d(TAG, "Nivel de agua: $level%")
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                if (!error.isNullOrEmpty()) {
                    Log.e(TAG, "Error del ViewModel: $error")
                    // Aquí puedes mostrar un Toast o Snackbar
                }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                // Aquí podrías mostrar/ocultar un indicador de carga
                Log.d(TAG, "Estado de carga: $isLoading")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar observers: ${e.message}", e)
        }
    }

    private fun loadData() {
        try {
            val userName = arguments?.getString("userName")
            Log.d(TAG, "Cargando datos para usuario: $userName")

            viewModel.loadHydrationData(userName)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
        }
    }

    // ✅ OCULTAR NAVEGACIÓN DEL CONTENTACTIVITY
    private fun hideContentActivityNavigation() {
        try {
            (activity as? ContentActivity)?.let { contentActivity ->

                // Buscar y ocultar el header principal
                val headerLayout = contentActivity.findViewById<LinearLayout>(R.id.headerLayout)
                headerLayout?.visibility = View.GONE

                // Buscar y ocultar el bottom navigation
                val bottomNav = contentActivity.findViewById<BottomNavigationView>(R.id.bottomNavigationMain)
                bottomNav?.visibility = View.GONE

                Log.d(TAG, "Elementos de navegación del ContentActivity ocultados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar navegación: ${e.message}", e)
        }
    }

    // ✅ MOSTRAR NAVEGACIÓN DEL CONTENTACTIVITY
    private fun showContentActivityNavigation() {
        try {
            (activity as? ContentActivity)?.let { contentActivity ->

                // Mostrar el header principal
                val headerLayout = contentActivity.findViewById<LinearLayout>(R.id.headerLayout)
                headerLayout?.visibility = View.VISIBLE

                // Mostrar el bottom navigation
                val bottomNav = contentActivity.findViewById<BottomNavigationView>(R.id.bottomNavigationMain)
                bottomNav?.visibility = View.VISIBLE

                Log.d(TAG, "Elementos de navegación del ContentActivity restaurados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar navegación: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            Log.d(TAG, "HidratacionFragment onDestroyView")

            // ✅ RESTAURAR NAVEGACIÓN AL SALIR DEL FRAGMENT
            showContentActivityNavigation()

            // Limpiar referencias
            tvCurrentHydration = null
            tvDailyGoal = null
            ivWaterGlass = null
            rvHydrationReminders = null
            tvTipDescription = null
            ivBackArrow = null
            tvCardTitle = null
            hydrationAdapter = null

        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroyView: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "HidratacionFragment onResume")

        try {
            // Asegurar que la navegación esté oculta al regresar
            hideContentActivityNavigation()
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }
}