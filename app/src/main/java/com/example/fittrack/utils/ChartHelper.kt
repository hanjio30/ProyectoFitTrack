package com.example.fittrack.utils

import android.graphics.Color
import android.graphics.Typeface
import com.example.fittrack.ViewModel.EstadisticasViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

/**
 * Clase helper para configurar y manejar gráficos MPAndroidChart
 */
class ChartHelper {

    companion object {
        // Colores del tema de la app
        private const val COLOR_PRIMARY = "#4CAF50"
        private const val COLOR_SECONDARY = "#FF6B35"
        private const val COLOR_ACCENT = "#2196F3"
        private const val COLOR_TEXT = "#333333"
        private const val COLOR_TEXT_LIGHT = "#666666"
        private const val COLOR_GRID = "#E0E0E0"

        /**
         * Configurar gráfico de barras para distancia diaria
         */
        fun configurarGraficoDistancia(
            chart: BarChart,
            datos: List<EstadisticasViewModel.DatoGrafico>
        ) {
            // Configuración básica del gráfico
            chart.apply {
                description.isEnabled = false
                legend.isEnabled = false

                // Configurar interacciones
                setTouchEnabled(true)
                setDragEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)

                // Configurar márgenes
                setExtraOffsets(5f, 10f, 5f, 5f)
            }

            // Configurar ejes
            configurarEjeX(chart.xAxis, datos.map { it.fechaCorta })
            configurarEjeY(chart.axisLeft, "km")
            chart.axisRight.isEnabled = false

            // Crear datos para el gráfico
            val entries = datos.mapIndexed { index, dato ->
                BarEntry(index.toFloat(), dato.valor)
            }

            val dataSet = BarDataSet(entries, "Distancia").apply {
                color = Color.parseColor(COLOR_PRIMARY)
                valueTextColor = Color.parseColor(COLOR_TEXT)
                valueTextSize = 10f
                valueTypeface = Typeface.DEFAULT_BOLD

                // Formatter para mostrar valores con "km"
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value > 0) "${String.format("%.1f", value)}" else ""
                    }
                }
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.8f
            }

            chart.data = barData
            chart.animateY(1000)
            chart.invalidate()
        }

        /**
         * Configurar gráfico de líneas para calorías
         */
        fun configurarGraficoCalorias(
            chart: LineChart,
            datos: List<EstadisticasViewModel.DatoGrafico>
        ) {
            // Configuración básica del gráfico
            chart.apply {
                description.isEnabled = false
                legend.isEnabled = false

                // Configurar interacciones
                setTouchEnabled(true)
                setDragEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)

                // Configurar márgenes
                setExtraOffsets(5f, 10f, 5f, 5f)
            }

            // Configurar ejes
            configurarEjeX(chart.xAxis, datos.map { it.fechaCorta })
            configurarEjeY(chart.axisLeft, "cal")
            chart.axisRight.isEnabled = false

            // Crear datos para el gráfico
            val entries = datos.mapIndexed { index, dato ->
                Entry(index.toFloat(), dato.valor)
            }

            val dataSet = LineDataSet(entries, "Calorías").apply {
                color = Color.parseColor(COLOR_SECONDARY)
                setCircleColor(Color.parseColor(COLOR_SECONDARY))
                lineWidth = 3f
                circleRadius = 5f
                setDrawCircleHole(false)
                valueTextColor = Color.parseColor(COLOR_TEXT)
                valueTextSize = 10f
                valueTypeface = Typeface.DEFAULT_BOLD

                // Configurar fill
                setDrawFilled(true)
                fillColor = Color.parseColor(COLOR_SECONDARY)
                fillAlpha = 50

                // Formatter para mostrar valores con "cal"
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value > 0) "${value.toInt()}" else ""
                    }
                }
            }

            val lineData = LineData(dataSet)
            chart.data = lineData
            chart.animateX(1000)
            chart.invalidate()
        }

        /**
         * Configurar gráfico de barras para pasos diarios
         */
        fun configurarGraficoPasos(
            chart: BarChart,
            datos: List<EstadisticasViewModel.DatoGrafico>
        ) {
            // Configuración básica del gráfico
            chart.apply {
                description.isEnabled = false
                legend.isEnabled = false

                // Configurar interacciones
                setTouchEnabled(true)
                setDragEnabled(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)

                // Configurar márgenes
                setExtraOffsets(5f, 10f, 5f, 5f)
            }

            // Configurar ejes
            configurarEjeX(chart.xAxis, datos.map { it.fechaCorta })
            configurarEjeY(chart.axisLeft, "pasos")
            chart.axisRight.isEnabled = false

            // Crear datos para el gráfico
            val entries = datos.mapIndexed { index, dato ->
                BarEntry(index.toFloat(), dato.valor)
            }

            val dataSet = BarDataSet(entries, "Pasos").apply {
                color = Color.parseColor(COLOR_ACCENT)
                valueTextColor = Color.parseColor(COLOR_TEXT)
                valueTextSize = 10f
                valueTypeface = Typeface.DEFAULT_BOLD

                // Formatter para mostrar valores
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value > 0) {
                            when {
                                value >= 1000 -> "${(value / 1000).toInt()}k"
                                else -> value.toInt().toString()
                            }
                        } else ""
                    }
                }
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.8f
            }

            chart.data = barData
            chart.animateY(1000)
            chart.invalidate()
        }

        /**
         * Configurar eje X común para todos los gráficos
         */
        private fun configurarEjeX(xAxis: XAxis, labels: List<String>) {
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                textColor = Color.parseColor(COLOR_TEXT_LIGHT)
                textSize = 12f
                granularity = 1f
                labelCount = labels.size
                valueFormatter = IndexAxisValueFormatter(labels)

                // Configurar líneas
                axisLineColor = Color.parseColor(COLOR_GRID)
                axisLineWidth = 1f
            }
        }

        /**
         * Configurar eje Y común para todos los gráficos
         */
        private fun configurarEjeY(yAxis: YAxis, unidad: String) {
            yAxis.apply {
                setDrawGridLines(true)
                setDrawAxisLine(false)
                textColor = Color.parseColor(COLOR_TEXT_LIGHT)
                textSize = 12f
                granularity = 1f

                // Configurar líneas de grid
                gridColor = Color.parseColor(COLOR_GRID)
                gridLineWidth = 0.5f

                // Configurar valores mínimos
                axisMinimum = 0f

                // Formatter personalizado según la unidad
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when (unidad) {
                            "km" -> if (value == 0f) "0" else String.format("%.1f", value)
                            "cal" -> if (value == 0f) "0" else value.toInt().toString()
                            "pasos" -> {
                                when {
                                    value == 0f -> "0"
                                    value >= 1000 -> "${(value / 1000).toInt()}k"
                                    else -> value.toInt().toString()
                                }
                            }
                            else -> value.toInt().toString()
                        }
                    }
                }
            }
        }

        /**
         * Limpiar y resetear un gráfico de barras
         */
        fun limpiarGraficoBarras(chart: BarChart) {
            chart.clear()
            chart.invalidate()
        }

        /**
         * Limpiar y resetear un gráfico de líneas
         */
        fun limpiarGraficoLineas(chart: LineChart) {
            chart.clear()
            chart.invalidate()
        }

        /**
         * Mostrar mensaje "Sin datos" en gráfico de barras
         */
        fun mostrarSinDatos(chart: BarChart, mensaje: String = "Sin datos disponibles") {
            chart.clear()
            chart.setNoDataText(mensaje)
            chart.setNoDataTextColor(Color.parseColor(COLOR_TEXT_LIGHT))
            chart.invalidate()
        }

        /**
         * Mostrar mensaje "Sin datos" en gráfico de líneas
         */
        fun mostrarSinDatos(chart: LineChart, mensaje: String = "Sin datos disponibles") {
            chart.clear()
            chart.setNoDataText(mensaje)
            chart.setNoDataTextColor(Color.parseColor(COLOR_TEXT_LIGHT))
            chart.invalidate()
        }

        /**
         * Verificar si hay datos válidos para mostrar
         */
        fun hayDatosValidos(datos: List<EstadisticasViewModel.DatoGrafico>): Boolean {
            return datos.isNotEmpty() && datos.any { it.valor > 0 }
        }

        /**
         * Obtener el valor máximo de una lista de datos (útil para configurar escalas)
         */
        fun obtenerValorMaximo(datos: List<EstadisticasViewModel.DatoGrafico>): Float {
            return datos.maxOfOrNull { it.valor } ?: 0f
        }

        /**
         * Configurar colores personalizados para las barras basado en valores
         */
        fun configurarColoresPersonalizados(
            dataSet: BarDataSet,
            datos: List<EstadisticasViewModel.DatoGrafico>,
            colorAlto: String = COLOR_PRIMARY,
            colorMedio: String = COLOR_SECONDARY,
            colorBajo: String = COLOR_ACCENT
        ) {
            val valorMaximo = datos.maxOfOrNull { it.valor } ?: 0f
            val colores = mutableListOf<Int>()

            datos.forEach { dato ->
                val color = when {
                    dato.valor >= valorMaximo * 0.7f -> Color.parseColor(colorAlto)
                    dato.valor >= valorMaximo * 0.3f -> Color.parseColor(colorMedio)
                    else -> Color.parseColor(colorBajo)
                }
                colores.add(color)
            }

            dataSet.colors = colores
        }
    }
}