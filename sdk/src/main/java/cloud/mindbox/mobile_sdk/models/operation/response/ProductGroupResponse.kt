package cloud.mindbox.mobile_sdk.models.operation.response

import cloud.mindbox.mobile_sdk.models.operation.Ids
import com.google.gson.annotations.SerializedName

open class ProductGroupResponse(
    @SerializedName("ids") val ids: Ids? = null
)
