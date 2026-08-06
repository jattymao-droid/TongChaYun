package com.ruoyi.biz.mapper;

import com.ruoyi.biz.domain.BizQueryPage;

public interface BizQueryPageMapper
{
    BizQueryPage selectByQueryId(Long queryId);

    int insertPage(BizQueryPage page);

    int updatePage(BizQueryPage page);

    int deleteByQueryId(Long queryId);
}
