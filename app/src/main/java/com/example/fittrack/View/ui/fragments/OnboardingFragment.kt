package com.example.fittrack.View.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fittrack.R
import com.example.fittrack.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = arguments?.getInt(ARG_POSITION) ?: 0
        setupContent(position)
    }

    private fun setupContent(position: Int) {
        when (position) {
            0 -> {
                binding.tvOnboardingTitle.text = "¡Hola! Estás a punto de empezar tu camino con FitTrack."
                binding.ivOnboardingImage.setImageResource(R.drawable.onboarding_image)
                binding.tvOnboardingDescription.text = "No se trata de ser perfecto, sino constante. Y tú ya diste el primer paso. ✨"
                binding.btnStartApp.visibility = View.GONE
            }
            1 -> {
                binding.tvOnboardingTitle.text = "Tu cuerpo es más fuerte de lo que crees."
                binding.ivOnboardingImage.setImageResource(R.drawable.onboarding_image)
                binding.tvOnboardingDescription.text = "Solo 30 minutos de movimiento al día pueden marcar una gran diferencia en tu salud."
                binding.btnStartApp.visibility = View.GONE
            }
            2 -> {
                binding.tvOnboardingTitle.text = "No se trata solo de moverse."
                binding.ivOnboardingImage.setImageResource(R.drawable.onboarding_image)
                binding.tvOnboardingDescription.text = "Dormir bien, hidratarte y cuidar tu mente también son parte del progreso. 🧠💧"
                binding.btnStartApp.visibility = View.GONE
            }
            3 -> {
                binding.tvOnboardingTitle.text = "Los pequeños hábitos construyen grandes resultados."
                binding.ivOnboardingImage.setImageResource(R.drawable.onboarding_image)
                binding.tvOnboardingDescription.text = "Registrar tu actividad te ayuda a mantenerte enfocado y motivado. 📈"
                binding.btnStartApp.visibility = View.GONE
            }
            4 -> {
                binding.tvOnboardingTitle.text = "Listo. Ahora es tu momento."
                binding.ivOnboardingImage.setImageResource(R.drawable.onboarding_image)
                binding.tvOnboardingDescription.text = "Vamos paso a paso... pero siempre hacia adelante. 💪\n¡FitTrack va contigo!"
                binding.btnStartApp.visibility = View.VISIBLE
                binding.btnStartApp.setOnClickListener {
                    (activity as? OnboardingListener)?.onOnboardingComplete()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface OnboardingListener {
        fun onOnboardingComplete()
    }
}