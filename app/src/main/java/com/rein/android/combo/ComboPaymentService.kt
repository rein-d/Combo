package com.rein.android.combo

import android.content.Intent
import ru.evotor.framework.core.IntegrationService
import ru.evotor.framework.core.action.event.receipt.payment.combined.PaymentDelegatorEventProcessor
import ru.evotor.framework.core.action.event.receipt.payment.combined.event.PaymentDelegatorEvent
import ru.evotor.framework.core.action.processor.ActionProcessor


class ComboPaymentService: IntegrationService() {
    override fun createProcessors(): MutableMap<String, ActionProcessor> = mutableMapOf(
        Pair(
            PaymentDelegatorEvent.NAME_ACTION,
            object : PaymentDelegatorEventProcessor() {
                override fun call(action: String, event: PaymentDelegatorEvent, callback: Callback) {
                    callback.startActivity(
                        Intent(this@ComboPaymentService, ComboPaymentActivity::class.java)
                            .putExtra(KEY_RECEIPT_UUID, event.receiptUuid)
                    )
                }
            }
        )
    )
}