package com.Himanshu.TradesPostProcessingSystem.repository;

import com.Himanshu.TradesPostProcessingSystem.entity.SettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<SettlementEntity, Long> {

    List<SettlementEntity> findByInstructionId(String instructionId);

    List<SettlementEntity> findByTradeId(String tradeId);
}
