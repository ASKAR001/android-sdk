package cloud.mindbox.mobile_sdk.inapp.domain

import cloud.mindbox.mobile_sdk.MindboxConfiguration
import cloud.mindbox.mobile_sdk.inapp.presentation.InAppMessageManagerImpl
import cloud.mindbox.mobile_sdk.logger.MindboxLoggerImpl
import cloud.mindbox.mobile_sdk.models.*
import com.android.volley.VolleyError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class InAppInteractorImpl(private val inAppRepositoryImpl: InAppRepository) :
    InAppInteractor {

    override fun processEventAndConfig(
        configuration: MindboxConfiguration,
    ): Flow<InAppType> {
        return inAppRepositoryImpl.listenInAppConfig().filterNotNull()
            //TODO add eventProcessing
            .combine(inAppRepositoryImpl.listenInAppEvents()
                .filter { inAppEventType -> inAppEventType is InAppEventType.AppStartup }) { config, event ->
                val inApp = chooseInAppToShow(config,
                    configuration)
                when (val type = inApp?.form?.variants?.first()) {
                    is Payload.SimpleImage -> InAppType.SimpleImage(inAppId = inApp.id,
                        imageUrl = type.imageUrl,
                        redirectUrl = type.redirectUrl,
                        intentData = type.intentPayload)
                    else -> null
                }
            }.filterNotNull()
    }

    override suspend fun chooseInAppToShow(
        config: InAppConfig,
        configuration: MindboxConfiguration,
    ): InApp? {
        val filteredConfig = prefilterConfig(config)
        val filteredConfigWithTargeting = getConfigWithTargeting(filteredConfig)
        val inAppsWithoutTargeting =
            filteredConfig.inApps.subtract(filteredConfigWithTargeting.inApps.toSet())
        return if (inAppsWithoutTargeting.isNotEmpty()) {
            inAppsWithoutTargeting.first()
        } else if (filteredConfigWithTargeting.inApps.isNotEmpty()) {
            runCatching {
                checkSegmentation(filteredConfig,
                    inAppRepositoryImpl.fetchSegmentations(
                        configuration,
                        filteredConfigWithTargeting))
            }.getOrElse { throwable ->
                if (throwable is VolleyError) {
                    MindboxLoggerImpl.e("", throwable.message ?: "", throwable)
                    null
                } else {
                    throw throwable
                }
            }
        } else {
            null
        }
    }

    override fun prefilterConfig(config: InAppConfig): InAppConfig {
        return config.copy(inApps = config.inApps.filter { inApp -> validateInAppVersion(inApp) }
            .filter { inApp -> validateInAppNotShown(inApp) && validateInAppTargeting(inApp) })
    }

    override fun validateInAppTargeting(inApp: InApp): Boolean {
        return when {
            (inApp.targeting == null) -> {
                false
            }
            (inApp.targeting.segmentation == null && inApp.targeting.segment != null) -> {
                false
            }
            (inApp.targeting.segmentation != null && inApp.targeting.segment == null) -> {
                false
            }
            else -> {
                true
            }
        }
    }

    override fun getConfigWithTargeting(config: InAppConfig): InAppConfig {
        return config.copy(inApps = config.inApps.filter { inApp -> inApp.targeting?.segmentation != null && inApp.targeting.segment != null })
    }

    override fun saveShownInApp(id: String) {
        inAppRepositoryImpl.saveShownInApp(id)
    }

    private suspend fun checkSegmentation(
        config: InAppConfig,
        segmentationCheckInApp: SegmentationCheckInApp,
    ): InApp? {
        return suspendCoroutine { continuation ->
            config.inApps.iterator().forEach { inApp ->
                segmentationCheckInApp.customerSegmentations.iterator()
                    .forEach { customerSegmentationInAppResponse ->
                        if (validateSegmentation(inApp, customerSegmentationInAppResponse)) {
                            continuation.resume(inApp)
                            return@suspendCoroutine
                        }
                    }
            }
            continuation.resume(null)
        }
    }

    override fun sendInAppShown(inAppId: String) {
        inAppRepositoryImpl.sendInAppShown(inAppId)
    }

    override fun sendInAppClicked(inAppId: String) {
        inAppRepositoryImpl.sendInAppClicked(inAppId)
    }


    override fun validateInAppNotShown(inApp: InApp): Boolean {
        return inAppRepositoryImpl.shownInApps.contains(inApp.id).not()
    }

    override fun validateSegmentation(
        inApp: InApp,
        customerSegmentationInApp: CustomerSegmentationInApp,
    ): Boolean {
        return if (customerSegmentationInApp.segment == null) {
            false
        } else {
            inApp.targeting?.segment == customerSegmentationInApp.segment.ids?.externalId
        }
    }

    override fun validateInAppVersion(inApp: InApp): Boolean {
        return ((inApp.minVersion?.let { min -> min <= InAppMessageManagerImpl.CURRENT_IN_APP_VERSION }
            ?: true) && (inApp.maxVersion?.let { max -> max >= InAppMessageManagerImpl.CURRENT_IN_APP_VERSION }
            ?: true))
    }


    override suspend fun fetchInAppConfig(configuration: MindboxConfiguration) {
        inAppRepositoryImpl.fetchInAppConfig(configuration)
    }
}