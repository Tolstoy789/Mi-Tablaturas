package com.tolstoy.mitablaturas.core.data.di

import javax.inject.Qualifier

/** Calificador para inyectar el dispatcher de E/S y poder reemplazarlo en tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
