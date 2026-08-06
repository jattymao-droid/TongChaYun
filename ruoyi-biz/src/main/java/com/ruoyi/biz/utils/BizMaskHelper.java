package com.ruoyi.biz.utils;

import com.ruoyi.common.utils.StringUtils;

/**
 * Mask sensitive display values for open query results.
 */
public final class BizMaskHelper
{
    private BizMaskHelper()
    {
    }

    public static String mask(String raw, String maskType)
    {
        if (raw == null)
        {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty())
        {
            return v;
        }
        String type = StringUtils.isEmpty(maskType) ? "none" : maskType.trim().toLowerCase();
        switch (type)
        {
            case "phone":
                return maskPhone(v);
            case "idcard":
                return maskIdCard(v);
            case "name":
                return maskName(v);
            case "email":
                return maskEmail(v);
            default:
                return v;
        }
    }

    private static String maskPhone(String v)
    {
        String digits = v.replaceAll("\\D", "");
        if (digits.length() == 11)
        {
            return digits.substring(0, 3) + "****" + digits.substring(7);
        }
        if (v.length() <= 4)
        {
            return "****";
        }
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    private static String maskIdCard(String v)
    {
        if (v.length() >= 15)
        {
            return v.substring(0, 4) + "**********" + v.substring(v.length() - 4);
        }
        if (v.length() <= 4)
        {
            return "****";
        }
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    private static String maskName(String v)
    {
        if (v.length() == 1)
        {
            return "*";
        }
        if (v.length() == 2)
        {
            return v.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(v.charAt(0));
        for (int i = 1; i < v.length() - 1; i++)
        {
            sb.append('*');
        }
        sb.append(v.charAt(v.length() - 1));
        return sb.toString();
    }

    private static String maskEmail(String v)
    {
        int at = v.indexOf('@');
        if (at <= 0)
        {
            return maskName(v);
        }
        String user = v.substring(0, at);
        String domain = v.substring(at);
        if (user.length() <= 2)
        {
            return "**" + domain;
        }
        return user.substring(0, 2) + "***" + domain;
    }
}
