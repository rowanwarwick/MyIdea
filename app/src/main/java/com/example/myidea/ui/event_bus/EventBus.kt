package com.example.myidea.ui.event_bus

import com.example.myidea.ui.model.CaptureCoordinate
import kotlinx.coroutines.flow.MutableSharedFlow

internal object EventBus {
    private var coordinateChannel = MutableSharedFlow<CaptureCoordinate>()

    fun listenCoordinateChannel() = coordinateChannel
    suspend fun sendCoordinateChannel(coordinate: CaptureCoordinate) = coordinateChannel.emit(coordinate)
}