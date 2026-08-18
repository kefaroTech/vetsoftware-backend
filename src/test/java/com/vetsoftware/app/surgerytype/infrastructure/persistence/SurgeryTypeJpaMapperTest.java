package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.surgerytype.testsupport.SurgeryTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SurgeryTypeJpaMapper")
class SurgeryTypeJpaMapperTest {

    private final SurgeryTypeJpaMapper mapper = new SurgeryTypeJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad, incluida la empresa")
        void copia_cada_campo_del_dominio_a_la_entidad() {
            SurgeryType tipo = SurgeryTypeMother.propioDeEmpresa();
            CompanyJpaEntity company = mock(CompanyJpaEntity.class);

            SurgeryTypeJpaEntity entity = mapper.toJpa(tipo, company);

            assertThat(entity.getId()).isEqualTo(tipo.getId());
            assertThat(entity.getName()).isEqualTo(tipo.getName());
            assertThat(entity.getDescription()).isEqualTo(tipo.getDescription());
            assertThat(entity.getCompany()).isSameAs(company);
            assertThat(entity.getGeneral()).isFalse();
            assertThat(entity.getCreatedDate()).isEqualTo(tipo.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un tipo general se guarda sin asociacion de empresa")
        void un_tipo_general_se_guarda_sin_empresa() {
            SurgeryTypeJpaEntity entity = mapper.toJpa(SurgeryTypeMother.general(), null);

            assertThat(entity.getCompany()).isNull();
            assertThat(entity.getGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain con CompanyRef ya resuelto — camino de escritura")
    class ToDomainConCompanyRef {

        @Test
        @DisplayName("usa el CompanyRef recibido en lugar de leer la asociacion JPA")
        void usa_el_company_ref_recibido() {
            SurgeryTypeJpaEntity entity = entidad(false);

            SurgeryType tipo = mapper.toDomain(entity, SurgeryTypeMother.OTRA_EMPRESA);

            assertThat(tipo.getCompany()).isEqualTo(SurgeryTypeMother.OTRA_EMPRESA);
            assertThat(tipo.getId()).isEqualTo(SurgeryTypeMother.SURGERY_TYPE_ID);
            assertThat(tipo.getName()).isEqualTo("Castracion");
        }

        @Test
        @DisplayName("un CompanyRef nulo reconstruye un tipo general")
        void un_company_ref_nulo_reconstruye_un_tipo_general() {
            SurgeryTypeJpaEntity entity = entidad(true);

            SurgeryType tipo = mapper.toDomain(entity, null);

            assertThat(tipo.getCompany()).isNull();
            assertThat(tipo.isGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion JPA — camino de lectura")
    class ToDomainDesdeLaAsociacion {

        @Test
        @DisplayName("reconstruye el CompanyRef desde la asociacion cuando el tipo es propio")
        void reconstruye_el_company_ref_desde_la_asociacion() {
            SurgeryTypeJpaEntity entity = entidad(false);
            CompanyJpaEntity company = mock(CompanyJpaEntity.class);
            when(company.getId()).thenReturn(SurgeryTypeMother.COMPANY_ID);
            when(company.getName()).thenReturn("Clinica Norte");
            when(company.getIdentifier()).thenReturn("900123456");
            entity.setCompany(company);

            SurgeryType tipo = mapper.toDomain(entity);

            assertThat(tipo.getCompany()).isEqualTo(SurgeryTypeMother.EMPRESA);
        }

        @Test
        @DisplayName("sin asociacion de empresa reconstruye un tipo general")
        void sin_asociacion_reconstruye_un_tipo_general() {
            SurgeryTypeJpaEntity entity = entidad(true);
            entity.setCompany(null);

            SurgeryType tipo = mapper.toDomain(entity);

            assertThat(tipo.getCompany()).isNull();
            assertThat(tipo.isGeneral()).isTrue();
        }
    }

    private static SurgeryTypeJpaEntity entidad(boolean general) {
        SurgeryTypeJpaEntity entity = new SurgeryTypeJpaEntity();
        entity.setId(SurgeryTypeMother.SURGERY_TYPE_ID);
        entity.setName(general ? "Cirugia general" : "Castracion");
        entity.setDescription("desc");
        entity.setGeneral(general);
        entity.setCreatedDate(SurgeryTypeMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }
}
