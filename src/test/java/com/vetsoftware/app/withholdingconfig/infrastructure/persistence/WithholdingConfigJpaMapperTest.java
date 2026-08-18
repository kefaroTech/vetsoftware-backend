package com.vetsoftware.app.withholdingconfig.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.withholdingconfig.domain.CompanyRef;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import com.vetsoftware.app.withholdingconfig.testsupport.WithholdingConfigMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CompanyJpaEntity} pertenece a otra feature y su constructor es
 * {@code protected}: desde este paquete no se puede instanciar, así que se
 * mockea. No es una entidad de dominio —no tiene invariantes— sino una fila.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WithholdingConfigJpaMapper — ida y vuelta dominio-entidad")
class WithholdingConfigJpaMapperTest {

    private final WithholdingConfigJpaMapper mapper = new WithholdingConfigJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    @Nested
    @DisplayName("toJpa")
    class ADominioPersistente {

        @Test
        @DisplayName("copia los campos y engancha la company recibida")
        void copia_los_campos_y_engancha_la_company() {
            WithholdingConfig config = WithholdingConfigMother.configValida();

            WithholdingConfigJpaEntity entity = mapper.toJpa(config, companyEntity);

            assertThat(entity.getId()).isEqualTo(config.getId());
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getReteFuenteRate()).isEqualByComparingTo(config.getReteFuenteRate());
            assertThat(entity.getReteIvaRate()).isEqualByComparingTo(config.getReteIvaRate());
            assertThat(entity.getReteIcaRate()).isEqualByComparingTo(config.getReteIcaRate());
            assertThat(entity.getCreatedDate()).isEqualTo(config.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una configuracion nueva viaja sin id para que lo genere la base")
        void una_configuracion_nueva_viaja_sin_id() {
            WithholdingConfig nueva = WithholdingConfig.create(
                    WithholdingConfigMother.VETERINARIA_CENTRAL, BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE);

            WithholdingConfigJpaEntity entity = mapper.toJpa(nueva, companyEntity);

            assertThat(entity.getId()).isNull();
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("propaga el estado deshabilitado")
        void propaga_el_estado_deshabilitado() {
            WithholdingConfigJpaEntity entity = mapper
                    .toJpa(WithholdingConfigMother.deshabilitada(), companyEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("construye el CompanyRef leyendo la company relacionada")
        void construye_el_company_ref_leyendo_la_relacion() {
            when(companyEntity.getId()).thenReturn(WithholdingConfigMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Veterinaria Central");
            when(companyEntity.getIdentifier()).thenReturn("900123456-1");
            WithholdingConfigJpaEntity entity = new WithholdingConfigJpaEntity();
            entity.setId(10L);
            entity.setCompany(companyEntity);
            entity.setReteFuenteRate(new BigDecimal("2.5"));
            entity.setReteIvaRate(new BigDecimal("15.0"));
            entity.setReteIcaRate(new BigDecimal("1.0"));
            entity.setCreatedDate(WithholdingConfigMother.CREADO);
            entity.setEnabled(true);

            WithholdingConfig domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(10L);
            assertThat(domain.getCompany()).isEqualTo(WithholdingConfigMother.VETERINARIA_CENTRAL);
            assertThat(domain.getCreatedDate()).isEqualTo(WithholdingConfigMother.CREADO);
            assertThat(domain.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("company nula en la fila produce un CompanyRef nulo, que el dominio rechaza")
        void company_nula_en_la_fila_produce_ref_nulo() {
            // El dominio exige company no nula: este test cubre la rama defensiva
            // `c == null ? null : ...` del mapper, no un flujo que llegue a persistir.
            WithholdingConfigJpaEntity entity = new WithholdingConfigJpaEntity();
            entity.setId(10L);
            entity.setCompany(null);
            entity.setReteFuenteRate(BigDecimal.ONE);
            entity.setReteIvaRate(BigDecimal.ONE);
            entity.setReteIcaRate(BigDecimal.ONE);
            entity.setCreatedDate(WithholdingConfigMother.CREADO);
            entity.setEnabled(true);

            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }

        @Test
        @DisplayName("la sobrecarga con ref no toca la relacion de la entidad")
        void la_sobrecarga_con_ref_no_toca_la_relacion() {
            WithholdingConfigJpaEntity entity = new WithholdingConfigJpaEntity();
            entity.setId(10L);
            entity.setReteFuenteRate(BigDecimal.ONE);
            entity.setReteIvaRate(BigDecimal.ONE);
            entity.setReteIcaRate(BigDecimal.ONE);
            entity.setCreatedDate(WithholdingConfigMother.CREADO);
            entity.setEnabled(false);
            CompanyRef otraCompany = new CompanyRef(99L, "Otra Veterinaria", "800999888-2");

            WithholdingConfig domain = mapper.toDomain(entity, otraCompany);

            assertThat(domain.getCompany()).isEqualTo(otraCompany);
            assertThat(domain.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio -> entidad -> dominio conserva todos los campos")
        void conserva_todos_los_campos() {
            WithholdingConfig original = WithholdingConfigMother.configValida();

            WithholdingConfigJpaEntity entity = mapper.toJpa(original, companyEntity);
            WithholdingConfig vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getReteFuenteRate())
                    .isEqualByComparingTo(original.getReteFuenteRate());
            assertThat(vuelta.getReteIvaRate()).isEqualByComparingTo(original.getReteIvaRate());
            assertThat(vuelta.getReteIcaRate()).isEqualByComparingTo(original.getReteIcaRate());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
