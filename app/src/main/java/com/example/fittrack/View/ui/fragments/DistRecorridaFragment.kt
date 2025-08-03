package com.example.fittrack.View.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fittrack.R
import com.example.fittrack.View.ui.activities.ContentActivity
import com.example.fittrack.ViewModel.DistanciaRecorridaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class DistRecorridaFragment : Fragment() {

    private lateinit var viewModel: DistanciaRecorridaViewModel

    // Views principales
    private var tvDistanciaTotal: TextView? = null
    private var tvDistanciaSemana: TextView? = null
    private var tvDistanciaMes: TextView? = null
    private var ivBackArrow: ImageView? = null
    private var tvCardTitle: TextView? = null

    companion object {
        private const val TAG = "DistRecorridaFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d(TAG, "=== CREANDO DistRecorridaFragment ===")
            inflater.inflate(R.layout.distancia_recorrida, container, false)
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
            setupObservers()
            loadData()

            Log.d(TAG, "=== DistRecorridaFragment CONFIGURADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error en onViewCreated: ${e.message}", e)
        }
    }

    private fun initializeViewModel() {
        try {
            viewModel = ViewModelProvider(this, DistanciaRecorridaViewModelFactory(requireContext()))[DistanciaRecorridaViewModel::class.java]
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar ViewModel: ${e.message}", e)
        }
    }

    private fun initializeViews(view: View) {
        try {
            // Buscar vistas en el layout
            tvDistanciaTotal = view.findViewById(R.id.tvDistanciaTotal)
            tvDistanciaSemana = view.findViewById(R.id.tvDistanciaSemana)
            tvDistanciaMes = view.findViewById(R.id.tvDistanciaMes)

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

            tvCardTitle?.text = "Distancia Recorrida"

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

    private fun setupObservers() {
        try {
            viewModel.distanciaTotal.observe(viewLifecycleOwner) { distancia ->
                tvDistanciaTotal?.text = distancia
            }

            viewModel.distanciaSemana.observe(viewLifecycleOwner) { distancia ->
                tvDistanciaSemana?.text = distancia
            }

            viewModel.distanciaMes.observe(viewLifecycleOwner) { distancia ->
                tvDistanciaMes?.text = distancia
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                if (!error.isNullOrEmpty()) {
                    Log.e(TAG, "Error del ViewModel: $error")
                    // Aquí puedes mostrar un Toast o Snackbar
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

            // Refrescar datos para mostrar los más actuales
            viewModel.refreshData()
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
            Log.d(TAG, "DistRecorridaFragment onDestroyView")

            // ✅ RESTAURAR NAVEGACIÓN AL SALIR DEL FRAGMENT
            showContentActivityNavigation()

            // Limpiar referencias
            tvDistanciaTotal = null
            tvDistanciaSemana = null
            tvDistanciaMes = null
            ivBackArrow = null
            tvCardTitle = null

        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroyView: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "DistRecorridaFragment onResume")

        try {
            // Asegurar que la navegación esté oculta al regresar
            hideContentActivityNavigation()

            // Refrescar datos por si vienen de MapFragment
            viewModel.refreshData()
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }
    class DistanciaRecorridaViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DistanciaRecorridaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DistanciaRecorridaViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}