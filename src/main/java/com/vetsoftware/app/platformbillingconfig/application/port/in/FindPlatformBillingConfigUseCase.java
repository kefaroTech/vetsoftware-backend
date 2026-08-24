package com.vetsoftware.app.platformbillingconfig.application.port.in;

import com.vetsoftware.app.platformbillingconfig.application.dto.PlatformBillingConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Lee la configuración de facturación de la plataforma. Es la mitad de lectura
 * del par lectura + actualización con el que se gobierna la tabla singleton: no
 * existe caso de uso de alta (ver {@code PlatformBillingConfig} para el
 * porqué).
 *
 * <p>
 * <b>No devuelve {@code Optional}.</b> Si la fila no existe lanza
 * {@code PlatformBillingConfigNotConfiguredException}, porque un sistema que no
 * sabe cuántos días de gracia dar ni qué día facturar está mal desplegado, no
 * ante un caso de negocio.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. La tabla no tiene
 * {@code company_id} —es configuración global de plataforma— y con eso se
 * satisface {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29).
 */
public interface FindPlatformBillingConfigUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PlatformBillingConfigDto find();
}
