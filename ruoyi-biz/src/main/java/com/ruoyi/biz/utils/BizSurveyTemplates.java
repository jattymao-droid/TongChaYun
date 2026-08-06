package com.ruoyi.biz.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.biz.domain.BizSurveyQuestion;
import com.ruoyi.common.exception.ServiceException;

/**
 * Built-in survey templates for one-click create.
 */
public final class BizSurveyTemplates
{
    private BizSurveyTemplates() {}

    public static List<Map<String, Object>> list()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(meta("satisfaction", "满意度调研", "NPS + 满意度单选 + 建议填空", "survey", 3, "el-icon-star-off"));
        list.add(meta("registration", "活动报名", "姓名/手机/邮箱/场次选择", "survey", 5, "el-icon-date"));
        list.add(meta("enrollment", "新生报名", "基本信息 + 意向班级 + 监护人联系方式", "survey", 6, "el-icon-school"));
        list.add(meta("feedback", "意见反馈", "是否推荐 + 评分 + 意见", "survey", 3, "el-icon-chat-dot-round"));
        return list;
    }

    public static TemplateDef require(String key)
    {
        if ("satisfaction".equals(key))
        {
            return satisfaction();
        }
        if ("registration".equals(key))
        {
            return registration();
        }
        if ("enrollment".equals(key))
        {
            return enrollment();
        }
        if ("feedback".equals(key))
        {
            return feedback();
        }
        throw new ServiceException("未知问卷模板: " + key);
    }

    private static Map<String, Object> meta(String key, String name, String desc, String type, int questionCount, String icon)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("desc", desc);
        m.put("type", type);
        m.put("questionCount", questionCount);
        m.put("icon", icon);
        return m;
    }

    private static TemplateDef satisfaction()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q("nps", "您向朋友推荐我们的可能性？", "1", null, "{\"min\":0,\"max\":10}", 1));
        qs.add(q("radio", "整体满意度", "1",
            "[{\"label\":\"非常满意\",\"value\":\"5\"},{\"label\":\"满意\",\"value\":\"4\"},{\"label\":\"一般\",\"value\":\"3\"},{\"label\":\"不满意\",\"value\":\"2\"},{\"label\":\"非常不满意\",\"value\":\"1\"}]",
            null, 2));
        qs.add(q("textarea", "还有哪些建议？", "0", null, "{\"maxLength\":500}", 3));
        return new TemplateDef("满意度调研（模板）", "快速了解用户满意度与推荐意愿", qs,
            theme("#1677ff", "linear-gradient(180deg, #e8f1ff 0%, #f7f7f7 280px, #f7f7f7 100%)", "all"),
            "1", "0");
    }

    private static TemplateDef registration()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q("input", "姓名", "1", null, "{\"maxLength\":50}", 1));
        qs.add(q("phone", "手机号", "1", null, null, 2));
        qs.add(q("email", "邮箱", "0", null, null, 3));
        qs.add(q("select", "报名场次", "1",
            "[{\"label\":\"上午场\",\"value\":\"am\"},{\"label\":\"下午场\",\"value\":\"pm\"},{\"label\":\"晚场\",\"value\":\"night\"}]",
            null, 4));
        qs.add(q("textarea", "备注", "0", null, "{\"maxLength\":200}", 5));
        return new TemplateDef("活动报名（模板）", "收集报名基本信息", qs,
            theme("#0f766e", "linear-gradient(180deg, #ecfdf5 0%, #f7f7f7 280px, #f7f7f7 100%)", "step"),
            "0", "1");
    }

    private static TemplateDef enrollment()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q("input", "学生姓名", "1", null, "{\"maxLength\":50}", 1));
        qs.add(q("input", "证件号后六位", "1", null, "{\"maxLength\":6}", 2));
        qs.add(q("phone", "监护人手机", "1", null, null, 3));
        qs.add(q("select", "意向校区", "1",
            "[{\"label\":\"东校区\",\"value\":\"east\"},{\"label\":\"西校区\",\"value\":\"west\"},{\"label\":\"南校区\",\"value\":\"south\"}]",
            null, 4));
        qs.add(q("radio", "是否住宿", "1",
            "[{\"label\":\"是\",\"value\":\"yes\"},{\"label\":\"否\",\"value\":\"no\"}]",
            null, 5));
        qs.add(q("textarea", "备注说明", "0", null, "{\"maxLength\":300}", 6));
        return new TemplateDef("新生报名（模板）", "采集新生报名与意向信息", qs,
            theme("#2b6de5", "linear-gradient(180deg, #eef3ff 0%, #f7f7f7 280px, #f7f7f7 100%)", "step"),
            "0", "1");
    }

    private static TemplateDef feedback()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q("yesno", "是否愿意继续使用？", "1", null, null, 1));
        qs.add(q("rate", "整体评分", "1", null, "{\"max\":5}", 2));
        qs.add(q("textarea", "意见与建议", "1", null, "{\"maxLength\":800}", 3));
        return new TemplateDef("意见反馈（模板）", "收集使用意愿与改进建议", qs,
            theme("#d48806", "linear-gradient(180deg, #fff7e6 0%, #f7f7f7 280px, #f7f7f7 100%)", "all"),
            "1", "0");
    }

    private static Map<String, Object> theme(String color, String bg, String fillMode)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("color", color);
        m.put("bg", bg);
        m.put("fillMode", fillMode);
        return m;
    }

    private static BizSurveyQuestion q(String type, String title, String required, String options, String props, int sort)
    {
        BizSurveyQuestion q = new BizSurveyQuestion();
        q.setQType(type);
        q.setTitle(title);
        q.setRequired(required);
        q.setOptionsJson(options);
        q.setPropsJson(props);
        q.setSort(sort);
        return q;
    }

    public static final class TemplateDef
    {
        public final String surveyName;
        public final String surveyDesc;
        public final List<BizSurveyQuestion> questions;
        public final Map<String, Object> theme;
        public final String allowMulti;
        public final String needCaptcha;

        public TemplateDef(String surveyName, String surveyDesc, List<BizSurveyQuestion> questions,
            Map<String, Object> theme, String allowMulti, String needCaptcha)
        {
            this.surveyName = surveyName;
            this.surveyDesc = surveyDesc;
            this.questions = Collections.unmodifiableList(questions);
            this.theme = theme == null ? Collections.emptyMap() : Collections.unmodifiableMap(theme);
            this.allowMulti = allowMulti == null ? "1" : allowMulti;
            this.needCaptcha = needCaptcha == null ? "0" : needCaptcha;
        }
    }
}
