package com.ruoyi.biz.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Query project entity
 */
public class BizQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long queryId;
    private String queryName;
    private String queryDesc;
    private String publicCode;
    /** 0 draft 1 published 2 offline */
    private String status;
    private String sourceFile;
    private String sheetName;
    private Integer rowCount;
    private String accessPwd;
    private Long viewCount;
    private Long searchCount;
    private Long createUserId;
    /** Creator department for data scope */
    private Long deptId;
    /** 0 ready 1 parsing 2 failed */
    private String parseStatus;
    private String parseMsg;
    /** 1 require captcha on open search */
    private String needCaptcha;
    /** max open searches per IP per day; 0 unlimited */
    private Integer dailyLimit;
    /** Display only: creator login name (joined) */
    private String ownerName;
    /** Display only: creator nickname (joined) */
    private String ownerNickName;

    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public String getQueryName() { return queryName; }
    public void setQueryName(String queryName) { this.queryName = queryName; }
    public String getQueryDesc() { return queryDesc; }
    public void setQueryDesc(String queryDesc) { this.queryDesc = queryDesc; }
    public String getPublicCode() { return publicCode; }
    public void setPublicCode(String publicCode) { this.publicCode = publicCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getAccessPwd() { return accessPwd; }
    public void setAccessPwd(String accessPwd) { this.accessPwd = accessPwd; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public Long getSearchCount() { return searchCount; }
    public void setSearchCount(Long searchCount) { this.searchCount = searchCount; }
    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getParseStatus() { return parseStatus; }
    public void setParseStatus(String parseStatus) { this.parseStatus = parseStatus; }
    public String getParseMsg() { return parseMsg; }
    public void setParseMsg(String parseMsg) { this.parseMsg = parseMsg; }
    public String getNeedCaptcha() { return needCaptcha; }
    public void setNeedCaptcha(String needCaptcha) { this.needCaptcha = needCaptcha; }
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerNickName() { return ownerNickName; }
    public void setOwnerNickName(String ownerNickName) { this.ownerNickName = ownerNickName; }
}
