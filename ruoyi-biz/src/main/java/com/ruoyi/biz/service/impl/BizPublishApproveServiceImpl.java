package com.ruoyi.biz.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.biz.domain.BizPublishRequest;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.mapper.BizPublishRequestMapper;
import com.ruoyi.biz.mapper.BizQueryMapper;
import com.ruoyi.biz.mapper.BizSurveyMapper;
import com.ruoyi.biz.service.IBizPublishApproveService;
import com.ruoyi.biz.service.IBizQueryService;
import com.ruoyi.biz.service.IBizSurveyService;
import com.ruoyi.biz.service.IBizVersionService;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;

@Service
public class BizPublishApproveServiceImpl implements IBizPublishApproveService
{
    @Autowired
    private ISysConfigService configService;
    @Autowired
    private BizPublishRequestMapper requestMapper;
    @Autowired
    private BizQueryMapper queryMapper;
    @Autowired
    private BizSurveyMapper surveyMapper;
    @Autowired
    @Lazy
    private IBizQueryService queryService;
    @Autowired
    @Lazy
    private IBizSurveyService surveyService;
    @Autowired
    private IBizVersionService versionService;

    @Override
    public boolean isApproveEnabled()
    {
        return Convert.toBool(configService.selectConfigByKey("sys.biz.publishApprove"), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestOrPublish(String projectType, Long projectId)
    {
        boolean query = "query".equals(projectType);
        String name;
        if (query)
        {
            BizQuery q = queryMapper.selectBizQueryById(projectId);
            if (q == null)
            {
                throw new ServiceException("查询项目不存在");
            }
            name = q.getQueryName();
            queryService.checkOwner(q);
        }
        else
        {
            BizSurvey s = surveyMapper.selectBizSurveyById(projectId);
            if (s == null)
            {
                throw new ServiceException("问卷不存在");
            }
            name = s.getSurveyName();
            // access via publish path ownership
            surveyService.selectDetail(projectId);
        }

        if (!isApproveEnabled() || SecurityUtils.isAdmin())
        {
            String code = query ? queryService.publish(projectId) : surveyService.publish(projectId);
            versionService.audit(projectType, projectId, "publish", "direct publicCode=" + code);
            Map<String, Object> data = new HashMap<>();
            data.put("pending", false);
            data.put("publicCode", code);
            data.put("path", (query ? "/q/" : "/s/") + code);
            return data;
        }

        BizPublishRequest probe = new BizPublishRequest();
        probe.setProjectType(projectType);
        probe.setProjectId(projectId);
        if (requestMapper.countPendingByProject(probe) > 0)
        {
            throw new ServiceException("已有待审批的发布申请，请勿重复提交");
        }
        BizPublishRequest req = new BizPublishRequest();
        req.setProjectType(projectType);
        req.setProjectId(projectId);
        req.setProjectName(name);
        req.setStatus("0");
        req.setApplyBy(SecurityUtils.getUsername());
        req.setApplyUserId(SecurityUtils.getUserId());
        requestMapper.insert(req);
        versionService.audit(projectType, projectId, "publish_request", "requestId=" + req.getRequestId());
        Map<String, Object> data = new HashMap<>();
        data.put("pending", true);
        data.put("requestId", req.getRequestId());
        data.put("message", "已提交发布审批，请等待管理员通过");
        return data;
    }

    @Override
    public List<BizPublishRequest> list(BizPublishRequest query)
    {
        if (!SecurityUtils.isAdmin())
        {
            if (query == null)
            {
                query = new BizPublishRequest();
            }
            query.setApplyUserId(SecurityUtils.getUserId());
        }
        return requestMapper.selectList(query == null ? new BizPublishRequest() : query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long requestId, String remark)
    {
        if (!SecurityUtils.isAdmin())
        {
            throw new ServiceException("仅管理员可审批");
        }
        BizPublishRequest req = requestMapper.selectById(requestId);
        if (req == null || !"0".equals(req.getStatus()))
        {
            throw new ServiceException("申请不存在或已处理");
        }
        String code;
        if ("query".equals(req.getProjectType()))
        {
            code = queryService.publish(req.getProjectId());
        }
        else
        {
            code = surveyService.publish(req.getProjectId());
        }
        BizPublishRequest upd = new BizPublishRequest();
        upd.setRequestId(requestId);
        upd.setStatus("1");
        upd.setReviewBy(SecurityUtils.getUsername());
        upd.setReviewUserId(SecurityUtils.getUserId());
        upd.setReviewRemark(StringUtils.substring(StringUtils.nvl(remark, ""), 0, 500));
        requestMapper.updateReview(upd);
        versionService.audit(req.getProjectType(), req.getProjectId(), "publish_approve",
            "requestId=" + requestId + ", code=" + code);
        Map<String, Object> data = new HashMap<>();
        data.put("pending", false);
        data.put("publicCode", code);
        data.put("path", ("query".equals(req.getProjectType()) ? "/q/" : "/s/") + code);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long requestId, String remark)
    {
        if (!SecurityUtils.isAdmin())
        {
            throw new ServiceException("仅管理员可审批");
        }
        BizPublishRequest req = requestMapper.selectById(requestId);
        if (req == null || !"0".equals(req.getStatus()))
        {
            throw new ServiceException("申请不存在或已处理");
        }
        BizPublishRequest upd = new BizPublishRequest();
        upd.setRequestId(requestId);
        upd.setStatus("2");
        upd.setReviewBy(SecurityUtils.getUsername());
        upd.setReviewUserId(SecurityUtils.getUserId());
        upd.setReviewRemark(StringUtils.substring(StringUtils.nvl(remark, "驳回"), 0, 500));
        requestMapper.updateReview(upd);
        versionService.audit(req.getProjectType(), req.getProjectId(), "publish_reject",
            "requestId=" + requestId);
    }
}
