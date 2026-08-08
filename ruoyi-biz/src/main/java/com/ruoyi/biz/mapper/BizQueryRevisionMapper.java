package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizQueryRevision;
import com.ruoyi.biz.domain.BizQueryRow;

public interface BizQueryRevisionMapper
{
    Integer selectMaxRevNo(Long queryId);

    int insertRevision(BizQueryRevision rev);

    int batchInsertRows(@Param("revId") Long revId, @Param("list") List<BizQueryRow> rows);

    List<BizQueryRevision> selectByQueryId(Long queryId);

    BizQueryRevision selectById(Long revId);

    BizQueryRevision selectByQueryAndRevNo(@Param("queryId") Long queryId, @Param("revNo") Integer revNo);

    List<BizQueryRow> selectRowsByRevId(Long revId);

    List<Long> selectOldRevIds(@Param("queryId") Long queryId, @Param("keep") int keep);

    int deleteRowsByRevIds(@Param("revIds") List<Long> revIds);

    int deleteByIds(@Param("revIds") List<Long> revIds);
}
