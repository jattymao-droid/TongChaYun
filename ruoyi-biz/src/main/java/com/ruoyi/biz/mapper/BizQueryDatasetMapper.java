package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizQueryDataset;

public interface BizQueryDatasetMapper
{
    List<BizQueryDataset> selectByQueryId(Long queryId);

    BizQueryDataset selectById(Long datasetId);

    int insertDataset(BizQueryDataset dataset);

    int updateDataset(BizQueryDataset dataset);

    int deleteById(Long datasetId);

    int deleteByQueryId(Long queryId);

    int clearPrimary(Long queryId);
}
