package com.ruoyi.biz.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizQueryAdmin;

public interface BizQueryAdminMapper
{
    List<BizQueryAdmin> selectByQueryId(Long queryId);

    int countByQueryAndUser(@Param("queryId") Long queryId, @Param("userId") Long userId);

    int insert(BizQueryAdmin admin);

    int deleteByQueryAndUser(@Param("queryId") Long queryId, @Param("userId") Long userId);

    int deleteByQueryId(Long queryId);

    List<Map<String, Object>> searchUsers(@Param("keyword") String keyword);

    Map<String, Object> findUserByNameOrPhone(@Param("keyword") String keyword);
}
