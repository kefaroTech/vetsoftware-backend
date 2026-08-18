package com.vetsoftware.app.vaccination.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.testsupport.VaccinationMother;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa.
 *
 * <p>
 * Las entidades JPA de otras features se mockean porque su constructor sin
 * argumentos es {@code protected} y no son instanciables desde este paquete. No
 * tienen logica: son portadores de datos, y mockearlas no oculta
 * comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VaccinationJpaMapper")
class VaccinationJpaMapperTest {

    private final VaccinationJpaMapper mapper = new VaccinationJpaMapper();

    @Mock
    private VaccinationTypeJpaEntity vaccinationTypeEntity;
    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private ConsultationJpaEntity consultationEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private VaccinationJpaEntity entidadCompleta() {
        VaccinationJpaEntity entity = new VaccinationJpaEntity();
        entity.setId(VaccinationMother.VACCINATION_ID);
        entity.setDate(VaccinationMother.FECHA);
        entity.setLot("L-2026-A");
        entity.setNotes("Sin reaccion");
        entity.setRoute("Subcutanea");
        entity.setApplicationSite("Cuello");
        entity.setNextVaccination(VaccinationMother.PROXIMA);
        entity.setCreatedDate(VaccinationMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            Vaccination vaccination = VaccinationMother.vigente();

            VaccinationJpaEntity entity = mapper.toJpa(vaccination, vaccinationTypeEntity,
                    animalEntity, consultationEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(VaccinationMother.VACCINATION_ID);
            assertThat(entity.getDate()).isEqualTo(VaccinationMother.FECHA);
            assertThat(entity.getLot()).isEqualTo("L-2026-A");
            assertThat(entity.getNotes()).isEqualTo("Sin reaccion");
            assertThat(entity.getRoute()).isEqualTo("Subcutanea");
            assertThat(entity.getApplicationSite()).isEqualTo("Cuello");
            assertThat(entity.getNextVaccination()).isEqualTo(VaccinationMother.PROXIMA);
            assertThat(entity.getCreatedDate()).isEqualTo(VaccinationMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            VaccinationJpaEntity entity = mapper.toJpa(VaccinationMother.vigente(),
                    vaccinationTypeEntity, animalEntity, consultationEntity, companyEntity);

            assertThat(entity.getVaccinationType()).isSameAs(vaccinationTypeEntity);
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getConsultation()).isSameAs(consultationEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("sin consulta asociada la columna queda en null, no una referencia rota")
        void sin_consulta_asociada_la_columna_queda_en_null() {
            VaccinationJpaEntity entity = mapper.toJpa(VaccinationMother.sinConsulta(),
                    vaccinationTypeEntity, animalEntity, null, companyEntity);

            assertThat(entity.getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            Vaccination vaccination = mapper.toDomain(entidadCompleta(), VaccinationMother.RABIA,
                    VaccinationMother.FIRULAIS, VaccinationMother.CONSULTA,
                    VaccinationMother.CLINICA);

            assertThat(vaccination.getId()).isEqualTo(VaccinationMother.VACCINATION_ID);
            assertThat(vaccination.getVaccinationType()).isEqualTo(VaccinationMother.RABIA);
            assertThat(vaccination.getAnimal()).isEqualTo(VaccinationMother.FIRULAIS);
            assertThat(vaccination.getConsultation()).isEqualTo(VaccinationMother.CONSULTA);
            assertThat(vaccination.getCompany()).isEqualTo(VaccinationMother.CLINICA);
            assertThat(vaccination.getLot()).isEqualTo("L-2026-A");
            assertThat(vaccination.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Vaccination original = VaccinationMother.vigente();

            VaccinationJpaEntity entity = mapper.toJpa(original, vaccinationTypeEntity,
                    animalEntity, consultationEntity, companyEntity);
            Vaccination vuelta = mapper.toDomain(entity, original.getVaccinationType(),
                    original.getAnimal(), original.getConsultation(), original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(vaccinationTypeEntity.getId()).thenReturn(VaccinationMother.RABIA.id());
            when(vaccinationTypeEntity.getName()).thenReturn(VaccinationMother.RABIA.name());
            when(animalEntity.getId()).thenReturn(VaccinationMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(VaccinationMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(VaccinationMother.FIRULAIS.code());
            when(consultationEntity.getId()).thenReturn(VaccinationMother.CONSULTA.id());
            when(consultationEntity.getDate()).thenReturn(VaccinationMother.CONSULTA.date());
            when(companyEntity.getId()).thenReturn(VaccinationMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(VaccinationMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(VaccinationMother.CLINICA.identifier());

            VaccinationJpaEntity entity = entidadCompleta();
            entity.setVaccinationType(vaccinationTypeEntity);
            entity.setAnimal(animalEntity);
            entity.setConsultation(consultationEntity);
            entity.setCompany(companyEntity);

            Vaccination vaccination = mapper.toDomain(entity);

            assertThat(vaccination.getVaccinationType()).isEqualTo(VaccinationMother.RABIA);
            assertThat(vaccination.getAnimal()).isEqualTo(VaccinationMother.FIRULAIS);
            assertThat(vaccination.getConsultation()).isEqualTo(VaccinationMother.CONSULTA);
            assertThat(vaccination.getCompany()).isEqualTo(VaccinationMother.CLINICA);
        }

        @Test
        @DisplayName("sin consulta asociada el companion queda en null")
        void sin_consulta_asociada_el_companion_queda_en_null() {
            when(vaccinationTypeEntity.getId()).thenReturn(VaccinationMother.RABIA.id());
            when(vaccinationTypeEntity.getName()).thenReturn(VaccinationMother.RABIA.name());
            when(animalEntity.getId()).thenReturn(VaccinationMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(VaccinationMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(VaccinationMother.FIRULAIS.code());
            when(companyEntity.getId()).thenReturn(VaccinationMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(VaccinationMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(VaccinationMother.CLINICA.identifier());

            VaccinationJpaEntity entity = entidadCompleta();
            entity.setVaccinationType(vaccinationTypeEntity);
            entity.setAnimal(animalEntity);
            entity.setConsultation(null);
            entity.setCompany(companyEntity);

            Vaccination vaccination = mapper.toDomain(entity);

            assertThat(vaccination.getConsultation()).isNull();
        }
    }
}
