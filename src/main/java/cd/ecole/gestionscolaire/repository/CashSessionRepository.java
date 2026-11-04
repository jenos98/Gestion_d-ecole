package cd.ecole.gestionscolaire.repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * CashSessionRepository — Contrat de gestion des sessions de caisse (PostgreSQL).
 */
public interface CashSessionRepository {

    long openSession(CashSessionRecord session);

    Optional<CashSessionRecord> findById(long id);

    Optional<CashSessionRecord> findActiveSessionByCashRegister(String registerCode);

    void updateReportStatus(long sessionId, boolean generated, boolean printed);

    void recordCountAndReconciliation(long sessionId, double countedUSD, double countedCDF, double diffUSD, double diffCDF);

    void closeSession(long sessionId, LocalDateTime closedAt, String notes, String status);

    record CashSessionRecord(
            long id,
            long cashRegisterId,
            String cashRegisterCode,
            String cashierUsername,
            LocalDateTime openedAt,
            LocalDateTime closedAt,
            double initialBalanceUSD,
            double initialBalanceCDF,
            double currentBalanceUSD,
            double currentBalanceCDF,
            Double countedBalanceUSD,
            Double countedBalanceCDF,
            Double differenceUSD,
            Double differenceCDF,
            String closureNotes,
            String status,
            boolean isReportGenerated,
            boolean isReportPrinted,
            boolean isExceptional
    ) {}
}
