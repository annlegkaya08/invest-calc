package com.example.investcalc.service.backtesting.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Trade {

    public enum Type { BUY, SELL }

    private final LocalDate date;
    private final String ticker;
    private final Type type;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final BigDecimal commission;
    private final BigDecimal amount;

    public Trade(LocalDate date, String ticker, Type type,
                 BigDecimal price, BigDecimal quantity, BigDecimal commission) {
        this.date = date;
        this.ticker = ticker;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.commission = commission;
        this.amount = price.multiply(quantity);
    }

    public LocalDate getDate() { return date; }
    public String getTicker() { return ticker; }
    public Type getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getCommission() { return commission; }
    public BigDecimal getAmount() { return amount; }
}