package com.example.fittrack.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fittrack.R
import androidx.fragment.app.Fragment

class RecorridoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recorrido, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aquí puedes agregar la lógica para cargar los datos del recorrido
        // Por ejemplo, desde una base de datos o API
        setupRecorridoData()
    }

    private fun setupRecorridoData() {
        // Ejemplo de cómo podrías cargar datos dinámicamente
        // Puedes crear una clase de datos para representar cada recorrido

        // data class Recorrido(
        //     val hora: String,
        //     val duracion: String,
        //     val origen: String,
        //     val destino: String,
        //     val imagen: Int
        // )

        // Y luego crear una lista de recorridos y poblar dinámicamente
        // el RecyclerView en lugar de usar un layout estático
    }
}