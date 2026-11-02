package com.moneytracker.app.sms

import java.util.regex.Pattern

object SmsParser {
    data class ParsedTransaction(
        val amount: Double,
        val type: String, // "DEBIT" or "CREDIT"
        val merchant: String,
        val account: String
    )

    // 1. Promotional, marketing, discount, medicine, coupon, deals, and spam keywords.
    // Real banks NEVER use these words in transaction alerts.
    private val PROMO_AND_SPAM_PATTERN = Pattern.compile(
        "(?i)\\b(?:" +
                "discount|coupon|voucher|promo|gift|code|use code|flat\\s*\\d+|off\\b|" +
                "medicines|medicine|medivines|pharma|pharmacy|doctor|lab test|" +
                "cashback|bonus|win\\b|reward|claim|deal|sale\\b|offer|save\\s*rs|" +
                "order|cart|shop|delivery|delivered|shipping|dispatched|courier|tracking|" +
                "flipkart|amazon|myntra|meesho|swiggy|zomato|blinkit|zepto|bigbasket|" +
                "otp|verification code|secret code|login code|do not share|" +
                "pack benefits|data balance|validity|talktime|unlimited calls|high speed data|" +
                "plan expires|plan validity|recharge successful|recharge of rs|" +
                "bill generated|bill due|payment due|due date|overdue|min amount due|pay before|" +
                "pre-approved|apply now|loan approved|instant loan|credit limit" +
                ")\\b"
    )

    // 2. Prepaid Card / Wallet Card / PPI Exclusion
    // Excluded to prevent duplicate transactions because the user already tracks the bank top-up
    private val PREPAID_CARD_PATTERN = Pattern.compile(
        "(?i)\\b(?:" +
                "prepaid\\s*card|prepaid\\s*wallet|prepaid\\s*instrument|prepaid\\s*a/c|prepaid\\s*account|" +
                "virtual\\s*prepaid|physical\\s*prepaid|prepaid\\s*bal|" +
                "card\\s*type\\s*:\\s*prepaid|spent\\s*on\\s*(?:your\\s*)?prepaid|debited\\s*from\\s*(?:your\\s*)?prepaid|" +
                "gift\\s*card|meal\\s*card|forex\\s*card|transit\\s*card|fastag|" +
                "omnicard|fampay|famapp|akudo|zaggle|sodexo|pluxee|livquik|transcorp|quikwallet|pockets" +
                ")\\b"
    )

    private val PREPAID_SENDER_PATTERN = Pattern.compile(
        "(?i)(?:OMNI|FAM|POCKET|ZAGGLE|SODEXO|PLUXEE|LIVQIK|TRANSC|QUICKW|PINELB|FASTAG)"
    )

    // 2. Mandatory Account Number Pattern (Must have a real 3-5 digit account/card suffix)
    // E.g. "A/c *1234", "A/c XX1234", "A/C no. ...5678", "Card ending 4321", "Card XX9876", "A/c 1234"
    private val ACCOUNT_NUMBER_PATTERN = Pattern.compile(
        "(?i)(?:a/c|acct|acc|account|card)\\s*(?:no\\.?|num\\.?|ending in|ending|is|xx|x|[*Xx]+)?\\s*[:.]?\\s*[*Xx]*([0-9]{3,5})\\b"
    )

    // Alternative masked account pattern: "**1234" or "XX1234"
    private val MASKED_ACCOUNT_PATTERN = Pattern.compile(
        "(?i)[*Xx]{2,}([0-9]{3,5})\\b"
    )

    // 3. Mandatory Bank Balance or Financial Reference Marker
    // Standard bank SMS in India always include resulting balance or formal UPI/Txn reference
    private val BANK_BALANCE_OR_REF_PATTERN = Pattern.compile(
        "(?i)\\b(?:" +
                "avl\\s*bal|avail\\s*bal|available\\s*bal(?:ance)?|bal(?:ance)?\\s*(?:is|:)|updated\\s*bal|tot\\s*bal|ledger\\s*bal|" +
                "upi\\s*(?:ref|txn|id)|rrn\\b|imps\\s*ref|neft\\s*ref|rtgs|atm\\s*wdr|pos\\s*txn|txn\\s*(?:id|ref)|ref\\s*no" +
                ")\\b"
    )

    // 4. Strict Banking Debit Verbs
    private val DEBIT_PATTERN = Pattern.compile(
        "(?i)\\b(?:" +
                "debited\\s*(?:by|with|for|from)?|" +
                "spent\\s*(?:on|at)?|" +
                "paid\\s*(?:to|for)?|" +
                "withdrawn\\s*(?:from|at)?|" +
                "transferred\\s*(?:to|rs)|" +
                "sent\\s*(?:rs|inr)|" +
                "charge(?:d)?\\s*(?:for|on)" +
                ")\\b"
    )

    // 5. Strict Banking Credit Verbs
    private val CREDIT_PATTERN = Pattern.compile(
        "(?i)\\b(?:" +
                "credited\\s*(?:by|with|to|for)?|" +
                "deposited\\s*(?:in|to|with)?|" +
                "refund\\s*(?:of|from)?|" +
                "received\\s*(?:rs|inr)" +
                ")\\b"
    )

