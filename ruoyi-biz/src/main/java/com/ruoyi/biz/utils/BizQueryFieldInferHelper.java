package com.ruoyi.biz.utils;

import java.util.Locale;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.common.utils.StringUtils;

/**
 * Infer query flags / match type / masking from Excel header names.
 */
public final class BizQueryFieldInferHelper
{
    private BizQueryFieldInferHelper()
    {
    }

    public static void apply(BizQueryField field, int colIndex)
    {
        String name = StringUtils.nvl(field.getFieldName(), "") + StringUtils.nvl(field.getFieldLabel(), "");
        String n = name.toLowerCase(Locale.ROOT);

        field.setHtmlType("input");
        field.setIsList("1");
        field.setIsSortable("0");
        field.setIsRequired("0");
        field.setMaskType("none");
        field.setIsQuery("0");
        field.setQueryType("EQ");

        if (containsAny(n, "\u624b\u673a", "\u7535\u8bdd", "mobile", "phone", "tel"))
        {
            field.setIsQuery("1");
            field.setQueryType("EQ");
            field.setIsRequired("1");
            field.setMaskType("phone");
            return;
        }
        if (containsAny(n, "\u8eab\u4efd\u8bc1", "\u8bc1\u4ef6\u53f7", "idcard", "id_no", "\u8eab\u4efd\u8bc1\u53f7"))
        {
            field.setIsQuery("1");
            field.setQueryType("EQ");
            field.setIsRequired("1");
            field.setMaskType("idcard");
            return;
        }
        if (containsAny(n, "\u90ae\u7bb1", "email", "mail"))
        {
            field.setIsQuery("1");
            field.setQueryType("EQ");
            field.setMaskType("email");
            return;
        }
        if (containsAny(n, "\u59d3\u540d", "\u540d\u5b57", "name", "\u8003\u751f"))
        {
            field.setIsQuery("1");
            field.setQueryType("EQ");
            field.setIsRequired("1");
            field.setMaskType("name");
            return;
        }
        if (containsAny(n, "\u5b66\u53f7", "\u8003\u53f7", "\u51c6\u8003\u8bc1", "\u5de5\u53f7", "\u7f16\u53f7", "\u8d26\u53f7", "admission", "student"))
        {
            field.setIsQuery("1");
            field.setQueryType("EQ");
            field.setIsRequired("1");
            return;
        }
        if (containsAny(n, "\u73ed\u7ea7", "\u5e74\u7ea7", "\u90e8\u95e8", "\u5b66\u9662", "\u4e13\u4e1a", "class", "dept"))
        {
            field.setIsQuery("1");
            field.setQueryType("EQ");
            field.setHtmlType("select");
            return;
        }
        if (containsAny(n, "\u65e5\u671f", "\u65f6\u95f4", "\u751f\u65e5", "date", "time"))
        {
            field.setHtmlType("date");
            field.setQueryType("BETWEEN");
            return;
        }
        // fallback: first 2 columns as optional query keys
        if (colIndex < 2)
        {
            field.setIsQuery("1");
            field.setQueryType("LIKE");
            field.setIsRequired(colIndex == 0 ? "1" : "0");
        }
    }

    private static boolean containsAny(String haystack, String... needles)
    {
        for (String n : needles)
        {
            if (haystack.contains(n.toLowerCase(Locale.ROOT)))
            {
                return true;
            }
        }
        return false;
    }
}
