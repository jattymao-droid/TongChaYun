package com.ruoyi.biz.domain.vo;

import java.util.List;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.domain.BizSurveyQuestion;

public class BizSurveyDetailVo
{
    private BizSurvey survey;
    private List<BizSurveyQuestion> questions;

    public BizSurvey getSurvey() { return survey; }
    public void setSurvey(BizSurvey survey) { this.survey = survey; }
    public List<BizSurveyQuestion> getQuestions() { return questions; }
    public void setQuestions(List<BizSurveyQuestion> questions) { this.questions = questions; }
}
