package cloud.mindbox.mobile_sdk.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cloud.mindbox.mobile_sdk.managers.WorkerDelegate
import cloud.mindbox.mobile_sdk.utils.LoggingExceptionHandler

internal class MindboxOneTimeEventWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    companion object {
        const val PROGRESS = "Progress"
    }

    private val workerDelegate: WorkerDelegate by lazy { WorkerDelegate() }

    override fun doWork(): Result = LoggingExceptionHandler.runCatching(
        defaultValue = Result.failure()
    ) {
            workerDelegate.sendEventsWithResult(
                context = applicationContext,
                worker = this,
                parent = this,
            )
        }

    override fun onStopped() {
        super.onStopped()
        LoggingExceptionHandler.runCatching {
            workerDelegate.onEndWork(this)
        }
    }

    fun setEventsProgress(eventIndex: Int, totalEvents: Int) {
        val progress = (eventIndex.toDouble() / totalEvents.toDouble()) * 100
        val progressData = workDataOf(PROGRESS to progress)
        setProgressAsync(progressData)
    }
}