package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizQueryDatasetRow;

public interface BizQueryDatasetRowMapper
{
    int batchInsertRows(List<BizQueryDatasetRow> list);

    int deleteByDatasetId(Long datasetId);

    int deleteByQueryId(Long queryId);

    List<BizQueryDatasetRow> selectByDatasetId(Long datasetId);

    Integer selectMaxRowNo(Long datasetId);
}
