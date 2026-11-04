package cd.ecole.gestionscolaire.service;

import cd.ecole.gestionscolaire.dto.PaymentReceiptDTO;

import java.io.File;

/**
 * PdfReportService — Contrat de génération PDF via Apache PDFBox.
 */
public interface PdfReportService {

    File generateReceiptPdf(PaymentReceiptDTO receipt, File outputFile);

    File generateDailyCashReportPdf(long sessionId, File outputFile);
}