    // Amount extraction: Rs. 500, Rs 500.00, INR 500, etc.
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:RS\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"
    )

    // Merchant / Payee extraction
    private val MERCHANT_PATTERN = Pattern.compile(
        "(?i)(?:at|to|towards|vpa|thru)\\s+([A-Za-z0-9@_#&\\s.-]{3,30})"
    )

    private val NOISE_MERCHANT_WORDS = listOf(
        "rs", "inr", "on", "via", "using", "ref", "rrn", "avl", "avail", "bal", "balance",
        "date", "time", "your", "account", "a/c", "card", "bank", "link", "click", "info"
    )

    fun parse(smsBody: String, sender: String = ""): ParsedTransaction? {
        val cleanBody = smsBody.replace("\n", " ").trim()

        // 1. Instant Rejection of Promotional, Marketing, Discount, or Spam messages
        if (PROMO_AND_SPAM_PATTERN.matcher(cleanBody).find()) {
            return null
        }

        // 2. Exclude Prepaid Card / Wallet Card / PPI transactions to prevent double-counting
        if (PREPAID_CARD_PATTERN.matcher(cleanBody).find() || PREPAID_SENDER_PATTERN.matcher(sender).find()) {
            return null
        }

        // 3. Mandatory Bank Template Component #1: Must have a specific account or card number
        var account = extractAccountNumber(cleanBody)
        val isSenderBank = isBankSender(sender)

        // If no explicit account number is found in the body, it is only acceptable if it's an official bank sender with a UPI/RRN reference
        if (account == null) {
            if (isSenderBank && cleanBody.contains("UPI", ignoreCase = true)) {
                account = "UPI"
            } else {
                return null // Reject generic "added to your account" without a real bank account number
            }
        }

        // 3. Mandatory Bank Template Component #2: Must have a bank balance indicator OR a formal UPI/Txn reference
        val hasBankBalanceOrRef = BANK_BALANCE_OR_REF_PATTERN.matcher(cleanBody).find() || isSenderBank
        if (!hasBankBalanceOrRef) {
            return null
        }

        // 4. Mandatory Bank Template Component #3: Strict Transaction Action (Debit or Credit)
        val isDebit = DEBIT_PATTERN.matcher(cleanBody).find()
        val isCredit = CREDIT_PATTERN.matcher(cleanBody).find()

        val type = when {
            isDebit -> "DEBIT"
            isCredit -> "CREDIT"
            else -> return null // Reject if neither debit nor credit is explicitly present
        }

        // 5. Amount Extraction
        val amountMatcher = AMOUNT_PATTERN.matcher(cleanBody)
        if (!amountMatcher.find()) return null

        val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // 6. Merchant / Payee extraction
        val merchantMatcher = MERCHANT_PATTERN.matcher(cleanBody)
        var merchant = cleanSenderName(sender).ifBlank { "Bank Transfer" }
        if (merchantMatcher.find()) {
            val rawMerchant = merchantMatcher.group(1)?.trim() ?: ""
            val cleaned = cleanMerchantString(rawMerchant)
            if (cleaned.isNotBlank()) {
                merchant = cleaned
            }
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            account = account
        )
    }

    private fun extractAccountNumber(body: String): String? {
        val matcher1 = ACCOUNT_NUMBER_PATTERN.matcher(body)
        if (matcher1.find()) {
            val acc = matcher1.group(1)
            if (!acc.isNullOrBlank()) return acc
        }
        val matcher2 = MASKED_ACCOUNT_PATTERN.matcher(body)
        if (matcher2.find()) {
            val acc = matcher2.group(1)
            if (!acc.isNullOrBlank()) return acc
        }
        return null
    }

    private fun isBankSender(sender: String): Boolean {
        if (sender.isBlank()) return false
        val clean = sender.uppercase()
        val bankKeywords = listOf(
            "BK", "BNK", "BANK", "SBI", "HDFC", "ICICI", "AXIS", "KOTAK", "PNB", "BOB",
            "BOI", "CANARA", "UNION", "INDUS", "FED", "YESB", "IDFC", "RBL", "AUBANK", "AUFIN",
            "PAYTM", "GPAY", "BHIM", "CRED"
        )
        return bankKeywords.any { clean.contains(it) }
    }

    private fun cleanSenderName(sender: String): String {
        if (sender.isBlank()) return ""
        val parts = sender.split("-")
        return parts.lastOrNull()?.trim() ?: sender.trim()
    }

    private fun cleanMerchantString(raw: String): String {
        val firstPart = raw.split(Pattern.compile("(?i)\\b(?:on|via|using|ref|rrn|avl|avail|bal|balance)\\b"))[0]
            .trim()
            .trimEnd('.', ',', '-', ':')

        if (firstPart.length < 2) return ""
        if (NOISE_MERCHANT_WORDS.any { firstPart.equals(it, ignoreCase = true) }) return ""
        return firstPart
    }
}
