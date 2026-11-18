-- Table and column names are quoted to keep the README's camelCase names exactly.
-- No CHECK constraint on "planType": unknown plan types are storable and earn no interest (legacy behavior).
-- No ID generation: no endpoint creates records.
CREATE TABLE "timeDeposits" (
    "id"         INTEGER        PRIMARY KEY,
    "planType"   VARCHAR(50)    NOT NULL,
    "days"       INTEGER        NOT NULL,
    "balance"    NUMERIC(19, 2) NOT NULL
);

CREATE TABLE "withdrawals" (
    "id"            INTEGER        PRIMARY KEY,
    "timeDepositId" INTEGER        NOT NULL REFERENCES "timeDeposits" ("id"),
    "amount"        NUMERIC(19, 2) NOT NULL,
    "date"          DATE           NOT NULL
);

CREATE INDEX "withdrawals_timeDepositId_idx" ON "withdrawals" ("timeDepositId");
