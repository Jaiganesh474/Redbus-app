package com.redbus.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.redbus.entity.Booking;
import com.redbus.entity.BookingPassenger;
import com.redbus.entity.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    public byte[] generateTicketPdf(Booking booking) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Colors
            Color redBusColor = new Color(216, 78, 85);
            Color darkGray = new Color(45, 55, 72);
            Color lightGray = new Color(243, 244, 246);

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, redBusColor);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, darkGray);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, darkGray);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkGray);
            Font whiteHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // Header Table (Brand + PNR & Status)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3f, 2f});

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            Paragraph brandP = new Paragraph("redBus", titleFont);
            Paragraph brandSub = new Paragraph("India's No. 1 Online Bus Ticketing Platform", smallFont);
            logoCell.addElement(brandP);
            logoCell.addElement(brandSub);
            headerTable.addCell(logoCell);

            PdfPCell pnrCell = new PdfPCell();
            pnrCell.setBorder(Rectangle.NO_BORDER);
            pnrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph pnrP = new Paragraph("PNR: " + booking.getPnr(), subtitleFont);
            pnrP.setAlignment(Element.ALIGN_RIGHT);
            Paragraph statusP = new Paragraph("Status: " + booking.getStatus(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, "CONFIRMED".equalsIgnoreCase(booking.getStatus()) ? new Color(16, 149, 93) : Color.RED));
            statusP.setAlignment(Element.ALIGN_RIGHT);
            pnrCell.addElement(pnrP);
            pnrCell.addElement(statusP);
            headerTable.addCell(pnrCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // Journey Route Banner
            Route route = booking.getRoute();
            PdfPTable routeTable = new PdfPTable(3);
            routeTable.setWidthPercentage(100);
            routeTable.setWidths(new float[]{2f, 1f, 2f});
            routeTable.getDefaultCell().setBackgroundColor(lightGray);
            routeTable.getDefaultCell().setPadding(10);
            routeTable.getDefaultCell().setBorderColor(Color.LIGHT_GRAY);

            PdfPCell fromCell = new PdfPCell();
            fromCell.setBackgroundColor(lightGray);
            fromCell.setPadding(10);
            fromCell.setBorderColor(Color.LIGHT_GRAY);
            fromCell.addElement(new Paragraph("FROM", smallFont));
            fromCell.addElement(new Paragraph(route.getSourceCity(), subtitleFont));
            fromCell.addElement(new Paragraph(route.getDepartureTime().format(TIME_FMT), boldFont));
            fromCell.addElement(new Paragraph(route.getTravelDate().format(DATE_FMT), normalFont));
            routeTable.addCell(fromCell);

            PdfPCell arrowCell = new PdfPCell();
            arrowCell.setBackgroundColor(lightGray);
            arrowCell.setPadding(10);
            arrowCell.setBorderColor(Color.LIGHT_GRAY);
            arrowCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph arrow = new Paragraph("➔\n" + route.getDurationHours() + " hrs", boldFont);
            arrow.setAlignment(Element.ALIGN_CENTER);
            arrowCell.addElement(arrow);
            routeTable.addCell(arrowCell);

            PdfPCell toCell = new PdfPCell();
            toCell.setBackgroundColor(lightGray);
            toCell.setPadding(10);
            toCell.setBorderColor(Color.LIGHT_GRAY);
            toCell.addElement(new Paragraph("TO", smallFont));
            toCell.addElement(new Paragraph(route.getDestinationCity(), subtitleFont));
            toCell.addElement(new Paragraph(route.getArrivalTime().format(TIME_FMT), boldFont));
            toCell.addElement(new Paragraph(route.getTravelDate().format(DATE_FMT), normalFont));
            routeTable.addCell(toCell);

            document.add(routeTable);
            document.add(new Paragraph(" "));

            // Bus & Operator Info
            PdfPTable busInfoTable = new PdfPTable(2);
            busInfoTable.setWidthPercentage(100);
            busInfoTable.setWidths(new float[]{1f, 1f});

            PdfPCell busCell1 = new PdfPCell();
            busCell1.setBorder(Rectangle.NO_BORDER);
            busCell1.addElement(new Paragraph("Bus Operator: " + route.getBus().getOperatorName(), boldFont));
            busCell1.addElement(new Paragraph("Bus Type: " + route.getBus().getBusType(), normalFont));
            busCell1.addElement(new Paragraph("Boarding Point: " + booking.getBoardingPoint(), normalFont));
            busInfoTable.addCell(busCell1);

            PdfPCell busCell2 = new PdfPCell();
            busCell2.setBorder(Rectangle.NO_BORDER);
            busCell2.addElement(new Paragraph("Booking Date: " + (booking.getCreatedAt() != null ? booking.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "Recent"), normalFont));
            busCell2.addElement(new Paragraph("Dropping Point: " + booking.getDroppingPoint(), normalFont));
            busCell2.addElement(new Paragraph("Contact: " + booking.getContactPhone() + " (" + booking.getContactEmail() + ")", normalFont));
            busInfoTable.addCell(busCell2);

            document.add(busInfoTable);
            document.add(new Paragraph(" "));

            // Passengers Table
            Paragraph passTitle = new Paragraph("Passenger Details", subtitleFont);
            passTitle.setSpacingAfter(8);
            document.add(passTitle);

            PdfPTable passengerTable = new PdfPTable(4);
            passengerTable.setWidthPercentage(100);
            passengerTable.setWidths(new float[]{1f, 4f, 2f, 2f});

            addHeaderCell(passengerTable, "Seat No", redBusColor, whiteHeaderFont);
            addHeaderCell(passengerTable, "Passenger Name", redBusColor, whiteHeaderFont);
            addHeaderCell(passengerTable, "Age", redBusColor, whiteHeaderFont);
            addHeaderCell(passengerTable, "Gender", redBusColor, whiteHeaderFont);

            for (BookingPassenger bp : booking.getPassengers()) {
                addBodyCell(passengerTable, bp.getSeatNumber(), normalFont);
                addBodyCell(passengerTable, bp.getName(), boldFont);
                addBodyCell(passengerTable, String.valueOf(bp.getAge()), normalFont);
                addBodyCell(passengerTable, bp.getGender(), normalFont);
            }
            document.add(passengerTable);
            document.add(new Paragraph(" "));

            // Fare & QR Code Section (Itemized Tax Invoice Breakdown)
            PdfPTable bottomTable = new PdfPTable(2);
            bottomTable.setWidthPercentage(100);
            bottomTable.setWidths(new float[]{3.2f, 1.8f});

            PdfPCell fareCell = new PdfPCell();
            fareCell.setBorder(Rectangle.BOX);
            fareCell.setPadding(10);
            fareCell.setBorderColor(Color.LIGHT_GRAY);
            
            Paragraph invoiceHeader = new Paragraph("Payment & Tax Invoice Breakdown", subtitleFont);
            invoiceHeader.setSpacingAfter(4);
            fareCell.addElement(invoiceHeader);

            int seatCount = booking.getPassengers().size();
            java.math.BigDecimal basePrice = route.getBasePrice();
            java.math.BigDecimal totalBaseFare = basePrice.multiply(java.math.BigDecimal.valueOf(seatCount));

            fareCell.addElement(new Paragraph("Base Ticket Price: ₹" + basePrice + " × " + seatCount + " seat(s) = ₹" + totalBaseFare, normalFont));

            if (Boolean.TRUE.equals(booking.getHasFreeCancellation())) {
                java.math.BigDecimal cancelFee = booking.getFreeCancellationFee() != null ? booking.getFreeCancellationFee() : new java.math.BigDecimal("21.00").multiply(java.math.BigDecimal.valueOf(seatCount));
                fareCell.addElement(new Paragraph("Free Cancellation Protection: +₹" + cancelFee, normalFont));
            }

            if (booking.getDiscountAmount() != null && booking.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                fareCell.addElement(new Paragraph("Coupon Discount (" + (booking.getCouponCode() != null ? booking.getCouponCode() : "PROMO") + "): -₹" + booking.getDiscountAmount(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(16, 149, 93))));
            }

            if (booking.getWalletAmountUsed() != null && booking.getWalletAmountUsed().compareTo(java.math.BigDecimal.ZERO) > 0) {
                fareCell.addElement(new Paragraph("redBus Wallet Deducted: -₹" + booking.getWalletAmountUsed(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(16, 149, 93))));
            }

            fareCell.addElement(new Paragraph("Operator Service Fee & GST: FREE (₹0.00)", normalFont));

            String paymentMode;
            if (booking.getWalletAmountUsed() != null && booking.getWalletAmountUsed().compareTo(java.math.BigDecimal.ZERO) > 0) {
                if (booking.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
                    paymentMode = "100% Paid via redBus Wallet";
                } else {
                    paymentMode = "Split Payment: Wallet (₹" + booking.getWalletAmountUsed() + ") + Online Gateway (₹" + booking.getTotalAmount() + ")";
                }
            } else {
                paymentMode = "Online Payment (Card / UPI / NetBanking)";
            }

            fareCell.addElement(new Paragraph("Payment Method: " + paymentMode, boldFont));
            
            Paragraph totalPaidP = new Paragraph("Final Gateway Paid: ₹" + booking.getTotalAmount(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, redBusColor));
            totalPaidP.setSpacingBefore(4);
            fareCell.addElement(totalPaidP);
            
            bottomTable.addCell(fareCell);

            // Generate QR Code
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.BOX);
            qrCell.setPadding(10);
            qrCell.setBorderColor(Color.LIGHT_GRAY);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            try {
                String qrData = "RedBus|PNR:" + booking.getPnr() + "|Route:" + route.getSourceCity() + "-" + route.getDestinationCity() + "|Date:" + route.getTravelDate();
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 100, 100);
                ByteArrayOutputStream qrStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrStream);
                Image qrImage = Image.getInstance(qrStream.toByteArray());
                qrImage.setAlignment(Element.ALIGN_CENTER);
                qrCell.addElement(qrImage);
                Paragraph qrLabel = new Paragraph("Scan for M-Ticket Verification", smallFont);
                qrLabel.setAlignment(Element.ALIGN_CENTER);
                qrCell.addElement(qrLabel);
            } catch (Exception e) {
                log.error("Failed to generate QR Code", e);
                qrCell.addElement(new Paragraph("PNR: " + booking.getPnr(), boldFont));
            }
            bottomTable.addCell(qrCell);

            document.add(bottomTable);
            document.add(new Paragraph(" "));

            // Important Instructions
            Paragraph termsTitle = new Paragraph("Terms & Important Instructions:", subtitleFont);
            termsTitle.setSpacingAfter(4);
            document.add(termsTitle);

            Paragraph terms1 = new Paragraph("1. Please arrive at the boarding point at least 15 minutes before the departure time.", smallFont);
            Paragraph terms2 = new Paragraph("2. Carry a valid government-issued photo ID proof (Aadhaar, Passport, Driving License) along with this e-ticket.", smallFont);
            Paragraph terms3 = new Paragraph("3. Free luggage allowance is up to 15 kg per passenger. Excess luggage is subject to operator charges.", smallFont);
            Paragraph terms4 = new Paragraph("4. For 24x7 AI Assistance or cancellations, visit redBus or chat with our automated booking assistant.", smallFont);
            document.add(terms1);
            document.add(terms2);
            document.add(terms3);
            document.add(terms4);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF for booking " + booking.getPnr(), e);
            throw new RuntimeException("Could not generate e-ticket PDF: " + e.getMessage());
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Color bg, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
