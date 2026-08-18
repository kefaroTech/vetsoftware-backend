package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dianprovider.domain.ProviderType;
import com.vetsoftware.app.dianprovider.infrastructure.persistence.DianProviderConfigJpaEntity;
import com.vetsoftware.app.dianprovider.infrastructure.persistence.DianProviderConfigJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProviderConfigQueryPort — config del proveedor DIAN ya descifrada")
class JpaProviderConfigQueryPortTest {

    @Mock
    private DianProviderConfigJpaRepository configJpaRepository;

    private JpaProviderConfigQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaProviderConfigQueryPort(configJpaRepository);
    }

    @Test
    @DisplayName("una config existente se traduce a ProviderConfigSnapshot completo")
    void config_existente_se_traduce_a_snapshot() throws Exception {
        DianProviderConfigJpaEntity entity = ReflectionEntities
                .newInstance(DianProviderConfigJpaEntity.class);
        entity.setProvider(ProviderType.MATIAS);
        entity.setBaseUrl("https://matias.test");
        entity.setClientId("id-1");
        entity.setClientSecret("secret-1");
        entity.setUsername("user-1");
        entity.setPassword("pass-1");
        entity.setApiToken("token-1");
        entity.setWebhookSecret("hook-1");
        entity.setNumberingProviderRef("ref-1");
        when(configJpaRepository.findByCompany_Id(9L)).thenReturn(Optional.of(entity));

        Optional<ProviderConfigSnapshot> snapshot = port.findByCompanyId(9L);

        assertThat(snapshot).contains(new ProviderConfigSnapshot("MATIAS", "https://matias.test",
                "id-1", "secret-1", "user-1", "pass-1", "token-1", "hook-1", "ref-1"));
    }

    @Test
    @DisplayName("sin config configurada devuelve vacio")
    void sin_config_devuelve_vacio() {
        when(configJpaRepository.findByCompany_Id(10L)).thenReturn(Optional.empty());

        assertThat(port.findByCompanyId(10L)).isEmpty();
    }
}
