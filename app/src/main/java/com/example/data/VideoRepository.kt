package com.example.data

import com.example.model.AdminSystemControl
import com.example.model.SnackVideoSettings
import com.example.model.UserProfile
import com.example.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoRepository {
    private val _currentUser = MutableStateFlow(UserProfile())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _videos = MutableStateFlow(
        listOf(
            VideoItem(id = "1", title = "Welcome to Tashan!", viewsCount = 8500),
            VideoItem(id = "2", title = "Creator Monetization Live", viewsCount = 6900)
        )
    )
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _adminSystemControl = MutableStateFlow(AdminSystemControl())
    val adminSystemControl: StateFlow<AdminSystemControl> = _adminSystemControl.asStateFlow()

    private val _snackVideoSettings = MutableStateFlow(SnackVideoSettings())
    val snackVideoSettings: StateFlow<SnackVideoSettings> = _snackVideoSettings.asStateFlow()

    fun updateAdminControl(updated: AdminSystemControl) {
        _adminSystemControl.value = updated
        _snackVideoSettings.value = _snackVideoSettings.value.copy(pkrRatePer1000Views = updated.monetizationPkrPer1000Views)
    }

    fun recordVideoView(videoId: String) {
        val rate = _adminSystemControl.value.monetizationPkrPer1000Views / 1000.0
        _videos.value = _videos.value.map { item ->
            if (item.id == videoId) {
                item.copy(viewsCount = item.viewsCount + 1)
            } else {
                item
            }
        }
        _currentUser.value = _currentUser.value.copy(totalViews = _currentUser.value.totalViews + 1)
        _adminSystemControl.value = _adminSystemControl.value.copy(
            totalViewsLogged = _adminSystemControl.value.totalViewsLogged + 1,
            totalRevenueDistributedPkr = _adminSystemControl.value.totalRevenueDistributedPkr + rate
        )
    }

    fun adminDeleteVideo(videoId: String) {
        _videos.value = _videos.value.filter { it.id != videoId }
    }
}
