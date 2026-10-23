package com.Himanshu.TradesPostProcessingSystem.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "settlements")
public class SettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String instructionId;

    @Column(nullable = false)
    private String tradeId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double cashAmount;

    @Column(nullable = false)
    private Instant settledAt;

    public SettlementEntity() {
    }

    public SettlementEntity(String instructionId, String tradeId, String accountId, int quantity, double cashAmount, Instant settledAt) {
        this.instructionId = instructionId;
        this.tradeId = tradeId;
        this.accountId = accountId;
        this.quantity = quantity;
        this.cashAmount = cashAmount;
        this.settledAt = settledAt;
    }

    public Long getId() { return id; }
    public String getInstructionId() { return instructionId; }
    public String getTradeId() { return tradeId; }
    public String getAccountId() { return accountId; }
    public int getQuantity() { return quantity; }
    public double getCashAmount() { return cashAmount; }
    public Instant getSettledAt() { return settledAt; }
}
