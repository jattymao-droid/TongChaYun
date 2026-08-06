package com.ruoyi.biz.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizQueryRow;

public interface BizQueryRowMapper
{
    int batchInsertRows(List<BizQueryRow> rows);

    int deleteByQueryId(Long queryId);

    List<BizQueryRow> searchRows(@Param("queryId") Long queryId,
        @Param("conditions") List<Map<String, String>> conditions,
        @Param("orderKey") String orderKey);

    long countRows(@Param("queryId") Long queryId, @Param("conditions") List<Map<String, String>> conditions);

    Integer selectMaxRowNo(Long queryId);

    List<BizQueryRow> selectAllRows(Long queryId);

    List<BizQueryRow> selectSampleRows(@Param("queryId") Long queryId, @Param("limit") int limit);

    List<Map<String, Object>> selectFieldDist(@Param("queryId") Long queryId, @Param("fieldKey") String fieldKey);

    List<Map<String, Object>> selectFieldDistFiltered(@Param("queryId") Long queryId,
        @Param("conditions") List<Map<String, String>> conditions,
        @Param("fieldKey") String fieldKey);
}
