package cloud.mindbox.mobile_sdk.models

import com.google.gson.annotations.SerializedName

private const val INIT_DATA_VERSION = 0

internal data class InitData(
    @SerializedName("token") val token: String,
    @SerializedName("isTokenAvailable") val isTokenAvailable: Boolean,
    @SerializedName("installationId") val installationId: String,
    @SerializedName("externalDeviceUUID") val externalDeviceUUID: String,
    @SerializedName("isNotificationsEnabled") val isNotificationsEnabled: Boolean,
    @SerializedName("subscribe") val subscribe: Boolean,
    @SerializedName("instanceId") val instanceId: String,
    @SerializedName("version") private val version: Int = INIT_DATA_VERSION
)

internal data class UpdateData(
    @SerializedName("token") val token: String,
    @SerializedName("isTokenAvailable") val isTokenAvailable: Boolean,
    @SerializedName("isNotificationsEnabled") val isNotificationsEnabled: Boolean,
    @SerializedName("instanceId") val instanceId: String,
    @SerializedName("version") val version: Int
)

internal data class TrackClickData(
    @SerializedName("messageUniqueKey") val messageUniqueKey: String,
    @SerializedName("buttonUniqueKey") val buttonUniqueKey: String
)

internal data class TrackVisitData(
    @SerializedName("ianaTimeZone") val ianaTimeZone: String,
    @SerializedName("endpointId") val endpointId: String
)
