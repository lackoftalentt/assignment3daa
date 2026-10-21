#!/usr/bin/env python3
"""Render the submission's controlled Markdown report into a printable PDF.

Optional dependency: reportlab. The Markdown report remains a complete deliverable.
Supports paragraphs, headings, tables, images, inline code/bold, and explicit page breaks.
"""
import html
from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Image, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle


def main():
    root = Path(__file__).resolve().parents[1]
    candidates = [Path("/usr/share/fonts/TTF"), Path("/usr/share/fonts/truetype/dejavu")]
    fonts = next((p for p in candidates if (p / "DejaVuSans.ttf").exists()), None)
    if fonts is None:
        raise SystemExit("Install DejaVu Sans fonts to render the PDF, or submit REPORT.md.")
    for name, file in [("Report", "DejaVuSans.ttf"), ("ReportBold", "DejaVuSans-Bold.ttf"), ("ReportMono", "DejaVuSansMono.ttf")]:
        pdfmetrics.registerFont(TTFont(name, str(fonts / file)))
    pdfmetrics.registerFontFamily("Report", normal="Report", bold="ReportBold", italic="Report", boldItalic="ReportBold")
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name="BodyReport", fontName="Report", fontSize=8.7, leading=12, spaceAfter=6, alignment=TA_LEFT))
    styles.add(ParagraphStyle(name="TitleReport", fontName="ReportBold", fontSize=17, leading=21, spaceAfter=10))
    styles.add(ParagraphStyle(name="HeadingReport", fontName="ReportBold", fontSize=12, leading=16, spaceAfter=8))
    styles.add(ParagraphStyle(name="CellReport", fontName="Report", fontSize=7.5, leading=10))
    styles.add(ParagraphStyle(name="CaptionReport", fontName="Report", fontSize=8.2, leading=11, spaceAfter=6))

    def inline(text):
        text = html.escape(text.strip())
        text = re.sub(r"`([^`]+)`", r'<font name="ReportMono">\1</font>', text)
        return re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)

    width = A4[0] - 80
    story = []
    lines = (root / "REPORT.md").read_text(encoding="utf-8").splitlines()
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1; continue
        if line == "<!-- PAGEBREAK -->":
            story.append(PageBreak()); i += 1; continue
        if line.startswith("# ") or line.startswith("## "):
            level = "TitleReport" if line.startswith("# ") else "HeadingReport"
            story.append(Paragraph(inline(line.lstrip("# ")), styles[level])); i += 1; continue
        if line.startswith("!["):
            match = re.fullmatch(r"!\[.*?\]\((.*?)\)", line)
            picture = Image(str(root / match.group(1)))
            picture.drawHeight *= width / picture.drawWidth
            picture.drawWidth = width
            story.append(picture); story.append(Spacer(1, 3)); i += 1; continue
        if line.startswith("|"):
            table_rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", c) for c in cells):
                    table_rows.append([Paragraph(inline(c), styles["CellReport"]) for c in cells])
                i += 1
            columns = len(table_rows[0])
            proportions = {3: [0.41, 0.29, 0.30], 4: [0.20, 0.24, 0.14, 0.42]}.get(columns, [1 / columns] * columns)
            table = Table(table_rows, colWidths=[width * p for p in proportions], repeatRows=1, hAlign="LEFT")
            table.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e8edf2")),
                ("GRID", (0, 0), (-1, -1), .35, colors.HexColor("#c3cbd3")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]))
            story.extend([table, Spacer(1, 7)]); continue
        paragraph = []
        while i < len(lines) and lines[i].strip() and not lines[i].startswith(("#", "|", "![", "<!--")):
            paragraph.append(inline(lines[i]) + ("<br/>" if lines[i].endswith("  ") else " "))
            i += 1
        story.append(Paragraph("".join(paragraph), styles["BodyReport"]))

    def footer(canvas, document):
        canvas.saveState()
        canvas.setFont("Report", 8)
        canvas.setFillColor(colors.HexColor("#526373"))
        canvas.drawString(40, 25, "Beibarys Balykbayev | SE-2516 | DAA Assignment 3")
        canvas.drawRightString(A4[0] - 40, 25, str(document.page))
        canvas.restoreState()

    document = SimpleDocTemplate(str(root / "REPORT.pdf"), pagesize=A4,
                                 rightMargin=40, leftMargin=40, topMargin=36, bottomMargin=42,
                                 title="DAA Assignment 3 — Graphs", author="Beibarys Balykbayev, SE-2516")
    document.build(story, onFirstPage=footer, onLaterPages=footer)
    print(root / "REPORT.pdf")


if __name__ == "__main__":
    main()
