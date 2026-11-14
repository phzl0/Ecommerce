package com.paulo.ecommerce.service;

import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageStorage {
  @Value("${app.storage-dir:./storage}")
  private String directory;

  @Value("${app.uploads.enabled:true}")
  private boolean enabled;

  public String save(MultipartFile file) throws IOException {
    if (!enabled)
      throw new IllegalStateException(
          "Uploads desativados neste ambiente. Configure armazenamento persistente.");
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw new IllegalArgumentException("Envie uma imagem JPG ou PNG de até 5 MB.");
    try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw new IllegalArgumentException("Imagem inválida.");
      var reader = readers.next();
      try {
        reader.setInput(input);
        String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
        if (!format.equals("jpeg") && !format.equals("png"))
          throw new IllegalArgumentException("Use JPG ou PNG.");
        if ((long) reader.getWidth(0) * reader.getHeight(0) > 20000000)
          throw new IllegalArgumentException("A resolução da imagem é muito grande.");
        var image = reader.read(0);
        String name = java.util.UUID.randomUUID() + "." + (format.equals("jpeg") ? "jpg" : "png");
        Path root = Paths.get(directory, "products").toAbsolutePath().normalize();
        Files.createDirectories(root);
        if (!ImageIO.write(image, format, root.resolve(name).toFile()))
          throw new IOException("Não foi possível salvar a imagem.");
        return "/media/products/" + name;
      } finally {
        reader.dispose();
      }
    }
  }
}
