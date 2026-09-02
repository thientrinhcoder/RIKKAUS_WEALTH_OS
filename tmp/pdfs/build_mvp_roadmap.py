from __future__ import annotations

import os
from math import ceil

from reportlab.lib import colors
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = "/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS"
OUTPUT = os.path.join(ROOT, "output/pdf/RIKKAUS_WEALTH_OS_MVP_ROADMAP.pdf")

PAGE_W, PAGE_H = landscape(A4)
MARGIN_X = 16 * mm
MARGIN_TOP = 17 * mm
MARGIN_BOTTOM = 15 * mm
CONTENT_W = PAGE_W - 2 * MARGIN_X
CONTENT_H = PAGE_H - MARGIN_TOP - MARGIN_BOTTOM

NAVY = HexColor("#10233F")
BLUE = HexColor("#2166F3")
CYAN = HexColor("#25B6D2")
MINT = HexColor("#23B38A")
AMBER = HexColor("#F0A93A")
CORAL = HexColor("#E8685B")
INK = HexColor("#1D2939")
MUTED = HexColor("#667085")
LIGHT = HexColor("#F5F7FB")
LINE = HexColor("#D9E0EA")
WHITE = colors.white

FONT_DIR = "/System/Library/Fonts/Supplemental"
pdfmetrics.registerFont(TTFont("ArialVN", os.path.join(FONT_DIR, "Arial.ttf")))
pdfmetrics.registerFont(TTFont("ArialVNBold", os.path.join(FONT_DIR, "Arial Bold.ttf")))
pdfmetrics.registerFont(TTFont("ArialVNItalic", os.path.join(FONT_DIR, "Arial Italic.ttf")))


