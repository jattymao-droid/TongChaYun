package com.ruoyi.biz.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.biz.domain.BizAccessLog;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.mapper.BizAccessLogMapper;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import jakarta.servlet.http.HttpServletRequest;

public final class BizAccessLogHelper
{
    private BizAccessLogHelper() {}

    public static void log(String targetType, Long targetId, String publicCode, String action)
    {
        log(targetType, targetId, publicCode, action, null);
    }

    public static void log(String targetType, Long targetId, String publicCode, String action, String detailJson)
    {
        try
        {
            BizAccessLogMapper mapper = SpringUtils.getBean(BizAccessLogMapper.class);
            BizAccessLog log = new BizAccessLog();
            log.setTargetType(targetType);
            log.setTargetId(targetId);
            log.setPublicCode(publicCode);
            log.setAction(action);
            String ip = IpUtils.getIpAddr();
            log.setClientIp(ip == null ? "" : ip);
            HttpServletRequest req = ServletUtils.getRequest();
            String ua = req == null ? "" : req.getHeader("User-Agent");
            if (StringUtils.isNotEmpty(ua) && ua.length() > 480)
            {
                ua = ua.substring(0, 480);
            }
            log.setUserAgent(ua == null ? "" : ua);
            if (StringUtils.isNotEmpty(detailJson) && detailJson.length() > 2000)
            {
                detailJson = detailJson.substring(0, 2000);
            }
            log.setDetailJson(detailJson);
            mapper.insertAccessLog(log);
        }
        catch (Exception ignored)
        {
            // access log must not break public APIs
        }
    }

    public static String buildQuerySearchDetail(List<BizQueryField> fields, Map<String, Object> params,
        long hitTotal, int pageNum)
    {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("pageNum", pageNum);
        detail.put("hitTotal", hitTotal);
        Map<String, String> masked = new LinkedHashMap<>();
        if (params != null && fields != null)
        {
            Map<String, BizQueryField> byKey = new LinkedHashMap<>();
            for (BizQueryField f : fields)
            {
                if (f != null && StringUtils.isNotEmpty(f.getFieldKey()))
                {
                    byKey.put(f.getFieldKey(), f);
                }
            }
            for (Map.Entry<String, Object> e : params.entrySet())
            {
                if (e.getValue() == null)
                {
                    continue;
                }
                String raw = String.valueOf(e.getValue());
                if (StringUtils.isEmpty(raw))
                {
                    continue;
                }
                BizQueryField f = byKey.get(e.getKey());
                String label = f != null && StringUtils.isNotEmpty(f.getFieldName()) ? f.getFieldName() : e.getKey();
                String val = raw;
                if (f != null && StringUtils.isNotEmpty(f.getMaskType()) && !"none".equalsIgnoreCase(f.getMaskType()))
                {
                    val = BizMaskHelper.mask(raw, f.getMaskType());
                }
                else if (val.length() > 64)
                {
                    val = val.substring(0, 64) + "...";
                }
                masked.put(label, val);
            }
        }
        detail.put("params", masked);
        return JSON.toJSONString(detail);
    }
}
