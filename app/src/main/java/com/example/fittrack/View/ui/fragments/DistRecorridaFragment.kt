package com.example.fittrack.View.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fittrack.R

class DistRecorridaFragment : Fragment() {

    // Views del layout
    private lateinit var tvDistanciaTotal: TextView
    private lateinit var tvDistanciaSemana: TextView
    private lateinit var tvDistanciaMes: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.distancia_recorrida, container, false)
    }

    // ✅ MANEJAR ARGUMENTOS RECIBIDOS
    private fun handleArguments() {
        arguments?.let { args ->
            val userName = args.getString("userName")
            // Usar el userName si es necesario
            userName?.let {
                // Personalizar la vista según el usuario
            }
        }
    }

    // ✅ CONFIGURAR NAVEGACIÓN DE RETROCESO
    private fun setupBackButton() {
        // Manejar el botón de retroceso del dispositivo
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Navegar de regreso usando Navigation Component
                    findNavController().navigateUp()
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el título del header
        setupHeader(view)

        // Inicializar las vistas
        initViews(view)

        // Configurar datos (puedes conectar esto con tu lógica de datos)
        setupData()

        // Configurar botón de retroceso
        setupBackButton()

        // Obtener argumentos si los hay
        handleArguments()
    }

    private fun setupHeader(view: View) {
        // Configurar el título del header como te pidieron
        val headerTitle = view.findViewById<TextView>(R.id.tvCardTitle)
        headerTitle.text = "Distancia recorrida"
    }

    private fun initViews(view: View) {
        tvDistanciaTotal = view.findViewById(R.id.tvDistanciaTotal)
        tvDistanciaSemana = view.findViewById(R.id.tvDistanciaSemana)
        tvDistanciaMes = view.findViewById(R.id.tvDistanciaMes)
    }

    private fun setupData() {
        // Aquí puedes conectar con tu base de datos o API
        // Por ahora uso datos de ejemplo basados en tu XML

        tvDistanciaTotal.text = "150.5 km"
        tvDistanciaSemana.text = "28.5 km"
        tvDistanciaMes.text = "112.8 km"
    }

    // Método para actualizar datos desde fuera del fragment si necesitas
    fun updateDistanceData(total: String, semana: String, mes: String) {
        if (::tvDistanciaTotal.isInitialized) {
            tvDistanciaTotal.text = total
            tvDistanciaSemana.text = semana
            tvDistanciaMes.text = mes
        }
    }

    companion object {
        fun newInstance(): DistRecorridaFragment {
            return DistRecorridaFragment()
        }
    }
}