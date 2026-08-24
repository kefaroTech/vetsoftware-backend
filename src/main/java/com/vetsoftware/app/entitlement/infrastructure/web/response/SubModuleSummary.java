package com.vetsoftware.app.entitlement.infrastructure.web.response;

/**
 * El submodulo dentro de un permiso, tal como sale por HTTP. Es un companion
 * local: este slice no expone la {@code Response} de {@code submodule}.
 */
public record SubModuleSummary(Long id, String code, String name) {
}
