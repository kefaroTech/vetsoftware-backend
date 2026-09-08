package com.vetsoftware.app.service.application.port.out;

/**
 * Techo gratuito del eje {@code SERVICE_ITEM}: tarifas activas de la empresa
 * ({@code STOCK}, contador = {@code COUNT(enabled)} en vivo sobre
 * {@code services}). Lanza si ya está en su techo vigente; no escribe nada —a
 * diferencia de los ejes de {@code companyusageevent}, aquí no hay hecho que
 * anotar: la propia fila de {@code services} ya es el contador.
 */
public interface ServiceUsageLimitPort {

    void checkNotExceeded(Long companyId);
}
