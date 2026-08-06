package com.ruoyi.biz.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.domain.BizSurveyAnswer;
import com.ruoyi.biz.domain.BizSurveyQuestion;
import com.ruoyi.biz.domain.vo.BizSurveyDetailVo;

public interface IBizSurveyService
{
    List<BizSurvey> selectBizSurveyList(BizSurvey survey);

    BizSurveyDetailVo selectDetail(Long surveyId);

    BizSurvey insertBizSurvey(BizSurvey survey);

    int updateBizSurvey(BizSurvey survey);

    int deleteBizSurveyByIds(Long[] surveyIds);

    int saveQuestions(Long surveyId, List<BizSurveyQuestion> questions);

    String publish(Long surveyId);

    int offline(Long surveyId);

    List<BizSurveyAnswer> selectAnswerList(BizSurveyAnswer answer);

    BizSurveyAnswer selectAnswerDetail(Long answerId);

    int updateAnswerMeta(BizSurveyAnswer answer);

    Map<String, Object> selectStats(Long surveyId);

    Map<String, Object> selectCrossStats(Long surveyId, Long q1Id, Long q2Id);

    Map<String, Object> openMeta(String code, String accessPwd);

    Long openSubmit(String code, Map<String, Object> body, String ip, String ua);

    Map<String, Object> openUpload(String code, String accessPwd, MultipartFile file) throws Exception;

    Map<String, Object> openLoadDraft(String code, String accessPwd, String clientToken);

    void openSaveDraft(String code, Map<String, Object> body);

    void exportAnswers(Long surveyId, BizSurveyAnswer filter, jakarta.servlet.http.HttpServletResponse response) throws Exception;

    void exportStats(Long surveyId, Long crossQ1, Long crossQ2, jakarta.servlet.http.HttpServletResponse response) throws Exception;

    BizSurvey copySurvey(Long surveyId);

    void testWebhook(Long surveyId);

    java.util.List<java.util.Map<String, Object>> listTemplates();

    BizSurvey createFromTemplate(String templateKey);

    /** Admin: reassign survey owner */
    int transferOwnership(Long surveyId, Long targetUserId);
}
