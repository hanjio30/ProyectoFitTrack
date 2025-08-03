package com.example.fittrack.View.ui.fragments

import android.app.Dialog
import android.content.Intent
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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fittrack.R
import com.example.fittrack.View.ui.activities.ContentActivity
import com.example.fittrack.ViewModel.MetaDiariaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MetaDiariaFragment : Fragment() {

    private lateinit var viewModel: MetaDiariaViewModel
    private lateinit var auth: FirebaseAuth

    // Views principales
    private var tvProgressValue: TextView? = null
    private var tvProgressTotal: TextView? = null
    private var tvProgressPercent: TextView? = null
    private var tvGoalMessage: TextView? = null
    private var tvPointsTotal: TextView? = null
    private var ivBackArrow: ImageView? = null
    private var tvCardTitle: TextView? = null
    private var ivEditIcon: ImageView? = null

    // Views dinámicas
    private var cardCongrats: CardView? = null
    private var cardPoints: CardView? = null
    private var cardShare: CardView? = null
    private var tvCongratsText: TextView? = null
    private var btnShare: Button? = null

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

            hideContentActivityNavigation()
            initializeFirebase()
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

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
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
            // Views principales
            tvProgressValue = view.findViewById(R.id.tv_progress_value)
            tvProgressTotal = view.findViewById(R.id.tv_progress_total)
            tvProgressPercent = view.findViewById(R.id.tv_progress_percent)
            tvGoalMessage = view.findViewById(R.id.tv_goal_message)
            tvPointsTotal = view.findViewById(R.id.tv_points_total)

            // Header
            ivBackArrow = view.findViewById(R.id.ivBackArrow)
            tvCardTitle = view.findViewById(R.id.tvCardTitle)
            ivEditIcon = view.findViewById(R.id.iv_edit_icon)

            // Cards dinámicas
            cardCongrats = view.findViewById(R.id.card_congrats)
            cardPoints = view.findViewById(R.id.card_points)
            cardShare = view.findViewById(R.id.card_share)
            tvCongratsText = view.findViewById(R.id.tv_congrats_text)
            btnShare = view.findViewById(R.id.btn_share)

            Log.d(TAG, "Vistas inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
        }
    }

    private fun setupHeader() {
        try {
            tvCardTitle?.text = "Meta Diaria"

            ivBackArrow?.setOnClickListener {
                Log.d(TAG, "Click en flecha detectado!")
                try {
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e(TAG, "Error en popBackStack: ${e.message}", e)
                }
            }

            Log.d(TAG, "Header configurado")

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar header: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        try {
            // Click en el ícono de editar
            ivEditIcon?.setOnClickListener {
                Log.d(TAG, "Click en ícono de editar detectado!")
                showEditGoalPopup()
            }

            // Click en botón compartir
            btnShare?.setOnClickListener {
                Log.d(TAG, "Click en botón compartir detectado!")
                compartirLogro()
            }

            Log.d(TAG, "Click listeners configurados")

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar click listeners: ${e.message}", e)
        }
    }

    // En MetaDiariaFragment.kt - REEMPLAZA la función setupObservers()

    private fun setupObservers() {
        try {
            // Observers básicos CON LOGS
            viewModel.progresoActual.observe(viewLifecycleOwner) { progreso ->
                Log.d(TAG, "Observer - Progreso recibido: $progreso")
                tvProgressValue?.text = String.format("%.1f", progreso)
            }

            viewModel.metaDiaria.observe(viewLifecycleOwner) { meta ->
                Log.d(TAG, "Observer - Meta recibida: $meta")
                tvProgressTotal?.text = "/${String.format("%.1f", meta)} km"
            }

            viewModel.porcentajeCompletado.observe(viewLifecycleOwner) { porcentaje ->
                Log.d(TAG, "Observer - Porcentaje recibido: $porcentaje")
                tvProgressPercent?.text = "$porcentaje% Completado"
            }

            viewModel.mensajeMeta.observe(viewLifecycleOwner) { mensaje ->
                Log.d(TAG, "Observer - Mensaje recibido: $mensaje")
                tvGoalMessage?.text = mensaje
            }

            viewModel.puntosGanados.observe(viewLifecycleOwner) { puntos ->
                Log.d(TAG, "Observer - Puntos recibidos: $puntos")
                tvPointsTotal?.text = "+$puntos"
            }

            // ✨ OBSERVERS PARA VISUALIZACIÓN DINÁMICA CON LOGS
            viewModel.mostrarFelicitaciones.observe(viewLifecycleOwner) { mostrar ->
                Log.d(TAG, "Observer - Mostrar felicitaciones: $mostrar")
                cardCongrats?.visibility = if (mostrar) View.VISIBLE else View.GONE
                Log.d(TAG, "CardCongrats visibility set to: ${if (mostrar) "VISIBLE" else "GONE"}")
            }

            viewModel.mostrarPuntos.observe(viewLifecycleOwner) { mostrar ->
                Log.d(TAG, "Observer - Mostrar puntos: $mostrar")
                cardPoints?.visibility = if (mostrar) View.VISIBLE else View.GONE
                Log.d(TAG, "CardPoints visibility set to: ${if (mostrar) "VISIBLE" else "GONE"}")
            }

            viewModel.mostrarCompartir.observe(viewLifecycleOwner) { mostrar ->
                Log.d(TAG, "Observer - Mostrar compartir: $mostrar")
                cardShare?.visibility = if (mostrar) View.VISIBLE else View.GONE
                Log.d(TAG, "CardShare visibility set to: ${if (mostrar) "VISIBLE" else "GONE"}")
            }

            viewModel.mensajeFelicitaciones.observe(viewLifecycleOwner) { mensaje ->
                Log.d(TAG, "Observer - Mensaje felicitaciones: $mensaje")
                tvCongratsText?.text = mensaje
            }

            viewModel.tipoLogro.observe(viewLifecycleOwner) { tipo ->
                Log.d(TAG, "Observer - Tipo logro: $tipo")
                actualizarTemaSegunLogro(tipo)
            }

            // Observers existentes
            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                if (!error.isNullOrEmpty()) {
                    Log.e(TAG, "Error del ViewModel: $error")
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }

            viewModel.metaActualizada.observe(viewLifecycleOwner) { actualizada ->
                if (actualizada) {
                    Toast.makeText(context, "Meta actualizada correctamente", Toast.LENGTH_SHORT).show()
                    viewModel.clearMetaActualizadaFlag()
                }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                Log.d(TAG, "Loading state: $isLoading")
            }

            Log.d(TAG, "Todos los observers configurados correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar observers: ${e.message}", e)
        }
    }

    private fun actualizarTemaSegunLogro(tipo: MetaDiariaViewModel.TipoLogro) {
        try {
            when (tipo) {
                MetaDiariaViewModel.TipoLogro.META_COMPLETADA -> {
                    // Cambiar colores para celebración
                    cardCongrats?.setCardBackgroundColor(
                        resources.getColor(R.color.bt_main, null)
                    )
                }
                MetaDiariaViewModel.TipoLogro.CASI_COMPLETA -> {
                    cardCongrats?.setCardBackgroundColor(
                        resources.getColor(R.color.card_purple_light, null)
                    )
                }
                MetaDiariaViewModel.TipoLogro.MEDIO_CAMINO -> {
                    cardCongrats?.setCardBackgroundColor(
                        resources.getColor(R.color.card_blue_light, null)
                    )
                }
                else -> {
                    // Tema por defecto
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar tema: ${e.message}", e)
        }
    }

    private fun compartirLogro() {
        try {
            val datosCompartir = viewModel.datosParaCompartir.value

            if (datosCompartir != null) {
                val mensaje = datosCompartir.mensajePersonalizado

                // Mostrar opciones de compartir
                mostrarOpcionesCompartir(mensaje)
            } else {
                Toast.makeText(context, "No hay datos para compartir", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir: ${e.message}", e)
        }
    }

    private fun mostrarOpcionesCompartir(mensaje: String) {
        try {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.popup_compartir_logro)

            // Buscar vistas del popup
            val btnWhatsApp = dialog.findViewById<Button>(R.id.btn_whatsapp)
            val btnFacebook = dialog.findViewById<Button>(R.id.btn_facebook)
            val btnTwitter = dialog.findViewById<Button>(R.id.btn_twitter)
            val btnInstagram = dialog.findViewById<Button>(R.id.btn_instagram)
            val btnOtros = dialog.findViewById<Button>(R.id.btn_otros)
            val btnCancelar = dialog.findViewById<Button>(R.id.btn_cancelar_compartir)

            // Verificar y ocultar botones de apps no instaladas
            if (!isAppInstalled("com.whatsapp") && !isAppInstalled("com.whatsapp.w4b")) {
                btnWhatsApp?.visibility = View.GONE
            }

            if (!isAppInstalled("com.facebook.katana") && !isAppInstalled("com.facebook.lite")) {
                btnFacebook?.visibility = View.GONE
            }

            if (!isAppInstalled("com.twitter.android") && !isAppInstalled("com.x.android")) {
                btnTwitter?.visibility = View.GONE
            }

            if (!isAppInstalled("com.instagram.android") && !isAppInstalled("com.instagram.lite")) {
                btnInstagram?.visibility = View.GONE
            }

            // Configurar clicks
            btnWhatsApp?.setOnClickListener {
                compartirEnWhatsApp(mensaje)
                dialog.dismiss()
            }

            btnFacebook?.setOnClickListener {
                compartirEnFacebook(mensaje)
                dialog.dismiss()
            }

            btnTwitter?.setOnClickListener {
                compartirEnTwitter(mensaje)
                dialog.dismiss()
            }

            btnInstagram?.setOnClickListener {
                compartirEnInstagram(mensaje)
                dialog.dismiss()
            }

            btnOtros?.setOnClickListener {
                compartirGenerico(mensaje)
                dialog.dismiss()
            }

            btnCancelar?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar opciones de compartir: ${e.message}", e)
            // Fallback: compartir genérico
            compartirGenerico(mensaje)
        }
    }

    private fun compartirEnWhatsApp(mensaje: String) {
        try {
            // Primero intentar WhatsApp normal
            var intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensaje)
                setPackage("com.whatsapp")
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
                return
            }

            // Si no funciona, intentar WhatsApp Business
            intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensaje)
                setPackage("com.whatsapp.w4b")
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
                return
            }

            // Si ninguno funciona, usar genérico
            Toast.makeText(context, "WhatsApp no está disponible", Toast.LENGTH_SHORT).show()
            compartirGenerico(mensaje)

        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir en WhatsApp: ${e.message}", e)
            compartirGenerico(mensaje)
        }
    }

    private fun compartirEnFacebook(mensaje: String) {
        try {
            // Lista de posibles paquetes de Facebook
            val facebookPackages = listOf(
                "com.facebook.katana",      // Facebook principal
                "com.facebook.lite",        // Facebook Lite
                "com.facebook.orca"         // Messenger
            )

            for (packageName in facebookPackages) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensaje)
                    setPackage(packageName)
                }

                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                    return
                }
            }

            // Si no encuentra ninguno, usar genérico
            Toast.makeText(context, "Facebook no está disponible", Toast.LENGTH_SHORT).show()
            compartirGenerico(mensaje)

        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir en Facebook: ${e.message}", e)
            compartirGenerico(mensaje)
        }
    }

    private fun compartirEnTwitter(mensaje: String) {
        try {
            // Twitter ahora es X, intentar ambos
            val twitterPackages = listOf(
                "com.twitter.android",     // Twitter viejo
                "com.x.android"           // X (nuevo Twitter)
            )

            for (packageName in twitterPackages) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensaje)
                    setPackage(packageName)
                }

                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                    return
                }
            }

            Toast.makeText(context, "Twitter/X no está disponible", Toast.LENGTH_SHORT).show()
            compartirGenerico(mensaje)

        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir en Twitter: ${e.message}", e)
            compartirGenerico(mensaje)
        }
    }

    private fun compartirEnInstagram(mensaje: String) {
        try {
            // Copiar al portapapeles primero
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Logro FitTrack", mensaje)
            clipboard.setPrimaryClip(clip)

            val instagramPackages = listOf(
                "com.instagram.android",   // Instagram principal
                "com.instagram.lite"       // Instagram Lite
            )

            for (packageName in instagramPackages) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensaje)
                    setPackage(packageName)
                }

                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    Toast.makeText(context, "Texto copiado. Pégalo en tu historia de Instagram", Toast.LENGTH_LONG).show()
                    startActivity(intent)
                    return
                }
            }

            Toast.makeText(context, "Instagram no está disponible", Toast.LENGTH_SHORT).show()
            compartirGenerico(mensaje)

        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir en Instagram: ${e.message}", e)
            compartirGenerico(mensaje)
        }
    }

    private fun compartirGenerico(mensaje: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensaje)
                putExtra(Intent.EXTRA_SUBJECT, "Mi logro en FitTrack")
            }

            val chooser = Intent.createChooser(intent, "Compartir logro")
            startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir genérico: ${e.message}", e)
            Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }


    private fun loadData() {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userId = currentUser.uid
                Log.d(TAG, "Cargando datos para usuario: $userId")
                viewModel.loadGoalData(userId)
            } else {
                Log.e(TAG, "Usuario no autenticado")
                Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
        }
    }

    private fun showEditGoalPopup() {
        try {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.popup_meta_diaria)

            val etMetaKilometros = dialog.findViewById<EditText>(R.id.et_meta_kilometros)
            val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
            val btnSave = dialog.findViewById<Button>(R.id.btn_save)

            viewModel.metaDiaria.value?.let { metaActual ->
                etMetaKilometros.setText(String.format("%.1f", metaActual))
            }

            btnCancel.setOnClickListener {
                Log.d(TAG, "Popup cancelado")
                dialog.dismiss()
            }

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

    private fun hideContentActivityNavigation() {
        try {
            (activity as? ContentActivity)?.let { contentActivity ->
                val headerLayout = contentActivity.findViewById<LinearLayout>(R.id.headerLayout)
                headerLayout?.visibility = View.GONE

                val bottomNav = contentActivity.findViewById<BottomNavigationView>(R.id.bottomNavigationMain)
                bottomNav?.visibility = View.GONE

                Log.d(TAG, "Elementos de navegación del ContentActivity ocultados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar navegación: ${e.message}", e)
        }
    }

    private fun showContentActivityNavigation() {
        try {
            (activity as? ContentActivity)?.let { contentActivity ->
                val headerLayout = contentActivity.findViewById<LinearLayout>(R.id.headerLayout)
                headerLayout?.visibility = View.VISIBLE

                val bottomNav = contentActivity.findViewById<BottomNavigationView>(R.id.bottomNavigationMain)
                bottomNav?.visibility = View.VISIBLE

                Log.d(TAG, "Elementos de navegación del ContentActivity restaurados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar navegación: ${e.message}", e)
        }
    }

    fun refreshMetaData() {
        try {
            Log.d(TAG, "Refrescando datos de meta diaria...")
            viewModel.refreshData()
        } catch (e: Exception) {
            Log.e(TAG, "Error al refrescar datos: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            Log.d(TAG, "MetaDiariaFragment onDestroyView")
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
            cardCongrats = null
            cardPoints = null
            cardShare = null
            tvCongratsText = null
            btnShare = null

        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroyView: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MetaDiariaFragment onResume")

        try {
            hideContentActivityNavigation()
            loadData()
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }
}