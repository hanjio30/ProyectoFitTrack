package com.example.fittrack.View.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.databinding.FragmentEstadisticasBinding
import com.example.fittrack.ViewModel.EstadisticasViewModel
import com.example.fittrack.utils.ChartHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart

class EstadisticasFragment : Fragment() {

    private var _binding: FragmentEstadisticasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EstadisticasViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "EstadisticasFragment"

    // Referencias a los gráficos
    private lateinit var chartDistancia: BarChart
    private lateinit var chartCalorias: LineChart
    private lateinit var chartPasos: BarChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadisticasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarGraficos()
        setupObservers()
        cargarEstadisticas()
    }

    /**
     * Inicializar referencias a los gráficos
     */
    private fun inicializarGraficos() {
        chartDistancia = binding.chartDistanciaBarras
        chartCalorias = binding.chartCaloriasLinea
        chartPasos = binding.chartPasosBarras

        Log.d(TAG, "📊 Gráficos inicializados")
    }

    private fun setupObservers() {
        // Observar cambios en las estadísticas básicas
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estadisticas.collect { estadisticas ->
                updateStatsCards(estadisticas)
            }
        }

        // ===== NUEVO: Observar datos para gráficos =====
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.datosGraficos.collect { datosGraficos ->
                actualizarGraficos(datosGraficos)
            }
        }

        // ===== NUEVO: Observar meta diaria =====
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.metaDiaria.collect { metaDiaria ->
                actualizarMetaDiaria(metaDiaria)
            }
        }

        // Observar estado de carga
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Mostrar/ocultar indicadores de carga en gráficos si es necesario
                if (isLoading) {
                    mostrarCargandoEnGraficos()
                }
                Log.d(TAG, "Loading state: $isLoading")
            }
        }

        // Observar errores
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Log.e(TAG, "Error en estadísticas: $it")
                    Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_SHORT).show()
                    viewModel.limpiarError()
                }
            }
        }
    }

    /**
     * Actualizar las tarjetas de estadísticas básicas (función existente)
     */
    private fun updateStatsCards(estadisticas: EstadisticasViewModel.EstadisticasUiState) {
        Log.d(TAG, "Actualizando tarjetas con: Pasos=${estadisticas.totalPasos}, Calorías=${estadisticas.totalCalorias}")

        // Actualizar pasos con formato
        binding.tvPasos.text = formatearNumero(estadisticas.totalPasos)

        // Actualizar calorías con formato
        binding.tvCalorias.text = formatearNumero(estadisticas.totalCalorias)
    }

    // ===== NUEVA FUNCIÓN: Actualizar gráficos =====
    private fun actualizarGraficos(datosGraficos: EstadisticasViewModel.DatosGraficosUiState) {
        Log.d(TAG, "📊 Actualizando gráficos...")

        try {
            // Actualizar gráfico de distancia
            if (ChartHelper.hayDatosValidos(datosGraficos.distanciaUltimos7Dias)) {
                ChartHelper.configurarGraficoDistancia(chartDistancia, datosGraficos.distanciaUltimos7Dias)
                Log.d(TAG, "✅ Gráfico de distancia actualizado")
            } else {
                ChartHelper.mostrarSinDatos(chartDistancia, "Sin datos de distancia")
                Log.d(TAG, "⚠️ Sin datos para gráfico de distancia")
            }

            // Actualizar gráfico de calorías
            if (ChartHelper.hayDatosValidos(datosGraficos.caloriasUltimos7Dias)) {
                ChartHelper.configurarGraficoCalorias(chartCalorias, datosGraficos.caloriasUltimos7Dias)
                Log.d(TAG, "✅ Gráfico de calorías actualizado")
            } else {
                ChartHelper.mostrarSinDatos(chartCalorias, "Sin datos de calorías")
                Log.d(TAG, "⚠️ Sin datos para gráfico de calorías")
            }

            // Actualizar gráfico de pasos
            if (ChartHelper.hayDatosValidos(datosGraficos.pasosUltimos7Dias)) {
                ChartHelper.configurarGraficoPasos(chartPasos, datosGraficos.pasosUltimos7Dias)
                Log.d(TAG, "✅ Gráfico de pasos actualizado")
            } else {
                ChartHelper.mostrarSinDatos(chartPasos, "Sin datos de pasos")
                Log.d(TAG, "⚠️ Sin datos para gráfico de pasos")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando gráficos: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al actualizar gráficos", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== NUEVA FUNCIÓN: Actualizar meta diaria =====
    private fun actualizarMetaDiaria(metaDiaria: EstadisticasViewModel.MetaDiariaUiState) {
        Log.d(TAG, "🎯 Actualizando meta diaria: ${metaDiaria.pasosActuales}/${metaDiaria.metaPasos}")

        try {
            // Actualizar barra de progreso
            binding.progressBarMeta.apply {
                max = 100
                progress = metaDiaria.porcentajeCompletado
            }

            // Actualizar texto de porcentaje
            binding.tvProgresoPorcentaje.text = "${metaDiaria.porcentajeCompletado}%"

            // Actualizar texto de progreso
            binding.tvProgresoTexto.text = "${formatearNumero(metaDiaria.pasosActuales)} / ${formatearNumero(metaDiaria.metaPasos)} pasos"

            // Cambiar color si la meta está alcanzada
            if (metaDiaria.metaAlcanzada) {
                binding.tvProgresoPorcentaje.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                Log.d(TAG, "🎉 ¡Meta diaria alcanzada!")
            } else {
                binding.tvProgresoPorcentaje.setTextColor(requireContext().getColor(android.R.color.holo_green_light))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando meta diaria: ${e.message}", e)
        }
    }

    // ===== NUEVA FUNCIÓN: Mostrar indicadores de carga en gráficos =====
    private fun mostrarCargandoEnGraficos() {
        try {
            ChartHelper.mostrarSinDatos(chartDistancia, "Cargando datos...")
            ChartHelper.mostrarSinDatos(chartCalorias, "Cargando datos...")
            ChartHelper.mostrarSinDatos(chartPasos, "Cargando datos...")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando estado de carga: ${e.message}", e)
        }
    }

    /**
     * Formatear números para mostrar en las tarjetas (función existente)
     */
    private fun formatearNumero(numero: Int): String {
        return when {
            numero >= 1000000 -> {
                String.format("%.1fM", numero / 1000000.0)
            }
            numero >= 1000 -> {
                String.format("%.1fK", numero / 1000.0)
            }
            else -> numero.toString()
        }
    }

    private fun cargarEstadisticas() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Cargando estadísticas para usuario: ${currentUser.uid}")
            viewModel.cargarEstadisticasDelDia(currentUser.uid)
        } else {
            Log.w(TAG, "Usuario no autenticado")
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== NUEVA FUNCIÓN: Refrescar todos los datos =====
    private fun refrescarTodo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "🔄 Refrescando todas las estadísticas y gráficos")

            // Limpiar gráficos primero
            mostrarCargandoEnGraficos()

            // Recargar datos
            viewModel.refrescarEstadisticas(currentUser.uid)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar estadísticas cuando el fragment se vuelve visible
        cargarEstadisticas()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Limpiar recursos de los gráficos
        try {
            if (::chartDistancia.isInitialized) {
                ChartHelper.limpiarGraficoBarras(chartDistancia)
            }
            if (::chartCalorias.isInitialized) {
                ChartHelper.limpiarGraficoLineas(chartCalorias)
            }
            if (::chartPasos.isInitialized) {
                ChartHelper.limpiarGraficoBarras(chartPasos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando gráficos: ${e.message}", e)
        }

        _binding = null
    }

    // ===== FUNCIONES PÚBLICAS PARA INTERACCIÓN EXTERNA =====

    /**
     * Función pública para refrescar desde otros components
     */
    fun refrescarEstadisticas() {
        refrescarTodo()
    }

    /**
     * Función pública para configurar meta de pasos personalizada
     */
    fun configurarMetaPasos(nuevaMetaPasos: Int) {
        viewModel.configurarMetaPasos(nuevaMetaPasos)
    }
}