-- =============================================
-- usp_CalcSettlementDate
-- Regular-way settlement date for the London book. Skips weekends and rows in dbo.Holiday.
-- History: 2014-10-06 changed @Lag default 3 -> 2 (CSDR). Do not change without Settlements sign-off.
-- =============================================
CREATE OR ALTER PROCEDURE dbo.usp_CalcSettlementDate
    @TradeDate DATE,
    @Lag INT = 2,
    @SettlementDate DATE OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @d DATE = @TradeDate, @added INT = 0;
    WHILE @added < @Lag
    BEGIN
        SET @d = DATEADD(day, 1, @d);
        IF DATENAME(weekday, @d) NOT IN ('Saturday', 'Sunday')
           AND NOT EXISTS (SELECT 1 FROM dbo.Holiday WHERE HolidayDate = @d AND Calendar = 'XLON')
            SET @added = @added + 1;
    END
    SET @SettlementDate = @d;
END