def ptext(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


styles = getSampleStyleSheet()
styles.add(
    ParagraphStyle(
        "Kicker",
        fontName="ArialVNBold",
        fontSize=7.5,
        leading=9,
        textColor=BLUE,
        spaceAfter=2 * mm,
        uppercase=True,
    )
)
styles.add(
    ParagraphStyle(
        "PageTitle",
        fontName="ArialVNBold",
        fontSize=22,
        leading=25,
        textColor=NAVY,
        spaceAfter=3 * mm,
    )
)
styles.add(
    ParagraphStyle(
        "Lead",
        fontName="ArialVN",
        fontSize=10.5,
        leading=15,
        textColor=MUTED,
        spaceAfter=4 * mm,
    )
)
styles.add(
    ParagraphStyle(
        "H2",
        fontName="ArialVNBold",
        fontSize=13,
        leading=16,
        textColor=NAVY,
        spaceBefore=1 * mm,
        spaceAfter=2.5 * mm,
    )
)
styles.add(
    ParagraphStyle(
        "Body",
        fontName="ArialVN",
        fontSize=8.4,
        leading=11.7,
        textColor=INK,
        spaceAfter=1.5 * mm,
    )
)
styles.add(
    ParagraphStyle(
        "Small",
        fontName="ArialVN",
        fontSize=7.2,
        leading=9.3,
        textColor=MUTED,
    )
)
styles.add(
    ParagraphStyle(
        "CardTitle",
        fontName="ArialVNBold",
        fontSize=9.2,
        leading=11,
        textColor=NAVY,
        spaceAfter=1.3 * mm,
    )
)
styles.add(
    ParagraphStyle(
        "CardBody",
        fontName="ArialVN",
        fontSize=7.5,
        leading=9.8,
        textColor=INK,
    )
)
styles.add(
    ParagraphStyle(
        "TableHead",
        fontName="ArialVNBold",
        fontSize=7.2,
        leading=8.6,
        textColor=WHITE,
        alignment=TA_LEFT,
    )
)
styles.add(
    ParagraphStyle(
        "TableBody",
        fontName="ArialVN",
        fontSize=7,
        leading=9,
        textColor=INK,
    )
)
styles.add(
    ParagraphStyle(
        "TableBodyBold",
        fontName="ArialVNBold",
        fontSize=7,
        leading=9,
        textColor=NAVY,
    )
)
styles.add(
    ParagraphStyle(
        "Quote",
        fontName="ArialVNBold",
        fontSize=14,
        leading=18,
        textColor=NAVY,
        alignment=TA_CENTER,
    )
)


def para(text: str, style: str = "Body") -> Paragraph:
    return Paragraph(text, styles[style])


def bullet(text: str, color: str = "#2166F3", size: float = 8.2) -> Paragraph:
    st = ParagraphStyle(
        f"bullet-{color}-{size}",
        parent=styles["Body"],
        fontSize=size,
        leading=size * 1.38,
        leftIndent=4 * mm,
        firstLineIndent=-3.2 * mm,
        spaceAfter=1.1 * mm,
    )
    return Paragraph(f'<font color="{color}">●</font>&nbsp;&nbsp;{text}', st)


class ColorBar(Flowable):
    def __init__(self, width=CONTENT_W, height=2 * mm, color=BLUE):
        super().__init__()
        self.width = width
        self.height = height
        self.color = color

    def draw(self):
        self.canv.setFillColor(self.color)
        self.canv.roundRect(0, 0, self.width, self.height, self.height / 2, fill=1, stroke=0)


class ProductFlow(Flowable):
    def __init__(self, width=CONTENT_W, height=46 * mm):
        super().__init__()
        self.width = width
        self.height = height

    def draw(self):
        c = self.canv
        labels = [
            ("Tạo tài khoản", BLUE),
            ("Nhập tài sản", CYAN),
            ("Nhập nợ", CORAL),
            ("Nhập dòng tiền", AMBER),
            ("Xem dashboard", MINT),
            ("Mục tiêu & gợi ý", NAVY),
        ]
        gap = 5 * mm
        box_w = (self.width - gap * (len(labels) - 1)) / len(labels)
        box_h = 22 * mm
        y = 10 * mm
        c.setLineWidth(1.2)
        for i, (label, color) in enumerate(labels):
            x = i * (box_w + gap)
            if i < len(labels) - 1:
                c.setStrokeColor(LINE)
                c.line(x + box_w, y + box_h / 2, x + box_w + gap - 1.5 * mm, y + box_h / 2)
                c.setFillColor(LINE)
                c.setStrokeColor(LINE)
                c.wedge(x + box_w + gap - 3 * mm, y + box_h / 2 - 1.5 * mm,
                        x + box_w + gap, y + box_h / 2 + 1.5 * mm, 315, 90, fill=1, stroke=0)
            c.setFillColor(color)
            c.roundRect(x, y, box_w, box_h, 3 * mm, fill=1, stroke=0)
            c.setFillColor(WHITE)
            c.setFont("ArialVNBold", 8.5)
            words = label.split()
            if len(words) <= 2:
                lines = [label]
            else:
                mid = ceil(len(words) / 2)
                lines = [" ".join(words[:mid]), " ".join(words[mid:])]
            for j, line in enumerate(lines):
                c.drawCentredString(x + box_w / 2, y + box_h / 2 + (3 - j * 10), line)
        c.setFillColor(MUTED)
        c.setFont("ArialVN", 7)
        c.drawString(0, 1 * mm, "Luồng trải nghiệm cốt lõi - dữ liệu được nhập thủ công, kết quả phải giải thích được")


class Timeline(Flowable):
    def __init__(self, width=CONTENT_W, height=67 * mm):
        super().__init__()
        self.width = width
        self.height = height

    def draw(self):
        c = self.canv
        phases = [
            ("MVP 1A", "Wealth Snapshot", "Tài sản, nợ, net worth", BLUE),
            ("MVP 1B", "Financial Health", "Dòng tiền, thanh khoản", CYAN),
            ("MVP 1C", "Goals & Advisor", "Mục tiêu, gợi ý quy tắc", MINT),
            ("MVP 1D", "Pilot Readiness", "Onboarding, feedback, polish", AMBER),
        ]
        gap = 7 * mm
        card_w = (self.width - gap * 3) / 4
        line_y = 42 * mm
        c.setStrokeColor(LINE)
        c.setLineWidth(3)
        c.line(card_w / 2, line_y, self.width - card_w / 2, line_y)
        for i, (phase, title, result, color) in enumerate(phases):
            x = i * (card_w + gap)
            cx = x + card_w / 2
            c.setFillColor(color)
            c.circle(cx, line_y, 4.2 * mm, fill=1, stroke=0)
            c.setFillColor(WHITE)
            c.setFont("ArialVNBold", 8)
            c.drawCentredString(cx, line_y - 2.5, str(i + 1))
            c.setFillColor(LIGHT)
            c.roundRect(x, 2 * mm, card_w, 29 * mm, 3 * mm, fill=1, stroke=0)
            c.setFillColor(color)
            c.roundRect(x, 24 * mm, card_w, 7 * mm, 3 * mm, fill=1, stroke=0)
            c.rect(x, 24 * mm, card_w, 3.5 * mm, fill=1, stroke=0)
            c.setFillColor(WHITE)
            c.setFont("ArialVNBold", 8)
            c.drawCentredString(cx, 26 * mm, phase)
            c.setFillColor(NAVY)
            c.setFont("ArialVNBold", 8.7)
            c.drawCentredString(cx, 16.5 * mm, title)
            c.setFillColor(MUTED)
            c.setFont("ArialVN", 7.1)
            c.drawCentredString(cx, 9.5 * mm, result)
            if i < 3:
                c.setFillColor(LINE)
                c.setFont("ArialVNBold", 11)
                c.drawCentredString(x + card_w + gap / 2, 15 * mm, "+")
        c.setFillColor(NAVY)
        c.setFont("ArialVNBold", 8.5)
        c.drawString(0, 58 * mm, "Thứ tự giao hàng theo giá trị có thể kiểm chứng")
        c.setFillColor(MUTED)
        c.setFont("ArialVN", 7.2)
        c.drawString(0, 53 * mm, "Mỗi increment phải dùng được độc lập và thu feedback trước khi mở rộng.")


class LayerDiagram(Flowable):
    def __init__(self, width=CONTENT_W, height=63 * mm):
        super().__init__()
        self.width = width
        self.height = height

    def draw(self):
        c = self.canv
        rows = [
            ("TRẢI NGHIỆM", "Dashboard  •  Financial Health  •  Goals  •  Basic Advisor", NAVY),
            ("TÍNH TOÁN", "Net worth  •  Allocation  •  Liquidity  •  Cash flow  •  Goal gap", BLUE),
            ("DỮ LIỆU CỐT LÕI", "Asset  •  Valuation  •  Liability  •  Cash Flow  •  Goal", CYAN),
            ("NGUỒN DỮ LIỆU", "Nhập tay  •  VND/USD  •  Tỷ giá người dùng nhập", MINT),
        ]
        row_h = 12 * mm
        gap = 2.5 * mm
        label_w = 42 * mm
        for i, (label, content, color) in enumerate(rows):
            y = self.height - (i + 1) * row_h - i * gap
            c.setFillColor(LIGHT)
            c.roundRect(0, y, self.width, row_h, 2.5 * mm, fill=1, stroke=0)
            c.setFillColor(color)
            c.roundRect(0, y, label_w, row_h, 2.5 * mm, fill=1, stroke=0)
            c.rect(label_w - 2.5 * mm, y, 2.5 * mm, row_h, fill=1, stroke=0)
            c.setFillColor(WHITE)
            c.setFont("ArialVNBold", 7.5)
            c.drawString(5 * mm, y + 4.5 * mm, label)
            c.setFillColor(INK)
            c.setFont("ArialVN", 8)
            c.drawString(label_w + 6 * mm, y + 4.5 * mm, content)


def card(title: str, lines: list[str], accent=BLUE, width=None):
    body = [para(title, "CardTitle")]
    body.extend(bullet(line, accent.hexval(), 7.4) for line in lines)
    table = Table([[body]], colWidths=[width] if width else None, hAlign="LEFT")
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), LIGHT),
                ("BOX", (0, 0), (-1, -1), 0.7, LINE),
                ("LINEBEFORE", (0, 0), (0, -1), 4, accent),
                ("LEFTPADDING", (0, 0), (-1, -1), 4 * mm),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4 * mm),
                ("TOPPADDING", (0, 0), (-1, -1), 3.5 * mm),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3 * mm),
            ]
        )
    )
    return table


