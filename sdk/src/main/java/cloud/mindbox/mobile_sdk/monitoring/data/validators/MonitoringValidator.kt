package cloud.mindbox.mobile_sdk.monitoring.data.validators

import cloud.mindbox.mobile_sdk.convertToZonedDateTime
import cloud.mindbox.mobile_sdk.models.operation.response.LogRequestDtoBlank
import cloud.mindbox.mobile_sdk.monitoring.domain.models.LogResponse
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal class MonitoringValidator {

    fun validateMonitoringBlank(logRequest: LogRequestDtoBlank): Boolean {
        return validateRequestId(logRequest) && validateDeviceId(logRequest) && validateFrom(
            logRequest
        ) && validateTo(logRequest)
    }

    private fun validateRequestId(logRequest: LogRequestDtoBlank): Boolean {
        return logRequest.requestId.isNullOrBlank().not()
    }

    private fun validateDeviceId(logRequest: LogRequestDtoBlank): Boolean {
        return logRequest.deviceId.isNullOrBlank().not()
    }

    private fun validateFrom(logRequest: LogRequestDtoBlank): Boolean {
        val errorRez = LocalDateTime.parse(
            "1970-01-01T00:00:00",
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        )
            .atZone(
                ZoneOffset.UTC
            )
        return !logRequest.from.isNullOrBlank() && logRequest.from.convertToZonedDateTime() != errorRez
    }

    private fun validateTo(logRequest: LogRequestDtoBlank): Boolean {
        val errorRez = LocalDateTime.parse(
            "1970-01-01T00:00:00",
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        )
            .atZone(
                ZoneOffset.UTC
            )
        return !logRequest.to.isNullOrBlank() && logRequest.to.convertToZonedDateTime() != errorRez
    }

    fun validateMonitoring(logResponse: LogResponse): Boolean {
        return logResponse.zonedDateTime.year != 1970
    }
}