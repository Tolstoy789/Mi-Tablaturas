package com.tolstoy.mitablaturas.core.model

/**
 * Direcciones de diseño que el usuario puede elegir.
 *
 * @property key identificador estable para guardar la preferencia y pasarlo al visor web.
 */
enum class AppDesign(val key: String) {
    /** Claro y sobrio; la partitura manda. */
    ATRIL("atril"),

    /** Oscuro y de alto contraste, para tocar con poca luz. */
    ESCENARIO("escenario"),

    /** Material You expresivo, orientado a practicar. */
    TALLER("taller");

    companion object {
        val DEFAULT = TALLER

        /** Devuelve el diseño para [key], o [DEFAULT] si no se reconoce (p. ej. un valor antiguo). */
        fun fromKey(key: String?): AppDesign = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
