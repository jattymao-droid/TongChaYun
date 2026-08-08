package com.ruoyi.biz.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizSurveyAdmin;

public interface BizSurveyAdminMapper
{
    List<BizSurveyAdmin> selectBySurveyId(Long surveyId);

    int countBySurveyAndUser(@Param("surveyId") Long surveyId, @Param("userId") Long userId);

    int insert(BizSurveyAdmin admin);

    int deleteBySurveyAndUser(@Param("surveyId") Long surveyId, @Param("userId") Long userId);

    int deleteBySurveyId(Long surveyId);

    List<Map<String, Object>> searchUsers(@Param("keyword") String keyword);

    Map<String, Object> findUserByNameOrPhone(@Param("keyword") String keyword);
}
