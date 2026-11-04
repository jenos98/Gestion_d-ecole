package cd.ecole.gestionscolaire.service;

import java.io.File;

/**
 * CashSessionService — Contrat régissant le cycle complet de clôture de caisse.
 *
 * SÉQUENCE OBLIGATOIRE :
 * OUVERTE -> RAPPORT_GÉNÉRÉ -> RAPPORT_IMPRIMÉ -> COMPTAGE_PHYSIQUE -> RAPPROCHEMENT -> CLÔTURÉE
 */
public interface CashSessionService {

    long openSession(String cashRegisterCode, String cashierUsername, double initialUSD, double initialCDF);

    void generateDailyReport(long sessionId);

    void markDailyReportPrinted(long sessionId);

    void recordPhysicalCount(long sessionId, double countedUSD, double countedCDF);

    void closeSession(long sessionId, String closureNotes);

    void forceCloseSession(long sessionId, String reason, String supervisorUsername);

    boolean isClosureBlockedPendingReport(long sessionId);
}