def data_table(headers, rows, widths, header_color=NAVY, font_size=7):
    head = [para(ptext(h), "TableHead") for h in headers]
    body = []
    for row in rows:
        cells = []
        for i, cell in enumerate(row):
            style = "TableBodyBold" if i == 0 else "TableBody"
            cells.append(para(ptext(str(cell)), style))
        body.append(cells)
    t = Table([head] + body, colWidths=widths, repeatRows=1, hAlign="LEFT")
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), header_color),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("GRID", (0, 0), (-1, -1), 0.45, LINE),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, LIGHT]),
                ("LEFTPADDING", (0, 0), (-1, -1), 2.5 * mm),
                ("RIGHTPADDING", (0, 0), (-1, -1), 2.5 * mm),
                ("TOPPADDING", (0, 0), (-1, -1), 2 * mm),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 2 * mm),
            ]
        )
    )
    return t


def page_header_footer(c: canvas.Canvas, doc):
    c.saveState()
    page = c.getPageNumber()
    c.setStrokeColor(LINE)
    c.setLineWidth(0.5)
    c.line(MARGIN_X, 10.5 * mm, PAGE_W - MARGIN_X, 10.5 * mm)
    c.setFont("ArialVN", 6.8)
    c.setFillColor(MUTED)
    c.drawString(MARGIN_X, 6.5 * mm, "RIKKAUS WEALTH OS  •  MVP PRODUCT ROADMAP")
    c.drawRightString(PAGE_W - MARGIN_X, 6.5 * mm, f"{page:02d}")
    c.restoreState()


def cover_page(c: canvas.Canvas, doc):
    c.saveState()
    c.setFillColor(NAVY)
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    c.setFillColor(BLUE)
    c.circle(PAGE_W - 35 * mm, PAGE_H - 20 * mm, 55 * mm, fill=1, stroke=0)
    c.setFillColor(CYAN)
    c.circle(PAGE_W - 5 * mm, 15 * mm, 42 * mm, fill=1, stroke=0)
    c.setFillColor(MINT)
    c.roundRect(18 * mm, PAGE_H - 32 * mm, 42 * mm, 8 * mm, 4 * mm, fill=1, stroke=0)
    c.setFillColor(WHITE)
    c.setFont("ArialVNBold", 8)
    c.drawCentredString(39 * mm, PAGE_H - 29.2 * mm, "PRODUCT BLUEPRINT")
    c.setFont("ArialVNBold", 32)
    c.drawString(18 * mm, PAGE_H - 65 * mm, "RIKKAUS")
    c.drawString(18 * mm, PAGE_H - 82 * mm, "WEALTH OS")
    c.setFont("ArialVN", 15)
    c.setFillColor(HexColor("#BFD0EE"))
    c.drawString(18 * mm, PAGE_H - 100 * mm, "MVP scope, sub-phases & functional roadmap")
    c.setFillColor(WHITE)
    c.setFont("ArialVNBold", 10)
    c.drawString(18 * mm, 38 * mm, "VIỆT NAM  •  CÁ NHÂN  •  MANUAL-FIRST")
    c.setFont("ArialVN", 8)
    c.setFillColor(HexColor("#BFD0EE"))
    c.drawString(18 * mm, 28 * mm, "Pilot baseline  •  2-5 người dùng  •  02.09.2026")
    c.restoreState()


