package cloud.mindbox.mobile_sdk.managers

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cloud.mindbox.mobile_sdk.Mindbox.IS_OPENED_FROM_PUSH_BUNDLE_KEY
import cloud.mindbox.mobile_sdk.logger.MindboxLogger
import cloud.mindbox.mobile_sdk.models.DIRECT
import cloud.mindbox.mobile_sdk.models.LINK
import cloud.mindbox.mobile_sdk.models.PUSH

internal class LifecycleManager(
    private var currentActivityName: String?,
    private var currentIntent: Intent?,
    private var onTrackVisitReady: (source: String, requestUrl: String?) -> Unit
) : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    companion object {

        private const val SCHEMA_HTTP = "http"
        private const val SCHEMA_HTTPS = "https"
        private const val MAX_INTENT_HASHES_SIZE = 50

    }

    private var isAppInBackground = true
    private var isIntentChanged = true
    private val intentHashes = mutableListOf<Int>()

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        val areActivitiesEqual = currentActivityName == activity.javaClass.name
        val intent = activity.intent
        isIntentChanged = if (currentIntent != intent) {
            updateActivityParameters(activity)
            intent?.hashCode()?.let(::updateHashesList) ?: true
        } else {
            false
        }

        if (isAppInBackground || !isIntentChanged) {
            isAppInBackground = false
            return
        }

        sendTrackVisit(activity.intent, areActivitiesEqual)
    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {
        if (currentIntent == null || currentActivityName == null) {
            updateActivityParameters(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onAppMovedToBackground() {
        isAppInBackground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onAppMovedToForeground() = currentIntent?.let(::sendTrackVisit)

    private fun updateActivityParameters(activity: Activity) {
        currentActivityName = activity.javaClass.name
        currentIntent = activity.intent
    }

    private fun sendTrackVisit(intent: Intent, areActivitiesEqual: Boolean = true) {
        val source = if (isIntentChanged) source(intent) else DIRECT

        if (areActivitiesEqual || source != DIRECT) {
            val requestUrl = if (source == LINK) intent.data?.toString() else null
            onTrackVisitReady.invoke(source, requestUrl)

            MindboxLogger.d(this, "Track visit event with source $source and url $requestUrl")
        }
    }

    private fun source(intent: Intent?) = when {
        intent?.scheme == SCHEMA_HTTP || intent?.scheme == SCHEMA_HTTPS -> LINK
        intent?.extras?.getBoolean(IS_OPENED_FROM_PUSH_BUNDLE_KEY) == true -> PUSH
        else -> DIRECT
    }

    private fun updateHashesList(code: Int) = if (!intentHashes.contains(code)) {
        if (intentHashes.size >= MAX_INTENT_HASHES_SIZE) {
            intentHashes.removeAt(0)
        }
        intentHashes.add(code)
        true
    } else {
        false
    }

}
