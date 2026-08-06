package com.ruoyi.biz.domain;

import java.io.Serializable;

public class BizQueryRow implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long rowId;
    private Long queryId;
    private Integer rowNo;
    /** JSON string for jsonb column */
    private String rowData;

    public Long getRowId() { return rowId; }
    public void setRowId(Long rowId) { this.rowId = rowId; }
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getRowData() { return rowData; }
    public void setRowData(String rowData) { this.rowData = rowData; }
}
