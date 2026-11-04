package com.jenos.gestionecole.repository;

import com.jenos.gestionecole.model.SalaryAdvance;
import com.jenos.gestionecole.model.SalaryPayment;
import com.jenos.gestionecole.model.SalaryRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * SalaryRepository — Persistance et consultation des rémunérations, avances et paiements du personnel.
 */
public class SalaryRepository {

    private final List<SalaryRecord> recordsStorage = new ArrayList<>();
    private final List<SalaryAdvance> advancesStorage = new ArrayList<>();
    private final List<SalaryPayment> paymentsStorage = new ArrayList<>();

    private final AtomicInteger recordSeq = new AtomicInteger(100);
    private final AtomicInteger advanceSeq = new AtomicInteger(10);
    private final AtomicInteger paymentSeq = new AtomicInteger(50);

    public SalaryRepository() {
        initDefaultData();
    }

    private void initDefaultData() {
        // Avance initiale de test pour Staff ID 1 (Alain Mukendi) : 100 USD
        SalaryAdvance adv1 = new SalaryAdvance(
                advanceSeq.incrementAndGet(),
                1,
                "STF-2025-0001",
                "MUKENDI Alain",
                100.0,
                "USD",
                LocalDate.of(2026, 9, 10),
                "Avance sur salaire rentrée scolaire",
                "directeur"
        );
        advancesStorage.add(adv1);

        // Avance initiale de test en CDF pour Staff ID 5 (Pierre Ilunga) : 150 000 CDF
        SalaryAdvance adv2 = new SalaryAdvance(
                advanceSeq.incrementAndGet(),
                5,
                "STF-2025-0005",
                "ILUNGA Pierre",
                150000.0,
                "CDF",
                LocalDate.of(2026, 9, 12),
                "Avance soins médicaux",
                "directeur"
        );
        advancesStorage.add(adv2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉMUNÉRATIONS (SalaryRecord)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void saveRecord(SalaryRecord record) {
        if (record == null) return;
        if (record.getId() == 0) {
            record.setId(recordSeq.incrementAndGet());
            recordsStorage.add(record);
        } else {
            for (int i = 0; i < recordsStorage.size(); i++) {
                if (recordsStorage.get(i).getId() == record.getId()) {
                    recordsStorage.set(i, record);
                    return;
                }
            }
            recordsStorage.add(record);
        }
    }

    public synchronized Optional<SalaryRecord> findRecordById(int id) {
        return recordsStorage.stream().filter(r -> r.getId() == id).findFirst();
    }

    public synchronized Optional<SalaryRecord> findRecordByNumber(String recordNumber) {
        if (recordNumber == null) return Optional.empty();
        return recordsStorage.stream()
                .filter(r -> recordNumber.equalsIgnoreCase(r.getRecordNumber()))
                .findFirst();
    }

    public synchronized List<SalaryRecord> findRecordsByStaff(int staffId) {
        return recordsStorage.stream()
                .filter(r -> r.getStaffId() == staffId)
                .collect(Collectors.toList());
    }

    public synchronized List<SalaryRecord> findRecordsByPeriod(String period) {
        if (period == null || period.isBlank()) return new ArrayList<>(recordsStorage);
        return recordsStorage.stream()
                .filter(r -> period.equalsIgnoreCase(r.getPeriod()))
                .collect(Collectors.toList());
    }

    public synchronized List<SalaryRecord> findAllRecords() {
        return new ArrayList<>(recordsStorage);
    }

    public synchronized int countRecords() {
        return recordsStorage.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AVANCES SUR SALAIRE (SalaryAdvance)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void saveAdvance(SalaryAdvance advance) {
        if (advance == null) return;
        if (advance.getId() == 0) {
            advance.setId(advanceSeq.incrementAndGet());
            advancesStorage.add(advance);
        } else {
            for (int i = 0; i < advancesStorage.size(); i++) {
                if (advancesStorage.get(i).getId() == advance.getId()) {
                    advancesStorage.set(i, advance);
                    return;
                }
            }
            advancesStorage.add(advance);
        }
    }

    public synchronized Optional<SalaryAdvance> findAdvanceById(int id) {
        return advancesStorage.stream().filter(a -> a.getId() == id).findFirst();
    }

    public synchronized List<SalaryAdvance> findAdvancesByStaff(int staffId) {
        return advancesStorage.stream()
                .filter(a -> a.getStaffId() == staffId)
                .collect(Collectors.toList());
    }

    public synchronized List<SalaryAdvance> findActiveAdvancesByStaff(int staffId, String currency) {
        return advancesStorage.stream()
                .filter(a -> a.getStaffId() == staffId)
                .filter(a -> a.getStatus() == SalaryAdvance.AdvanceStatus.EN_COURS)
                .filter(a -> currency == null || currency.equalsIgnoreCase(a.getCurrency()))
                .collect(Collectors.toList());
    }

    public synchronized List<SalaryAdvance> findAllAdvances() {
        return new ArrayList<>(advancesStorage);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAIEMENTS (SalaryPayment)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void savePayment(SalaryPayment payment) {
        if (payment == null) return;
        if (payment.getId() == 0) {
            payment.setId(paymentSeq.incrementAndGet());
        }
        paymentsStorage.add(payment);
    }

    public synchronized List<SalaryPayment> findPaymentsByRecord(int salaryRecordId) {
        return paymentsStorage.stream()
                .filter(p -> p.getSalaryRecordId() == salaryRecordId)
                .collect(Collectors.toList());
    }

    public synchronized List<SalaryPayment> findAllPayments() {
        return new ArrayList<>(paymentsStorage);
    }
}
