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
import java.util.Timer
import java.util.TimerTask

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

    // Timer para actualización automática
    private var updateTimer: Timer? = null

    companion object {
        private const val TAG = "HidratacionFragment"
        private const val UPDATE_INTERVAL_MS = 60000L // 1 minuto
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

            // ✅ CORRECCIÓN: Pasar los 3 parámetros requeridos
            hydrationAdapter = HydrationRemindersAdapter(
                reminders = emptyList(),
                onReminderCompleted = { reminderId ->
                    Log.d(TAG, "Callback recibido para completar recordatorio: $reminderId")
                    viewModel.completeReminder(reminderId)
                },
                viewModel = viewModel // ← AGREGAR ESTE PARÁMETRO
            )

            rvHydrationReminders?.adapter = hydrationAdapter
            Log.d(TAG, "RecyclerView y Adapter configurados exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar RecyclerView: ${e.message}", e)
        }
    }

    // Método helper para actualizar la imagen del vaso según el nivel
    private fun updateWaterGlassImage(level: Int) {
        try {
            ivWaterGlass?.let { imageView ->
                // Ejemplo de cómo cambiar la imagen según el nivel
                when {
                    level >= 100 -> imageView.setImageResource(R.drawable.ic_water_glass_full)
                    level >= 75 -> imageView.setImageResource(R.drawable.ic_water_glass_three_quarter)
                    level >= 50 -> imageView.setImageResource(R.drawable.ic_water_glass_half)
                    level >= 25 -> imageView.setImageResource(R.drawable.ic_water_glass_quarter)
                    else -> imageView.setImageResource(R.drawable.ic_water_glass)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar imagen del vaso: ${e.message}", e)
        }
    }

    // Método helper para mostrar errores al usuario
    private fun showErrorToUser(error: String) {
        try {
            // Usando Toast (puedes cambiar por Snackbar si prefieres)
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar mensaje de error: ${e.message}", e)
        }
    }

    // Método helper para mostrar/ocultar indicador de carga
    private fun showLoadingIndicator(isLoading: Boolean) {
        try {
            // Si tienes un ProgressBar, puedes mostrarlo/ocultarlo aquí
            // progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE

            Log.d(TAG, if (isLoading) "Mostrando indicador de carga" else "Ocultando indicador de carga")
        } catch (e: Exception) {
            Log.e(TAG, "Error al manejar indicador de carga: ${e.message}", e)
        }
    }

    private fun setupObservers() {
        try {
            viewModel.currentHydration.observe(viewLifecycleOwner) { hydration ->
                tvCurrentHydration?.text = hydration
                Log.d(TAG, "Hidratación actual actualizada: $hydration")
            }

            viewModel.dailyGoal.observe(viewLifecycleOwner) { goal ->
                tvDailyGoal?.text = goal
                Log.d(TAG, "Meta diaria actualizada: $goal")
            }

            viewModel.dailyTip.observe(viewLifecycleOwner) { tip ->
                tvTipDescription?.text = tip
                Log.d(TAG, "Tip diario actualizado")
            }

            viewModel.hydrationReminders.observe(viewLifecycleOwner) { reminders ->
                Log.d(TAG, "Recordatorios recibidos: ${reminders.size}")

                // Actualizar el adapter del RecyclerView
                hydrationAdapter?.updateReminders(reminders)

                // Log detallado de los recordatorios
                reminders.forEachIndexed { index, reminder ->
                    Log.d(TAG, "Recordatorio $index: (${reminder.hora}) - ${if (reminder.completado) "✓" else "○"}")
                }
            }

            viewModel.waterGlassLevel.observe(viewLifecycleOwner) { level ->
                // Aquí podrías cambiar la imagen del vaso según el nivel
                Log.d(TAG, "Nivel de agua: $level%")

                // Si tienes diferentes imágenes para diferentes niveles, puedes cambiarlas aquí
                updateWaterGlassImage(level)
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                if (!error.isNullOrEmpty()) {
                    Log.e(TAG, "Error del ViewModel: $error")

                    // Mostrar error al usuario (puedes usar Toast, Snackbar, etc.)
                    showErrorToUser(error)

                    // Limpiar el error después de mostrarlo
                    viewModel.clearError()
                }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                Log.d(TAG, "Estado de carga: $isLoading")

                // Aquí puedes mostrar/ocultar un indicador de carga
                showLoadingIndicator(isLoading)
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

    // ✅ TIMER DE ACTUALIZACIÓN AUTOMÁTICA
    private fun startUpdateTimer() {
        try {
            stopUpdateTimer() // Detener timer anterior si existe

            updateTimer = Timer()
            updateTimer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    // Verificar que el fragment aún esté activo
                    if (isAdded && !isDetached && view != null) {
                        // Ejecutar en el hilo principal
                        requireActivity().runOnUiThread {
                            try {
                                Log.d(TAG, "Actualizando recordatorios automáticamente...")

                                // Refrescar el adapter para actualizar los estados de horario
                                val currentReminders = viewModel.hydrationReminders.value
                                if (currentReminders != null) {
                                    hydrationAdapter?.updateReminders(currentReminders)
                                    Log.d(TAG, "Recordatorios actualizados: ${currentReminders.size}")
                                }

                                // También podrías actualizar otros datos si es necesario
                                // viewModel.refreshData()

                            } catch (e: Exception) {
                                Log.e(TAG, "Error en actualización automática: ${e.message}", e)
                            }
                        }
                    } else {
                        Log.d(TAG, "Fragment no activo, deteniendo timer")
                        stopUpdateTimer()
                    }
                }
            }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS)

            Log.d(TAG, "Timer de actualización iniciado (cada ${UPDATE_INTERVAL_MS / 1000} segundos)")

        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar timer de actualización: ${e.message}", e)
        }
    }

    private fun stopUpdateTimer() {
        try {
            updateTimer?.cancel()
            updateTimer = null
            Log.d(TAG, "Timer de actualización detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener timer: ${e.message}", e)
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

    // ✅ LIFECYCLE METHODS CON TIMER
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "HidratacionFragment onResume")

        try {
            // Asegurar que la navegación esté oculta al regresar
            hideContentActivityNavigation()

            // Iniciar timer de actualización automática
            startUpdateTimer()

        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "HidratacionFragment onPause")

        try {
            // Detener timer de actualización automática
            stopUpdateTimer()

        } catch (e: Exception) {
            Log.e(TAG, "Error en onPause: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            Log.d(TAG, "HidratacionFragment onDestroyView")

            // ✅ DETENER TIMER ANTES DE DESTRUIR LA VISTA
            stopUpdateTimer()

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
}