package com.ruoyi.biz.domain;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Survey question
 */
public class BizSurveyQuestion implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long questionId;
    private Long surveyId;
    /** radio/checkbox/input/textarea/select */
    private String qType;
    private String title;
    private String required;
    private String optionsJson;
    private String propsJson;
    private Integer sort;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }

    @JsonProperty("qType")
    public String getQType() { return qType; }

    @JsonProperty("qType")
    public void setQType(String qType) { this.qType = qType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRequired() { return required; }
    public void setRequired(String required) { this.required = required; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public String getPropsJson() { return propsJson; }
    public void setPropsJson(String propsJson) { this.propsJson = propsJson; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
