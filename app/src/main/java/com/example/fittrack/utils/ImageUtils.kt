package com.example.fittrack.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat

object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * Método mejorado para aplicar imagen circular desde Bitmap
     */
    fun makeImageCircular(imageView: ImageView, bitmap: Bitmap) {
        try {
            Log.d(TAG, "makeImageCircular - Bitmap: ${bitmap.width}x${bitmap.height}")

            // Crear bitmap circular
            val circularBitmap = getCircularBitmap(bitmap)
            if (circularBitmap != null) {
                imageView.setImageBitmap(circularBitmap)
                Log.d(TAG, "Imagen circular aplicada exitosamente")
            } else {
                Log.w(TAG, "Error al crear bitmap circular")
                imageView.setImageBitmap(bitmap) // Fallback a imagen original
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en makeImageCircular con Bitmap: ${e.message}", e)
            imageView.setImageBitmap(bitmap) // Fallback
        }
    }

    /**
     * Método para aplicar imagen circular desde recurso drawable
     */
    fun makeImageCircular(imageView: ImageView, drawableRes: Int) {
        try {
            Log.d(TAG, "makeImageCircular - Drawable resource: $drawableRes")

            val context = imageView.context
            val drawable = ContextCompat.getDrawable(context, drawableRes)

            if (drawable != null) {
                // Convertir drawable a bitmap
                val bitmap = drawableToBitmap(drawable)
                if (bitmap != null) {
                    makeImageCircular(imageView, bitmap)
                } else {
                    Log.w(TAG, "Error al convertir drawable a bitmap")
                    imageView.setImageResource(drawableRes) // Fallback
                }
            } else {
                Log.w(TAG, "Drawable no encontrado para resource: $drawableRes")
                imageView.setImageResource(drawableRes) // Fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en makeImageCircular con drawable: ${e.message}", e)
            imageView.setImageResource(drawableRes) // Fallback
        }
    }

    /**
     * Convierte un Drawable a Bitmap
     */
    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir drawable a bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Crea un bitmap circular a partir de un bitmap original
     */
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap? {
        return try {
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2

            // Crear bitmap cuadrado centrado
            val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)

            // Crear bitmap circular
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            val rect = Rect(0, 0, size, size)
            val rectF = RectF(rect)
            val radius = size / 2f

            // Dibujar círculo
            canvas.drawOval(rectF, paint)

            // Aplicar modo de mezcla para crear máscara circular
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(squaredBitmap, rect, rect, paint)

            Log.d(TAG, "Bitmap circular creado exitosamente - Tamaño: ${size}x${size}")
            output

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear bitmap circular: ${e.message}", e)
            null
        }
    }

    /**
     * Redimensiona un bitmap manteniendo la proporción
     */
    fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        return try {
            val width = bitmap.width
            val height = bitmap.height

            if (width <= maxSize && height <= maxSize) {
                return bitmap
            }

            val ratio = minOf(
                maxSize.toFloat() / width,
                maxSize.toFloat() / height
            )

            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()

            val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            Log.d(TAG, "Bitmap redimensionado de ${width}x${height} a ${newWidth}x${newHeight}")

            resized
        } catch (e: Exception) {
            Log.e(TAG, "Error al redimensionar bitmap: ${e.message}", e)
            bitmap // Retornar original en caso de error
        }
    }
}