package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizQuery;

public interface BizQueryMapper
{
    BizQuery selectBizQueryById(Long queryId);

    BizQuery selectBizQueryByCode(String publicCode);

    List<BizQuery> selectBizQueryList(BizQuery query);

    int insertBizQuery(BizQuery query);

    int updateBizQuery(BizQuery query);

    int deleteBizQueryByIds(Long[] queryIds);

    int increaseViewCount(Long queryId);

    int increaseSearchCount(Long queryId);

    int transferOwner(BizQuery query);

    List<BizQuery> selectDuePublish();

    List<BizQuery> selectDueExpire();

    List<BizQuery> selectDueRemind();
}
