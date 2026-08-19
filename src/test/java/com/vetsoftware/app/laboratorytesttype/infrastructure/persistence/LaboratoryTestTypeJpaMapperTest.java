package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LaboratoryTestTypeJpaMapper")
class LaboratoryTestTypeJpaMapperTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Norte", "NIT-900");

    private final LaboratoryTestTypeJpaMapper mapper = new LaboratoryTestTypeJpaMapper();

    /**
     * CompanyJpaEntity tiene constructor protegido: es una entidad de otra feature.
     */
    private static CompanyJpaEntity nuevaCompanyJpaEntity() throws ReflectiveOperationException {
        var constructor = CompanyJpaEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad, incluida la company")
        void copia_cada_campo_a_la_entidad() throws ReflectiveOperationException {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", "Hemograma completo",
                    CLINICA, false, CREADO, null, true);
            CompanyJpaEntity company = nuevaCompanyJpaEntity();
            company.setId(9L);

            LaboratoryTestTypeJpaEntity entity = mapper.toJpa(tipo, company);

            assertThat(entity.getId()).isEqualTo(70L);
            assertThat(entity.getName()).isEqualTo("Hemograma");
            assertThat(entity.getDescription()).isEqualTo("Hemograma completo");
            assertThat(entity.getCompany()).isSameAs(company);
            assertThat(entity.getGeneral()).isFalse();
            assertThat(entity.getCreatedDate()).isEqualTo(CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un tipo general se mapea sin company")
        void un_tipo_general_se_mapea_sin_company() {
            LaboratoryTestType general = new LaboratoryTestType(71L, "Perfil renal",
                    "Perfil renal basico", null, true, CREADO, null, true);

            LaboratoryTestTypeJpaEntity entity = mapper.toJpa(general, null);

            assertThat(entity.getCompany()).isNull();
            assertThat(entity.getGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain(entity) — reconstruye el CompanyRef desde la relacion JPA")
    class ToDomainConEntidad {

        @Test
        @DisplayName("una entidad con company hidrata el CompanyRef desde el ManyToOne")
        void hidrata_el_company_ref_desde_el_many_to_one() throws ReflectiveOperationException {
            CompanyJpaEntity company = nuevaCompanyJpaEntity();
            company.setId(9L);
            company.setName("Clinica Norte");
            company.setIdentifier("NIT-900");
            LaboratoryTestTypeJpaEntity entity = new LaboratoryTestTypeJpaEntity();
            entity.setId(70L);
            entity.setName("Hemograma");
            entity.setDescription("Hemograma completo");
            entity.setCompany(company);
            entity.setGeneral(false);
            entity.setCreatedDate(CREADO);
            entity.setEnabled(true);

            LaboratoryTestType tipo = mapper.toDomain(entity);

            assertThat(tipo.getCompany()).isEqualTo(CLINICA);
        }

        @Test
        @DisplayName("una entidad sin company (tipo general) mapea un CompanyRef nulo")
        void una_entidad_sin_company_mapea_company_ref_nulo() {
            LaboratoryTestTypeJpaEntity entity = new LaboratoryTestTypeJpaEntity();
            entity.setId(71L);
            entity.setName("Perfil renal");
            entity.setDescription("Perfil renal basico");
            entity.setCompany(null);
            entity.setGeneral(true);
            entity.setCreatedDate(CREADO);
            entity.setEnabled(true);

            LaboratoryTestType general = mapper.toDomain(entity);

            assertThat(general.getCompany()).isNull();
            assertThat(general.isGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain(entity, companyRef) — reusa un CompanyRef ya resuelto")
    class ToDomainConCompanyRefExplicito {

        @Test
        @DisplayName("copia cada campo de la entidad y usa el CompanyRef recibido, sin leer la relacion")
        void usa_el_company_ref_recibido() {
            LaboratoryTestTypeJpaEntity entity = new LaboratoryTestTypeJpaEntity();
            entity.setId(70L);
            entity.setName("Hemograma");
            entity.setDescription("Hemograma completo");
            entity.setGeneral(false);
            entity.setCreatedDate(CREADO);
            entity.setEnabled(false);

            LaboratoryTestType tipo = mapper.toDomain(entity, CLINICA);

            assertThat(tipo.getId()).isEqualTo(70L);
            assertThat(tipo.getName()).isEqualTo("Hemograma");
            assertThat(tipo.getCompany()).isEqualTo(CLINICA);
            assertThat(tipo.isEnabled()).isFalse();
        }
    }
}
