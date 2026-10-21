-- Additive validation guards; do not change V1 or the earlier identity migration checksum.
ALTER TABLE otp_challenges ADD CONSTRAINT ck_otp_purpose CHECK(purpose IN ('REGISTER','PASSWORD_RESET'));
ALTER TABLE otp_challenges ADD CONSTRAINT ck_otp_attempts CHECK(attempts>=0);
ALTER TABLE otp_challenges ADD CONSTRAINT ck_otp_expiry CHECK(expires_at>created_at);
ALTER TABLE store_memberships ADD CONSTRAINT ck_membership_role CHECK(member_role IN ('MANAGER','CATALOG_ADMIN'));
ALTER TABLE user_addresses ADD CONSTRAINT ck_address_required CHECK(CHAR_LENGTH(TRIM(recipient_name))>0 AND CHAR_LENGTH(TRIM(address_detail))>0 AND CHAR_LENGTH(phone) BETWEEN 8 AND 20);
ALTER TABLE loyalty_accounts ADD CONSTRAINT ck_loyalty_net CHECK(balance=0 OR debt=0);
ALTER TABLE loyalty_transactions ADD CONSTRAINT ck_ledger_type CHECK(type IN ('REDEEM','REFUND','EARN','REVERSAL'));
ALTER TABLE loyalty_transactions ADD CONSTRAINT ck_ledger_sign CHECK((type IN ('EARN','REFUND') AND points_delta>=0) OR (type IN ('REDEEM','REVERSAL') AND points_delta<=0));
ALTER TABLE loyalty_transactions ADD CONSTRAINT ck_ledger_balance CHECK(balance_after>=0 AND debt_after>=0 AND (balance_after=0 OR debt_after=0));
ALTER TABLE product_views ADD CONSTRAINT ck_view_count CHECK(view_count>0);
ALTER TABLE reviews ADD CONSTRAINT ck_review_status CHECK(status IN ('VISIBLE','HIDDEN'));
ALTER TABLE product_comments ADD CONSTRAINT ck_comment_status CHECK(status IN ('VISIBLE','HIDDEN'));
ALTER TABLE product_comments ADD CONSTRAINT ck_comment_text CHECK(status='HIDDEN' OR CHAR_LENGTH(TRIM(content))>=50);
ALTER TABLE review_media ADD CONSTRAINT ck_review_media_type CHECK(resource_type IN ('image','video') AND display_order>=0);
ALTER TABLE comment_media ADD CONSTRAINT ck_comment_media_type CHECK(resource_type IN ('image','video') AND display_order>=0);
