package com.mka.controller;

import com.mka.repository.UserRepository;
import com.mka.service.AiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private AiService aiService;

    @Mock
    private UserRepository userRepository;

    private FileUploadController controller;
    private Path testFile;

    @BeforeEach
    void setUp() throws Exception {
        controller = new FileUploadController(aiService, userRepository);
        Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
        testFile = uploadRoot.resolve("path-security-test.png");
        Files.write(testFile, "valid-image-content".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(testFile);
    }

    @Test
    void getUploadedFile_ValidExistingFile() {
        ResponseEntity<byte[]> response = controller.getUploadedFile("path-security-test.png");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals("valid-image-content".getBytes(StandardCharsets.UTF_8), response.getBody());
    }

    @Test
    void getUploadedFile_NonexistentFilePreservesFallbackBehavior() {
        ResponseEntity<byte[]> response = controller.getUploadedFile("does-not-exist.png");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("image/svg+xml", response.getHeaders().getFirst("Content-Type"));
    }

    @Test
    void getUploadedFile_RejectsParentTraversal() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.getUploadedFile("../secret").getStatusCode());
    }

    @Test
    void getUploadedFile_RejectsMultipleTraversalLevels() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.getUploadedFile("../../../../secret").getStatusCode());
    }

    @Test
    void getUploadedFile_RejectsAbsolutePath() {
        String absolutePath = Paths.get("secret.txt").toAbsolutePath().normalize().toString();

        assertEquals(HttpStatus.BAD_REQUEST, controller.getUploadedFile(absolutePath).getStatusCode());
    }

    @Test
    void getUploadedFile_RejectsEncodedTraversal() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.getUploadedFile("%2e%2e%2fsecret").getStatusCode());
    }
}
