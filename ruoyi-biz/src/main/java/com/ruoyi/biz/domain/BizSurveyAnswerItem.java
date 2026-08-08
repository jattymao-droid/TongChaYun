package com.ruoyi.biz.domain;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Survey answer item
 */
public class BizSurveyAnswerItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long itemId;
    private Long answerId;
    private Long questionId;
    private String answerValue;
    /** join display */
    private String questionTitle;
    private String qType;
    /** human-readable answer for detail / export preview */
    private String displayValue;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getAnswerValue() { return answerValue; }
    public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }
    public String getQuestionTitle() { return questionTitle; }
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }

    @JsonProperty("qType")
    public String getQType() { return qType; }

    @JsonProperty("qType")
    public void setQType(String qType) { this.qType = qType; }

    public String getDisplayValue() { return displayValue; }
    public void setDisplayValue(String displayValue) { this.displayValue = displayValue; }
}
