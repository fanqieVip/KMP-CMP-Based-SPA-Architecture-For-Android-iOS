package com.basic.base.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object DateUtils {
    /**
     * 获取当前时间
     */
    @OptIn(ExperimentalTime::class)
    fun getNowTime(): LocalDateTime {
        val now = Clock.System.now()
        return now.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    /**
     * 毫秒时间戳转LocalDateTime
     */
    fun Long.toLocalDateTimeSeconds(
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDateTime {
        return Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)
    }

    /**
     * 获取转换后的时间格式（yyyy-MM-dd HH:mm:ss）
     */
    fun getFormatTime(dateTime: LocalDateTime): String {
        return "${dateTime.year.toString().padStart(4, '0')}-" +
                "${dateTime.month.number.toString().padStart(2, '0')}-" +
                "${dateTime.day.toString().padStart(2, '0')} " +
                "${dateTime.hour.toString().padStart(2, '0')}:" +
                "${dateTime.minute.toString().padStart(2, '0')}:" +
                dateTime.second.toString().padStart(2, '0')
    }
}