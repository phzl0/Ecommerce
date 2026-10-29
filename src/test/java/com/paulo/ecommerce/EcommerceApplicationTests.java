package com.paulo.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties={"spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create", "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/schema.sql"})
class EcommerceApplicationTests {

	@Test
	void contextLoads() {
	}

}
