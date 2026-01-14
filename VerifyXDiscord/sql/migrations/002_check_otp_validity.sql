-- Replacement/validation for check_otp_validity function
CREATE OR REPLACE FUNCTION check_otp_validity(p_otp_code VARCHAR(6))
RETURNS TABLE(is_valid BOOLEAN, message TEXT, player_name VARCHAR(32), player_uuid UUID, discord_id VARCHAR(32)) AS $$
DECLARE
    v_rec RECORD;
BEGIN
    SELECT * INTO v_rec
    FROM otps
    WHERE otp_code = p_otp_code
      AND (is_used IS NULL OR is_used = FALSE)
      AND expires_at > NOW()
    LIMIT 1;

    IF NOT FOUND THEN
        is_valid := FALSE;
        message := '❌ Invalid or expired OTP code.';
        player_name := NULL;
        player_uuid := NULL;
        discord_id := NULL;
        RETURN NEXT;
        RETURN;
    END IF;

    is_valid := TRUE;
    message := '✅ OTP is valid and ready for verification.';
    player_name := v_rec.player_name;
    player_uuid := v_rec.player_uuid;
    discord_id := v_rec.discord_id;
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;