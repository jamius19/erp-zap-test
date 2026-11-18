/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Paging (stable JSON shape, max page size 100) and the OpenAPI bearer security scheme.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@OpenAPIDefinition(info = @Info(title = "School ERP API", version = "v1"),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class WebConfig {

	@Bean
	public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
		return resolver -> resolver.setMaxPageSize(100);
	}
}
