package com.ruoyi.biz.mapper;

import java.util.List;
import com.ruoyi.biz.domain.BizSurveyQuestion;

public interface BizSurveyQuestionMapper
{
    List<BizSurveyQuestion> selectBySurveyId(Long surveyId);

    int batchInsert(List<BizSurveyQuestion> list);

    int deleteBySurveyId(Long surveyId);
}
