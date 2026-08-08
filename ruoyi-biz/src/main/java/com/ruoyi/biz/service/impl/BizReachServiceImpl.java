package com.ruoyi.biz.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.domain.BizSurveyQuestion;
import com.ruoyi.biz.mapper.BizQueryFieldMapper;
import com.ruoyi.biz.mapper.BizQueryMapper;
import com.ruoyi.biz.mapper.BizSurveyMapper;
import com.ruoyi.biz.mapper.BizSurveyQuestionMapper;
import com.ruoyi.biz.service.IBizNotifyService;
import com.ruoyi.biz.service.IBizReachService;
import com.ruoyi.biz.utils.BizProjectScopeHelper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysBasicService;
import com.ruoyi.system.service.ISysUserService;

@Service
public class BizReachServiceImpl implements IBizReachService
{
    private static final Logger log = LoggerFactory.getLogger(BizReachServiceImpl.class);
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BizSurveyMapper surveyMapper;
    @Autowired
    private BizSurveyQuestionMapper questionMapper;
    @Autowired
    private BizQueryMapper queryMapper;
    @Autowired
    private BizQueryFieldMapper fieldMapper;
    @Autowired
    private IBizNotifyService notifyService;
    @Autowired
    private ISysBasicService basicService;
    @Autowired
    private ISysUserService userService;
    @Autowired
    private BizProjectScopeHelper projectScopeHelper;

