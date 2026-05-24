package com.paulo.ecommerce.service;

import com.paulo.ecommerce.model.Pedido;
import com.paulo.ecommerce.model.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.remetente}")
    private String remetente;

    @Value("${app.url}")
    private String appUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarBoasVindas(Usuario usuario) {
        String assunto = "Bem-vindo a MinhaLoja!";
        String corpo = """
            <div style="font-family:sans-serif;max-width:600px;margin:0 auto;">
              <h2 style="color:#6c63ff;">Ola, %s!</h2>
              <p>Sua conta foi criada com sucesso.</p>
              <p>Agora voce pode explorar nossos produtos e fazer suas compras.</p>
              <a href="%s/produtos"
                 style="display:inline-block;background:#6c63ff;color:#fff;
                        padding:12px 24px;border-radius:6px;text-decoration:none;
                        margin-top:16px;">
                Ver produtos
              </a>
              <p style="margin-top:24px;color:#999;font-size:12px;">
                MinhaLoja - obrigado por se cadastrar!
              </p>
            </div>
            """.formatted(usuario.getNome(), appUrl);

        enviar(usuario.getEmail(), assunto, corpo);
    }

    @Async
    public void enviarConfirmacaoPedido(Pedido pedido) {
        String assunto = "Pedido " + pedido.getNumero() + " confirmado!";

        StringBuilder itensHtml = new StringBuilder();
        pedido.getItens().forEach(item ->
                itensHtml.append("""
                <tr>
                  <td style="padding:8px;border-bottom:1px solid #eee;">%s</td>
                  <td style="padding:8px;border-bottom:1px solid #eee;
                             text-align:center;">%d</td>
                  <td style="padding:8px;border-bottom:1px solid #eee;
                             text-align:right;">R$ %.2f</td>
                </tr>
                """.formatted(
                                item.getProduto().getNome(),
                                item.getQuantidade(),
                                item.getSubtotal()
                        )
                )
        );

        String corpo = """
            <div style="font-family:sans-serif;max-width:600px;margin:0 auto;">
              <h2 style="color:#6c63ff;">Pedido confirmado!</h2>
              <p>Ola, <strong>%s</strong>! Seu pedido foi recebido.</p>

              <div style="background:#f9f9f9;border-radius:8px;padding:16px;margin:16px 0;">
                <p style="margin:0;font-size:13px;color:#999;">Pedido</p>
                <p style="margin:4px 0;font-size:18px;font-weight:700;">%s</p>
              </div>

              <table style="width:100%%;border-collapse:collapse;">
                <thead>
                  <tr style="background:#f5f5f5;">
                    <th style="padding:8px;text-align:left;">Produto</th>
                    <th style="padding:8px;text-align:center;">Qtd</th>
                    <th style="padding:8px;text-align:right;">Subtotal</th>
                  </tr>
                </thead>
                <tbody>%s</tbody>
                <tfoot>
                  <tr>
                    <td colspan="2"
                        style="padding:12px 8px;font-weight:700;">Total</td>
                    <td style="padding:12px 8px;font-weight:700;
                               text-align:right;color:#6c63ff;">
                      R$ %.2f
                    </td>
                  </tr>
                </tfoot>
              </table>

              <p style="margin-top:16px;">
                <strong>Endereco de entrega:</strong><br>
                %s
              </p>

              <a href="%s/pedidos/%d"
                 style="display:inline-block;background:#6c63ff;color:#fff;
                        padding:12px 24px;border-radius:6px;text-decoration:none;
                        margin-top:16px;">
                Acompanhar pedido
              </a>
            </div>
            """.formatted(
                pedido.getUsuario().getNome(),
                pedido.getNumero(),
                itensHtml.toString(),
                pedido.getTotal(),
                pedido.getEnderecoEntrega(),
                appUrl,
                pedido.getId()
        );

        enviar(pedido.getUsuario().getEmail(), assunto, corpo);
    }

    @Async
    public void enviarAtualizacaoStatus(Pedido pedido) {
        String assunto = "Pedido " + pedido.getNumero() + " - Status atualizado";
        String corpo = """
            <div style="font-family:sans-serif;max-width:600px;margin:0 auto;">
              <h2 style="color:#6c63ff;">Status do seu pedido atualizado</h2>
              <p>Ola, <strong>%s</strong>!</p>
              <p>O status do seu pedido <strong>%s</strong>
                 foi atualizado para:</p>
              <div style="background:#f0efff;border-radius:8px;padding:16px;
                          margin:16px 0;text-align:center;">
                <span style="font-size:20px;font-weight:700;color:#6c63ff;">
                  %s
                </span>
              </div>
              <a href="%s/pedidos/%d"
                 style="display:inline-block;background:#6c63ff;color:#fff;
                        padding:12px 24px;border-radius:6px;text-decoration:none;">
                Ver pedido
              </a>
            </div>
            """.formatted(
                pedido.getUsuario().getNome(),
                pedido.getNumero(),
                pedido.getStatus(),
                appUrl,
                pedido.getId()
        );

        enviar(pedido.getUsuario().getEmail(), assunto, corpo);
    }

    private void enviar(String destinatario, String assunto, String corpo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}
