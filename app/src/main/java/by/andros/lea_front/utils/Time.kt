package by.andros.lea_front.utils

fun getDaysSinceEpoch(): Long {
    val millis = System.currentTimeMillis()
    return millis / (1000 * 60 * 60 * 24)
}