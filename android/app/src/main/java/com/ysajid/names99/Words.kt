package com.ysajid.names99

/**
 * Everything on screen that is not one of the 99 names. Kept here rather than
 * in strings.xml because the toggle is inside the app, not the system locale:
 * a child switching to English should not have to change the whole tablet.
 */
class Words(private val lang: Lang) {

    private fun pick(bn: String, en: String) = if (lang == Lang.BN) bn else en

    val pickWho get() = pick("কে খেলবে?", "Who is playing?")
    val pickSub get() = pick("নাম বেছে নাও, বা নতুন নাম লিখো।", "Pick a name, or add a new one.")
    val newName get() = pick("নতুন নাম", "New name")
    val add get() = pick("যোগ করো", "Add")

    val journey get() = pick("৯৯ নামের যাত্রা", "The 99 Names Journey")
    val subtitle get() = pick("এগারো দ্বীপ · প্রতি রাতে একটা", "Eleven islands · one a night")
    val locked get() = pick("আগের দ্বীপটা আগে শেষ করো", "Finish the island before this one")
    val viewAll get() = pick("৯৯টি নাম দেখো", "See all 99 names")

    val back get() = pick("আগেরটা", "Back")
    val next get() = pick("পরেরটা", "Next")
    val toGame get() = pick("এখন খেলা", "Play the game")
    val listen get() = pick("শোনো", "Listen")
    val todayLabel get() = pick("আজকে এটা করো", "Try this today")

    val gameTitle get() = pick("মিলাও", "Match them up")
    val gameSub get() = pick("নামের সাথে মানে মিলাও।", "Match each name to what it means.")

    val wellDone get() = pick("দ্বীপটা জ্বলে উঠল!", "The island is lit!")
    val finishSub
        get() = pick(
            "আজকের কাজটা মনে রেখো। কালকে আবার নতুন দ্বীপ।",
            "Remember today's one thing. A new island tomorrow.",
        )
    val lastIsland
        get() = pick(
            "এগারোটা দ্বীপ শেষ! সবগুলো নাম এখন তোমার।",
            "All eleven islands done! Every name is yours now.",
        )
    val backToMap get() = pick("মানচিত্রে ফিরে যাও", "Back to the map")

    val allNames get() = pick("৯৯টি নাম", "The 99 Names")
    val allSub get() = pick("চিরাচরিত ক্রমে সাজানো", "In the traditional order")
    val close get() = pick("বন্ধ করো", "Close")
    val cancel get() = pick("থাক", "Cancel")
    val delete get() = pick("মুছে ফেলো", "Delete")

    fun islandsDone(done: Int, total: Int) = pick(
        "${done.inLang(lang)} / ${total.inLang(lang)} দ্বীপ শেষ",
        "$done of $total islands lit",
    )

    fun playingAs(who: String) = pick("$who খেলছে", "Playing as $who")

    fun nth(island: Int) = pick("${island.inLang(lang)} নম্বর দ্বীপ", "Island $island")

    fun round(at: Int, of: Int) = pick(
        "রাউন্ড ${at.inLang(lang)} / ${of.inLang(lang)}",
        "Round $at of $of",
    )

    fun cardCount(at: Int, of: Int) = "${at.inLang(lang)}/${of.inLang(lang)}"

    fun removeQuestion(who: String) = pick(
        "$who-এর সব অগ্রগতি মুছে ফেলবে?",
        "Delete all of $who's progress?",
    )
}
