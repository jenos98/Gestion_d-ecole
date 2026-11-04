package com.jenos.gestionecole.model;

import com.jenos.gestionecole.util.RoleGuard;

import java.time.LocalDateTime;

/**
 * User — Compte utilisateur du système de gestion.
 * Gère l'identité, le rôle, l'éventuelle caisse de rattachement et le statut.
 * RÈGLE ABSOLUE : Pas de suppression physique d'un utilisateur ayant opéré (désactivation).
 */
public class User {

    public enum UserStatus {
        ACTIF,
        BLOQUE,
        DESACTIVE
    }

    private int id;
    private String nom;
    private String postnom;
    private String prenom;
    private String username;
    private String email;
    private String phone;
    private RoleGuard.Role role;
    private String cashRegisterCode; // null ou ex: "Caisse 01" si Caissière
    private UserStatus status;
    private LocalDateTime lastLogin;
    private String passwordHash; // Ne jamais afficher en clair

    public User() {
        this.status = UserStatus.ACTIF;
    }

    public User(int id, String nom, String postnom, String prenom, String username,
                String email, String phone, RoleGuard.Role role, String cashRegisterCode,
                UserStatus status, LocalDateTime lastLogin, String passwordHash) {
        this.id = id;
        this.nom = nom;
        this.postnom = postnom;
        this.prenom = prenom;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.cashRegisterCode = cashRegisterCode;
        this.status = status;
        this.lastLogin = lastLogin;
        this.passwordHash = passwordHash;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPostnom() { return postnom; }
    public void setPostnom(String postnom) { this.postnom = postnom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (nom != null) sb.append(nom).append(" ");
        if (postnom != null) sb.append(postnom).append(" ");
        if (prenom != null) sb.append(prenom);
        return sb.toString().trim();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public RoleGuard.Role getRole() { return role; }
    public void setRole(RoleGuard.Role role) { this.role = role; }

    public String getCashRegisterCode() { return cashRegisterCode; }
    public void setCashRegisterCode(String cashRegisterCode) { this.cashRegisterCode = cashRegisterCode; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @Override
    public String toString() {
        return getFullName() + " (" + username + " - " + role + ")";
    }
}
