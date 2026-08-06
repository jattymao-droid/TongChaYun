package com.ruoyi.biz.service.impl;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.domain.BizSurveyNotify;
import com.ruoyi.biz.mapper.BizAccessLogMapper;
import com.ruoyi.biz.service.IBizDashboardService;
import com.ruoyi.biz.service.IBizNotifyService;
import com.ruoyi.biz.service.IBizQueryService;
import com.ruoyi.biz.service.IBizSurveyService;
import com.ruoyi.common.utils.SecurityUtils;

@Service
public class BizDashboardServiceImpl implements IBizDashboardService
{
    @Autowired
    private BizAccessLogMapper accessLogMapper;
    @Autowired
    private IBizQueryService queryService;
    @Autowired
    private IBizSurveyService surveyService;
    @Autowired
    private IBizNotifyService notifyService;

    @Override
    public Map<String, Object> overview()
    {
        // Scoped lists so dashboard matches @DataScope visibility
        List<BizQuery> queries = queryService.selectBizQueryList(new BizQuery());
        List<BizSurvey> surveys = surveyService.selectBizSurveyList(new BizSurvey());

        long queryPublished = queries.stream().filter(x -> "1".equals(x.getStatus())).count();
        long surveyPublished = surveys.stream().filter(x -> "1".equals(x.getStatus())).count();
        long queryViews = queries.stream().mapToLong(x -> nz(x.getViewCount())).sum();
        long querySearches = queries.stream().mapToLong(x -> nz(x.getSearchCount())).sum();
        long surveyViews = surveys.stream().mapToLong(x -> nz(x.getViewCount())).sum();
        long answerCount = surveys.stream().mapToLong(x -> nz(x.getAnswerCount())).sum();
        long rowCount = queries.stream().mapToLong(x -> x.getRowCount() == null ? 0L : x.getRowCount()).sum();

        Comparator<Date> newestFirst = Comparator.nullsLast(Comparator.reverseOrder());
        List<Map<String, Object>> recentQueries = queries.stream()
            .sorted(Comparator.comparing(
                (BizQuery item) -> item.getUpdateTime() != null ? item.getUpdateTime() : item.getCreateTime(),
                newestFirst))
            .map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("queryId", item.getQueryId());
            m.put("queryName", item.getQueryName());
            m.put("status", item.getStatus());
            m.put("rowCount", item.getRowCount());
            m.put("viewCount", item.getViewCount());
            m.put("searchCount", item.getSearchCount());
            m.put("publicCode", item.getPublicCode());
            m.put("createTime", item.getCreateTime());
            m.put("updateTime", item.getUpdateTime());
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> recentSurveys = surveys.stream()
            .sorted(Comparator.comparing(
                (BizSurvey item) -> item.getUpdateTime() != null ? item.getUpdateTime() : item.getCreateTime(),
                newestFirst))
            .map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("surveyId", item.getSurveyId());
            m.put("surveyName", item.getSurveyName());
            m.put("status", item.getStatus());
            m.put("answerCount", item.getAnswerCount());
            m.put("viewCount", item.getViewCount());
            m.put("publicCode", item.getPublicCode());
            m.put("createTime", item.getCreateTime());
            m.put("updateTime", item.getUpdateTime());
            return m;
        }).collect(Collectors.toList());

        BizSurveyNotify nq = new BizSurveyNotify();
        nq.setUserId(SecurityUtils.getUserId());
        List<BizSurveyNotify> recentNotifies = notifyService.selectList(nq).stream().limit(10).collect(Collectors.toList());
        long unreadNotify = notifyService.countUnread();

        Long ownerId = SecurityUtils.isAdmin() ? null : SecurityUtils.getUserId();
        long queryUvToday = nz(accessLogMapper.countDistinctIp("query", "view", ownerId, true));
        long surveyUvToday = nz(accessLogMapper.countDistinctIp("survey", "view", ownerId, true));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("queryTotal", queries.size());
        data.put("queryPublished", queryPublished);
        data.put("surveyTotal", surveys.size());
        data.put("surveyPublished", surveyPublished);
        data.put("rowCount", rowCount);
        data.put("answerCount", answerCount);
        data.put("unreadNotify", unreadNotify);
        data.put("queryViews", queryViews);
        data.put("querySearches", querySearches);
        data.put("queryUvToday", queryUvToday);
        data.put("surveyUvToday", surveyUvToday);
        data.put("surveyViews", surveyViews);
        data.put("recentQueries", recentQueries);
        data.put("recentSurveys", recentSurveys);
        data.put("recentNotifies", recentNotifies);
        return data;
    }

    private long nz(Long v)
    {
        return v == null ? 0L : v;
    }
}
