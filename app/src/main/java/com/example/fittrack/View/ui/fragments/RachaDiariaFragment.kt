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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fittrack.R
import com.example.fittrack.View.ui.activities.ContentActivity
import com.example.fittrack.ViewModel.RachaDiariaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class RachaDiariaFragment : Fragment() {

    private lateinit var viewModel: RachaDiariaViewModel

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

    // ✅ NUEVOS CARDVIEWS DESPLEGABLES
    private var cardCalendarMenu: CardView? = null
    private var cardGiftMenu: CardView? = null
    private var ivCloseCalendar: ImageView? = null
    private var ivCloseGift: ImageView? = null

    // Estado de visibilidad de los cards
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

            // ✅ OCULTAR ELEMENTOS DE NAVEGACIÓN DEL CONTENTACTIVITY
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

            // ✅ NUEVOS CARDVIEWS DESPLEGABLES
            cardCalendarMenu = view.findViewById(R.id.card_calendar_menu)
            cardGiftMenu = view.findViewById(R.id.card_gift_menu)
            ivCloseCalendar = view.findViewById(R.id.iv_close_calendar)
            ivCloseGift = view.findViewById(R.id.iv_close_gift)

            Log.d(TAG, "Vistas inicializadas correctamente")
            Log.d(TAG, "CardCalendarMenu encontrado: ${cardCalendarMenu != null}")
            Log.d(TAG, "CardGiftMenu encontrado: ${cardGiftMenu != null}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
        }
    }

    private fun setupHeader() {
        try {
            Log.d(TAG, "BackArrow encontrado: ${ivBackArrow != null}")
            Log.d(TAG, "TitleView encontrado: ${tvCardTitle != null}")

            tvCardTitle?.text = "Racha Diaria"

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

    private fun setupClickListeners() {
        try {
            // ✅ CLICK EN ÍCONO DE CALENDARIO
            ivCalendar?.setOnClickListener {
                Log.d(TAG, "Click en calendario")
                toggleCalendarMenu()
            }

            // ✅ CLICK EN ÍCONO DE REGALO
            ivGift?.setOnClickListener {
                Log.d(TAG, "Click en regalo")
                toggleGiftMenu()
            }

            // ✅ BOTONES DE CERRAR DENTRO DE LOS CARDS
            ivCloseCalendar?.setOnClickListener {
                Log.d(TAG, "Click en cerrar calendario")
                hideCalendarMenu()
            }

            ivCloseGift?.setOnClickListener {
                Log.d(TAG, "Click en cerrar regalo")
                hideGiftMenu()
            }

            // Click en la card principal para incrementar racha (solo para testing)
            view?.findViewById<androidx.cardview.widget.CardView>(R.id.cv_main_streak)?.setOnClickListener {
                Log.d(TAG, "Click en card principal - incrementando racha")
                viewModel.incrementStreak()
                viewModel.updateMilestoneProgress()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar click listeners: ${e.message}", e)
        }
    }

    // ✅ FUNCIÓN PARA ALTERNAR LA VISIBILIDAD DEL MENÚ DEL CALENDARIO
    private fun toggleCalendarMenu() {
        try {
            if (isCalendarMenuVisible) {
                hideCalendarMenu()
            } else {
                showCalendarMenu()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al alternar menú de calendario: ${e.message}", e)
        }
    }

    // ✅ FUNCIÓN PARA ALTERNAR LA VISIBILIDAD DEL MENÚ DE REGALOS
    private fun toggleGiftMenu() {
        try {
            if (isGiftMenuVisible) {
                hideGiftMenu()
            } else {
                showGiftMenu()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al alternar menú de regalos: ${e.message}", e)
        }
    }

    // ✅ MOSTRAR MENÚ DEL CALENDARIO CON ANIMACIÓN
    private fun showCalendarMenu() {
        try {
            cardCalendarMenu?.let { card ->
                Log.d(TAG, "Mostrando menú del calendario")

                // Hacer visible el card
                card.visibility = View.VISIBLE
                card.alpha = 0f

                // Animación de fade in
                ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
                    duration = ANIMATION_DURATION
                    start()
                }

                isCalendarMenuVisible = true
                Log.d(TAG, "Menú del calendario mostrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar menú del calendario: ${e.message}", e)
        }
    }

    // ✅ OCULTAR MENÚ DEL CALENDARIO CON ANIMACIÓN
    private fun hideCalendarMenu() {
        try {
            cardCalendarMenu?.let { card ->
                Log.d(TAG, "Ocultando menú del calendario")

                // Animación de fade out
                ObjectAnimator.ofFloat(card, "alpha", 1f, 0f).apply {
                    duration = ANIMATION_DURATION
                    start()
                }.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        card.visibility = View.GONE
                    }
                })

                isCalendarMenuVisible = false
                Log.d(TAG, "Menú del calendario ocultado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar menú del calendario: ${e.message}", e)
        }
    }

    // ✅ MOSTRAR MENÚ DE REGALOS CON ANIMACIÓN
    private fun showGiftMenu() {
        try {
            cardGiftMenu?.let { card ->
                Log.d(TAG, "Mostrando menú de regalos")

                // Hacer visible el card
                card.visibility = View.VISIBLE
                card.alpha = 0f

                // Animación de fade in
                ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
                    duration = ANIMATION_DURATION
                    start()
                }

                isGiftMenuVisible = true
                Log.d(TAG, "Menú de regalos mostrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar menú de regalos: ${e.message}", e)
        }
    }

    // ✅ OCULTAR MENÚ DE REGALOS CON ANIMACIÓN
    private fun hideGiftMenu() {
        try {
            cardGiftMenu?.let { card ->
                Log.d(TAG, "Ocultando menú de regalos")

                // Animación de fade out
                ObjectAnimator.ofFloat(card, "alpha", 1f, 0f).apply {
                    duration = ANIMATION_DURATION
                    start()
                }.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        card.visibility = View.GONE
                    }
                })

                isGiftMenuVisible = false
                Log.d(TAG, "Menú de regalos ocultado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar menú de regalos: ${e.message}", e)
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
            }

            viewModel.totalPoints.observe(viewLifecycleOwner) { points ->
                tvTotalPointsValue?.text = points.toString()
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
            }

            viewModel.dailyTip.observe(viewLifecycleOwner) { tip ->
                tvDailyTip?.text = tip
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

            viewModel.loadStreakData(userName)
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
            Log.d(TAG, "RachaDiariaFragment onDestroyView")

            // ✅ RESTAURAR NAVEGACIÓN AL SALIR DEL FRAGMENT
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

            // ✅ LIMPIAR REFERENCIAS DE LOS NUEVOS CARDVIEWS
            cardCalendarMenu = null
            cardGiftMenu = null
            ivCloseCalendar = null
            ivCloseGift = null

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
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }
}