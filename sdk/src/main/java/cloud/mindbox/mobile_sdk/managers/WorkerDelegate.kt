package cloud.mindbox.mobile_sdk.managers

import android.content.Context
import android.os.Build
import androidx.work.ListenableWorker
import cloud.mindbox.mobile_sdk.Mindbox
import cloud.mindbox.mobile_sdk.MindboxConfiguration
import cloud.mindbox.mobile_sdk.logger.MindboxLogger
import cloud.mindbox.mobile_sdk.logOnException
import cloud.mindbox.mobile_sdk.repository.MindboxPreferences
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import java.util.concurrent.CountDownLatch

internal class WorkerDelegate() {

    private var isWorkerStopped = false

    fun sendEventsWithResult(
        context: Context,
        parent: Any
    ): ListenableWorker.Result {
        MindboxLogger.d(parent, "Start working...")

        try {
            Mindbox.initComponents(context)

            // Handle SSL error for Android less 21
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                try {
                    ProviderInstaller.installIfNeeded(context)
                } catch (repairableException: GooglePlayServicesRepairableException) {
                    MindboxLogger.e(
                        parent,
                        "GooglePlayServices should be updated",
                        repairableException
                    )
                } catch (notAvailableException: GooglePlayServicesNotAvailableException) {
                    MindboxLogger.e(
                        parent,
                        "GooglePlayServices aren't available",
                        notAvailableException
                    )
                }
            }

            val configuration = DbManager.getConfigurations()

            if (MindboxPreferences.isFirstInitialize || configuration == null) {
                MindboxLogger.e(
                    parent,
                    "MindboxConfiguration was not initialized",
                )
                return ListenableWorker.Result.failure()
            }

            val eventKeys = DbManager.getFilteredEventsKeys()
            if (eventKeys.isNullOrEmpty()) {
                MindboxLogger.d(parent, "Events list is empty")
                return ListenableWorker.Result.success()
            } else {
                MindboxLogger.d(parent, "Will be sent ${eventKeys.size}")

                sendEvents(context, eventKeys, configuration, parent)

                return if (DbManager.getFilteredEventsKeys().isNullOrEmpty()) {
                    ListenableWorker.Result.success()
                } else if (!isWorkerStopped) {
                    ListenableWorker.Result.retry()
                } else {
                    ListenableWorker.Result.failure()
                }
            }
        } catch (e: Exception) {
            MindboxLogger.e(parent, "Failed events work", e)
            return ListenableWorker.Result.failure()
        }
    }

    private fun sendEvents(
        context: Context,
        eventKeys: List<String>,
        configuration: MindboxConfiguration,
        parent: Any
    ) {
        runCatching {

            val eventsCount = eventKeys.size - 1
            val deviceUuid = MindboxPreferences.deviceUuid

            eventKeys.forEachIndexed { index, eventKey ->
                val countDownLatch = CountDownLatch(1)
                val event = DbManager.getEvent(eventKey) ?: return@forEachIndexed

                if (isWorkerStopped) return

                GatewayManager.sendEvent(context, configuration, deviceUuid, event) { isSent ->
                    if (isSent) {
                        DbManager.removeEventFromQueue(eventKey)
                    }

                    MindboxLogger.i(parent, "sent event #${index + 1} from $eventsCount")

                    countDownLatch.countDown()
                }

                try {
                    countDownLatch.await()
                } catch (e: InterruptedException) {
                    MindboxLogger.e(parent, "doWork -> sending was interrupted", e)
                }
            }
        }.logOnException()
    }

    fun onEndWork(parent: Any) {
        isWorkerStopped = true
        MindboxLogger.d(parent, "onStopped work")
    }

}
