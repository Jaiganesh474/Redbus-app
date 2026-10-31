package com.redbus.ai.ui.localization

/**
 * Multi-language localization dictionary for redBus AI Mobile Application.
 * Supports English, Tamil (தமிழ்), Hindi (हिंदी), Kannada (ಕನ್ನಡ), Telugu (తెలుగు), and Malayalam (മലയാളം).
 */
object AppStrings {

    enum class Lang(val code: String, val label: String) {
        ENGLISH("en", "English"),
        TAMIL("ta", "தமிழ் (Tamil)"),
        HINDI("hi", "हिंदी (Hindi)"),
        KANNADA("kn", "ಕನ್ನಡ (Kannada)"),
        TELUGU("te", "తెలుగు (Telugu)"),
        MALAYALAM("ml", "മലയാളം (Malayalam)");

        companion object {
            fun parse(langName: String?): Lang {
                if (langName == null) return ENGLISH
                val lower = langName.lowercase()
                return when {
                    lower.contains("tamil") || lower.contains("தமிழ்") -> TAMIL
                    lower.contains("hindi") || lower.contains("हिंदी") -> HINDI
                    lower.contains("kannada") || lower.contains("ಕನ್ನಡ") -> KANNADA
                    lower.contains("telugu") || lower.contains("తెలుగు") -> TELUGU
                    lower.contains("malayalam") || lower.contains("മലയാളം") -> MALAYALAM
                    else -> ENGLISH
                }
            }
        }
    }

