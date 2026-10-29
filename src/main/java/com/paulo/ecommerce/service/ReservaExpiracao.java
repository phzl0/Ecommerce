package com.paulo.ecommerce.service;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import com.paulo.ecommerce.repository.PedidoRepository;
import com.paulo.ecommerce.model.Pedido;
import java.time.LocalDateTime;
@Component
public class ReservaExpiracao {
 private final PedidoRepository repository; private final PedidoService pedidos;
 public ReservaExpiracao(PedidoRepository repository,PedidoService pedidos){this.repository=repository;this.pedidos=pedidos;}
 @Scheduled(fixedDelay=60000,initialDelay=60000)
 public void expire(){
  for(Pedido p:repository.findTop100ByStatusAndDataPedidoBeforeOrderByIdAsc(Pedido.Status.PENDENTE,LocalDateTime.now().minusHours(24))){
   try { pedidos.expirarReserva(p.getId()); } catch(RuntimeException error){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Reserva {} requer nova tentativa",p.getId());}
  }
 }
}
