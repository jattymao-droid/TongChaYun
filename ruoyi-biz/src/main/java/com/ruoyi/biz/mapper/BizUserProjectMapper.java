package com.ruoyi.biz.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.vo.BizUserProjectVo;

public interface BizUserProjectMapper
{
    List<BizUserProjectVo> selectUserProjectList(BizUserProjectVo query);

    Map<String, Object> selectUserBrief(@Param("userId") Long userId);
}
