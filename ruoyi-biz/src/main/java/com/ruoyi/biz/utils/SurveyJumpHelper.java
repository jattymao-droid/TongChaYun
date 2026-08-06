package com.ruoyi.biz.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.biz.domain.BizSurveyQuestion;
import com.ruoyi.common.utils.StringUtils;

/**
 * Resolve visible questions by jump rules + optional visibleIf in props_json.
 * jumps: [{ "value": "A", "toSort": 3 }]  toSort=-1 means end; null/missing means next
 * visibleIf: { "sourceSort": 0, "value": "1" }  show only when source question answer matches
 */
public final class SurveyJumpHelper
{
    private SurveyJumpHelper() {}

    public static List<BizSurveyQuestion> visibleQuestions(List<BizSurveyQuestion> questions, Map<Long, String> answers)
    {
        if (questions == null || questions.isEmpty())
        {
            return new ArrayList<>();
        }
        Map<Integer, Integer> sortIndex = new HashMap<>();
        for (int i = 0; i < questions.size(); i++)
        {
            Integer sort = questions.get(i).getSort();
            sortIndex.put(sort == null ? i : sort, i);
        }
        List<BizSurveyQuestion> visible = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        int i = 0;
        while (i >= 0 && i < questions.size() && !visited.contains(i))
        {
            visited.add(i);
            BizSurveyQuestion q = questions.get(i);
            visible.add(q);
            Integer next = resolveNextIndex(q, answers == null ? null : answers.get(q.getQuestionId()), questions, sortIndex, i);
            if (next == null)
            {
                break;
            }
            i = next;
        }
        return applyVisibleIf(visible, questions, answers);
    }

    private static List<BizSurveyQuestion> applyVisibleIf(List<BizSurveyQuestion> candidates,
        List<BizSurveyQuestion> all, Map<Long, String> answers)
    {
        List<BizSurveyQuestion> out = new ArrayList<>();
        for (BizSurveyQuestion q : candidates)
        {
            if (matchesVisibleIf(q, all, answers))
            {
                out.add(q);
            }
        }
        return out;
    }

    private static boolean matchesVisibleIf(BizSurveyQuestion q, List<BizSurveyQuestion> all, Map<Long, String> answers)
    {
        JSONObject rule = readVisibleIf(q.getPropsJson());
        if (rule == null)
        {
            return true;
        }
        if (!rule.containsKey("sourceSort") || rule.get("sourceSort") == null || "".equals(String.valueOf(rule.get("sourceSort"))))
        {
            return true;
        }
        int sourceSort;
        try
        {
            sourceSort = Integer.parseInt(String.valueOf(rule.get("sourceSort")));
        }
        catch (Exception ex)
        {
            return true;
        }
        String expect = rule.get("value") == null ? null : String.valueOf(rule.get("value"));
        if (expect == null)
        {
            return true;
        }
        BizSurveyQuestion source = null;
        for (int i = 0; i < all.size(); i++)
        {
            BizSurveyQuestion x = all.get(i);
            int s = x.getSort() == null ? i : x.getSort();
            if (s == sourceSort)
            {
                source = x;
                break;
            }
        }
        if (source == null || source.getQuestionId() == null)
        {
            return false;
        }
        String ans = answers == null ? null : answers.get(source.getQuestionId());
        String scalar = firstScalar(ans);
        return expect.equals(scalar);
    }

    private static JSONObject readVisibleIf(String propsJson)
    {
        if (StringUtils.isEmpty(propsJson))
        {
            return null;
        }
        try
        {
            JSONObject props = JSON.parseObject(propsJson);
            if (props == null)
            {
                return null;
            }
            return props.getJSONObject("visibleIf");
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static Integer resolveNextIndex(BizSurveyQuestion q, String answer, List<BizSurveyQuestion> questions,
        Map<Integer, Integer> sortIndex, int currentIndex)
    {
        if (!("radio".equals(q.getQType()) || "select".equals(q.getQType())
            || "yesno".equals(q.getQType()) || "image_radio".equals(q.getQType()) || "likert".equals(q.getQType())))
        {
            return currentIndex + 1 < questions.size() ? currentIndex + 1 : null;
        }
        String matchValue = firstScalar(answer);
        Integer toSort = findJumpToSort(q.getPropsJson(), matchValue);
        if (toSort == null)
        {
            return currentIndex + 1 < questions.size() ? currentIndex + 1 : null;
        }
        if (toSort < 0)
        {
            return null;
        }
        Integer idx = sortIndex.get(toSort);
        if (idx == null)
        {
            return currentIndex + 1 < questions.size() ? currentIndex + 1 : null;
        }
        return idx;
    }

    private static Integer findJumpToSort(String propsJson, String value)
    {
        if (StringUtils.isEmpty(propsJson) || value == null)
        {
            return null;
        }
        try
        {
            JSONObject props = JSON.parseObject(propsJson);
            if (props == null)
            {
                return null;
            }
            JSONArray jumps = props.getJSONArray("jumps");
            if (jumps == null)
            {
                return null;
            }
            for (int i = 0; i < jumps.size(); i++)
            {
                JSONObject j = jumps.getJSONObject(i);
                if (j == null)
                {
                    continue;
                }
                if (value.equals(String.valueOf(j.get("value"))))
                {
                    if (!j.containsKey("toSort") || j.get("toSort") == null || "".equals(String.valueOf(j.get("toSort"))))
                    {
                        return null;
                    }
                    return Integer.valueOf(String.valueOf(j.get("toSort")));
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    private static String firstScalar(String answer)
    {
        if (StringUtils.isEmpty(answer))
        {
            return null;
        }
        String t = answer.trim();
        if (t.startsWith("["))
        {
            try
            {
                JSONArray arr = JSON.parseArray(t);
                if (arr != null && !arr.isEmpty())
                {
                    return String.valueOf(arr.get(0));
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return t;
    }
}
