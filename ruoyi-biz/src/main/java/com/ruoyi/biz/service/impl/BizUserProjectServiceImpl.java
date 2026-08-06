package com.ruoyi.biz.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.biz.domain.vo.BizUserProjectVo;
import com.ruoyi.biz.mapper.BizUserProjectMapper;
import com.ruoyi.biz.service.IBizUserProjectService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

@Service
public class BizUserProjectServiceImpl implements IBizUserProjectService
{
    @Autowired
    private BizUserProjectMapper userProjectMapper;

    @Override
    public List<BizUserProjectVo> selectUserProjectList(BizUserProjectVo query)
    {
        assertAdminManage();
        return userProjectMapper.selectUserProjectList(query);
    }

    @Override
    public Map<String, Object> requireActiveUser(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("请指定目标用户");
        }
        Map<String, Object> user = userProjectMapper.selectUserBrief(userId);
        if (user == null || user.isEmpty())
        {
            throw new ServiceException("目标用户不存在");
        }
        Object status = user.get("status");
        if (status != null && !"0".equals(String.valueOf(status)))
        {
            throw new ServiceException("目标用户已停用，无法转让");
        }
        return user;
    }

    public static void assertAdminManage()
    {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasRole("admin"))
        {
            throw new ServiceException("仅管理员可管理全部用户业务");
        }
    }
}
