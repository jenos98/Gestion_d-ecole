package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Receipt;
import com.jenos.gestionecole.service.PaymentService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * ReceiptPreviewController — Contrôleur de prévisualisation et d'impression d'une quittance officielle.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class ReceiptPreviewController {

    @FXML private VBox receiptSheet;

    // En-tête reçu
    @FXML private Label lblReceiptNumber;
    @FXML private Label lblBadgeOriginal;
    @FXML private Label lblIssueDate;

    // Bénéficiaire
    @FXML private Label lblBeneficiaryName;
    @FXML private Label lblBeneficiaryMatricule;
    @FXML private Label lblBeneficiaryDetail;

    // Détail règlement
    @FXML private Label lblFeeName;
    @FXML private Label lblPaymentMethod;
    @FXML private Label lblTransactionRef;
    @FXML private Label lblAmountPaid;

    // Solde restant
    @FXML private Label lblFeeTotalDue;
    @FXML private Label lblBalanceRemaining;

    // Caisse & Signature
    @FXML private Label lblCashier;

    // Boutons
    @FXML private Button btnClose;
    @FXML private Button btnPrint;

    private Receipt currentReceipt;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (currentReceipt == null) {
            Receipt last = PaymentService.getInstance().getLastGeneratedReceipt();
            if (last != null) {
                setReceipt(last);
            }
        }
    }

    public void setReceipt(Receipt receipt) {
        this.currentReceipt = receipt;
        if (receipt == null) return;

        lblReceiptNumber.setText(receipt.getReceiptNumber());

        if (receipt.isDuplicate()) {
            lblBadgeOriginal.setText("DUPLICATA (Tirage N°" + receipt.getPrintCount() + ")");
            lblBadgeOriginal.getStyleClass().removeAll("badge-success");
            lblBadgeOriginal.getStyleClass().add("badge-danger");
        } else {
            lblBadgeOriginal.setText("ORIGINAL");
            lblBadgeOriginal.getStyleClass().removeAll("badge-danger");
            lblBadgeOriginal.getStyleClass().add("badge-success");
        }

        if (receipt.getIssuedAt() != null) {
            lblIssueDate.setText("Émis le: " + receipt.getIssuedAt().format(dtf));
        }

        lblBeneficiaryName.setText(receipt.getBeneficiaryName() != null ? receipt.getBeneficiaryName() : "—");
        lblBeneficiaryMatricule.setText(receipt.getBeneficiaryMatricule() != null ? receipt.getBeneficiaryMatricule() : "—");
        lblBeneficiaryDetail.setText(receipt.getBeneficiaryDetail() != null ? receipt.getBeneficiaryDetail() : "—");

        lblFeeName.setText(receipt.getFeeOrPurpose() != null ? receipt.getFeeOrPurpose() : "—");
        lblPaymentMethod.setText(receipt.getPaymentMethod() != null ? receipt.getPaymentMethod() : "ESPECES");

        String ref = receipt.getTransactionReference();
        lblTransactionRef.setText(ref != null && !ref.isBlank() ? ref : "Aucune (Règlement direct)");

        String cur = receipt.getCurrency() != null ? receipt.getCurrency() : "USD";
        if ("USD".equalsIgnoreCase(cur)) {
            lblAmountPaid.setText(String.format("%.2f USD", receipt.getAmountPaid()));
            lblFeeTotalDue.setText(String.format("%.2f USD", receipt.getFeeTotalDue()));
            lblBalanceRemaining.setText(String.format("%.2f USD", receipt.getBalanceRemaining()));
        } else {
            lblAmountPaid.setText(String.format("%,.0f CDF", receipt.getAmountPaid()));
            lblFeeTotalDue.setText(String.format("%,.0f CDF", receipt.getFeeTotalDue()));
            lblBalanceRemaining.setText(String.format("%,.0f CDF", receipt.getBalanceRemaining()));
        }

        String cashier = receipt.getCashierUsername() != null ? receipt.getCashierUsername() : "Jenos";
        String register = receipt.getCashRegisterName() != null ? receipt.getCashRegisterName() : "Caisse Principale 01";
        lblCashier.setText("Caissière : " + cashier + " | Guichet : " + register);
    }

    @FXML
    private void handlePrint() {
        if (currentReceipt != null) {
            currentReceipt.setPrintCount(currentReceipt.getPrintCount() + 1);
            currentReceipt.setDuplicate(true);
        }

        // Lancement de l'impression système JavaFX
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean success = job.printPage(receiptSheet);
            if (success) {
                job.endJob();
            }
        }

        // Rafraîchir l'affichage du badge duplicata pour les impressions ultérieures
        if (currentReceipt != null) {
            setReceipt(currentReceipt);
        }
    }

    @FXML
    private void handleClose() {
        NavigationManager.getInstance().navigate(Route.PAYMENTS);
    }
}
