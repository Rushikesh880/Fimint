package com.moneytracker.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.moneytracker.app.data.AppDatabase
import com.moneytracker.app.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.transactionDao()

                    for (sms in messages) {
                        val body = sms.messageBody ?: continue
                        val timestamp = sms.timestampMillis
                        val sender = sms.originatingAddress ?: ""

                        val parsed = SmsParser.parse(body, sender)
                        if (parsed != null) {
                            val count = dao.getCountByBodyAndTimestamp(body, timestamp)
                            if (count == 0) {
                                val tx = Transaction(
                                    amount = parsed.amount,
                                    merchant = parsed.merchant,
                                    type = parsed.type,
                                    timestamp = timestamp,
                                    rawSmsBody = body,
                                    account = parsed.account,
                                    isManual = false
                                )
                                dao.insertTransaction(tx)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing incoming SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
