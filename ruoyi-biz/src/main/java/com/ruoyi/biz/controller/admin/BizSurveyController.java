package com.ruoyi.biz.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.domain.BizSurveyAnswer;
import com.ruoyi.biz.domain.BizSurveyQuestion;
import com.ruoyi.biz.service.IBizSurveyService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

@RestController
@RequestMapping("/biz/survey")
public class BizSurveyController extends BaseController
{
    @Autowired
    private IBizSurveyService surveyService;

    @PreAuthorize("@ss.hasPermi('biz:survey:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizSurvey survey)
    {
        startPage();
        List<BizSurvey> list = surveyService.selectBizSurveyList(survey);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/{surveyId}")
    public AjaxResult getInfo(@PathVariable Long surveyId)
    {
        return success(surveyService.selectDetail(surveyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:add')")
    @Log(title = "问卷项目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizSurvey survey)
    {
        return success(surveyService.insertBizSurvey(survey));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:edit')")
    @Log(title = "问卷项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizSurvey survey)
    {
        return toAjax(surveyService.updateBizSurvey(survey));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:remove')")
    @Log(title = "问卷项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{surveyIds}")
    public AjaxResult remove(@PathVariable Long[] surveyIds)
    {
        return toAjax(surveyService.deleteBizSurveyByIds(surveyIds));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:edit')")
    @Log(title = "问卷题目", businessType = BusinessType.UPDATE)
    @PutMapping("/questions/{surveyId}")
    public AjaxResult saveQuestions(@PathVariable Long surveyId, @RequestBody List<BizSurveyQuestion> questions)
    {
        return toAjax(surveyService.saveQuestions(surveyId, questions));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:publish')")
    @Log(title = "问卷发布", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/{surveyId}")
    public AjaxResult publish(@PathVariable Long surveyId)
    {
        String code = surveyService.publish(surveyId);
        Map<String, Object> data = new HashMap<>();
        data.put("publicCode", code);
        data.put("path", "/s/" + code);
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:publish')")
    @Log(title = "问卷停用", businessType = BusinessType.UPDATE)
    @PostMapping("/offline/{surveyId}")
    public AjaxResult offline(@PathVariable Long surveyId)
    {
        return toAjax(surveyService.offline(surveyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/answer/list")
    public TableDataInfo answers(BizSurveyAnswer answer)
    {
        startPage();
        List<BizSurveyAnswer> list = surveyService.selectAnswerList(answer);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/answer/{answerId}")
    public AjaxResult answerDetail(@PathVariable Long answerId)
    {
        return success(surveyService.selectAnswerDetail(answerId));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:edit')")
    @Log(title = "答卷标记", businessType = BusinessType.UPDATE)
    @PutMapping("/answer/{answerId}")
    public AjaxResult updateAnswer(@PathVariable Long answerId, @RequestBody BizSurveyAnswer body)
    {
        if (body == null)
        {
            body = new BizSurveyAnswer();
        }
        body.setAnswerId(answerId);
        return toAjax(surveyService.updateAnswerMeta(body));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/stats/{surveyId}")
    public AjaxResult stats(@PathVariable Long surveyId)
    {
        return success(surveyService.selectStats(surveyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/stats/{surveyId}/cross")
    public AjaxResult crossStats(@PathVariable Long surveyId,
        @RequestParam("q1") Long q1,
        @RequestParam("q2") Long q2)
    {
        return success(surveyService.selectCrossStats(surveyId, q1, q2));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @Log(title = "答卷导出", businessType = BusinessType.EXPORT)
    @PostMapping("/answer/export/{surveyId}")
    public void exportAnswers(@PathVariable Long surveyId, BizSurveyAnswer query, jakarta.servlet.http.HttpServletResponse response) throws Exception
    {
        surveyService.exportAnswers(surveyId, query, response);
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @Log(title = "问卷统计导出", businessType = BusinessType.EXPORT)
    @PostMapping("/stats/export/{surveyId}")
    public void exportStats(@PathVariable Long surveyId,
        @RequestParam(value = "q1", required = false) Long q1,
        @RequestParam(value = "q2", required = false) Long q2,
        jakarta.servlet.http.HttpServletResponse response) throws Exception
    {
        surveyService.exportStats(surveyId, q1, q2, response);
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:add')")
    @Log(title = "问卷复制", businessType = BusinessType.INSERT)
    @PostMapping("/copy/{surveyId}")
    public AjaxResult copy(@PathVariable Long surveyId)
    {
        return success(surveyService.copySurvey(surveyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:edit')")
    @Log(title = "问卷Webhook测试", businessType = BusinessType.OTHER)
    @PostMapping("/webhook/test/{surveyId}")
    public AjaxResult testWebhook(@PathVariable Long surveyId)
    {
        surveyService.testWebhook(surveyId);
        return success("已发送测试回调");
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:add')")
    @GetMapping("/templates")
    public AjaxResult templates()
    {
        return success(surveyService.listTemplates());
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:add')")
    @Log(title = "问卷模板创建", businessType = BusinessType.INSERT)
    @PostMapping("/fromTemplate/{key}")
    public AjaxResult fromTemplate(@PathVariable("key") String key)
    {
        return success(surveyService.createFromTemplate(key));
    }

    @PreAuthorize("@ss.hasPermi('biz:user:transfer')")
    @Log(title = "问卷转让归属", businessType = BusinessType.UPDATE)
    @PutMapping("/{surveyId}/transfer/{targetUserId}")
    public AjaxResult transfer(@PathVariable Long surveyId, @PathVariable Long targetUserId)
    {
        return toAjax(surveyService.transferOwnership(surveyId, targetUserId));
    }
}
