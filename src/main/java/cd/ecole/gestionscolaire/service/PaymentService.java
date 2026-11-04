package cd.ecole.gestionscolaire.service;

import cd.ecole.gestionscolaire.dto.*;

/**
 * PaymentService — Contrat central pour la gestion des paiements et encaissements.
 *
 * RÈGLES MÉTIER ASSOCIÉES :
 * - Le montant doit être strictement positif.
 * - La devise doit être explicitement 'USD' ou 'CDF' (aucun mélange ni conversion implicite).
 * - Une référence est exigée pour MOBILE_MONEY, VIREMENT, CHEQUE.
 * - Idempotence : une opération ne peut pas être enregistrée en double (anti-double clic).
 * - L'écriture en caisse, le reçu et la traçabilité doivent être atomiques dans une transaction JDBC.
 * - L'annulation est non-destructive et conserve l'historique complet.
 * - L'impression du reçu n'intervient qu'après confirmation de l'enregistrement.
 */
public interface PaymentService {

    PaymentReceiptDTO registerPayment(CreatePaymentDTO request);

    PaymentDTO findById(long paymentId);

    PageResult<PaymentDTO> searchPayments(PaymentFilter filter, PageRequest page);

    void requestCancellation(long paymentId, String reason);

    void approveCancellation(long paymentId, String reason);
}
