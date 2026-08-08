package com.ruoyi.biz.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Survey co-admin
 */
public class BizSurveyAdmin implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long surveyId;
    private Long userId;
    private String createBy;
    private Date createTime;

    /** join */
    private String userName;
    private String nickName;
    private String phonenumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getPhonenumber() { return phonenumber; }
    public void setPhonenumber(String phonenumber) { this.phonenumber = phonenumber; }
}
