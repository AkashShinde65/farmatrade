package com.farmatrade.billing.service;

import com.farmatrade.billing.entity.Invoice;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Renders an Invoice as a styled, boxed PDF (logo + letterhead, bordered sale/charges/payment
 * sections, a highlighted grand-total row) instead of the previous plain, unstyled list of
 * paragraphs.
 */
@Service
@RequiredArgsConstructor
public class PdfInvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(PdfInvoiceService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final String LOGO_CLASSPATH_RESOURCE = "static/farmatrade-logo.png";

    private static final Color BRAND_GREEN = new Color(0x2E, 0x7D, 0x32);
    private static final Color LIGHT_GREEN = new Color(0xE8, 0xF5, 0xE9);
    private static final Color LABEL_GREY = new Color(0x6B, 0x6B, 0x6B);
    private static final Color BORDER_GREY = new Color(0xDD, 0xDD, 0xDD);
    private static final Color HEADER_ROW_GREY = new Color(0xFA, 0xFA, 0xFA);

    private final InvoiceService invoiceService;

    public byte[] generateInvoicePdf(Long invoiceId) {

        logger.info("Generating PDF for Invoice ID: {}", invoiceId);

        Invoice invoice = invoiceService.getInvoice(invoiceId);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(buildLetterhead(invoice));
            document.add(spacer(4));
            document.add(rule());
            document.add(spacer(18));

            document.add(sectionTitle("Sale Details"));
            document.add(saleDetailsTable(invoice));
            document.add(spacer(18));

            document.add(sectionTitle("Charges"));
            document.add(chargesTable(invoice));
            document.add(spacer(18));

            document.add(sectionTitle("Payment"));
            document.add(paymentTable(invoice));
            document.add(spacer(28));

            document.add(rule());
            document.add(spacer(8));
            document.add(footer());

            document.close();

            logger.info("PDF generated successfully for Invoice ID: {}", invoiceId);

            return out.toByteArray();

        } catch (Exception e) {

            logger.error("Failed to generate PDF for Invoice ID: {}", invoiceId, e);

            throw new RuntimeException("Unable to generate PDF", e);
        }
    }

    private PdfPTable buildLetterhead(Invoice invoice) throws Exception {
        PdfPTable letterhead = new PdfPTable(2);
        letterhead.setWidthPercentage(100);
        letterhead.setWidths(new float[]{1.6f, 1f});

        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setPadding(0);
        brandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPTable brandInner = new PdfPTable(2);
        brandInner.setWidths(new float[]{1f, 4.5f});

        PdfPCell logoCell;
        try {
            Image logo = Image.getInstance(loadLogoBytes());
            logo.scaleToFit(40, 40);
            logoCell = new PdfPCell(logo, false);
        } catch (Exception e) {
            logger.warn("Could not load invoice logo image, continuing without it", e);
            logoCell = new PdfPCell();
        }
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        brandInner.addCell(logoCell);

        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPaddingLeft(10);
        nameCell.addElement(new Paragraph("FarmaTrade", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND_GREEN)));
        nameCell.addElement(new Paragraph("F2B Agricultural Marketplace", FontFactory.getFont(FontFactory.HELVETICA, 9, LABEL_GREY)));
        brandInner.addCell(nameCell);

        brandCell.addElement(brandInner);
        letterhead.addCell(brandCell);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        metaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph title = new Paragraph("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, BRAND_GREEN));
        title.setAlignment(Element.ALIGN_RIGHT);
        Paragraph number = new Paragraph(nullSafe(invoice.getInvoiceNumber()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY));
        number.setAlignment(Element.ALIGN_RIGHT);
        Paragraph date = new Paragraph(formatDate(invoice.getCreatedAt()), FontFactory.getFont(FontFactory.HELVETICA, 9, LABEL_GREY));
        date.setAlignment(Element.ALIGN_RIGHT);

        metaCell.addElement(title);
        metaCell.addElement(number);
        metaCell.addElement(date);
        letterhead.addCell(metaCell);

        return letterhead;
    }

    private PdfPTable saleDetailsTable(Invoice invoice) {
        PdfPTable table = boxedTable(2, new float[]{1f, 1.6f});
        addLabelValueRow(table, "Invoice ID", String.valueOf(invoice.getInvoiceId()));
        addLabelValueRow(table, "Sale ID", String.valueOf(invoice.getSaleId()));
        addLabelValueRow(table, "Lot ID", String.valueOf(invoice.getLotId()));
        addLabelValueRow(table, "Crop", nullSafe(invoice.getCropName()));
        addLabelValueRow(table, "Quantity", invoice.getQuantity() != null ? String.valueOf(invoice.getQuantity()) : "-");
        addLabelValueRow(table, "Buyer ID", String.valueOf(invoice.getBuyerId()));
        addLabelValueRow(table, "Farmer ID", String.valueOf(invoice.getFarmerId()));
        addLabelValueRow(table, "Payment method", nullSafe(invoice.getPaymentMethod()));
        addLabelValueRow(table, "Logistics included",
                Boolean.TRUE.equals(invoice.getLogisticsAccepted()) ? "Yes" : "No");
        return table;
    }

    private PdfPTable chargesTable(Invoice invoice) {
        PdfPTable table = boxedTable(2, new float[]{2f, 1f});

        addChargesHeaderCell(table, "Description");
        addChargesHeaderCell(table, "Amount");

        addChargeRow(table, "Winning price", invoice.getAmount(), false);
        addChargeRow(table, "Platform fee", invoice.getPlatformFee(), false);
        addChargeRow(table, "GST", invoice.getGst(), false);
        addChargeRow(table, "Grand total", invoice.getTotalAmount(), true);

        return table;
    }

    private PdfPTable paymentTable(Invoice invoice) {
        PdfPTable table = boxedTable(2, new float[]{1f, 1.6f});
        addLabelValueRow(table, "Status", nullSafe(invoice.getStatus() != null ? invoice.getStatus().name() : null));
        addLabelValueRow(table, "Razorpay order ID", dashIfBlank(invoice.getRazorpayOrderId()));
        addLabelValueRow(table, "Razorpay payment ID", dashIfBlank(invoice.getRazorpayPaymentId()));
        addLabelValueRow(table, "Created at", formatDate(invoice.getCreatedAt()));
        addLabelValueRow(table, "Paid at", invoice.getPaidAt() != null ? formatDate(invoice.getPaidAt()) : "-");
        return table;
    }

    // ------------------------------------------------------------------
    // Shared layout helpers
    // ------------------------------------------------------------------

    private PdfPTable boxedTable(int columns, float[] widths) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        try {
            table.setWidths(widths);
        } catch (Exception ignored) {
            // widths.length must match columns; both are always passed together by callers above.
        }
        return table;
    }

    private void addLabelValueRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, LABEL_GREY)));
        labelCell.setBackgroundColor(HEADER_ROW_GREY);
        labelCell.setBorderColor(BORDER_GREY);
        labelCell.setPadding(7);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY)));
        valueCell.setBorderColor(BORDER_GREY);
        valueCell.setPadding(7);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(valueCell);
    }

    private void addChargesHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setBackgroundColor(BRAND_GREEN);
        cell.setBorderColor(BRAND_GREEN);
        cell.setPadding(8);
        cell.setHorizontalAlignment(text.equals("Amount") ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addChargeRow(PdfPTable table, String label, Double amount, boolean highlight) {
        Font labelFont = FontFactory.getFont(highlight ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA,
                highlight ? 11 : 10, highlight ? Color.DARK_GRAY : Color.DARK_GRAY);
        Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, highlight ? 12 : 10,
                highlight ? BRAND_GREEN : Color.DARK_GRAY);
        Color background = highlight ? LIGHT_GREEN : Color.WHITE;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(background);
        labelCell.setBorderColor(BORDER_GREY);
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(formatAmount(amount), amountFont));
        amountCell.setBackgroundColor(background);
        amountCell.setBorderColor(BORDER_GREY);
        amountCell.setPadding(8);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    private Paragraph sectionTitle(String text) {
        Paragraph title = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_GREEN));
        title.setSpacingAfter(6);
        return title;
    }

    private PdfPTable rule() {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(BRAND_GREEN);
        cell.setFixedHeight(2f);
        rule.addCell(cell);
        return rule;
    }

    private Paragraph spacer(float height) {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(0);
        spacer.setLeading(height);
        return spacer;
    }

    private Paragraph footer() {
        Paragraph thanks = new Paragraph("Thank you for trading with FarmaTrade.",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND_GREEN));
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.add(new Phrase("\nThis is a system-generated invoice and does not require a signature.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, LABEL_GREY)));
        return thanks;
    }

    private byte[] loadLogoBytes() throws Exception {
        try (var input = new ClassPathResource(LOGO_CLASSPATH_RESOURCE).getInputStream()) {
            return input.readAllBytes();
        }
    }

    private String formatAmount(Double amount) {
        return amount == null ? "Rs. 0.00" : String.format("Rs. %.2f", amount);
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_FORMAT);
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String dashIfBlank(String value) {
        return value == null || value.isBlank() ? "Not yet available" : value;
    }
}
