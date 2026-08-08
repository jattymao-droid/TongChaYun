package com.ruoyi.biz.controller.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.service.IBizReachService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;

@RestController
@RequestMapping("/biz/reach")
public class BizReachController extends BaseController
{
    @Autowired
    private IBizReachService reachService;

    @PreAuthorize("@ss.hasPermi('biz:survey:publish')")
    @Log(title = "Survey schedule publish", businessType = BusinessType.UPDATE)
    @PostMapping("/survey/schedule/{surveyId}")
    public AjaxResult scheduleSurvey(@PathVariable Long surveyId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishAt)
    {
        String code = reachService.scheduleSurveyPublish(surveyId, publishAt);
        Map<String, Object> data = new HashMap<>();
        data.put("publicCode", code);
        data.put("path", "/s/" + code);
        data.put("publishAt", publishAt);
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:publish')")
    @Log(title = "Cancel survey schedule", businessType = BusinessType.UPDATE)
    @PostMapping("/survey/schedule/cancel/{surveyId}")
    public AjaxResult cancelSurvey(@PathVariable Long surveyId)
    {
        return toAjax(reachService.cancelSurveySchedule(surveyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:publish')")
    @Log(title = "Query schedule publish", businessType = BusinessType.UPDATE)
    @PostMapping("/query/schedule/{queryId}")
    public AjaxResult scheduleQuery(@PathVariable Long queryId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishAt)
    {
        String code = reachService.scheduleQueryPublish(queryId, publishAt);
        Map<String, Object> data = new HashMap<>();
        data.put("publicCode", code);
        data.put("path", "/q/" + code);
        data.put("publishAt", publishAt);
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('biz:query:publish')")
    @Log(title = "Cancel query schedule", businessType = BusinessType.UPDATE)
    @PostMapping("/query/schedule/cancel/{queryId}")
    public AjaxResult cancelQuery(@PathVariable Long queryId)
    {
        return toAjax(reachService.cancelQuerySchedule(queryId));
    }

    @PreAuthorize("@ss.hasAnyPermi('biz:survey:publish,biz:query:publish')")
    @Log(title = "Publish mail notify", businessType = BusinessType.OTHER)
    @PostMapping("/publish-notify")
    public AjaxResult publishNotify(@RequestBody Map<String, Object> body)
    {
        String type = body == null || body.get("type") == null ? "survey" : String.valueOf(body.get("type"));
        Long projectId = body == null || body.get("projectId") == null ? null
            : Long.valueOf(String.valueOf(body.get("projectId")));
        String emails = body == null || body.get("emails") == null ? null : String.valueOf(body.get("emails"));
        String link = body == null || body.get("link") == null ? null : String.valueOf(body.get("link"));
        if (projectId == null || StringUtils.isEmpty(emails))
        {
            return error("projectId and emails required");
        }
        reachService.sendPublishNotify(type, projectId, emails, link);
        return success();
    }
}
