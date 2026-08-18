package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.testsupport.DiagnosticImagingTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CompanyJpaEntity} pertenece a otra feature y su constructor es
 * {@code protected}: desde aqui se mockea como fila, no como entidad de
 * dominio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticImagingTypeJpaMapper — ida y vuelta dominio <-> entidad")
class DiagnosticImagingTypeJpaMapperTest {

    private final DiagnosticImagingTypeJpaMapper mapper = new DiagnosticImagingTypeJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    @Nested
    @DisplayName("toJpa")
    class ADominioPersistente {

        @Test
        @DisplayName("copia cada campo y engancha la company recibida")
        void copia_cada_campo_y_engancha_la_company() {
            DiagnosticImagingType type = DiagnosticImagingTypeMother.propiaDeEmpresa();

            DiagnosticImagingTypeJpaEntity entity = mapper.toJpa(type, companyEntity);

            assertThat(entity.getId()).isEqualTo(type.getId());
            assertThat(entity.getName()).isEqualTo(type.getName());
            assertThat(entity.getDescription()).isEqualTo(type.getDescription());
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getGeneral()).isEqualTo(type.isGeneral());
            assertThat(entity.getCreatedDate()).isEqualTo(type.getCreatedDate());
            assertThat(entity.isEnabled()).isEqualTo(type.isEnabled());
        }

        @Test
        @DisplayName("un tipo general viaja sin company")
        void un_tipo_general_viaja_sin_company() {
            DiagnosticImagingType general = DiagnosticImagingTypeMother.general();

            DiagnosticImagingTypeJpaEntity entity = mapper.toJpa(general, null);

            assertThat(entity.getCompany()).isNull();
            assertThat(entity.getGeneral()).isTrue();
        }

        @Test
        @DisplayName("un tipo nuevo viaja sin id para que lo genere la base")
        void un_tipo_nuevo_viaja_sin_id() {
            DiagnosticImagingTypeJpaEntity entity = mapper
                    .toJpa(DiagnosticImagingType.create("Radiografia", "desc", null, true), null);

            assertThat(entity.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain(entity) — read path, resuelve la company desde la asociacion")
    class ADominioDesdeLaEntidad {

        @Test
        @DisplayName("mapea una entidad con company asociada")
        void mapea_una_entidad_con_company_asociada() {
            when(companyEntity.getId()).thenReturn(DiagnosticImagingTypeMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("900123456");
            DiagnosticImagingTypeJpaEntity entity = mapper
                    .toJpa(DiagnosticImagingTypeMother.propiaDeEmpresa(), companyEntity);
            entity.setId(DiagnosticImagingTypeMother.TYPE_ID);

            DiagnosticImagingType domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(DiagnosticImagingTypeMother.TYPE_ID);
            assertThat(domain.getCompany()).isEqualTo(new CompanyRef(
                    DiagnosticImagingTypeMother.COMPANY_ID, "Clinica Norte", "900123456"));
        }

        @Test
        @DisplayName("mapea una entidad general sin company asociada")
        void mapea_una_entidad_general_sin_company() {
            DiagnosticImagingTypeJpaEntity entity = mapper
                    .toJpa(DiagnosticImagingTypeMother.general(), null);
            entity.setId(DiagnosticImagingTypeMother.TYPE_ID);

            DiagnosticImagingType domain = mapper.toDomain(entity);

            assertThat(domain.getCompany()).isNull();
            assertThat(domain.isGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain(entity, ref) — write path, reusa la company ya cargada")
    class ADominioReusandoElRef {

        @Test
        @DisplayName("reusa el CompanyRef recibido sin tocar la asociacion de la entidad")
        void reusa_el_company_ref_recibido() {
            DiagnosticImagingTypeJpaEntity entity = mapper
                    .toJpa(DiagnosticImagingTypeMother.propiaDeEmpresa(), companyEntity);
            entity.setId(DiagnosticImagingTypeMother.TYPE_ID);

            DiagnosticImagingType domain = mapper.toDomain(entity,
                    DiagnosticImagingTypeMother.EMPRESA);

            assertThat(domain.getCompany()).isSameAs(DiagnosticImagingTypeMother.EMPRESA);
        }
    }
}
