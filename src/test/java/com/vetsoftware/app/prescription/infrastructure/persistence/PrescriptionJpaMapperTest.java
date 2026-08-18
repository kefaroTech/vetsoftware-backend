package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.prescription.domain.AnimalRef;
import com.vetsoftware.app.prescription.domain.CompanyRef;
import com.vetsoftware.app.prescription.domain.ConsultationRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link AnimalJpaEntity}, {@link ConsultationJpaEntity} y
 * {@link CompanyJpaEntity} pertenecen a otras features y su constructor es
 * {@code protected}: desde aqui se mockean como filas, no como entidades de
 * dominio (no tienen invariantes propias).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionJpaMapper — ida y vuelta dominio <-> entidad")
class PrescriptionJpaMapperTest {

    private final PrescriptionJpaMapper mapper = new PrescriptionJpaMapper();

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private ConsultationJpaEntity consultationEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    @Nested
    @DisplayName("toJpa")
    class ADominioPersistente {

        @Test
        @DisplayName("copia cada campo y engancha las tres asociaciones recibidas")
        void copia_los_campos_y_las_relaciones() {
            Prescription prescription = PrescriptionMother.persistida();

            PrescriptionJpaEntity entity = mapper.toJpa(prescription, animalEntity,
                    consultationEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(prescription.getId());
            assertThat(entity.getDate()).isEqualTo(prescription.getDate());
            assertThat(entity.getDiagnosis()).isEqualTo(prescription.getDiagnosis());
            assertThat(entity.getObservations()).isEqualTo(prescription.getObservations());
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getConsultation()).isSameAs(consultationEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(prescription.getCreatedDate());
            assertThat(entity.isEnabled()).isEqualTo(prescription.isEnabled());
        }

        @Test
        @DisplayName("una receta nueva viaja sin id para que lo genere la base")
        void una_receta_nueva_viaja_sin_id() {
            PrescriptionJpaEntity entity = mapper.toJpa(PrescriptionMother.valida(), animalEntity,
                    consultationEntity, companyEntity);

            assertThat(entity.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("construye los tres Ref leyendo las asociaciones hidratadas")
        void construye_los_refs_desde_las_asociaciones() {
            when(animalEntity.getId()).thenReturn(PrescriptionMother.ANIMAL_ID);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");
            when(consultationEntity.getId()).thenReturn(PrescriptionMother.CONSULTATION_ID);
            when(consultationEntity.getDate()).thenReturn(PrescriptionMother.FECHA);
            when(companyEntity.getId()).thenReturn(PrescriptionMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Veterinaria Test");
            when(companyEntity.getIdentifier()).thenReturn("900123456");

            PrescriptionJpaEntity entity = new PrescriptionJpaEntity();
            entity.setId(PrescriptionMother.PRESCRIPTION_ID);
            entity.setDate(PrescriptionMother.FECHA);
            entity.setDiagnosis("Otitis externa");
            entity.setObservations("Control en 7 dias");
            entity.setAnimal(animalEntity);
            entity.setConsultation(consultationEntity);
            entity.setCompany(companyEntity);
            entity.setCreatedDate(PrescriptionMother.CREADO);
            entity.setEnabled(true);

            Prescription domain = mapper.toDomain(entity);

            assertThat(domain.getAnimal())
                    .isEqualTo(new AnimalRef(PrescriptionMother.ANIMAL_ID, "Firulais", "A-001"));
            assertThat(domain.getConsultation()).isEqualTo(new ConsultationRef(
                    PrescriptionMother.CONSULTATION_ID, PrescriptionMother.FECHA));
            assertThat(domain.getCompany()).isEqualTo(
                    new CompanyRef(PrescriptionMother.COMPANY_ID, "Veterinaria Test", "900123456"));
        }

        @Test
        @DisplayName("la sobrecarga con refs no toca las asociaciones de la entidad")
        void la_sobrecarga_con_refs_no_toca_las_relaciones() {
            PrescriptionJpaEntity entity = new PrescriptionJpaEntity();
            entity.setId(PrescriptionMother.PRESCRIPTION_ID);
            entity.setDate(PrescriptionMother.FECHA);
            entity.setCreatedDate(PrescriptionMother.CREADO);
            entity.setEnabled(false);

            Prescription domain = mapper.toDomain(entity, PrescriptionMother.MASCOTA,
                    PrescriptionMother.CONSULTA, PrescriptionMother.CLINICA);

            assertThat(domain.getAnimal()).isEqualTo(PrescriptionMother.MASCOTA);
            assertThat(domain.getConsultation()).isEqualTo(PrescriptionMother.CONSULTA);
            assertThat(domain.getCompany()).isEqualTo(PrescriptionMother.CLINICA);
            assertThat(domain.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio -> entidad -> dominio conserva todos los campos")
        void conserva_todos_los_campos() {
            Prescription original = PrescriptionMother.persistida();

            PrescriptionJpaEntity entity = mapper.toJpa(original, animalEntity, consultationEntity,
                    companyEntity);
            Prescription vuelta = mapper.toDomain(entity, original.getAnimal(),
                    original.getConsultation(), original.getCompany());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getDate()).isEqualTo(original.getDate());
            assertThat(vuelta.getDiagnosis()).isEqualTo(original.getDiagnosis());
            assertThat(vuelta.getObservations()).isEqualTo(original.getObservations());
            assertThat(vuelta.getAnimal()).isEqualTo(original.getAnimal());
            assertThat(vuelta.getConsultation()).isEqualTo(original.getConsultation());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
