package com.example.investcalc.api.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Единый формат ошибки API.
 *
 * @param timestamp момент ошибки
 * @param status    HTTP-статус
 * @param code      машиночитаемый код ({@code ApiErrorCode} либо {@code HTTP_<status>} для ошибок фреймворка)
 * @param message   сообщение для человека
 * @param path      путь запроса
 * @param details   уточнения, например список невалидных полей
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<String> details
) {
    public static ErrorResponse of(int status, String code, String message, String path, List<String> details) {
        return new ErrorResponse(Instant.now(), status, code, message, path,
                details == null ? List.of() : List.copyOf(details));
    }
}
