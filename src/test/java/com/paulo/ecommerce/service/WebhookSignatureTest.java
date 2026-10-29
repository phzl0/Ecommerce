package com.paulo.ecommerce.service;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
class WebhookSignatureTest {
 @Test void checksSignatureIdentityAndTimestamp() throws Exception {
  var validator=new WebhookSignature();ReflectionTestUtils.setField(validator,"secret","test-only-secret");
  String ts=String.valueOf(Instant.now().getEpochSecond());String manifest="id:123;request-id:request-1;ts:"+ts+";";
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec("test-only-secret".getBytes(),"HmacSHA256"));
  String signature="ts="+ts+",v1="+HexFormat.of().formatHex(mac.doFinal(manifest.getBytes()));
  assertTrue(validator.valid(signature,"request-1","123"));
  assertFalse(validator.valid(signature,"request-2","123"));assertFalse(validator.valid(signature,"request-1","124"));
  assertFalse(validator.valid("ts=1,v1=00","request-1","123"));assertFalse(validator.valid(null,"request-1","123"));
 }
}
