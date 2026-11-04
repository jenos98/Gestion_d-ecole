package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.SystemNotification;
import com.jenos.gestionecole.util.Route;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NotificationService — Gestionnaire des alertes et notifications du système.
 *
 * RÈGLE ABSOLUE :
 * Les notifications de priorité CRITICAL (ex: rapport de caisse non généré avant clôture,
 * dépassement budgétaire > 100%) ne peuvent pas être supprimées sans résolution effective.
 */
public class NotificationService {

    private static NotificationService instance;

    public interface NotificationObserver {
        void onNotificationsChanged();
    }

    private final List<SystemNotification> notifications = new ArrayList<>();
    private final List<NotificationObserver> observers = new ArrayList<>();
    private int idCounter = 1;

    private NotificationService() {
        initDefaultNotifications();
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    private void initDefaultNotifications() {
        // Notification informative
        addNotification(new SystemNotification(
                idCounter++,
                SystemNotification.Priority.INFO,
                "Année Scolaire Active",
                "L'année académique 2024-2025 est actuellement définie comme l'exercice de référence.",
                LocalDateTime.now().minusHours(2),
                Route.ACADEMIC_YEAR
        ));

        // Avertissement budgétaire
        addNotification(new SystemNotification(
                idCounter++,
                SystemNotification.Priority.WARNING,
                "Seuil Budgétaire Approché",
                "La ligne budgétaire 'Maintenance & Travaux' a atteint 85% de sa dotation en Francs Congolais (CDF).",
                LocalDateTime.now().minusHours(1),
                Route.BUDGET
        ));

        // Alerte critique de caisse
        addNotification(new SystemNotification(
                idCounter++,
                SystemNotification.Priority.CRITICAL,
                "Rapport Journalier de Caisse Requis",
                "La Caisse Principale 01 a des encaissements en cours. Le rapport journalier doit être généré et imprimé avant la clôture.",
                LocalDateTime.now().minusMinutes(20),
                Route.CASH_REGISTER
        ));
    }

    public synchronized List<SystemNotification> getAllNotifications() {
        return new ArrayList<>(notifications);
    }

    public synchronized List<SystemNotification> getUnreadNotifications() {
        return notifications.stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    public synchronized int getUnreadCount() {
        return (int) notifications.stream().filter(n -> !n.isRead()).count();
    }

    public synchronized List<SystemNotification> getByPriority(SystemNotification.Priority priority) {
        if (priority == null) return getAllNotifications();
        return notifications.stream()
                .filter(n -> n.getPriority() == priority)
                .collect(Collectors.toList());
    }

    public synchronized void addNotification(SystemNotification notification) {
        if (notification == null) return;
        if (notification.getId() == 0) {
            notification.setId(idCounter++);
        }
        notifications.add(0, notification); // Plus récente en tête
        notifyObservers();
    }

    public synchronized SystemNotification createAlert(SystemNotification.Priority priority, String title, String message, Route actionRoute) {
        SystemNotification n = new SystemNotification(idCounter++, priority, title, message, LocalDateTime.now(), actionRoute);
        addNotification(n);
        return n;
    }

    public synchronized void markAsRead(int notificationId) {
        for (SystemNotification n : notifications) {
            if (n.getId() == notificationId) {
                n.setRead(true);
                notifyObservers();
                break;
            }
        }
    }

    public synchronized void markAllAsRead() {
        for (SystemNotification n : notifications) {
            n.setRead(true);
        }
        notifyObservers();
    }

    /**
     * Supprime une notification si autorisée.
     * RÈGLE ABSOLUE : Une alerte CRITIQUE ne peut pas être supprimée arbitrairement.
     */
    public synchronized boolean dismissNotification(int notificationId) {
        for (int i = 0; i < notifications.size(); i++) {
            SystemNotification n = notifications.get(i);
            if (n.getId() == notificationId) {
                if (n.getPriority() == SystemNotification.Priority.CRITICAL) {
                    throw new IllegalStateException("Les alertes critiques requièrent la résolution de l'incident (action requise).");
                }
                notifications.remove(i);
                notifyObservers();
                return true;
            }
        }
        return false;
    }

    public void addObserver(NotificationObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(NotificationObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        for (NotificationObserver observer : observers) {
            try {
                observer.onNotificationsChanged();
            } catch (Exception ignored) {}
        }
    }

    /** Pour les tests */
    public synchronized void resetForTests() {
        notifications.clear();
        observers.clear();
        idCounter = 1;
        initDefaultNotifications();
    }
}
