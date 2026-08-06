package com.ruoyi.biz.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Survey answer sheet
 */
public class BizSurveyAnswer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long answerId;
    private Long surveyId;
    private String submitIp;
    private String submitUa;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;
    private Integer costMs;
    private String channelCode;
    private String clientToken;
    /** 1 valid 0 invalid */
    private String validFlag;
    private String remark;
    private List<BizSurveyAnswerItem> items;

    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }
    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public String getSubmitIp() { return submitIp; }
    public void setSubmitIp(String submitIp) { this.submitIp = submitIp; }
    public String getSubmitUa() { return submitUa; }
    public void setSubmitUa(String submitUa) { this.submitUa = submitUa; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }
    public Integer getCostMs() { return costMs; }
    public void setCostMs(Integer costMs) { this.costMs = costMs; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public String getClientToken() { return clientToken; }
    public void setClientToken(String clientToken) { this.clientToken = clientToken; }
    public String getValidFlag() { return validFlag; }
    public void setValidFlag(String validFlag) { this.validFlag = validFlag; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<BizSurveyAnswerItem> getItems() { return items; }
    public void setItems(List<BizSurveyAnswerItem> items) { this.items = items; }
}
