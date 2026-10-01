package com.arden.poskeeper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;

/** Reads trade_id,trade_date(yyyyMMdd) from stdin, prints trade_id,settle_date. Used by the EOD position roll. */
public class PosKeeperMain {
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("trade_id") || line.trim().isEmpty()) continue;
            String[] parts = line.split(",");
            Date settle = SettleDateUtil.settleDate(SettleDateUtil.parse(parts[1].replace("-", "")));
            System.out.println(parts[0] + "," + SettleDateUtil.format(settle));
        }
    }
}
