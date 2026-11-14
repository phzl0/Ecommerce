package com.paulo.ecommerce.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebhookSignature {
  @Value("${app.payments.webhook-secret:}")
  private String secret;

  public boolean valid(String signature, String requestId, String dataId) {
    if (secret == null
        || secret.isBlank()
        || signature == null
        || requestId == null
        || dataId == null) return false;
    try {
      String ts = null, v1 = null;
      for (String part : signature.split(",")) {
        String[] kv = part.trim().split("=", 2);
        if (kv.length == 2) {
          if (kv[0].equals("ts")) ts = kv[1];
          if (kv[0].equals("v1")) v1 = kv[1];
        }
      }
      if (ts == null || v1 == null) return false;
      long stamp = Long.parseLong(ts);
      if (stamp > 100000000000L) stamp /= 1000;
      if (Math.abs(Instant.now().getEpochSecond() - stamp) > 300) return false;
      String manifest =
          "id:"
              + dataId.toLowerCase(java.util.Locale.ROOT)
              + ";request-id:"
              + requestId
              + ";ts:"
              + ts
              + ";";
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return MessageDigest.isEqual(
          mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)), HexFormat.of().parseHex(v1));
    } catch (Exception e) {
      return false;
    }
  }
}
