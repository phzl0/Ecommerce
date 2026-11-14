package com.paulo.ecommerce.config;

import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/** Content identifiers change automatically whenever the packaged admin assets change. */
@Component("adminAssets")
public class AdminAssets {
  private final String css = version("static/css/admin.css");
  private final String js = version("static/js/admin.js");
  public String getCss() { return css; }
  public String getJs() { return js; }
  private static String version(String path) {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return DigestUtils.md5DigestAsHex(input);
    } catch (IOException error) {
      throw new IllegalStateException("Recurso administrativo ausente: " + path, error);
    }
  }
}
