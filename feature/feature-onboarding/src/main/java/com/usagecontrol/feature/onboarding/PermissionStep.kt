package com.usagecontrol.feature.onboarding

enum class PermissionStep(val index: Int) {
    NotificationAccess(0),
    UsageAccess(1),
    DndAccess(2);

    companion object {
        const val TOTAL_STEPS = 3
        fun fromIndex(index: Int): PermissionStep = entries[index]
    }
}
