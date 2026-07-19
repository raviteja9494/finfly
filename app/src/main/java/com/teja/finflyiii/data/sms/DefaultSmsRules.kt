/* Data-layer default Indian bank and merchant category rule configuration. */
package com.teja.finflyiii.data.sms

import com.teja.finflyiii.domain.model.BankRule
import com.teja.finflyiii.domain.model.CategoryRule

object DefaultSmsRules {
    fun bankRules(now: Long): List<BankRule> = listOf(
        bank(
            "00000000-0000-4000-8000-000000000001", "HDFC Savings",
            listOf("AD-HDFCBK-S", "AX-HDFCBK-S", "VM-HDFCBK"),
            listOf("debited", "paid from", "sent"), listOf("credited", "received"),
            listOf("Rs.{amount}", "Rs {amount}", "INR {amount}"),
            listOf("To {description} On", "To {description} Ref"),
            listOf("Ref {ref}", "UPI Ref No.{ref}", "Ref No {ref}"), now,
        ),
        bank(
            "00000000-0000-4000-8000-000000000002", "HDFC Credit Card",
            listOf("AD-HDFCBK-S", "AX-HDFCBK"),
            listOf("spent", "transaction of"), listOf("credited", "payment received"),
            listOf("Rs.{amount}", "INR {amount}"),
            listOf("At {description} On", "at {description}"), listOf("Ref {ref}"), now,
        ),
        bank(
            "00000000-0000-4000-8000-000000000003", "ICICI Savings",
            listOf("AX-ICICIT-S", "VM-ICICIB", "AD-ICICIB"),
            listOf("debited", "spent"), listOf("credited with", "is credited"),
            listOf("Rs {amount}", "Rs.{amount}"),
            listOf("from {description}. UPI", "to {description} UPI"),
            listOf("UPI:{ref}", "UPI Ref {ref}"), now,
        ),
        bank(
            "00000000-0000-4000-8000-000000000004", "Edge CSB / Jupiter",
            listOf("VM-JTEDGE-S", "AD-JTEDGE"),
            listOf("debited to your", "paid from your"), listOf("credited to your"),
            listOf("₹{amount}", "Rs.{amount}"),
            listOf("to {description} on 20", "to {description} on 7/"),
            listOf("UPI Ref no.{ref}", "Ref no.{ref}"), now,
        ),
    )

    fun categoryRules(): List<CategoryRule> = listOf(
        category("10000000-0000-4000-8000-000000000001", "Food & Dining", 10,
            "SWIGGY", "ZOMATO", "ANANDA", "DARSHINI", "RESTAURANT", "CAFE", "HOTEL", "BIRYANI",
            "PIZZA", "BURGER", "KFC", "DOMINOS", "TACO BELL", "UDUPI", "PAROTTA", "DOSA",
            "DUGDHA", "PARLOUR", "BARBEQUENATION"),
        category("10000000-0000-4000-8000-000000000002", "Groceries", 20,
            "DMART", "AVENUE SUPERMARTS", "BIGBASKET", "BLINKIT", "ZEPTO", "STAR BAZAAR",
            "RELIANCE SMART", "SMART BAZAR", "SPAR"),
        category("10000000-0000-4000-8000-000000000003", "Transportation", 30,
            "UBER", "OLA", "RAPIDO", "IRCTC", "BMTC", "METRO", "INDIGO", "PETROL", "FUEL",
            "BPCL", "HPCL", "APSRTC"),
        category("10000000-0000-4000-8000-000000000004", "Health", 40,
            "PHARMACY", "MEDPLUS", "APOLLO", "NETMEDS", "1MG", "HOSPITAL", "CLINIC", "MANIPAL",
            "PHARMEASY"),
        category("10000000-0000-4000-8000-000000000005", "Shopping", 50,
            "AMAZON", "FLIPKART", "MEESHO", "MYNTRA", "AJIO", "NYKAA", "IKEA", "DECATHLON",
            "ZUDIO", "BATA"),
        category("10000000-0000-4000-8000-000000000006", "Bills & Recharges", 60,
            "AIRTEL", "JIO", "VODAFONE", "NETFLIX", "SPOTIFY", "HOTSTAR", "BROADBAND"),
        category("10000000-0000-4000-8000-000000000007", "Finance", 70,
            "MUTUAL FUND", "ICCL", "ZERODHA", "GROWW", "CRED", "LOAN", "EMI"),
        category("10000000-0000-4000-8000-000000000008", "Transfers & Gifts", 80,
            "TIRUMALA", "TIRUPATI", "TEMPLE", "DONATION"),
    )

    private fun bank(
        id: String, name: String, senders: List<String>, debit: List<String>, credit: List<String>,
        amounts: List<String>, descriptions: List<String>, references: List<String>, now: Long,
    ) = BankRule(id, name, true, senders, "", "", debit, credit, amounts, descriptions, references, now, now)

    private fun category(id: String, name: String, priority: Int, vararg keywords: String) =
        CategoryRule(id, name, keywords.toList(), name, priority, true)
}
