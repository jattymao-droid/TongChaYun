package com.ruoyi.biz.controller.open;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.biz.service.IBizSurveyService;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.ip.IpUtils;

@RestController
@RequestMapping("/open/survey")
public class OpenSurveyController
{
    @Autowired
    private IBizSurveyService surveyService;

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/{code}/meta")
    public AjaxResult meta(@PathVariable("code") String code,
        @RequestParam(value = "accessPwd", required = false) String accessPwd,
        @RequestParam(value = "channel", required = false) String channel)
    {
        return AjaxResult.success(surveyService.openMeta(code, accessPwd, channel));
    }

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @PostMapping("/{code}/event")
    public AjaxResult event(@PathVariable("code") String code, @RequestBody(required = false) Map<String, Object> body)
    {
        surveyService.openEvent(code, body);
        return AjaxResult.success();
    }

    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/{code}/submit")
    public AjaxResult submit(@PathVariable("code") String code, @RequestBody(required = false) Map<String, Object> body,
        HttpServletRequest request)
    {
        String ua = request.getHeader("User-Agent");
        Long answerId = surveyService.openSubmit(code, body, IpUtils.getIpAddr(request), ua);
        Map<String, Object> data = new HashMap<>();
        data.put("answerId", answerId);
        return AjaxResult.success("提交成功", data);
    }

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/{code}/draft")
    public AjaxResult loadDraft(@PathVariable("code") String code,
        @RequestParam(value = "accessPwd", required = false) String accessPwd,
        @RequestParam("clientToken") String clientToken)
    {
        return AjaxResult.success(surveyService.openLoadDraft(code, accessPwd, clientToken));
    }

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @PutMapping("/{code}/draft")
    public AjaxResult saveDraft(@PathVariable("code") String code, @RequestBody(required = false) Map<String, Object> body)
    {
        surveyService.openSaveDraft(code, body);
        return AjaxResult.success();
    }

    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/{code}/upload")
    public AjaxResult upload(@PathVariable("code") String code,
        @RequestParam(value = "accessPwd", required = false) String accessPwd,
        @RequestParam("file") MultipartFile file) throws Exception
    {
        Map<String, Object> data = surveyService.openUpload(code, accessPwd, file);
        AjaxResult ajax = AjaxResult.success();
        ajax.putAll(data);
        return ajax;
    }
}
