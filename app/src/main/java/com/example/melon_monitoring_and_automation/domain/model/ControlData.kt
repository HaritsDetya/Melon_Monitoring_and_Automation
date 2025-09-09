package com.example.melon_monitoring_and_automation.domain.model

data class ControlData (
    val waterPump: Boolean = false,
    val nutrientPumpA: Boolean = false,
    val phThreshold: Double = 0.0,
    val irrigationInterval: Int = 0
)