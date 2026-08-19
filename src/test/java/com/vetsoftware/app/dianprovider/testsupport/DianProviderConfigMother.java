package com.vetsoftware.app.dianprovider.testsupport;

import com.vetsoftware.app.dianprovider.application.command.CreateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.command.UpdateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.domain.CompanyRef;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo dianprovider.
 *
 * <p>
 * Se construyen con el constructor publico y no con {@code DianProviderConfig
 * .create(...)}: el factory pone {@code LocalDateTime.now()} y haria no
 * deterministas las aserciones sobre {@code createdDate}.
 */
public final class DianProviderConfigMother {

    public static final Long CONFIG_ID = 500L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 10L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_COMPANY_ID, "Clinica Sur",
            "NIT-901");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime TOKEN_EXPIRA = LocalDateTime.of(2026, 1, 15, 11, 30);

    private DianProviderConfigMother() {
    }

    /**
     * Config con login (username/password) y sin token cacheado. El caso por
     * defecto.
     */
    public static DianProviderConfig configValida() {
        return configValida(CONFIG_ID);
    }

    public static DianProviderConfig configValida(Long id) {
        return new DianProviderConfig(id, CLINICA, ProviderType.MATIAS, "https://api.matias.test",
                "client-id", "client-secret", "user@test.com", "secret-pass", null,
                "webhook-secret", null, null, "RES-001", CREADO, null, true);
    }

    /** Config con el token de acceso ya cacheado (post-login). */
    public static DianProviderConfig configConTokenCacheado() {
        return new DianProviderConfig(CONFIG_ID, CLINICA, ProviderType.MATIAS,
                "https://api.matias.test", "client-id", "client-secret", "user@test.com",
                "secret-pass", null, "webhook-secret", "cached-access-token", TOKEN_EXPIRA,
                "RES-001", CREADO, null, true);
    }

    /** Config autenticada solo con PAT estatico, sin login. */
    public static DianProviderConfig configConApiToken() {
        return new DianProviderConfig(CONFIG_ID, CLINICA, ProviderType.MATIAS,
                "https://api.matias.test", null, null, null, null, "static-pat-token",
                "webhook-secret", null, null, "RES-001", CREADO, null, true);
    }

    /** Config deshabilitada. */
    public static DianProviderConfig configDeshabilitada() {
        return new DianProviderConfig(CONFIG_ID, CLINICA, ProviderType.MATIAS,
                "https://api.matias.test", "client-id", "client-secret", "user@test.com",
                "secret-pass", null, "webhook-secret", null, null, "RES-001", CREADO, null, false);
    }

    /** Comando de creacion coherente con {@link #CLINICA}. */
    public static CreateDianProviderConfigCommand comandoCrear() {
        return new CreateDianProviderConfigCommand(ProviderType.MATIAS, "https://api.matias.test",
                "client-id", "client-secret", "user@test.com", "secret-pass", null,
                "webhook-secret", "RES-001", COMPANY_ID);
    }

    /** Comando de actualizacion que cambia todos los campos mutables. */
    public static UpdateDianProviderConfigCommand comandoActualizar() {
        return new UpdateDianProviderConfigCommand(ProviderType.MATIAS,
                "https://api.matias.test/v2", "client-id-2", "client-secret-2", "user2@test.com",
                "secret-pass-2", null, "webhook-secret-2", "RES-002", COMPANY_ID);
    }
}
