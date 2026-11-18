/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN;
import static org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER;

/**
 * Stateless JWT resource server. Two chains: a relaxed CSP for the Swagger UI, a strict one for everything else.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {

	private static final String API_CSP = "default-src 'none'; frame-ancestors 'none'";
	private static final String DOCS_CSP = "default-src 'self'; img-src 'self' data:; "
			+ "style-src 'self' 'unsafe-inline'; frame-ancestors 'none'";

	@Bean
	@Order(1)
	public SecurityFilterChain docsChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/v3/api-docs/**", "/v3/api-docs", "/swagger-ui/**", "/swagger-ui.html")
				.authorizeHttpRequests(a -> a.anyRequest().permitAll())
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
				.headers(h -> commonHeaders(h).contentSecurityPolicy(c -> c.policyDirectives(DOCS_CSP)));
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
		AuthenticationEntryPoint entryPoint = (req, res, ex) -> {
			res.setHeader("WWW-Authenticate", "Bearer");
			writeProblem(res, 401, "Unauthorized", "Authentication required");
		};
		AccessDeniedHandler deniedHandler = (req, res, ex) -> writeProblem(res, 403, "Forbidden", "Access denied");

		http.authorizeHttpRequests(a -> a
						.requestMatchers(POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
						.requestMatchers(GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated()
				)
				.csrf(AbstractHttpConfigurer::disable)
				.cors(c -> {
				})
				.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.oauth2ResourceServer(o -> o
						.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(deniedHandler)
				)
				.exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
				.headers(h -> commonHeaders(h).contentSecurityPolicy(c -> c.policyDirectives(API_CSP)));
		return http.build();
	}

	/**
	 * Maps the single-valued "role" claim to the authority ROLE_&lt;role&gt;.
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(props.cors().allowedOrigins());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		cors.setAllowCredentials(true);
		cors.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

	private static HeadersConfigurer<HttpSecurity> commonHeaders(HeadersConfigurer<HttpSecurity> h) {
		return h.httpStrictTransportSecurity(s -> s.includeSubDomains(true).maxAgeInSeconds(31_536_000))
				.contentTypeOptions(c -> {
				})
				.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
				.referrerPolicy(r -> r.policy(NO_REFERRER))
				.crossOriginResourcePolicy(c -> c.policy(SAME_ORIGIN));
	}

	private static void writeProblem(HttpServletResponse res, int status, String title, String detail)
			throws IOException {
		res.setStatus(status);
		res.setContentType("application/problem+json");
		res.getWriter().write(
				"{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
						+ ",\"detail\":\"" + detail + "\"}"
		);
	}
}
