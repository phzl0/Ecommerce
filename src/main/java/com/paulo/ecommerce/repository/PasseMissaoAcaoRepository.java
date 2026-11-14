package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.PasseMissaoAcao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PasseMissaoAcaoRepository extends JpaRepository<PasseMissaoAcao,Long>{
 boolean existsByUsuarioIdAndTemplateIdAndPeriodoInicioAndReferencia(Long usuario,Long template,java.time.LocalDateTime inicio,String referencia);
}