doc = BaseDocTemplate(
    OUTPUT,
    pagesize=landscape(A4),
    leftMargin=MARGIN_X,
    rightMargin=MARGIN_X,
    topMargin=MARGIN_TOP,
    bottomMargin=MARGIN_BOTTOM,
    title="Rikkaus Wealth OS - MVP Product Roadmap",
    author="Rikkaus Wealth OS",
    subject="MVP scope, sub-phases, functional timeline and pilot plan",
)

cover_frame = Frame(0, 0, PAGE_W, PAGE_H, id="cover", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
body_frame = Frame(
    MARGIN_X,
    MARGIN_BOTTOM,
    CONTENT_W,
    CONTENT_H,
    id="body",
    leftPadding=0,
    rightPadding=0,
    topPadding=0,
    bottomPadding=0,
)
doc.addPageTemplates(
    [
        PageTemplate(id="Cover", frames=[cover_frame], onPage=cover_page, autoNextPageTemplate="Body"),
        PageTemplate(id="Body", frames=[body_frame], onPage=page_header_footer),
    ]
)

story = []

# Cover page: the canvas callback renders it; one spacer forces a page.
story.extend([Spacer(1, PAGE_H - 5 * mm), PageBreak()])

# Page 2 - Executive summary
story.extend(
    [
        para("01  •  PRODUCT DEFINITION", "Kicker"),
        para("MVP trong một trang", "PageTitle"),
        para(
            "Một ứng dụng quản lý tài sản cá nhân tại Việt Nam: người dùng nhập tay tài sản, nợ và dòng tiền; hệ thống tổng hợp net worth, trực quan hóa sức khỏe tài chính và đưa ra nhận định cơ bản có thể giải thích.",
            "Lead",
        ),
        ColorBar(color=BLUE),
        Spacer(1, 5 * mm),
    ]
)

summary_cards = Table(
    [[
        card("Đối tượng", ["Cá nhân tại Việt Nam", "Pilot 2-5 người dùng", "Mỗi người có dữ liệu độc lập"], BLUE, 76 * mm),
        card("Cách vận hành", ["Nhập tay trước", "VND và USD", "Tỷ giá, định giá do người dùng nhập"], CYAN, 76 * mm),
        card("Giá trị cốt lõi", ["Biết tài sản ròng", "Hiểu thanh khoản & dòng tiền", "Nhận biết điều cần chú ý"], MINT, 76 * mm),
    ]],
    colWidths=[80.5 * mm] * 3,
)
summary_cards.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 0), ("RIGHTPADDING", (0, 0), (-1, -1), 4.5 * mm)]))
story.extend([summary_cards, Spacer(1, 6 * mm), para("8 câu hỏi MVP phải trả lời", "H2")])
questions = [
    "Tổng tài sản là bao nhiêu?",
    "Tổng nợ là bao nhiêu?",
    "Net worth hiện tại là bao nhiêu?",
    "Tài sản tập trung ở đâu?",
    "Thanh khoản có đủ không?",
    "Thu nhập có cao hơn chi tiêu?",
    "Điều gì cần chú ý ngay?",
    "Mục tiêu có đúng tiến độ?",
]
q_cells = []
for i, q in enumerate(questions, 1):
    q_cells.append([para(f'<font color="#2166F3"><b>{i:02d}</b></font>&nbsp;&nbsp;{q}', "CardBody")])
q_table = Table([q_cells[:4], q_cells[4:]], colWidths=[CONTENT_W / 4] * 4, rowHeights=[14 * mm, 14 * mm])
q_table.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), LIGHT), ("GRID", (0, 0), (-1, -1), 0.6, LINE), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("LEFTPADDING", (0, 0), (-1, -1), 4 * mm), ("RIGHTPADDING", (0, 0), (-1, -1), 3 * mm)]))
story.extend([q_table, PageBreak()])

# Page 3 - Flow and scope
story.extend(
    [
        para("02  •  EXPERIENCE & SCOPE", "Kicker"),
        para("Trải nghiệm cốt lõi", "PageTitle"),
        para("MVP ưu tiên một vòng lặp ngắn: nhập dữ liệu có chủ đích, thấy kết quả ngay và hiểu được nguyên nhân phía sau từng chỉ số.", "Lead"),
        ProductFlow(),
        Spacer(1, 5 * mm),
        para("Kiến trúc chức năng MVP", "H2"),
        LayerDiagram(),
        Spacer(1, 4 * mm),
        Table(
            [[
                para("<b>Nguyên tắc:</b> snapshot trước sophistication; dữ liệu real-time không phải điều kiện để tạo giá trị.", "CardBody"),
                para("<b>Ranh giới:</b> không household, không AI, không external API, không document vault trong MVP.", "CardBody"),
            ]],
            colWidths=[CONTENT_W / 2] * 2,
            style=TableStyle([("BACKGROUND", (0, 0), (-1, -1), HexColor("#EEF4FF")), ("BOX", (0, 0), (-1, -1), 0.7, HexColor("#B8CDF8")), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 4 * mm), ("RIGHTPADDING", (0, 0), (-1, -1), 4 * mm), ("TOPPADDING", (0, 0), (-1, -1), 3 * mm), ("BOTTOMPADDING", (0, 0), (-1, -1), 3 * mm)]),
        ),
        PageBreak(),
    ]
)

