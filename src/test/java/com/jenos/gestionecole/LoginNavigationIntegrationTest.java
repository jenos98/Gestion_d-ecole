package com.jenos.gestionecole;

import com.jenos.gestionecole.controller.MainLayoutController;
import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.SchoolSettingsService;
import com.jenos.gestionecole.service.SessionManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class LoginNavigationIntegrationTest {

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testCaissiereLoginAndMainLayoutLoad() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // 1. Simuler Stage
                Stage stage = new Stage();
                NavigationManager.getInstance().setPrimaryStage(stage);

                // 2. Login caissiere01
                SessionManager session = SessionManager.getInstance();
                session.resetForTests();
                User user = session.login("caissiere01", "Caisse1234!");
                assertNotNull(user, "User should be logged in");

                // 3. Charger MainLayout
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "MainLayout root must not be null");

                MainLayoutController mainController = loader.getController();
                assertNotNull(mainController, "MainLayoutController must not be null");

                String schoolName = SchoolSettingsService.getInstance().getSettings().getSchoolName();
                mainController.initUserInfo(user.getFullName(), user.getRole().name(), user.getCashRegisterCode());
                mainController.initSchoolInfo(schoolName);

                Scene scene = new Scene(root, 1366, 768);
                stage.setScene(scene);

                // 4. Tester la navigation vers d'autres pages pour la caissière
                NavigationManager.getInstance().navigate(Route.CASH);
                NavigationManager.getInstance().navigate(Route.PAYMENTS);
                NavigationManager.getInstance().navigate(Route.EXPENSES);
                NavigationManager.getInstance().navigate(Route.DASHBOARD);

            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timeout during JavaFX execution");
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
    }
}
