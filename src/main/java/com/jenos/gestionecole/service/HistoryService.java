package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.Operation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * HistoryService — Service de consultation et d'audit non destructif des opérations.
 */
public class HistoryService {

    private static HistoryService instance;
    private final List<Operation> operations = new ArrayList<>();

    private HistoryService() {
        initMockData();
    }

    public static HistoryService getInstance() {
        if (instance == null) {
            instance = new HistoryService();
        }
        return instance;
    }

    private void initMockData() {
        Operation op1 = new Operation(1, "REC-2026-0001", Operation.OperationType.PAIEMENT_ELEVE,
                "Frais Inscription - KABEYA Jonathan", 50.0, "USD", "ESPECES", "REC-001",
                "Jenos", "Caisse Principale 01", LocalDateTime.now().minusHours(4), Operation.OperationStatus.VALIDEE);
        op1.setBeneficiaryName("KABEYA Jonathan");
        op1.setStudentMatricule("2026-SEC-001");
        op1.setSectionName("Secondaire");
        op1.setPromotionName("6ème");
        op1.setOptionName("Scientifique");
        op1.setClassName("6ème A");
        operations.add(op1);

        Operation op2 = new Operation(2, "REC-2026-0002", Operation.OperationType.PAIEMENT_ELEVE,
                "Frais techniques - ILUNGA Esther", 85000.0, "CDF", "MOBILE_MONEY", "MP-88392",
                "Jenos", "Caisse Principale 01", LocalDateTime.now().minusHours(3), Operation.OperationStatus.VALIDEE);
        op2.setBeneficiaryName("ILUNGA Esther");
        op2.setStudentMatricule("2026-SEC-002");
        op2.setSectionName("Secondaire");
        op2.setPromotionName("5ème");
        op2.setOptionName("Commerciale");
        op2.setClassName("5ème A");
        operations.add(op2);

        Operation op3 = new Operation(3, "DEP-2026-0001", Operation.OperationType.DEPENSE,
                "Achat rames papier - Papeterie Centrale", 65.0, "USD", "ESPECES", "FAC-8891",
                "Jenos", "Caisse Principale 01", LocalDateTime.now().minusHours(2), Operation.OperationStatus.VALIDEE);
        op3.setBeneficiaryName("Papeterie Centrale");
        operations.add(op3);

        Operation op4 = new Operation(4, "REC-2026-0004", Operation.OperationType.PAIEMENT_PERSONNEL,
                "Avance sur salaire - Mukendi Alain", 100.0, "USD", "ESPECES", "AV-012",
                "Jenos", "Caisse Principale 01", LocalDateTime.now().minusHours(1), Operation.OperationStatus.VALIDEE);
        op4.setBeneficiaryName("Mukendi Alain");
        operations.add(op4);
    }

    public List<Operation> search(LocalDate start, LocalDate end,
                                  Operation.OperationType type,
                                  Operation.OperationStatus status,
                                  String cashier, String cashRegister,
                                  String keyword) {
        return search(start, end, type, status, cashier, cashRegister, keyword, null, null, null, null);
    }

