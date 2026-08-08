package com.ruoyi.biz.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.biz.domain.BizProjectBlacklist;

public interface IBizRiskService
{
    Map<String, Object> surveyBoard(Long surveyId);

    List<BizProjectBlacklist> listBlacklist(String targetType, Long targetId);

    int addBlacklist(String targetType, Long targetId, String kind, String value, String reason, boolean markInvalid);

    int removeBlacklist(String targetType, Long targetId, Long id);

    /** Reject when IP or device is blacklisted. */
    void assertNotBlacklisted(String targetType, Long targetId, String ip, String device);

    List<Map<String, Object>> buildFunnel(String targetType, Long targetId);

    List<Map<String, Object>> buildChannelCompare(Long surveyId);

    List<Map<String, Object>> decorateRiskRows(List<Map<String, Object>> rows);
}
