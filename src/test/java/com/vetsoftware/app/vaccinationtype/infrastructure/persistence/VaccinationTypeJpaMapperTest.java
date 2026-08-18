package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * {@code CompanyJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es portador de datos, y mockearla no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VaccinationTypeJpaMapper")
class VaccinationTypeJpaMapperTest {

    private final VaccinationTypeJpaMapper mapper = new VaccinationTypeJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    private VaccinationTypeJpaEntity entidadCompleta() {
        VaccinationTypeJpaEntity entity = new VaccinationTypeJpaEntity();
        entity.setId(VaccinationTypeMother.TYPE_ID);
        entity.setName("Rabia");
        entity.setDescription("Vacuna antirrabica");
        entity.setGeneral(false);
        entity.setCreatedDate(VaccinationTypeMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            VaccinationType tipo = VaccinationTypeMother.propia();

            VaccinationTypeJpaEntity entity = mapper.toJpa(tipo, companyEntity);

            assertThat(entity.getId()).isEqualTo(VaccinationTypeMother.TYPE_ID);
            assertThat(entity.getName()).isEqualTo("Rabia");
            assertThat(entity.getDescription()).isEqualTo("Vacuna antirrabica");
            assertThat(entity.getGeneral()).isFalse();
            assertThat(entity.getCreatedDate()).isEqualTo(VaccinationTypeMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha la compania en su slot")
        void engancha_la_compania_en_su_slot() {
            VaccinationTypeJpaEntity entity = mapper.toJpa(VaccinationTypeMother.propia(),
                    companyEntity);

            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("un tipo general se guarda sin compania")
        void un_tipo_general_se_guarda_sin_compania() {
            VaccinationTypeJpaEntity entity = mapper.toJpa(VaccinationTypeMother.general(), null);

            assertThat(entity.getCompany()).isNull();
            assertThat(entity.getGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain con ref precargado — camino de escritura")
    class ToDomainConRef {

        @Test
        @DisplayName("reconstruye el agregado sin tocar la asociacion JPA")
        void reconstruye_el_agregado_sin_tocar_la_asociacion() {
            // Este overload existe para no inicializar el proxy de getReferenceById: si
            // leyera entity.getCompany(), Hibernate lanzaria un SELECT extra por save.
            VaccinationType tipo = mapper.toDomain(entidadCompleta(),
                    VaccinationTypeMother.CLINICA);

            assertThat(tipo.getId()).isEqualTo(VaccinationTypeMother.TYPE_ID);
            assertThat(tipo.getName()).isEqualTo("Rabia");
            assertThat(tipo.getDescription()).isEqualTo("Vacuna antirrabica");
            assertThat(tipo.getCompany()).isEqualTo(VaccinationTypeMother.CLINICA);
            assertThat(tipo.isGeneral()).isFalse();
            assertThat(tipo.getCreatedDate()).isEqualTo(VaccinationTypeMother.CREADO);
            assertThat(tipo.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            VaccinationType original = VaccinationTypeMother.propia();

            VaccinationTypeJpaEntity entity = mapper.toJpa(original, companyEntity);
            VaccinationType vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }

        @Test
        @DisplayName("un ref nulo reconstruye un tipo general sin compania")
        void un_ref_nulo_reconstruye_un_tipo_general_sin_compania() {
            VaccinationTypeJpaEntity entity = entidadCompleta();
            entity.setGeneral(true);

            VaccinationType tipo = mapper.toDomain(entity, null);

            assertThat(tipo.getCompany()).isNull();
            assertThat(tipo.isGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion — camino de lectura")
    class ToDomainDesdeAsociacion {

        @Test
        @DisplayName("construye el companion VO desde la asociacion hidratada")
        void construye_el_companion_vo_desde_la_asociacion_hidratada() {
            when(companyEntity.getId()).thenReturn(VaccinationTypeMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(VaccinationTypeMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(VaccinationTypeMother.CLINICA.identifier());
            VaccinationTypeJpaEntity entity = entidadCompleta();
            entity.setCompany(companyEntity);

            VaccinationType tipo = mapper.toDomain(entity);

            assertThat(tipo.getCompany()).isEqualTo(VaccinationTypeMother.CLINICA);
        }

        @Test
        @DisplayName("sin compania hidratada, un tipo general vuelve sin compania")
        void sin_compania_hidratada_un_tipo_general_vuelve_sin_compania() {
            VaccinationTypeJpaEntity entity = entidadCompleta();
            entity.setGeneral(true);
            entity.setCompany(null);

            VaccinationType tipo = mapper.toDomain(entity);

            assertThat(tipo.getCompany()).isNull();
            assertThat(tipo.isGeneral()).isTrue();
        }
    }
}