# Page 4 - Roadmap
story.extend(
    [
        para("03  •  DELIVERY ROADMAP", "Kicker"),
        para("Bốn increment có thể kiểm chứng", "PageTitle"),
        para("Không gắn timeline theo tuần khi chưa có ước lượng kỹ thuật. Timeline dưới đây biểu thị thứ tự phụ thuộc và thời điểm mở pilot feedback.", "Lead"),
        Timeline(),
        Spacer(1, 5 * mm),
    ]
)
roadmap_rows = [
    ("MVP 1A", "Wealth Snapshot", "Tài sản + nợ + tỷ giá", "Net worth + allocation + due dates", "Pilot vòng 1"),
    ("MVP 1B", "Financial Health", "Thu/chi + recurring", "Cash flow + liquidity + forecast", "Pilot vòng 2"),
    ("MVP 1C", "Goals & Advisor", "Mục tiêu + threshold rules", "Goal gap + next actions", "Pilot vòng 3"),
    ("MVP 1D", "Pilot Readiness", "Onboarding + validation", "Trải nghiệm lặp lại ổn định", "Pilot hoàn chỉnh"),
]
story.extend([
    data_table(["Increment", "Chủ đề", "Input mới", "Output chính", "Mốc feedback"], roadmap_rows, [28*mm, 42*mm, 54*mm, 72*mm, 42*mm], BLUE),
    Spacer(1, 4 * mm),
    para("Gate chuyển phase: increment trước phải dùng được độc lập, số liệu reconcile và feedback trọng yếu đã được ghi nhận.", "Small"),
    PageBreak(),
])

# Page 5 - 1A
story.extend([
    para("04  •  MVP 1A", "Kicker"),
    para("Wealth Snapshot", "PageTitle"),
    para("Outcome: người dùng nhập tài sản và nợ, sau đó hiểu ngay tài sản ròng và cấu trúc wealth hiện tại.", "Lead"),
])
phase_1a = Table(
    [[
        card("Input", ["VND/USD + tỷ giá tự nhập", "11 nhóm tài sản", "6 nhóm khoản nợ", "Valuation history thủ công"], BLUE, 74*mm),
        card("Dashboard", ["Tổng tài sản / tổng nợ", "Net worth", "Debt-to-asset", "Giá trị tài sản thanh khoản"], CYAN, 74*mm),
        card("Visual & Insight", ["Allocation theo loại", "Allocation theo liquidity", "Top assets", "Nợ sắp đến hạn / dữ liệu cũ"], MINT, 74*mm),
    ]], colWidths=[80.5*mm]*3,
)
phase_1a.setStyle(TableStyle([("VALIGN", (0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(-1,-1),4.5*mm)]))
story.extend([phase_1a, Spacer(1, 5*mm), para("Danh mục dữ liệu", "H2")])
asset_rows = [
    ("Tài sản", "Tiền mặt, ngân hàng, tiết kiệm, chứng khoán, trái phiếu, quỹ, BĐS, cổ phần DN, valuable asset, collectible, khác", "Tên, loại, giá trị, currency, ngày định giá, liquidity"),
    ("Khoản nợ", "Vay ngân hàng, cá nhân, margin, thẻ tín dụng, bảo lãnh, khác", "Dư nợ, lãi suất, đáo hạn, kỳ trả tiếp theo, collateral"),
    ("Định giá", "Một lịch sử định giá cho mỗi asset", "Value, currency, effective date, FX rate, note"),
]
story.extend([
    data_table(["Nhóm", "Phạm vi", "Dữ liệu tối thiểu"], asset_rows, [32*mm, 104*mm, 102*mm], NAVY),
    Spacer(1, 5*mm),
    para("Definition of done", "H2"),
    Table([[bullet("Onboarding snapshot không cần hỗ trợ", "#2166F3"), bullet("USD quy đổi chính xác về VND", "#2166F3"), bullet("Charts reconcile với records", "#2166F3")], [bullet("Edit value tạo valuation history", "#2166F3"), bullet("Cross-user access bị chặn", "#2166F3"), bullet("Insight nêu rõ lý do", "#2166F3")]], colWidths=[CONTENT_W/3]*3, style=TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"), ("LEFTPADDING",(0,0),(-1,-1),0), ("RIGHTPADDING",(0,0),(-1,-1),4*mm)])),
    PageBreak(),
])

# Page 6 - 1B
story.extend([
    para("05  •  MVP 1B", "Kicker"),
    para("Cash Flow & Financial Health", "PageTitle"),
    para("Outcome: người dùng biết dòng tiền và tài sản thanh khoản có đủ đáp ứng chi tiêu, nợ và nghĩa vụ gần hạn hay không.", "Lead"),
])
cash_rows = [
    ("Thu nhập", "Lương, kinh doanh, cho thuê, cổ tức, lãi tiền gửi, trái phiếu/quỹ, khác"),
    ("Chi tiêu", "Sinh hoạt, nhà ở, giáo dục, y tế, bảo hiểm, thuế/phí, trả gốc, trả lãi, khác"),
    ("Tần suất", "Một lần hoặc định kỳ theo tháng, quý, năm"),
    ("Liên kết", "Có thể gắn với một asset hoặc một liability"),
]
health_rows = [
    ("Dòng tiền ròng", "Tổng thu - Tổng chi"),
    ("Quỹ dự phòng", "Tài sản thanh khoản cao / chi phí thiết yếu tháng"),
    ("Gánh nặng trả nợ", "Nghĩa vụ trả nợ định kỳ / thu nhập định kỳ"),
    ("Forecast", "3 tháng từ recurring entries + scheduled liabilities"),
]
two_tables = Table([[data_table(["Dữ liệu", "Phạm vi"], cash_rows, [38*mm, 76*mm], CYAN), data_table(["Chỉ số", "Cách hiểu"], health_rows, [42*mm, 82*mm], NAVY)]], colWidths=[119*mm, 129*mm])
two_tables.setStyle(TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(0,-1),5*mm),("RIGHTPADDING",(1,0),(1,-1),0)]))
story.extend([two_tables, Spacer(1, 5*mm)])
insights_b = Table([[card("Biểu đồ", ["Income vs expense", "Cơ cấu thu / chi", "Net cash flow theo tháng", "Forecast 3 tháng"], CYAN, 112*mm), card("Khuyến nghị", ["Chi > thu", "Forecast âm", "Quỹ dự phòng thấp", "Khoản đến hạn thiếu liquidity"], CORAL, 112*mm)]], colWidths=[124*mm]*2)
insights_b.setStyle(TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(0,-1),6*mm),("RIGHTPADDING",(1,0),(1,-1),0)]))
story.extend([insights_b, Spacer(1, 5*mm), para("Điều kiện kiểm chứng", "H2"), bullet("Forecast chỉ dùng records hiển thị được và người dùng truy ngược được từng số liệu.", "#25B6D2"), bullet("Passive income xuất hiện đúng dưới asset đã liên kết; cash-flow total khớp với entries.", "#25B6D2"), PageBreak()])

