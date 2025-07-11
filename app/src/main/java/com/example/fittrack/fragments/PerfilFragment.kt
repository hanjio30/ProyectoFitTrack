package com.example.fittrack.fragments
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.fittrack.R
import de.hdodenhof.circleimageview.CircleImageView


class PerfilFragment : Fragment() {

    private lateinit var ivProfilePhoto: CircleImageView
    private lateinit var btnEditPhoto: ImageView
    private lateinit var spinnerGender: Spinner
    private lateinit var etAge: EditText
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var btnSave: Button

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(ivProfilePhoto)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        initViews(view)
        setupGenderSpinner()
        setupListeners()

        return view
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
        val genderOptions = arrayOf("Masculino", "Femenino", "Otro")
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
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveProfile() {
        val gender = spinnerGender.selectedItem.toString()
        val age = etAge.text.toString()
        val weight = etWeight.text.toString()
        val height = etHeight.text.toString()

        // Validar campos
        if (age.isEmpty() || weight.isEmpty() || height.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Aquí puedes agregar la lógica para guardar el perfil
        // Por ejemplo: guardar en SharedPreferences, base de datos, etc.

        Toast.makeText(requireContext(), "Perfil guardado exitosamente", Toast.LENGTH_SHORT).show()
    }
}