    @Override
    public Map<String, Integer> runDueTasks()
    {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("surveyPublished", processSurveyPublish());
        result.put("surveyExpired", processSurveyExpire());
        result.put("surveyReminded", processSurveyRemind());
        result.put("queryPublished", processQueryPublish());
        result.put("queryExpired", processQueryExpire());
        result.put("queryReminded", processQueryRemind());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String scheduleSurveyPublish(Long surveyId, Date publishAt)
    {
        if (publishAt == null)
        {
            throw new ServiceException("请选择预约发布时间");
        }
        if (!publishAt.after(new Date()))
        {
            throw new ServiceException("预约时间须晚于当前时间");
        }
        BizSurvey survey = requireSurvey(surveyId);
        assertSurveyAccess(survey);
        assertSurveyReadyToPublish(survey);
        String code = ensureSurveyCode(survey);
        BizSurvey upd = new BizSurvey();
        upd.setSurveyId(surveyId);
        upd.setPublicCode(code);
        upd.setPublishAt(publishAt);
        upd.setStatus("0");
        upd.setUpdateBy(currentUser());
        surveyMapper.updateBizSurvey(upd);
        return code;
    }

    @Override
    public int cancelSurveySchedule(Long surveyId)
    {
        BizSurvey survey = requireSurvey(surveyId);
        assertSurveyAccess(survey);
        BizSurvey upd = new BizSurvey();
        upd.setSurveyId(surveyId);
        upd.getParams().put("clearPublishAt", true);
        upd.setUpdateBy(currentUser());
        return surveyMapper.updateBizSurvey(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String scheduleQueryPublish(Long queryId, Date publishAt)
    {
        if (publishAt == null)
        {
            throw new ServiceException("请选择预约发布时间");
        }
        if (!publishAt.after(new Date()))
        {
            throw new ServiceException("预约时间须晚于当前时间");
        }
        BizQuery query = requireQuery(queryId);
        assertQueryAccess(query);
        assertQueryReadyToPublish(query);
        String code = ensureQueryCode(query);
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.setPublicCode(code);
        upd.setPublishAt(publishAt);
        upd.setStatus("0");
        upd.setUpdateBy(currentUser());
        queryMapper.updateBizQuery(upd);
        return code;
    }

    @Override
    public int cancelQuerySchedule(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        assertQueryAccess(query);
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.getParams().put("clearPublishAt", true);
        upd.setUpdateBy(currentUser());
        return queryMapper.updateBizQuery(upd);
    }

    @Override
    public boolean sendPublishNotify(String type, Long projectId, String toEmails, String link)
    {
        if (StringUtils.isEmpty(toEmails))
        {
            throw new ServiceException("请填写收件邮箱");
        }
        String title;
        String name;
        if ("query".equals(type))
        {
            BizQuery q = requireQuery(projectId);
            assertQueryAccess(q);
            name = q.getQueryName();
            title = "查询已发布：" + name;
        }
        else
        {
            BizSurvey s = requireSurvey(projectId);
            assertSurveyAccess(s);
            name = s.getSurveyName();
            title = "问卷已发布：" + name;
        }
        String site = basicService.getSiteTitle();
        String html = "<p>您好，</p><p><b>" + escape(name) + "</b> 已发布。</p>"
            + (StringUtils.isNotEmpty(link) ? "<p>公开链接：<a href=\"" + escape(link) + "\">" + escape(link) + "</a></p>" : "")
            + "<p>— " + escape(site) + "</p>";
        boolean any = false;
        for (String to : splitEmails(toEmails))
        {
            if (basicService.sendBizMail(to, "【" + site + "】" + title, html))
            {
                any = true;
            }
        }
        if (!any)
        {
            throw new ServiceException("邮件未发送成功，请检查基础设置中的 SMTP 配置");
        }
        return true;
    }

    @Override
    public void sendAnswerMailIfNeeded(BizSurvey survey, Long answerId, String channelCode)
    {
        if (survey == null || !"1".equals(survey.getMailNotify()))
        {
            return;
        }
        String site = basicService.getSiteTitle();
        String subject = "【" + site + "】收到新答卷：" + StringUtils.nvl(survey.getSurveyName(), "问卷");
        StringBuilder html = new StringBuilder();
        html.append("<p>问卷 <b>").append(escape(survey.getSurveyName())).append("</b> 收到新答卷。</p>");
        html.append("<p>答卷 ID：").append(answerId).append("</p>");
        if (StringUtils.isNotEmpty(channelCode))
        {
            html.append("<p>渠道：").append(escape(channelCode)).append("</p>");
        }
        html.append("<p>请登录后台查看答卷明细。</p><p>— ").append(escape(site)).append("</p>");
        for (String to : resolveSurveyMailTargets(survey))
        {
            basicService.sendBizMail(to, subject, html.toString());
        }
    }

    private int processSurveyPublish()
    {
        int n = 0;
        List<BizSurvey> list = surveyMapper.selectDuePublish();
        if (list == null)
        {
            return 0;
        }
        for (BizSurvey s : list)
        {
            try
            {
                if (!isSurveyReadyQuiet(s))
                {
                    log.warn("skip auto-publish survey {}: not ready", s.getSurveyId());
                    continue;
                }
                String code = ensureSurveyCode(s);
                BizSurvey upd = new BizSurvey();
                upd.setSurveyId(s.getSurveyId());
                upd.setPublicCode(code);
                upd.setStatus("1");
                upd.getParams().put("clearPublishAt", true);
                upd.setUpdateBy("system");
                surveyMapper.updateBizSurvey(upd);
                notifyService.createSimpleNotify(s.getCreateUserId(), s.getSurveyId(),
                    "问卷已自动发布：" + s.getSurveyName(),
                    "预约时间已到，短码 " + code + " 现已可访问。");
                n++;
            }
            catch (Exception e)
            {
                log.warn("auto publish survey {} failed: {}", s.getSurveyId(), e.getMessage());
            }
        }
        return n;
    }

    private int processSurveyExpire()
    {
        int n = 0;
        List<BizSurvey> list = surveyMapper.selectDueExpire();
        if (list == null)
        {
            return 0;
        }
        for (BizSurvey s : list)
        {
            try
            {
                BizSurvey upd = new BizSurvey();
                upd.setSurveyId(s.getSurveyId());
                upd.setStatus("3");
                upd.setUpdateBy("system");
                surveyMapper.updateBizSurvey(upd);
                notifyService.createSimpleNotify(s.getCreateUserId(), s.getSurveyId(),
                    "问卷已截止：" + s.getSurveyName(),
                    "截止时间已到，公开页已停止接收答卷。");
                n++;
            }
            catch (Exception e)
            {
                log.warn("auto expire survey {} failed: {}", s.getSurveyId(), e.getMessage());
            }
        }
        return n;
    }

    private int processSurveyRemind()
    {
        int n = 0;
        List<BizSurvey> list = surveyMapper.selectDueRemind();
        if (list == null)
        {
            return 0;
        }
        for (BizSurvey s : list)
        {
            try
            {
                String end = s.getEndTime() == null ? "" : DF.format(s.getEndTime());
                String content = "问卷将于 " + end + " 截止，请及时关注回收进度。";
                notifyService.createSimpleNotify(s.getCreateUserId(), s.getSurveyId(),
                    "即将截止：" + s.getSurveyName(), content);
                if ("1".equals(s.getRemindMail()))
                {
                    String site = basicService.getSiteTitle();
                    String html = "<p>" + escape(content) + "</p><p>— " + escape(site) + "</p>";
                    for (String to : resolveSurveyMailTargets(s))
                    {
                        basicService.sendBizMail(to, "【" + site + "】即将截止：" + s.getSurveyName(), html);
                    }
                }
                BizSurvey upd = new BizSurvey();
                upd.setSurveyId(s.getSurveyId());
                upd.setRemindSent("1");
                upd.setUpdateBy("system");
                surveyMapper.updateBizSurvey(upd);
                n++;
            }
            catch (Exception e)
            {
                log.warn("remind survey {} failed: {}", s.getSurveyId(), e.getMessage());
            }
        }
        return n;
    }

    private int processQueryPublish()
    {
        int n = 0;
        List<BizQuery> list = queryMapper.selectDuePublish();
        if (list == null)
        {
            return 0;
        }
        for (BizQuery q : list)
        {
            try
            {
                if (!isQueryReadyQuiet(q))
                {
                    log.warn("skip auto-publish query {}: not ready", q.getQueryId());
                    continue;
                }
                String code = ensureQueryCode(q);
                BizQuery upd = new BizQuery();
                upd.setQueryId(q.getQueryId());
                upd.setPublicCode(code);
                upd.setStatus("1");
                upd.getParams().put("clearPublishAt", true);
                upd.setUpdateBy("system");
                queryMapper.updateBizQuery(upd);
                notifyService.createSimpleNotify(q.getCreateUserId(), null,
                    "查询已自动发布：" + q.getQueryName(),
                    "预约时间已到，短码 " + code + " 现已可访问。");
                n++;
            }
            catch (Exception e)
            {
                log.warn("auto publish query {} failed: {}", q.getQueryId(), e.getMessage());
            }
        }
        return n;
    }

    private int processQueryExpire()
    {
        int n = 0;
        List<BizQuery> list = queryMapper.selectDueExpire();
        if (list == null)
        {
            return 0;
        }
        for (BizQuery q : list)
        {
            try
            {
                BizQuery upd = new BizQuery();
                upd.setQueryId(q.getQueryId());
                upd.setStatus("3");
                upd.setUpdateBy("system");
                queryMapper.updateBizQuery(upd);
                notifyService.createSimpleNotify(q.getCreateUserId(), null,
                    "查询已截止：" + q.getQueryName(),
                    "截止时间已到，公开页已停止查询。");
                n++;
            }
            catch (Exception e)
            {
                log.warn("auto expire query {} failed: {}", q.getQueryId(), e.getMessage());
            }
        }
        return n;
    }

    private int processQueryRemind()
    {
        int n = 0;
        List<BizQuery> list = queryMapper.selectDueRemind();
        if (list == null)
        {
            return 0;
        }
        for (BizQuery q : list)
        {
            try
            {
                String end = q.getEndTime() == null ? "" : DF.format(q.getEndTime());
                String content = "查询将于 " + end + " 截止。";
                notifyService.createSimpleNotify(q.getCreateUserId(), null,
                    "即将截止：" + q.getQueryName(), content);
                if ("1".equals(q.getRemindMail()))
                {
                    String site = basicService.getSiteTitle();
                    String html = "<p>" + escape(content) + "</p><p>— " + escape(site) + "</p>";
                    String email = ownerEmail(q.getCreateUserId());
                    if (StringUtils.isNotEmpty(email))
                    {
                        basicService.sendBizMail(email, "【" + site + "】即将截止：" + q.getQueryName(), html);
                    }
                }
                BizQuery upd = new BizQuery();
                upd.setQueryId(q.getQueryId());
                upd.setRemindSent("1");
                upd.setUpdateBy("system");
                queryMapper.updateBizQuery(upd);
                n++;
            }
            catch (Exception e)
            {
                log.warn("remind query {} failed: {}", q.getQueryId(), e.getMessage());
            }
        }
        return n;
    }

    private void assertSurveyReadyToPublish(BizSurvey survey)
    {
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(survey.getSurveyId());
        if (questions == null || questions.isEmpty()
            || questions.stream().noneMatch(q -> !isDisplayOnly(q.getQType())))
        {
            throw new ServiceException("请先设计可作答题目再预约发布");
        }
    }

    private boolean isSurveyReadyQuiet(BizSurvey survey)
    {
        try
        {
            assertSurveyReadyToPublish(survey);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void assertQueryReadyToPublish(BizQuery query)
    {
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(query.getQueryId());
        if (fields == null || fields.isEmpty() || query.getRowCount() == null || query.getRowCount() <= 0)
        {
            throw new ServiceException("请先上传数据再预约发布");
        }
        boolean hasQuery = fields.stream().anyMatch(f -> "1".equals(f.getIsQuery()));
        boolean hasList = fields.stream().anyMatch(f -> "1".equals(f.getIsList()));
        if (!hasQuery)
        {
            throw new ServiceException("请至少配置一个查询条件字段后再预约发布");
        }
        if (!hasList)
        {
            throw new ServiceException("请至少配置一个结果展示字段后再预约发布");
        }
    }

    private boolean isQueryReadyQuiet(BizQuery query)
    {
        try
        {
            assertQueryReadyToPublish(query);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private String ensureSurveyCode(BizSurvey survey)
    {
        if (StringUtils.isNotEmpty(survey.getPublicCode()))
        {
            return survey.getPublicCode();
        }
        for (int i = 0; i < 20; i++)
        {
            String code = randomCode();
            if (surveyMapper.selectBizSurveyByCode(code) == null)
            {
                return code;
            }
        }
        throw new ServiceException("生成公开码失败");
    }

    private String ensureQueryCode(BizQuery query)
    {
        if (StringUtils.isNotEmpty(query.getPublicCode()))
        {
            return query.getPublicCode();
        }
        for (int i = 0; i < 20; i++)
        {
            String code = randomCode();
            if (queryMapper.selectBizQueryByCode(code) == null)
            {
                return code;
            }
        }
        throw new ServiceException("生成公开码失败");
    }

    private String randomCode()
    {
        String chars = "abcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 8; j++)
        {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private static boolean isDisplayOnly(String qType)
    {
        return "section".equals(qType) || "page_break".equals(qType);
    }

    private List<String> resolveSurveyMailTargets(BizSurvey survey)
    {
        ArrayList<String> list = new ArrayList<>();
        if (StringUtils.isNotEmpty(survey.getMailNotifyTo()))
        {
            list.addAll(splitEmails(survey.getMailNotifyTo()));
        }
        if (list.isEmpty())
        {
            String email = ownerEmail(survey.getCreateUserId());
            if (StringUtils.isNotEmpty(email))
            {
                list.add(email);
            }
        }
        return list;
    }

    private String ownerEmail(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        try
        {
            SysUser user = userService.selectUserById(userId);
            if (user != null && StringUtils.isNotEmpty(user.getEmail()))
            {
                return user.getEmail().trim();
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    private static List<String> splitEmails(String raw)
    {
        ArrayList<String> list = new ArrayList<>();
        if (StringUtils.isEmpty(raw))
        {
            return list;
        }
        for (String p : raw.split("[,;\\s]+"))
        {
            String e = p.trim();
            if (e.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") && !list.contains(e))
            {
                list.add(e);
            }
        }
        return list;
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

    private BizQuery requireQuery(Long queryId)
    {
        BizQuery query = queryMapper.selectBizQueryById(queryId);
        if (query == null)
        {
            throw new ServiceException("查询项目不存在");
        }
        return query;
    }

    private void assertSurveyAccess(BizSurvey survey)
    {
        projectScopeHelper.assertAccess(survey.getCreateUserId(), survey.getDeptId(),
            "biz:survey:list,biz:survey:query,biz:survey:edit,biz:survey:publish", "无权操作该问卷");
    }

    private void assertQueryAccess(BizQuery query)
    {
        projectScopeHelper.assertAccess(query.getCreateUserId(), query.getDeptId(),
            "biz:query:list,biz:query:query,biz:query:edit,biz:query:publish", "无权操作该查询");
    }

    private String currentUser()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }

    private static String escape(String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
