package com.tolstoy.mitablaturas.core.model

import kotlin.math.roundToInt

/** Tamaño de la partitura (la escala de AlphaTab). Se guarda entre sesiones. */
object ScoreScale {
    const val DEFAULT = 0.9f
    const val MIN = 0.6f
    const val MAX = 2.0f
    const val STEP = 0.1f

    /** Valor válido más cercano a [scale], redondeado a décimas para comparar sin errores de coma flotante. */
    fun normalize(scale: Float): Float {
        val valid = if (scale.isNaN()) DEFAULT else scale.coerceIn(MIN, MAX)
        return (valid * 10).roundToInt() / 10f
    }

    /** Escala tras [steps] pasos de [STEP] (negativos para achicar), dentro de los límites. */
    fun step(scale: Float, steps: Int): Float = normalize(scale + steps * STEP)
}
