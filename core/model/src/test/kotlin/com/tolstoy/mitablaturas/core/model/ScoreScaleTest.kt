package com.tolstoy.mitablaturas.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreScaleTest {

    @Test
    fun `avanza en decimas sin errores de coma flotante`() {
        assertEquals(1.0f, ScoreScale.step(0.9f, 1))
        assertEquals(0.8f, ScoreScale.step(0.9f, -1))
        assertEquals(1.3f, ScoreScale.step(ScoreScale.step(ScoreScale.step(1.0f, 1), 1), 1))
    }

    @Test
    fun `no pasa de los limites`() {
        assertEquals(ScoreScale.MAX, ScoreScale.step(ScoreScale.MAX, 1))
        assertEquals(ScoreScale.MIN, ScoreScale.step(ScoreScale.MIN, -1))
        assertEquals(ScoreScale.MAX, ScoreScale.normalize(7f))
        assertEquals(ScoreScale.DEFAULT, ScoreScale.normalize(Float.NaN))
    }
}
