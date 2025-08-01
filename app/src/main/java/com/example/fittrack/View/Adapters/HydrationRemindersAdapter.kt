package com.example.fittrack.View.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import com.example.fittrack.ViewModel.HidratacionViewModel
import com.google.android.material.button.MaterialButton

class HydrationRemindersAdapter(
    private var reminders: List<HidratacionViewModel.RecordatorioHidratacion> = emptyList(),
    private val onReminderCompleted: (Int) -> Unit // Callback cuando se completa un recordatorio
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
            // Configurar tiempo - mostrar hora y título
            holder.timeRange.text = "${reminder.hora} - ${reminder.titulo}"

            // Configurar descripción - mostrar cantidad y descripción
            holder.description.text = "${reminder.cantidad} - ${reminder.descripcion}"

            // Configurar estado visual según si está completado
            if (reminder.completado) {
                // Recordatorio completado
                holder.statusIndicator.background = ContextCompat.getDrawable(
                    holder.itemView.context,
                    R.drawable.circle_green // Necesitarás crear este drawable
                )
                holder.drinkButton.text = "✓ Completado"
                holder.drinkButton.isEnabled = false
                holder.drinkButton.backgroundTintList = ContextCompat.getColorStateList(
                    holder.itemView.context,
                    R.color.gray_light
                )

                // Opcional: cambiar la opacidad del texto
                holder.timeRange.alpha = 0.6f
                holder.description.alpha = 0.6f

            } else {
                // Recordatorio pendiente
                holder.statusIndicator.background = ContextCompat.getDrawable(
                    holder.itemView.context,
                    R.drawable.circle_gray
                )
                holder.drinkButton.text = "Tomé agua"
                holder.drinkButton.isEnabled = true
                holder.drinkButton.backgroundTintList = ContextCompat.getColorStateList(
                    holder.itemView.context,
                    R.color.teal_700
                )

                holder.timeRange.alpha = 1.0f
                holder.description.alpha = 1.0f
            }

            // Configurar click del botón solo si no está completado
            holder.drinkButton.setOnClickListener {
                if (!reminder.completado) {
                    Log.d(TAG, "Completando recordatorio: ${reminder.id}")
                    onReminderCompleted(reminder.id)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar item $position: ${e.message}", e)
        }
    }

    override fun getItemCount(): Int = reminders.size

    // Método para actualizar la lista de recordatorios
    fun updateReminders(newReminders: List<HidratacionViewModel.RecordatorioHidratacion>) {
        reminders = newReminders
        notifyDataSetChanged()
        Log.d(TAG, "Recordatorios actualizados: ${reminders.size} items")
    }

    // Método para actualizar un recordatorio específico
    fun updateReminder(reminderId: Int) {
        val position = reminders.indexOfFirst { it.id == reminderId }
        if (position != -1) {
            notifyItemChanged(position)
            Log.d(TAG, "Recordatorio $reminderId actualizado en posición $position")
        }
    }
}