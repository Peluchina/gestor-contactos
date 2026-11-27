package com.gestorcontactos.gestor_contactos.config;

import com.gestorcontactos.gestor_contactos.security.CustomUserDetailsService;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**", "/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/contactos/nuevo/natural")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/contactos/natural")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/contactos/nuevo/juridica")
                                .hasAnyRole("ADMINISTRADOR", "COMPRAS")
                        .requestMatchers(HttpMethod.POST, "/contactos/juridica")
                                .hasAnyRole("ADMINISTRADOR", "COMPRAS")
                        .requestMatchers(HttpMethod.GET, "/contactos/buscar")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR", "COMPRAS")
                        .requestMatchers(HttpMethod.GET, "/contactos/*/editar")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR", "COMPRAS")
                        .requestMatchers(HttpMethod.POST, "/contactos/*/relaciones/cliente")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/contactos/*/relaciones/proveedor")
                                .hasAnyRole("ADMINISTRADOR", "COMPRAS")
                        .requestMatchers(HttpMethod.POST,
                                "/contactos/*/editar",
                                "/contactos/*/telefonos",
                                "/contactos/*/telefonos/*/editar",
                                "/contactos/*/telefonos/*/eliminar",
                                "/contactos/*/correos",
                                "/contactos/*/correos/*/editar",
                                "/contactos/*/correos/*/eliminar")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR", "COMPRAS")
                        .requestMatchers("/contactos/**")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR", "COMPRAS", "ALMACENERO")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(-1)
                        .expiredUrl("/login?expired")
                        .sessionRegistry(sessionRegistry));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    Clock applicationClock() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
