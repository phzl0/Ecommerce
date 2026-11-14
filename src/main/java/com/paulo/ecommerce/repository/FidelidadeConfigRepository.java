package com.paulo.ecommerce.repository;
import com.paulo.ecommerce.model.FidelidadeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FidelidadeConfigRepository extends JpaRepository<FidelidadeConfig, Long> {

 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select c from FidelidadeConfig c where c.id=1")
 java.util.Optional<FidelidadeConfig> bloquearPublicacao();
}
