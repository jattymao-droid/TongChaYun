package com.ruoyi.biz.utils;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizQueryRow;
import com.ruoyi.common.utils.StringUtils;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Scorecard-style PDF export: one page per person.
 */
public final class BizQueryPdfHelper
{
    private BizQueryPdfHelper() {}

    public static void writeScorecardPdf(String queryName, List<BizQueryField> fields, List<BizQueryRow> rows,
        HttpServletResponse response) throws Exception
    {
        String title = StringUtils.isEmpty(queryName) ? "\u67e5\u8be2\u7ed3\u679c" : queryName;
        String fileName = URLEncoder.encode(title + ".pdf", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        writeScorecardPdf(title, fields, rows, response.getOutputStream());
    }

    public static void writeScorecardPdf(String title, List<BizQueryField> fields, List<BizQueryRow> rows,
        OutputStream out) throws Exception
    {
        BaseFont bf = resolveChineseFont();
        Font titleFont = new Font(bf, 18, Font.BOLD);
        Font labelFont = new Font(bf, 11, Font.NORMAL);
        Font valueFont = new Font(bf, 12, Font.BOLD);
        Font tipFont = new Font(bf, 9, Font.NORMAL);

        Document doc = new Document(PageSize.A4, 48, 48, 56, 48);
        PdfWriter.getInstance(doc, out);
        doc.open();
        try
        {
            if (rows == null || rows.isEmpty())
            {
                doc.add(new Paragraph(title, titleFont));
                doc.add(new Paragraph("\u6682\u65e0\u6570\u636e", labelFont));
                return;
            }
            int total = rows.size();
            for (int i = 0; i < total; i++)
            {
                BizQueryRow row = rows.get(i);
                if (i > 0)
                {
                    doc.newPage();
                }
                Paragraph head = new Paragraph(title, titleFont);
                head.setAlignment(Element.ALIGN_CENTER);
                head.setSpacingAfter(6f);
                doc.add(head);

                Paragraph sub = new Paragraph("\u6210\u7ee9\u5355 / \u7ed3\u679c\u51ed\u8bc1  \u00b7  " + (i + 1) + " / " + total, tipFont);
                sub.setAlignment(Element.ALIGN_CENTER);
                sub.setSpacingAfter(18f);
                doc.add(sub);

                Map<String, String> map = parseRow(row);
                PdfPTable table = new PdfPTable(new float[] { 0.32f, 0.68f });
                table.setWidthPercentage(100f);
                table.setSpacingBefore(8f);

                if (fields == null || fields.isEmpty())
                {
                    addRow(table, "\u6570\u636e", row == null ? "" : StringUtils.nvl(row.getRowData(), ""), labelFont, valueFont);
                }
                else
                {
                    for (BizQueryField f : fields)
                    {
                        if (f == null || StringUtils.isEmpty(f.getFieldKey()))
                        {
                            continue;
                        }
                        String label = StringUtils.nvl(f.getFieldLabel(), f.getFieldName());
                        if (StringUtils.isEmpty(label))
                        {
                            label = f.getFieldKey();
                        }
                        String val = map == null ? "" : map.get(f.getFieldKey());
                        addRow(table, label, val == null ? "" : val, labelFont, valueFont);
                    }
                }
                doc.add(table);

                Paragraph foot = new Paragraph("\u901a\u67e5\u4e91 \u00b7 \u7531\u7cfb\u7edf\u81ea\u52a8\u751f\u6210", tipFont);
                foot.setAlignment(Element.ALIGN_CENTER);
                foot.setSpacingBefore(28f);
                doc.add(foot);
            }
        }
        finally
        {
            doc.close();
        }
    }

    private static void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont)
    {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setPadding(10f);
        c1.setBackgroundColor(new java.awt.Color(248, 250, 252));
        PdfPCell c2 = new PdfPCell(new Phrase(value == null ? "" : value, valueFont));
        c2.setPadding(10f);
        table.addCell(c1);
        table.addCell(c2);
    }

    private static Map<String, String> parseRow(BizQueryRow row)
    {
        if (row == null || StringUtils.isEmpty(row.getRowData()))
        {
            return null;
        }
        try
        {
            return JSON.parseObject(row.getRowData(), new TypeReference<Map<String, String>>() {});
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static BaseFont resolveChineseFont() throws Exception
    {
        String[][] candidates = new String[][] {
            { "STSong-Light", "UniGB-UCS2-H" },
            { "STSongStd-Light", "UniGB-UCS2-H" }
        };
        for (String[] c : candidates)
        {
            try
            {
                return BaseFont.createFont(c[0], c[1], BaseFont.NOT_EMBEDDED);
            }
            catch (Exception ignored)
            {
            }
        }
        String[] paths = new String[] {
            "/System/Library/Fonts/PingFang.ttc,0",
            "/System/Library/Fonts/STHeiti Light.ttc,0",
            "/Library/Fonts/Arial Unicode.ttf",
            "C:\\\\Windows\\\\Fonts\\\\msyh.ttc,0",
            "C:\\\\Windows\\\\Fonts\\\\simsun.ttc,0",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0"
        };
        for (String path : paths)
        {
            try
            {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            catch (Exception ignored)
            {
            }
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }
}
