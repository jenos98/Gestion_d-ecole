package com.jenos.gestionecole.util;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * MoneyVisibilityManager - Gestionnaire centralisé de la confidentialité des montants.
 *
 * Fonctionnement :
 * - Par défaut, les montants sont MASQUÉS (••••••).
 * - Après authentification, ils sont visibles.
 * - Un timer de 5 minutes d'inactivité les masque automatiquement.
 * - La session reste active; ce n'est PAS une reconnexion.
 *
 * Usage :
 *   MoneyVisibilityManager.getInstance().show();
 *   MoneyVisibilityManager.getInstance().hideAll();
 *   MoneyVisibilityManager.getInstance().registerObserver(myCard);
 */
public class MoneyVisibilityManager {

    public interface VisibilityObserver {
        void onShow();
        void onHide();
    }

    public enum State {
        VISIBLE,
        MASKED,
        UNAUTHORIZED
    }

    private static MoneyVisibilityManager instance;

    private State currentState = State.MASKED;
    private final List<VisibilityObserver> observers = new ArrayList<>();

    // Timer : 5 minutes d'inactivité -> masquage automatique
    private static final int AUTO_HIDE_SECONDS = 300;
    private PauseTransition autoHideTimer;

    private MoneyVisibilityManager() {
        initAutoHideTimer();
    }

    public static MoneyVisibilityManager getInstance() {
        if (instance == null) {
            instance = new MoneyVisibilityManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // ENREGISTREMENT DES OBSERVATEURS
    // -------------------------------------------------------------------------

    /** Enregistre un composant MoneyCard pour recevoir les notifications. */
    public void registerObserver(VisibilityObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(VisibilityObserver observer) {
        observers.remove(observer);
    }

    // -------------------------------------------------------------------------
    // ACTIONS PRINCIPALES
    // -------------------------------------------------------------------------

    /**
     * Affiche tous les montants.
     * Appelé après vérification du mot de passe (PasswordDialog).
     */
    public void showAll() {
        currentState = State.VISIBLE;
        notifyAll(true);
        resetAutoHideTimer();
    }

    /**
     * Masque tous les montants.
     * Appelé manuellement (btnHideAmounts) ou automatiquement par le timer.
     */
    public void hideAll() {
        currentState = State.MASKED;
        notifyAll(false);
        stopAutoHideTimer();
    }

    /**
     * Marque les montants comme non autorisés pour ce rôle.
     */
    public void setUnauthorized() {
        currentState = State.UNAUTHORIZED;
        notifyAll(false);
    }

    public boolean isVisible() {
        return currentState == State.VISIBLE;
    }

    public State getCurrentState() {
        return currentState;
    }

    // -------------------------------------------------------------------------
    // TIMER D'INACTIVITÉ
    // -------------------------------------------------------------------------

    private void initAutoHideTimer() {
        autoHideTimer = new PauseTransition(Duration.seconds(AUTO_HIDE_SECONDS));
        autoHideTimer.setOnFinished(event -> hideAll());
    }

    /**
     * Réinitialise le timer.
     * À appeler lors d'interactions utilisateur (scroll, clic, etc.)
     * depuis le contrôleur principal.
     */
    public void resetAutoHideTimer() {
        if (currentState == State.VISIBLE && autoHideTimer != null) {
            try {
                autoHideTimer.playFromStart();
            } catch (Exception ignored) {
                // Ignoré si le toolkit JavaFX n'est pas initialisé (environnement de test headless)
            }
        }
    }

    public void stopAutoHideTimer() {
        if (autoHideTimer != null) {
            try {
                autoHideTimer.stop();
            } catch (Exception ignored) {
            }
        }
    }

    /** Réinitialise complet : à appeler à la déconnexion. */
    public void reset() {
        hideAll();
        observers.clear();
    }

    // -------------------------------------------------------------------------
    // NOTIFICATION DES OBSERVATEURS
    // -------------------------------------------------------------------------

    private void notifyAll(boolean visible) {
        for (VisibilityObserver observer : observers) {
            if (visible) {
                observer.onShow();
            } else {
                observer.onHide();
            }
        }
    }
}
