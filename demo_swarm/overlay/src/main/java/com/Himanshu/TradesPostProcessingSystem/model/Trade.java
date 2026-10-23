package com.Himanshu.TradesPostProcessingSystem.model;

import java.util.UUID;

public class Trade {
    private String tradeId;
    private String accountId;
    private String symbol;
    private int quantity;
    private double price;

    public Trade() {
    }

    public Trade(String accountId, String symbol, int quantity, double price) {
        this.tradeId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
    }

    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public String toString() {
        return "Trade{tradeId='" + tradeId + "', accountId='" + accountId + "', symbol='" + symbol
                + "', quantity=" + quantity + ", price=" + price + '}';
    }
}
