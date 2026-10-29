package com.paulo.ecommerce.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ProductImageStorageTest {
  @TempDir Path root;

  @Test
  void validatesImageAndNeverUsesTheOriginalPath() throws Exception {
    var service = new ProductImageStorage();
    ReflectionTestUtils.setField(service, "directory", root.toString());
    ReflectionTestUtils.setField(service, "enabled", true);
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", bytes);
    String url =
        service.save(
            new MockMultipartFile("image", "../../outside.png", "image/png", bytes.toByteArray()));
    assertTrue(url.matches("/media/products/[a-f0-9-]+\\.png"));
    assertEquals(1, Files.list(root.resolve("products")).count());
    assertFalse(Files.exists(root.resolve("outside.png")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.save(
                new MockMultipartFile(
                    "image", "script.svg", "image/svg+xml", "<svg/>".getBytes())));
  }
}