    // Key -> Map of Lang to Translation
    private val translations: Map<String, Map<Lang, String>> = mapOf(
        // Navigation Tabs
        "nav_home" to mapOf(
            Lang.ENGLISH to "Home",
            Lang.TAMIL to "முகப்பு",
            Lang.HINDI to "होम",
            Lang.KANNADA to "ಮುಖಪುಟ",
            Lang.TELUGU to "హోమ్",
            Lang.MALAYALAM to "ഹോം"
        ),
        "nav_buses" to mapOf(
            Lang.ENGLISH to "Buses",
            Lang.TAMIL to "பேருந்துகள்",
            Lang.HINDI to "बसें",
            Lang.KANNADA to "ಬಸ್‌ಗಳು",
            Lang.TELUGU to "బస్సులు",
            Lang.MALAYALAM to "ബസുകൾ"
        ),
        "nav_bookings" to mapOf(
            Lang.ENGLISH to "Bookings",
            Lang.TAMIL to "முன்பதிவுகள்",
            Lang.HINDI to "बुकिंग",
            Lang.KANNADA to "ಬುಕಿಂಗ್‌ಗಳು",
            Lang.TELUGU to "బుకింగ్‌లు",
            Lang.MALAYALAM to "ബുക്കിംഗുകൾ"
        ),
        "nav_ask_ray" to mapOf(
            Lang.ENGLISH to "Ask Ray",
            Lang.TAMIL to "ரேயிடம் கேளுங்கள்",
            Lang.HINDI to "रे से पूछें",
            Lang.KANNADA to "ರೇಯನ್ನು ಕೇಳಿ",
            Lang.TELUGU to "రేని అడగండి",
            Lang.MALAYALAM to "റേയോട് ചോദിക്കൂ"
        ),
        "nav_account" to mapOf(
            Lang.ENGLISH to "Account",
            Lang.TAMIL to "கணக்கு",
            Lang.HINDI to "खाता",
            Lang.KANNADA to "ಖಾತೆ",
            Lang.TELUGU to "ఖాతా",
            Lang.MALAYALAM to "അക്കൗണ്ട്"
        ),

        // Home Screen
        "book_bus_tickets" to mapOf(
            Lang.ENGLISH to "Book bus tickets",
            Lang.TAMIL to "பேருந்து டிக்கெட் முன்பதிவு",
            Lang.HINDI to "बस टिकट बुक करें",
            Lang.KANNADA to "ಬಸ್ ಟಿಕೆಟ್ ಕಾಯ್ದಿರಿಸಿ",
            Lang.TELUGU to "బస్సు టిక్కెట్లు బుక్ చేయండి",
            Lang.MALAYALAM to "ബസ് ടിക്കറ്റുകൾ ബുക്ക് ചെയ്യുക"
        ),
        "lowest_price_guaranteed" to mapOf(
            Lang.ENGLISH to "Lowest price guaranteed*",
            Lang.TAMIL to "குறைந்த கட்டண உத்தரவாதம்*",
            Lang.HINDI to "न्यूनतम मूल्य की गारंटी*",
            Lang.KANNADA to "ಕಡಿಮೆ ಬೆಲೆ ಖಾತರಿ*",
            Lang.TELUGU to "అతి తక్కువ ధర గ్యారెంటీ*",
            Lang.MALAYALAM to "ഏറ്റവും കുറഞ്ഞ നിരക്ക് ഗ്യാരണ്ടി*"
        ),
        "from" to mapOf(
            Lang.ENGLISH to "From",
            Lang.TAMIL to "புறப்படும் இடம்",
            Lang.HINDI to "कहाँ से",
            Lang.KANNADA to "ಇಲ್ಲಿಂದ",
            Lang.TELUGU to "నుండి",
            Lang.MALAYALAM to "എവിടെ നിന്ന്"
        ),
        "to" to mapOf(
            Lang.ENGLISH to "To",
            Lang.TAMIL to "சேரும் இடம்",
            Lang.HINDI to "कहाँ तक",
            Lang.KANNADA to "ಇಲ್ಲಿಗೆ",
            Lang.TELUGU to "వరకు",
            Lang.MALAYALAM to "എങ്ങോട്ട്"
        ),
        "date_of_journey" to mapOf(
            Lang.ENGLISH to "Date of Journey",
            Lang.TAMIL to "பயண தேதி",
            Lang.HINDI to "यात्रा की तारीख",
            Lang.KANNADA to "ಪ್ರಯಾಣ ದಿನಾಂಕ",
            Lang.TELUGU to "ప్రయాణ తేదీ",
            Lang.MALAYALAM to "യാത്രാ തീയതി"
        ),
        "today" to mapOf(
            Lang.ENGLISH to "Today",
            Lang.TAMIL to "இன்று",
            Lang.HINDI to "आज",
            Lang.KANNADA to "ಇಂದು",
            Lang.TELUGU to "ఈరోజు",
            Lang.MALAYALAM to "ഇന്ന്"
        ),
        "tomorrow" to mapOf(
            Lang.ENGLISH to "Tomorrow",
            Lang.TAMIL to "நாளை",
            Lang.HINDI to "कल",
            Lang.KANNADA to "ನಾಳೆ",
            Lang.TELUGU to "రేపు",
            Lang.MALAYALAM to "നാളെ"
        ),
        "booking_for_women" to mapOf(
            Lang.ENGLISH to "Booking for women",
            Lang.TAMIL to "பெண்களுக்கான முன்பதிவு",
            Lang.HINDI to "महिला आरक्षण",
            Lang.KANNADA to "ಮಹಿಳೆಯರ ಬುಕಿಂಗ್",
            Lang.TELUGU to "మహిళల కోసం బుకింగ్",
            Lang.MALAYALAM to "വനിതാ ബുക്കിംഗ്"
        ),
        "know_more" to mapOf(
            Lang.ENGLISH to "Know more",
            Lang.TAMIL to "மேலும் அறிய",
            Lang.HINDI to "अधिक जानें",
            Lang.KANNADA to "ಇನ್ನಷ್ಟು ತಿಳಿಯಿರಿ",
            Lang.TELUGU to "మరింత తెలుసుకోండి",
            Lang.MALAYALAM to "കൂടുതലറിയുക"
        ),
        "search_buses" to mapOf(
            Lang.ENGLISH to "Search buses",
            Lang.TAMIL to "பேருந்துகளைத் தேடுக",
            Lang.HINDI to "बसें खोजें",
            Lang.KANNADA to "ಬಸ್‌ಗಳನ್ನು ಹುಡುಕಿ",
            Lang.TELUGU to "బస్సులను శోధించండి",
            Lang.MALAYALAM to "ബസുകൾ തിരയുക"
        ),
        "upcoming_trips" to mapOf(
            Lang.ENGLISH to "Upcoming trips",
            Lang.TAMIL to "வரவிருக்கும் பயணங்கள்",
            Lang.HINDI to "आगामी यात्राएं",
            Lang.KANNADA to "ಮುಂಬರುವ ಪ್ರಯಾಣಗಳು",
            Lang.TELUGU to "రాబోయే ప్రయాణాలు",
            Lang.MALAYALAM to "വരാനിരിക്കുന്ന യാത്രകൾ"
        ),
        "verified_partners" to mapOf(
            Lang.ENGLISH to "AUTHORIZED & VERIFIED BUS PARTNERS",
            Lang.TAMIL to "அங்கீகரிக்கப்பட்ட & சரிபார்க்கப்பட்ட பேருந்து கூட்டாளர்கள்",
            Lang.HINDI to "अधिकृत और सत्यापित बस भागीदार",
            Lang.KANNADA to "ಅಧಿಕೃತ ಮತ್ತು ಪರಿಶೀಲಿಸಿದ ಬಸ್ ಪಾಲುದಾರರು",
            Lang.TELUGU to "అధికారిక & ధృవీకరించబడిన బస్సు భాగస్వాములు",
            Lang.MALAYALAM to "അംഗീകൃത & സ്ഥിരീകരിച്ച ബസ് പങ്കാളികൾ"
        ),
        "wallet" to mapOf(
            Lang.ENGLISH to "Wallet",
            Lang.TAMIL to "பணப்பை",
            Lang.HINDI to "वॉलेट",
            Lang.KANNADA to "ವ್ಯಾಲೆಟ್",
            Lang.TELUGU to "వాలెట్",
            Lang.MALAYALAM to "വാലറ്റ്"
        ),
        "my_account" to mapOf(
            Lang.ENGLISH to "My Account",
            Lang.TAMIL to "என் கணக்கு",
            Lang.HINDI to "मेरा खाता",
            Lang.KANNADA to "ನನ್ನ ಖಾತೆ",
            Lang.TELUGU to "నా ఖాతా",
            Lang.MALAYALAM to "എന്റെ അക്കൗണ്ട്"
        ),

        // Account Screen
        "try_redbus_in_your_language" to mapOf(
            Lang.ENGLISH to "Try redBus in your language",
            Lang.TAMIL to "உங்கள் மொழியில் redBus-ஐ முயற்சிக்கவும்",
            Lang.HINDI to "अपनी भाषा में redBus आज़माएं",
            Lang.KANNADA to "ನಿಮ್ಮ ಭಾಷೆಯಲ್ಲಿ redBus ಪ್ರಯತ್ನಿಸಿ",
            Lang.TELUGU to "మీ భాషలో redBus ఉపయోగించండి",
            Lang.MALAYALAM to "നിങ്ങളുടെ ഭാഷയിൽ redBus ഉപയോഗിക്കുക"
        ),
        "my_details" to mapOf(
            Lang.ENGLISH to "My details",
            Lang.TAMIL to "என் விவரங்கள்",
            Lang.HINDI to "मेरा विवरण",
            Lang.KANNADA to "ನನ್ನ ವಿವರಗಳು",
            Lang.TELUGU to "నా వివరాలు",
            Lang.MALAYALAM to "എന്റെ വിവരങ്ങൾ"
        ),
        "personal_information" to mapOf(
            Lang.ENGLISH to "Personal information",
            Lang.TAMIL to "தனிப்பட்ட தகவல்",
            Lang.HINDI to "व्यक्तिगत जानकारी",
            Lang.KANNADA to "ವೈಯಕ್ತಿಕ ಮಾಹಿತಿ",
            Lang.TELUGU to "వ్యక్తిగత సమాచారం",
            Lang.MALAYALAM to "വ്യക്തിഗത വിവരങ്ങൾ"
        ),
        "passengers" to mapOf(
            Lang.ENGLISH to "Passengers",
            Lang.TAMIL to "பயணிகள்",
            Lang.HINDI to "यात्री",
            Lang.KANNADA to "ಪ್ರಯಾಣಿಕರು",
            Lang.TELUGU to "ప్రయాణికులు",
            Lang.MALAYALAM to "യാത്രക്കാർ"
        ),
        "payments" to mapOf(
            Lang.ENGLISH to "Payments",
            Lang.TAMIL to "பணம் செலுத்துதல்",
            Lang.HINDI to "भुगतान",
            Lang.KANNADA to "ಪಾವತಿಗಳು",
            Lang.TELUGU to "చెల్లింపులు",
            Lang.MALAYALAM to "പേയ്‌മെന്റുകൾ"
        ),
        "redbus_wallet" to mapOf(
            Lang.ENGLISH to "redBus Wallet",
            Lang.TAMIL to "redBus பணப்பை",
            Lang.HINDI to "redBus वॉलेट",
            Lang.KANNADA to "redBus ವ್ಯಾಲೆಟ್",
            Lang.TELUGU to "redBus వాలెట్",
            Lang.MALAYALAM to "redBus വാലറ്റ്"
        ),
        "more" to mapOf(
            Lang.ENGLISH to "More",
            Lang.TAMIL to "மேலும்",
            Lang.HINDI to "अधिक",
            Lang.KANNADA to "ಇನ್ನಷ್ಟು",
            Lang.TELUGU to "మరిన్ని",
            Lang.MALAYALAM to "കൂടുതൽ"
        ),
        "help" to mapOf(
            Lang.ENGLISH to "Help",
            Lang.TAMIL to "உதவி",
            Lang.HINDI to "सहायता",
            Lang.KANNADA to "ಸಹಾಯ",
            Lang.TELUGU to "సహాయం",
            Lang.MALAYALAM to "സഹായം"
        ),
        "account_settings" to mapOf(
            Lang.ENGLISH to "Account settings",
            Lang.TAMIL to "கணக்கு அமைப்புகள்",
            Lang.HINDI to "खाता सेटिंग्स",
            Lang.KANNADA to "ಖಾತೆ ಸೆಟ್ಟಿಂಗ್‌ಗಳು",
            Lang.TELUGU to "ఖాతా సెట్టింగ్‌లు",
            Lang.MALAYALAM to "അക്കൗണ്ട് ക്രമീകരണങ്ങൾ"
        ),
        "logout" to mapOf(
            Lang.ENGLISH to "Logout",
            Lang.TAMIL to "வெளியேறு",
            Lang.HINDI to "लॉग आउट",
            Lang.KANNADA to "ಲಾಗ್ ಔಟ್",
            Lang.TELUGU to "లాగ్ అవుట్",
            Lang.MALAYALAM to "ലോഗ് ഔട്ട്"
        ),

        // Search Results Screen
        "buses_found" to mapOf(
            Lang.ENGLISH to "buses found",
            Lang.TAMIL to "பேருந்துகள் உள்ளன",
            Lang.HINDI to "बसें मिलीं",
            Lang.KANNADA to "ಬಸ್‌ಗಳು ಸಿಕ್ಕಿವೆ",
            Lang.TELUGU to "బస్సులు దొరికాయి",
            Lang.MALAYALAM to "ബസുകൾ ലഭ്യമാണ്"
        ),
        "sort_by" to mapOf(
            Lang.ENGLISH to "Sort by:",
            Lang.TAMIL to "வரிசைப்படுத்துக:",
            Lang.HINDI to "क्रमबद्ध करें:",
            Lang.KANNADA to "ವಿಂಗಡಿಸಿ:",
            Lang.TELUGU to "క్రమబద్ధీకరించు:",
            Lang.MALAYALAM to "തരംതിരിക്കുക:"
        ),
        "ratings" to mapOf(
            Lang.ENGLISH to "Ratings",
            Lang.TAMIL to "மதிப்பீடு",
            Lang.HINDI to "रेटिंग्स",
            Lang.KANNADA to "ರೇಟಿಂಗ್‌ಗಳು",
            Lang.TELUGU to "రేటింగ్‌లు",
            Lang.MALAYALAM to "റേറ്റിംഗുകൾ"
        ),
        "departure_time" to mapOf(
            Lang.ENGLISH to "Departure time",
            Lang.TAMIL to "புறப்படும் நேரம்",
            Lang.HINDI to "प्रस्थान समय",
            Lang.KANNADA to "ಹೊರಡುವ ಸಮಯ",
            Lang.TELUGU to "బయలుదేరే సమయం",
            Lang.MALAYALAM to "പുറപ്പെടുന്ന സമയം"
        ),
        "price" to mapOf(
            Lang.ENGLISH to "Price",
            Lang.TAMIL to "கட்டணம்",
            Lang.HINDI to "किराया",
            Lang.KANNADA to "ದರ",
            Lang.TELUGU to "ధర",
            Lang.MALAYALAM to "നിരക്ക്"
        ),
        "filter_buses" to mapOf(
            Lang.ENGLISH to "Filter buses",
            Lang.TAMIL to "வடிகட்டுதல்",
            Lang.HINDI to "फ़िल्टर करें",
            Lang.KANNADA to "ಫಿಲ್ಟರ್ ಮಾಡಿ",
            Lang.TELUGU to "ఫిల్టర్ చేయండి",
            Lang.MALAYALAM to "ഫിൽട്ടർ ചെയ്യുക"
        ),
        "all_filters" to mapOf(
            Lang.ENGLISH to "All Filters",
            Lang.TAMIL to "அனைத்து வடிகட்டிகள்",
            Lang.HINDI to "सभी फ़िल्टर",
            Lang.KANNADA to "ಎಲ್ಲಾ ಫಿಲ್ಟರ್‌ಗಳು",
            Lang.TELUGU to "అన్ని ఫిల్టర్‌లు",
            Lang.MALAYALAM to "എല്ലാ ഫിൽട്ടറുകളും"
        ),
        "apply_filters" to mapOf(
            Lang.ENGLISH to "Apply Filters",
            Lang.TAMIL to "வடிகட்டிகளைப் பயன்படுத்துக",
            Lang.HINDI to "फ़िल्टर लागू करें",
            Lang.KANNADA to "ಫಿಲ್ಟರ್ ಅನ್ವಯಿಸಿ",
            Lang.TELUGU to "ఫిల్టర్‌లను వర్తింపజేయండి",
            Lang.MALAYALAM to "ഫിൽട്ടറുകൾ പ്രയോഗിക്കുക"
        ),
        "view_seats" to mapOf(
            Lang.ENGLISH to "View Seats",
            Lang.TAMIL to "இருக்கைகளைப் பார்க்க",
            Lang.HINDI to "सीटें देखें",
            Lang.KANNADA to "ಸೀಟ್‌ಗಳನ್ನು ನೋಡಿ",
            Lang.TELUGU to "సీట్లు చూడండి",
            Lang.MALAYALAM to "സീറ്റുകൾ കാണുക"
        ),
        "seats_left" to mapOf(
            Lang.ENGLISH to "seats left",
            Lang.TAMIL to "இருக்கைகள் உள்ளன",
            Lang.HINDI to "सीटें शेष",
            Lang.KANNADA to "ಸೀಟುಗಳು ಲಭ್ಯವಿದೆ",
            Lang.TELUGU to "సీట్లు మిగిలి ఉన్నాయి",
            Lang.MALAYALAM to "സീറ്റുകൾ ബാക്കി"
        ),
        "ask_ray_btn" to mapOf(
            Lang.ENGLISH to "Ask Ray",
            Lang.TAMIL to "ரேயிடம் கேளுங்கள்",
            Lang.HINDI to "रे से पूछें",
            Lang.KANNADA to "ರೇಯನ್ನು ಕೇಳಿ",
            Lang.TELUGU to "రేని అడగండి",
            Lang.MALAYALAM to "റേയോട് ചോദിക്കൂ"
        ),

        // Ray Assistant Screen
        "ray_assistant_title" to mapOf(
            Lang.ENGLISH to "Ray Travel Assistant",
            Lang.TAMIL to "ரே டிராவல் அசிஸ்டண்ட்",
            Lang.HINDI to "रे ट्रेवल असिस्टेंट",
            Lang.KANNADA to "ರೇ ಟ್ರಾವೆಲ್ ಅಸಿಸ್ಟೆಂಟ್",
            Lang.TELUGU to "రే ట్రావెల్ అసిస్టెంట్",
            Lang.MALAYALAM to "റേ ട്രാവൽ അസിസ്റ്റന്റ്"
        ),
        "ray_subtitle" to mapOf(
            Lang.ENGLISH to "Smart Seat Advisor • Delay Predictor • Fare Optimizer",
            Lang.TAMIL to "ஸ்மார்ட் இருக்கை வழிகாட்டி • தாமத கணிப்பாளர் • கட்டண தேர்வு",
            Lang.HINDI to "स्मार्ट सीट सलाहकार • विलंब भविष्यवक्ता • किराया अनुकूलक",
            Lang.KANNADA to "ಸ್ಮಾರ್ಟ್ ಸೀಟ್ ಸಲಹೆಗಾರ • ವಿಳಂಬ ಭವಿಷ್ಯವಾಣಿ",
            Lang.TELUGU to "స్మార్ట్ సీట్ అడ్వైజర్ • ఆలస్య ప్రిడిక్టర్",
            Lang.MALAYALAM to "സ്മാർട്ട് സീറ്റ് ഉപദേശകൻ • യാത്രാ വിവരങ്ങൾ"
        ),
        "ask_ray_placeholder" to mapOf(
            Lang.ENGLISH to "Ask Ray about seats, delays, or coupons...",
            Lang.TAMIL to "இருக்கைகள், தாமதங்கள் அல்லது கூப்பன்கள் பற்றி ரேயிடம் கேளுங்கள்...",
            Lang.HINDI to "सीटों, देरी या कूपन के बारे में रे से पूछें...",
            Lang.KANNADA to "ಸೀಟ್‌ಗಳು, ವಿಳಂಬ ಅಥವಾ ಕೂಪನ್‌ಗಳ ಬಗ್ಗೆ ರೇಯನ್ನು ಕೇಳಿ...",
            Lang.TELUGU to "సీట్లు, ఆలస్యాలు లేదా కూపన్‌ల గురించి రేని అడగండి...",
            Lang.MALAYALAM to "സീറ്റുകൾ, വൈകൽ അല്ലെങ്കിൽ കൂപ്പണുകളെ കുറിച്ച് ചോദിക്കൂ..."
        ),

        // Bookings Screen
        "my_bookings" to mapOf(
            Lang.ENGLISH to "My Bookings",
            Lang.TAMIL to "என் முன்பதிவுகள்",
            Lang.HINDI to "मेरी बुकिंग",
            Lang.KANNADA to "ನನ್ನ ಬುಕಿಂಗ್‌ಗಳು",
            Lang.TELUGU to "నా బుకింగ్‌లు",
            Lang.MALAYALAM to "എന്റെ ബുക്കിംഗുകൾ"
        ),
        "past_trips" to mapOf(
            Lang.ENGLISH to "Past trips",
            Lang.TAMIL to "கடந்த பயணங்கள்",
            Lang.HINDI to "पिछली यात्राएं",
            Lang.KANNADA to "ಹಿಂದಿನ ಪ್ರಯಾಣಗಳು",
            Lang.TELUGU to "గత ప్రయాణాలు",
            Lang.MALAYALAM to "കഴിഞ്ഞ യാത്രകൾ"
        ),
        "cancelled_trips" to mapOf(
            Lang.ENGLISH to "Cancelled trips",
            Lang.TAMIL to "ரத்து செய்யப்பட்டவை",
            Lang.HINDI to "रद्द की गई यात्राएं",
            Lang.KANNADA to "ರದ್ದುಗೊಂಡ ಪ್ರಯಾಣಗಳು",
            Lang.TELUGU to "రద్దు చేసిన ప్రయాణాలు",
            Lang.MALAYALAM to "റദ്ദാക്കിയ യാത്രകൾ"
        ),
        "view_ticket" to mapOf(
            Lang.ENGLISH to "View Ticket",
            Lang.TAMIL to "டிக்கெட்டைக் காண்க",
            Lang.HINDI to "टिकट देखें",
            Lang.KANNADA to "ಟಿಕೆಟ್ ನೋಡಿ",
            Lang.TELUGU to "టికెట్ చూడండి",
            Lang.MALAYALAM to "ടിക്കറ്റ് കാണുക"
        ),
        "share_ticket" to mapOf(
            Lang.ENGLISH to "Share your ticket",
            Lang.TAMIL to "உங்கள் டிக்கெட்டைப் பகிர்க",
            Lang.HINDI to "अपना टिकट साझा करें",
            Lang.KANNADA to "ನಿಮ್ಮ ಟಿಕೆಟ್ ಹಂಚಿಕೊಳ್ಳಿ",
            Lang.TELUGU to "మీ టిక్కెట్‌ను షేర్ చేయండి",
            Lang.MALAYALAM to "നിങ്ങളുടെ ടിക്കറ്റ് പങ്കിടുക"
        )
    )

    fun get(key: String, selectedLanguage: String?): String {
        val lang = Lang.parse(selectedLanguage)
        val dict = translations[key] ?: return key
        return dict[lang] ?: dict[Lang.ENGLISH] ?: key
    }
}
