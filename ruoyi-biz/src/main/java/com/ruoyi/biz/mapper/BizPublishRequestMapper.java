package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizPublishRequest;

public interface BizPublishRequestMapper
{
    int insert(BizPublishRequest req);

    BizPublishRequest selectById(Long requestId);

    List<BizPublishRequest> selectList(BizPublishRequest query);

    int updateReview(BizPublishRequest req);

    int countPendingByProject(BizPublishRequest query);
}
