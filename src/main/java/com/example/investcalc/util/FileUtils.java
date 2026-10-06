package com.example.investcalc.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Безопасная работа с файлами отчётов. */
public final class FileUtils {

    private FileUtils() {
    }

    /** Создаёт каталог (с родителями) и возвращает его абсолютный путь. */
    public static Path ensureDirectory(String dir) throws IOException {
        Path path = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }

    /** Оставляет в имени файла только латиницу, цифры, точку, дефис и подчёркивание. */
    public static String sanitizeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Склеивает каталог и имя файла и проверяет, что результат остался внутри каталога
     * (защита от {@code ../} в имени).
     */
    public static Path safeResolve(Path baseDir, String fileName) {
        Path base = baseDir.toAbsolutePath().normalize();
        Path resolved = base.resolve(fileName).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Путь выходит за пределы каталога: " + fileName);
        }
        return resolved;
    }

    public static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // удалить не удалось: временный файл подчистит ОС или следующий запуск
        }
    }
}
