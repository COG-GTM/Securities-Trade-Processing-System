package com.Himanshu.TradesPostProcessingSystem.swarm;

import com.Himanshu.TradesPostProcessingSystem.entity.TradeEntity;
import com.Himanshu.TradesPostProcessingSystem.model.AmendmentRequest;
import com.Himanshu.TradesPostProcessingSystem.model.SettlementRequest;
import com.Himanshu.TradesPostProcessingSystem.repository.SettlementRepository;
import com.Himanshu.TradesPostProcessingSystem.repository.TradeRepository;
import com.Himanshu.TradesPostProcessingSystem.service.SettlementService;
import com.Himanshu.TradesPostProcessingSystem.service.TradeAmendmentService;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RACE-2: amend checks "not SETTLED" on the row it loaded, then writes the whole row back. A settlement that commits
 * in between is silently overwritten: the settled trade flips to AMENDED with new economics. The repository mock
 * behaves like JPA: each find returns a fresh copy (own persistence context) and save enforces a @Version if the
 * entity declares one, otherwise last write wins.
 */
@Tag("swarm")
class Race2SwarmTest {

    private static final Field VERSION = Arrays.stream(TradeEntity.class.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(Version.class)).peek(f -> f.setAccessible(true)).findFirst().orElse(null);

    private final AtomicReference<TradeEntity> row = new AtomicReference<>();

    private static Object version(TradeEntity t) throws IllegalAccessException {
        return VERSION == null ? null : VERSION.get(t);
    }

    private static TradeEntity copy(TradeEntity t) throws IllegalAccessException {
        TradeEntity c = new TradeEntity(t.getTradeId(), t.getAccountId(), t.getSymbol(), t.getQuantity(), t.getPrice());
        c.setId(t.getId());
        c.setStatus(t.getStatus());
        if (VERSION != null) {
            VERSION.set(c, VERSION.get(t));
        }
        return c;
    }

    private TradeEntity commit(TradeEntity incoming) throws IllegalAccessException {
        TradeEntity current = row.get();
        if (VERSION != null) {
            if (!java.util.Objects.equals(version(current), version(incoming))) {
                throw new ObjectOptimisticLockingFailureException(TradeEntity.class, incoming.getId());
            }
            Number v = (Number) version(incoming);
            VERSION.set(incoming, convert(v == null ? 1L : v.longValue() + 1));
        }
        row.set(copy(incoming));
        return incoming;
    }

    private static Object convert(long v) {
        return VERSION.getType() == Integer.class || VERSION.getType() == int.class ? (Object) (int) v : (Object) v;
    }

    @Test
    void settlementCommittedMidAmendIsNotOverwritten() throws Exception {
        TradeEntity booked = new TradeEntity("T-100", "ACCT-A", "AAPL", 100, 190.0);
        booked.setId(1L);
        if (VERSION != null) {
            VERSION.set(booked, convert(0L));
        }
        row.set(booked);

        TradeRepository trades = mock(TradeRepository.class);
        SettlementRepository settlements = mock(SettlementRepository.class);
        when(settlements.save(any())).thenAnswer(i -> i.getArgument(0));
        when(trades.save(any(TradeEntity.class))).thenAnswer(i -> commit(i.getArgument(0)));
        SettlementService settlementService = new SettlementService(trades, settlements);

        AtomicBoolean interleaved = new AtomicBoolean();
        when(trades.findByTradeIdAndAccountId(anyString(), anyString())).thenAnswer(i -> {
            TradeEntity snapshot = copy(row.get());
            if (interleaved.compareAndSet(false, true)) {
                // The amend request has read the row; the custodian's settlement commits before it writes back.
                settlementService.settle("ACCT-A", new SettlementRequest("SI-1", "T-100"));
            }
            return snapshot;
        });

        try {
            new TradeAmendmentService(trades).amend("ACCT-A", "T-100", new AmendmentRequest(150, 195.0));
        } catch (RuntimeException rejected) {
            // A rejected stale amendment is the safe outcome.
        }

        TradeEntity stored = row.get();
        assertEquals("SETTLED", stored.getStatus(), "settled trade was overwritten by a concurrent amendment");
        assertEquals(100, stored.getQuantity(), "settled quantity changed after settlement (custodian settled 100)");
        assertEquals(190.0, stored.getPrice());
    }
}
