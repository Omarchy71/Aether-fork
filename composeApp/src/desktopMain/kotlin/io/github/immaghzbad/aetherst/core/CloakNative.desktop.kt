package io.github.immaghzbad.aetherst.core

@Suppress("unused")
object CloakNative {
    fun isAvailable(): Boolean = false
    fun start(jPath: String): Int = -1
    fun stop(): Int = -1
    fun isRunning(): Int = 0
    fun setLogLevel(level: Int) {}
}
