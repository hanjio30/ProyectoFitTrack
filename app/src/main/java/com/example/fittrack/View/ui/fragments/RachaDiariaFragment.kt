package com.example.fittrack.View.ui.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fittrack.R
import com.example.fittrack.View.ui.activities.ContentActivity
import com.example.fittrack.ViewModel.RachaDiariaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class RachaDiariaFragment : Fragment() {

    private lateinit var viewModel: RachaDiariaViewModel
    private lateinit var auth: FirebaseAuth

    // Views principales
    private var tvStreakNumber: TextView? = null
    private var tvStreakLabel: TextView? = null
    private var tvStreakLevel: TextView? = null
    private var tvBestStreakValue: TextView? = null
    private var tvTotalPointsValue: TextView? = null
    private var tvMilestoneTitle: TextView? = null
    private var tvMilestoneProgress: TextView? = null
    private var tvMilestoneDescription: TextView? = null
    private var progressBarMilestone: ProgressBar? = null
    private var tvMotivationalMessage: TextView? = null
    private var tvDailyTip: TextView? = null

    // Header views
    private var ivBackArrow: ImageView? = null
    private var tvCardTitle: TextView? = null
    private var ivCalendar: ImageView? = null
    private var ivGift: ImageView? = null

    // CardViews desplegables
    private var cardCalendarMenu: CardView? = null
    private var cardGiftMenu: CardView? = null
    private var ivCloseCalendar: ImageView? = null
    private var ivCloseGift: ImageView? = null
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    // Contenedores para historial semanal
    private var weeklyHistoryContainer: LinearLayout? = null

    // Estado de visibilidad
    private var isCalendarMenuVisible = false
    private var isGiftMenuVisible = false

    companion object {
        private const val TAG = "RachaDiariaFragment"
        private const val ANIMATION_DURATION = 300L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d(TAG, "=== CREANDO RachaDiariaFragment ===")
            inflater.inflate(R.layout.racha_diaria, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error al inflar layout: ${e.message}", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d(TAG, "=== CONFIGURANDO VISTAS ===")

            // Inicializar Firebase Auth
            auth = FirebaseAuth.getInstance()

            // Ocultar elementos de navegación del ContentActivity
            hideContentActivityNavigation()

            initializeViewModel()
            initializeViews(view)
            setupHeader()
            setupClickListeners()
            setupObservers()
            loadData()

            Log.d(TAG, "=== RachaDiariaFragment CONFIGURADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onViewCreated: ${e.message}", e)
        }
    }

    private fun initializeViewModel() {
        try {
            viewModel = ViewModelProvider(this)[RachaDiariaViewModel::class.java]
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar ViewModel: ${e.message}", e)
        }
    }

    private fun initializeViews(view: View) {
        try {
            // Views principales de racha
            tvStreakNumber = view.findViewById(R.id.streak_number)
            tvStreakLabel = view.findViewById(R.id.streak_label)
            tvStreakLevel = view.findViewById(R.id.streak_level)
            tvBestStreakValue = view.findViewById(R.id.tv_best_streak_value)
            tvTotalPointsValue = view.findViewById(R.id.tv_total_points_value)

            // Views de hito/milestone
            tvMilestoneTitle = view.findViewById(R.id.tv_milestone_title)
            tvMilestoneProgress = view.findViewById(R.id.tv_milestone_progress)
            tvMilestoneDescription = view.findViewById(R.id.tv_milestone_description)
            progressBarMilestone = view.findViewById(R.id.progress_milestone)

            // Views de motivación
            tvMotivationalMessage = view.findViewById(R.id.tv_motivational_message)
            tvDailyTip = view.findViewById(R.id.tv_daily_tip)

            // Header views
            ivBackArrow = view.findViewById(R.id.ivBackArrow)
            tvCardTitle = view.findViewById(R.id.tvCardTitle)
            ivCalendar = view.findViewById(R.id.iv_calendar)
            ivGift = view.findViewById(R.id.iv_gift)

            // CardViews desplegables
            cardCalendarMenu = view.findViewById(R.id.card_calendar_menu)
            cardGiftMenu = view.findViewById(R.id.card_gift_menu)
            ivCloseCalendar = view.findViewById(R.id.iv_close_calendar)
            ivCloseGift = view.findViewById(R.id.iv_close_gift)

            // Contenedor para historial semanal (dentro del card del calendario)
            weeklyHistoryContainer = cardCalendarMenu?.findViewById(R.id.weekly_history_container)

            Log.d(TAG, "Vistas inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
        }
    }

    private fun setupHeader() {
        try {
            tvCardTitle?.text = "Racha Diaria"

            ivBackArrow?.setOnClickListener {
                Log.d(TAG, "Click en flecha detectado!")
                try {
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e(TAG, "Error en popBackStack: ${e.message}", e)
                }
            }

            Log.d(TAG, "Header configurado")

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar header: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        try {
            // Click en ícono de calendario
            ivCalendar?.setOnClickListener {
                Log.d(TAG, "Click en calendario")
                toggleCalendarMenu()
            }

            // Click en ícono de regalo
            ivGift?.setOnClickListener {
                Log.d(TAG, "Click en regalo")
                toggleGiftMenu()
            }

            // Botones de cerrar dentro de los cards
            ivCloseCalendar?.setOnClickListener {
                Log.d(TAG, "Click en cerrar calendario")
                hideCalendarMenu()
            }

            ivCloseGift?.setOnClickListener {
                Log.d(TAG, "Click en cerrar regalo")
                hideGiftMenu()
            }

            // REMOVIDO: Click en la card principal para testing
            // Ya no necesitamos el click manual para cargar datos

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar click listeners: ${e.message}", e)
        }
    }

    private fun setupObservers() {
        try {
            viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
                tvStreakNumber?.text = streak.toString()
                Log.d(TAG, "Racha actual actualizada: $streak")
            }

            viewModel.streakLevel.observe(viewLifecycleOwner) { level ->
                tvStreakLevel?.text = level
                Log.d(TAG, "Nivel actualizado: $level")
            }

            viewModel.bestStreak.observe(viewLifecycleOwner) { bestStreak ->
                tvBestStreakValue?.text = bestStreak.toString()
                Log.d(TAG, "Mejor racha: $bestStreak")
            }

            viewModel.totalPoints.observe(viewLifecycleOwner) { points ->
                tvTotalPointsValue?.text = points.toString()
                Log.d(TAG, "Puntos totales: $points")
            }

            viewModel.nextMilestone.observe(viewLifecycleOwner) { milestone ->
                tvMilestoneTitle?.text = milestone.name
                tvMilestoneProgress?.text = "${milestone.progress}%"
                tvMilestoneDescription?.text = milestone.description
                progressBarMilestone?.progress = milestone.progress
                Log.d(TAG, "Hito actualizado: ${milestone.name} - ${milestone.progress}%")
            }

            viewModel.motivationalMessage.observe(viewLifecycleOwner) { message ->
                tvMotivationalMessage?.text = message
                Log.d(TAG, "Mensaje motivacional actualizado")
            }

            viewModel.dailyTip.observe(viewLifecycleOwner) { tip ->
                tvDailyTip?.text = tip
                Log.d(TAG, "Consejo diario actualizado")
            }

            viewModel.weeklyHistory.observe(viewLifecycleOwner) { history ->
                updateWeeklyHistoryUI(history)
                Log.d(TAG, "Historial semanal actualizado")
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                if (!error.isNullOrEmpty()) {
                    Log.e(TAG, "Error del ViewModel: $error")
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                // Aquí podrías mostrar/ocultar un indicador de carga
                Log.d(TAG, "Estado de carga: $isLoading")
                if (!isLoading) {
                    Log.d(TAG, "Carga completada - datos disponibles en UI")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar observers: ${e.message}", e)
        }
    }

    private fun loadData() {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userId = currentUser.uid
                Log.d(TAG, "=== INICIANDO CARGA DE DATOS ===")
                Log.d(TAG, "Cargando datos para usuario: $userId")

                // Cargar datos inmediatamente al entrar al fragment
                viewModel.loadStreakData(userId)

                Log.d(TAG, "Solicitud de carga enviada al ViewModel")
            } else {
                Log.e(TAG, "Usuario no autenticado")
                _errorMessage.value = "Error: Usuario no autenticado"
                Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            Toast.makeText(context, "Error al cargar datos de racha", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateWeeklyHistoryUI(history: List<RachaDiariaViewModel.DayStatus>) {
        try {
            weeklyHistoryContainer?.let { container ->
                container.removeAllViews()

                // Crear fila de días de la semana
                val daysRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val statusRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, 16, 0, 0)
                }

                history.forEach { dayStatus ->
                    // Texto del día
                    val dayText = TextView(context).apply {
                        text = dayStatus.dayName.take(1).uppercase()
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        gravity = android.view.Gravity.CENTER
                        textSize = 12f
                        setTextColor(resources.getColor(R.color.text_secondary, null))
                    }
                    daysRow.addView(dayText)

                    // Estado visual del día
                    val statusView = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 32.dpToPx(), 1f).apply {
                            setMargins(4.dpToPx(), 0, 4.dpToPx(), 0)
                        }
                        setBackgroundResource(R.drawable.ic_calendar)
                        backgroundTintList = if (dayStatus.completed) {
                            resources.getColorStateList(R.color.calendar_racha, null)
                        } else {
                            resources.getColorStateList(R.color.calendar_no_racha, null)
                        }
                    }
                    statusRow.addView(statusView)
                }

                container.addView(daysRow)
                container.addView(statusRow)
            }

            Log.d(TAG, "Historial semanal actualizado en UI")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar historial semanal: ${e.message}", e)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }


    // Funciones para alternar menús
    private fun toggleCalendarMenu() {
        try {
            if (isCalendarMenuVisible) {
                hideCalendarMenu()
            } else {
                showCalendarMenu()
                // Cerrar el otro menú si está abierto
                if (isGiftMenuVisible) {
                    hideGiftMenu()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al alternar menú de calendario: ${e.message}", e)
        }
    }

    private fun toggleGiftMenu() {
        try {
            if (isGiftMenuVisible) {
                hideGiftMenu()
            } else {
                showGiftMenu()
                // Cerrar el otro menú si está abierto
                if (isCalendarMenuVisible) {
                    hideCalendarMenu()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al alternar menú de regalos: ${e.message}", e)
        }
    }

    private fun showCalendarMenu() {
        try {
            cardCalendarMenu?.let { card ->
                Log.d(TAG, "Mostrando menú del calendario")

                card.visibility = View.VISIBLE
                card.alpha = 0f

                // Animación de fade in con escala
                val fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f)
                val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 0.8f, 1f)
                val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f)

                fadeIn.duration = ANIMATION_DURATION
                scaleX.duration = ANIMATION_DURATION
                scaleY.duration = ANIMATION_DURATION

                fadeIn.start()
                scaleX.start()
                scaleY.start()

                isCalendarMenuVisible = true
                Log.d(TAG, "Menú del calendario mostrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar menú del calendario: ${e.message}", e)
        }
    }

    private fun hideCalendarMenu() {
        try {
            cardCalendarMenu?.let { card ->
                Log.d(TAG, "Ocultando menú del calendario")

                val fadeOut = ObjectAnimator.ofFloat(card, "alpha", 1f, 0f)
                val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 0.8f)
                val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.8f)

                fadeOut.duration = ANIMATION_DURATION
                scaleX.duration = ANIMATION_DURATION
                scaleY.duration = ANIMATION_DURATION

                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        card.visibility = View.GONE
                    }
                })

                fadeOut.start()
                scaleX.start()
                scaleY.start()

                isCalendarMenuVisible = false
                Log.d(TAG, "Menú del calendario ocultado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar menú del calendario: ${e.message}", e)
        }
    }

    private fun showGiftMenu() {
        try {
            cardGiftMenu?.let { card ->
                Log.d(TAG, "Mostrando menú de regalos")

                card.visibility = View.VISIBLE
                card.alpha = 0f

                // Animación de fade in con escala
                val fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f)
                val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 0.8f, 1f)
                val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f)

                fadeIn.duration = ANIMATION_DURATION
                scaleX.duration = ANIMATION_DURATION
                scaleY.duration = ANIMATION_DURATION

                fadeIn.start()
                scaleX.start()
                scaleY.start()

                isGiftMenuVisible = true
                Log.d(TAG, "Menú de regalos mostrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar menú de regalos: ${e.message}", e)
        }
    }

    private fun hideGiftMenu() {
        try {
            cardGiftMenu?.let { card ->
                Log.d(TAG, "Ocultando menú de regalos")

                val fadeOut = ObjectAnimator.ofFloat(card, "alpha", 1f, 0f)
                val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 0.8f)
                val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.8f)

                fadeOut.duration = ANIMATION_DURATION
                scaleX.duration = ANIMATION_DURATION
                scaleY.duration = ANIMATION_DURATION

                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        card.visibility = View.GONE
                    }
                })

                fadeOut.start()
                scaleX.start()
                scaleY.start()

                isGiftMenuVisible = false
                Log.d(TAG, "Menú de regalos ocultado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar menú de regalos: ${e.message}", e)
        }
    }

    // Ocultar navegación del ContentActivity
    private fun hideContentActivityNavigation() {
        try {
            (activity as? ContentActivity)?.let { contentActivity ->
                val headerLayout = contentActivity.findViewById<LinearLayout>(R.id.headerLayout)
                headerLayout?.visibility = View.GONE

                val bottomNav = contentActivity.findViewById<BottomNavigationView>(R.id.bottomNavigationMain)
                bottomNav?.visibility = View.GONE

                Log.d(TAG, "Elementos de navegación del ContentActivity ocultados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar navegación: ${e.message}", e)
        }
    }

    // Mostrar navegación del ContentActivity
    private fun showContentActivityNavigation() {
        try {
            (activity as? ContentActivity)?.let { contentActivity ->
                val headerLayout = contentActivity.findViewById<LinearLayout>(R.id.headerLayout)
                headerLayout?.visibility = View.VISIBLE

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
            Log.d(TAG, "RachaDiariaFragment onDestroyView")

            // Restaurar navegación al salir del fragment
            showContentActivityNavigation()

            // Limpiar referencias
            tvStreakNumber = null
            tvStreakLabel = null
            tvStreakLevel = null
            tvBestStreakValue = null
            tvTotalPointsValue = null
            tvMilestoneTitle = null
            tvMilestoneProgress = null
            tvMilestoneDescription = null
            progressBarMilestone = null
            tvMotivationalMessage = null
            tvDailyTip = null
            ivBackArrow = null
            tvCardTitle = null
            ivCalendar = null
            ivGift = null
            cardCalendarMenu = null
            cardGiftMenu = null
            ivCloseCalendar = null
            ivCloseGift = null
            weeklyHistoryContainer = null

        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroyView: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "RachaDiariaFragment onResume")

        try {
            // Asegurar que la navegación esté oculta al regresar
            hideContentActivityNavigation()

            // Refrescar datos al volver al fragment
            viewModel.refreshData()
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }
}