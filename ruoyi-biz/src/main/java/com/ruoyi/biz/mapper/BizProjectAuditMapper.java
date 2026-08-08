package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizProjectAudit;

public interface BizProjectAuditMapper
{
    int insert(BizProjectAudit audit);

    List<BizProjectAudit> selectList(BizProjectAudit query);
}
