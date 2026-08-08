package com.ruoyi.biz.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.domain.BizSurveyAnswer;
import com.ruoyi.biz.domain.BizSurveyAnswerItem;
import com.ruoyi.biz.domain.BizSurveyDraft;
import com.ruoyi.biz.domain.BizSurveyQuestion;
import com.ruoyi.biz.domain.vo.BizSurveyDetailVo;
import com.ruoyi.biz.mapper.BizSurveyAnswerItemMapper;
import com.ruoyi.biz.mapper.BizSurveyAnswerMapper;
import com.ruoyi.biz.mapper.BizSurveyDraftMapper;
import com.ruoyi.biz.mapper.BizSurveyMapper;
import com.ruoyi.biz.mapper.BizSurveyQuestionMapper;
import com.ruoyi.biz.service.IBizNotifyService;
import com.ruoyi.biz.service.IBizSurveyService;
import com.ruoyi.biz.service.IBizUserProjectService;
import com.ruoyi.biz.utils.BizAccessLogHelper;
import com.ruoyi.biz.utils.BizAccessPwdHelper;
import com.ruoyi.biz.utils.BizProjectScopeHelper;
import com.ruoyi.biz.utils.BizSurveyTemplates;
import com.ruoyi.biz.utils.OpenCaptchaHelper;
import com.ruoyi.biz.utils.SurveyJumpHelper;
import com.ruoyi.biz.utils.SurveyWebhookHelper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;

@Service
public class BizSurveyServiceImpl implements IBizSurveyService
{
    private static final String CODE_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(
        Arrays.asList(
            "radio", "checkbox", "input", "textarea", "select", "rate", "date", "phone", "file",
            "yesno", "number", "nps", "section", "page_break", "agreement", "signature",
            "email", "datetime", "slider", "image_radio", "matrix_radio", "cascade_select",
            "time", "url", "idcard", "image_checkbox", "likert"
        ));

    private static final Set<String> DISPLAY_ONLY_TYPES = new HashSet<>(Arrays.asList("section", "page_break"));

    private static boolean isDisplayOnly(String qType)
    {
        return DISPLAY_ONLY_TYPES.contains(qType);
    }

    private static final Set<String> CHOICE_TYPES = new HashSet<>(
        Arrays.asList("radio", "checkbox", "select", "yesno", "image_radio", "image_checkbox", "likert", "cascade_select", "agreement"));

    private static final Set<String> TEXT_STAT_TYPES = new HashSet<>(
        Arrays.asList("input", "textarea", "phone", "date", "rate", "file", "signature", "number", "nps", "email", "datetime", "slider",
            "time", "url", "idcard"));

    /** Multi-select types store answers as JSON arrays. */
    private static final Set<String> MULTI_CHOICE_TYPES = new HashSet<>(
        Arrays.asList("checkbox", "image_checkbox"));

    /** survey open upload: images + common docs */
    private static final String[] SURVEY_UPLOAD_EXTENSIONS = {
        "bmp", "gif", "jpg", "jpeg", "png", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar"
    };

    /** Hard ceiling; must stay aligned with frontend SURVEY_UPLOAD_HARD_MAX_MB. */
    private static final long SURVEY_UPLOAD_MAX_BYTES = 10L * 1024 * 1024;

    @Autowired
    private BizSurveyMapper surveyMapper;
    @Autowired
    private BizSurveyQuestionMapper questionMapper;
    @Autowired
    private BizSurveyAnswerMapper answerMapper;
    @Autowired
    private BizSurveyAnswerItemMapper answerItemMapper;
    @Autowired
    private BizSurveyDraftMapper draftMapper;
    @Autowired
    private IBizNotifyService notifyService;
    @Autowired
    private BizProjectScopeHelper projectScopeHelper;
    @Autowired
    private IBizUserProjectService userProjectService;

    @Override
    @DataScope(deptAlias = "s", userAlias = "s", userField = "create_user_id", permission = "biz:survey:list")
    public List<BizSurvey> selectBizSurveyList(BizSurvey survey)
    {
        List<BizSurvey> list = surveyMapper.selectBizSurveyList(survey);
        if (list != null)
        {
            for (BizSurvey s : list)
            {
                if (s != null)
                {
                    s.setAccessPwd(BizAccessPwdHelper.maskForApi(s.getAccessPwd()));
                }
            }
        }
        return list;
    }

