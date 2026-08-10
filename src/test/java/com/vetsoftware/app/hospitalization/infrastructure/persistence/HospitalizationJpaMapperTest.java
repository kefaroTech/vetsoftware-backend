package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
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
@DisplayName("HospitalizationJpaMapper")
class HospitalizationJpaMapperTest {

    private final HospitalizationJpaMapper mapper = new HospitalizationJpaMapper();

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private ConsultationJpaEntity consultationEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private HospitalizationJpaEntity entidadCompleta() {
        HospitalizationJpaEntity entity = new HospitalizationJpaEntity();
        entity.setId(HospitalizationMother.HOSPITALIZATION_ID);
        entity.setDate(HospitalizationMother.FECHA);
        entity.setStartDate(HospitalizationMother.INICIO);
        entity.setEndDate(HospitalizationMother.FIN);
        entity.setType(HospitalizationType.HOSPITALIZATION);
        entity.setReasonLeaving(ReasonLeaving.MEDICAL_DISCHARGE);
        entity.setReason("Gastroenteritis aguda");
        entity.setObservations("Sin complicaciones");
        entity.setCreatedDate(HospitalizationMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            HospitalizationJpaEntity entity = mapper.toJpa(HospitalizationMother.internado(),
                    animalEntity, consultationEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(entity.getDate()).isEqualTo(HospitalizationMother.FECHA);
            assertThat(entity.getStartDate()).isEqualTo(HospitalizationMother.INICIO);
            assertThat(entity.getEndDate()).isEqualTo(HospitalizationMother.FIN);
            assertThat(entity.getType()).isEqualTo(HospitalizationType.HOSPITALIZATION);
            assertThat(entity.getReasonLeaving()).isEqualTo(ReasonLeaving.MEDICAL_DISCHARGE);
            assertThat(entity.getReason()).isEqualTo("Gastroenteritis aguda");
            assertThat(entity.getObservations()).isEqualTo("Sin complicaciones");
            assertThat(entity.getCreatedDate()).isEqualTo(HospitalizationMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha las asociaciones que recibe, sin inventarselas del dominio")
        void engancha_las_asociaciones_que_recibe() {
            HospitalizationJpaEntity entity = mapper.toJpa(HospitalizationMother.internado(),
                    animalEntity, consultationEntity, companyEntity);

            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getConsultation()).isSameAs(consultationEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("una consulta nula se mapea a asociacion nula")
        void consulta_nula_se_mapea_a_asociacion_nula() {
            HospitalizationJpaEntity entity = mapper.toJpa(
                    HospitalizationMother.ambulatorioSinConsulta(), animalEntity, null,
                    companyEntity);

            assertThat(entity.getConsultation()).isNull();
            assertThat(entity.getReasonLeaving()).isNull();
            assertThat(entity.getEndDate()).isNull();
        }

        @Test
        @DisplayName("una hospitalizacion deshabilitada mantiene enabled=false")
        void deshabilitada_mantiene_enabled_false() {
            HospitalizationJpaEntity entity = mapper.toJpa(HospitalizationMother.deshabilitado(),
                    animalEntity, consultationEntity, companyEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("el read path construye los companion VO desde las asociaciones hidratadas")
        void read_path_construye_los_companion_vo() {
            when(animalEntity.getId()).thenReturn(HospitalizationMother.ANIMAL_ID);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");
            when(consultationEntity.getId()).thenReturn(HospitalizationMother.CONSULTATION_ID);
            when(consultationEntity.getDate()).thenReturn(HospitalizationMother.CONSULTA.date());
            when(companyEntity.getId()).thenReturn(HospitalizationMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Vet");
            when(companyEntity.getIdentifier()).thenReturn("900123456");
            HospitalizationJpaEntity entity = entidadCompleta();
            entity.setAnimal(animalEntity);
            entity.setConsultation(consultationEntity);
            entity.setCompany(companyEntity);

            Hospitalization hospitalization = mapper.toDomain(entity);

            assertThat(hospitalization.getAnimal()).isEqualTo(HospitalizationMother.FIRULAIS);
            assertThat(hospitalization.getConsultation()).isEqualTo(HospitalizationMother.CONSULTA);
            assertThat(hospitalization.getCompany()).isEqualTo(HospitalizationMother.CLINICA);
        }

        @Test
        @DisplayName("el read path tolera la consulta nula sin tocar sus getters")
        void read_path_tolera_la_consulta_nula() {
            when(animalEntity.getId()).thenReturn(HospitalizationMother.ANIMAL_ID);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");
            when(companyEntity.getId()).thenReturn(HospitalizationMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Vet");
            when(companyEntity.getIdentifier()).thenReturn("900123456");
            HospitalizationJpaEntity entity = entidadCompleta();
            entity.setAnimal(animalEntity);
            entity.setConsultation(null);
            entity.setCompany(companyEntity);

            assertThat(mapper.toDomain(entity).getConsultation()).isNull();
        }

        @Test
        @DisplayName("el write path reusa los refs precargados sin tocar los proxies")
        void write_path_reusa_los_refs_precargados() {
            Hospitalization hospitalization = mapper.toDomain(entidadCompleta(),
                    HospitalizationMother.FIRULAIS, HospitalizationMother.CONSULTA,
                    HospitalizationMother.CLINICA);

            assertThat(hospitalization.getId()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(hospitalization.getDate()).isEqualTo(HospitalizationMother.FECHA);
            assertThat(hospitalization.getStartDate()).isEqualTo(HospitalizationMother.INICIO);
            assertThat(hospitalization.getEndDate()).isEqualTo(HospitalizationMother.FIN);
            assertThat(hospitalization.getType()).isEqualTo(HospitalizationType.HOSPITALIZATION);
            assertThat(hospitalization.getReasonLeaving())
                    .isEqualTo(ReasonLeaving.MEDICAL_DISCHARGE);
            assertThat(hospitalization.getReason()).isEqualTo("Gastroenteritis aguda");
            assertThat(hospitalization.getObservations()).isEqualTo("Sin complicaciones");
            assertThat(hospitalization.getAnimal()).isSameAs(HospitalizationMother.FIRULAIS);
            assertThat(hospitalization.getConsultation()).isSameAs(HospitalizationMother.CONSULTA);
            assertThat(hospitalization.getCompany()).isSameAs(HospitalizationMother.CLINICA);
            assertThat(hospitalization.getCreatedDate()).isEqualTo(HospitalizationMother.CREADO);
            assertThat(hospitalization.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio a entidad y vuelta al dominio no pierde ningun campo")
        void ida_y_vuelta_no_pierde_ningun_campo() {
            Hospitalization original = HospitalizationMother.internado();

            HospitalizationJpaEntity entity = mapper.toJpa(original, animalEntity,
                    consultationEntity, companyEntity);
            Hospitalization vuelta = mapper.toDomain(entity, original.getAnimal(),
                    original.getConsultation(), original.getCompany());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getDate()).isEqualTo(original.getDate());
            assertThat(vuelta.getStartDate()).isEqualTo(original.getStartDate());
            assertThat(vuelta.getEndDate()).isEqualTo(original.getEndDate());
            assertThat(vuelta.getType()).isEqualTo(original.getType());
            assertThat(vuelta.getReasonLeaving()).isEqualTo(original.getReasonLeaving());
            assertThat(vuelta.getReason()).isEqualTo(original.getReason());
            assertThat(vuelta.getObservations()).isEqualTo(original.getObservations());
            assertThat(vuelta.getAnimal()).isEqualTo(original.getAnimal());
            assertThat(vuelta.getConsultation()).isEqualTo(original.getConsultation());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }

        @Test
        @DisplayName("la variante ambulatoria sin consulta sobrevive la ida y vuelta")
        void variante_ambulatoria_sobrevive_la_ida_y_vuelta() {
            Hospitalization original = HospitalizationMother.ambulatorioSinConsulta();

            HospitalizationJpaEntity entity = mapper.toJpa(original, animalEntity, null,
                    companyEntity);
            Hospitalization vuelta = mapper.toDomain(entity, original.getAnimal(), null,
                    original.getCompany());

            assertThat(vuelta.getConsultation()).isNull();
            assertThat(vuelta.getReasonLeaving()).isNull();
            assertThat(vuelta.getEndDate()).isNull();
            assertThat(vuelta.getType()).isEqualTo(HospitalizationType.OUTPATIENT);
        }
    }
}
