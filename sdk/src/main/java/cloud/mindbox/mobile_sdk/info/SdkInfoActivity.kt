package cloud.mindbox.mobile_sdk.info

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.work.WorkManager
import cloud.mindbox.mobile_sdk.BuildConfig
import cloud.mindbox.mobile_sdk.Mindbox
import cloud.mindbox.mobile_sdk.R
import cloud.mindbox.mobile_sdk.managers.DbManager
import cloud.mindbox.mobile_sdk.services.BackgroundWorkManager
import cloud.mindbox.mobile_sdk.services.MindboxOneTimeEventWorker
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SdkInfoActivity : Activity() {

    private data class SdkInfo(
        val sdkVersion: String,
        val configuration: String,
        val uuid: String,
        val pushService: String?,
        val pushToken: String?,
        val tokenSaveDate: String,
        val workerStatus: WorkerStatus,
        val eventsCount: Int,
    )

    private data class WorkerStatus(
        val status: String,
        val progress: Double,
    )

    private var sdkInfo: SdkInfo? = null

    private lateinit var sdkVersionView: TextView
    private lateinit var configurationView: TextView
    private lateinit var uuidView: TextView
    private lateinit var pushServiceView: TextView
    private lateinit var pushTokenView: TextView
    private lateinit var tokenSaveDateView: TextView
    private lateinit var workerStatusView: TextView
    private lateinit var workerProgressView: TextView
    private lateinit var eventsCountView: TextView

    private val coroutineContext = SupervisorJob()
    private val scope = CoroutineScope(coroutineContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sdk_info)

        sdkVersionView = findViewById(R.id.sdkVersion)
        configurationView = findViewById(R.id.configuration)
        uuidView = findViewById(R.id.uuid)
        pushServiceView = findViewById(R.id.pushService)
        pushTokenView = findViewById(R.id.pushToken)
        tokenSaveDateView = findViewById(R.id.tokenSaveDate)
        workerStatusView = findViewById(R.id.workerStatus)
        workerProgressView = findViewById(R.id.workerProgress)
        eventsCountView = findViewById(R.id.eventsCount)

        getAndDisplayData()

        findViewById<Button>(R.id.updateBtn).setOnClickListener { getAndDisplayData() }
        findViewById<Button>(R.id.copyToClipboardBtn).setOnClickListener { copyToClipboard() }
    }

    private fun getAndDisplayData() {
        scope.launch {
            sdkInfo = acquireData()
            launch(Dispatchers.Main) { displayData() }
        }
    }

    private suspend fun acquireData() = SdkInfo(
        sdkVersion = BuildConfig.VERSION_NAME,
        configuration = withContext(Dispatchers.IO) { DbManager.getConfigurations().toString() },
        uuid = getDeviceUUID(),
        pushService = Mindbox.pushServiceHandler?.notificationProvider,
        pushToken = getPushToken(),
        tokenSaveDate = Mindbox.getPushTokenSaveDate(),
        workerStatus = getWorkerStatus(),
        eventsCount = withContext(Dispatchers.IO) { DbManager.getEvents().size },
    )

    private suspend fun getDeviceUUID(): String = suspendCoroutine { continuation ->
        Mindbox.subscribeDeviceUuid(continuation::resume)
    }

    private suspend fun getPushToken(): String? = suspendCoroutine { continuation ->
        Mindbox.subscribePushToken(continuation::resume)
    }

    private suspend fun getWorkerStatus(): WorkerStatus = suspendCoroutine { continuation ->
        val future = WorkManager.getInstance(this).getWorkInfosByTag(
            BackgroundWorkManager.WORKER_TAG
        )
        future.addListener(
            {
                val workerData = future.get().firstOrNull()
                val status = workerData?.state?.name ?: "Not started"
                val progress = workerData?.progress?.getDouble(
                    MindboxOneTimeEventWorker.PROGRESS,
                    -1.0,
                ) ?: -1.0
                val workerStatus = WorkerStatus(
                    status = status,
                    progress = progress,
                )
                continuation.resume(workerStatus)
            },
            Dispatchers.Default.asExecutor()
        )
    }

    private fun displayData() {

        sdkVersionView.text = sdkInfo?.sdkVersion
        configurationView.text = sdkInfo?.configuration
        uuidView.text = sdkInfo?.uuid
        pushServiceView.text = sdkInfo?.pushService
        pushTokenView.text = sdkInfo?.pushToken
        tokenSaveDateView.text = sdkInfo?.tokenSaveDate
        workerStatusView.text = sdkInfo?.workerStatus?.status
        workerProgressView.text = getString(R.string.worker_progress_value, sdkInfo?.workerStatus?.progress.toString())
        eventsCountView.text = sdkInfo?.eventsCount.toString()

    }

    private fun copyToClipboard() {

        val clipboardText = """
            ${getString(R.string.uuid)}: ${sdkInfo?.sdkVersion}
            ${getString(R.string.configuration)}: ${sdkInfo?.configuration}
            ${getString(R.string.uuid)}: ${sdkInfo?.uuid}
            ${getString(R.string.push_service)}: ${sdkInfo?.pushService}
            ${getString(R.string.push_token)}: ${sdkInfo?.pushToken}
            ${getString(R.string.token_save_date)}: ${sdkInfo?.tokenSaveDate}
            ${getString(R.string.worker_status)}: ${sdkInfo?.workerStatus}
            ${getString(R.string.events_in_database)}: ${sdkInfo?.eventsCount}
        """.trimIndent()

        val clipData = ClipData.newPlainText("Mindbox info", clipboardText)

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clipData)

    }

}