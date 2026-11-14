package com.paulo.ecommerce.controller;
import com.paulo.ecommerce.service.DigitalService;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/account/digitais")
public class DigitalApiController {
 private final DigitalService digitais;
 public DigitalApiController(DigitalService d) {digitais=d;}
 @GetMapping public ResponseEntity<?> listar(Principal user) {
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(digitais.listar(user.getName()));
 }
 @PostMapping("/{id}/codigo") public ResponseEntity<?> codigo(@PathVariable Long id,Principal user) {
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of("codigo",digitais.revelar(user.getName(),id)));
 }
 @GetMapping("/{id}/arquivo") public ResponseEntity<byte[]> arquivo(@PathVariable Long id,Principal user) {
  var a=digitais.baixar(user.getName(),id);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
    .header("Content-Security-Policy","default-src 'none'; sandbox")
    .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(a.nome).build().toString())
    .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(a.dados.length).body(a.dados);
 }
}
