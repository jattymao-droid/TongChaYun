package com.ruoyi.biz.domain;

import java.io.Serializable;

/** Dataset raw row: biz_query_dataset_row */
public class BizQueryDatasetRow implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long rowId;
    private Long datasetId;
    private Long queryId;
    private Integer rowNo;
    private String rowData;

    public Long getRowId() { return rowId; }
    public void setRowId(Long rowId) { this.rowId = rowId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getRowData() { return rowData; }
    public void setRowData(String rowData) { this.rowData = rowData; }
}
