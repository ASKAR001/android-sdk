package com.mindbox.androidsdk.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import cloud.mindbox.mobile_sdk.InitializeMindboxException
import cloud.mindbox.mobile_sdk.Mindbox
import com.mindbox.androidsdk.InitializeData
import com.mindbox.androidsdk.R
import kotlinx.android.synthetic.main.fragment_complete_data.*
import java.util.*

class CompleteDataFragment(
    private val data: InitializeData
) :
    Fragment(R.layout.fragment_complete_data) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fillData()
        updateButton.setOnClickListener { fillData() }
        sendPushEvent.setOnClickListener { sendPushDeliveryEvents() }
    }

    private fun fillData() {
        initParams.text = """
            domain: ${data.domain}
            
            endpoint: ${data.endpoint}
            
            deviceUUID: ${data.deviceId}
            
            installId: ${data.installId}
            
            SubscribeCustomerIfCreated: ${data.subscribe}
        """.trimIndent()

        sdkData.text = """
                deviceUUID: ${
            try {
                Mindbox.getDeviceUuid()
            } catch (e: InitializeMindboxException) {
                "null"
            }
        }
                
                save token: ${Mindbox.getFmsToken()}
                
                save token date: ${Mindbox.getFmsTokenSaveDate()}
                
                SDK version: ${Mindbox.getSdkVersion()}
            """.trimIndent()
    }

    private fun sendPushDeliveryEvents() {
        try {
            visibleProgress()
            val count: Int = countPushEvents.text.toString().toInt()

            val thread: Thread = object : Thread() {
                override fun run() {

                    try {
                        for (i in 1..count) {
                            Mindbox.onPushReceived(
                                context = requireContext(),
                                uniqKey = UUID.randomUUID().toString()
                            )
                        }

                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } finally {
                        goneProgress()
                    }
                }
            }

            thread.start()

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "It's not a number", Toast.LENGTH_SHORT).show()
            goneProgress()
        }
    }

    private fun visibleProgress() {
        generateProgress.visibility = View.VISIBLE
        sendPushEvent.visibility = View.GONE
    }

    private fun goneProgress() {
        activity?.runOnUiThread {
            generateProgress.visibility = View.GONE
            sendPushEvent.visibility = View.VISIBLE
        }
    }
}