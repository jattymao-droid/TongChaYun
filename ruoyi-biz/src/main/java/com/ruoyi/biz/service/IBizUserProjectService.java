package com.ruoyi.biz.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.biz.domain.vo.BizUserProjectVo;

public interface IBizUserProjectService
{
    List<BizUserProjectVo> selectUserProjectList(BizUserProjectVo query);

    Map<String, Object> requireActiveUser(Long userId);
}
