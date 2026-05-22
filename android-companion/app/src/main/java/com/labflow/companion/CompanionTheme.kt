package com.labflow.companion

import android.graphics.Color

enum class CompanionPalette(val displayName: String) {
    RED("Red"),
    GREEN("Green"),
    PURPLE("Purple"),
    BLUE("Blue")
}

enum class CompanionMode {
    DARK,
    LIGHT
}

data class CompanionColors(
    val background: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val border: Int,
    val primary: Int,
    val accent: Int,
    val accentStrong: Int,
    val foreground: Int,
    val muted: Int,
    val danger: Int,
    val success: Int,
    val warning: Int,
    val cardOverlay: Int,
    val starGlowA: Int,
    val starGlowB: Int,
    val starGlowC: Int,
    val starColor: Int
)

object CompanionTheme {
    fun resolve(palette: CompanionPalette, mode: CompanionMode): CompanionColors {
        val dark = mode == CompanionMode.DARK
        return when (palette) {
            CompanionPalette.RED -> if (dark) {
                CompanionColors(
                    background = Color.rgb(16, 7, 11),
                    surface = Color.rgb(24, 16, 20),
                    surfaceAlt = Color.rgb(33, 22, 27),
                    border = Color.rgb(64, 36, 47),
                    primary = Color.rgb(229, 72, 99),
                    accent = Color.rgb(192, 57, 74),
                    accentStrong = Color.rgb(107, 15, 26),
                    foreground = Color.rgb(248, 250, 252),
                    muted = Color.rgb(167, 175, 186),
                    danger = Color.rgb(255, 122, 140),
                    success = Color.rgb(84, 214, 161),
                    warning = Color.rgb(242, 184, 75),
                    cardOverlay = Color.argb(68, 229, 72, 99),
                    starGlowA = Color.argb(92, 107, 15, 26),
                    starGlowB = Color.argb(42, 192, 57, 74),
                    starGlowC = Color.argb(50, 242, 167, 177),
                    starColor = Color.rgb(242, 231, 235)
                )
            } else {
                CompanionColors(
                    background = Color.rgb(255, 247, 248),
                    surface = Color.rgb(255, 255, 255),
                    surfaceAlt = Color.rgb(255, 240, 242),
                    border = Color.rgb(234, 205, 212),
                    primary = Color.rgb(107, 15, 26),
                    accent = Color.rgb(156, 37, 50),
                    accentStrong = Color.rgb(49, 8, 31),
                    foreground = Color.rgb(42, 7, 22),
                    muted = Color.rgb(140, 89, 104),
                    danger = Color.rgb(180, 35, 47),
                    success = Color.rgb(15, 159, 110),
                    warning = Color.rgb(183, 121, 0),
                    cardOverlay = Color.argb(46, 156, 37, 50),
                    starGlowA = Color.argb(54, 242, 167, 177),
                    starGlowB = Color.argb(36, 107, 15, 26),
                    starGlowC = Color.argb(28, 156, 37, 50),
                    starColor = Color.rgb(103, 41, 55)
                )
            }

            CompanionPalette.GREEN -> if (dark) {
                CompanionColors(
                    background = Color.rgb(7, 17, 10),
                    surface = Color.rgb(16, 26, 19),
                    surfaceAlt = Color.rgb(23, 37, 26),
                    border = Color.rgb(42, 70, 49),
                    primary = Color.rgb(13, 171, 118),
                    accent = Color.rgb(19, 154, 67),
                    accentStrong = Color.rgb(5, 59, 6),
                    foreground = Color.rgb(248, 250, 252),
                    muted = Color.rgb(167, 183, 173),
                    danger = Color.rgb(255, 122, 140),
                    success = Color.rgb(34, 200, 119),
                    warning = Color.rgb(242, 184, 75),
                    cardOverlay = Color.argb(70, 13, 171, 118),
                    starGlowA = Color.argb(76, 13, 171, 118),
                    starGlowB = Color.argb(44, 19, 154, 67),
                    starGlowC = Color.argb(34, 11, 93, 30),
                    starColor = Color.rgb(226, 243, 233)
                )
            } else {
                CompanionColors(
                    background = Color.rgb(242, 248, 243),
                    surface = Color.rgb(255, 255, 255),
                    surfaceAlt = Color.rgb(234, 245, 238),
                    border = Color.rgb(201, 223, 205),
                    primary = Color.rgb(19, 154, 67),
                    accent = Color.rgb(13, 171, 118),
                    accentStrong = Color.rgb(5, 59, 6),
                    foreground = Color.rgb(6, 27, 8),
                    muted = Color.rgb(83, 106, 89),
                    danger = Color.rgb(168, 50, 62),
                    success = Color.rgb(19, 154, 67),
                    warning = Color.rgb(138, 101, 0),
                    cardOverlay = Color.argb(44, 13, 171, 118),
                    starGlowA = Color.argb(46, 19, 154, 67),
                    starGlowB = Color.argb(30, 13, 171, 118),
                    starGlowC = Color.argb(22, 11, 93, 30),
                    starColor = Color.rgb(36, 74, 43)
                )
            }

            CompanionPalette.PURPLE -> if (dark) {
                CompanionColors(
                    background = Color.rgb(13, 14, 24),
                    surface = Color.rgb(21, 20, 33),
                    surfaceAlt = Color.rgb(30, 26, 45),
                    border = Color.rgb(60, 53, 81),
                    primary = Color.rgb(185, 146, 255),
                    accent = Color.rgb(115, 83, 186),
                    accentStrong = Color.rgb(47, 25, 95),
                    foreground = Color.rgb(248, 250, 252),
                    muted = Color.rgb(170, 166, 186),
                    danger = Color.rgb(255, 122, 140),
                    success = Color.rgb(99, 230, 168),
                    warning = Color.rgb(242, 184, 75),
                    cardOverlay = Color.argb(64, 115, 83, 186),
                    starGlowA = Color.argb(82, 47, 25, 95),
                    starGlowB = Color.argb(42, 185, 146, 255),
                    starGlowC = Color.argb(38, 250, 166, 255),
                    starColor = Color.rgb(234, 228, 250)
                )
            } else {
                CompanionColors(
                    background = Color.rgb(248, 243, 250),
                    surface = Color.rgb(255, 255, 255),
                    surfaceAlt = Color.rgb(241, 234, 247),
                    border = Color.rgb(213, 202, 233),
                    primary = Color.rgb(115, 83, 186),
                    accent = Color.rgb(183, 128, 225),
                    accentStrong = Color.rgb(47, 25, 95),
                    foreground = Color.rgb(18, 11, 34),
                    muted = Color.rgb(102, 91, 119),
                    danger = Color.rgb(180, 35, 47),
                    success = Color.rgb(24, 121, 78),
                    warning = Color.rgb(138, 101, 0),
                    cardOverlay = Color.argb(42, 115, 83, 186),
                    starGlowA = Color.argb(44, 183, 128, 225),
                    starGlowB = Color.argb(32, 115, 83, 186),
                    starGlowC = Color.argb(26, 250, 166, 255),
                    starColor = Color.rgb(59, 43, 90)
                )
            }

            CompanionPalette.BLUE -> if (dark) {
                CompanionColors(
                    background = Color.rgb(7, 17, 29),
                    surface = Color.rgb(16, 25, 39),
                    surfaceAlt = Color.rgb(23, 35, 55),
                    border = Color.rgb(47, 68, 95),
                    primary = Color.rgb(6, 190, 225),
                    accent = Color.rgb(23, 104, 172),
                    accentStrong = Color.rgb(3, 37, 108),
                    foreground = Color.rgb(248, 250, 252),
                    muted = Color.rgb(165, 179, 195),
                    danger = Color.rgb(255, 122, 140),
                    success = Color.rgb(99, 223, 160),
                    warning = Color.rgb(242, 184, 75),
                    cardOverlay = Color.argb(72, 23, 104, 172),
                    starGlowA = Color.argb(84, 3, 37, 108),
                    starGlowB = Color.argb(44, 37, 65, 178),
                    starGlowC = Color.argb(36, 6, 190, 225),
                    starColor = Color.rgb(229, 242, 250)
                )
            } else {
                CompanionColors(
                    background = Color.rgb(242, 248, 253),
                    surface = Color.rgb(255, 255, 255),
                    surfaceAlt = Color.rgb(234, 244, 251),
                    border = Color.rgb(198, 217, 236),
                    primary = Color.rgb(23, 104, 172),
                    accent = Color.rgb(6, 190, 225),
                    accentStrong = Color.rgb(3, 37, 108),
                    foreground = Color.rgb(4, 26, 52),
                    muted = Color.rgb(88, 105, 123),
                    danger = Color.rgb(180, 35, 47),
                    success = Color.rgb(22, 125, 78),
                    warning = Color.rgb(138, 101, 0),
                    cardOverlay = Color.argb(46, 23, 104, 172),
                    starGlowA = Color.argb(48, 6, 190, 225),
                    starGlowB = Color.argb(30, 37, 65, 178),
                    starGlowC = Color.argb(24, 23, 104, 172),
                    starColor = Color.rgb(33, 70, 109)
                )
            }
        }
    }
}