    @Override
    public BizSurveyDetailVo selectDetail(Long surveyId)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        return buildDetail(survey);
    }

    @Override
    public BizSurvey insertBizSurvey(BizSurvey survey)
    {
        survey.setStatus("0");
        survey.setViewCount(0L);
        survey.setAnswerCount(0L);
        if (survey.getMaxAnswers() == null)
        {
            survey.setMaxAnswers(0);
        }
        if (StringUtils.isEmpty(survey.getAllowMulti()))
        {
            survey.setAllowMulti("1");
        }
        if (survey.getDailyLimit() == null)
        {
            survey.setDailyLimit(0);
        }
        if (StringUtils.isEmpty(survey.getNeedCaptcha()))
        {
            survey.setNeedCaptcha("0");
        }
        survey.setAccessPwd(BizAccessPwdHelper.encodeForStore(survey.getAccessPwd()));
        survey.setCreateUserId(SecurityUtils.getUserId());
        survey.setDeptId(SecurityUtils.getDeptId());
        survey.setCreateBy(SecurityUtils.getUsername());
        surveyMapper.insertBizSurvey(survey);
        survey.setAccessPwd(BizAccessPwdHelper.maskForApi(survey.getAccessPwd()));
        return survey;
    }

    @Override
    public int updateBizSurvey(BizSurvey survey)
    {
        BizSurvey db = requireSurvey(survey.getSurveyId());
        checkOwner(db);
        if (StringUtils.isNotEmpty(survey.getWebhookUrl()) && !SurveyWebhookHelper.isValidUrl(survey.getWebhookUrl()))
        {
            throw new ServiceException("Webhook 地址须以 http:// 或 https:// 开头");
        }
        survey.setAccessPwd(BizAccessPwdHelper.prepareForUpdate(survey.getAccessPwd()));
        survey.setUpdateBy(SecurityUtils.getUsername());
        survey.setCreateUserId(null);
        survey.setCreateBy(null);
        return surveyMapper.updateBizSurvey(survey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizSurveyByIds(Long[] surveyIds)
    {
        for (Long id : surveyIds)
        {
            BizSurvey db = requireSurvey(id);
            checkOwner(db);
            answerItemMapper.deleteBySurveyId(id);
            answerMapper.deleteBySurveyId(id);
            draftMapper.deleteBySurveyId(id);
            questionMapper.deleteBySurveyId(id);
        }
        return surveyMapper.deleteBizSurveyByIds(surveyIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveQuestions(Long surveyId, List<BizSurveyQuestion> questions)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        if (questions == null || questions.isEmpty())
        {
            throw new ServiceException("至少保留一道题目");
        }
        if (questions.size() > 100)
        {
            throw new ServiceException("题目数量不能超过 100");
        }
        int sort = 0;
        for (BizSurveyQuestion q : questions)
        {
            if (StringUtils.isEmpty(q.getTitle()) && !"page_break".equals(q.getQType()))
            {
                throw new ServiceException("题目标题不能为空");
            }
            if (StringUtils.isEmpty(q.getQType()) || !ALLOWED_TYPES.contains(q.getQType()))
            {
                throw new ServiceException("不支持的题型: " + q.getQType());
            }
            if (isDisplayOnly(q.getQType()))
            {
                q.setRequired("0");
                if (q.getTitle() == null)
                {
                    q.setTitle("");
                }
            }
            if ("yesno".equals(q.getQType()) && StringUtils.isEmpty(q.getOptionsJson()))
            {
                q.setOptionsJson("[{\"label\":\"是\",\"value\":\"1\"},{\"label\":\"否\",\"value\":\"0\"}]");
            }
            if ("likert".equals(q.getQType()) && StringUtils.isEmpty(q.getOptionsJson()))
            {
                q.setOptionsJson("[{\"label\":\"非常不同意\",\"value\":\"1\"},{\"label\":\"不同意\",\"value\":\"2\"},{\"label\":\"一般\",\"value\":\"3\"},{\"label\":\"同意\",\"value\":\"4\"},{\"label\":\"非常同意\",\"value\":\"5\"}]");
            }
            if (CHOICE_TYPES.contains(q.getQType()) && !"agreement".equals(q.getQType())
                && StringUtils.isEmpty(q.getOptionsJson()))
            {
                throw new ServiceException("选择题必须配置选项: " + q.getTitle());
            }
            if ("matrix_radio".equals(q.getQType()))
            {
                if (StringUtils.isEmpty(q.getOptionsJson()))
                {
                    throw new ServiceException("矩阵题必须配置量表选项: " + q.getTitle());
                }
                if (!hasMatrixRows(q.getPropsJson()))
                {
                    throw new ServiceException("矩阵题必须配置至少一行陈述: " + q.getTitle());
                }
            }
            q.setSurveyId(surveyId);
            q.setSort(sort++);
            if (StringUtils.isEmpty(q.getRequired()))
            {
                q.setRequired("0");
            }
        }
        long answerable = questions.stream().filter(x -> !isDisplayOnly(x.getQType())).count();
        if (answerable < 1)
        {
            throw new ServiceException("至少保留一道可作答题（说明段落与分页符不算）");
        }
        questionMapper.deleteBySurveyId(surveyId);
        return questionMapper.batchInsert(questions);
    }

    @Override
    public String publish(Long surveyId)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(surveyId);
        if (questions == null || questions.isEmpty()
            || questions.stream().noneMatch(q -> !isDisplayOnly(q.getQType())))
        {
            throw new ServiceException("请先设计可作答题目再发布");
        }
        String code = survey.getPublicCode();
        if (StringUtils.isEmpty(code))
        {
            code = genUniqueCode();
        }
        BizSurvey upd = new BizSurvey();
        upd.setSurveyId(surveyId);
        upd.setPublicCode(code);
        upd.setStatus("1");
        upd.setUpdateBy(SecurityUtils.getUsername());
        surveyMapper.updateBizSurvey(upd);
        return code;
    }

    @Override
    public int offline(Long surveyId)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        BizSurvey upd = new BizSurvey();
        upd.setSurveyId(surveyId);
        upd.setStatus("2");
        upd.setUpdateBy(SecurityUtils.getUsername());
        return surveyMapper.updateBizSurvey(upd);
    }

    @Override
    public List<BizSurveyAnswer> selectAnswerList(BizSurveyAnswer answer)
    {
        if (answer.getSurveyId() == null)
        {
            throw new ServiceException("surveyId 不能为空");
        }
        BizSurvey survey = requireSurvey(answer.getSurveyId());
        checkOwner(survey);
        return answerMapper.selectAnswerList(answer);
    }

    @Override
    public BizSurveyAnswer selectAnswerDetail(Long answerId)
    {
        BizSurveyAnswer answer = answerMapper.selectAnswerById(answerId);
        if (answer == null)
        {
            throw new ServiceException("答卷不存在");
        }
        BizSurvey survey = requireSurvey(answer.getSurveyId());
        checkOwner(survey);
        List<BizSurveyAnswerItem> items = answerItemMapper.selectByAnswerId(answerId);
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(survey.getSurveyId());
        Map<Long, BizSurveyQuestion> qMap = new HashMap<>();
        for (BizSurveyQuestion q : questions)
        {
            qMap.put(q.getQuestionId(), q);
        }
        for (BizSurveyAnswerItem it : items)
        {
            BizSurveyQuestion q = qMap.get(it.getQuestionId());
            if (q != null)
            {
                it.setDisplayValue(formatExportValue(q, it.getAnswerValue()));
            }
            else
            {
                it.setDisplayValue(it.getAnswerValue());
            }
        }
        answer.setItems(items);
        return answer;
    }

    @Override
    public int updateAnswerMeta(BizSurveyAnswer answer)
    {
        if (answer == null || answer.getAnswerId() == null)
        {
            throw new ServiceException("answerId 不能为空");
        }
        BizSurveyAnswer exist = answerMapper.selectAnswerById(answer.getAnswerId());
        if (exist == null)
        {
            throw new ServiceException("答卷不存在");
        }
        BizSurvey survey = requireSurvey(exist.getSurveyId());
        checkOwner(survey);
        if (StringUtils.isNotEmpty(answer.getValidFlag())
            && !"0".equals(answer.getValidFlag()) && !"1".equals(answer.getValidFlag()))
        {
            throw new ServiceException("validFlag 仅支持 0/1");
        }
        BizSurveyAnswer upd = new BizSurveyAnswer();
        upd.setAnswerId(answer.getAnswerId());
        upd.setValidFlag(answer.getValidFlag());
        if (answer.getRemark() != null)
        {
            upd.setRemark(StringUtils.substring(answer.getRemark(), 0, 500));
        }
        return answerMapper.updateAnswerMeta(upd);
    }

    @Override
    public Map<String, Object> selectStats(Long surveyId)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(surveyId);
        List<BizSurveyAnswerItem> items = answerItemMapper.selectBySurveyId(surveyId);
        long totalAnswers = answerMapper.countValidBySurveyId(surveyId);
        long totalAll = answerMapper.countBySurveyId(surveyId);
        long invalidCount = Math.max(0, totalAll - totalAnswers);

        // 一次加载有效答卷时间，供趋势复用
        BizSurveyAnswer validQuery = new BizSurveyAnswer();
        validQuery.setSurveyId(surveyId);
        validQuery.setValidFlag("1");
        List<BizSurveyAnswer> validAnswers = answerMapper.selectAnswerList(validQuery);
        Map<Long, Date> timeMap = new HashMap<>();
        for (BizSurveyAnswer a : validAnswers)
        {
            if (a.getSubmitTime() != null)
            {
                timeMap.put(a.getAnswerId(), a.getSubmitTime());
            }
        }

        List<Map<String, Object>> questionStats = new ArrayList<>();
        List<Map<String, Object>> textStats = new ArrayList<>();
        for (BizSurveyQuestion q : questions)
        {
            if (isDisplayOnly(q.getQType()))
            {
                continue;
            }
            if (TEXT_STAT_TYPES.contains(q.getQType()))
            {
                List<String> values = new ArrayList<>();
                for (BizSurveyAnswerItem item : items)
                {
                    if (q.getQuestionId().equals(item.getQuestionId()) && StringUtils.isNotEmpty(item.getAnswerValue()))
                    {
                        values.add(item.getAnswerValue());
                    }
                }
                Map<String, Object> ts = new LinkedHashMap<>();
                ts.put("questionId", q.getQuestionId());
                ts.put("title", q.getTitle());
                ts.put("qType", q.getQType());
                ts.put("count", values.size());
                List<String> displaySamples = new ArrayList<>();
                int sampleLimit = Math.min(50, values.size());
                for (int i = 0; i < sampleLimit; i++)
                {
                    displaySamples.add(formatExportValue(q, values.get(i)));
                }
                ts.put("samples", displaySamples);
                if ("nps".equals(q.getQType()) || "rate".equals(q.getQType()) || "number".equals(q.getQType()) || "slider".equals(q.getQType()))
                {
                    ts.put("avg", avgNumeric(values));
                    ts.put("distribution", buildNumericDistribution(values, totalAnswers));
                }
                if ("nps".equals(q.getQType()))
                {
                    putNpsBreakdown(ts, values);
                }
                if ("nps".equals(q.getQType()) || "rate".equals(q.getQType()) || "slider".equals(q.getQType()))
                {
                    ts.put("trends", buildNumericTrends(q.getQuestionId(), items, timeMap));
                }
                textStats.add(ts);
                continue;
            }
            if ("matrix_radio".equals(q.getQType()))
            {
                questionStats.add(buildMatrixStats(q, items, totalAnswers));
                continue;
            }
            if (!CHOICE_TYPES.contains(q.getQType()))
            {
                continue;
            }
            Map<String, Long> counter = new LinkedHashMap<>();
            List<Map<String, Object>> options = new ArrayList<>();
            try
            {
                if (StringUtils.isNotEmpty(q.getOptionsJson()))
                {
                    JSONArray arr = JSON.parseArray(q.getOptionsJson());
                    if ("cascade_select".equals(q.getQType()))
                    {
                        collectCascadeLeafOptions(arr, options, counter);
                    }
                    else
                    {
                        for (int i = 0; i < arr.size(); i++)
                        {
                            Object o = arr.get(i);
                            String value;
                            String label;
                            if (o instanceof Map)
                            {
                                Map<?, ?> m = (Map<?, ?>) o;
                                value = String.valueOf(m.get("value"));
                                label = String.valueOf(m.get("label"));
                            }
                            else
                            {
                                value = String.valueOf(o);
                                label = value;
                            }
                            counter.put(value, 0L);
                            Map<String, Object> opt = new LinkedHashMap<>();
                            opt.put("value", value);
                            opt.put("label", label);
                            options.add(opt);
                        }
                    }
                }
            }
            catch (Exception ignored)
            {
            }

            if ("agreement".equals(q.getQType()) && options.isEmpty())
            {
                counter.put("1", 0L);
                Map<String, Object> opt = new LinkedHashMap<>();
                opt.put("value", "1");
                opt.put("label", "已同意");
                options.add(opt);
            }

            for (BizSurveyAnswerItem item : items)
            {
                if (!q.getQuestionId().equals(item.getQuestionId()))
                {
                    continue;
                }
                String av = item.getAnswerValue();
                if (StringUtils.isEmpty(av))
                {
                    continue;
                }
                if (MULTI_CHOICE_TYPES.contains(q.getQType()))
                {
                    try
                    {
                        JSONArray vals = JSON.parseArray(av);
                        for (int i = 0; i < vals.size(); i++)
                        {
                            String v = String.valueOf(vals.get(i));
                            counter.put(v, counter.getOrDefault(v, 0L) + 1);
                        }
                    }
                    catch (Exception ex)
                    {
                        counter.put(av, counter.getOrDefault(av, 0L) + 1);
                    }
                }
                else if ("cascade_select".equals(q.getQType()))
                {
                    String leaf = cascadeLeafValue(av);
                    if (StringUtils.isNotEmpty(leaf))
                    {
                        counter.put(leaf, counter.getOrDefault(leaf, 0L) + 1);
                    }
                }
                else
                {
                    counter.put(av, counter.getOrDefault(av, 0L) + 1);
                }
            }

            List<Map<String, Object>> optionStats = new ArrayList<>();
            long sum = 0;
            for (Map<String, Object> opt : options)
            {
                String value = String.valueOf(opt.get("value"));
                long cnt = counter.getOrDefault(value, 0L);
                sum += cnt;
                Map<String, Object> os = new LinkedHashMap<>();
                os.put("value", value);
                os.put("label", opt.get("label"));
                os.put("count", cnt);
                optionStats.add(os);
            }
            for (Map.Entry<String, Long> e : counter.entrySet())
            {
                boolean known = options.stream().anyMatch(o -> String.valueOf(o.get("value")).equals(e.getKey()));
                if (!known && e.getValue() > 0)
                {
                    Map<String, Object> os = new LinkedHashMap<>();
                    os.put("value", e.getKey());
                    os.put("label", e.getKey());
                    os.put("count", e.getValue());
                    optionStats.add(os);
                    sum += e.getValue();
                }
            }
            for (Map<String, Object> os : optionStats)
            {
                long cnt = ((Number) os.get("count")).longValue();
                double pct = totalAnswers == 0 ? 0 : (cnt * 100.0 / totalAnswers);
                os.put("percent", Math.round(pct * 10) / 10.0);
            }
            Map<String, Object> qs = new LinkedHashMap<>();
            qs.put("questionId", q.getQuestionId());
            qs.put("title", q.getTitle());
            qs.put("qType", q.getQType());
            qs.put("options", optionStats);
            qs.put("answeredCount", sum);
            questionStats.add(qs);
        }

        Long viewCount = survey.getViewCount() == null ? 0L : survey.getViewCount();
        double convertRate = viewCount == 0 ? 0 : Math.round(totalAnswers * 1000.0 / viewCount) / 10.0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("surveyId", surveyId);
        data.put("surveyName", survey.getSurveyName());
        data.put("answerCount", totalAnswers);
        data.put("invalidCount", invalidCount);
        data.put("totalCount", totalAll);
        data.put("viewCount", viewCount);
        data.put("convertRate", convertRate);
        data.put("questions", questionStats);
        data.put("textQuestions", textStats);
        data.put("channels", answerMapper.selectChannelStats(surveyId));
        data.put("dailyTrends", answerMapper.selectDailyStats(surveyId));
        return data;
    }

    @Override
    public Map<String, Object> selectAnswerMatrix(Long surveyId, Integer pageNum, Integer pageSize, String validFlag)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        String vf = StringUtils.isEmpty(validFlag) ? "1" : validFlag;
        if (!"0".equals(vf) && !"1".equals(vf) && !"all".equals(vf))
        {
            vf = "1";
        }

        BizSurveyAnswer query = new BizSurveyAnswer();
        query.setSurveyId(surveyId);
        if (!"all".equals(vf))
        {
            query.setValidFlag(vf);
        }
        List<BizSurveyAnswer> allAnswers = answerMapper.selectAnswerList(query);
        int total = allAnswers.size();
        int from = Math.min((pn - 1) * ps, total);
        int to = Math.min(from + ps, total);
        List<BizSurveyAnswer> pageAnswers = allAnswers.subList(from, to);

        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(surveyId);
        List<BizSurveyQuestion> answerable = new ArrayList<>();
        for (BizSurveyQuestion q : questions)
        {
            if (!isDisplayOnly(q.getQType()))
            {
                answerable.add(q);
            }
        }

        // signatures bound to an agreement are shown inside agreement viewer, not as separate columns
        Set<Long> embeddedSignatureIds = new HashSet<>();
        Map<Integer, List<BizSurveyQuestion>> signaturesByAgreementSort = new HashMap<>();
        for (BizSurveyQuestion q : answerable)
        {
            if (!"signature".equals(q.getQType()))
            {
                continue;
            }
            Map<String, Object> props = parseProps(q.getPropsJson());
            Object bind = props.get("bindAgreementSort");
            if (bind == null || "".equals(String.valueOf(bind)))
            {
                continue;
            }
            try
            {
                int sort = Integer.parseInt(String.valueOf(bind));
                embeddedSignatureIds.add(q.getQuestionId());
                signaturesByAgreementSort.computeIfAbsent(sort, k -> new ArrayList<>()).add(q);
            }
            catch (Exception ignored)
            {
            }
        }

        List<Map<String, Object>> columns = new ArrayList<>();
        for (BizSurveyQuestion q : answerable)
        {
            if (embeddedSignatureIds.contains(q.getQuestionId()))
            {
                continue;
            }
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("questionId", q.getQuestionId());
            col.put("title", q.getTitle());
            col.put("qType", q.getQType());
            col.put("sort", q.getSort());
            Map<String, Object> props = parseProps(q.getPropsJson());
            if ("agreement".equals(q.getQType()))
            {
                col.put("content", props.get("content") == null ? "" : String.valueOf(props.get("content")));
                col.put("agreeLabel", props.get("agreeLabel") == null ? "我已阅读并同意" : String.valueOf(props.get("agreeLabel")));
                List<Map<String, Object>> boundSigs = new ArrayList<>();
                int sort = q.getSort() == null ? -1 : q.getSort();
                List<BizSurveyQuestion> sigs = signaturesByAgreementSort.getOrDefault(sort, Collections.emptyList());
                for (BizSurveyQuestion sq : sigs)
                {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("questionId", sq.getQuestionId());
                    s.put("title", sq.getTitle());
                    boundSigs.add(s);
                }
                col.put("boundSignatures", boundSigs);
            }
            columns.add(col);
        }

        Map<Long, Map<Long, String>> answerItemMap = new HashMap<>();
        if (!pageAnswers.isEmpty())
        {
            List<Long> ids = new ArrayList<>(pageAnswers.size());
            for (BizSurveyAnswer a : pageAnswers)
            {
                ids.add(a.getAnswerId());
            }
            List<BizSurveyAnswerItem> items = answerItemMapper.selectByAnswerIds(ids);
            for (BizSurveyAnswerItem it : items)
            {
                answerItemMap.computeIfAbsent(it.getAnswerId(), k -> new HashMap<>())
                    .put(it.getQuestionId(), it.getAnswerValue());
            }
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < pageAnswers.size(); i++)
        {
            BizSurveyAnswer a = pageAnswers.get(i);
            Map<Long, String> valueMap = answerItemMap.getOrDefault(a.getAnswerId(), Collections.emptyMap());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("answerId", a.getAnswerId());
            row.put("index", from + i + 1);
            row.put("label", "#" + (from + i + 1));
            row.put("submitTime", a.getSubmitTime() == null ? "" : sdf.format(a.getSubmitTime()));
            row.put("submitIp", StringUtils.nvl(a.getSubmitIp(), ""));
            row.put("channelCode", StringUtils.nvl(a.getChannelCode(), ""));
            row.put("validFlag", StringUtils.isEmpty(a.getValidFlag()) ? "1" : a.getValidFlag());

            Map<String, Object> cells = new LinkedHashMap<>();
            for (BizSurveyQuestion q : answerable)
            {
                String raw = valueMap.get(q.getQuestionId());
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("display", formatExportValue(q, raw));
                cell.put("raw", raw == null ? "" : raw);
                if ("file".equals(q.getQType()) || "signature".equals(q.getQType()))
                {
                    cell.put("url", extractUploadPath(raw));
                    cell.put("fileName", extractUploadName(raw));
                }
                cells.put(String.valueOf(q.getQuestionId()), cell);
            }
            row.put("cells", cells);
            rows.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("surveyId", surveyId);
        data.put("surveyName", survey.getSurveyName());
        data.put("total", total);
        data.put("pageNum", pn);
        data.put("pageSize", ps);
        data.put("validFlag", vf);
        data.put("columns", columns);
        data.put("rows", rows);
        return data;
    }

    private String extractUploadPath(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return "";
        }
        try
        {
            if (raw.trim().startsWith("{"))
            {
                JSONObject obj = JSON.parseObject(raw);
                String path = obj.getString("fileName");
                if (StringUtils.isEmpty(path))
                {
                    path = obj.getString("url");
                }
                return path == null ? "" : path;
            }
            if (raw.startsWith("data:") || raw.startsWith("http") || raw.startsWith("/"))
            {
                return raw;
            }
        }
        catch (Exception ignored)
        {
        }
        return "";
    }

    private String extractUploadName(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return "";
        }
        try
        {
            if (raw.trim().startsWith("{"))
            {
                JSONObject obj = JSON.parseObject(raw);
                String name = obj.getString("originalFilename");
                if (StringUtils.isNotEmpty(name))
                {
                    return name;
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return "";
    }

    @Override
    public Map<String, Object> selectCrossStats(Long surveyId, Long q1Id, Long q2Id)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        if (q1Id == null || q2Id == null || q1Id.equals(q2Id))
        {
            throw new ServiceException("请选择两道不同的单选题");
        }
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(surveyId);
        BizSurveyQuestion q1 = null;
        BizSurveyQuestion q2 = null;
        for (BizSurveyQuestion q : questions)
        {
            if (q1Id.equals(q.getQuestionId())) { q1 = q; }
            if (q2Id.equals(q.getQuestionId())) { q2 = q; }
        }
        Set<String> choice = new HashSet<>(Arrays.asList("radio", "select", "yesno", "image_radio", "likert"));
        if (q1 == null || q2 == null || !choice.contains(q1.getQType()) || !choice.contains(q2.getQType()))
        {
            throw new ServiceException("交叉分析仅支持单选题（单选/下拉/是非/图片单选/量表）");
        }
        List<Map<String, Object>> opts1 = parseOptionsList(q1);
        List<Map<String, Object>> opts2 = parseOptionsList(q2);
        Map<String, String> label1 = new LinkedHashMap<>();
        Map<String, String> label2 = new LinkedHashMap<>();
        for (Map<String, Object> o : opts1) { label1.put(String.valueOf(o.get("value")), String.valueOf(o.get("label"))); }
        for (Map<String, Object> o : opts2) { label2.put(String.valueOf(o.get("value")), String.valueOf(o.get("label"))); }

        List<BizSurveyAnswerItem> items = answerItemMapper.selectBySurveyId(surveyId);
        Map<Long, Map<Long, String>> byAnswer = new HashMap<>();
        for (BizSurveyAnswerItem it : items)
        {
            if (!q1Id.equals(it.getQuestionId()) && !q2Id.equals(it.getQuestionId()))
            {
                continue;
            }
            byAnswer.computeIfAbsent(it.getAnswerId(), k -> new HashMap<>()).put(it.getQuestionId(), it.getAnswerValue());
        }
        Map<String, Long> counter = new LinkedHashMap<>();
        long paired = 0;
        for (Map<Long, String> pair : byAnswer.values())
        {
            String v1 = firstScalar(pair.get(q1Id));
            String v2 = firstScalar(pair.get(q2Id));
            if (StringUtils.isEmpty(v1) || StringUtils.isEmpty(v2))
            {
                continue;
            }
            paired++;
            String key = v1 + "\u0001" + v2;
            counter.put(key, counter.getOrDefault(key, 0L) + 1);
        }
        List<Map<String, Object>> cells = new ArrayList<>();
        for (Map.Entry<String, Long> e : counter.entrySet())
        {
            String[] parts = e.getKey().split("\u0001", 2);
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("rowValue", parts[0]);
            cell.put("rowLabel", label1.getOrDefault(parts[0], parts[0]));
            cell.put("colValue", parts.length > 1 ? parts[1] : "");
            cell.put("colLabel", label2.getOrDefault(parts.length > 1 ? parts[1] : "", parts.length > 1 ? parts[1] : ""));
            cell.put("count", e.getValue());
            cells.add(cell);
        }
        Map<String, Object> q1Info = new LinkedHashMap<>();
        q1Info.put("questionId", q1.getQuestionId());
        q1Info.put("title", q1.getTitle());
        q1Info.put("options", opts1);
        Map<String, Object> q2Info = new LinkedHashMap<>();
        q2Info.put("questionId", q2.getQuestionId());
        q2Info.put("title", q2.getTitle());
        q2Info.put("options", opts2);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("surveyId", surveyId);
        data.put("pairedCount", paired);
        data.put("q1", q1Info);
        data.put("q2", q2Info);
        data.put("cells", cells);
        return data;
    }

    private List<Map<String, Object>> parseOptionsList(BizSurveyQuestion q)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        if (StringUtils.isEmpty(q.getOptionsJson()))
        {
            if ("yesno".equals(q.getQType()))
            {
                list.add(Map.of("value", "1", "label", "是"));
                list.add(Map.of("value", "0", "label", "否"));
            }
            else if ("likert".equals(q.getQType()))
            {
                list.add(Map.of("value", "1", "label", "非常不同意"));
                list.add(Map.of("value", "2", "label", "不同意"));
                list.add(Map.of("value", "3", "label", "一般"));
                list.add(Map.of("value", "4", "label", "同意"));
                list.add(Map.of("value", "5", "label", "非常同意"));
            }
            return list;
        }
        try
        {
            JSONArray arr = JSON.parseArray(q.getOptionsJson());
            if (arr != null)
            {
                for (int i = 0; i < arr.size(); i++)
                {
                    JSONObject o = arr.getJSONObject(i);
                    if (o == null) continue;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("value", String.valueOf(o.get("value")));
                    m.put("label", o.get("label") == null ? String.valueOf(o.get("value")) : String.valueOf(o.get("label")));
                    list.add(m);
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return list;
    }

    private String firstScalar(String answer)
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

    @Override
    public Map<String, Object> openMeta(String code, String accessPwd)
    {
        BizSurvey survey = requireOpenable(code);
        Map<String, Object> data = new HashMap<>();
        data.put("code", survey.getPublicCode());
        data.put("surveyName", survey.getSurveyName());
        data.put("surveyDesc", survey.getSurveyDesc());
        boolean needPwd = StringUtils.isNotEmpty(survey.getAccessPwd());
        data.put("needPwd", needPwd);
        if (needPwd && !BizAccessPwdHelper.matches(survey.getAccessPwd(), accessPwd))
        {
            data.put("ready", false);
            data.put("unlocked", false);
            return data;
        }

        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(survey.getSurveyId());

        surveyMapper.increaseViewCount(survey.getSurveyId());
        BizAccessLogHelper.log("survey", survey.getSurveyId(), survey.getPublicCode(), "view");

        Map<String, Object> theme = new HashMap<>();
        if (StringUtils.isNotEmpty(survey.getThemeJson()))
        {
            try
            {
                theme = JSON.parseObject(survey.getThemeJson(), Map.class);
            }
            catch (Exception ignored)
            {
            }
        }

        data.put("theme", theme);
        data.put("questions", questions);
        data.put("needCaptcha", "1".equals(survey.getNeedCaptcha()));
        data.put("allowMulti", survey.getAllowMulti());
        data.put("ready", true);
        data.put("unlocked", true);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long openSubmit(String code, Map<String, Object> body, String ip, String ua)
    {
        BizSurvey survey = requireOpenable(code);
        survey = surveyMapper.selectBizSurveyByIdForUpdate(survey.getSurveyId());
        if (survey == null || !"1".equals(survey.getStatus()))
        {
            throw new ServiceException("问卷不存在或未发布");
        }
        Date now = new Date();
        if (survey.getStartTime() != null && now.before(survey.getStartTime()))
        {
            throw new ServiceException("问卷尚未开始");
        }
        if (survey.getEndTime() != null && now.after(survey.getEndTime()))
        {
            throw new ServiceException("问卷已结束");
        }
        BizAccessLogHelper.log("survey", survey.getSurveyId(), survey.getPublicCode(), "submit");
        String accessPwd = body == null || body.get("accessPwd") == null ? null : String.valueOf(body.get("accessPwd"));
        assertAccessPwd(survey.getAccessPwd(), accessPwd);
        if (survey.getMaxAnswers() != null && survey.getMaxAnswers() > 0)
        {
            long count = answerMapper.countBySurveyId(survey.getSurveyId());
            if (count >= survey.getMaxAnswers())
            {
                throw new ServiceException("答卷已达上限");
            }
        }
        String clientToken = "";
        if (body != null && body.get("clientToken") != null)
        {
            clientToken = StringUtils.substring(String.valueOf(body.get("clientToken")).trim(), 0, 64);
        }
        if ("0".equals(survey.getAllowMulti()))
        {
            if (StringUtils.isEmpty(clientToken) && StringUtils.isEmpty(ip))
            {
                throw new ServiceException("无法校验重复提交，请刷新页面后重试");
            }
            if (StringUtils.isNotEmpty(clientToken))
            {
                long tokenCount = answerMapper.countBySurveyIdAndClientToken(survey.getSurveyId(), clientToken);
                if (tokenCount > 0)
                {
                    throw new ServiceException("该问卷不允许重复提交");
                }
            }
            if (StringUtils.isNotEmpty(ip))
            {
                long ipCount = answerMapper.countBySurveyIdAndIp(survey.getSurveyId(), ip);
                if (ipCount > 0)
                {
                    throw new ServiceException("该问卷不允许重复提交");
                }
            }
        }
        if (survey.getDailyLimit() != null && survey.getDailyLimit() > 0)
        {
            long today = answerMapper.countTodayBySurveyId(survey.getSurveyId());
            if (today >= survey.getDailyLimit())
            {
                throw new ServiceException("今日答卷已达上限");
            }
        }
        if ("1".equals(survey.getNeedCaptcha()))
        {
            String captcha = body == null || body.get("code") == null ? null : String.valueOf(body.get("code"));
            String uuid = body == null || body.get("uuid") == null ? null : String.valueOf(body.get("uuid"));
            OpenCaptchaHelper.validate(captcha, uuid);
        }

        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(survey.getSurveyId());
        Map<Long, BizSurveyQuestion> qMap = questions.stream()
            .collect(Collectors.toMap(BizSurveyQuestion::getQuestionId, q -> q, (a, b) -> a));

        Object answersObj = body == null ? null : body.get("answers");
        if (!(answersObj instanceof List) || ((List<?>) answersObj).isEmpty())
        {
            throw new ServiceException("提交内容不能为空");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) answersObj;
        Map<Long, String> valueMap = new HashMap<>();
        for (Map<String, Object> a : answers)
        {
            if (a.get("questionId") == null)
            {
                continue;
            }
            Long qid = Long.valueOf(String.valueOf(a.get("questionId")));
            if (!qMap.containsKey(qid))
            {
                continue;
            }
            Object val = a.get("value");
            String stored;
            if (val == null)
            {
                stored = "";
            }
            else if (val instanceof List || val instanceof Map)
            {
                stored = JSON.toJSONString(val);
            }
            else
            {
                stored = String.valueOf(val).trim();
            }
            valueMap.put(qid, stored);
        }

        List<BizSurveyQuestion> visible = SurveyJumpHelper.visibleQuestions(questions, valueMap);
        for (BizSurveyQuestion q : visible)
        {
            if (isDisplayOnly(q.getQType()))
            {
                continue;
            }
            String v = valueMap.get(q.getQuestionId());
            if ("1".equals(q.getRequired()) && StringUtils.isEmpty(v))
            {
                throw new ServiceException("请完成必填题: " + q.getTitle());
            }
            if (MULTI_CHOICE_TYPES.contains(q.getQType()) && StringUtils.isNotEmpty(v))
            {
                try
                {
                    JSONArray arr = JSON.parseArray(v);
                    if ((arr == null || arr.isEmpty()) && "1".equals(q.getRequired()))
                    {
                        throw new ServiceException("请完成必填题: " + q.getTitle());
                    }
                }
                catch (ServiceException ex)
                {
                    throw ex;
                }
                catch (Exception ex)
                {
                    throw new ServiceException("多选题答案格式错误");
                }
            }
            validateChoiceAnswer(q, v);
            validateMatrixAnswer(q, v);
            validateCascadeAnswer(q, v);
            validateAnswerProps(q, v);
        }
        valueMap.keySet().retainAll(visible.stream()
            .filter(q -> !isDisplayOnly(q.getQType()))
            .map(BizSurveyQuestion::getQuestionId)
            .collect(Collectors.toSet()));

        Integer costMs = 0;
        if (body != null && body.get("costMs") != null)
        {
            try
            {
                costMs = Integer.parseInt(String.valueOf(body.get("costMs")));
            }
            catch (Exception ignored)
            {
            }
        }

        String channel = "";
        if (body != null && body.get("channel") != null)
        {
            channel = StringUtils.substring(String.valueOf(body.get("channel")).trim(), 0, 64);
        }
        BizSurveyAnswer answer = new BizSurveyAnswer();
        answer.setSurveyId(survey.getSurveyId());
        answer.setSubmitIp(StringUtils.substring(ip, 0, 128));
        answer.setSubmitUa(StringUtils.substring(ua, 0, 500));
        answer.setCostMs(costMs);
        answer.setChannelCode(StringUtils.isEmpty(channel) ? null : channel);
        answer.setClientToken(StringUtils.isEmpty(clientToken) ? null : clientToken);
        answerMapper.insertAnswer(answer);

        List<BizSurveyAnswerItem> items = new ArrayList<>();
        for (BizSurveyQuestion q : questions)
        {
            String v = valueMap.get(q.getQuestionId());
            if (v == null)
            {
                continue;
            }
            BizSurveyAnswerItem item = new BizSurveyAnswerItem();
            item.setAnswerId(answer.getAnswerId());
            item.setQuestionId(q.getQuestionId());
            item.setAnswerValue(v);
            items.add(item);
        }
        if (!items.isEmpty())
        {
            answerItemMapper.batchInsert(items);
        }
        surveyMapper.increaseAnswerCount(survey.getSurveyId());
        try
        {
            notifyService.createAnswerNotify(survey.getCreateUserId(), survey.getSurveyId(),
                answer.getAnswerId(), survey.getSurveyName(), answer.getChannelCode());
        }
        catch (Exception ignored)
        {
        }
        if (StringUtils.isNotEmpty(clientToken))
        {
            try
            {
                draftMapper.deleteBySurveyAndToken(survey.getSurveyId(), clientToken);
            }
            catch (Exception ignored)
            {
            }
        }
        dispatchWebhook(survey, answer.getAnswerId(), questions, valueMap);
        return answer.getAnswerId();
    }

    private void dispatchWebhook(BizSurvey survey, Long answerId, List<BizSurveyQuestion> questions, Map<Long, String> valueMap)
    {
        if (!SurveyWebhookHelper.isValidUrl(survey.getWebhookUrl()))
        {
            return;
        }
        try
        {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "survey.answer.submitted");
            payload.put("surveyId", survey.getSurveyId());
            payload.put("surveyName", survey.getSurveyName());
            payload.put("publicCode", survey.getPublicCode());
            payload.put("answerId", answerId);
            payload.put("submitTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            List<Map<String, Object>> answers = new ArrayList<>();
            for (BizSurveyQuestion q : questions)
            {
                if (!valueMap.containsKey(q.getQuestionId()))
                {
                    continue;
                }
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("questionId", q.getQuestionId());
                a.put("title", q.getTitle());
                a.put("qType", q.getQType());
                a.put("value", valueMap.get(q.getQuestionId()));
                answers.add(a);
            }
            payload.put("answers", answers);
            SurveyWebhookHelper.dispatchAsync(survey.getWebhookUrl(), JSON.toJSONString(payload), survey.getWebhookSecret());
        }
        catch (Exception ex)
        {
            // never break submit
        }
    }

    @Override
    public Map<String, Object> openUpload(String code, String accessPwd, MultipartFile file) throws Exception
    {
        BizSurvey survey = requireOpenable(code);
        assertAccessPwd(survey.getAccessPwd(), accessPwd);
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请选择文件");
        }
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(survey.getSurveyId());
        boolean hasUploadQ = questions.stream().anyMatch(q -> "file".equals(q.getQType()) || "signature".equals(q.getQType()));
        if (!hasUploadQ)
        {
            throw new ServiceException("该问卷未启用附件或签名题");
        }
        if (file.getSize() > SURVEY_UPLOAD_MAX_BYTES)
        {
            throw new ServiceException("附件不能超过 10MB");
        }
        String filePath = RuoYiConfig.getUploadPath();
        String fileName = FileUploadUtils.upload(filePath, file, SURVEY_UPLOAD_EXTENSIONS, true);
        Map<String, Object> data = new HashMap<>();
        data.put("fileName", fileName);
        data.put("url", fileName);
        data.put("originalFilename", file.getOriginalFilename());
        data.put("newFileName", FileUtils.getName(fileName));
        return data;
    }

    private void validateChoiceAnswer(BizSurveyQuestion q, String v)
    {
        if (!CHOICE_TYPES.contains(q.getQType()) || "cascade_select".equals(q.getQType())
            || "agreement".equals(q.getQType()) || StringUtils.isEmpty(v))
        {
            return;
        }
        Set<String> allowed = parseOptionValues(q);
        if (allowed.isEmpty())
        {
            throw new ServiceException("题目选项未配置: " + q.getTitle());
        }
        if (MULTI_CHOICE_TYPES.contains(q.getQType()))
        {
            try
            {
                JSONArray arr = JSON.parseArray(v);
                if (arr == null)
                {
                    throw new ServiceException("多选题答案格式错误");
                }
                for (int i = 0; i < arr.size(); i++)
                {
                    String item = String.valueOf(arr.get(i));
                    if (!allowed.contains(item))
                    {
                        throw new ServiceException("选项无效: " + q.getTitle());
                    }
                }
            }
            catch (ServiceException ex)
            {
                throw ex;
            }
            catch (Exception ex)
            {
                throw new ServiceException("多选题答案格式错误");
            }
            return;
        }
        if (!allowed.contains(v))
        {
            throw new ServiceException("选项无效: " + q.getTitle());
        }
    }

    private Set<String> parseOptionValues(BizSurveyQuestion q)
    {
        Set<String> allowed = new HashSet<>();
        String raw = q.getOptionsJson();
        if (StringUtils.isEmpty(raw) && "yesno".equals(q.getQType()))
        {
            allowed.add("1");
            allowed.add("0");
            return allowed;
        }
        if (StringUtils.isEmpty(raw) && "likert".equals(q.getQType()))
        {
            allowed.add("1");
            allowed.add("2");
            allowed.add("3");
            allowed.add("4");
            allowed.add("5");
            return allowed;
        }
        if (StringUtils.isEmpty(raw))
        {
            return allowed;
        }
        try
        {
            JSONArray arr = JSON.parseArray(raw);
            if (arr == null)
            {
                return allowed;
            }
            for (int i = 0; i < arr.size(); i++)
            {
                Object item = arr.get(i);
                if (item instanceof JSONObject)
                {
                    Object val = ((JSONObject) item).get("value");
                    if (val != null)
                    {
                        allowed.add(String.valueOf(val));
                    }
                }
                else if (item != null)
                {
                    allowed.add(String.valueOf(item));
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return allowed;
    }

    private void validateAnswerProps(BizSurveyQuestion q, String v)
    {
        if (StringUtils.isEmpty(v))
        {
            return;
        }
        Map<String, Object> props = parseProps(q.getPropsJson());
        if ("file".equals(q.getQType()) || "signature".equals(q.getQType()))
        {
            validateFileAnswer(q, v);
            return;
        }
        if ("agreement".equals(q.getQType()))
        {
            if (!"1".equals(v))
            {
                throw new ServiceException("请阅读并同意: " + q.getTitle());
            }
            return;
        }
        if ("phone".equals(q.getQType()) || "phone".equals(String.valueOf(props.getOrDefault("format", ""))))
        {
            if (!v.matches("^1\\d{10}$"))
            {
                throw new ServiceException("请输入正确手机号: " + q.getTitle());
            }
        }
        Object minLen = props.get("minLength");
        Object maxLen = props.get("maxLength");
        if (minLen != null && StringUtils.isNotEmpty(String.valueOf(minLen)))
        {
            int min = Integer.parseInt(String.valueOf(minLen));
            if (v.length() < min)
            {
                throw new ServiceException(q.getTitle() + " 至少 " + min + " 个字符");
            }
        }
        if (maxLen != null && StringUtils.isNotEmpty(String.valueOf(maxLen)))
        {
            int max = Integer.parseInt(String.valueOf(maxLen));
            if (v.length() > max)
            {
                throw new ServiceException(q.getTitle() + " 最多 " + max + " 个字符");
            }
        }
        if ("rate".equals(q.getQType()) || "nps".equals(q.getQType()) || "slider".equals(q.getQType()))
        {
            try
            {
                int rate = Integer.parseInt(v);
                int min = "nps".equals(q.getQType()) ? 0 : 1;
                int max = "nps".equals(q.getQType()) ? 10 : 5;
                if ("slider".equals(q.getQType()))
                {
                    min = props.get("min") != null ? Integer.parseInt(String.valueOf(props.get("min"))) : 0;
                    max = props.get("max") != null ? Integer.parseInt(String.valueOf(props.get("max"))) : 100;
                }
                else if (props.get("max") != null)
                {
                    max = Integer.parseInt(String.valueOf(props.get("max")));
                }
                if (props.get("min") != null && !"slider".equals(q.getQType()) && !"nps".equals(q.getQType()))
                {
                    min = Integer.parseInt(String.valueOf(props.get("min")));
                }
                if (rate < min || rate > max)
                {
                    throw new ServiceException("分值超出范围: " + q.getTitle());
                }
            }
            catch (ServiceException ex)
            {
                throw ex;
            }
            catch (Exception ex)
            {
                throw new ServiceException("分值格式错误: " + q.getTitle());
            }
        }
        if ("number".equals(q.getQType()))
        {
            try
            {
                double num = Double.parseDouble(v);
                if (props.get("min") != null && num < Double.parseDouble(String.valueOf(props.get("min"))))
                {
                    throw new ServiceException(q.getTitle() + " 不能小于 " + props.get("min"));
                }
                if (props.get("max") != null && num > Double.parseDouble(String.valueOf(props.get("max"))))
                {
                    throw new ServiceException(q.getTitle() + " 不能大于 " + props.get("max"));
                }
            }
            catch (ServiceException ex)
            {
                throw ex;
            }
            catch (Exception ex)
            {
                throw new ServiceException("请输入有效数字: " + q.getTitle());
            }
        }
        if ("email".equals(q.getQType()))
        {
            if (!v.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            {
                throw new ServiceException("请输入正确邮箱: " + q.getTitle());
            }
        }
        if ("url".equals(q.getQType()))
        {
            if (!v.matches("^(https?://).+"))
            {
                throw new ServiceException("请输入正确网址（需以 http:// 或 https:// 开头）: " + q.getTitle());
            }
        }
        if ("idcard".equals(q.getQType()))
        {
            if (!v.matches("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$"))
            {
                throw new ServiceException("请输入正确身份证号: " + q.getTitle());
            }
        }
        if ("time".equals(q.getQType()))
        {
            if (!v.matches("^([01]\\d|2[0-3]):[0-5]\\d$"))
            {
                throw new ServiceException("请输入正确时间（HH:mm）: " + q.getTitle());
            }
        }
        String pattern = props.get("pattern") == null ? null : String.valueOf(props.get("pattern"));
        if (StringUtils.isNotEmpty(pattern) && !v.matches(pattern))
        {
            throw new ServiceException("格式不正确: " + q.getTitle());
        }
    }



    private boolean hasMatrixRows(String propsJson)
    {
        try
        {
            JSONObject props = parsePropsJson(propsJson);
            JSONArray rows = props == null ? null : props.getJSONArray("rows");
            return rows != null && !rows.isEmpty();
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private JSONObject parsePropsJson(String propsJson)
    {
        if (StringUtils.isEmpty(propsJson))
        {
            return null;
        }
        try
        {
            return JSON.parseObject(propsJson);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private void validateMatrixAnswer(BizSurveyQuestion q, String v)
    {
        if (!"matrix_radio".equals(q.getQType()))
        {
            return;
        }
        List<String> rowKeys = matrixRowKeys(q.getPropsJson());
        if (rowKeys.isEmpty())
        {
            throw new ServiceException("矩阵题未配置行: " + q.getTitle());
        }
        Set<String> allowed = parseOptionValues(q);
        if (allowed.isEmpty())
        {
            throw new ServiceException("矩阵题选项未配置: " + q.getTitle());
        }
        Map<String, String> ans = new HashMap<>();
        if (StringUtils.isNotEmpty(v))
        {
            try
            {
                JSONObject obj = JSON.parseObject(v);
                if (obj != null)
                {
                    for (String key : obj.keySet())
                    {
                        Object val = obj.get(key);
                        if (val != null)
                        {
                            ans.put(key, String.valueOf(val));
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                throw new ServiceException("矩阵题答案格式错误: " + q.getTitle());
            }
        }
        if ("1".equals(q.getRequired()))
        {
            for (String rk : rowKeys)
            {
                if (StringUtils.isEmpty(ans.get(rk)))
                {
                    throw new ServiceException("请完成必填题: " + q.getTitle());
                }
            }
        }
        for (Map.Entry<String, String> e : ans.entrySet())
        {
            if (!rowKeys.contains(e.getKey()))
            {
                throw new ServiceException("矩阵题行无效: " + q.getTitle());
            }
            if (StringUtils.isNotEmpty(e.getValue()) && !allowed.contains(e.getValue()))
            {
                throw new ServiceException("矩阵题选项无效: " + q.getTitle());
            }
        }
    }

    private List<String> matrixRowKeys(String propsJson)
    {
        List<String> keys = new ArrayList<>();
        JSONObject props = parsePropsJson(propsJson);
        if (props == null)
        {
            return keys;
        }
        JSONArray rows = props.getJSONArray("rows");
        if (rows == null)
        {
            return keys;
        }
        for (int i = 0; i < rows.size(); i++)
        {
            Object o = rows.get(i);
            if (o instanceof JSONObject)
            {
                String value = ((JSONObject) o).getString("value");
                if (StringUtils.isEmpty(value))
                {
                    value = "r" + (i + 1);
                }
                keys.add(value);
            }
        }
        return keys;
    }

    private Map<String, Object> buildMatrixStats(BizSurveyQuestion q, List<BizSurveyAnswerItem> items, long totalAnswers)
    {
        List<Map<String, Object>> columns = new ArrayList<>();
        Map<String, String> colLabels = new LinkedHashMap<>();
        try
        {
            if (StringUtils.isNotEmpty(q.getOptionsJson()))
            {
                JSONArray arr = JSON.parseArray(q.getOptionsJson());
                for (int i = 0; i < arr.size(); i++)
                {
                    Object o = arr.get(i);
                    String value;
                    String label;
                    if (o instanceof Map)
                    {
                        Map<?, ?> m = (Map<?, ?>) o;
                        value = String.valueOf(m.get("value"));
                        label = String.valueOf(m.get("label"));
                    }
                    else
                    {
                        value = String.valueOf(o);
                        label = value;
                    }
                    colLabels.put(value, label);
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("value", value);
                    col.put("label", label);
                    columns.add(col);
                }
            }
        }
        catch (Exception ignored)
        {
        }
        List<Map<String, Object>> rowStats = new ArrayList<>();
        JSONObject props = parsePropsJson(q.getPropsJson());
        JSONArray rows = props == null ? null : props.getJSONArray("rows");
        if (rows != null)
        {
            for (int i = 0; i < rows.size(); i++)
            {
                Object o = rows.get(i);
                if (!(o instanceof JSONObject))
                {
                    continue;
                }
                JSONObject jo = (JSONObject) o;
                String rk = jo.getString("value");
                if (StringUtils.isEmpty(rk))
                {
                    rk = "r" + (i + 1);
                }
                String rlabel = jo.getString("label");
                if (StringUtils.isEmpty(rlabel))
                {
                    rlabel = rk;
                }
                Map<String, Long> counter = new LinkedHashMap<>();
                for (String ck : colLabels.keySet())
                {
                    counter.put(ck, 0L);
                }
                for (BizSurveyAnswerItem item : items)
                {
                    if (!q.getQuestionId().equals(item.getQuestionId()) || StringUtils.isEmpty(item.getAnswerValue()))
                    {
                        continue;
                    }
                    try
                    {
                        JSONObject ans = JSON.parseObject(item.getAnswerValue());
                        if (ans == null || !ans.containsKey(rk))
                        {
                            continue;
                        }
                        String cv = String.valueOf(ans.get(rk));
                        counter.put(cv, counter.getOrDefault(cv, 0L) + 1);
                    }
                    catch (Exception ignored)
                    {
                    }
                }
                List<Map<String, Object>> optionStats = new ArrayList<>();
                for (Map.Entry<String, Long> e : counter.entrySet())
                {
                    Map<String, Object> os = new LinkedHashMap<>();
                    os.put("value", e.getKey());
                    os.put("label", colLabels.getOrDefault(e.getKey(), e.getKey()));
                    os.put("count", e.getValue());
                    double pct = totalAnswers == 0 ? 0 : (e.getValue() * 100.0 / totalAnswers);
                    os.put("percent", Math.round(pct * 10) / 10.0);
                    optionStats.add(os);
                }
                Map<String, Object> rs = new LinkedHashMap<>();
                rs.put("rowValue", rk);
                rs.put("rowLabel", rlabel);
                rs.put("options", optionStats);
                rowStats.add(rs);
            }
        }
        Map<String, Object> qs = new LinkedHashMap<>();
        qs.put("questionId", q.getQuestionId());
        qs.put("title", q.getTitle());
        qs.put("qType", q.getQType());
        qs.put("columns", columns);
        qs.put("rows", rowStats);
        return qs;
    }

    private void putNpsBreakdown(Map<String, Object> ts, List<String> values)
    {
        int promoters = 0;
        int passives = 0;
        int detractors = 0;
        int scored = 0;
        for (String s : values)
        {
            try
            {
                int n = Integer.parseInt(String.valueOf(s).trim());
                if (n < 0 || n > 10)
                {
                    continue;
                }
                scored++;
                if (n >= 9)
                {
                    promoters++;
                }
                else if (n >= 7)
                {
                    passives++;
                }
                else
                {
                    detractors++;
                }
            }
            catch (Exception ignored)
            {
            }
        }
        ts.put("promoters", promoters);
        ts.put("passives", passives);
        ts.put("detractors", detractors);
        if (scored == 0)
        {
            ts.put("npsScore", null);
        }
        else
        {
            double score = (promoters - detractors) * 100.0 / scored;
            ts.put("npsScore", Math.round(score * 10) / 10.0);
        }
    }

    private String formatExportValue(BizSurveyQuestion q, String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return "";
        }
        if ("file".equals(q.getQType()) || "signature".equals(q.getQType()))
        {
            try
            {
                if (raw.trim().startsWith("{"))
                {
                    JSONObject obj = JSON.parseObject(raw);
                    String name = obj.getString("originalFilename");
                    String path = obj.getString("fileName");
                    if (StringUtils.isEmpty(path))
                    {
                        path = obj.getString("url");
                    }
                    if ("signature".equals(q.getQType()))
                    {
                        if (StringUtils.isNotEmpty(path) && path.startsWith("data:"))
                        {
                            return "[签名图]";
                        }
                        return StringUtils.isNotEmpty(path) ? path : "[签名]";
                    }
                    if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(path))
                    {
                        return name + " (" + path + ")";
                    }
                    if (StringUtils.isNotEmpty(path))
                    {
                        return path;
                    }
                    if (StringUtils.isNotEmpty(name))
                    {
                        return name;
                    }
                }
            }
            catch (Exception ignored)
            {
            }
            return raw;
        }
        if ("agreement".equals(q.getQType()))
        {
            return "1".equals(raw) ? "已同意" : raw;
        }
        if ("matrix_radio".equals(q.getQType()))
        {
            return formatMatrixValue(q, raw);
        }
        if ("cascade_select".equals(q.getQType()))
        {
            return formatCascadeValue(q, raw);
        }
        if (CHOICE_TYPES.contains(q.getQType()))
        {
            Map<String, String> labelMap = optionLabelMap(q);
            if (MULTI_CHOICE_TYPES.contains(q.getQType()))
            {
                try
                {
                    JSONArray arr = JSON.parseArray(raw);
                    if (arr == null)
                    {
                        return raw;
                    }
                    List<String> labels = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++)
                    {
                        String v = String.valueOf(arr.get(i));
                        labels.add(labelMap.getOrDefault(v, v));
                    }
                    return String.join("、", labels);
                }
                catch (Exception ex)
                {
                    return labelMap.getOrDefault(raw, raw);
                }
            }
            return labelMap.getOrDefault(raw, raw);
        }
        return raw;
    }

    private Map<String, String> optionLabelMap(BizSurveyQuestion q)
    {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isEmpty(q.getOptionsJson()) && "yesno".equals(q.getQType()))
        {
            map.put("1", "是");
            map.put("0", "否");
            return map;
        }
        if (StringUtils.isEmpty(q.getOptionsJson()) && "likert".equals(q.getQType()))
        {
            map.put("1", "非常不同意");
            map.put("2", "不同意");
            map.put("3", "一般");
            map.put("4", "同意");
            map.put("5", "非常同意");
            return map;
        }
        if (StringUtils.isEmpty(q.getOptionsJson()))
        {
            return map;
        }
        try
        {
            JSONArray arr = JSON.parseArray(q.getOptionsJson());
            if (arr == null)
            {
                return map;
            }
            for (int i = 0; i < arr.size(); i++)
            {
                Object o = arr.get(i);
                if (o instanceof JSONObject)
                {
                    JSONObject jo = (JSONObject) o;
                    String value = jo.getString("value");
                    String label = jo.getString("label");
                    if (value != null)
                    {
                        map.put(value, StringUtils.isNotEmpty(label) ? label : value);
                    }
                }
                else if (o != null)
                {
                    map.put(String.valueOf(o), String.valueOf(o));
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return map;
    }

    private String formatMatrixValue(BizSurveyQuestion q, String raw)
    {
        Map<String, String> colLabels = optionLabelMap(q);
        Map<String, String> rowLabels = matrixRowLabelMap(q.getPropsJson());
        try
        {
            JSONObject obj = JSON.parseObject(raw);
            if (obj == null || obj.isEmpty())
            {
                return raw;
            }
            List<String> parts = new ArrayList<>();
            for (String rk : obj.keySet())
            {
                String cv = obj.getString(rk);
                String rl = rowLabels.getOrDefault(rk, rk);
                String cl = colLabels.getOrDefault(cv, cv == null ? "" : cv);
                parts.add(rl + ":" + cl);
            }
            return String.join("；", parts);
        }
        catch (Exception e)
        {
            return raw;
        }
    }

    private Map<String, String> matrixRowLabelMap(String propsJson)
    {
        Map<String, String> map = new LinkedHashMap<>();
        JSONObject props = parsePropsJson(propsJson);
        if (props == null)
        {
            return map;
        }
        JSONArray rows = props.getJSONArray("rows");
        if (rows == null)
        {
            return map;
        }
        for (int i = 0; i < rows.size(); i++)
        {
            Object o = rows.get(i);
            if (o instanceof JSONObject)
            {
                JSONObject jo = (JSONObject) o;
                String value = jo.getString("value");
                if (StringUtils.isEmpty(value))
                {
                    value = "r" + (i + 1);
                }
                String label = jo.getString("label");
                map.put(value, StringUtils.isNotEmpty(label) ? label : value);
            }
        }
        return map;
    }

    private Double avgNumeric(List<String> values)
    {
        if (values == null || values.isEmpty())
        {
            return null;
        }
        double sum = 0;
        int n = 0;
        for (String s : values)
        {
            try
            {
                sum += Double.parseDouble(s.trim());
                n++;
            }
            catch (Exception ignored)
            {
            }
        }
        if (n == 0)
        {
            return null;
        }
        return Math.round(sum / n * 100.0) / 100.0;
    }

    private void validateFileAnswer(BizSurveyQuestion q, String v)
    {
        String path = v;
        try
        {
            if (v.trim().startsWith("{"))
            {
                JSONObject obj = JSON.parseObject(v);
                path = obj.getString("fileName");
                if (StringUtils.isEmpty(path))
                {
                    path = obj.getString("url");
                }
            }
        }
        catch (Exception ex)
        {
            throw new ServiceException("附件答案格式错误: " + q.getTitle());
        }
        if (StringUtils.isEmpty(path) || path.contains(".."))
        {
            throw new ServiceException(("signature".equals(q.getQType()) ? "签名" : "附件") + "无效: " + q.getTitle());
        }
        // preview mode may store data URL; reject on open submit
        if (path.startsWith("data:"))
        {
            throw new ServiceException("签名无效，请重新签署: " + q.getTitle());
        }
        if (!(path.startsWith("/profile/") || path.contains("/profile/")))
        {
            throw new ServiceException(("signature".equals(q.getQType()) ? "签名" : "附件") + "无效: " + q.getTitle());
        }
    }

    private Map<String, Object> parseProps(String propsJson)
    {
        if (StringUtils.isEmpty(propsJson))
        {
            return new HashMap<>();
        }
        try
        {
            Map<String, Object> m = JSON.parseObject(propsJson, Map.class);
            return m == null ? new HashMap<>() : m;
        }
        catch (Exception e)
        {
            return new HashMap<>();
        }
    }

    @Override
    public void exportAnswers(Long surveyId, BizSurveyAnswer filter, jakarta.servlet.http.HttpServletResponse response) throws Exception
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(surveyId);
        BizSurveyAnswer query = filter == null ? new BizSurveyAnswer() : filter;
        query.setSurveyId(surveyId);
        List<BizSurveyAnswer> answers = answerMapper.selectAnswerList(query);

        List<BizSurveyQuestion> exportQs = new ArrayList<>();
        for (BizSurveyQuestion q : questions)
        {
            if (!isDisplayOnly(q.getQType()))
            {
                exportQs.add(q);
            }
        }

        Map<Long, Map<Long, String>> answerItemMap = new HashMap<>();
        if (!answers.isEmpty())
        {
            List<Long> ids = new ArrayList<>(answers.size());
            for (BizSurveyAnswer a : answers)
            {
                ids.add(a.getAnswerId());
            }
            // 分批避免超长 IN
            final int batchSize = 500;
            for (int from = 0; from < ids.size(); from += batchSize)
            {
                List<Long> batch = ids.subList(from, Math.min(from + batchSize, ids.size()));
                List<BizSurveyAnswerItem> allItems = answerItemMapper.selectByAnswerIds(batch);
                for (BizSurveyAnswerItem it : allItems)
                {
                    answerItemMap.computeIfAbsent(it.getAnswerId(), k -> new HashMap<>())
                        .put(it.getQuestionId(), it.getAnswerValue());
                }
            }
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook())
        {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("答卷明细");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            String[] metaHeaders = { "答卷ID", "有效性", "提交时间", "IP", "渠道", "耗时(ms)", "备注" };
            for (int i = 0; i < metaHeaders.length; i++)
            {
                header.createCell(i).setCellValue(metaHeaders[i]);
            }
            for (int i = 0; i < exportQs.size(); i++)
            {
                header.createCell(metaHeaders.length + i).setCellValue(exportQs.get(i).getTitle());
            }
            int r = 1;
            for (BizSurveyAnswer a : answers)
            {
                Map<Long, String> map = answerItemMap.getOrDefault(a.getAnswerId(), Collections.emptyMap());
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(a.getAnswerId() == null ? 0 : a.getAnswerId());
                String vf = a.getValidFlag();
                row.createCell(1).setCellValue("0".equals(vf) ? "无效" : "有效");
                row.createCell(2).setCellValue(a.getSubmitTime() == null ? "" : sdf.format(a.getSubmitTime()));
                row.createCell(3).setCellValue(StringUtils.nvl(a.getSubmitIp(), ""));
                row.createCell(4).setCellValue(StringUtils.nvl(a.getChannelCode(), ""));
                if (a.getCostMs() != null)
                {
                    row.createCell(5).setCellValue(a.getCostMs());
                }
                else
                {
                    row.createCell(5).setCellValue("");
                }
                row.createCell(6).setCellValue(StringUtils.nvl(a.getRemark(), ""));
                for (int i = 0; i < exportQs.size(); i++)
                {
                    BizSurveyQuestion q = exportQs.get(i);
                    row.createCell(metaHeaders.length + i).setCellValue(formatExportValue(q, map.get(q.getQuestionId())));
                }
            }
            writeExcelResponse(wb, response, survey.getSurveyName() + "-答卷明细.xlsx");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void exportStats(Long surveyId, Long crossQ1, Long crossQ2, jakarta.servlet.http.HttpServletResponse response) throws Exception
    {
        Map<String, Object> stats = selectStats(surveyId);
        String surveyName = String.valueOf(stats.getOrDefault("surveyName", "问卷"));
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook())
        {
            org.apache.poi.ss.usermodel.Sheet overview = wb.createSheet("概览");
            String[][] overviewRows = {
                { "问卷名称", surveyName },
                { "有效答卷", String.valueOf(stats.getOrDefault("answerCount", 0)) },
                { "无效答卷", String.valueOf(stats.getOrDefault("invalidCount", 0)) },
                { "全部答卷", String.valueOf(stats.getOrDefault("totalCount", 0)) },
                { "浏览量", String.valueOf(stats.getOrDefault("viewCount", 0)) },
                { "浏览转化率(%)", String.valueOf(stats.getOrDefault("convertRate", 0)) }
            };
            for (int i = 0; i < overviewRows.length; i++)
            {
                org.apache.poi.ss.usermodel.Row row = overview.createRow(i);
                row.createCell(0).setCellValue(overviewRows[i][0]);
                row.createCell(1).setCellValue(overviewRows[i][1]);
            }

            org.apache.poi.ss.usermodel.Sheet channelSheet = wb.createSheet("渠道分布");
            org.apache.poi.ss.usermodel.Row chHeader = channelSheet.createRow(0);
            chHeader.createCell(0).setCellValue("渠道");
            chHeader.createCell(1).setCellValue("答卷数");
            List<Map<String, Object>> channels = (List<Map<String, Object>>) stats.getOrDefault("channels", Collections.emptyList());
            int cr = 1;
            for (Map<String, Object> ch : channels)
            {
                org.apache.poi.ss.usermodel.Row row = channelSheet.createRow(cr++);
                row.createCell(0).setCellValue(String.valueOf(ch.getOrDefault("channelCode", "")));
                row.createCell(1).setCellValue(toLong(ch.get("count")));
            }

            org.apache.poi.ss.usermodel.Sheet dailySheet = wb.createSheet("每日提交");
            org.apache.poi.ss.usermodel.Row dHeader = dailySheet.createRow(0);
            dHeader.createCell(0).setCellValue("日期");
            dHeader.createCell(1).setCellValue("有效答卷数");
            List<Map<String, Object>> daily = (List<Map<String, Object>>) stats.getOrDefault("dailyTrends", Collections.emptyList());
            int dr = 1;
            for (Map<String, Object> d : daily)
            {
                org.apache.poi.ss.usermodel.Row row = dailySheet.createRow(dr++);
                row.createCell(0).setCellValue(String.valueOf(d.getOrDefault("date", "")));
                row.createCell(1).setCellValue(toLong(d.get("count")));
            }

            org.apache.poi.ss.usermodel.Sheet choiceSheet = wb.createSheet("选择题统计");
            org.apache.poi.ss.usermodel.Row cHeader = choiceSheet.createRow(0);
            cHeader.createCell(0).setCellValue("题目");
            cHeader.createCell(1).setCellValue("题型");
            cHeader.createCell(2).setCellValue("选项");
            cHeader.createCell(3).setCellValue("票数");
            cHeader.createCell(4).setCellValue("占比(%)");
            List<Map<String, Object>> questions = (List<Map<String, Object>>) stats.getOrDefault("questions", Collections.emptyList());
            int qr = 1;
            for (Map<String, Object> q : questions)
            {
                if ("matrix_radio".equals(String.valueOf(q.get("qType"))))
                {
                    continue;
                }
                List<Map<String, Object>> options = (List<Map<String, Object>>) q.getOrDefault("options", Collections.emptyList());
                for (Map<String, Object> opt : options)
                {
                    org.apache.poi.ss.usermodel.Row row = choiceSheet.createRow(qr++);
                    row.createCell(0).setCellValue(String.valueOf(q.getOrDefault("title", "")));
                    row.createCell(1).setCellValue(String.valueOf(q.getOrDefault("qType", "")));
                    row.createCell(2).setCellValue(String.valueOf(opt.getOrDefault("label", opt.get("value"))));
                    row.createCell(3).setCellValue(toLong(opt.get("count")));
                    row.createCell(4).setCellValue(String.valueOf(opt.getOrDefault("percent", 0)));
                }
            }

            org.apache.poi.ss.usermodel.Sheet matrixSheet = wb.createSheet("矩阵题统计");
            org.apache.poi.ss.usermodel.Row mHeader = matrixSheet.createRow(0);
            mHeader.createCell(0).setCellValue("题目");
            mHeader.createCell(1).setCellValue("行");
            mHeader.createCell(2).setCellValue("选项");
            mHeader.createCell(3).setCellValue("票数");
            mHeader.createCell(4).setCellValue("占比(%)");
            int mr = 1;
            for (Map<String, Object> q : questions)
            {
                if (!"matrix_radio".equals(String.valueOf(q.get("qType"))))
                {
                    continue;
                }
                List<Map<String, Object>> rows = (List<Map<String, Object>>) q.getOrDefault("rows", Collections.emptyList());
                for (Map<String, Object> rowStat : rows)
                {
                    List<Map<String, Object>> options = (List<Map<String, Object>>) rowStat.getOrDefault("options", Collections.emptyList());
                    for (Map<String, Object> opt : options)
                    {
                        org.apache.poi.ss.usermodel.Row row = matrixSheet.createRow(mr++);
                        row.createCell(0).setCellValue(String.valueOf(q.getOrDefault("title", "")));
                        row.createCell(1).setCellValue(String.valueOf(rowStat.getOrDefault("rowLabel", rowStat.get("rowValue"))));
                        row.createCell(2).setCellValue(String.valueOf(opt.getOrDefault("label", opt.get("value"))));
                        row.createCell(3).setCellValue(toLong(opt.get("count")));
                        row.createCell(4).setCellValue(String.valueOf(opt.getOrDefault("percent", 0)));
                    }
                }
            }

            org.apache.poi.ss.usermodel.Sheet textSheet = wb.createSheet("填空与评分");
            org.apache.poi.ss.usermodel.Row tHeader = textSheet.createRow(0);
            tHeader.createCell(0).setCellValue("题目");
            tHeader.createCell(1).setCellValue("题型");
            tHeader.createCell(2).setCellValue("作答数");
            tHeader.createCell(3).setCellValue("均值");
            tHeader.createCell(4).setCellValue("NPS");
            tHeader.createCell(5).setCellValue("样本(最多50条，以 | 分隔)");
            List<Map<String, Object>> texts = (List<Map<String, Object>>) stats.getOrDefault("textQuestions", Collections.emptyList());
            int tr = 1;
            for (Map<String, Object> q : texts)
            {
                org.apache.poi.ss.usermodel.Row row = textSheet.createRow(tr++);
                row.createCell(0).setCellValue(String.valueOf(q.getOrDefault("title", "")));
                row.createCell(1).setCellValue(String.valueOf(q.getOrDefault("qType", "")));
                row.createCell(2).setCellValue(toLong(q.get("count")));
                row.createCell(3).setCellValue(q.get("avg") == null ? "" : String.valueOf(q.get("avg")));
                row.createCell(4).setCellValue(q.get("npsScore") == null ? "" : String.valueOf(q.get("npsScore")));
                List<String> samples = (List<String>) q.getOrDefault("samples", Collections.emptyList());
                row.createCell(5).setCellValue(String.join(" | ", samples));
            }

            org.apache.poi.ss.usermodel.Sheet distSheet = wb.createSheet("评分分布");
            org.apache.poi.ss.usermodel.Row distHeader = distSheet.createRow(0);
            distHeader.createCell(0).setCellValue("题目");
            distHeader.createCell(1).setCellValue("题型");
            distHeader.createCell(2).setCellValue("分值");
            distHeader.createCell(3).setCellValue("票数");
            distHeader.createCell(4).setCellValue("占比(%)");
            int disr = 1;
            for (Map<String, Object> q : texts)
            {
                List<Map<String, Object>> dist = (List<Map<String, Object>>) q.getOrDefault("distribution", Collections.emptyList());
                for (Map<String, Object> d : dist)
                {
                    org.apache.poi.ss.usermodel.Row row = distSheet.createRow(disr++);
                    row.createCell(0).setCellValue(String.valueOf(q.getOrDefault("title", "")));
                    row.createCell(1).setCellValue(String.valueOf(q.getOrDefault("qType", "")));
                    row.createCell(2).setCellValue(String.valueOf(d.getOrDefault("label", d.get("value"))));
                    row.createCell(3).setCellValue(toLong(d.get("count")));
                    row.createCell(4).setCellValue(String.valueOf(d.getOrDefault("percent", 0)));
                }
            }

            if (crossQ1 != null && crossQ2 != null && !crossQ1.equals(crossQ2))
            {
                try
                {
                    Map<String, Object> cross = selectCrossStats(surveyId, crossQ1, crossQ2);
                    org.apache.poi.ss.usermodel.Sheet crossSheet = wb.createSheet("交叉分析");
                    Map<String, Object> q1Info = (Map<String, Object>) cross.get("q1");
                    Map<String, Object> q2Info = (Map<String, Object>) cross.get("q2");
                    List<Map<String, Object>> opts1 = q1Info == null ? Collections.emptyList()
                        : (List<Map<String, Object>>) q1Info.getOrDefault("options", Collections.emptyList());
                    List<Map<String, Object>> opts2 = q2Info == null ? Collections.emptyList()
                        : (List<Map<String, Object>>) q2Info.getOrDefault("options", Collections.emptyList());
                    org.apache.poi.ss.usermodel.Row xh = crossSheet.createRow(0);
                    xh.createCell(0).setCellValue(q1Info == null ? "行" : String.valueOf(q1Info.getOrDefault("title", "行")));
                    for (int i = 0; i < opts2.size(); i++)
                    {
                        xh.createCell(i + 1).setCellValue(String.valueOf(opts2.get(i).getOrDefault("label", opts2.get(i).get("value"))));
                    }
                    Map<String, Long> cellMap = new HashMap<>();
                    List<Map<String, Object>> cells = (List<Map<String, Object>>) cross.getOrDefault("cells", Collections.emptyList());
                    for (Map<String, Object> cell : cells)
                    {
                        cellMap.put(String.valueOf(cell.get("rowValue")) + "\0" + String.valueOf(cell.get("colValue")),
                            toLong(cell.get("count")));
                    }
                    int xr = 1;
                    for (Map<String, Object> rowOpt : opts1)
                    {
                        org.apache.poi.ss.usermodel.Row row = crossSheet.createRow(xr++);
                        String rv = String.valueOf(rowOpt.get("value"));
                        row.createCell(0).setCellValue(String.valueOf(rowOpt.getOrDefault("label", rv)));
                        for (int i = 0; i < opts2.size(); i++)
                        {
                            String cv = String.valueOf(opts2.get(i).get("value"));
                            row.createCell(i + 1).setCellValue(cellMap.getOrDefault(rv + "\0" + cv, 0L));
                        }
                    }
                    org.apache.poi.ss.usermodel.Row meta = crossSheet.createRow(xr + 1);
                    meta.createCell(0).setCellValue("有效配对答卷");
                    meta.createCell(1).setCellValue(toLong(cross.get("pairedCount")));
                }
                catch (ServiceException ignored)
                {
                    // 交叉题型不符时跳过该 sheet
                }
            }

            writeExcelResponse(wb, response, surveyName + "-统计汇总.xlsx");
        }
    }

    private void writeExcelResponse(org.apache.poi.xssf.usermodel.XSSFWorkbook wb,
        jakarta.servlet.http.HttpServletResponse response, String fileName) throws Exception
    {
        String encoded = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        wb.write(response.getOutputStream());
    }

    private long toLong(Object v)
    {
        if (v == null)
        {
            return 0L;
        }
        if (v instanceof Number)
        {
            return ((Number) v).longValue();
        }
        try
        {
            return Long.parseLong(String.valueOf(v));
        }
        catch (Exception e)
        {
            return 0L;
        }
    }


    @Override
    public void testWebhook(Long surveyId)
    {
        BizSurvey survey = requireSurvey(surveyId);
        checkOwner(survey);
        if (!SurveyWebhookHelper.isValidUrl(survey.getWebhookUrl()))
        {
            throw new ServiceException("请先配置有效的 Webhook 地址");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "survey.webhook.test");
        payload.put("surveyId", survey.getSurveyId());
        payload.put("surveyName", survey.getSurveyName());
        payload.put("publicCode", survey.getPublicCode());
        payload.put("message", "通查云 webhook test");
        SurveyWebhookHelper.dispatchAsync(survey.getWebhookUrl(), JSON.toJSONString(payload), survey.getWebhookSecret());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizSurvey copySurvey(Long surveyId)
    {
        BizSurvey src = requireSurvey(surveyId);
        checkOwner(src);
        BizSurvey neo = new BizSurvey();
        String name = src.getSurveyName() == null ? "问卷" : src.getSurveyName();
        if (!name.endsWith("（副本）") && !name.endsWith("(副本)"))
        {
            name = name + "（副本）";
        }
        if (name.length() > 100)
        {
            name = name.substring(0, 100);
        }
        neo.setSurveyName(name);
        neo.setSurveyDesc(src.getSurveyDesc());
        neo.setStartTime(src.getStartTime());
        neo.setEndTime(src.getEndTime());
        neo.setMaxAnswers(src.getMaxAnswers());
        neo.setAllowMulti(src.getAllowMulti());
        neo.setAccessPwd(src.getAccessPwd());
        neo.setThemeJson(src.getThemeJson());
        neo.setWebhookUrl(src.getWebhookUrl());
        neo.setDailyLimit(src.getDailyLimit());
        neo.setNeedCaptcha(src.getNeedCaptcha());
        neo.setWebhookSecret(src.getWebhookSecret());
        neo.setRemark(src.getRemark());
        insertBizSurvey(neo);

        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(surveyId);
        if (questions != null && !questions.isEmpty())
        {
            for (BizSurveyQuestion q : questions)
            {
                q.setQuestionId(null);
                q.setSurveyId(neo.getSurveyId());
            }
            questionMapper.batchInsert(questions);
        }
        return neo;
    }

    @Override
    public List<Map<String, Object>> listTemplates()
    {
        return BizSurveyTemplates.list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizSurvey createFromTemplate(String templateKey)
    {
        BizSurveyTemplates.TemplateDef def = BizSurveyTemplates.require(templateKey);
        BizSurvey neo = new BizSurvey();
        neo.setSurveyName(def.surveyName);
        neo.setSurveyDesc(def.surveyDesc);
        neo.setAllowMulti(def.allowMulti);
        neo.setMaxAnswers(0);
        neo.setDailyLimit(0);
        neo.setNeedCaptcha(def.needCaptcha);
        if (def.theme != null && !def.theme.isEmpty())
        {
            neo.setThemeJson(JSON.toJSONString(def.theme));
        }
        else
        {
            neo.setThemeJson("{\"color\":\"#1677ff\"}");
        }
        insertBizSurvey(neo);
        List<BizSurveyQuestion> questions = new ArrayList<>();
        for (BizSurveyQuestion q : def.questions)
        {
            BizSurveyQuestion nq = new BizSurveyQuestion();
            nq.setSurveyId(neo.getSurveyId());
            nq.setQType(q.getQType());
            nq.setTitle(q.getTitle());
            nq.setRequired(q.getRequired());
            nq.setOptionsJson(q.getOptionsJson());
            nq.setPropsJson(q.getPropsJson());
            nq.setSort(q.getSort());
            questions.add(nq);
        }
        if (!questions.isEmpty())
        {
            questionMapper.batchInsert(questions);
        }
        return neo;
    }

    private void checkOwner(BizSurvey survey)
    {
        projectScopeHelper.assertAccess(survey.getCreateUserId(), survey.getDeptId(),
            "biz:survey:list,biz:survey:query,biz:survey:edit", "无权操作该问卷");
    }

    @Override
    public int transferOwnership(Long surveyId, Long targetUserId)
    {
        BizUserProjectServiceImpl.assertAdminManage();
        BizSurvey db = requireSurvey(surveyId);
        checkOwner(db);
        Map<String, Object> user = userProjectService.requireActiveUser(targetUserId);
        BizSurvey upd = new BizSurvey();
        upd.setSurveyId(surveyId);
        upd.setCreateUserId(targetUserId);
        Object deptId = user.get("deptId");
        upd.setDeptId(deptId == null ? null : Long.valueOf(String.valueOf(deptId)));
        upd.setCreateBy(String.valueOf(user.get("userName")));
        upd.setUpdateBy(SecurityUtils.getUsername());
        return surveyMapper.transferOwner(upd);
    }

    private BizSurvey requireSurvey(Long surveyId)
    {
        BizSurvey survey = surveyMapper.selectBizSurveyById(surveyId);
        if (survey == null)
        {
            throw new ServiceException("问卷不存在");
        }
        return survey;
    }

    private BizSurvey requireOpenable(String code)
    {
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("无效链接");
        }
        BizSurvey survey = surveyMapper.selectBizSurveyByCode(code);
        if (survey == null || !"1".equals(survey.getStatus()))
        {
            throw new ServiceException("问卷不存在或未发布");
        }
        Date now = new Date();
        if (survey.getStartTime() != null && now.before(survey.getStartTime()))
        {
            throw new ServiceException("问卷尚未开始");
        }
        if (survey.getEndTime() != null && now.after(survey.getEndTime()))
        {
            throw new ServiceException("问卷已结束");
        }
        return survey;
    }

    private BizSurveyDetailVo buildDetail(BizSurvey survey)
    {
        survey.setAccessPwd(BizAccessPwdHelper.maskForApi(survey.getAccessPwd()));
        BizSurveyDetailVo vo = new BizSurveyDetailVo();
        vo.setSurvey(survey);
        vo.setQuestions(questionMapper.selectBySurveyId(survey.getSurveyId()));
        return vo;
    }

    private String genUniqueCode()
    {
        for (int i = 0; i < 20; i++)
        {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++)
            {
                sb.append(CODE_CHARS.charAt(ThreadLocalRandom.current().nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (surveyMapper.selectBizSurveyByCode(code) == null)
            {
                return code;
            }
        }
        throw new ServiceException("生成短码失败，请重试");
    }

    @Override
    public Map<String, Object> openLoadDraft(String code, String accessPwd, String clientToken)
    {
        BizSurvey survey = requireOpenable(code);
        assertAccessPwd(survey.getAccessPwd(), accessPwd);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("form", null);
        data.put("savedAt", null);
        if (StringUtils.isEmpty(clientToken))
        {
            return data;
        }
        String token = StringUtils.substring(clientToken.trim(), 0, 64);
        BizSurveyDraft draft = draftMapper.selectBySurveyAndToken(survey.getSurveyId(), token);
        if (draft == null || StringUtils.isEmpty(draft.getDraftJson()))
        {
            return data;
        }
        try
        {
            Map<String, Object> parsed = JSON.parseObject(draft.getDraftJson(), Map.class);
            if (parsed != null)
            {
                data.put("form", parsed.get("form"));
                data.put("savedAt", parsed.get("savedAt"));
            }
        }
        catch (Exception ignored)
        {
        }
        return data;
    }

    @Override
    public void openSaveDraft(String code, Map<String, Object> body)
    {
        BizSurvey survey = requireOpenable(code);
        String accessPwd = body == null || body.get("accessPwd") == null ? null : String.valueOf(body.get("accessPwd"));
        assertAccessPwd(survey.getAccessPwd(), accessPwd);
        String clientToken = body == null || body.get("clientToken") == null ? "" : String.valueOf(body.get("clientToken")).trim();
        if (StringUtils.isEmpty(clientToken))
        {
            throw new ServiceException("缺少客户端标记");
        }
        clientToken = StringUtils.substring(clientToken, 0, 64);
        Object form = body.get("form");
        if (form == null)
        {
            form = new HashMap<>();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("savedAt", System.currentTimeMillis());
        payload.put("form", form);
        String json = JSON.toJSONString(payload);
        if (json.length() > 200000)
        {
            throw new ServiceException("草稿过大");
        }
        BizSurveyDraft draft = new BizSurveyDraft();
        draft.setSurveyId(survey.getSurveyId());
        draft.setClientToken(clientToken);
        draft.setDraftJson(json);
        draftMapper.upsertDraft(draft);
    }

    private void assertAccessPwd(String expected, String actual)
    {
        if (!BizAccessPwdHelper.matches(expected, actual))
        {
            throw new ServiceException("访问密码错误");
        }
    }

    private List<Map<String, Object>> buildNumericDistribution(List<String> values, long totalAnswers)
    {
        Map<String, Long> counter = new java.util.TreeMap<>((a, b) -> {
            try
            {
                return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
            }
            catch (Exception e)
            {
                return a.compareTo(b);
            }
        });
        for (String v : values)
        {
            if (StringUtils.isEmpty(v))
            {
                continue;
            }
            String key = v.trim();
            counter.put(key, counter.getOrDefault(key, 0L) + 1);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : counter.entrySet())
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("value", e.getKey());
            row.put("label", e.getKey());
            row.put("count", e.getValue());
            double pct = totalAnswers == 0 ? 0 : (e.getValue() * 100.0 / totalAnswers);
            row.put("percent", Math.round(pct * 10) / 10.0);
            list.add(row);
        }
        return list;
    }

    private List<Map<String, Object>> buildNumericTrends(Long questionId, List<BizSurveyAnswerItem> items, Map<Long, Date> timeMap)
    {
        java.text.SimpleDateFormat dayFmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Map<String, List<Double>> buckets = new java.util.TreeMap<>();
        for (BizSurveyAnswerItem item : items)
        {
            if (!questionId.equals(item.getQuestionId()) || StringUtils.isEmpty(item.getAnswerValue()))
            {
                continue;
            }
            Date t = timeMap.get(item.getAnswerId());
            if (t == null)
            {
                continue;
            }
            try
            {
                double n = Double.parseDouble(item.getAnswerValue().trim());
                String day = dayFmt.format(t);
                buckets.computeIfAbsent(day, k -> new ArrayList<>()).add(n);
            }
            catch (Exception ignored)
            {
            }
        }
        List<Map<String, Object>> trends = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : buckets.entrySet())
        {
            List<Double> vals = e.getValue();
            double sum = 0;
            for (Double d : vals)
            {
                sum += d;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", e.getKey());
            row.put("count", vals.size());
            row.put("avg", Math.round(sum / vals.size() * 100.0) / 100.0);
            trends.add(row);
        }
        return trends;
    }

    private List<Map<String, Object>> buildNumericTrends(Long surveyId, Long questionId, List<BizSurveyAnswerItem> items)
    {
        BizSurveyAnswer query = new BizSurveyAnswer();
        query.setSurveyId(surveyId);
        query.setValidFlag("1");
        List<BizSurveyAnswer> answers = answerMapper.selectAnswerList(query);
        Map<Long, Date> timeMap = new HashMap<>();
        for (BizSurveyAnswer a : answers)
        {
            if (a.getSubmitTime() != null)
            {
                timeMap.put(a.getAnswerId(), a.getSubmitTime());
            }
        }
        return buildNumericTrends(questionId, items, timeMap);
    }

    private void collectCascadeLeafOptions(JSONArray arr, List<Map<String, Object>> options, Map<String, Long> counter)
    {
        if (arr == null)
        {
            return;
        }
        for (int i = 0; i < arr.size(); i++)
        {
            Object o = arr.get(i);
            if (!(o instanceof JSONObject))
            {
                continue;
            }
            JSONObject jo = (JSONObject) o;
            JSONArray children = jo.getJSONArray("children");
            if (children != null && !children.isEmpty())
            {
                collectCascadeLeafOptions(children, options, counter);
            }
            else
            {
                String value = jo.getString("value");
                String label = jo.getString("label");
                if (StringUtils.isEmpty(value))
                {
                    continue;
                }
                counter.put(value, 0L);
                Map<String, Object> opt = new LinkedHashMap<>();
                opt.put("value", value);
                opt.put("label", StringUtils.isNotEmpty(label) ? label : value);
                options.add(opt);
            }
        }
    }

    private String cascadeLeafValue(String raw)
    {
        List<String> path = parseCascadePath(raw);
        if (path.isEmpty())
        {
            return "";
        }
        return path.get(path.size() - 1);
    }

    private List<String> parseCascadePath(String raw)
    {
        List<String> path = new ArrayList<>();
        if (StringUtils.isEmpty(raw))
        {
            return path;
        }
        try
        {
            if (raw.trim().startsWith("["))
            {
                JSONArray arr = JSON.parseArray(raw);
                if (arr != null)
                {
                    for (int i = 0; i < arr.size(); i++)
                    {
                        path.add(String.valueOf(arr.get(i)));
                    }
                }
                return path;
            }
        }
        catch (Exception ignored)
        {
        }
        path.add(raw);
        return path;
    }

    private void validateCascadeAnswer(BizSurveyQuestion q, String v)
    {
        if (!"cascade_select".equals(q.getQType()) || StringUtils.isEmpty(v))
        {
            return;
        }
        List<String> path = parseCascadePath(v);
        if (path.isEmpty())
        {
            throw new ServiceException("级联选项无效: " + q.getTitle());
        }
        if (!isValidCascadePath(q.getOptionsJson(), path))
        {
            throw new ServiceException("级联选项无效: " + q.getTitle());
        }
    }

    private boolean isValidCascadePath(String optionsJson, List<String> path)
    {
        if (StringUtils.isEmpty(optionsJson) || path == null || path.isEmpty())
        {
            return false;
        }
        try
        {
            JSONArray nodes = JSON.parseArray(optionsJson);
            for (int depth = 0; depth < path.size(); depth++)
            {
                if (nodes == null)
                {
                    return false;
                }
                String want = path.get(depth);
                JSONObject hit = null;
                for (int i = 0; i < nodes.size(); i++)
                {
                    Object o = nodes.get(i);
                    if (!(o instanceof JSONObject))
                    {
                        continue;
                    }
                    JSONObject jo = (JSONObject) o;
                    if (want.equals(String.valueOf(jo.get("value"))))
                    {
                        hit = jo;
                        break;
                    }
                }
                if (hit == null)
                {
                    return false;
                }
                nodes = hit.getJSONArray("children");
                if (depth == path.size() - 1)
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            return false;
        }
        return false;
    }

    private String formatCascadeValue(BizSurveyQuestion q, String raw)
    {
        List<String> path = parseCascadePath(raw);
        if (path.isEmpty())
        {
            return raw;
        }
        List<String> labels = new ArrayList<>();
        try
        {
            JSONArray nodes = JSON.parseArray(q.getOptionsJson());
            for (String want : path)
            {
                if (nodes == null)
                {
                    labels.add(want);
                    continue;
                }
                JSONObject hit = null;
                for (int i = 0; i < nodes.size(); i++)
                {
                    Object o = nodes.get(i);
                    if (!(o instanceof JSONObject))
                    {
                        continue;
                    }
                    JSONObject jo = (JSONObject) o;
                    if (want.equals(String.valueOf(jo.get("value"))))
                    {
                        hit = jo;
                        break;
                    }
                }
                if (hit == null)
                {
                    labels.add(want);
                    nodes = null;
                }
                else
                {
                    String label = hit.getString("label");
                    labels.add(StringUtils.isNotEmpty(label) ? label : want);
                    nodes = hit.getJSONArray("children");
                }
            }
        }
        catch (Exception e)
        {
            return String.join("/", path);
        }
        return String.join("/", labels);
    }
}
