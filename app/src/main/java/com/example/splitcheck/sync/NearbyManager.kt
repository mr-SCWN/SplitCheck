package com.example.splitcheck.sync

import android.content.Context
import com.example.splitcheck.util.JsonUtil
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NearbyManager {

    private const val SERVICE_ID = "com.example.splitcheck.SPLITCHECK"
    private val strategy = Strategy.P2P_POINT_TO_POINT

    private var client: ConnectionsClient? = null
    private var endpointId: String? = null

    private val _foundEndpoints = MutableSharedFlow<Endpoint>(extraBufferCapacity = 64)
    val foundEndpoints = _foundEndpoints.asSharedFlow()

    private val _syncFlow = MutableSharedFlow<SyncState>(extraBufferCapacity = 64)
    val syncFlow = _syncFlow.asSharedFlow()

    fun init(context: Context) {
        if (client == null) client = Nearby.getConnectionsClient(context.applicationContext)
    }

    fun isConnected(): Boolean = endpointId != null

    fun startAdvertising(context: Context, name: String) {
        init(context)
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client?.startAdvertising(
            name,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        )
    }

    fun startDiscovery(context: Context) {
        init(context)
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client?.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
    }

    fun requestConnection(context: Context, localName: String, remoteEndpointId: String) {
        init(context)
        client?.requestConnection(localName, remoteEndpointId, connectionLifecycleCallback)
    }

    fun stopAll() {
        client?.stopAdvertising()
        client?.stopDiscovery()
    }

    fun disconnect() {
        endpointId?.let { client?.disconnectFromEndpoint(it) }
        endpointId = null
    }

    fun sendSync(state: SyncState) {
        val eid = endpointId ?: return
        val json = JsonUtil.gson.toJson(WireMessage(type = "sync_state", payload = JsonUtil.gson.toJson(state)))
        client?.sendPayload(eid, Payload.fromBytes(json.toByteArray()))
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(eid: String, info: DiscoveredEndpointInfo) {
            _foundEndpoints.tryEmit(Endpoint(eid, info.endpointName))
        }
        override fun onEndpointLost(eid: String) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(eid: String, info: ConnectionInfo) {
            // auto accept
            client?.acceptConnection(eid, payloadCallback)
        }

        override fun onConnectionResult(eid: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                endpointId = eid
                stopAll()
            }
        }

        override fun onDisconnected(eid: String) {
            if (endpointId == eid) endpointId = null
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(eid: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val json = String(bytes)

            runCatching {
                val msg = JsonUtil.gson.fromJson(json, WireMessage::class.java)
                if (msg.type == "sync_state") {
                    val st = JsonUtil.gson.fromJson(msg.payload, SyncState::class.java)
                    _syncFlow.tryEmit(st)
                }
            }
        }
        override fun onPayloadTransferUpdate(eid: String, update: PayloadTransferUpdate) {}
    }
}

data class Endpoint(val id: String, val name: String)

data class SyncState(
    val people: Int,
    val names: List<String>,
    val items: List<com.example.splitcheck.ml.ReceiptItem>,
    val selections: List<List<Boolean>>
)

data class WireMessage(
    val type: String,
    val payload: String
)
