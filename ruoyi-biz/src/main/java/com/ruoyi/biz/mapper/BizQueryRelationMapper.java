package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizQueryRelation;

public interface BizQueryRelationMapper
{
    List<BizQueryRelation> selectByQueryId(Long queryId);

    int insertRelation(BizQueryRelation relation);

    int deleteByQueryId(Long queryId);

    int deleteByDatasetId(Long datasetId);
}
