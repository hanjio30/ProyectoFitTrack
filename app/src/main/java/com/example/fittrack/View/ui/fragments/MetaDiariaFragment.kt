package com.example.fittrack.View.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fittrack.R
import com.example.fittrack.View.ui.activities.ContentActivity
import com.example.fittrack.ViewModel.MetaDiariaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MetaDiariaFragment : Fragment() {

    private lateinit var viewModel: MetaDiariaViewModel

    // Views principales
    private var tvProgressValue: TextView? = null
    private var tvProgressTotal: TextView? = null
    private var tvProgressPercent: TextView? = null
    private var tvGoalMessage: TextView? = null
    private var tvPointsTotal: TextView? = null
    private var ivBackArrow: ImageView? = null
    private var tvCardTitle: TextView? = null
    private var ivEditIcon: ImageView? = null

    companion object {
        private const val TAG = "MetaDiariaFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d(TAG, "=== CREANDO MetaDiariaFragment ===")
            inflater.inflate(R.layout.meta_diaria, container, false)
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

            Log.d(TAG, "=== MetaDiariaFragment CONFIGURADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onViewCreated: ${e.message}", e)
        }
    }

    private fun initializeViewModel() {
        try {
            viewModel = ViewModelProvider(this)[MetaDiariaViewModel::class.java]
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar ViewModel: ${e.message}", e)
        }
    }

    private fun initializeViews(view: View) {
        try {
            // Buscar vistas principales
            tvProgressValue = view.findViewById(R.id.tv_progress_value)
            tvProgressTotal = view.findViewById(R.id.tv_progress_total)
            tvProgressPercent = view.findViewById(R.id.tv_progress_percent)
            tvGoalMessage = view.findViewById(R.id.tv_goal_message)
            tvPointsTotal = view.findViewById(R.id.tv_points_total)

            // Buscar vistas del header incluido
            ivBackArrow = view.findViewById(R.id.ivBackArrow)
            tvCardTitle = view.findViewById(R.id.tvCardTitle)

            // Buscar el ícono de editar
            ivEditIcon = view.findViewById(R.id.iv_edit_icon)

            Log.d(TAG, "Vistas inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
        }
    }

    private fun setupHeader() {
        try {
            Log.d(TAG, "BackArrow encontrado: ${ivBackArrow != null}")
            Log.d(TAG, "TitleView encontrado: ${tvCardTitle != null}")

            tvCardTitle?.text = "Meta Diaria"

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
            // Click en el ícono de editar para abrir el popup
            ivEditIcon?.setOnClickListener {
                Log.d(TAG, "Click en ícono de editar detectado!")
                showEditGoalPopup()
            }

            Log.d(TAG, "Click listeners configurados")

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar click listeners: ${e.message}", e)
        }
    }

    private fun setupObservers() {
        try {
            viewModel.progresoActual.observe(viewLifecycleOwner) { progreso ->
                tvProgressValue?.text = progreso.toString()
            }

            viewModel.metaDiaria.observe(viewLifecycleOwner) { meta ->
                tvProgressTotal?.text = "/$meta km"
            }

            viewModel.porcentajeCompletado.observe(viewLifecycleOwner) { porcentaje ->
                tvProgressPercent?.text = "$porcentaje% Completado"
            }

            viewModel.mensajeMeta.observe(viewLifecycleOwner) { mensaje ->
                tvGoalMessage?.text = mensaje
            }

            viewModel.puntosGanados.observe(viewLifecycleOwner) { puntos ->
                tvPointsTotal?.text = "+$puntos"
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                if (!error.isNullOrEmpty()) {
                    Log.e(TAG, "Error del ViewModel: $error")
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }

            viewModel.metaActualizada.observe(viewLifecycleOwner) { actualizada ->
                if (actualizada) {
                    Toast.makeText(context, "Meta actualizada correctamente", Toast.LENGTH_SHORT).show()
                    viewModel.clearMetaActualizadaFlag()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar observers: ${e.message}", e)
        }
    }

    private fun loadData() {
        try {
            val userName = arguments?.getString("userName")
            Log.d(TAG, "Cargando datos para usuario: $userName")

            viewModel.loadGoalData(userName)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
        }
    }

    private fun showEditGoalPopup() {
        try {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.popup_meta_diaria)

            // Buscar vistas del popup
            val etMetaKilometros = dialog.findViewById<EditText>(R.id.et_meta_kilometros)
            val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
            val btnSave = dialog.findViewById<Button>(R.id.btn_save)

            // Establecer el valor actual de la meta
            viewModel.metaDiaria.value?.let { metaActual ->
                etMetaKilometros.setText(metaActual.toString())
            }

            // Configurar botón cancelar
            btnCancel.setOnClickListener {
                Log.d(TAG, "Popup cancelado")
                dialog.dismiss()
            }

            // Configurar botón guardar
            btnSave.setOnClickListener {
                val nuevaMeta = etMetaKilometros.text.toString().trim()

                if (nuevaMeta.isNotEmpty()) {
                    try {
                        val metaValue = nuevaMeta.toDouble()
                        if (metaValue > 0) {
                            Log.d(TAG, "Guardando nueva meta: $metaValue")
                            viewModel.updateGoal(metaValue)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, "La meta debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(context, "Por favor ingresa un número válido", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Por favor ingresa una meta", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
            Log.d(TAG, "Popup mostrado")

        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar popup: ${e.message}", e)
            Toast.makeText(context, "Error al abrir configuración", Toast.LENGTH_SHORT).show()
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
            Log.d(TAG, "MetaDiariaFragment onDestroyView")

            // ✅ RESTAURAR NAVEGACIÓN AL SALIR DEL FRAGMENT
            showContentActivityNavigation()

            // Limpiar referencias
            tvProgressValue = null
            tvProgressTotal = null
            tvProgressPercent = null
            tvGoalMessage = null
            tvPointsTotal = null
            ivBackArrow = null
            tvCardTitle = null
            ivEditIcon = null

        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroyView: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MetaDiariaFragment onResume")

        try {
            // Asegurar que la navegación esté oculta al regresar
            hideContentActivityNavigation()
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }
}