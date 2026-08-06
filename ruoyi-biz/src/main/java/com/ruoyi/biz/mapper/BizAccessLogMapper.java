package com.ruoyi.biz.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizAccessLog;

public interface BizAccessLogMapper
{
    int insertAccessLog(BizAccessLog log);

    Long countDistinctIp(@Param("targetType") String targetType,
        @Param("action") String action,
        @Param("createUserId") Long createUserId,
        @Param("todayOnly") boolean todayOnly);

    java.util.List<BizAccessLog> selectByTarget(@Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        @Param("action") String action,
        @Param("limit") int limit);
}
