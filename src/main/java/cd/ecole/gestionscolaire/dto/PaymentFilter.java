package cd.ecole.gestionscolaire.dto;

import java.time.LocalDate;

/**
 * PaymentFilter — Critères de recherche et de filtrage pour les paiements.
 */
public class PaymentFilter {

    private String searchTerm; // Nom, matricule, numéro de reçu
    private String academicYear;
    private Long sectionId;
    private Long promotionId;
    private Long optionId;
    private Long classeId;
    private String currency; // 'USD' ou 'CDF'
    private String paymentMethod;
    private String status; // 'VALIDEE', 'ANNULEE'
    private LocalDate startDate;
    private LocalDate endDate;
    private String cashierUsername;

    public PaymentFilter() {}

    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }

    public Long getClasseId() { return classeId; }
    public void setClasseId(Long classeId) { this.classeId = classeId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }
}
