package com.example.investcalc.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

/** Работа с датами в московском времени: торги MOEX привязаны к MSK, а не к часовому поясу сервера. */
public final class DateUtils {

    public static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    private DateUtils() {
    }

    public static LocalDate today() {
        return LocalDate.now(MOSCOW);
    }

    /** Начало расчётного периода: сегодня минус {@code years} лет. */
    public static LocalDate periodStart(int years) {
        return today().minusYears(years);
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }
}
