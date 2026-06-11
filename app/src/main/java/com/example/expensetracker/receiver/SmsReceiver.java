package com.example.expensetracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.SmsPattern;
import com.example.expensetracker.data.entity.Transaction;
import com.example.expensetracker.ml.ExpenseClassifier;
import com.example.expensetracker.utils.NotificationHelper;
import com.example.expensetracker.utils.SenderIdExtractor;
import com.example.expensetracker.utils.SmsParser;

import java.util.List;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        for (SmsMessage sms : messages) {
            String sender    = sms.getOriginatingAddress();
            String body      = sms.getMessageBody();
            long   timestamp = sms.getTimestampMillis();

            if (sender == null || body == null) continue;
            if (!SmsParser.looksLikeTransaction(body)) continue;

            AppDatabase.dbExecutor.execute(() ->
                processSms(context, sender, body, timestamp));
        }
    }

    private void processSms(Context context, String rawSender, String body, long timestamp) {
        AppDatabase db = AppDatabase.getInstance(context);

        // ── Strip the TRAI prefix (e.g. "AD-SBIINB" → "SBIINB") ─────────────
        String strippedSender = SenderIdExtractor.strip(rawSender);

        // ── 1. Match against registered bank accounts ─────────────────────────
        List<String> registeredSenders = db.bankAccountDao().getSenderIdsForUser(1);

        boolean senderMatched    = false;
        String  matchedSenderId  = null;

        for (String regSender : registeredSenders) {
            // Compare stripped sender against stored 6-char header
            if (strippedSender.equalsIgnoreCase(regSender)) {
                senderMatched   = true;
                matchedSenderId = regSender;
                break;
            }
        }

        // Fallback: partial contains (handles rare sub-variants like SBIBNK vs SBIINB)
        if (!senderMatched) {
            for (String regSender : registeredSenders) {
                if (strippedSender.contains(regSender.toUpperCase()) ||
                    regSender.toUpperCase().contains(strippedSender)) {
                    senderMatched   = true;
                    matchedSenderId = regSender;
                    break;
                }
            }
        }

        if (!senderMatched && !SmsParser.looksLikeTransaction(body)) return;

        // ── 2. Load matching SmsPattern ───────────────────────────────────────
        SmsPattern pattern = null;

        if (matchedSenderId != null) {
            // Exact match by registered sender ID
            pattern = db.smsPatternDao().getPatternBySenderId(matchedSenderId);
        }

        if (pattern == null) {
            // Fallback: scan all enabled patterns, match by stripped sender header
            List<SmsPattern> allPatterns = db.smsPatternDao().getEnabledPatterns();
            for (SmsPattern p : allPatterns) {
                String patternHeader = SenderIdExtractor.strip(p.smsSenderId);
                if (strippedSender.equalsIgnoreCase(patternHeader) ||
                    strippedSender.contains(patternHeader) ||
                    patternHeader.contains(strippedSender)) {
                    pattern = p;
                    break;
                }
            }
        }

        if (pattern == null) return; // Unknown sender — ignore

        // ── 3. Parse SMS ──────────────────────────────────────────────────────
        SmsParser.ParsedSms parsed = SmsParser.parse(body, pattern);
        if (parsed == null || parsed.amount <= 0) return;

        parsed.timestamp = timestamp;

        // ── 4. Classify category ──────────────────────────────────────────────
        int categoryId = ExpenseClassifier.classify(parsed.merchant, body);

        // ── 5. Resolve bankAccountId ──────────────────────────────────────────
        Integer bankAccountId = null;
        if (matchedSenderId != null) {
            List<com.example.expensetracker.data.entity.BankAccount> accounts =
                db.bankAccountDao().getAccountsForUserSync(1);
            for (com.example.expensetracker.data.entity.BankAccount acc : accounts) {
                if (acc.smsSenderId.equalsIgnoreCase(matchedSenderId)) {
                    bankAccountId = acc.id;
                    break;
                }
            }
        }

        // ── 6. Check for duplicate pending transaction ───────────────────────
        // Look for pending manual UPI transactions within ±5 minutes with same amount
        long timeWindowStart = timestamp - (5 * 60 * 1000); // 5 minutes before
        long timeWindowEnd = timestamp + (5 * 60 * 1000);   // 5 minutes after
        
        List<Transaction> potentialDuplicates = db.transactionDao()
                .findPendingByAmountAndTime(parsed.amount, timeWindowStart, timeWindowEnd);
        
        Transaction matchingPending = null;
        if (potentialDuplicates != null && !potentialDuplicates.isEmpty()) {
            // Use first match (should typically be only one)
            matchingPending = potentialDuplicates.get(0);
        }

        // ── 7. Save Transaction ───────────────────────────────────────────────
        Transaction txn = new Transaction();
        txn.amount          = parsed.amount;
        txn.categoryId      = categoryId;
        txn.bankAccountId   = bankAccountId;
        txn.source          = "SMS";
        txn.transactionType = parsed.type;
        txn.merchantName    = parsed.merchant;
        txn.upiRef          = parsed.upiRef;
        txn.note            = null;
        txn.rawSms          = parsed.rawSms;
        txn.isEdited        = false;
        txn.timestamp       = parsed.timestamp;
        txn.status          = "COMPLETED"; // SMS transactions are always completed
        
        // Set payment method based on transaction details
        if (parsed.upiRef != null && !parsed.upiRef.isEmpty()) {
            txn.paymentMethod = "UPI"; // Has UPI reference, it's a UPI payment
        } else if (parsed.rawSms != null && parsed.rawSms.toUpperCase().contains("CARD")) {
            txn.paymentMethod = "CARD"; // SMS mentions card
        } else if (parsed.rawSms != null && (parsed.rawSms.toUpperCase().contains("NEFT") || 
                   parsed.rawSms.toUpperCase().contains("RTGS") || 
                   parsed.rawSms.toUpperCase().contains("IMPS"))) {
            txn.paymentMethod = "NET_BANKING"; // Bank transfer
        } else {
            txn.paymentMethod = "UPI"; // Default to UPI for most digital transactions
        }

        db.transactionDao().insert(txn);

        // ── 8. Notify (with duplicate detection) ──────────────────────────────
        if (matchingPending != null) {
            // Found a potential duplicate - notify with merge suggestion
            NotificationHelper.showSmsTransactionWithPendingMatch(context,
                    parsed.merchant != null ? parsed.merchant : pattern.bankName,
                    parsed.amount, parsed.type, matchingPending);
        } else {
            // Normal SMS transaction notification
            NotificationHelper.showSmsTransaction(context,
                    parsed.merchant != null ? parsed.merchant : pattern.bankName,
                    parsed.amount, parsed.type);
        }
    }
}
