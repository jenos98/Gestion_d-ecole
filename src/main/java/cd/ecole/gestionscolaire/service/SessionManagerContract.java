package cd.ecole.gestionscolaire.service;

/**
 * SessionManagerContract — Contrat de gestion de l'état de session utilisateur et de caisse active.
 */
public interface SessionManagerContract {

    boolean isLoggedIn();

    String getCurrentUsername();

    String getCurrentRole();

    String getActiveCashRegisterCode();

    String getActiveAcademicYear();

    void setActiveAcademicYear(String academicYearCode);

    void logout();
}
