package cloud.mindbox.mobile_sdk_core.managers

import android.content.Context
import cloud.mindbox.mobile_sdk_core.logOnException
import cloud.mindbox.mobile_sdk_core.logger.MindboxLoggerInternal
import cloud.mindbox.mobile_sdk_core.models.*
import cloud.mindbox.mobile_sdk_core.models.operation.OperationResponseBaseInternal
import cloud.mindbox.mobile_sdk_core.repository.MindboxPreferences
import cloud.mindbox.mobile_sdk_core.services.BackgroundWorkManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

internal object MindboxEventManager {

    private const val EMPTY_JSON_OBJECT = "{}"
    private const val NULL_JSON = "null"

    private val gson = Gson()

    fun appInstalled(context: Context, initData: InitData, shouldCreateCustomer: Boolean) {
        runCatching {
            val eventType = if (shouldCreateCustomer) {
                EventType.AppInstalled
            } else {
                EventType.AppInstalledWithoutCustomer
            }
            DbManager.addEventToQueue(
                context, Event(eventType = eventType, body = gson.toJson(initData))
            )
        }.logOnException()
    }

    fun appInfoUpdate(context: Context, initData: UpdateData) {
        runCatching {
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.AppInfoUpdated,
                    body = gson.toJson(initData)
                )
            )
        }.logOnException()
    }

    fun pushDelivered(context: Context, uniqKey: String) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                val fields = hashMapOf(
                    EventParameters.UNIQ_KEY.fieldName to uniqKey
                )
                DbManager.addEventToQueue(
                    context, Event(
                        eventType = EventType.PushDelivered,
                        additionalFields = fields
                    )
                )
            }
        }.logOnException()
    }

    fun pushClicked(context: Context, clickData: TrackClickData) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                DbManager.addEventToQueue(
                    context, Event(
                        eventType = EventType.PushClicked,
                        body = gson.toJson(clickData)
                    )
                )
            }
        }.logOnException()
    }

    fun appStarted(context: Context, trackVisitData: TrackVisitData) {
        runCatching {
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.TrackVisit,
                    body = gson.toJson(trackVisitData)
                )
            )
        }.logOnException()
    }

    fun asyncOperation(context: Context, name: String, body: String) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                DbManager.addEventToQueue(
                    context, Event(
                        eventType = EventType.AsyncOperation(name),
                        body = if (body.isNotBlank() && body != NULL_JSON) body else EMPTY_JSON_OBJECT
                    )
                )
            }
        }.logOnException()
    }

    fun <T, V : OperationResponseBaseInternal> syncOperation(
        context: Context,
        name: String,
        body: T,
        classOfV: Class<V>,
        onSuccess: (V) -> Unit,
        onError: (MindboxErrorInternal) -> Unit
    ) = runCatching {
        val configuration = checkConfiguration(onError) ?: return

        val json = gson.toJson(body)
        val jsonBody = if (json.isNotBlank() && json != NULL_JSON) json else EMPTY_JSON_OBJECT
        val event = createSyncEvent(name, jsonBody)
        val deviceUuid = MindboxPreferences.deviceUuid

        GatewayManager.sendEvent(
            context = context,
            configuration = configuration,
            deviceUuid = deviceUuid,
            event = event,
            classOfT = classOfV,
            onSuccess = onSuccess,
            onError = onError
        )
    }.logOnException()

    fun syncOperation(
        context: Context,
        name: String,
        bodyJson: String,
        onSuccess: (String) -> Unit,
        onError: (MindboxErrorInternal) -> Unit
    ) = runCatching {
        val configuration = checkConfiguration(onError) ?: return

        val event = createSyncEvent(name, bodyJson)
        val deviceUuid = MindboxPreferences.deviceUuid

        GatewayManager.sendEvent(
            context = context,
            configuration = configuration,
            deviceUuid = deviceUuid,
            event = event,
            onSuccess = onSuccess,
            onError = onError
        )
    }.logOnException()

    private fun createSyncEvent(
        name: String,
        bodyJson: String
    ) = Event(
        eventType = EventType.SyncOperation(name),
        body = bodyJson
    )

    private fun checkConfiguration(onError: (MindboxErrorInternal) -> Unit): Configuration? {
        val configuration = DbManager.getConfigurations()
        if (MindboxPreferences.isFirstInitialize || configuration == null) {
            MindboxLoggerInternal.e(this, "Configuration was not initialized")
            onError.invoke(MindboxErrorInternal.Unknown())
            return null
        }
        return configuration
    }

    fun sendEventsIfExist(context: Context) {
        runCatching {
            if (DbManager.getFilteredEvents().isNotEmpty()) {
                BackgroundWorkManager.startOneTimeService(context)
            }
        }.logOnException()
    }

    fun <T> operationBodyJson(body: T): String = gson.toJson(body)

}
