package com.jenos.gestionecole.util;

/**
 * Route — Énumération de toutes les routes de navigation.
 *
 * Toute navigation dans l'application doit utiliser cette enum.
 * Cela évite les erreurs de chemin FXML dispersées dans les contrôleurs.
 *
 * Usage :
 *   NavigationManager.getInstance().navigate(Route.STUDENTS);
 */
public enum Route {

    DASHBOARD   ("dashboard.fxml"),
    STUDENTS    ("students.fxml"),
    STUDENT_FORM("student-form.fxml"),
    ENROLLMENT  ("enrollment.fxml"),
    STUDENT_DETAILS("student-details.fxml"),
    STUDENT_HISTORY("student-history.fxml"),
    STAFF          ("staff.fxml"),
    STAFF_FORM     ("staff-form.fxml"),
    STAFF_DETAILS  ("staff-details.fxml"),
    FEES           ("fees.fxml"),
    PAYMENTS       ("payments.fxml"),
    RECEIPT_PREVIEW("receipt-preview.fxml"),
    PAYSLIP_PREVIEW("payslip-preview.fxml"),
    EXPENSES       ("expenses.fxml"),
    CASH           ("cash-register.fxml"),
    CASH_REGISTER  ("cash-register.fxml"),
    DAILY_REPORT   ("daily-report.fxml"),
    DAILY_REPORT_PREVIEW("daily-report-preview.fxml"),
    HISTORY        ("history.fxml"),
    REPORTS        ("reports.fxml"),
    REPORT_PREVIEW ("report-preview.fxml"),
    SETTINGS       ("settings.fxml"),
    OPTIONS        ("options.fxml"),
    GUIDE          ("guide.fxml"),
    LOGIN          ("login.fxml"),

    // Administration & Configuration (Bloc 6)
    USERS               ("users.fxml"),
    USER_FORM           ("user-form.fxml"),
    ROLES_PERMISSIONS   ("roles-permissions.fxml"),
    SCHOOL_SETTINGS     ("school-settings.fxml"),
    ACADEMIC_YEAR       ("academic-year.fxml"),
    SECTIONS            ("sections.fxml"),
    PROMOTIONS          ("promotions.fxml"),
    CLASSES             ("classes.fxml"),
    PAYMENT_METHODS     ("payment-methods.fxml"),
    BUDGET              ("budget.fxml"),

    // Écrans Système & Socle Transversal (Bloc 7)
    NOTIFICATIONS        ("notifications.fxml"),
    PROFILE              ("profile.fxml"),
    AUDIT_LOG            ("audit-log.fxml"),

    // Dialogues
    CONFIRM_DIALOG       ("dialogs/confirm-dialog.fxml"),
    PASSWORD_DIALOG      ("dialogs/password-dialog.fxml"),
    CLOSE_CASH_DIALOG    ("dialogs/cash-closing-dialog.fxml"),
    CASH_CLOSING_DIALOG  ("dialogs/cash-closing-dialog.fxml"),
    OPERATION_DETAIL     ("dialogs/operation-detail-dialog.fxml"),
    PRINT_PREVIEW_DIALOG ("dialogs/print-preview-dialog.fxml"),
    EXPORT_DIALOG        ("dialogs/export-dialog.fxml"),
    ERROR_DIALOG         ("dialogs/error-dialog.fxml");

    private final String fxmlPath;

    Route(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }
}
