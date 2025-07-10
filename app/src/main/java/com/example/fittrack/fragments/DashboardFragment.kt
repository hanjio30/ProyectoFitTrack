package com.example.fittrack.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fittrack.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    data class UserStats(
        val calories: Int = 0,
        val dailyActivityMinutes: Int = 0,
        val todaySteps: Int = 0,
        val hydrationLiters: Double = 0.0,
        val activeStreak: Int = 0,
        val totalDistanceKm: Double = 0.0,
        val dailyGoalProgress: Int = 0
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("user_stats")

        setupView()
        loadUserStats()
    }

    private fun setupView() {
        // Configurar el saludo personalizado
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 0..11 -> "¡BUENOS DÍAS USUARIO!"
            in 12..17 -> "¡BUENAS TARDES USUARIO!"
            else -> "¡BUENAS NOCHES USUARIO!"
        }

        binding.tvGreeting.text = greeting
        binding.tvSubtitle.text = "Sigue avanzando hacia tu mejor versión."

        // Configurar clicks en las cards
        binding.cardCalories.setOnClickListener {
            // TODO: Navegar a detalles de calorías
        }

        binding.cardActivity.setOnClickListener {
            // TODO: Navegar a detalles de actividad
        }

        binding.cardSteps.setOnClickListener {
            // TODO: Navegar a detalles de pasos
        }

        binding.cardHydration.setOnClickListener {
            // TODO: Navegar a detalles de hidratación
        }

        binding.cardStreak.setOnClickListener {
            // TODO: Navegar a detalles de racha
        }

        binding.cardDistance.setOnClickListener {
            // TODO: Navegar a detalles de distancia
        }

        binding.cardGoal.setOnClickListener {
            // TODO: Navegar a detalles de meta
        }
    }

    private fun loadUserStats() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            databaseReference.child(currentUser.uid).child(today)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val stats = snapshot.getValue(UserStats::class.java)
                            stats?.let { updateUI(it) }
                        } else {
                            // Si no hay datos para hoy, mostrar valores por defecto
                            updateUI(UserStats())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // En caso de error, mostrar valores por defecto
                        updateUI(UserStats())
                    }
                })
        } else {
            // Si no hay usuario logueado, mostrar valores por defecto
            updateUI(UserStats())
        }
    }

    private fun updateUI(stats: UserStats) {
        // Actualizar calorías
        binding.tvCaloriesValue.text = "${stats.calories}"
        binding.tvCaloriesLabel.text = "Calorías"

        // Actualizar actividad física diaria
        binding.tvActivityValue.text = "${stats.dailyActivityMinutes} min"
        binding.tvActivityLabel.text = "Actividad física diaria"

        // Actualizar pasos de hoy
        binding.tvStepsValue.text = "${stats.todaySteps}"
        binding.tvStepsLabel.text = "Pasos de hoy"

        // Actualizar hidratación
        binding.tvHydrationValue.text = "${stats.hydrationLiters} lit."
        binding.tvHydrationLabel.text = "Hidratación"

        // Actualizar racha activa
        binding.tvStreakValue.text = "${stats.activeStreak} días"
        binding.tvStreakLabel.text = "Racha Activa"

        // Actualizar distancia recorrida
        binding.tvDistanceValue.text = "${stats.totalDistanceKm.toInt()} km"
        binding.tvDistanceLabel.text = "Distancia recorrida"

        // Actualizar meta diaria
        binding.tvGoalValue.text = "${stats.dailyGoalProgress}%"
        binding.tvGoalLabel.text = "Meta Diaria"

        // Actualizar el progreso de la meta
        binding.progressGoal.progress = stats.dailyGoalProgress
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}