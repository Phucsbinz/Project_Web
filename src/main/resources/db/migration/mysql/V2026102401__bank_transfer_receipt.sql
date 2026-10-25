-- Nullable provider references allow multiple pending/COD payments. A bank receipt
-- can settle at most one order, including when confirmations race across admins.
ALTER TABLE payment_transactions ADD CONSTRAINT uk_payment_provider_receipt
    UNIQUE (provider, provider_txn_id);
