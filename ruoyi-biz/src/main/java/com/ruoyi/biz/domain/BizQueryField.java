package com.ruoyi.biz.domain;

import java.io.Serializable;

public class BizQueryField implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long fieldId;
    private Long queryId;
    private String fieldKey;
    private String fieldName;
    private String fieldLabel;
    private String dataType;
    private String isQuery;
    private String queryType;
    private String htmlType;
    private String isList;
    private String isSortable;
    private String dictOptions;
    private Integer sort;
    private Integer width;
    /** 1 required query condition 0 optional */
    private String isRequired;
    /** none / phone / idcard / name / email */
    private String maskType;

    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getIsQuery() { return isQuery; }
    public void setIsQuery(String isQuery) { this.isQuery = isQuery; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public String getHtmlType() { return htmlType; }
    public void setHtmlType(String htmlType) { this.htmlType = htmlType; }
    public String getIsList() { return isList; }
    public void setIsList(String isList) { this.isList = isList; }
    public String getIsSortable() { return isSortable; }
    public void setIsSortable(String isSortable) { this.isSortable = isSortable; }
    public String getDictOptions() { return dictOptions; }
    public void setDictOptions(String dictOptions) { this.dictOptions = dictOptions; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public String getIsRequired() { return isRequired; }
    public void setIsRequired(String isRequired) { this.isRequired = isRequired; }
    public String getMaskType() { return maskType; }
    public void setMaskType(String maskType) { this.maskType = maskType; }
}
