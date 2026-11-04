package cd.ecole.gestionscolaire;

import javafx.application.Application;

/**
 * AppLauncher — Point d'entrée de démarrage de l'application.
 *
 * RÈGLE ARCHITECTURALE :
 * Cette classe ne doit PAS hériter de javafx.application.Application afin
 * de permettre le lancement direct depuis un JAR exécutable sans incident de modularité.
 * Elle ne contient aucune logique métier, aucune connexion SQL, ni règle de sécurité.
 */
public class AppLauncher {

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
