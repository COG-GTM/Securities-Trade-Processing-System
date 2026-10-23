package com.Himanshu.TradesPostProcessingSystem.repository;

import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, Long> {

    List<TradeEntity> findBySymbol(String symbol);

    TradeEntity findByTradeId(String tradeId);

    List<TradeEntity> findByAccountId(String accountId);

    TradeEntity findByTradeIdAndAccountId(String tradeId, String accountId);
}
