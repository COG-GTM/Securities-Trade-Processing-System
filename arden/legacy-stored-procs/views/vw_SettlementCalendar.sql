CREATE OR ALTER VIEW dbo.vw_SettlementCalendar AS
SELECT t.TradeId,
       t.TradeDate,
       DATEADD(day, 2, t.TradeDate) AS NaiveSettlementDate,   -- used by the legacy client statement only
       t.SettlementDate
FROM dbo.Trade t;
