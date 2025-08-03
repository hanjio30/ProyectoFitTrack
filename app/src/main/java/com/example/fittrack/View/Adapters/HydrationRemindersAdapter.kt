package com.example.fittrack.View.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import android.widget.Toast
import com.example.fittrack.ViewModel.HidratacionViewModel
import com.google.android.material.button.MaterialButton

// Actualiza tu HydrationRemindersAdapter con estos cambios:

class HydrationRemindersAdapter(
    private var reminders: List<HidratacionViewModel.RecordatorioHidratacion> = emptyList(),
    private val onReminderCompleted: (Int) -> Unit,
    private val viewModel: HidratacionViewModel // Agregar referencia al ViewModel
) : RecyclerView.Adapter<HydrationRemindersAdapter.ReminderViewHolder>() {

    companion object {
        private const val TAG = "HydrationAdapter"
    }

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val statusIndicator: View = itemView.findViewById(R.id.v_status_indicator)
        val timeRange: TextView = itemView.findViewById(R.id.tv_time_range)
        val description: TextView = itemView.findViewById(R.id.tv_description)
        val drinkButton: MaterialButton = itemView.findViewById(R.id.btn_drink_water)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hydration_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]

        try {
            // Configurar tiempo - mostrar hora
            holder.timeRange.text = reminder.hora

            // Configurar descripción - mostrar cantidad y descripción
            holder.description.text = "${reminder.cantidad} - ${reminder.descripcion}"

            // Verificar si el recordatorio debe estar habilitado
            val isEnabled = viewModel.isReminderEnabled(reminder)

            Log.d(TAG, "Recordatorio ${reminder.id}: Completado=${reminder.completado}, Habilitado=$isEnabled")

            // Configurar estado visual
            when {
                reminder.completado -> {
                    // Recordatorio completado
                    configurarEstadoCompletado(holder)
                    Log.d(TAG, "Recordatorio ${reminder.id} mostrado como completado")
                }

                isEnabled -> {
                    // Recordatorio activo y en horario
                    configurarEstadoActivo(holder)
                    Log.d(TAG, "Recordatorio ${reminder.id} mostrado como activo")
                }

                else -> {
                    // Recordatorio fuera de horario
                    configurarEstadoInactivo(holder)
                    Log.d(TAG, "Recordatorio ${reminder.id} mostrado como inactivo (Bloqueado)")
                }
            }

            // Configurar click del botón
            holder.drinkButton.setOnClickListener {
                when {
                    reminder.completado -> {
                        Log.d(TAG, "Recordatorio ${reminder.id} ya está completado")
                    }

                    !isEnabled -> {
                        Log.d(TAG, "Recordatorio ${reminder.id} no está en horario activo")
                        // Opcional: mostrar mensaje al usuario
                        Toast.makeText(holder.itemView.context,
                            "Este recordatorio no está disponible en este horario",
                            Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        Log.d(TAG, "Usuario completando recordatorio: ${reminder.id} (${reminder.cantidad})")

                        // Inmediatamente deshabilitar el botón
                        holder.drinkButton.isEnabled = false
                        holder.drinkButton.text = "Procesando..."

                        // Ejecutar callback
                        onReminderCompleted(reminder.id)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar item $position: ${e.message}", e)
            configurarEstadoError(holder)
        }
    }

    private fun configurarEstadoCompletado(holder: ReminderViewHolder) {
        // Indicador verde
        holder.statusIndicator.background = ContextCompat.getDrawable(
            holder.itemView.context,
            R.drawable.circle_green
        )

        // Botón deshabilitado
        holder.drinkButton.text = "✓ Completado"
        holder.drinkButton.isEnabled = false
        holder.drinkButton.backgroundTintList = ContextCompat.getColorStateList(
            holder.itemView.context,
            android.R.color.darker_gray
        )
        holder.drinkButton.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
        )

        // Reducir opacidad del texto
        holder.timeRange.alpha = 0.6f
        holder.description.alpha = 0.6f
    }

    private fun configurarEstadoActivo(holder: ReminderViewHolder) {
        // Indicador azul (activo)
        holder.statusIndicator.background = ContextCompat.getDrawable(
            holder.itemView.context,
            R.drawable.circle_blue // Necesitarás crear este drawable
        )

        // Botón habilitado
        holder.drinkButton.text = "Tomé agua"
        holder.drinkButton.isEnabled = true
        holder.drinkButton.backgroundTintList = ContextCompat.getColorStateList(
            holder.itemView.context,
            R.color.teal_700
        )
        holder.drinkButton.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )

        // Opacidad normal
        holder.timeRange.alpha = 1.0f
        holder.description.alpha = 1.0f
    }

    private fun configurarEstadoInactivo(holder: ReminderViewHolder) {
        // Indicador gris
        holder.statusIndicator.background = ContextCompat.getDrawable(
            holder.itemView.context,
            R.drawable.circle_gray
        )

        // Botón deshabilitado temporalmente
        holder.drinkButton.text = "Bloqueado"
        holder.drinkButton.isEnabled = false
        holder.drinkButton.backgroundTintList = ContextCompat.getColorStateList(
            holder.itemView.context,
            android.R.color.darker_gray
        )
        holder.drinkButton.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
        )

        // Reducir opacidad
        holder.timeRange.alpha = 0.7f
        holder.description.alpha = 0.7f
    }

    private fun configurarEstadoError(holder: ReminderViewHolder) {
        holder.timeRange.text = "Error"
        holder.description.text = "Error al cargar datos"
        holder.drinkButton.isEnabled = false
        holder.drinkButton.text = "Error"
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${reminders.size} recordatorios")
        return reminders.size
    }

    fun updateReminders(newReminders: List<HidratacionViewModel.RecordatorioHidratacion>) {
        Log.d(TAG, "Actualizando recordatorios: ${newReminders.size} items")
        reminders = newReminders
        notifyDataSetChanged()
        Log.d(TAG, "Recordatorios actualizados exitosamente")
    }
}

