package com.example.fittrack.view.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.navigation.NavController
import com.example.fittrack.R
import com.example.fittrack.databinding.HeaderCardBinding

class HeaderCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: HeaderCardBinding
    private var onBackClick: (() -> Unit)? = null

    init {
        val inflater = LayoutInflater.from(context)
        binding = HeaderCardBinding.inflate(inflater, this, true)

        binding.ivBackArrow.setOnClickListener {
            android.util.Log.d("HeaderCardView", "Click detectado")
            onBackClick?.invoke()
        }
    }

    fun setTitle(title: String) {
        binding.tvCardTitle.text = title
    }

    fun setOnBackClickListener(listener: () -> Unit) {
        this.onBackClick = listener
        android.util.Log.d("HeaderCardView", "Listener configurado")
    }
}