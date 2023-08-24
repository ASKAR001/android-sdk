package cloud.mindbox.mobile_sdk.inapp.presentation.view

import android.view.ViewGroup
import cloud.mindbox.mobile_sdk.SnackbarPosition
import cloud.mindbox.mobile_sdk.inapp.domain.interfaces.InAppImageSizeStorage
import cloud.mindbox.mobile_sdk.inapp.domain.models.Element
import cloud.mindbox.mobile_sdk.inapp.domain.models.InAppType
import cloud.mindbox.mobile_sdk.inapp.domain.models.InAppTypeWrapper
import cloud.mindbox.mobile_sdk.inapp.domain.models.Layer
import cloud.mindbox.mobile_sdk.inapp.presentation.InAppCallback
import cloud.mindbox.mobile_sdk.logger.mindboxLogI


internal class SnackbarInAppViewHolder(
    override val wrapper: InAppTypeWrapper<InAppType.Snackbar>,
    private val inAppCallback: InAppCallback,
    private val inAppImageSizeStorage: InAppImageSizeStorage
) :
    AbstractInAppViewHolder<InAppType.Snackbar>() {

    override val isActive: Boolean
        get() = isInAppMessageActive

    override fun show(currentRoot: ViewGroup) {
        super.show(currentRoot)
        wrapper.inAppType.layers.forEach { layer ->
            when (layer) {
                is Layer.ImageLayer -> {
                    addUrlSource(layer, inAppCallback)
                }
            }
        }
        mindboxLogI("Show ${wrapper.inAppType.inAppId} on ${this.hashCode()}")
        currentDialog.requestFocus()
    }

    override fun initView(currentRoot: ViewGroup) {
        super.initView(currentRoot)
        currentDialog.setSwipeToDismissCallback {
            hide()
        }
    }

    override fun addUrlSource(layer: Layer.ImageLayer, inAppCallback: InAppCallback) {
        super.addUrlSource(layer, inAppCallback)
        when (layer.source) {
            is Layer.ImageLayer.Source.UrlSource -> {
                InAppImageView(currentDialog.context).also { inAppImageView ->
                    mindboxLogI("Try to show inapp with id ${wrapper.inAppType.inAppId}")
                    getImageFromCache(layer.source.url, inAppImageView)
                    currentDialog.addView(inAppImageView)
                    inAppImageView.prepareViewForSnackBar(
                        inAppImageSizeStorage.getSizeByIdAndUrl(
                            wrapper.inAppType.inAppId,
                            layer.source.url
                        )
                    )
                }
            }
        }

    }

    override fun bind() {
        wrapper.inAppType.elements.forEach { element ->
            when (element) {
                is Element.CloseButton -> {
                    val inAppCrossView = InAppCrossView(currentDialog.context, element).apply {
                        setOnClickListener {
                            mindboxLogI("In-app dismissed by close click")
                            inAppCallback.onInAppDismissed(wrapper.inAppType.inAppId)
                            hide()
                        }
                    }
                    currentDialog.addView(inAppCrossView)
                    inAppCrossView.prepareViewForSnackbar(currentDialog)
                }
            }
        }
        when (wrapper.inAppType.position.gravity.vertical) {
            SnackbarPosition.TOP -> {
                currentDialog.slideDown()
            }

            SnackbarPosition.BOTTOM -> {
                currentDialog.slideUp()
            }
        }

        mindboxLogI("In-app shown")
        wrapper.onInAppShown.onShown()
    }

    override fun hide() {
        super.hide()
        (currentDialog.parent as? ViewGroup?)?.apply {
            removeView(currentDialog)
            removeView(currentBackground)
        }
    }
}