# Page 7 - 1C
story.extend([
    para("06  •  MVP 1C", "Kicker"),
    para("Goals & Basic Advisor", "PageTitle"),
    para("Outcome: kết nối trạng thái hiện tại với mục tiêu tương lai và gợi ý hành động bằng các quy tắc minh bạch - chưa dùng AI.", "Lead"),
])
goal_rows = [
    ("Template", "Quỹ dự phòng, mua nhà, giáo dục, nghỉ hưu, tăng net worth, giảm nợ, khác"),
    ("Input", "Target amount, current allocation, target date, monthly contribution, expected return"),
    ("Output", "% hoàn thành, số tiền thiếu, thời gian còn lại, contribution cần thiết, trạng thái"),
]
advisor_rows = [
    ("Tích cực", "Dòng tiền dương, quỹ dự phòng đủ, mục tiêu đúng tiến độ"),
    ("Cần chú ý", "Concentration, liquidity thấp, debt burden, goal gap"),
    ("Hành động", "Tăng dự phòng, điều chỉnh contribution, cập nhật valuation, review concentration"),
]
story.extend([Table([[data_table(["Goal model", "Chi tiết"], goal_rows, [38*mm, 78*mm], MINT), data_table(["Advisor output", "Chi tiết"], advisor_rows, [40*mm, 86*mm], NAVY)]], colWidths=[121*mm, 127*mm], style=TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(0,-1),5*mm),("RIGHTPADDING",(1,0),(1,-1),0)])), Spacer(1, 5*mm)])
story.extend([
    para("Anatomy của một khuyến nghị", "H2"),
    Table([[para("01<br/><b>Metric</b><br/><font color='#667085'>Điều gì đang xảy ra?</font>", "CardBody"), para("02<br/><b>Rule</b><br/><font color='#667085'>Ngưỡng nào kích hoạt?</font>", "CardBody"), para("03<br/><b>Context</b><br/><font color='#667085'>Asset/liability nào liên quan?</font>", "CardBody"), para("04<br/><b>Action</b><br/><font color='#667085'>Hành động nào nên cân nhắc?</font>", "CardBody"), para("05<br/><b>Disclaimer</b><br/><font color='#667085'>Dựa trên dữ liệu tự nhập.</font>", "CardBody")]], colWidths=[CONTENT_W/5]*5, rowHeights=[28*mm], style=TableStyle([("BACKGROUND",(0,0),(-1,-1),LIGHT),("GRID",(0,0),(-1,-1),0.6,LINE),("VALIGN",(0,0),(-1,-1),"MIDDLE"),("LEFTPADDING",(0,0),(-1,-1),4*mm),("RIGHTPADDING",(0,0),(-1,-1),3*mm)])),
    Spacer(1, 5*mm),
    Table([[para("“Bất động sản đang chiếm 78% tổng tài sản. Ngưỡng tham khảo của hệ thống là 60%. Danh mục có mức tập trung cao; cân nhắc đánh giá lại nhu cầu thanh khoản trước nghĩa vụ gần nhất.”", "Quote")]], colWidths=[CONTENT_W], style=TableStyle([("BACKGROUND",(0,0),(-1,-1),HexColor("#EAF8F3")),("BOX",(0,0),(-1,-1),0.7,HexColor("#A7DEC9")),("LEFTPADDING",(0,0),(-1,-1),12*mm),("RIGHTPADDING",(0,0),(-1,-1),12*mm),("TOPPADDING",(0,0),(-1,-1),7*mm),("BOTTOMPADDING",(0,0),(-1,-1),7*mm)])),
    PageBreak(),
])

