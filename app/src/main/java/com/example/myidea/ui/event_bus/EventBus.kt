package com.example.myidea.ui.event_bus

import com.example.myidea.ui.model.CaptureCoordinate
import kotlinx.coroutines.flow.MutableSharedFlow

internal object EventBus {

    private var coordinateChannel = MutableSharedFlow<Pair<String, CaptureCoordinate>>()
    private var endDrawRectChannel = MutableSharedFlow<Unit>()

    fun listenCoordinateChannel() = coordinateChannel
    suspend fun sendCoordinateChannel(slot: Pair<String, CaptureCoordinate>) =
        coordinateChannel.emit(slot)

    fun listenStopDrawChannel() = endDrawRectChannel
    suspend fun sendStopDrawChannel() = endDrawRectChannel.emit(Unit)
}