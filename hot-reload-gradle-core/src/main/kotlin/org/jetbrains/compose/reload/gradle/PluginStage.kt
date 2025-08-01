/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.gradle

import org.gradle.api.Project
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.core.update
import org.jetbrains.compose.reload.gradle.PluginStage.State.Finished
import org.jetbrains.compose.reload.gradle.PluginStage.State.Started
import java.util.concurrent.atomic.AtomicReference

@InternalHotReloadApi
enum class PluginStage {
    /**
     * The plugin was applied, but we're waiting for the Kotlin entities (aka Kotlin plugin)
     *  to start configuring Compose Hot Reload
     */
    PluginApplied,

    /**
     * The plugin is performing its eager configuration aka, configuration
     * which is not dependent on any user input
     */
    EagerConfiguration,

    /**
     * The plugin has waited for user input and is now performing configuration based
     * upon all entities up until now.
     */
    DeferredConfiguration;

    enum class State {
        Started, Finished
    }
}

@InternalHotReloadApi
data class PluginState(
    val stage: PluginStage, val state: PluginStage.State,
) : Comparable<PluginState> {
    override fun toString(): String {
        return "$stage ($state)"
    }

    override operator fun compareTo(other: PluginState): Int {
        if (this == other) return 0
        val stageComparison = stage.compareTo(other.stage)
        if (stageComparison != 0) return stageComparison

        return state.compareTo(other.state)
    }
}

private val Project.pluginState: AtomicReference<PluginState?> by lazyProjectProperty {
    AtomicReference<PluginState?>(null)
}

private val Project.pluginStateFutures by lazyProjectProperty {
    mutableMapOf<PluginState, CompletableFuture<PluginState>>()
}

@InternalHotReloadApi
inline fun Project.runStage(stage: PluginStage, action: () -> Unit = {}) {
    start(stage)
    action()
    finish(stage)
}

@InternalHotReloadApi
fun Project.start(stage: PluginStage) {
    val (previous, new) = pluginState.update { PluginState(stage, Started) }
    if (new == null) throw NullPointerException("'new' state was null after update")

    /* Validate */
    if (stage.ordinal == 0) {
        if (previous != null) error("Illegal state transition: '$previous' -> '$stage'")
    } else if (previous != PluginState(stage = PluginStage.entries[stage.ordinal - 1], Finished))
        error("Illegal state transition: '$previous' -> '$stage'")

    /* Resume futures */
    pluginStateFuture(new).complete(new)
}

@InternalHotReloadApi
fun Project.finish(stage: PluginStage) {
    val (previous, new) = pluginState.update { PluginState(stage, Finished) }
    if (new == null) throw NullPointerException("'new' state was null after update")

    /* Validate */
    if (previous != PluginState(stage, Started))
        error("Illegal state transition: '$previous' -> '$stage'")

    /* Resume futures */
    pluginStateFuture(new).complete(new)
}

private fun Project.pluginStateFuture(state: PluginState): CompletableFuture<PluginState> {
    return pluginStateFutures.getOrPut(state) { CompletableFuture() }
}

@InternalHotReloadApi
fun Project.onStartFuture(stage: PluginStage): Future<PluginState> {
    return pluginStateFuture(PluginState(stage, Started))
}

@InternalHotReloadApi
fun Project.onFinishFuture(stage: PluginStage): Future<PluginState> {
    return pluginStateFuture(PluginState(stage, Finished))
}

@InternalHotReloadApi
suspend fun PluginStage.await() {
    project().onStartFuture(this).await()
}

@InternalHotReloadApi
suspend fun PluginStage.awaitFinish() {
    project().onFinishFuture(this).await()
}
