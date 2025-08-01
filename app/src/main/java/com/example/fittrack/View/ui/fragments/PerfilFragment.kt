package com.example.fittrack.View.ui.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.fittrack.R
import com.example.fittrack.ViewModel.ContentViewModel
import com.example.fittrack.ViewModel.PerfilViewModel
import com.example.fittrack.model.UserProfile
import de.hdodenhof.circleimageview.CircleImageView

class PerfilFragment : Fragment() {

    private lateinit var ivProfilePhoto: CircleImageView
    private lateinit var btnEditPhoto: ImageView
    private lateinit var spinnerGender: Spinner
    private lateinit var etAge: EditText
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var btnSave: Button

    private lateinit var viewModel: PerfilViewModel
    // ✅ AGREGADO: Referencia al ContentViewModel para refrescar imagen
    private var contentViewModel: ContentViewModel? = null
    private var selectedImageUri: Uri? = null

    companion object {
        private const val TAG = "PerfilFragment"
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                selectedImageUri = uri
                Log.d(TAG, "Imagen seleccionada: $uri")
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .into(ivProfilePhoto)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView")
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        viewModel = ViewModelProvider(this)[PerfilViewModel::class.java]

        // ✅ OBTENER ContentViewModel de la Activity
        try {
            contentViewModel = ViewModelProvider(requireActivity())[ContentViewModel::class.java]
            Log.d(TAG, "ContentViewModel obtenido exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ContentViewModel: ${e.message}", e)
        }

        initViews(view)
        setupGenderSpinner()
        setupListeners()
        observeViewModel()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated - Cargando datos del perfil")
        // ✅ CARGAR DATOS DEL PERFIL AL CREAR LA VISTA
        viewModel.loadUserProfile()
    }

    private fun initViews(view: View) {
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo)
        btnEditPhoto = view.findViewById(R.id.btn_edit_photo)
        spinnerGender = view.findViewById(R.id.spinner_gender)
        etAge = view.findViewById(R.id.et_age)
        etWeight = view.findViewById(R.id.et_weight)
        etHeight = view.findViewById(R.id.et_height)
        btnSave = view.findViewById(R.id.btn_save)
    }

    private fun setupGenderSpinner() {
        val genderOptions = arrayOf("Seleccionar", "Masculino", "Femenino", "Otro")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            genderOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter
    }

    private fun setupListeners() {
        btnEditPhoto.setOnClickListener {
            openImagePicker()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        // Listener para eliminar imagen (opcional - click largo)
        ivProfilePhoto.setOnLongClickListener {
            showDeleteImageDialog()
            true
        }
    }

    // ✅ OBSERVERS MEJORADOS
    private fun observeViewModel() {
        viewModel.profileData.observe(viewLifecycleOwner) { profile ->
            Log.d(TAG, "Datos del perfil recibidos: ${profile != null}")
            if (profile != null) {
                populateFields(profile)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            btnSave.isEnabled = !isLoading
            btnEditPhoto.isEnabled = !isLoading

            if (isLoading) {
                btnSave.text = "Guardando..."
            } else {
                btnSave.text = "Guardar"
            }
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                selectedImageUri = null // Limpiar la imagen seleccionada después de guardar

                // ✅ NOTIFICAR AL ContentViewModel QUE LA IMAGEN SE ACTUALIZÓ
                contentViewModel?.refreshProfileImage()
                Log.d(TAG, "Imagen guardada exitosamente - ContentViewModel notificado")
            }

            result.onFailure { exception ->
                val errorMessage = exception.message ?: "Error desconocido"
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ✅ MÉTODO MEJORADO para poblar campos
    private fun populateFields(profile: UserProfile) {
        try {
            Log.d(TAG, "Poblando campos con datos del perfil")

            // ✅ MEJORADO: Cargar imagen de perfil desde Base64
            if (!profile.profileImageBase64.isNullOrEmpty()) {
                try {
                    Log.d(TAG, "Cargando imagen desde Base64 - Tamaño: ${profile.profileImageBase64.length}")
                    val imageBytes = Base64.decode(profile.profileImageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        Log.d(TAG, "Bitmap creado exitosamente - Aplicando a ImageView")
                        ivProfilePhoto.setImageBitmap(bitmap)
                    } else {
                        Log.w(TAG, "Error al crear bitmap desde Base64")
                        ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al decodificar imagen Base64: ${e.message}", e)
                    ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder)
                }
            } else {
                Log.d(TAG, "No hay imagen Base64 - Mostrando placeholder")
                ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder)
            }

            // Cargar género
            val genderOptions = (spinnerGender.adapter as ArrayAdapter<String>)
            val genderPosition = when (profile.gender) {
                "Masculino" -> 1
                "Femenino" -> 2
                "Otro" -> 3
                else -> 0
            }
            spinnerGender.setSelection(genderPosition)

            // Cargar otros campos
            if (profile.age > 0) {
                etAge.setText(profile.age.toString())
            }

            if (profile.weight > 0) {
                etWeight.setText(profile.weight.toString())
            }

            if (profile.height > 0) {
                etHeight.setText(profile.height.toString())
            }

            Log.d(TAG, "Campos poblados exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al poblar campos: ${e.message}", e)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun saveProfile() {
        val selectedGender = spinnerGender.selectedItem.toString()
        val gender = if (selectedGender == "Seleccionar") "" else selectedGender
        val age = etAge.text.toString().trim()
        val weight = etWeight.text.toString().trim()
        val height = etHeight.text.toString().trim()

        // Validación básica en la UI
        if (gender.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor selecciona un género", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Guardando perfil - Género: $gender, URI: $selectedImageUri")
        viewModel.saveProfile(requireContext(), gender, age, weight, height, selectedImageUri)
    }

    private fun showDeleteImageDialog() {
        val currentProfile = viewModel.profileData.value
        if (currentProfile?.profileImageBase64?.isNotEmpty() == true) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar foto de perfil")
                .setMessage("¿Estás seguro de que quieres eliminar tu foto de perfil?")
                .setPositiveButton("Eliminar") { _, _ ->
                    Log.d(TAG, "Eliminando imagen de perfil")
                    viewModel.deleteProfileImage()
                    ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder)

                    // ✅ NOTIFICAR AL ContentViewModel QUE LA IMAGEN SE ELIMINÓ
                    contentViewModel?.refreshProfileImage()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    // ✅ AGREGADO: Refrescar imagen al mostrar el fragmento
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - Refrescando datos del perfil")
        viewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        selectedImageUri = null
        Log.d(TAG, "onDestroyView")
    }
}