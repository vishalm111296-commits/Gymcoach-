package com.gymcoach.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal inline fun <T, R> Flow<List<T>>.mapList(crossinline transform: (T) -> R): Flow<List<R>> =
    map { list -> list.map { transform(it) } }
