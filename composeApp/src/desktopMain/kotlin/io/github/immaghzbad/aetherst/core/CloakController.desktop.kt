package io.github.immaghzbad.aetherst.core

import io.github.immaghzbad.aetherst.shared.model.AetherConfig

@Suppress("unused")
object CloakController {
    fun isSupported(config: AetherConfig): Boolean = false
    fun getCloakPort(): Int = 40443
    fun prepareConfig(context: Any, config: AetherConfig): String = ""
    fun start(context: Any, config: AetherConfig): Boolean = false
    fun stop() {}
    fun isRunning(): Boolean = false
    fun getEffectivePeer(config: AetherConfig): String = config.peer
}
