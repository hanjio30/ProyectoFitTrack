package com.example.fittrack.View.ui.fragments

import android.widget.LinearLayout.LayoutParams
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.fittrack.R
import com.example.fittrack.ViewModel.RecorridoViewModel
import java.text.SimpleDateFormat
import java.util.*

class RecorridoFragment : Fragment() {

    private val recorridoViewModel: RecorridoViewModel by activityViewModels()
    private lateinit var containerRecorridos: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recorrido, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerRecorridos = view.findViewById(R.id.containerRecorridos)
        setupObservers()
        // Solo para pruebas - eliminar después
        // recorridoViewModel.agregarDatosPrueba()

    }

    private fun setupObservers() {
        recorridoViewModel.recorridos.observe(viewLifecycleOwner, Observer { recorridos ->
            actualizarHistorialRecorridos(recorridos)
        })
    }

    private fun actualizarHistorialRecorridos(recorridos: MutableList<com.example.fittrack.ViewModel.Recorrido>) {
        containerRecorridos.removeAllViews()

        if (recorridos.isEmpty()) {
            mostrarMensajeVacio()
            return
        }

        // Agrupar por día de la semana
        val recorridosPorDia = recorridos.groupBy { obtenerDiaSemanaReal(it.fecha) }

        // Ordenar días (Lunes, Martes, etc.)
        val diasOrdenados = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

        diasOrdenados.forEach { dia ->
            val recorridosDelDia = recorridosPorDia[dia]
            if (!recorridosDelDia.isNullOrEmpty()) {
                agregarTituloDia(dia)

                // Crear contenedor de timeline para este día
                val timelineContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 64 // Espacio entre días
                    }
                }

                recorridosDelDia.sortedBy { it.hora }.forEachIndexed { index, recorrido ->
                    val recorridoView = crearVistaRecorridoMejorada(recorrido, index == recorridosDelDia.size - 1)
                    timelineContainer.addView(recorridoView)
                }

                containerRecorridos.addView(timelineContainer)
            }
        }
    }

    private fun obtenerDiaSemanaReal(fecha: String): String {
        return try {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val fechaRecorrido = sdf.parse(fecha)
            val fechaHoy = Date()

            val calRecorrido = Calendar.getInstance().apply { time = fechaRecorrido }
            val calHoy = Calendar.getInstance().apply { time = fechaHoy }

            /// Obtener día de la semana (incluso si es hoy)

            // Obtener día de la semana
            when (calRecorrido.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> "Hoy"
            }
        } catch (e: Exception) {
            "Hoy"
        }
    }

    private fun mostrarMensajeVacio() {
        val textView = TextView(requireContext()).apply {
            text = "No hay recorridos registrados"
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            setPadding(32, 64, 32, 64)
            gravity = android.view.Gravity.CENTER
        }
        containerRecorridos.addView(textView)
    }

    private fun agregarTituloDia(dia: String) {
        val tituloView = TextView(requireContext()).apply {
            text = dia
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }
        containerRecorridos.addView(tituloView)
    }

    private fun crearVistaRecorridoMejorada(recorrido: com.example.fittrack.ViewModel.Recorrido, esUltimo: Boolean): View {
        // Contenedor principal horizontal
        val mainContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 40
            }
        }

        // === TIMELINE (HORA Y LÍNEA) ===
        val timelineContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = 32
                width = 120 // Ancho fijo para que se vea mejor
            }
        }

        // Hora - SIEMPRE visible
        val horaText = TextView(requireContext()).apply {
            text = recorrido.hora
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        timelineContainer.addView(horaText)

        // Línea vertical (solo si no es el último)
        if (!esUltimo) {
            val lineaVertical = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(4, 200).apply {
                    topMargin = 16
                }
                setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            timelineContainer.addView(lineaVertical)
        }

        // === CARD DEL RECORRIDO ===
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                topMargin = 16
            }
            radius = 24f
            cardElevation = 4f
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.teal_700))
        }

        // Contenido del card
        val cardContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 24, 24, 24)
        }

        // === IMAGEN PLACEHOLDER ===
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                rightMargin = 24
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            // Puedes cambiar por: setImageResource(R.drawable.ic_placeholder_image)
        }

        // === INFORMACIÓN DEL RECORRIDO ===
        val infoContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // === FILA SUPERIOR: DURACIÓN Y DESDE (INICIO) ===
        val filaSuperiror = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Duración con icono (izquierda)
        val duracionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val iconoClock = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                rightMargin = 8
            }
            setImageResource(R.drawable.ic_clock)
            setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        val duracionText = TextView(requireContext()).apply {
            text = recorrido.duracion
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        duracionContainer.addView(iconoClock)
        duracionContainer.addView(duracionText)

        // Coordenada de INICIO con icono (derecha)
        val inicioContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val iconoInicio = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                rightMargin = 8
            }
            setImageResource(R.drawable.ic_walk)
            setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        val inicioText = TextView(requireContext()).apply {
            text = if (recorrido.coordenadasInicio != null) {
                "${String.format("%.4f, %.4f", recorrido.coordenadasInicio.latitude, recorrido.coordenadasInicio.longitude)}"
            } else {
                "Sin coordenadas"
            }
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        inicioContainer.addView(iconoInicio)
        inicioContainer.addView(inicioText)

        // Agregar ambos a la fila superior
        filaSuperiror.addView(duracionContainer)
        filaSuperiror.addView(inicioContainer)

        // === FILA INFERIOR: COORDENADA DE FIN (centrada debajo de la imagen) ===
        val finContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val iconoFin = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                rightMargin = 8
            }
            setImageResource(R.drawable.ic_location)
            setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        val finText = TextView(requireContext()).apply {
            text = if (recorrido.coordenadasFin != null) {
                "${String.format("%.4f, %.4f", recorrido.coordenadasFin.latitude, recorrido.coordenadasFin.longitude)}"
            } else {
                "Sin coordenadas"
            }
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        finContainer.addView(iconoFin)
        finContainer.addView(finText)

        // === ENSAMBLAR LA INFORMACIÓN ===
        infoContainer.addView(filaSuperiror)
        infoContainer.addView(finContainer)

        cardContent.addView(imageView)
        cardContent.addView(infoContainer)
        cardView.addView(cardContent)

        mainContainer.addView(timelineContainer)
        mainContainer.addView(cardView)

        return mainContainer
    }
}