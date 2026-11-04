package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.util.RoleGuard;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService — Gestion des comptes utilisateurs et de leurs accès.
 * RÈGLE ABSOLUE :
 * - Pas de suppression physique d'un utilisateur (archivage / désactivation).
 * - Caisse unique par caissière active.
 */
public class UserService {

    private static UserService instance;
    private final List<User> users = new ArrayList<>();

    private UserService() {
        initMockUsers();
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    private void initMockUsers() {
        users.add(new User(1, "MUTOMBO", "KASENDE", "Jenos", "admin_promoteur",
                "promoteur@ecolejenos.cd", "+243 81 111 1111", RoleGuard.Role.PROMOTEUR, null,
                User.UserStatus.ACTIF, LocalDateTime.now().minusHours(1), hashPassword("Admin1234!")));

        users.add(new User(2, "KABANGE", "MPOYI", "Sarah", "caissiere01",
                "sarah.k@ecolejenos.cd", "+243 82 222 2222", RoleGuard.Role.CAISSIERE, "Caisse Principale 01",
                User.UserStatus.ACTIF, LocalDateTime.now().minusMinutes(30), hashPassword("Caisse1234!")));

        users.add(new User(3, "ILUNGA", "TSHIMANGA", "David", "directeur",
                "david.i@ecolejenos.cd", "+243 83 333 3333", RoleGuard.Role.DIRECTEUR, null,
                User.UserStatus.ACTIF, LocalDateTime.now().minusDays(1), hashPassword("Directeur1234!")));

        users.add(new User(4, "NGOYI", "BANZA", "Clarisse", "secretariat",
                "clarisse.n@ecolejenos.cd", "+243 84 444 4444", RoleGuard.Role.SECRETARIAT, null,
                User.UserStatus.ACTIF, LocalDateTime.now().minusHours(5), hashPassword("Secretaire1234!")));

        users.add(new User(5, "LUMUMBA", "KASONGO", "Marc", "admin_tech",
                "tech@ecolejenos.cd", "+243 85 555 5555", RoleGuard.Role.ADMIN_TECHNIQUE, null,
                User.UserStatus.ACTIF, LocalDateTime.now().minusDays(2), hashPassword("Technique1234!")));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public List<User> search(String keyword, RoleGuard.Role role, User.UserStatus status, String cashRegister) {
        return users.stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> status == null || u.getStatus() == status)
                .filter(u -> cashRegister == null || cashRegister.isBlank() || "Toutes les caisses".equalsIgnoreCase(cashRegister)
                        || (u.getCashRegisterCode() != null && u.getCashRegisterCode().equalsIgnoreCase(cashRegister)))
                .filter(u -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String kw = keyword.toLowerCase().trim();
                    return u.getFullName().toLowerCase().contains(kw)
                            || u.getUsername().toLowerCase().contains(kw)
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw))
                            || (u.getPhone() != null && u.getPhone().toLowerCase().contains(kw));
                })
                .collect(Collectors.toList());
    }

    public User findById(int id) {
        return users.stream().filter(u -> u.getId() == id).findFirst().orElse(null);
    }

    public User findByUsername(String username) {
        if (username == null) return null;
        return users.stream().filter(u -> username.equalsIgnoreCase(u.getUsername())).findFirst().orElse(null);
    }

    /**
     * Crée un utilisateur avec contrôle d'unicité de username et de caisse active.
     */
    public User createUser(User user, String plainPassword) {
        if (user == null) throw new IllegalArgumentException("Utilisateur requis.");
        if (findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("Le nom d'utilisateur '" + user.getUsername() + "' est déjà attribué.");
        }
        if (user.getRole() == RoleGuard.Role.CAISSIERE && user.getCashRegisterCode() != null && !user.getCashRegisterCode().isBlank()) {
            validateCashRegisterAssignment(user.getCashRegisterCode(), 0);
        }

        user.setId(users.size() + 1);
        user.setPasswordHash(hashPassword(plainPassword));
        user.setStatus(User.UserStatus.ACTIF);
        users.add(user);

        AuditService.getInstance().logAction("admin", "CRÉATION_UTILISATEUR", "User", null, user.getUsername() + " (" + user.getRole() + ")");
        return user;
    }

    /**
     * Met à jour un utilisateur.
     */
    public void updateUser(User user) {
        User existing = findById(user.getId());
        if (existing == null) throw new IllegalArgumentException("Utilisateur non trouvé.");

        if (user.getRole() == RoleGuard.Role.CAISSIERE && user.getCashRegisterCode() != null && !user.getCashRegisterCode().isBlank()) {
            validateCashRegisterAssignment(user.getCashRegisterCode(), user.getId());
        }

        existing.setNom(user.getNom());
        existing.setPostnom(user.getPostnom());
        existing.setPrenom(user.getPrenom());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setRole(user.getRole());
        existing.setCashRegisterCode(user.getCashRegisterCode());
        existing.setStatus(user.getStatus());

        AuditService.getInstance().logAction("admin", "MODIFICATION_UTILISATEUR", "User", existing.getUsername(), "Mise à jour des informations");
    }

    /**
     * RÈGLE ABSOLUE : Pas de suppression physique. Désactive le compte.
     */
    public void deactivateUser(int id, String reason) {
        User user = findById(id);
        if (user == null) throw new IllegalArgumentException("Utilisateur introuvable.");
        user.setStatus(User.UserStatus.DESACTIVE);
        AuditService.getInstance().logAction("admin", "DÉSACTIVATION_UTILISATEUR", "User", user.getUsername(), "Désactivé : " + reason);
    }

    public void blockUser(int id) {
        User user = findById(id);
        if (user == null) throw new IllegalArgumentException("Utilisateur introuvable.");
        user.setStatus(User.UserStatus.BLOQUE);
        AuditService.getInstance().logAction("admin", "BLOCAGE_UTILISATEUR", "User", user.getUsername(), "Compte bloqué");
    }

    public void activateUser(int id) {
        User user = findById(id);
        if (user == null) throw new IllegalArgumentException("Utilisateur introuvable.");
        user.setStatus(User.UserStatus.ACTIF);
        AuditService.getInstance().logAction("admin", "ACTIVATION_UTILISATEUR", "User", user.getUsername(), "Compte réactivé");
    }

    public void resetPassword(int id, String newPassword) {
        User user = findById(id);
        if (user == null) throw new IllegalArgumentException("Utilisateur introuvable.");
        user.setPasswordHash(hashPassword(newPassword));
        AuditService.getInstance().logAction("admin", "RÉINITIALISATION_MDP", "User", user.getUsername(), "Nouveau mot de passe configuré");
    }

    /**
     * Valide qu'une même caisse active n'est pas déjà affectée à une autre caissière active.
     */
    public void validateCashRegisterAssignment(String cashRegisterCode, int excludeUserId) {
        for (User u : users) {
            if (u.getId() != excludeUserId && u.getStatus() == User.UserStatus.ACTIF && u.getRole() == RoleGuard.Role.CAISSIERE) {
                if (cashRegisterCode.equalsIgnoreCase(u.getCashRegisterCode())) {
                    throw new IllegalStateException("La caisse '" + cashRegisterCode + "' est déjà assignée à la caissière active : " + u.getFullName());
                }
            }
        }
    }

    public boolean verifyPassword(User user, String plainPassword) {
        if (user == null || user.getPasswordHash() == null) return false;
        return user.getPasswordHash().equals(hashPassword(plainPassword));
    }

    public String hashPassword(String plain) {
        if (plain == null || plain.isBlank()) return "HASH_DEFAULT";
        return "SHA256:" + Integer.toHexString(plain.hashCode());
    }
}