# Page 8 - 1D
story.extend([
    para("07  •  MVP 1D", "Kicker"),
    para("Pilot Readiness", "PageTitle"),
    para("Outcome: 2-5 người dùng có thể tự onboarding, sử dụng lặp lại và gửi feedback mà không cần developer hướng dẫn trực tiếp.", "Lead"),
])
pilot_cards = Table([[card("Onboarding", ["7 bước guided setup", "Checklist mức hoàn thiện", "Example data & empty states"], AMBER, 74*mm), card("Usability", ["Validation số tiền/ngày", "Search & filter", "Confirm edit/delete", "Mobile web responsive"], BLUE, 74*mm), card("Feedback & Quality", ["In-product feedback", "Minimal error logging", "Printable summary nếu chi phí thấp"], MINT, 74*mm)]], colWidths=[80.5*mm]*3)
pilot_cards.setStyle(TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(-1,-1),4.5*mm)]))
story.extend([pilot_cards, Spacer(1, 5*mm), para("Onboarding timeline", "H2")])
onboarding = ["Tỷ giá", "Cash & bank", "Tài sản lớn", "Khoản nợ", "Thu / chi", "Dashboard", "Mục tiêu"]
step_cells = []
for i, step in enumerate(onboarding, 1):
    step_cells.append(para(f'<font color="#F0A93A"><b>{i:02d}</b></font><br/><b>{step}</b>', "CardBody"))
story.extend([Table([step_cells], colWidths=[CONTENT_W/7]*7, rowHeights=[23*mm], style=TableStyle([("BACKGROUND",(0,0),(-1,-1),LIGHT),("GRID",(0,0),(-1,-1),0.6,LINE),("VALIGN",(0,0),(-1,-1),"MIDDLE"),("ALIGN",(0,0),(-1,-1),"CENTER"),("LEFTPADDING",(0,0),(-1,-1),3*mm),("RIGHTPADDING",(0,0),(-1,-1),3*mm)])), Spacer(1, 5*mm), para("Pilot learning questions", "H2")])
learning = [
    ("Activation", "Người dùng có hoàn tất nhập liệu để xem net worth không?"),
    ("Friction", "Bước và trường dữ liệu nào gây cản trở nhất?"),
    ("Trust", "Người dùng có tin vào số liệu tự nhập và công thức không?"),
    ("Value", "Chart/insight nào làm thay đổi cách hiểu tài chính?"),
    ("Retention", "Workflow nào tạo lý do quay lại hằng tuần?"),
]
story.extend([data_table(["Chủ đề", "Câu hỏi cần học"], learning, [42*mm, 196*mm], AMBER), PageBreak()])

