/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.core

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

public typealias Try<T> = Either<T, Throwable>

public inline fun <T> Try(block: () -> T): Try<T> {
    return try {
        block().toLeft()
    } catch (t: Throwable) {
        t.toRight()
    }
}

@OptIn(ExperimentalContracts::class)
public fun <T> Try<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is Left<T>)
        returns(false) implies (this@isSuccess is Right<Throwable>)
    }
    return this is Left
}

@OptIn(ExperimentalContracts::class)
public fun <T> Try<T>.isFailure(): Boolean {
    contract {
        returns(true) implies (this@isFailure is Right<Throwable>)
        returns(false) implies (this@isFailure is Left<T>)
    }
    return this is Right
}

public fun <T> Try<T>.getOrThrow(): T = when (this) {
    is Left<T> -> value
    is Right<Throwable> -> throw value
}

public fun <T> Try<T>.getOrNull(): T? = when (this) {
    is Left<T> -> value
    is Right<Throwable> -> null
}

public fun <T> Try<T>.exceptionOrNull(): Throwable? = when (this) {
    is Left<T> -> null
    is Right<Throwable> -> value
}

public val Right<Throwable>.exception: Throwable
    get() = value

public fun <T> Result<T>.toTry(): Try<T> {
    return fold(
        onSuccess = { it.toLeft() },
        onFailure = { it.toRight() }
    )
}

public fun <T> Try<T>.toResult(): Result<T> {
    return when (this) {
        is Left<T> -> Result.success(value)
        is Right -> Result.failure(exception)
    }
}

public fun <T> Try<Try<T>>.flatten(): Try<T> {
    return when (this) {
        is Left<Try<T>> -> value
        is Right<Throwable> -> this
    }
}
