package com.example.expensetracker.ml;

import java.util.HashMap;
import java.util.Map;

/**
 * Keyword-based expense category classifier.
 * Uses merchant name + SMS body to assign a category ID.
 * Falls back to "Other" (id = 10) if no keywords match.
 *
 * Category IDs match the pre-populated order in AppDatabase:
 *  1 = Food
 *  2 = Shopping
 *  3 = Fund Transfer
 *  4 = Friend
 *  5 = Bills & Utilities
 *  6 = Transport
 *  7 = Health & Medical
 *  8 = Entertainment
 *  9 = Education
 * 10 = Other
 */
public class ExpenseClassifier {

    private static final int CAT_FOOD        = 1;
    private static final int CAT_SHOPPING    = 2;
    private static final int CAT_TRANSFER    = 3;
    private static final int CAT_FRIEND      = 4;
    private static final int CAT_BILLS       = 5;
    private static final int CAT_TRANSPORT   = 6;
    private static final int CAT_HEALTH      = 7;
    private static final int CAT_ENTERTAIN   = 8;
    private static final int CAT_EDUCATION   = 9;
    private static final int CAT_OTHER       = 10;

    private static final Map<String, Integer> KEYWORD_MAP = new HashMap<>();

    static {
        // Food
        for (String k : new String[]{
            "zomato","swiggy","domino","pizza","burger","kfc","mcdonald","subway",
            "dunkin","starbucks","cafe","restaurant","food","dining","eat","biryani",
            "hotel","bakery","juice","canteen","mess","dabba","kitchen"
        }) KEYWORD_MAP.put(k, CAT_FOOD);

        // Shopping
        for (String k : new String[]{
            "amazon","flipkart","myntra","ajio","nykaa","meesho","snapdeal","shopsy",
            "mall","store","shop","retail","cloth","fashion","apparel","beauty",
            "cosmetic","supermarket","bigbasket","dmart","reliance","more","zepto",
            "blinkit","instamart","grofer","jiomart","market"
        }) KEYWORD_MAP.put(k, CAT_SHOPPING);

        // Fund Transfer
        for (String k : new String[]{
            "neft","imps","rtgs","transfer","upi","fund","sent to","paid to","vpa"
        }) KEYWORD_MAP.put(k, CAT_TRANSFER);

        // Bills & Utilities
        for (String k : new String[]{
            "electricity","water","gas","internet","broadband","wifi","jio","airtel",
            "bsnl","vodafone","vi","recharge","mobile","postpaid","prepaid",
            "bill","utility","tneb","bescom","msedcl","rent","maintenance","society",
            "tata sky","d2h","netflix","prime","hotstar","disney","spotify","youtube"
        }) KEYWORD_MAP.put(k, CAT_BILLS);

        // Transport
        for (String k : new String[]{
            "uber","ola","rapido","auto","cab","taxi","metro","bus","train","irctc",
            "railway","flight","indigo","spicejet","air india","vistara","petrol",
            "diesel","fuel","hp","iocl","bpcl","parking","toll","fastag","redbus",
            "makemytrip","goibibo","yatra"
        }) KEYWORD_MAP.put(k, CAT_TRANSPORT);

        // Health & Medical
        for (String k : new String[]{
            "hospital","clinic","doctor","pharmacy","medical","medicine","apollo",
            "fortis","medplus","netmeds","1mg","practo","diagnostics","lab","scan",
            "xray","health","dental","eye","optician","chemist","drug"
        }) KEYWORD_MAP.put(k, CAT_HEALTH);

        // Entertainment
        for (String k : new String[]{
            "pvr","inox","movie","cinema","theatre","bookmyshow","concert","event",
            "gaming","game","playstation","xbox","steam","entertainment","arcade",
            "amusement","park","zoo","museum","sport","gym","fitness"
        }) KEYWORD_MAP.put(k, CAT_ENTERTAIN);

        // Education
        for (String k : new String[]{
            "school","college","university","course","tuition","coaching","udemy",
            "coursera","byju","vedantu","unacademy","exam","fee","book","stationery",
            "education","library","hostel"
        }) KEYWORD_MAP.put(k, CAT_EDUCATION);
    }

    private ExpenseClassifier() {}

    /**
     * Classify based on merchant name and/or SMS body.
     * Returns a category ID (1–10).
     */
    public static int classify(String merchantName, String smsBody) {
        // Try merchant name first (more reliable)
        if (merchantName != null) {
            int id = matchKeywords(merchantName.toLowerCase());
            if (id != CAT_OTHER) return id;
        }

        // Fall back to full SMS body scan
        if (smsBody != null) {
            int id = matchKeywords(smsBody.toLowerCase());
            if (id != CAT_OTHER) return id;
        }

        return CAT_OTHER;
    }

    private static int matchKeywords(String text) {
        for (Map.Entry<String, Integer> entry : KEYWORD_MAP.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return CAT_OTHER;
    }
}
