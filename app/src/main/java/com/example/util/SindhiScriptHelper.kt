package com.example.util

object SindhiScriptHelper {

    // Unique Sindhi Arabic script letters that are often hard to type without a special keyboard
    val SINDHI_SPECIAL_LETTERS = listOf(
        "ٻ" to "ٻ (Bbe)",
        "ڀ" to "ڀ (Bhe)",
        "ٿ" to "ٿ (The)",
        "ٺ" to "ٺ (Tthe)",
        "ٽ" to "ٽ (Te)",
        "ڄ" to "ڄ (Jje)",
        "ڃ" to "ڃ (Nye)",
        "ڇ" to "ڇ (Che)",
        "ڌ" to "ڌ (Dhal)",
        "ڍ" to "ڍ (Dhad)",
        "ڊ" to "ڊ (Dal)",
        "ڏ" to "ڏ (Ddal)",
        "ڙ" to "ڙ (Rre)",
        "ڦ" to "ڦ (Phe)",
        "ڪ" to "ڪ (Kaf)",
        "ڳ" to "ڳ (Gge)",
        "ڱ" to "ڱ (Nge)",
        "ڻ" to "ڻ (Nnoon)"
    )

    // Sindhi diacritic marks
    val SINDHI_DIACRITICS = listOf(
        "َ" to "زبر (Zabar)",
        "ِ" to "زير (Zer)",
        "ُ" to "پيش (Pesh)",
        "ّ" to "تشديد (Tashdeed)",
        "ً" to "ٻه زبر (Tanween)",
        "ء" to "همزو (Hamza)",
        "ـ" to "ڪشيده (Kashida)"
    )

    val SAMPLE_SINDHI_DOCUMENTS = listOf(
        SampleDocument(
            title = "شاهه عبداللطيف ڀٽائي - سر سسئي",
            englishTitle = "Shah Abdul Latif Bhittai - Sur Sasui",
            sindhiText = """
ويا جي پهڻ لتاڙي، سي پير نه ڏسان پانڌي!
ڏونگر ڏکوين کي، روئي ڏئي روءِ!
جي هئا هڏ وڃڻ جا، سي پڄي پهتا پار!
سسئي سڀ ويچار، ڏسندي ڏک اڳيان ٿيا!

پيرين پيئي نه پڄان، ويٺي واٽ مٺي،
ڪنهن کي چوان ڪير ٻڌي، هيءَ ويڌن ويل وٺي!
            """.trimIndent()
        ),
        SampleDocument(
            title = "سنڌي ٻوليءَ جو تاريخي پس منظر",
            englishTitle = "Historical Background of Sindhi Language",
            sindhiText = """
سنڌي ٻولي دنيا جي قديم ترين ٻولين مان هڪ آهي، جنهن جون پاڙون موهن جي دڙي ۽ سنڌو تهذيب تائين وڃي پهچن ٿيون. سنڌي لپي ۾ 52 اکر آهن، جيڪي عربي رسم الخط مان تيار ڪيا ويا آهن ۽ ان ۾ مخصوص سنڌي آوازن لاءِ اضافي اکر شامل آهن.
            """.trimIndent()
        )
    )

    data class SampleDocument(
        val title: String,
        val englishTitle: String,
        val sindhiText: String
    )
}