# Page 9 - Data and calculations
story.extend([
    para("08  •  DATA & CALCULATION", "Kicker"),
    para("Backbone dữ liệu tối thiểu", "PageTitle"),
    para("Mô hình dữ liệu cần đủ để tái lập mọi con số trên dashboard, nhưng tránh cấu trúc Family Office và integrations ở MVP.", "Lead"),
])
entity_rows = [
    ("User", "Biên sở hữu dữ liệu cá nhân"),
    ("UserSetting", "VND/USD, tỷ giá và ngày cập nhật"),
    ("Asset", "Danh tính, phân loại, liquidity, metadata"),
    ("AssetValuation", "Lịch sử định giá append-only"),
    ("Liability", "Dư nợ, lịch trả, collateral"),
    ("CashFlowEntry", "Thu/chi một lần hoặc định kỳ"),
    ("Goal", "Target, contribution và projection"),
    ("InsightRule / Result", "Threshold và kết quả tái lập"),
]
formula_rows = [
    ("Quy đổi USD", "USD value × USD/VND rate"),
    ("Net worth", "Tổng assets VND - Tổng liabilities VND"),
    ("Debt-to-asset", "Tổng liabilities / Tổng assets"),
    ("Net cash flow", "Monthly income - Monthly expenses"),
    ("Reserve months", "High-liquidity assets / Essential monthly expenses"),
    ("Goal gap", "Target future value - Projected future value"),
]
story.extend([Table([[data_table(["Entity", "Vai trò"], entity_rows, [42*mm, 74*mm], NAVY), data_table(["Chỉ số", "Công thức minh bạch"], formula_rows, [48*mm, 78*mm], BLUE)]], colWidths=[121*mm,127*mm], style=TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(0,-1),5*mm),("RIGHTPADDING",(1,0),(1,-1),0)])), Spacer(1, 5*mm)])
story.extend([
    Table([[para("<b>Money:</b> dùng decimal-safe arithmetic; không lưu tiền bằng binary floating-point.", "CardBody"), para("<b>Freshness:</b> mọi valuation và FX rate phải có effective date.", "CardBody"), para("<b>Traceability:</b> forecast và insight phải truy ngược tới record nguồn.", "CardBody")]], colWidths=[CONTENT_W/3]*3, style=TableStyle([("BACKGROUND",(0,0),(-1,-1),HexColor("#EEF4FF")),("BOX",(0,0),(-1,-1),0.7,HexColor("#B8CDF8")),("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),4*mm),("RIGHTPADDING",(0,0),(-1,-1),4*mm),("TOPPADDING",(0,0),(-1,-1),4*mm),("BOTTOMPADDING",(0,0),(-1,-1),4*mm)])),
    Spacer(1, 5*mm),
    para("Minimum safety baseline", "H2"),
    Table([[bullet("Hash passwords", "#E8685B"), bullet("Server-side ownership checks", "#E8685B"), bullet("Không log financial payload", "#E8685B")], [bullet("Session expiry", "#E8685B"), bullet("HTTPS khi remote pilot", "#E8685B"), bullet("Chưa lưu tài liệu nhạy cảm", "#E8685B")]], colWidths=[CONTENT_W/3]*3, style=TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(-1,-1),4*mm)])),
    PageBreak(),
])

# Page 10 - Scope matrix
story.extend([
    para("09  •  SCOPE CONTROL", "Kicker"),
    para("MVP vs Phase 2", "PageTitle"),
    para("Bảng này là guardrail chống scope creep. Tính năng Phase 2 chỉ được kéo vào MVP khi product owner thay đổi baseline.", "Lead"),
])
scope_rows = [
    ("Dữ liệu", "Nhập tay", "Excel/CSV, duplicate detection, external APIs"),
    ("Tiền tệ", "VND + USD, FX tự nhập", "Automatic FX, nhiều currency"),
    ("Định giá", "User-entered + history", "Market data, BĐS, DN tư nhân, collectible"),
    ("Dashboard", "Net worth, allocation, liquidity, cash flow", "Benchmark, macro, custom dashboard"),
    ("Planning", "Goal model dùng chung", "Retirement, education, scenario, stress test"),
    ("Advisor", "Rules-based, explainable", "AI Advisor, portfolio optimization"),
    ("Documents", "Metadata + storage location", "Vault, encryption, OCR, Q&A"),
    ("Ownership", "Một user / một asset", "Household, co-ownership, legal entities"),
    ("Access", "Dữ liệu độc lập", "Advisor access, RBAC, audit nâng cao"),
    ("Client", "Responsive web", "Native mobile apps"),
]
story.extend([data_table(["Năng lực", "MVP", "Phase 2"], scope_rows, [38*mm, 78*mm, 122*mm], NAVY), Spacer(1, 5*mm)])
phase2 = [
    ("2A", "Reduce manual entry", "Excel/CSV, export, auto FX"),
    ("2B", "Integrations", "Bank, brokerage, market prices"),
    ("2C", "Secure Documents", "Encryption, backup, OCR"),
    ("2D", "Planning & AI", "Stress test, advisor, risk analysis"),
    ("2E", "Family Office", "Household, entities, succession"),
]
story.extend([para("Hướng mở rộng sau MVP", "H2"), data_table(["Phase", "Chủ đề", "Năng lực"], phase2, [28*mm, 62*mm, 148*mm], BLUE), PageBreak()])

# Page 11 - Final gate
story.extend([
    para("10  •  COMPLETION GATE", "Kicker"),
    para("Khi nào MVP được xem là hoàn thành?", "PageTitle"),
    para("Không đo completion bằng số lượng màn hình. MVP hoàn thành khi một pilot user có thể tự tạo và duy trì bức tranh tài chính có thể tin cậy.", "Lead"),
])
done = [
    "Tạo account và thiết lập tỷ giá.",
    "Nhập, sửa và duy trì assets/liabilities.",
    "Xem net worth dashboard reconcile theo VND.",
    "Ghi nhận income/expenses định kỳ.",
    "Đọc được financial health và upcoming obligations.",
    "Tạo goal và hiểu calculated gap.",
    "Hiểu nguồn gốc từng rules-based insight.",
    "Lặp lại workflow không cần developer hỗ trợ.",
]
done_cells = []
for i, item in enumerate(done, 1):
    done_cells.append(para(f'<font color="#23B38A"><b>[OK]</b></font>&nbsp;&nbsp;{item}', "CardBody"))
story.extend([
    Table([done_cells[:4], done_cells[4:]], colWidths=[CONTENT_W/4]*4, rowHeights=[23*mm, 23*mm], style=TableStyle([("BACKGROUND",(0,0),(-1,-1),LIGHT),("GRID",(0,0),(-1,-1),0.6,LINE),("VALIGN",(0,0),(-1,-1),"MIDDLE"),("LEFTPADDING",(0,0),(-1,-1),4*mm),("RIGHTPADDING",(0,0),(-1,-1),4*mm)])),
    Spacer(1, 7*mm),
    para("North-star của pilot", "H2"),
    Table([[para("Người dùng có sẵn sàng nhập dữ liệu thủ công để đổi lấy một bức tranh tài sản ròng thống nhất - và có quay lại để duy trì bức tranh đó hay không?", "Quote")]], colWidths=[CONTENT_W], style=TableStyle([("BACKGROUND",(0,0),(-1,-1),HexColor("#EEF4FF")),("BOX",(0,0),(-1,-1),0.8,HexColor("#B8CDF8")),("LEFTPADDING",(0,0),(-1,-1),16*mm),("RIGHTPADDING",(0,0),(-1,-1),16*mm),("TOPPADDING",(0,0),(-1,-1),9*mm),("BOTTOMPADDING",(0,0),(-1,-1),9*mm)])),
    Spacer(1, 7*mm),
    Table([[card("Không phải điều kiện completion", ["Real-time prices", "Bank/brokerage integration", "AI Advisor", "Document vault", "Family Office"], CORAL, CONTENT_W)]], colWidths=[CONTENT_W], style=TableStyle([("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(-1,-1),0)])),
])

doc.build(story)
print(OUTPUT)
