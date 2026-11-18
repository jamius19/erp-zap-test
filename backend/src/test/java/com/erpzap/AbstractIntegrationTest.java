/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base for integration tests: one shared Postgres container (Testcontainers) for the whole test JVM.
 * Seeding is disabled; tests create their own data.
 */
@SpringBootTest
@TestPropertySource(properties = {"app.seed.enabled=false", "app.upload-dir=build/test-uploads"})
public abstract class AbstractIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	static {
		POSTGRES.start();
	}
}
