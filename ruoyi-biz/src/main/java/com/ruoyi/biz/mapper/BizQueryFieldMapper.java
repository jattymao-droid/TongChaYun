package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizQueryField;

public interface BizQueryFieldMapper
{
    List<BizQueryField> selectFieldsByQueryId(Long queryId);

    int insertField(BizQueryField field);

    int batchInsertFields(List<BizQueryField> fields);

    int updateField(BizQueryField field);

    int deleteByQueryId(Long queryId);
}
