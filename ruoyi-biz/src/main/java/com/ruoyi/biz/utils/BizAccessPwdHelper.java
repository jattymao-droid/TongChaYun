package com.ruoyi.biz.utils;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * Access password helpers for open query/survey pages.
 * New values are stored as BCrypt; legacy plaintext still verifies.
 * Admin APIs use {@link #MASK} so hashes never leave the server.
 */
public final class BizAccessPwdHelper
{
    public static final String MASK = "******";

    private BizAccessPwdHelper()
    {
    }

    public static boolean isBcrypt(String value)
    {
        return value != null
            && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    /** Verify open-page access password; empty stored means no password required. */
    public static boolean matches(String stored, String input)
    {
        if (StringUtils.isEmpty(stored))
        {
            return true;
        }
        if (StringUtils.isEmpty(input))
        {
            return false;
        }
        if (isBcrypt(stored))
        {
            return SecurityUtils.matchesPassword(input, stored);
        }
        return stored.equals(input);
    }

    /** Hash plaintext for insert; keep empty; pass through existing bcrypt (e.g. copy). */
    public static String encodeForStore(String raw)
    {
        if (StringUtils.isEmpty(raw) || MASK.equals(raw))
        {
            return "";
        }
        if (isBcrypt(raw))
        {
            return raw;
        }
        return SecurityUtils.encryptPassword(raw);
    }

    /**
     * Prepare field for MyBatis update (accessPwd != null updates the column).
     * MASK / null -> skip (return null); empty -> clear; else hash.
     */
    public static String prepareForUpdate(String incoming)
    {
        if (incoming == null || MASK.equals(incoming))
        {
            return null;
        }
        if (incoming.isEmpty())
        {
            return "";
        }
        if (isBcrypt(incoming))
        {
            return incoming;
        }
        return SecurityUtils.encryptPassword(incoming);
    }

    public static String maskForApi(String stored)
    {
        return StringUtils.isEmpty(stored) ? "" : MASK;
    }
}
