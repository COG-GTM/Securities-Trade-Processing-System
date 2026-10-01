CREATE TABLE dbo.Holiday (HolidayDate DATE NOT NULL, Calendar CHAR(4) NOT NULL, PRIMARY KEY (HolidayDate, Calendar));
CREATE TABLE dbo.Trade (
    TradeId VARCHAR(32) PRIMARY KEY,
    TradeDate DATE NOT NULL,
    SettlementDate DATE NULL,
    SettledOn DATE NULL,
    LateFlag BIT NOT NULL DEFAULT 0
);
