package com.vetsoftware.app.shared.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un puerto de entrada que deliberadamente no lleva
 * {@code @PreAuthorize}.
 *
 * <p>
 * No hace nada en tiempo de ejecución: existe para que la ausencia de un gate
 * sea una decisión escrita y revisable en vez de un olvido. La regla
 * {@code puertos_autorizados} de {@code HexagonalArchitectureTest} exige
 * {@code @PreAuthorize} en todo método de {@code ..application.port.in..}, y
 * solo acepta esta anotación como excepción.
 *
 * <p>
 * <strong>No es la lista de rutas públicas.</strong> Qué rutas se sirven sin
 * JWT lo decide {@code PublicRoutes}, que consumen el {@code AuthFilter} y la
 * {@code SecurityFilterChain}. Esta anotación solo documenta por qué el puerto
 * no autoriza; un puerto marcado puede perfectamente exigir token y no exigir
 * permiso.
 *
 * @see com.vetsoftware.app.auth.infrastructure.config.PublicRoutes
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NoAuthorizationRequired {

    /**
     * Por qué este puerto no necesita gate. Obligatorio: sin motivo escrito no hay
     * excepción.
     */
    String reason();
}
