# Invest Calc

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Invest Calc** — backend-сервис для комплексного анализа инвестиционных портфелей с фокусом на российский рынок (Московская биржа).

Сервис принимает состав портфеля, выбранную торговую стратегию и горизонт анализа, асинхронно рассчитывает диверсификацию, результаты бэктеста и метрики риска, после чего формирует PDF-отчёт в ZIP-архиве.


## Возможности

| Модуль              | Описание                                                                 |
|---------------------|--------------------------------------------------------------------------|
| **Диверсификация**  | Секторальная структура портфеля, концентрация, рекомендации по ребалансировке |
| **Бэктестинг**      | Стратегии `BUY_AND_HOLD` и `MA_CROSSOVER` (скользящие средние 10/30)    |
| **Анализ риска**    | Historical VaR, Parametric VaR, Expected Shortfall (ES)                 |
| **Рыночные данные** | Свечи и метаданные тикеров через ISS API Московской биржи (MOEX)        |
| **Поиск тикеров**   | Автодополнение по тикеру или названию                                   |
| **Отчёты**          | Асинхронная генерация PDF-отчёта + ZIP-архив                            |

---

## Стек технологий

| Технология              | Версия / Назначение          |
|-------------------------|------------------------------|
| Java                    | 21                           |
| Spring Boot             | 3.4.1                        |
| Spring Data JPA         | Работа с PostgreSQL          |
| Flyway                  | Миграции БД                  |
| Caffeine                | Кэширование                  |
| SpringDoc OpenAPI       | 2.8.5 (Swagger UI)           |
| OpenPDF                 | 1.3.40 (генерация PDF)       |
| Lombok                  | Упрощение кода               |
| Gradle                  | Сборка                       |

---

## Требования

- **JDK 21+**
- **PostgreSQL 14+**
- **Gradle 8+** (или используйте обёртку `./gradlew`)

---

## Быстрый старт

### 1. Подготовка базы данных

```bash
createdb investcalc
psql -c "CREATE USER invest WITH PASSWORD 'invest';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE investcalc TO invest;"
