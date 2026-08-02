package com.vetsoftware.app.companysettings.application.command;

/**
 * Upsert de un ajuste por empresa. {@code companyId} lo inyecta el controller
 * desde el contexto (nunca el cliente).
 */
public record SetCompanySettingCommand(Long companyId, String propertyName, String value) {
}
