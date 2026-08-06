package com.ruoyi.biz.domain.vo;

/**
 * Admin view: system user with query/survey project counts.
 */
public class BizUserProjectVo
{
    private Long userId;
    private String userName;
    private String nickName;
    private String status;
    private Long deptId;
    private String deptName;
    private Long queryCount;
    private Long surveyCount;
    private Long queryPublished;
    private Long surveyPublished;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Long getQueryCount() { return queryCount; }
    public void setQueryCount(Long queryCount) { this.queryCount = queryCount; }
    public Long getSurveyCount() { return surveyCount; }
    public void setSurveyCount(Long surveyCount) { this.surveyCount = surveyCount; }
    public Long getQueryPublished() { return queryPublished; }
    public void setQueryPublished(Long queryPublished) { this.queryPublished = queryPublished; }
    public Long getSurveyPublished() { return surveyPublished; }
    public void setSurveyPublished(Long surveyPublished) { this.surveyPublished = surveyPublished; }
}
