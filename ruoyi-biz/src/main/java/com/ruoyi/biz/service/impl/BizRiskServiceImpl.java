package com.ruoyi.biz.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.biz.domain.BizProjectBlacklist;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.mapper.BizAccessLogMapper;
import com.ruoyi.biz.mapper.BizProjectBlacklistMapper;
import com.ruoyi.biz.mapper.BizSurveyAdminMapper;
import com.ruoyi.biz.mapper.BizSurveyAnswerMapper;
import com.ruoyi.biz.mapper.BizSurveyMapper;
import com.ruoyi.biz.service.IBizRiskService;
import com.ruoyi.biz.utils.BizProjectScopeHelper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

@Service
public class BizRiskServiceImpl implements IBizRiskService
{
    @Autowired
    private BizSurveyMapper surveyMapper;
    @Autowired
    private BizSurveyAdminMapper surveyAdminMapper;
    @Autowired
    private BizSurveyAnswerMapper answerMapper;
    @Autowired
    private BizAccessLogMapper accessLogMapper;
    @Autowired
    private BizProjectBlacklistMapper blacklistMapper;
    @Autowired
    private BizProjectScopeHelper projectScopeHelper;

    @Override
    public Map<String, Object> surveyBoard(Long surveyId)
    {
        BizSurvey survey = requireSurveyAccess(surveyId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("surveyId", survey.getSurveyId());
        data.put("surveyName", survey.getSurveyName());
        data.put("riskByIp", decorateRiskRows(answerMapper.selectRiskByIp(surveyId)));
        data.put("riskByDevice", decorateRiskRows(answerMapper.selectRiskByDevice(surveyId)));
        data.put("funnel", buildFunnel("survey", surveyId));
        data.put("channelCompare", buildChannelCompare(surveyId));
        data.put("blacklist", listBlacklist("survey", surveyId));
        return data;
    }

    @Override
    public List<BizProjectBlacklist> listBlacklist(String targetType, Long targetId)
    {
        assertTargetAccess(targetType, targetId);
        return blacklistMapper.selectByTarget(targetType, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addBlacklist(String targetType, Long targetId, String kind, String value, String reason,
        boolean markInvalid)
    {
        assertTargetAccess(targetType, targetId);
        String k = normalizeKind(kind);
        String v = StringUtils.trim(value);
        if (StringUtils.isEmpty(v))
        {
            throw new ServiceException("blacklist value required");
        }
        v = StringUtils.substring(v, 0, 128);
        BizProjectBlacklist row = new BizProjectBlacklist();
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setKind(k);
        row.setValue(v);
        row.setReason(StringUtils.substring(StringUtils.nvl(reason, ""), 0, 200));
        try
        {
            row.setCreateBy(SecurityUtils.getUsername());
        }
        catch (Exception ignored)
        {
            row.setCreateBy("");
        }
        int n = blacklistMapper.insert(row);
        if (markInvalid && "survey".equals(targetType))
        {
            if ("ip".equals(k))
            {
                answerMapper.markInvalidByIp(targetId, v);
            }
            else
            {
                answerMapper.markInvalidByDevice(targetId, v);
            }
        }
        return n;
    }

    @Override
    public int removeBlacklist(String targetType, Long targetId, Long id)
    {
        assertTargetAccess(targetType, targetId);
        if (id == null)
        {
            throw new ServiceException("id required");
        }
        return blacklistMapper.deleteById(id, targetType, targetId);
    }

    @Override
    public void assertNotBlacklisted(String targetType, Long targetId, String ip, String device)
    {
        if (targetId == null)
        {
            return;
        }
        int hit = blacklistMapper.countHit(targetType, targetId,
            StringUtils.isEmpty(ip) ? null : ip.trim(),
            StringUtils.isEmpty(device) ? null : device.trim());
        if (hit > 0)
        {
            throw new ServiceException("\u63d0\u4ea4\u88ab\u9650\u5236\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458");
        }
    }

    @Override
    public List<Map<String, Object>> buildFunnel(String targetType, Long targetId)
    {
        List<String> actions = Arrays.asList("view", "start", "submit");
        List<Map<String, Object>> raw = accessLogMapper.selectFunnel(targetType, targetId, actions);
        Map<String, Map<String, Object>> byAction = new HashMap<>();
        if (raw != null)
        {
            for (Map<String, Object> row : raw)
            {
                if (row != null && row.get("action") != null)
                {
                    byAction.put(String.valueOf(row.get("action")), row);
                }
            }
        }
        long viewUv = longOf(byAction, "view", "uv");
        long startUv = longOf(byAction, "start", "uv");
        long submitUv = longOf(byAction, "submit", "uv");
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(funnelStep("view", "\u6d4f\u89c8", longOf(byAction, "view", "events"), viewUv, null));
        out.add(funnelStep("start", "\u5f00\u59cb\u586b\u5199", longOf(byAction, "start", "events"), startUv, viewUv));
        out.add(funnelStep("submit", "\u63d0\u4ea4", longOf(byAction, "submit", "events"), submitUv, startUv > 0 ? startUv : viewUv));
        return out;
    }

    @Override
    public List<Map<String, Object>> buildChannelCompare(Long surveyId)
    {
        List<Map<String, Object>> submits = answerMapper.selectChannelStats(surveyId);
        List<Map<String, Object>> uvs = accessLogMapper.selectChannelUv("survey", surveyId, "view");
        Map<String, Long> submitMap = new LinkedHashMap<>();
        if (submits != null)
        {
            for (Map<String, Object> row : submits)
            {
                String code = row.get("channelCode") == null ? "(\u9ed8\u8ba4)" : String.valueOf(row.get("channelCode"));
                submitMap.put(code, toLong(row.get("count")));
            }
        }
        Map<String, Long> uvMap = new LinkedHashMap<>();
        if (uvs != null)
        {
            for (Map<String, Object> row : uvs)
            {
                String code = row.get("channelCode") == null ? "(\u9ed8\u8ba4)" : String.valueOf(row.get("channelCode"));
                uvMap.put(code, toLong(row.get("viewUv")));
            }
        }
        List<String> keys = new ArrayList<>();
        for (String k : uvMap.keySet())
        {
            if (!keys.contains(k))
            {
                keys.add(k);
            }
        }
        for (String k : submitMap.keySet())
        {
            if (!keys.contains(k))
            {
                keys.add(k);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (String code : keys)
        {
            long uv = uvMap.getOrDefault(code, 0L);
            long cnt = submitMap.getOrDefault(code, 0L);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("channelCode", code);
            row.put("viewUv", uv);
            row.put("submitCnt", cnt);
            row.put("convertRate", uv <= 0 ? 0 : Math.round(cnt * 1000.0 / uv) / 10.0);
            out.add(row);
        }
        out.sort((a, b) -> Long.compare(toLong(b.get("submitCnt")), toLong(a.get("submitCnt"))));
        return out;
    }

    @Override
    public List<Map<String, Object>> decorateRiskRows(List<Map<String, Object>> rows)
    {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null)
        {
            return out;
        }
        for (Map<String, Object> row : rows)
        {
            if (row == null)
            {
                continue;
            }
            Map<String, Object> copy = new LinkedHashMap<>(row);
            long submitCnt = toLong(copy.get("submitCnt"));
            long invalidCnt = toLong(copy.get("invalidCnt"));
            copy.put("invalidRate", submitCnt <= 0 ? 0 : Math.round(invalidCnt * 1000.0 / submitCnt) / 10.0);
            out.add(copy);
        }
        return out;
    }

    private Map<String, Object> funnelStep(String action, String label, long events, long uv, Long baseUv)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("action", action);
        m.put("label", label);
        m.put("events", events);
        m.put("uv", uv);
        if (baseUv == null)
        {
            m.put("rate", 100.0);
        }
        else if (baseUv <= 0)
        {
            m.put("rate", 0.0);
        }
        else
        {
            m.put("rate", Math.round(uv * 1000.0 / baseUv) / 10.0);
        }
        return m;
    }

    private long longOf(Map<String, Map<String, Object>> byAction, String action, String field)
    {
        Map<String, Object> row = byAction.get(action);
        if (row == null)
        {
            return 0L;
        }
        return toLong(row.get(field));
    }

    private static long toLong(Object v)
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

    private String normalizeKind(String kind)
    {
        if ("device".equalsIgnoreCase(kind) || "client".equalsIgnoreCase(kind) || "token".equalsIgnoreCase(kind))
        {
            return "device";
        }
        if ("ip".equalsIgnoreCase(kind))
        {
            return "ip";
        }
        throw new ServiceException("kind must be ip or device");
    }

    private void assertTargetAccess(String targetType, Long targetId)
    {
        if (!"survey".equals(targetType))
        {
            throw new ServiceException("unsupported targetType");
        }
        requireSurveyAccess(targetId);
    }

    private BizSurvey requireSurveyAccess(Long surveyId)
    {
        if (surveyId == null)
        {
            throw new ServiceException("surveyId required");
        }
        BizSurvey survey = surveyMapper.selectBizSurveyById(surveyId);
        if (survey == null)
        {
            throw new ServiceException("\u95ee\u5377\u4e0d\u5b58\u5728");
        }
        Long uid = null;
        try
        {
            uid = SecurityUtils.getUserId();
        }
        catch (Exception ignored)
        {
        }
        if (uid != null && surveyAdminMapper.countBySurveyAndUser(surveyId, uid) > 0)
        {
            return survey;
        }
        projectScopeHelper.assertAccess(survey.getCreateUserId(), survey.getDeptId(),
            "biz:survey:list,biz:survey:query,biz:survey:edit", "\u65e0\u6743\u64cd\u4f5c\u8be5\u95ee\u5377");
        return survey;
    }
}
