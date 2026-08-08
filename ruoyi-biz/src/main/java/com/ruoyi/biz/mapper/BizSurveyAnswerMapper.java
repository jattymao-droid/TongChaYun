package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizSurveyAnswer;

public interface BizSurveyAnswerMapper
{
    List<BizSurveyAnswer> selectAnswerList(BizSurveyAnswer answer);

    BizSurveyAnswer selectAnswerById(Long answerId);

    int insertAnswer(BizSurveyAnswer answer);

    int updateAnswerMeta(BizSurveyAnswer answer);

    int deleteBySurveyId(Long surveyId);

    long countBySurveyId(Long surveyId);

    long countValidBySurveyId(Long surveyId);

    long countBySurveyIdAndIp(@Param("surveyId") Long surveyId, @Param("submitIp") String submitIp);

    long countBySurveyIdAndClientToken(@Param("surveyId") Long surveyId, @Param("clientToken") String clientToken);

    long countTodayBySurveyId(Long surveyId);

    java.util.List<java.util.Map<String, Object>> selectChannelStats(Long surveyId);

    java.util.List<java.util.Map<String, Object>> selectDailyStats(Long surveyId);

    java.util.List<java.util.Map<String, Object>> selectRiskByIp(Long surveyId);

    java.util.List<java.util.Map<String, Object>> selectRiskByDevice(Long surveyId);

    int markInvalidByIp(@Param("surveyId") Long surveyId, @Param("submitIp") String submitIp);

    int markInvalidByDevice(@Param("surveyId") Long surveyId, @Param("clientToken") String clientToken);
}
