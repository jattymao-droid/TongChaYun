package com.ruoyi.biz.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.biz.domain.BizPublishRequest;

public interface IBizPublishApproveService
{
    boolean isApproveEnabled();

    /** Returns null if published immediately; otherwise pending request info. */
    Map<String, Object> requestOrPublish(String projectType, Long projectId);

    List<BizPublishRequest> list(BizPublishRequest query);

    Map<String, Object> approve(Long requestId, String remark);

    void reject(Long requestId, String remark);
}
