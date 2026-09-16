package com.ysajid.names99

/**
 * Where everything sits on the chart.
 *
 * Kept free of Compose so it can be checked without a device: the bug this
 * guards against is label plates colliding on a narrow screen, which is a
 * matter of arithmetic, not of rendering.
 *
 * Units match the web build: the ocean is drawn on a 100 x 300 box and
 * stretched to fill, while each island keeps its own shape.
 */
object Chart {

    const val SEA_W = 100f
    const val SEA_H = 300f

    /** An island's art box, as a fraction of the chart width, before `size`. */
    const val ISLE_FRACTION = 0.26f * 1.32f

    /**
     * An island stops growing past this. Without a cap the art keeps pace with
     * the screen, so on a tablet the islands turn enormous and the chart has
     * to run nearly five screens long to keep their labels apart.
     */
    const val ART_MAX_DP = 190f

    /** The label is a little wider than the art, and never narrower than this. */
    const val PLATE_OF_ART = 1.04f
    const val PLATE_MIN_DP = 124f

    /** How far the label is tucked up over the bottom of the island. */
    const val PLATE_TUCK = 0.18f

    /** A plate is about this tall with a number, a name and three stars in it. */
    const val PLATE_H_DP = 58f

    /** Clear water to leave between one island's label and the next island. */
    const val GAP_PAD_DP = 12f

    fun artSizeDp(isl: Island, widthDp: Float): Float =
        minOf(widthDp * ISLE_FRACTION * isl.size, ART_MAX_DP * isl.size)

    fun plateWidthDp(isl: Island, widthDp: Float): Float =
        maxOf(artSizeDp(isl, widthDp) * PLATE_OF_ART, PLATE_MIN_DP)

    /** Island plus the label hanging below it. */
    fun boxHeightDp(isl: Island, widthDp: Float, plateHeightDp: Float = PLATE_H_DP): Float =
        artSizeDp(isl, widthDp) * (1f - PLATE_TUCK) + plateHeightDp

    /** How far apart the closest pair of islands are, as a fraction of the route. */
    fun minGap(islands: List<Island>): Float =
        islands.zipWithNext { a, b -> b.y - a.y }.minOrNull() ?: 1f

    /**
     * The shortest chart on which no label reaches the island below it.
     *
     * Checked pair by pair rather than worst-gap against tallest-island: those
     * two are not the same pair, and assuming they were made the chart far
     * longer to scroll than it needs to be.
     */
    fun heightToFit(
        widthDp: Float,
        islands: List<Island>,
        plateHeightDp: Float = PLATE_H_DP,
    ): Float = islands.zipWithNext { above, below ->
        val gap = below.y - above.y
        if (gap <= 0f) 0f else (boxHeightDp(above, widthDp, plateHeightDp) + GAP_PAD_DP) / gap
    }.maxOrNull() ?: 0f

    /** The shape the chart wants on a roomy screen, purely for looks. */
    private fun baseRatio(widthDp: Float): Float = when {
        widthDp >= 700f -> 2.6f
        widthDp >= 560f -> 3.1f
        else -> 4.4f
    }

    /**
     * The chart's height.
     *
     * Island spacing is a fraction of the chart height while labels are a
     * fixed size in dp, so a short chart on a small screen is exactly where
     * labels start to collide. Rather than pick breakpoints and hope, the
     * height is grown until the closest pair of islands has room for the
     * tallest label between them. On a roomy screen the looks win instead.
     */
    fun chartHeightDp(
        widthDp: Float,
        islands: List<Island>,
        plateHeightDp: Float = PLATE_H_DP,
    ): Float {
        val forLooks = widthDp * baseRatio(widthDp)
        return maxOf(forLooks, heightToFit(widthDp, islands, plateHeightDp))
    }

    /** Centre of the island, in dp from the chart's top-left. */
    fun centreDp(isl: Island, widthDp: Float, chartHeightDp: Float): Pair<Float, Float> =
        isl.x * widthDp to isl.y * chartHeightDp

    /**
     * The box one island occupies: its art, plus the label hanging below.
     * Returned as left, top, right, bottom in dp.
     */
    fun boundsDp(
        isl: Island,
        widthDp: Float,
        chartHeightDp: Float,
        plateHeightDp: Float = PLATE_H_DP,
    ): FloatArray {
        val (cx, cy) = centreDp(isl, widthDp, chartHeightDp)
        val art = artSizeDp(isl, widthDp)
        val plateW = plateWidthDp(isl, widthDp)
        val top = cy - art / 2f
        val plateTop = top + art - art * PLATE_TUCK
        return floatArrayOf(
            minOf(cx - art / 2f, cx - plateW / 2f),
            top,
            maxOf(cx + art / 2f, cx + plateW / 2f),
            plateTop + plateHeightDp,
        )
    }

    /** One leg of the route, bent so it looks sailed rather than ruled. */
    fun legPull(index: Int): Float = if (index % 2 == 1) 7f else -7f
}
