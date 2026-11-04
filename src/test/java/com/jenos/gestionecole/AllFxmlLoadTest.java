package com.jenos.gestionecole;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllFxmlLoadTest {

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Platform already started
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    public void testAllFxmlCanBeParsed() throws IOException {
        Path fxmlRoot = Paths.get("src/main/resources/fxml");
        List<String> failedFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(fxmlRoot)) {
            paths.filter(p -> p.toString().endsWith(".fxml")).forEach(path -> {
                String relativePath = "/" + Paths.get("src/main/resources").relativize(path).toString().replace(File.separatorChar, '/');
                try {
                    URL resource = getClass().getResource(relativePath);
                    if (resource == null) {
                        failedFiles.add(relativePath + ": resource not found");
                        return;
                    }
                    FXMLLoader loader = new FXMLLoader(resource);
                    loader.load();
                } catch (Exception e) {
                    System.err.println("Failed to load: " + relativePath + " -> " + e.getMessage());
                    e.printStackTrace();
                    failedFiles.add(relativePath + ": " + e.getMessage());
                }
            });
        }

        if (!failedFiles.isEmpty()) {
            System.err.println("=== FAILED FXML FILES (" + failedFiles.size() + ") ===");
            failedFiles.forEach(System.err::println);
        }
        assertTrue(failedFiles.isEmpty(), "Some FXML files failed to load: " + failedFiles);
    }
}
