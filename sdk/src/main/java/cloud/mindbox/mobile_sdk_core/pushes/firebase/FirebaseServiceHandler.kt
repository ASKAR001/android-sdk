package cloud.mindbox.mobile_sdk_core.pushes.firebase

import android.content.Context
import android.os.Build
import cloud.mindbox.mobile_sdk_core.logger.MindboxLoggerInternal
import cloud.mindbox.mobile_sdk_core.pushes.PushServiceHandler
import cloud.mindbox.mobile_sdk_core.returnOnException
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import java.util.*
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FirebaseServiceHandler : PushServiceHandler() {

    private const val ZERO_ID = "00000000-0000-0000-0000-000000000000"

    override val notificationProvider: String = "FCM"

    override fun initService(context: Context) {
        FirebaseApp.initializeApp(context)
    }

    override suspend fun getToken(
        context: Context
    ): String? = suspendCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnCanceledListener {
                continuation.resumeWithException(CancellationException())
            }
            .addOnSuccessListener { token ->
                continuation.resumeWith(Result.success(token))
            }
            .addOnFailureListener(continuation::resumeWithException)
    }

    override fun getAdsIdentification(context: Context): String = runCatching {
        val advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
        val id = advertisingIdInfo.id
        if (advertisingIdInfo.isLimitAdTrackingEnabled || id.isNullOrEmpty() || id == ZERO_ID) {
            MindboxLoggerInternal.d(
                this,
                "Device uuid cannot be received from AdvertisingIdClient. Will be generated from random. " +
                        "isLimitAdTrackingEnabled=${advertisingIdInfo.isLimitAdTrackingEnabled}, " +
                        "uuid from AdvertisingIdClient = $id"
            )
            generateRandomUuid()
        } else {
            MindboxLoggerInternal.d(this, "Received from AdvertisingIdClient: device uuid - $id")
            id
        }
    }.returnOnException {
        MindboxLoggerInternal.d(
            this,
            "Device uuid cannot be received from AdvertisingIdClient. Will be generated from random"
        )
        generateRandomUuid()
    }

    override fun ensureVersionCompatibility(context: Context, logParent: Any) {
        // Handle SSL error for Android less 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                ProviderInstaller.installIfNeeded(context)
            } catch (repairableException: GooglePlayServicesRepairableException) {
                MindboxLoggerInternal.e(
                    logParent,
                    "GooglePlayServices should be updated",
                    repairableException
                )
            } catch (notAvailableException: GooglePlayServicesNotAvailableException) {
                MindboxLoggerInternal.e(
                    logParent,
                    "GooglePlayServices aren't available",
                    notAvailableException
                )
            }
        }
    }

    private fun generateRandomUuid() = UUID.randomUUID().toString()

}