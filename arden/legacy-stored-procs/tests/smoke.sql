DECLARE @out DATE;
EXEC dbo.usp_CalcSettlementDate @TradeDate = '2027-10-11', @SettlementDate = @out OUTPUT;
SELECT @out AS SettlementDate;  -- expect 2027-10-13
