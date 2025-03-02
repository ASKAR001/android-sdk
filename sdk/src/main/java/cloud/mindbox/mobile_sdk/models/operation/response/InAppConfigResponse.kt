package cloud.mindbox.mobile_sdk.models.operation.response


import cloud.mindbox.mobile_sdk.inapp.data.dto.PayloadDto
import cloud.mindbox.mobile_sdk.inapp.domain.models.InAppTime
import cloud.mindbox.mobile_sdk.models.TreeTargetingDto
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

internal data class InAppConfigResponse(
    @SerializedName("inapps")
    val inApps: List<InAppDto>?,
    @SerializedName("monitoring")
    val monitoring: List<LogRequestDto>?,
    @SerializedName("settings")
    val settings: SettingsDto?,
    @SerializedName("abtests")
    val abtests: List<ABTestDto>?,
)

internal data class SettingsDtoBlank(
    @SerializedName("operations")
    val operations: Map<String?, OperationDtoBlank?>?,
    @SerializedName("ttl")
    val ttl: TtlDtoBlank?
)
 {
    internal data class OperationDtoBlank(
        @SerializedName("systemName")
        val systemName: String?
    )

    internal data class TtlDtoBlank(
        @SerializedName("inapps")
        val inApps: TtlParametersDtoBlank?
    )

    internal data class TtlParametersDtoBlank(
        @SerializedName("unit")
        val unit: String?,
        @SerializedName("value")
        val value: Long?
    )
}

internal data class SettingsDto(
    @SerializedName("operations")
    val operations: Map<String, OperationDto>?,
    @SerializedName("ttl")
    val ttl:TtlDto?
)
internal data class OperationDto(
    @SerializedName("systemName")
    val systemName: String
)

internal data class TtlDto(
    @SerializedName("inapps")
    val inApps: TtlParametersDto?
)

internal data class TtlParametersDto(
    @SerializedName("unit")
    val unit: InAppTime,
    @SerializedName("value")
    val value: Long
)

internal data class LogRequestDto(
    @SerializedName("requestId")
    val requestId: String,
    @SerializedName("deviceUUID")
    val deviceId: String,
    @SerializedName("from")
    val from: String,
    @SerializedName("to")
    val to: String,
)

internal data class InAppDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("frequency")
    val frequency: FrequencyDto,
    @SerializedName("sdkVersion")
    val sdkVersion: SdkVersion?,
    @SerializedName("targeting")
    val targeting: TreeTargetingDto?,
    @SerializedName("form")
    val form: FormDto?,
)

internal sealed class FrequencyDto {
    internal data class FrequencyOnceDto(
        @SerializedName("${"$"}type")
        val type: String,
        @SerializedName("kind")
        val kind: String
    ): FrequencyDto() {
        internal companion object {
            const val FREQUENCY_ONCE_JSON_NAME = "once"

            const val FREQUENCY_KIND_LIFETIME = "lifetime"
            const val FREQUENCY_KIND_SESSION = "session"
        }
    }

    internal data class FrequencyPeriodicDto(
        @SerializedName("${"$"}type")
        val type: String,
        @SerializedName("unit")
        val unit: String,
        @SerializedName("value")
        val value: Long
    ): FrequencyDto() {
        internal companion object {
            const val FREQUENCY_PERIODIC_JSON_NAME = "periodic"

            const val FREQUENCY_UNIT_HOURS = "MINUTES"
            const val FREQUENCY_UNIT_MINUTES = "HOURS"
            const val FREQUENCY_UNIT_DAYS = "DAYS"
            const val FREQUENCY_UNIT_SECONDS = "SECONDS"
        }
    }
}

internal data class SdkVersion(
    @SerializedName("min")
    val minVersion: Int?,
    @SerializedName("max")
    val maxVersion: Int?,
)

internal data class FormDto(
    @SerializedName("variants")
    val variants: List<PayloadDto?>?,
)

internal data class MonitoringDto(
    @SerializedName("logs")
    val logs: List<LogRequestDtoBlank>?,
)

internal data class LogRequestDtoBlank(
    @SerializedName("requestId")
    val requestId: String?,
    @SerializedName("deviceUUID")
    val deviceId: String?,
    @SerializedName("from")
    val from: String?,
    @SerializedName("to")
    val to: String?,
)

internal data class InAppConfigResponseBlank(
    @SerializedName("inapps")
    val inApps: List<InAppDtoBlank>?,
    @SerializedName("monitoring")
    val monitoring: MonitoringDto?,
    @SerializedName("settings")
    val settings: SettingsDtoBlank?,
    @SerializedName("abtests")
    val abtests: List<ABTestDto>?,
) {

    internal data class InAppDtoBlank(
        @SerializedName("id")
        val id: String,
        @SerializedName("frequency")
        val frequency: JsonObject?,
        @SerializedName("sdkVersion")
        val sdkVersion: SdkVersion?,
        @SerializedName("targeting")
        val targeting: JsonObject?,
        @SerializedName("form")
        val form: JsonObject?, // FormDto. Parsed after filtering inApp versions.
    )
}