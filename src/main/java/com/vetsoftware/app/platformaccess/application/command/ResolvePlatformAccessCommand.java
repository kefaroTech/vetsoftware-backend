package com.vetsoftware.app.platformaccess.application.command;

/**
 * Token del enlace del aprobador más el código de 6 dígitos. El mismo código
 * sirve para aprobar y para rechazar: no hay un código por decisión.
 */
public record ResolvePlatformAccessCommand(String token, String code) {
}
