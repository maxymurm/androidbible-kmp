package org.androidbible.data.sync

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.androidbible.data.local.AndroidBibleDatabase
import org.androidbible.data.remote.ApiService
import org.androidbible.di.ApiConfig
import org.androidbible.domain.model.*

/**
 * Manages offline sync queue, push/pull operations,
 * and WebSocket real-time sync via Laravel Reverb.
 */
class SyncManager(
    private val db: AndroidBibleDatabase,
    private val api: ApiService,
    private val client: HttpClient,
) {
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private var wsJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val deviceId: String by lazy {
        // Get or create device ID from sync_state table
        val existing = db.syncQueries.getSyncState("default").executeAsOneOrNull()
        existing?.device_id ?: run {
            val id = com.benasher44.uuid.uuid4().toString()
            db.syncQueries.upsertSyncState(id, 0, null)
            id
        }
    }

    /**
     * Start the sync manager: push pending, pull new, connect WebSocket.
     */
    fun start() {
        scope.launch {
            pushPendingEvents()
            pullNewEvents()
            connectWebSocket()
            updatePendingCount()
        }
    }

    /**
     * Stop the sync manager.
     */
    fun stop() {
        wsJob?.cancel()
        scope.cancel()
    }

    /**
     * Push all pending items from the offline queue to the server.
     */
    suspend fun pushPendingEvents() {
        _syncState.value = SyncState.PUSHING

        val pending = db.syncQueries.getPendingSyncItems().executeAsList()
        if (pending.isEmpty()) {
            _syncState.value = SyncState.IDLE
            return
        }

        try {
            val events = pending.map { item ->
                SyncEventPayload(
                    entityType = item.entity_type,
                    entityId = item.entity_id,
                    action = item.action,
                    payload = item.payload,
                )
            }

            val response = api.syncPush(SyncPushRequest(events = events, deviceId = deviceId))

            // Mark as processed
            val now = Clock.System.now().toString()
            db.transaction {
                pending.forEach { item ->
                    db.syncQueries.markSyncItemProcessed(now, item.id)
                }
            }

            // Update sync state
            db.syncQueries.upsertSyncState(deviceId, response.currentVersion, now)

            // Clean up old processed items
            db.syncQueries.deleteProcessedItems()

            Napier.i("Pushed ${response.processed} sync events", tag = "Sync")
        } catch (e: Exception) {
            Napier.e("Failed to push sync events", e, tag = "Sync")
            // Mark items as failed for retry
            pending.forEach { item ->
                db.syncQueries.markSyncItemFailed(item.id)
            }
        }

        _syncState.value = SyncState.IDLE
        updatePendingCount()
    }

    /**
     * Pull new events from the server.
     */
    suspend fun pullNewEvents() {
        _syncState.value = SyncState.PULLING

        try {
            val state = db.syncQueries.getSyncState(deviceId).executeAsOneOrNull()
            val lastVersion = state?.last_version ?: 0

            val response = api.syncPull(
                SyncPullRequest(lastVersion = lastVersion, deviceId = deviceId)
            )

            if (response.events.isNotEmpty()) {
                applyEvents(response.events)
                val now = Clock.System.now().toString()
                db.syncQueries.upsertSyncState(deviceId, response.currentVersion, now)
                Napier.i("Pulled ${response.events.size} sync events", tag = "Sync")
            }
        } catch (e: Exception) {
            Napier.e("Failed to pull sync events", e, tag = "Sync")
        }

        _syncState.value = SyncState.IDLE
    }

    /**
     * Apply remote sync events to local database.
     */
    private fun applyEvents(events: List<SyncEvent>) {
        db.transaction {
            events.forEach { event ->
                try {
                    when (event.entityType) {
                        "marker" -> applyMarkerEvent(event)
                        "label" -> applyLabelEvent(event)
                        "progress_mark" -> applyProgressMarkEvent(event)
                        else -> Napier.w("Unknown entity type: ${event.entityType}", tag = "Sync")
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to apply event ${event.id}", e, tag = "Sync")
                }
            }
        }
    }

    private fun applyMarkerEvent(event: SyncEvent) {
        // Parse payload and apply to local DB
        // The payload contains the marker data from the server
        val now = Clock.System.now().toString()
        when (event.action) {
            "create", "update" -> {
                try {
                    val marker = json.decodeFromString<Marker>(event.payload)
                    db.markerQueries.insertMarker(
                        id = null,
                        gid = marker.gid,
                        server_id = event.entityId.toLongOrNull(),
                        user_id = marker.userId,
                        bible_version_id = marker.bibleVersionId,
                        ari = marker.ari.toLong(),
                        kind = marker.kind.toLong(),
                        caption = marker.caption,
                        verse_count = marker.verseCount.toLong(),
                        color = marker.color?.toLong(),
                        is_synced = 1,
                        created_at = marker.createdAt ?: now,
                        updated_at = now,
                        deleted_at = null,
                    )
                } catch (e: Exception) {
                    Napier.e("Failed to parse marker event", e, tag = "Sync")
                }
            }
            "delete" -> {
                val existing = db.markerQueries.getMarkerByGid(event.entityId).executeAsOneOrNull()
                if (existing != null) {
                    db.markerQueries.softDeleteMarker(now, now, existing.id)
                }
            }
        }
    }

    private fun applyLabelEvent(event: SyncEvent) {
        val now = Clock.System.now().toString()
        when (event.action) {
            "create", "update" -> {
                try {
                    val label = json.decodeFromString<Label>(event.payload)
                    db.markerQueries.insertLabel(
                        id = null,
                        gid = label.gid,
                        server_id = event.entityId.toLongOrNull(),
                        user_id = label.userId,
                        title = label.title,
                        background_color = label.backgroundColor?.toLong(),
                        is_synced = 1,
                        created_at = label.createdAt ?: now,
                        updated_at = now,
                        deleted_at = null,
                    )
                } catch (e: Exception) {
                    Napier.e("Failed to parse label event", e, tag = "Sync")
                }
            }
            "delete" -> {
                val existing = db.markerQueries.getLabelByGid(event.entityId).executeAsOneOrNull()
                if (existing != null) {
                    db.markerQueries.softDeleteLabel(now, now, existing.id)
                }
            }
        }
    }

    private fun applyProgressMarkEvent(event: SyncEvent) {
        // Similar pattern for progress marks
        Napier.d("Progress mark event: ${event.action}", tag = "Sync")
    }

    /**
     * Connect to Laravel Reverb WebSocket for real-time updates.
     */
    private fun connectWebSocket() {
        wsJob?.cancel()
        wsJob = scope.launch {
            while (isActive) {
                try {
                    client.webSocket(ApiConfig.WS_URL) {
                        Napier.i("WebSocket connected", tag = "Sync")

                        // Subscribe to user sync channel
                        val subscribeMsg = """{"event":"pusher:subscribe","data":{"channel":"private-user.sync"}}"""
                        send(Frame.Text(subscribeMsg))

                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    handleWebSocketMessage(text)
                                }
                                else -> {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    Napier.e("WebSocket disconnected, reconnecting in 5s", e, tag = "Sync")
                    delay(5000) // Reconnect delay
                }
            }
        }
    }

    private suspend fun handleWebSocketMessage(message: String) {
        try {
            // Parse Reverb/Pusher message format
            if (message.contains("sync.event")) {
                Napier.d("Received sync event via WebSocket", tag = "Sync")
                pullNewEvents()
            }
        } catch (e: Exception) {
            Napier.e("Failed to handle WebSocket message", e, tag = "Sync")
        }
    }

    private fun updatePendingCount() {
        _pendingCount.value = db.syncQueries.getPendingCount().executeAsOne().toInt()
    }

    enum class SyncState {
        IDLE,
        PUSHING,
        PULLING,
    }
}
