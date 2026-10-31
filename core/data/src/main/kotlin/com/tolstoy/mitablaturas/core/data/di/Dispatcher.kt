package com.tolstoy.mitablaturas.core.data.di

import javax.inject.Qualifier

/** Calificador para inyectar el dispatcher de E/S y poder reemplazarlo en tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Ámbito de corrutinas que vive tanto como la app, para trabajos que deben terminar aunque
 * se cierre la pantalla que los lanzó (p. ej. guardar los ajustes al salir del visor).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
