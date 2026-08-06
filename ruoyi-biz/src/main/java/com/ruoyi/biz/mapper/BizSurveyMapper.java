package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizSurvey;

public interface BizSurveyMapper
{
    BizSurvey selectBizSurveyById(Long surveyId);

    /** Lock survey row for concurrent submit limit checks. */
    BizSurvey selectBizSurveyByIdForUpdate(Long surveyId);

    BizSurvey selectBizSurveyByCode(String publicCode);

    List<BizSurvey> selectBizSurveyList(BizSurvey survey);

    int insertBizSurvey(BizSurvey survey);

    int updateBizSurvey(BizSurvey survey);

    int deleteBizSurveyByIds(Long[] surveyIds);

    int increaseAnswerCount(Long surveyId);

    int increaseViewCount(Long surveyId);

    int transferOwner(BizSurvey survey);
}
