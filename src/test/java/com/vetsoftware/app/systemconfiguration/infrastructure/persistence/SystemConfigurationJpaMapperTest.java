package com.vetsoftware.app.systemconfiguration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import com.vetsoftware.app.systemconfiguration.testsupport.SystemConfigurationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SystemConfigurationJpaMapper")
class SystemConfigurationJpaMapperTest {

    private final SystemConfigurationJpaMapper mapper = new SystemConfigurationJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo en su columna")
        void copia_cada_campo_en_su_columna() {
            SystemConfiguration config = SystemConfigurationMother.configuracionExistente();

            SystemConfigurationJpaEntity entity = mapper.toJpa(config);

            assertThat(entity.getId()).isEqualTo(SystemConfigurationMother.CONFIG_ID);
            assertThat(entity.getPropertyName()).isEqualTo(SystemConfigurationMother.PROPERTY_NAME);
            assertThat(entity.getValue()).isEqualTo(SystemConfigurationMother.VALUE);
            assertThat(entity.getCreatedDate()).isEqualTo(SystemConfigurationMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una configuracion sin id (nueva) mapea a entidad sin id")
        void una_configuracion_sin_id_mapea_a_entidad_sin_id() {
            SystemConfiguration config = SystemConfiguration.create("uvt", "47065");

            assertThat(mapper.toJpa(config).getId()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado sin perder ningun campo")
        void reconstruye_el_agregado_sin_perder_ningun_campo() {
            SystemConfigurationJpaEntity entity = new SystemConfigurationJpaEntity();
            entity.setId(SystemConfigurationMother.CONFIG_ID);
            entity.setPropertyName(SystemConfigurationMother.PROPERTY_NAME);
            entity.setValue(SystemConfigurationMother.VALUE);
            entity.setCreatedDate(SystemConfigurationMother.CREADO);
            entity.setEnabled(false);

            SystemConfiguration config = mapper.toDomain(entity);

            assertThat(config.getId()).isEqualTo(SystemConfigurationMother.CONFIG_ID);
            assertThat(config.getPropertyName()).isEqualTo(SystemConfigurationMother.PROPERTY_NAME);
            assertThat(config.getValue()).isEqualTo(SystemConfigurationMother.VALUE);
            assertThat(config.getCreatedDate()).isEqualTo(SystemConfigurationMother.CREADO);
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            SystemConfiguration original = SystemConfigurationMother.configuracionExistente();

            SystemConfigurationJpaEntity entity = mapper.toJpa(original);
            SystemConfiguration vuelta = mapper.toDomain(entity);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
