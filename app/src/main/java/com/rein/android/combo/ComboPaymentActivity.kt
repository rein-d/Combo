package com.rein.android.combo

import android.os.Bundle
import android.widget.Toast
import ru.evotor.framework.calculator.MoneyCalculator
import ru.evotor.framework.component.PaymentPerformer
import ru.evotor.framework.component.PaymentPerformerApi
import ru.evotor.framework.core.IntegrationActivity
import ru.evotor.framework.core.action.event.receipt.changes.position.SetExtra
import ru.evotor.framework.core.action.event.receipt.payment.combined.result.PaymentDelegatorCanceledAllEventResult
import ru.evotor.framework.core.action.event.receipt.payment.combined.result.PaymentDelegatorSelectedEventResult
import ru.evotor.framework.payment.PaymentPurpose
import ru.evotor.framework.payment.PaymentType
import ru.evotor.framework.receipt.Receipt
import ru.evotor.framework.receipt.ReceiptApi
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.thread

class ComboPaymentActivity : IntegrationActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combo_payment)


        intent.getStringExtra(KEY_RECEIPT_UUID)?.let { receiptUuid ->
            startProcessingPayment(receiptUuid)
        } ?: setCancelAllResultAndFinish()
    }

    private fun startProcessingPayment(receiptUuid: String) {
        thread {
            val receipt = ReceiptApi.getReceipt(this, receiptUuid)
            if (receipt == null) {
                setCancelAllResultAndFinish()
                return@thread
            }
            val payments = receipt.getPayments()
            val totalSum = calcTotalSum(receipt)
            val alreadyPayedSum = calcAlreadyPayedSum(receipt)

            val alreadyHasAdvance =
                payments.any { it.paymentPerformer.paymentSystem?.paymentType == PaymentType.ADVANCE }


            val paymentId = UUID.randomUUID().toString()
            val paymentTotal: BigDecimal
            val paymentDescription: String?
            val paymentPerformer: PaymentPerformer?
            if (alreadyHasAdvance) {
                val performers = PaymentPerformerApi.getAllPaymentPerformers(packageManager)

                paymentPerformer =
                    performers.firstOrNull { it.paymentSystem?.paymentType == PaymentType.ELECTRON }
                paymentDescription = CARD_PAYMENT_DESCRIPTION

                paymentTotal = MoneyCalculator.subtract(totalSum, alreadyPayedSum)
            } else {
                val performers = PaymentPerformerApi.getAllPaymentPerformers(packageManager)

                paymentPerformer =
                    performers.firstOrNull { it.paymentSystem?.paymentType == PaymentType.ADVANCE }
                paymentDescription = ADVANCE_PAYMENT_DESCRIPTION

                paymentTotal = MoneyCalculator.divide(totalSum, BigDecimal(2))
            }
            if (paymentPerformer == null) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Не найден исполнитель платежа! Возможно надо поставить приложение аванса?",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    setCancelAllResultAndFinish()
                }

                return@thread
            }
            onPaymentDelegatorSelected(
                PaymentPurpose(
                    paymentId,
                    paymentPerformer.paymentSystem?.paymentSystemId,
                    paymentPerformer,
                    paymentTotal,
                    null,
                    paymentDescription
                ), null
            )
        }
    }

    private fun calcAlreadyPayedSum(receipt: Receipt): BigDecimal {
        return receipt.getPayments().sumByBigDecimal { it.value }
    }

    private fun calcTotalSum(receipt: Receipt): BigDecimal {
        val sum = receipt.printDocuments.sumByBigDecimal { doc ->
            doc.positions.sumByBigDecimal { it.totalWithoutDocumentDiscount }
        }

        return sum.subtract(receipt.getDiscount())
    }

    private fun setCancelAllResultAndFinish() {
        setIntegrationResult(PaymentDelegatorCanceledAllEventResult(null))
        finish()
    }

    private fun onPaymentDelegatorSelected(paymentPurpose: PaymentPurpose, extra: SetExtra?) {
        setIntegrationResult(PaymentDelegatorSelectedEventResult(paymentPurpose, extra))
        finish()
    }

    private inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
        var sum = BigDecimal.ZERO
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }

}

const val ADVANCE_PAYMENT_DESCRIPTION = "Предоплатой"
const val CARD_PAYMENT_DESCRIPTION = "Банковская карта"
const val KEY_RECEIPT_UUID = "RECEIPT_UUID"