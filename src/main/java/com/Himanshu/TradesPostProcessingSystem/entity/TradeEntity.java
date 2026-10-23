package com.Himanshu.TradesPostProcessingSystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trades")
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tradeId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String status = "BOOKED";

    @Version
    private Long version;

    public TradeEntity() {
    }

    public TradeEntity(String tradeId, String accountId, String symbol, int quantity, double price) {
        this.tradeId = tradeId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "TradeEntity{id=" + id + ", tradeId='" + tradeId + "', accountId='" + accountId + "', symbol='" + symbol
                + "', quantity=" + quantity + ", price=" + price + ", status='" + status + "'}";
    }
}
