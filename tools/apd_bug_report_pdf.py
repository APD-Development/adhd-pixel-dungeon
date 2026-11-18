#!/usr/bin/env python3
"""Render the maintained APD bug report, with optional build evidence, as a PDF."""
import argparse
from datetime import datetime
from pathlib import Path
from xml.sax.saxutils import escape
from zoneinfo import ZoneInfo

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, PageBreak, LongTable, TableStyle


def markdown_blocks(text):
    # Keep headings separate even when the source omits blank lines around them.
    lines = []
    for line in text.strip().splitlines():
        if line.startswith(("# ", "## ")):
            lines.extend(("", line, ""))
        else:
            lines.append(line)
    return [block.strip() for block in "\n".join(lines).split("\n\n") if block.strip()]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('source', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--verification', type=Path)
    parser.add_argument('--appendix', type=Path)
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle('ReportBody', fontName='Helvetica', fontSize=10,
                              leading=14, spaceAfter=8, alignment=TA_LEFT,
                              textColor=colors.HexColor('#253044')))
    styles['Heading1'].textColor = colors.HexColor('#173d65')
    styles['Heading2'].textColor = colors.HexColor('#173d65')
    story = []
    prepared = datetime.now(ZoneInfo('America/New_York')).strftime('%B %d, %Y at %I:%M %p %Z')
    story.append(Paragraph('ADHD Pixel Dungeon', styles['Title']))
    story.append(Paragraph('Bug fixes, causes and possible side effects', styles['Heading1']))
    story.append(Paragraph('Private development report. Prepared ' + escape(prepared), styles['ReportBody']))
    if args.verification:
        story.append(Paragraph('Build verification', styles['Heading2']))
        for paragraph in args.verification.read_text().split('\n\n'):
            story.append(Paragraph(escape(paragraph).replace('\n', '<br/>'), styles['ReportBody']))
    def append_markdown(path):
        for block in markdown_blocks(path.read_text()):
            if block.startswith('# '):
                continue
            if block.startswith('## '):
                story.append(Paragraph(escape(block[3:]), styles['Heading2']))
            elif block.startswith('|'):
                rows = []
                for line in block.splitlines():
                    cells = [c.strip() for c in line.strip('|').split('|')]
                    if all(c and set(c) <= set('-: ') for c in cells):
                        continue
                    rows.append([Paragraph(escape(c), styles['ReportBody']) for c in cells])
                table = LongTable(rows, colWidths=[7.2 * inch / len(rows[0])] * len(rows[0]), repeatRows=1)
                table.setStyle(TableStyle([
                    ('VALIGN', (0, 0), (-1, -1), 'TOP'),
                    ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#e8eef5')),
                    ('LINEBELOW', (0, 0), (-1, -1), 0.3, colors.HexColor('#ccd5df')),
                ]))
                story.append(table)
            else:
                story.append(Paragraph(escape(block).replace('\n', '<br/>'), styles['ReportBody']))
    append_markdown(args.source)
    if args.appendix:
        story.append(PageBreak())
        story.append(Paragraph('Development records and commits', styles['Heading1']))
        append_markdown(args.appendix)
    def footer(canvas, doc):
        canvas.saveState()
        canvas.setFont('Helvetica', 8)
        canvas.setFillColor(colors.HexColor('#536479'))
        canvas.drawString(0.65 * inch, 0.4 * inch, 'APD private test • Automated checks are separate from physical-phone testing')
        canvas.drawRightString(7.85 * inch, 0.4 * inch, str(doc.page))
        canvas.restoreState()
    SimpleDocTemplate(str(args.output), pagesize=(8.5 * inch, 11 * inch),
                      rightMargin=0.65 * inch, leftMargin=0.65 * inch,
                      topMargin=0.65 * inch, bottomMargin=0.65 * inch,
                      title='APD bug fixes, causes and risks',
                      author='APD development').build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == '__main__':
    main()
