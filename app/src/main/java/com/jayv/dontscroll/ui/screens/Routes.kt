package com.jayv.dontscroll.ui.screens

sealed class Route {
    data object Settings : Route()
    data object Blocking : Route()
    data class EditSchedule(val id: Long?) : Route()
    data object Reliability : Route()
    data object Debug : Route()
    data object About : Route()
}
