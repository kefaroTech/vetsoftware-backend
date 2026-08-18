package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaEntity;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaUvtQueryPort — lee y parsea el UVT vigente de la configuracion global")
class JpaUvtQueryPortTest {

    @Mock
    private SystemConfigurationJpaRepository systemConfigurationJpaRepository;

    private JpaUvtQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaUvtQueryPort(systemConfigurationJpaRepository);
    }

    private static SystemConfigurationJpaEntity conValor(String value) throws Exception {
        SystemConfigurationJpaEntity entity = ReflectionEntities
                .newInstance(SystemConfigurationJpaEntity.class);
        entity.setValue(value);
        return entity;
    }

    @Test
    @DisplayName("un valor numerico valido se parsea a BigDecimal")
    void valor_numerico_valido_se_parsea() throws Exception {
        when(systemConfigurationJpaRepository.findByPropertyName("uvt"))
                .thenReturn(Optional.of(conValor("49799")));

        assertThat(port.currentUvt()).contains(new BigDecimal("49799"));
    }

    @Test
    @DisplayName("sin la propiedad configurada devuelve vacio")
    void sin_propiedad_configurada_devuelve_vacio() {
        when(systemConfigurationJpaRepository.findByPropertyName("uvt"))
                .thenReturn(Optional.empty());

        assertThat(port.currentUvt()).isEmpty();
    }

    @Nested
    @DisplayName("valores no numericos o vacios")
    class ValoresInvalidos {

        @ParameterizedTest
        @ValueSource(strings = {"no-es-un-numero", "", "   "})
        @DisplayName("un valor no numerico o en blanco se ignora, sin lanzar")
        void valor_no_numerico_o_en_blanco_se_ignora(String value) throws Exception {
            when(systemConfigurationJpaRepository.findByPropertyName("uvt"))
                    .thenReturn(Optional.of(conValor(value)));

            assertThat(port.currentUvt()).isEmpty();
        }
    }
}
