package com.ruoyi.biz.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.common.exception.ServiceException;

/**
 * Built-in query templates (fields + page hints + sample rows).
 */
public final class BizQueryTemplates
{
    private BizQueryTemplates() {}

    public static List<Map<String, Object>> list()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(meta("score_lookup", "成绩查询", "学号精确 / 姓名模糊，成绩单样式，含演示数据", "query", true, 4, 3, "el-icon-data-analysis"));
        list.add(meta("class_assign", "分班查询", "学号/姓名查班级与校区，含演示数据", "query", true, 4, 3, "el-icon-office-building"));
        list.add(meta("staff_dir", "通讯录查询", "姓名模糊 / 部门精确，含演示数据", "query", true, 4, 3, "el-icon-user"));
        return list;
    }

    public static TemplateDef require(String key)
    {
        if ("score_lookup".equals(key))
        {
            return scoreLookup();
        }
        if ("class_assign".equals(key))
        {
            return classAssign();
        }
        if ("staff_dir".equals(key))
        {
            return staffDir();
        }
        throw new ServiceException("未知查询模板: " + key);
    }

    private static Map<String, Object> meta(String key, String name, String desc, String type, boolean hasSample,
        int fieldCount, int sampleCount, String icon)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("desc", desc);
        m.put("type", type);
        m.put("hasSample", hasSample);
        m.put("fieldCount", fieldCount);
        m.put("sampleCount", sampleCount);
        m.put("icon", icon);
        return m;
    }

    private static TemplateDef scoreLookup()
    {
        List<BizQueryField> fields = new ArrayList<>();
        fields.add(f("student_no", "学号", "string", "1", "EQ", "input", "1", "0", 1, 120));
        fields.add(f("name", "姓名", "string", "1", "LIKE", "input", "1", "0", 2, 100));
        fields.add(f("score", "成绩", "number", "0", "EQ", "input", "1", "1", 3, 80));
        fields.add(f("subject", "科目", "string", "0", "EQ", "input", "1", "0", 4, 100));
        Map<String, Object> page = pageHints("成绩查询", "请输入学号或姓名查询", "未查询到相关成绩", "#1677ff");
        Map<String, Object> layout = baseLayout("scorecard", "成绩查询结果", "name",
            Arrays.asList("student_no", "score", "subject"), "请核对学号或姓名后重试，或联系班主任");
        page.put("layoutJson", layout);
        List<Map<String, String>> samples = new ArrayList<>();
        samples.add(row("student_no", "2024001", "name", "张三", "score", "92", "subject", "数学"));
        samples.add(row("student_no", "2024002", "name", "李四", "score", "85", "subject", "英语"));
        samples.add(row("student_no", "2024003", "name", "王五", "score", "78", "subject", "物理"));
        return new TemplateDef("成绩查询（模板）", "按学号/姓名查询成绩，已内置演示数据可直接发布", fields, page, samples);
    }

    private static TemplateDef classAssign()
    {
        List<BizQueryField> fields = new ArrayList<>();
        fields.add(f("student_no", "学号", "string", "1", "EQ", "input", "1", "0", 1, 120));
        fields.add(f("name", "姓名", "string", "1", "LIKE", "input", "1", "0", 2, 100));
        fields.add(f("class_name", "班级", "string", "0", "EQ", "input", "1", "0", 3, 140));
        fields.add(f("campus", "校区", "string", "0", "EQ", "input", "1", "0", 4, 100));
        Map<String, Object> page = pageHints("分班查询", "请输入学号或姓名查询分班结果", "未查询到分班信息", "#2b6de5");
        Map<String, Object> layout = baseLayout("default", "分班查询结果", "name",
            Arrays.asList("student_no", "class_name", "campus"), "请核对学号后重试，或联系招生办");
        page.put("layoutJson", layout);
        List<Map<String, String>> samples = new ArrayList<>();
        samples.add(row("student_no", "2026001", "name", "周一同", "class_name", "高一(1)班", "campus", "东校区"));
        samples.add(row("student_no", "2026002", "name", "吴二明", "class_name", "高一(3)班", "campus", "东校区"));
        samples.add(row("student_no", "2026003", "name", "郑三华", "class_name", "高一(5)班", "campus", "西校区"));
        return new TemplateDef("分班查询（模板）", "新生分班结果查询，已内置演示数据可直接发布", fields, page, samples);
    }

    private static TemplateDef staffDir()
    {
        List<BizQueryField> fields = new ArrayList<>();
        fields.add(f("name", "姓名", "string", "1", "LIKE", "input", "1", "0", 1, 100));
        fields.add(f("dept", "部门", "string", "1", "EQ", "input", "1", "0", 2, 120));
        fields.add(f("phone", "电话", "string", "0", "EQ", "input", "1", "0", 3, 120));
        fields.add(f("email", "邮箱", "string", "0", "EQ", "input", "1", "0", 4, 160));
        Map<String, Object> page = pageHints("通讯录查询", "按姓名或部门查询联系方式", "未找到匹配人员", "#0f766e");
        Map<String, Object> layout = baseLayout("default", "通讯录查询结果", "name",
            Arrays.asList("dept", "phone"), null);
        page.put("layoutJson", layout);
        List<Map<String, String>> samples = new ArrayList<>();
        samples.add(row("name", "赵六", "dept", "教务处", "phone", "13800001111", "email", "zhaoliu@example.com"));
        samples.add(row("name", "钱七", "dept", "信息中心", "phone", "13800002222", "email", "qianqi@example.com"));
        samples.add(row("name", "孙八", "dept", "教务处", "phone", "13800003333", "email", "sunba@example.com"));
        return new TemplateDef("通讯录查询（模板）", "按姓名/部门查询，已内置演示数据可直接发布", fields, page, samples);
    }

    private static Map<String, Object> pageHints(String title, String subtitle, String tips, String color)
    {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("title", title);
        page.put("subtitle", subtitle);
        page.put("resultTips", tips);
        page.put("themeColor", color);
        return page;
    }

    private static Map<String, Object> baseLayout(String resultStyle, String resultTitle, String titleField,
        List<String> summaryFields, String emptyGuide)
    {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("resultStyle", resultStyle);
        layout.put("resultTitle", resultTitle);
        layout.put("resultTitleField", titleField);
        layout.put("resultSummaryFields", summaryFields);
        layout.put("resultLayout", "card");
        layout.put("resultCardColumns", 1);
        layout.put("resultShowExport", true);
        layout.put("resultShowPrint", true);
        layout.put("formColumns", "2");
        if (emptyGuide != null)
        {
            layout.put("resultEmptyGuide", emptyGuide);
        }
        return layout;
    }

    private static Map<String, String> row(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4)
    {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        m.put(k4, v4);
        return m;
    }

    private static BizQueryField f(String key, String label, String dataType, String isQuery, String queryType,
        String htmlType, String isList, String isSortable, int sort, int width)
    {
        BizQueryField field = new BizQueryField();
        field.setFieldKey(key);
        field.setFieldName(key);
        field.setFieldLabel(label);
        field.setDataType(dataType);
        field.setIsQuery(isQuery);
        field.setQueryType(queryType);
        field.setHtmlType(htmlType);
        field.setIsList(isList);
        field.setIsSortable(isSortable);
        field.setSort(sort);
        field.setWidth(width);
        field.setIsRequired("1".equals(isQuery) ? "1" : "0");
        String mask = "none";
        if (label != null)
        {
            if (label.contains("手机") || label.contains("电话")) mask = "phone";
            else if (label.contains("身份证")) mask = "idcard";
            else if (label.contains("姓名") || label.contains("名字")) mask = "name";
            else if (label.contains("邮箱")) mask = "email";
        }
        field.setMaskType(mask);
        return field;
    }

    public static final class TemplateDef
    {
        public final String queryName;
        public final String queryDesc;
        public final List<BizQueryField> fields;
        public final Map<String, Object> pageHints;
        public final List<Map<String, String>> sampleRows;

        public TemplateDef(String queryName, String queryDesc, List<BizQueryField> fields, Map<String, Object> pageHints,
            List<Map<String, String>> sampleRows)
        {
            this.queryName = queryName;
            this.queryDesc = queryDesc;
            this.fields = Collections.unmodifiableList(fields);
            this.pageHints = pageHints;
            this.sampleRows = sampleRows == null ? Collections.emptyList() : Collections.unmodifiableList(sampleRows);
        }
    }
}
