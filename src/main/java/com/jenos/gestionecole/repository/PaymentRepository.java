package com.jenos.gestionecole.repository;

import cd.ecole.gestionscolaire.config.DatabaseConfig;
import com.jenos.gestionecole.model.Payment;
import com.jenos.gestionecole.model.Receipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PaymentRepository — Persistance des paiements et des quittances de caisse avec synchronisation PostgreSQL.
 * Aucune suppression physique n'est opérée (non destructif).
 */
public class PaymentRepository {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRepository.class);
    private final List<Payment> payments = new ArrayList<>();
    private final List<Receipt> receipts = new ArrayList<>();

    public PaymentRepository() {
        initDefaultData();
    }

    private void initDefaultData() {
        if (DatabaseConfig.isDatabaseReachable()) {
            loadFromDatabase();
            if (!payments.isEmpty()) {
                logger.info("{} paiements chargés depuis PostgreSQL.", payments.size());
                return;
            }
        }
        // Paiement initial 1
        Payment p1 = new Payment();
        p1.setId(1);
        p1.setReceiptNumber("REC-2025-0001");
        p1.setPaymentType(Payment.PaymentType.ELEVE);
        p1.setAmount(150.0);
        p1.setCurrency("USD");
        p1.setPaymentMethod(Payment.PaymentMethod.ESPECES);
        p1.setPaymentDate(LocalDate.now());
        p1.setCreatedAt(LocalDateTime.now().minusHours(3));
        p1.setCashierUsername("Jenos");
        p1.setCashRegisterName("Caisse Principale 01");
        payments.add(p1);

        Receipt r1 = new Receipt();
        r1.setId(1);
        r1.setReceiptNumber("REC-2025-0001");
        r1.setPayment(p1);
        r1.setIssuedAt(p1.getCreatedAt());
        r1.setCashierUsername("Jenos");
        r1.setCashRegisterName("Caisse Principale 01");
        r1.setBeneficiaryName("Mukendi Tshilumba Jonathan");
        r1.setBeneficiaryMatricule("2025-SEC-0001");
        r1.setBeneficiaryDetail("6e Math-Physique A - Scientifique");
        r1.setFeeOrPurpose("Minerval 1er Trimestre");
        r1.setAmountPaid(150.0);
        r1.setCurrency("USD");
        r1.setPaymentMethod("ESPECES");
        r1.setFeeTotalDue(150.0);
        r1.setFeePreviouslyPaid(0.0);
        r1.setBalanceRemaining(0.0);
        receipts.add(r1);

        // Paiement initial 2
        Payment p2 = new Payment();
        p2.setId(2);
        p2.setReceiptNumber("REC-2025-0002");
        p2.setPaymentType(Payment.PaymentType.ELEVE);
        p2.setAmount(35000.0);
        p2.setCurrency("CDF");
        p2.setPaymentMethod(Payment.PaymentMethod.MOBILE_MONEY);
        p2.setReference("MP-20250925-883");
        p2.setPaymentDate(LocalDate.now());
        p2.setCreatedAt(LocalDateTime.now().minusHours(2));
        p2.setCashierUsername("Jenos");
        p2.setCashRegisterName("Caisse Principale 01");
        payments.add(p2);

        Receipt r2 = new Receipt();
        r2.setId(2);
        r2.setReceiptNumber("REC-2025-0002");
        r2.setPayment(p2);
        r2.setIssuedAt(p2.getCreatedAt());
        r2.setCashierUsername("Jenos");
        r2.setCashRegisterName("Caisse Principale 01");
        r2.setBeneficiaryName("Ilunga Kapinga Esther");
        r2.setBeneficiaryMatricule("2025-SEC-0002");
        r2.setBeneficiaryDetail("5e Commerciale A - Commerciale & Gestion");
        r2.setFeeOrPurpose("Atelier Informatique & Pratique Commerciale");
        r2.setAmountPaid(35000.0);
        r2.setCurrency("CDF");
        r2.setPaymentMethod("MOBILE_MONEY");
        r2.setTransactionReference("MP-20250925-883");
        r2.setFeeTotalDue(35000.0);
        r2.setFeePreviouslyPaid(0.0);
        r2.setBalanceRemaining(0.0);
        receipts.add(r2);
    }

    public void save(Payment payment) {
        if (payment == null) return;
        if (payment.getId() == 0) {
            int maxId = payments.stream().mapToInt(Payment::getId).max().orElse(0);
            payment.setId(maxId + 1);
            payments.add(payment);
            if (DatabaseConfig.isDatabaseReachable()) {
                saveToDatabase(payment, true);
            }
        } else {
            for (int i = 0; i < payments.size(); i++) {
                if (payments.get(i).getId() == payment.getId()) {
                    payments.set(i, payment);
                    break;
                }
            }
            if (DatabaseConfig.isDatabaseReachable()) {
                saveToDatabase(payment, false);
            }
        }
    }

    private void saveToDatabase(Payment p, boolean isInsert) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (isInsert) {
                String sql = "INSERT INTO payments (receipt_number, cashier_username, cash_register_name, amount, currency, payment_method, reference, status, payment_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, p.getReceiptNumber() != null ? p.getReceiptNumber() : "REC-" + System.currentTimeMillis());
                    ps.setString(2, p.getCashierUsername() != null ? p.getCashierUsername() : "Jenos");
                    ps.setString(3, p.getCashRegisterName() != null ? p.getCashRegisterName() : "Caisse Principale");
                    ps.setDouble(4, p.getAmount());
                    ps.setString(5, p.getCurrency() != null ? p.getCurrency() : "USD");
                    ps.setString(6, p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "ESPECES");
                    ps.setString(7, p.getReference());
                    ps.setString(8, "VALIDEE");
                    ps.setDate(9, p.getPaymentDate() != null ? Date.valueOf(p.getPaymentDate()) : Date.valueOf(LocalDate.now()));
                    ps.setTimestamp(10, p.getCreatedAt() != null ? Timestamp.valueOf(p.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE payments SET amount = ?, currency = ?, payment_method = ?, reference = ? WHERE receipt_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, p.getAmount());
                    ps.setString(2, p.getCurrency() != null ? p.getCurrency() : "USD");
                    ps.setString(3, p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "ESPECES");
                    ps.setString(4, p.getReference());
                    ps.setString(5, p.getReceiptNumber());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            logger.warn("Synchronisation PostgreSQL du paiement {} échouée : {}", p.getReceiptNumber(), e.getMessage());
        }
    }

    private void loadFromDatabase() {
        String sql = "SELECT id, receipt_number, cashier_username, cash_register_name, amount, currency, payment_method, reference, status, payment_date, created_at FROM payments ORDER BY id";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Payment p = new Payment();
                p.setId(rs.getInt("id"));
                p.setReceiptNumber(rs.getString("receipt_number"));
                p.setCashierUsername(rs.getString("cashier_username"));
                p.setCashRegisterName(rs.getString("cash_register_name"));
                p.setAmount(rs.getDouble("amount"));
                p.setCurrency(rs.getString("currency"));
                String methodStr = rs.getString("payment_method");
                if (methodStr != null) {
                    try {
                        p.setPaymentMethod(Payment.PaymentMethod.valueOf(methodStr));
                    } catch (Exception ignored) {}
                }
                p.setReference(rs.getString("reference"));
                Date d = rs.getDate("payment_date");
                if (d != null) p.setPaymentDate(d.toLocalDate());
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
                payments.add(p);
            }
        } catch (Exception e) {
            logger.warn("Impossible de lire les paiements depuis PostgreSQL : {}", e.getMessage());
        }
    }

    public void saveReceipt(Receipt receipt) {
        if (receipt == null) return;
        if (receipt.getId() == 0) {
            int maxId = receipts.stream().mapToInt(Receipt::getId).max().orElse(0);
            receipt.setId(maxId + 1);
            receipts.add(receipt);
        } else {
            for (int i = 0; i < receipts.size(); i++) {
                if (receipts.get(i).getId() == receipt.getId()) {
                    receipts.set(i, receipt);
                    return;
                }
            }
        }
    }

    public List<Payment> findAllPayments() {
        return new ArrayList<>(payments);
    }

    public List<Receipt> findAllReceipts() {
        return new ArrayList<>(receipts);
    }

    public List<Receipt> findRecentReceipts(int limit) {
        return receipts.stream()
                .sorted(Comparator.comparing(Receipt::getIssuedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Receipt findReceiptByNumber(String receiptNumber) {
        if (receiptNumber == null) return null;
        return receipts.stream()
                .filter(r -> receiptNumber.equalsIgnoreCase(r.getReceiptNumber()))
                .findFirst()
                .orElse(null);
    }

    public Receipt findReceiptByPaymentId(int paymentId) {
        return receipts.stream()
                .filter(r -> r.getPayment() != null && r.getPayment().getId() == paymentId)
                .findFirst()
                .orElse(null);
    }

    /** Recherche les paiements d'un élève par son identifiant. */
    public List<Payment> findByStudentId(int studentId) {
        return payments.stream()
                .filter(p -> p.getStudent() != null && p.getStudent().getId() == studentId)
                .collect(Collectors.toList());
    }

    /** Recherche les paiements d'un élève pour un frais spécifique. */
    public List<Payment> findByStudentAndFee(int studentId, int feeId) {
        return payments.stream()
                .filter(p -> p.getStudent() != null && p.getStudent().getId() == studentId
                          && p.getFee() != null && p.getFee().getId() == feeId)
                .collect(Collectors.toList());
    }

    public int countTotalPayments() {
        return payments.size();
    }
}