    public List<Operation> search(LocalDate start, LocalDate end,
                                  Operation.OperationType type,
                                  Operation.OperationStatus status,
                                  String cashier, String cashRegister,
                                  String keyword,
                                  String section, String promotion,
                                  String option, String classe) {
        List<Operation> results = new ArrayList<>();
        for (Operation op : operations) {
            if (type != null && op.getType() != type) continue;
            if (status != null && op.getStatus() != status) continue;
            if (cashier != null && !cashier.isBlank() && !op.getCashierUsername().equalsIgnoreCase(cashier)) continue;
            if (cashRegister != null && !cashRegister.isBlank() && !op.getCashRegisterName().equalsIgnoreCase(cashRegister)) continue;

            if (start != null && op.getDateTime().toLocalDate().isBefore(start)) continue;
            if (end != null && op.getDateTime().toLocalDate().isAfter(end)) continue;

            // Filtres hiérarchiques scolaires
            if (section != null && !section.isBlank() && !"Toutes".equalsIgnoreCase(section)) {
                if (!section.equalsIgnoreCase(op.getSectionName())) continue;
            }
            if (promotion != null && !promotion.isBlank() && !"Toutes".equalsIgnoreCase(promotion)) {
                if (!promotion.equalsIgnoreCase(op.getPromotionName())) continue;
            }
            if (option != null && !option.isBlank() && !"Toutes".equalsIgnoreCase(option)) {
                if (!option.equalsIgnoreCase(op.getOptionName())) continue;
            }
            if (classe != null && !classe.isBlank() && !"Toutes".equalsIgnoreCase(classe)) {
                if (!classe.equalsIgnoreCase(op.getClassName())) continue;
            }

            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.toLowerCase();
                boolean match = op.getReferenceNumber().toLowerCase().contains(kw)
                        || op.getDescription().toLowerCase().contains(kw)
                        || (op.getBeneficiaryName() != null && op.getBeneficiaryName().toLowerCase().contains(kw))
                        || (op.getStudentMatricule() != null && op.getStudentMatricule().toLowerCase().contains(kw))
                        || (op.getTransactionReference() != null && op.getTransactionReference().toLowerCase().contains(kw));
                if (!match) continue;
            }

            results.add(op);
        }
        return results;
    }

    /**
     * Annule une opération.
     * RÈGLE ABSOLUE : Pas de DELETE physique.
     * L'opération passe au statut ANNULEE et consigne le motif et l'auteur.
     */
    public void cancelOperation(int id, String reason, String authorizedBy) {
        for (Operation op : operations) {
            if (op.getId() == id) {
                if (op.getStatus() == Operation.OperationStatus.ANNULEE) {
                    throw new IllegalStateException("Cette opération est déjà annulée.");
                }
                op.setStatus(Operation.OperationStatus.ANNULEE);
                op.setCancelReason(reason);
                op.setCancelledBy(authorizedBy);
                op.setCancelledAt(LocalDateTime.now());
                return;
            }
        }
    }

    /**
     * Corrige une opération.
     * L'opération originale passe à CORRIGEE, et une nouvelle opération de rectification est créée.
     */
    public Operation correctOperation(int originalId, double newAmount, String reason, String authorizedBy) {
        Operation original = null;
        for (Operation op : operations) {
            if (op.getId() == originalId) {
                original = op;
                break;
            }
        }

        if (original == null) {
            throw new IllegalArgumentException("Opération introuvable.");
        }

        original.setStatus(Operation.OperationStatus.CORRIGEE);
        original.setCorrectionNotes("Corrigée par " + authorizedBy + ". Motif: " + reason);

        // Créer l'opération rectificative
        int newId = operations.size() + 1;
        Operation corrective = new Operation(
                newId,
                "CORR-" + original.getReferenceNumber(),
                original.getType(),
                "Correction de " + original.getReferenceNumber() + " : " + original.getDescription(),
                newAmount,
                original.getCurrency(),
                original.getPaymentMethod(),
                original.getTransactionReference(),
                authorizedBy,
                original.getCashRegisterName(),
                LocalDateTime.now(),
                Operation.OperationStatus.VALIDEE
        );
        corrective.setCorrectedOperationId(original.getId());
        corrective.setCorrectionNotes("Nouvelle valeur suite à correction de " + original.getReferenceNumber());
        operations.add(corrective);

        return corrective;
    }

    public void recordOperation(Operation op) {
        if (op == null) return;
        if (op.getId() == 0) {
            op.setId(operations.size() + 1);
        }
        operations.add(op);
    }

    public int getNextOperationSequence() {
        return operations.size() + 1;
    }

    public Operation getById(int id) {
        for (Operation op : operations) {
            if (op.getId() == id) return op;
        }
        return null;
    }

    public List<Operation> getAllOperations() {
        return new ArrayList<>(operations);
    }
}
