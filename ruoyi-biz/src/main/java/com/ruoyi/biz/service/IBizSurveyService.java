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

    /** Batch update validFlag/remark. Body: answerIds, validFlag, remark */
    int batchUpdateAnswerMeta(Map<String, Object> body);

    Map<String, Object> selectStats(Long surveyId);

    /**
     * Answer matrix for stats: rows = questions (+ meta), columns = respondents.
     * @param pageNum 1-based respondent page
     * @param pageSize respondents per page (columns)
     * @param validFlag optional filter: 1 valid / 0 invalid / null all
     */
    Map<String, Object> selectAnswerMatrix(Long surveyId, Integer pageNum, Integer pageSize, String validFlag);

    Map<String, Object> selectCrossStats(Long surveyId, Long q1Id, Long q2Id);

    Map<String, Object> openMeta(String code, String accessPwd, String channel);

    void openEvent(String code, Map<String, Object> body);

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

    List<com.ruoyi.biz.domain.BizSurveyAdmin> listSurveyAdmins(Long surveyId);

    List<Map<String, Object>> searchUsersForAdmin(String keyword);

    int addSurveyAdmin(Long surveyId, Long userId, String keyword);

    int removeSurveyAdmin(Long surveyId, Long userId);
}
