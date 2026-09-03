package moe.evil.hyperstatus.xposed.model

enum class StatusIconGroup(val cli: String) {
    RINGER("ringer"),
    HEADSET("headset"),
    BLUETOOTH("bluetooth"),
    ALARM("alarm"),
    LOCATION("location"),
    VOLTE("volte"),
}

enum class BluetoothTransfer { IN, OUT, INOUT }

data class StatusIconSwap(val host: String, val group: StatusIconGroup, val assets: List<String>)
