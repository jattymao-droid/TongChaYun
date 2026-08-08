package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizProjectBlacklist;

public interface BizProjectBlacklistMapper
{
    int insert(BizProjectBlacklist row);

    int deleteById(@Param("id") Long id, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    List<BizProjectBlacklist> selectByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    int countHit(@Param("targetType") String targetType, @Param("targetId") Long targetId,
        @Param("ip") String ip, @Param("device") String device);
}
