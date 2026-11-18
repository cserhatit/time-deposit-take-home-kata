-- Integration-test fixture (independent of the dev-only demo data).
DELETE FROM "withdrawals";
DELETE FROM "timeDeposits";

INSERT INTO "timeDeposits" ("id", "planType", "days", "balance") VALUES
    (1, 'basic',   31,  10000.00),
    (2, 'student', 100, 1234567.89),
    (3, 'premium', 46,  10000.00),
    (4, 'student', 366, 10000.00),
    (5, 'premium', 45,  10000.00),
    (6, 'gold',    100, 10000.00);

INSERT INTO "withdrawals" ("id", "timeDepositId", "amount", "date") VALUES
    (2, 1, 50.25,  '2026-02-10'),
    (1, 1, 100.00, '2026-01-10'),
    (3, 3, 200.00, '2026-03-01');
