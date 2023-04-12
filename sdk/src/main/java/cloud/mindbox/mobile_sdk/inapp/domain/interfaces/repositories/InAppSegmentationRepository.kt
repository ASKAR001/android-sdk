package cloud.mindbox.mobile_sdk.inapp.domain.interfaces.repositories

import cloud.mindbox.mobile_sdk.inapp.domain.models.*
import cloud.mindbox.mobile_sdk.inapp.domain.models.CustomerSegmentationInApp
import cloud.mindbox.mobile_sdk.inapp.domain.models.InApp
import cloud.mindbox.mobile_sdk.inapp.domain.models.CustomerSegmentationFetchStatus

internal interface InAppSegmentationRepository {

    var unShownInApps: List<InApp>

    suspend fun fetchCustomerSegmentations()

    suspend fun fetchProductSegmentation(product: Pair<String, String>)

    fun getProductSegmentations(productId: String): Set<ProductSegmentationResponseWrapper?>

    fun setCustomerSegmentationStatus(status: CustomerSegmentationFetchStatus)

    fun getCustomerSegmentationFetched(): CustomerSegmentationFetchStatus

    fun getProductSegmentationFetched(): ProductSegmentationFetchStatus

    fun setProductSegmentationFetchStatus(status: ProductSegmentationFetchStatus)

    fun getCustomerSegmentations(): List<CustomerSegmentationInApp>
}