package io.github.immaghzbad.aetherst.shared.model

data class AutoConnectSettings(
    val autoConnectOnStart: Boolean = false,
    val autoConnectOnBoot: Boolean = false,
    val autoConnectOnNetwork: Boolean = false,
    val autoRestartOnCrash: Boolean = false,
    val autoConnectAfterCrash: Boolean = false
)
