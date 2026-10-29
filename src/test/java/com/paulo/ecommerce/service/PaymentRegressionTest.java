package com.paulo.ecommerce.service;
import com.paulo.ecommerce.model.*;
import com.paulo.ecommerce.repository.PagamentoRepository;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PaymentRegressionTest {
 @Test void checksAmountBeforeGivingXpAndIgnoresRepeatedApproval() throws Exception {
  var repo=mock(PagamentoRepository.class);var orders=mock(PedidoService.class);var passes=mock(PasseService.class);
  var service=spy(new PagamentoService(repo,orders,mock(EmailService.class),passes));
  ReflectionTestUtils.setField(service,"paymentsEnabled",true);ReflectionTestUtils.setField(service,"collectorId","123");
  var order=new Pedido();order.setId(10L);order.setTotal(new BigDecimal("124.90"));order.setSubtotal(new BigDecimal("100"));
  var user=new Usuario();user.setEmail("a@example.test");order.setUsuario(user);
  var payment=new Pagamento();payment.setPedido(order);order.setPagamento(payment);
  when(orders.bloquearPedido(10L)).thenReturn(order);when(repo.findByPedidoId(10L)).thenReturn(Optional.of(payment));
  var mp=mock(Payment.class);when(mp.getExternalReference()).thenReturn("10");when(mp.getCurrencyId()).thenReturn("BRL");when(mp.getCollectorId()).thenReturn(123L);when(mp.isLiveMode()).thenReturn(false);when(mp.getStatus()).thenReturn("approved");when(mp.getTransactionAmount()).thenReturn(new BigDecimal("100"));
  doReturn(mp).when(service).consultarPagamento(1L);
  assertThrows(IllegalStateException.class,()->service.processarWebhook("1","payment"));verifyNoInteractions(passes);
  when(mp.getTransactionAmount()).thenReturn(new BigDecimal("124.90"));service.processarWebhook("1","payment");service.processarWebhook("1","payment");
  verify(passes,times(1)).registrarXpCompraAprovada("a@example.test",10L,new BigDecimal("100"));
  assertEquals(Pagamento.StatusPagamento.APROVADO,payment.getStatus());
 }
}
