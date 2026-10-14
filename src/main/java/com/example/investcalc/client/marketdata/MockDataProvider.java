package com.example.investcalc.client.marketdata;

import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.repository.entity.TickerMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "marketdata.provider", havingValue = "mock")
public class MockDataProvider implements MarketDataProvider {

    private static final Map<String, TickerMetadata> META = Map.of(
            "SBER", new TickerMetadata("SBER", "Sberbank", "FINANCE"),
            "GAZP", new TickerMetadata("GAZP", "Gazprom", "OIL_GAS"),
            "LKOH", new TickerMetadata("LKOH", "Lukoil", "OIL_GAS"),
            "YNDX", new TickerMetadata("YNDX", "Yandex", "IT"),
            "GMKN", new TickerMetadata("GMKN", "Nornickel", "METALS"),
            "ROSN", new TickerMetadata("ROSN", "Rosneft", "OIL_GAS"),
            "VTBR", new TickerMetadata("VTBR", "VTB", "FINANCE"),
            "MGNT", new TickerMetadata("MGNT", "Magnit", "RETAIL"),
            "ALRS", new TickerMetadata("ALRS", "Alrosa", "METALS"),
            "TATN", new TickerMetadata("TATN", "Tatneft", "OIL_GAS")
    );

    @Override
    public List<Candle> getCandles(String ticker, LocalDate from, LocalDate to) {
        String key = ticker.toUpperCase();
        if (!META.containsKey(key)) {
            return List.of();
        }

        List<Candle> result = new ArrayList<>();
        BigDecimal price = BigDecimal.valueOf(100 + ThreadLocalRandom.current().nextDouble(50, 400));
        LocalDate current = from;

        while (!current.isAfter(to)) {
            //пропускаем выходные
            if (current.getDayOfWeek().getValue() < 6) {
                double change = ThreadLocalRandom.current().nextDouble(-0.025, 0.025);
                BigDecimal open = price;
                BigDecimal close = price.multiply(BigDecimal.valueOf(1 + change))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1.005))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(0.995))
                        .setScale(2, RoundingMode.HALF_UP);
                long volume = ThreadLocalRandom.current().nextLong(100_000, 5_000_000);

                result.add(new Candle(key, current, open, high, low, close, volume));
                price = close;
            }
            current = current.plusDays(1);
        }
        return result;
    }

    @Override
    public Optional<TickerMetadata> getTickerMetadata(String ticker) {
        return Optional.ofNullable(META.get(ticker.toUpperCase()));
    }

    @Override
    public List<TickerMetadata> searchTickers(String query) {
        String q = query.toLowerCase();
        return META.values().stream()
                .filter(t -> t.getTicker().toLowerCase().contains(q)
                        || t.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }
}