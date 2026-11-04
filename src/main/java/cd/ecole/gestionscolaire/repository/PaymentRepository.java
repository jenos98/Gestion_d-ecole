package cd.ecole.gestionscolaire.repository;

import cd.ecole.gestionscolaire.dto.PaymentDTO;
import cd.ecole.gestionscolaire.dto.PaymentFilter;
import cd.ecole.gestionscolaire.dto.PageRequest;
import cd.ecole.gestionscolaire.dto.PageResult;

import java.util.Optional;

/**
 * PaymentRepository — Contrat de persistance des paiements (PostgreSQL).
 */
public interface PaymentRepository {

    long save(PaymentDTO payment);

    Optional<PaymentDTO> findById(long id);

    Optional<PaymentDTO> findByReceiptNumber(String receiptNumber);

    PageResult<PaymentDTO> search(PaymentFilter filter, PageRequest page);

    void updateStatus(long id, String newStatus, String justification);

    String generateNextReceiptNumber();
}
