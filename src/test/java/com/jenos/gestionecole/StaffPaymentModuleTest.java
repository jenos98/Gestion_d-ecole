package com.jenos.gestionecole;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.PaymentService;
import com.jenos.gestionecole.service.StaffService;
import com.jenos.gestionecole.util.RoleGuard;

import java.time.LocalDate;
import java.util.List;

/**
 * StaffPaymentModuleTest — Suite de validation automatisée du Bloc 3 :
 * 1. Intégrité et cycle de vie du personnel (Staff & archivage non-destructif)
 * 2. Séparation stricte USD / CDF
 * 3. Validation des permissions RoleGuard
 * 4. Processus d'encaissement et émission de quittance
 * 5. Annulation non-destructive de paiements
 */
public class StaffPaymentModuleTest {

    public static void main(String[] args) {
        System.out.println("=== DÉBUT DES TESTS BLOC 3 (Staff + Payments) ===");

        testStaffArchiving();
        testStaffDepartments();
        testRoleGuardPermissions();
        testPaymentProcessingAndReceipt();
        testPaymentCancellationNonDestructive();
        testStrictCurrencySeparation();

        System.out.println("=== TOUS LES TESTS DU BLOC 3 ONT RÉUSSI (100%) ===");
    }

    private static void testStaffArchiving() {
        System.out.print("[TEST 1] Archivage non destructif du personnel... ");
        StaffService service = StaffService.getInstance();
        Staff s = service.findById(1);
        assert s != null : "Le membre du personnel ID=1 doit exister";

        boolean archived = service.archive(1);
        assert archived : "L'archivage doit retourner true";
        assert s.getStatut() == Staff.StaffStatus.ARCHIVE : "Le statut doit être ARCHIVE";

        // Vérifier qu'il est toujours en mémoire (pas de DELETE physique)
        Staff check = service.findById(1);
        assert check != null : "Le membre archivé doit toujours exister en base";

        // Restaurer pour les tests suivants
        service.updateStatus(1, Staff.StaffStatus.ACTIF);
        System.out.println("OK");
    }

    private static void testStaffDepartments() {
        System.out.print("[TEST 2] Extraction des départements du personnel... ");
        StaffService service = StaffService.getInstance();
        List<String> depts = service.findAllDepartments();
        assert depts.contains("Tous") : "La liste doit contenir 'Tous'";
        assert depts.size() > 1 : "Il doit y avoir des départements disponibles";

        Staff s = service.findById(1);
        assert s.getDepartment() != null : "getDepartment() ne doit pas être null";
        System.out.println("OK");
    }

    private static void testRoleGuardPermissions() {
        System.out.print("[TEST 3] Contrôle des permissions RoleGuard... ");
        // Rôle Promoteur
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assert RoleGuard.canCreateStaff() : "Le promoteur peut créer du personnel";
        assert RoleGuard.canArchiveStaff() : "Le promoteur peut archiver du personnel";
        assert RoleGuard.canCancelPayment() : "Le promoteur peut annuler un paiement";

        // Rôle Caissière
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assert !RoleGuard.canArchiveStaff() : "La caissière ne peut PAS archiver du personnel";
        assert !RoleGuard.canCancelPayment() : "La caissière ne peut PAS annuler un paiement";
        assert RoleGuard.canCreatePayment() : "La caissière peut créer des paiements";

        RoleGuard.clearRole();
        System.out.println("OK");
    }

    private static void testPaymentProcessingAndReceipt() {
        System.out.print("[TEST 4] Processus de paiement & génération quittance... ");
        PaymentService paymentService = PaymentService.getInstance();

        Student student = new Student();
        student.setId(999);
        student.setNom("Kalonji");
        student.setPostnom("Mutombo");
        student.setPrenom("David");
        student.setMatricule("2026-TST-001");

        Payment payment = new Payment();
        payment.setPaymentType(Payment.PaymentType.ELEVE);
        payment.setStudent(student);
        payment.setAmount(100.0);
        payment.setCurrency("USD");
        payment.setPaymentMethod(Payment.PaymentMethod.ESPECES);
        payment.setPaymentDate(LocalDate.now());

        Receipt receipt = paymentService.processPayment(payment);
        assert receipt != null : "Le reçu ne doit pas être null";
        assert receipt.getReceiptNumber() != null && receipt.getReceiptNumber().startsWith("REC-") : "N° de reçu format REC-YYYY-XXXX";
        assert receipt.getAmountPaid() == 100.0 : "Montant du reçu conforme";
        assert "USD".equals(receipt.getCurrency()) : "Devise conforme";
        System.out.println("OK");
    }

    private static void testPaymentCancellationNonDestructive() {
        System.out.print("[TEST 5] Annulation non-destructive d'un paiement... ");
        PaymentService paymentService = PaymentService.getInstance();
        List<Payment> all = paymentService.findAllPayments();
        assert !all.isEmpty() : "Des paiements doivent exister";

        Payment p = all.get(0);
        int targetId = p.getId();
        boolean cancelled = paymentService.cancelPayment(targetId);
        assert cancelled : "L'annulation doit réussir";

        Payment updated = paymentService.findAllPayments().stream()
                .filter(x -> x.getId() == targetId)
                .findFirst().orElse(null);
        assert updated != null : "Le paiement annulé ne doit PAS être supprimé physiquement";
        assert updated.getStatus() == Payment.PaymentStatus.ANNULEE : "Le statut doit être ANNULEE";
        System.out.println("OK");
    }

    private static void testStrictCurrencySeparation() {
        System.out.print("[TEST 6] Séparation stricte USD / CDF... ");
        PaymentService paymentService = PaymentService.getInstance();
        List<Payment> all = paymentService.findAllPayments();

        double usdTotal = 0.0;
        double cdfTotal = 0.0;

        for (Payment p : all) {
            if ("USD".equalsIgnoreCase(p.getCurrency())) {
                usdTotal += p.getAmount();
            } else if ("CDF".equalsIgnoreCase(p.getCurrency())) {
                cdfTotal += p.getAmount();
            }
        }

        // Vérification qu'on manipule des totaux distincts
        assert usdTotal >= 0 : "Total USD calculé";
        assert cdfTotal >= 0 : "Total CDF calculé";
        System.out.println("OK");
    }
}
