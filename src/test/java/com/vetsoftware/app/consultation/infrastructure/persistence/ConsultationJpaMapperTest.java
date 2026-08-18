package com.vetsoftware.app.consultation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.domain.AnimalRef;
import com.vetsoftware.app.consultation.domain.CompanyRef;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
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
 * Las entidades JPA de otras features se mockean porque su constructor sin
 * argumentos es {@code protected} y no son instanciables desde este paquete. No
 * tienen logica: son portadores de datos, y mockearlas no oculta
 * comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultationJpaMapper")
class ConsultationJpaMapperTest {

    private final ConsultationJpaMapper mapper = new ConsultationJpaMapper();

    @Mock
    private ConsultationTypeJpaEntity consultationTypeEntity;
    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private ConsultationJpaEntity entidadCompleta() {
        ConsultationJpaEntity entity = new ConsultationJpaEntity();
        entity.setId(ConsultationMother.CONSULTATION_ID);
        entity.setDate(ConsultationMother.FECHA);
        entity.setAnamnesis("Anamnesis del paciente");
        entity.setDiagnosis("Diagnostico");
        entity.setPrognosis("Pronostico reservado");
        entity.setTemperature(new java.math.BigDecimal("38.5"));
        entity.setHeartRate(90);
        entity.setRespiratoryRate(22);
        entity.setMucousMembranes("Rosadas");
        entity.setCapillaryRefill("< 2 seg");
        entity.setHydration("Normal");
        entity.setBodyConditionScore(5);
        entity.setPainScore(2);
        entity.setAttitude("Alerta");
        entity.setExamFindings("Sin hallazgos relevantes");
        entity.setNextControl(ConsultationMother.FECHA.plusDays(15));
        entity.setCreatedDate(ConsultationMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y el examen fisico en su columna")
        void copia_cada_campo_escalar_y_el_examen_fisico() {
            Consultation consultation = ConsultationMother.consultaValida();

            ConsultationJpaEntity entity = mapper.toJpa(consultation, consultationTypeEntity,
                    animalEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(ConsultationMother.CONSULTATION_ID);
            assertThat(entity.getDate()).isEqualTo(ConsultationMother.FECHA);
            assertThat(entity.getAnamnesis()).isEqualTo("Anamnesis del paciente");
            assertThat(entity.getDiagnosis()).isEqualTo("Diagnostico");
            assertThat(entity.getPrognosis()).isEqualTo("Pronostico reservado");
            assertThat(entity.getTemperature()).isEqualByComparingTo("38.5");
            assertThat(entity.getHeartRate()).isEqualTo(90);
            assertThat(entity.getRespiratoryRate()).isEqualTo(22);
            assertThat(entity.getMucousMembranes()).isEqualTo("Rosadas");
            assertThat(entity.getCapillaryRefill()).isEqualTo("< 2 seg");
            assertThat(entity.getHydration()).isEqualTo("Normal");
            assertThat(entity.getBodyConditionScore()).isEqualTo(5);
            assertThat(entity.getPainScore()).isEqualTo(2);
            assertThat(entity.getAttitude()).isEqualTo("Alerta");
            assertThat(entity.getExamFindings()).isEqualTo("Sin hallazgos relevantes");
            assertThat(entity.getNextControl()).isEqualTo(ConsultationMother.FECHA.plusDays(15));
            assertThat(entity.getCreatedDate()).isEqualTo(ConsultationMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            ConsultationJpaEntity entity = mapper.toJpa(ConsultationMother.consultaValida(),
                    consultationTypeEntity, animalEntity, companyEntity);

            assertThat(entity.getConsultationType()).isSameAs(consultationTypeEntity);
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById: si
            // leyera entity.getConsultationType(), Hibernate lanzaria un SELECT extra por
            // save.
            Consultation consultation = mapper.toDomain(entidadCompleta(),
                    ConsultationMother.CONTROL, ConsultationMother.FIRULAIS,
                    ConsultationMother.CLINICA);

            assertThat(consultation.getId()).isEqualTo(ConsultationMother.CONSULTATION_ID);
            assertThat(consultation.getConsultationType()).isEqualTo(ConsultationMother.CONTROL);
            assertThat(consultation.getAnimal()).isEqualTo(ConsultationMother.FIRULAIS);
            assertThat(consultation.getCompany()).isEqualTo(ConsultationMother.CLINICA);
            assertThat(consultation.getAnamnesis()).isEqualTo("Anamnesis del paciente");
            assertThat(consultation.getPhysicalExam().temperature()).isEqualByComparingTo("38.5");
            assertThat(consultation.getCreatedDate()).isEqualTo(ConsultationMother.CREADO);
            assertThat(consultation.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Consultation original = ConsultationMother.consultaValida();

            ConsultationJpaEntity entity = mapper.toJpa(original, consultationTypeEntity,
                    animalEntity, companyEntity);
            Consultation vuelta = mapper.toDomain(entity, original.getConsultationType(),
                    original.getAnimal(), original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(consultationTypeEntity.getId()).thenReturn(ConsultationMother.CONTROL.id());
            when(consultationTypeEntity.getName()).thenReturn(ConsultationMother.CONTROL.name());
            when(animalEntity.getId()).thenReturn(ConsultationMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(ConsultationMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(ConsultationMother.FIRULAIS.code());
            when(companyEntity.getId()).thenReturn(ConsultationMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(ConsultationMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(ConsultationMother.CLINICA.identifier());

            ConsultationJpaEntity entity = entidadCompleta();
            entity.setConsultationType(consultationTypeEntity);
            entity.setAnimal(animalEntity);
            entity.setCompany(companyEntity);

            Consultation consultation = mapper.toDomain(entity);

            assertThat(consultation.getConsultationType()).isEqualTo(new ConsultationTypeRef(
                    ConsultationMother.CONTROL.id(), ConsultationMother.CONTROL.name()));
            assertThat(consultation.getAnimal()).isEqualTo(new AnimalRef(
                    ConsultationMother.FIRULAIS.id(), ConsultationMother.FIRULAIS.name(),
                    ConsultationMother.FIRULAIS.code()));
            assertThat(consultation.getCompany()).isEqualTo(new CompanyRef(
                    ConsultationMother.CLINICA.id(), ConsultationMother.CLINICA.name(),
                    ConsultationMother.CLINICA.identifier()));
        }
    }
}
