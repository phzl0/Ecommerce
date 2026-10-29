package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.*;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PasseEventoRepository extends JpaRepository<PasseEvento,Long> {
 boolean existsByUsuarioIdAndDiaAndAcaoAndReferencia(Long usuarioId,LocalDate dia,PasseAcaoTipo acao,String referencia);
}
