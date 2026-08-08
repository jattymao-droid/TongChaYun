package com.ruoyi.biz.controller.admin;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizPublishRequest;
import com.ruoyi.biz.domain.BizProjectAudit;
import com.ruoyi.biz.domain.BizQueryRevision;
import com.ruoyi.biz.domain.BizSurveyRevision;
import com.ruoyi.biz.service.IBizPublishApproveService;
import com.ruoyi.biz.service.IBizVersionService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

@RestController
@RequestMapping("/biz/version")
public class BizVersionController extends BaseController
{
    @Autowired
    private IBizVersionService versionService;
    @Autowired
    private IBizPublishApproveService publishApproveService;

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/query/{queryId}/revisions")
    public AjaxResult queryRevisions(@PathVariable Long queryId)
    {
        List<BizQueryRevision> list = versionService.listQueryRevisions(queryId);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "Query data rollback", businessType = BusinessType.UPDATE)
    @PostMapping("/query/{queryId}/revisions/{revId}/rollback")
    public AjaxResult rollbackQuery(@PathVariable Long queryId, @PathVariable Long revId)
    {
        versionService.rollbackQuery(queryId, revId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/survey/{surveyId}/revisions")
    public AjaxResult surveyRevisions(@PathVariable Long surveyId)
    {
        List<BizSurveyRevision> list = versionService.listSurveyRevisions(surveyId);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:edit')")
    @Log(title = "Survey design rollback", businessType = BusinessType.UPDATE)
    @PostMapping("/survey/{surveyId}/revisions/{revId}/rollback")
    public AjaxResult rollbackSurvey(@PathVariable Long surveyId, @PathVariable Long revId)
    {
        versionService.rollbackSurvey(surveyId, revId);
        return success();
    }

    @PreAuthorize("@ss.hasAnyPermi('biz:query:query,biz:survey:query')")
    @GetMapping("/audit")
    public AjaxResult audit(@RequestParam String projectType, @RequestParam Long projectId)
    {
        List<BizProjectAudit> list = versionService.listAudit(projectType, projectId);
        return success(list);
    }

    @PreAuthorize("@ss.hasAnyPermi('biz:query:publish,biz:survey:publish')")
    @GetMapping("/publish/requests")
    public TableDataInfo publishRequests(BizPublishRequest query)
    {
        startPage();
        return getDataTable(publishApproveService.list(query));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:publish') or @ss.hasPermi('biz:survey:publish')")
    @GetMapping("/publish/approve-enabled")
    public AjaxResult approveEnabled()
    {
        return success(publishApproveService.isApproveEnabled());
    }

    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "Approve publish", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/requests/{requestId}/approve")
    public AjaxResult approve(@PathVariable Long requestId, @RequestBody(required = false) Map<String, Object> body)
    {
        String remark = body == null || body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return success(publishApproveService.approve(requestId, remark));
    }

    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "Reject publish", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/requests/{requestId}/reject")
    public AjaxResult reject(@PathVariable Long requestId, @RequestBody(required = false) Map<String, Object> body)
    {
        String remark = body == null || body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        publishApproveService.reject(requestId, remark);
        return success();
    }
}
