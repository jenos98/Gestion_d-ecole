package com.jenos.gestionecole.model;

import com.jenos.gestionecole.util.Route;

import java.time.LocalDateTime;

/**
 * SystemNotification — Modèle représentant une alerte ou notification système.
 */
public class SystemNotification {

    public enum Priority {
        INFO("Information", "badge-info"),
        WARNING("Avertissement", "badge-warning"),
        CRITICAL("Critique", "badge-danger");

        private final String label;
        private final String styleClass;

        Priority(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        public String getLabel() {
            return label;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }

    private int id;
    private Priority priority;
    private String title;
    private String message;
    private LocalDateTime timestamp;
    private boolean read;
    private Route actionRoute;

    public SystemNotification(int id, Priority priority, String title, String message, LocalDateTime timestamp, Route actionRoute) {
        this.id = id;
        this.priority = priority;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.read = false;
        this.actionRoute = actionRoute;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Route getActionRoute() {
        return actionRoute;
    }

    public void setActionRoute(Route actionRoute) {
        this.actionRoute = actionRoute;
    }
}
