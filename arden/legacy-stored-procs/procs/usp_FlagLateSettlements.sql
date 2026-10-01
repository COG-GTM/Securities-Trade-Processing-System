CREATE OR ALTER PROCEDURE dbo.usp_FlagLateSettlements
AS
BEGIN
    -- A trade is late if not settled by close of T+2 (regular way). Fed to the CSDR penalty report.
    UPDATE t SET t.LateFlag = 1
    FROM dbo.Trade t
    WHERE t.SettledOn IS NULL
      AND DATEADD(day, 2, t.TradeDate) < CAST(GETDATE() AS DATE);
END
