package cd.ecole.gestionscolaire.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ExpenseRepository — Contrat de persistance des dépenses et décaissements (PostgreSQL).
 */
public interface ExpenseRepository {

    long save(ExpenseRecord expense);

    Optional<ExpenseRecord> findById(long id);

    Optional<ExpenseRecord> findByVoucherNumber(String voucherNumber);

    List<ExpenseRecord> findBySessionId(long sessionId);

    void updateStatus(long id, String newStatus, String approvedBy);

    String generateNextVoucherNumber();

    record ExpenseRecord(
            long id,
            String voucherNumber,
            long cashSessionId,
            String beneficiaryName,
            String category,
            double amount,
            String currency,
            String paymentMethod,
            String reference,
            String justification,
            String attachmentName,
            String status,
            boolean requiresAuthorization,
            String approvedBy,
            String createdBy,
            LocalDateTime createdAt
    ) {}
}
