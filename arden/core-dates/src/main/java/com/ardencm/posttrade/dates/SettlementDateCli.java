package com.ardencm.posttrade.dates;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;

/**
 * CLI used by ops runbooks and the parity harness.
 *
 * <pre>
 * java -jar core-dates.jar [--cycle T_PLUS_1|T_PLUS_2] &lt; trades.csv
 * </pre>
 *
 * Input CSV: {@code trade_id,trade_date,mic}. Output CSV: {@code trade_id,settlement_date,tag64,tag63,affirmation_deadline}.
 * Without {@code --cycle} the market default from {@link SettlementCycle#forMarket(String)} is used.
 */
public final class SettlementDateCli {

    public static void main(String[] args) throws Exception {
        SettlementCycle override = null;
        for (int i = 0; i < args.length; i++) {
            if ("--cycle".equals(args[i]) && i + 1 < args.length) {
                override = SettlementCycle.valueOf(args[++i]);
            }
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        boolean header = true;
        System.out.println("trade_id,settlement_date,tag64,tag63,affirmation_deadline");
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            if (header && line.startsWith("trade_id")) {
                header = false;
                continue;
            }
            header = false;
            String[] cols = line.split(",");
            String tradeId = cols[0].trim();
            LocalDate tradeDate = LocalDate.parse(cols[1].trim());
            String mic = cols[2].trim();
            SettlementCycle cycle = override != null ? override : SettlementCycle.forMarket(mic);
            LocalDate sd = SettlementDates.settlementDate(tradeDate, mic, cycle);
            System.out.println(String.join(",",
                    tradeId,
                    sd.toString(),
                    FixDates.tag64(sd),
                    FixDates.tag63(cycle),
                    SettlementDates.affirmationDeadline(sd, mic).toString()));
        }
    }
}
