package com.vetsoftware.app.companysettings.testsupport;

import com.vetsoftware.app.companysettings.application.command.SetCompanySettingCommand;
import com.vetsoftware.app.companysettings.domain.CompanySetting;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo companysettings.
 *
 * <p>
 * Se construye con el constructor publico y no con
 * {@code CompanySetting.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class CompanySettingMother {

    public static final Long SETTING_ID = 500L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 11L;
    public static final String PROPERTY_NAME = "inventory.allow_negative_stock";
    public static final String VALUE = "true";
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private CompanySettingMother() {
    }

    /** Ajuste habilitado, con id, tal como vendria de la base. */
    public static CompanySetting ajusteExistente() {
        return ajusteExistente(SETTING_ID, COMPANY_ID, PROPERTY_NAME, VALUE);
    }

    public static CompanySetting ajusteExistente(Long id, Long companyId, String propertyName,
            String value) {
        return new CompanySetting(id, companyId, propertyName, value, CREADO, true);
    }

    public static SetCompanySettingCommand comandoValido() {
        return new SetCompanySettingCommand(COMPANY_ID, PROPERTY_NAME, VALUE);
    }
}
