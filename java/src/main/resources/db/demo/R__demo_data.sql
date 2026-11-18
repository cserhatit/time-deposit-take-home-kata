-- Demo data for local development only (enabled by the dev profile).
-- Repeatable and idempotent: existing rows are kept. Reset with `docker compose down -v`.
INSERT INTO "timeDeposits" ("id", "planType", "days", "balance") VALUES
    (1, 'basic',   20,  5000.00),   -- first 30 days: no interest
    (2, 'basic',   31,  10000.00),  -- earns 1% / 12
    (3, 'student', 200, 20000.00),  -- earns 3% / 12
    (4, 'student', 400, 15000.00),  -- after 365 days: no interest
    (5, 'premium', 45,  50000.00),  -- first 45 days: no interest
    (6, 'premium', 90,  100000.00)  -- earns 5% / 12
ON CONFLICT ("id") DO NOTHING;

INSERT INTO "withdrawals" ("id", "timeDepositId", "amount", "date") VALUES
    (1, 2, 500.00,  '2026-01-15'),
    (2, 3, 1000.00, '2026-02-01'),
    (3, 3, 250.50,  '2026-03-10'),
    (4, 6, 5000.00, '2026-04-20')
ON CONFLICT ("id") DO NOTHING;
