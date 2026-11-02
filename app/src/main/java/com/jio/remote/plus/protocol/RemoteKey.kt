package com.jio.remote.plus.protocol

enum class RemoteKey(val keyCode: Int, val description: String) {
    POWER(26, "Power"),
    UP(19, "DPad Up"),
    DOWN(20, "DPad Down"),
    LEFT(21, "DPad Left"),
    RIGHT(22, "DPad Right"),
    CENTER(23, "DPad OK"),
    BACK(4, "Back"),
    HOME(3, "Home"),
    MENU(82, "Menu"),
    VOLUME_UP(24, "Volume Up"),
    VOLUME_DOWN(25, "Volume Down"),
    MUTE(164, "Mute"),
    CHANNEL_UP(166, "Channel Up"),
    CHANNEL_DOWN(167, "Channel Down"),
    PLAY_PAUSE(85, "Play/Pause"),
    VOICE_SEARCH(84, "Voice Search"),
    SETTINGS(176, "Settings"),
    GUIDE(172, "Guide / Live TV"),
    INFO(165, "Info"),
    REWIND(89, "Rewind"),
    FAST_FORWARD(90, "Fast Forward")
}

data class RemoteAppShortcut(
    val id: String,
    val name: String,
    val packageName: String,
    val activityName: String? = null,
    val primaryColorHex: Long,
    val secondaryColorHex: Long
)

object KnownJioApps {
    val YOUTUBE = RemoteAppShortcut(
        id = "youtube",
        name = "YouTube",
        packageName = "com.google.android.youtube.tv",
        primaryColorHex = 0xFFFF0000,
        secondaryColorHex = 0xFF282828
    )
    val JIOCINEMA = RemoteAppShortcut(
        id = "jiocinema",
        name = "JioCinema",
        packageName = "com.jio.media.ondemand",
        primaryColorHex = 0xFFD81B60,
        secondaryColorHex = 0xFF1E1E2E
    )
    val HOTSTAR = RemoteAppShortcut(
        id = "hotstar",
        name = "Disney+ Hotstar",
        packageName = "in.startv.hotstar",
        primaryColorHex = 0xFF0C2040,
        secondaryColorHex = 0xFF143058
    )
    val NETFLIX = RemoteAppShortcut(
        id = "netflix",
        name = "Netflix",
        packageName = "com.netflix.ninja",
        primaryColorHex = 0xFFE50914,
        secondaryColorHex = 0xFF141414
    )
    val JIOTV_PLUS = RemoteAppShortcut(
        id = "jiotvplus",
        name = "JioTV+",
        packageName = "com.jio.media.jiotvplus",
        primaryColorHex = 0xFF0055FF,
        secondaryColorHex = 0xFF0A2266
    )

    val QUICK_APPS = listOf(YOUTUBE, JIOCINEMA, HOTSTAR, NETFLIX)
}
