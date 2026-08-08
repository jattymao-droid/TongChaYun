package com.ruoyi.biz.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Survey project entity
 */
public class BizSurvey extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long surveyId;
    private String surveyName;
    private String surveyDesc;
    private String publicCode;
    /** 0 draft 1 published 2 offline 3 ended */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private Integer maxAnswers;
    private String allowMulti;
    private String accessPwd;
    private String themeJson;
    /** POST JSON callback on submit */
    private String webhookUrl;
    /** Daily answer cap; 0 = unlimited */
    private Integer dailyLimit;
    /** 0 off 1 on captcha for open submit */
    private String needCaptcha;
    /** HMAC secret for webhook signature */
    private String webhookSecret;
    private Long viewCount;
    private Long answerCount;
    private Long createUserId;
    /** Creator department for data scope */
    private Long deptId;
    /** Display only: creator login name (joined) */
    private String ownerName;
    /** Display only: creator nickname (joined) */
    private String ownerNickName;
    /** 预约发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishAt;
    /** 新答卷邮件通知 0/1 */
    private String mailNotify;
    /** 通知邮箱，逗号分隔 */
    private String mailNotifyTo;
    /** 截止前提醒小时数 */
    private Integer remindHours;
    /** 截止提醒已发送 0/1 */
    private String remindSent;
    /** 截止提醒发邮件 0/1 */
    private String remindMail;

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public String getSurveyName() { return surveyName; }
    public void setSurveyName(String surveyName) { this.surveyName = surveyName; }
    public String getSurveyDesc() { return surveyDesc; }
    public void setSurveyDesc(String surveyDesc) { this.surveyDesc = surveyDesc; }
    public String getPublicCode() { return publicCode; }
    public void setPublicCode(String publicCode) { this.publicCode = publicCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public Integer getMaxAnswers() { return maxAnswers; }
    public void setMaxAnswers(Integer maxAnswers) { this.maxAnswers = maxAnswers; }
    public String getAllowMulti() { return allowMulti; }
    public void setAllowMulti(String allowMulti) { this.allowMulti = allowMulti; }
    public String getAccessPwd() { return accessPwd; }
    public void setAccessPwd(String accessPwd) { this.accessPwd = accessPwd; }
    public String getThemeJson() { return themeJson; }
    public void setThemeJson(String themeJson) { this.themeJson = themeJson; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    public String getNeedCaptcha() { return needCaptcha; }
    public void setNeedCaptcha(String needCaptcha) { this.needCaptcha = needCaptcha; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public Long getAnswerCount() { return answerCount; }
    public void setAnswerCount(Long answerCount) { this.answerCount = answerCount; }
    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerNickName() { return ownerNickName; }
    public void setOwnerNickName(String ownerNickName) { this.ownerNickName = ownerNickName; }
    public Date getPublishAt() { return publishAt; }
    public void setPublishAt(Date publishAt) { this.publishAt = publishAt; }
    public String getMailNotify() { return mailNotify; }
    public void setMailNotify(String mailNotify) { this.mailNotify = mailNotify; }
    public String getMailNotifyTo() { return mailNotifyTo; }
    public void setMailNotifyTo(String mailNotifyTo) { this.mailNotifyTo = mailNotifyTo; }
    public Integer getRemindHours() { return remindHours; }
    public void setRemindHours(Integer remindHours) { this.remindHours = remindHours; }
    public String getRemindSent() { return remindSent; }
    public void setRemindSent(String remindSent) { this.remindSent = remindSent; }
    public String getRemindMail() { return remindMail; }
    public void setRemindMail(String remindMail) { this.remindMail = remindMail; }
}
