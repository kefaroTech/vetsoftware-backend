package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import com.vetsoftware.app.diagnosticimaging.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link DiagnosticImagingTypeJpaEntity}, {@link AnimalJpaEntity},
 * {@link ConsultationJpaEntity} y {@link CompanyJpaEntity} pertenecen a otras
 * features y su constructor es {@code protected}: desde aqui se mockean como
 * filas, no como entidades de dominio (no tienen invariantes propias).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticImagingJpaMapper — ida y vuelta dominio <-> entidad")
class DiagnosticImagingJpaMapperTest {

    private final DiagnosticImagingJpaMapper mapper = new DiagnosticImagingJpaMapper();

    @Mock
    private DiagnosticImagingTypeJpaEntity typeEntity;
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
        @DisplayName("copia cada campo y engancha las cuatro asociaciones recibidas")
        void copia_los_campos_y_las_relaciones() {
            DiagnosticImaging imaging = DiagnosticImagingMother.persistida();

            DiagnosticImagingJpaEntity entity = mapper.toJpa(imaging, typeEntity, animalEntity,
                    consultationEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(imaging.getId());
            assertThat(entity.getDate()).isEqualTo(imaging.getDate());
            assertThat(entity.getClinicalSigns()).isEqualTo(imaging.getClinicalSigns());
            assertThat(entity.getStudyType()).isEqualTo(imaging.getStudyType());
            assertThat(entity.getDiagnosis()).isEqualTo(imaging.getDiagnosis());
            assertThat(entity.getObservations()).isEqualTo(imaging.getObservations());
            assertThat(entity.getStatus()).isEqualTo(imaging.getStatus().name());
            assertThat(entity.getDiagnosticImagingType()).isSameAs(typeEntity);
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getConsultation()).isSameAs(consultationEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(imaging.getCreatedDate());
            assertThat(entity.isEnabled()).isEqualTo(imaging.isEnabled());
        }

        @Test
        @DisplayName("una imagen nueva viaja sin id para que lo genere la base")
        void una_imagen_nueva_viaja_sin_id() {
            DiagnosticImagingJpaEntity entity = mapper.toJpa(DiagnosticImagingMother.valida(),
                    typeEntity, animalEntity, consultationEntity, companyEntity);

            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("una imagen sin consulta viaja con la asociacion de consulta en null")
        void sin_consulta_viaja_con_la_asociacion_en_null() {
            DiagnosticImagingJpaEntity entity = mapper.toJpa(DiagnosticImagingMother.sinConsulta(),
                    typeEntity, animalEntity, null, companyEntity);

            assertThat(entity.getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("construye los cuatro Ref leyendo las asociaciones hidratadas")
        void construye_los_refs_desde_las_asociaciones() {
            when(typeEntity.getId()).thenReturn(DiagnosticImagingMother.TYPE_ID);
            when(typeEntity.getName()).thenReturn("Radiografia");
            when(animalEntity.getId()).thenReturn(DiagnosticImagingMother.ANIMAL_ID);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");
            when(consultationEntity.getId()).thenReturn(DiagnosticImagingMother.CONSULTATION_ID);
            when(consultationEntity.getDate()).thenReturn(DiagnosticImagingMother.FECHA);
            when(companyEntity.getId()).thenReturn(DiagnosticImagingMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("900123456");

            DiagnosticImagingJpaEntity entity = new DiagnosticImagingJpaEntity();
            entity.setId(DiagnosticImagingMother.IMAGING_ID);
            entity.setDate(DiagnosticImagingMother.FECHA);
            entity.setDiagnosticImagingType(typeEntity);
            entity.setClinicalSigns("Cojera pata trasera");
            entity.setStudyType("Radiografia de cadera");
            entity.setDiagnosis("Displasia leve");
            entity.setObservations("Control en 30 dias");
            entity.setStatus("PENDIENTE");
            entity.setAnimal(animalEntity);
            entity.setConsultation(consultationEntity);
            entity.setCompany(companyEntity);
            entity.setCreatedDate(DiagnosticImagingMother.CREADO);
            entity.setEnabled(true);

            DiagnosticImaging domain = mapper.toDomain(entity);

            assertThat(domain.getDiagnosticImagingType()).isEqualTo(
                    new DiagnosticImagingTypeRef(DiagnosticImagingMother.TYPE_ID, "Radiografia"));
            assertThat(domain.getAnimal()).isEqualTo(
                    new AnimalRef(DiagnosticImagingMother.ANIMAL_ID, "Firulais", "A-001"));
            assertThat(domain.getConsultation()).isEqualTo(new ConsultationRef(
                    DiagnosticImagingMother.CONSULTATION_ID, DiagnosticImagingMother.FECHA));
            assertThat(domain.getCompany()).isEqualTo(new CompanyRef(
                    DiagnosticImagingMother.COMPANY_ID, "Clinica Norte", "900123456"));
        }

        @Test
        @DisplayName("una entidad sin consulta asociada deja consultation en null")
        void sin_consulta_asociada_deja_consultation_en_null() {
            when(typeEntity.getId()).thenReturn(DiagnosticImagingMother.TYPE_ID);
            when(typeEntity.getName()).thenReturn("Radiografia");
            when(animalEntity.getId()).thenReturn(DiagnosticImagingMother.ANIMAL_ID);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");
            when(companyEntity.getId()).thenReturn(DiagnosticImagingMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("900123456");

            DiagnosticImagingJpaEntity entity = new DiagnosticImagingJpaEntity();
            entity.setId(DiagnosticImagingMother.IMAGING_ID);
            entity.setDate(DiagnosticImagingMother.FECHA);
            entity.setDiagnosticImagingType(typeEntity);
            entity.setClinicalSigns("Cojera pata trasera");
            entity.setStudyType("Radiografia de cadera");
            entity.setDiagnosis("Displasia leve");
            entity.setStatus("PENDIENTE");
            entity.setAnimal(animalEntity);
            entity.setConsultation(null);
            entity.setCompany(companyEntity);
            entity.setCreatedDate(DiagnosticImagingMother.CREADO);
            entity.setEnabled(true);

            DiagnosticImaging domain = mapper.toDomain(entity);

            assertThat(domain.getConsultation()).isNull();
        }

        @Test
        @DisplayName("la sobrecarga con refs no toca las asociaciones de la entidad")
        void la_sobrecarga_con_refs_no_toca_las_relaciones() {
            DiagnosticImagingJpaEntity entity = new DiagnosticImagingJpaEntity();
            entity.setId(DiagnosticImagingMother.IMAGING_ID);
            entity.setDate(DiagnosticImagingMother.FECHA);
            entity.setClinicalSigns("Cojera pata trasera");
            entity.setStudyType("Radiografia de cadera");
            entity.setDiagnosis("Displasia leve");
            entity.setStatus("PENDIENTE");
            entity.setCreatedDate(DiagnosticImagingMother.CREADO);
            entity.setEnabled(false);

            DiagnosticImaging domain = mapper.toDomain(entity, DiagnosticImagingMother.TIPO,
                    DiagnosticImagingMother.MASCOTA, DiagnosticImagingMother.CONSULTA,
                    DiagnosticImagingMother.EMPRESA);

            assertThat(domain.getDiagnosticImagingType()).isEqualTo(DiagnosticImagingMother.TIPO);
            assertThat(domain.getAnimal()).isEqualTo(DiagnosticImagingMother.MASCOTA);
            assertThat(domain.getConsultation()).isEqualTo(DiagnosticImagingMother.CONSULTA);
            assertThat(domain.getCompany()).isEqualTo(DiagnosticImagingMother.EMPRESA);
            assertThat(domain.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio -> entidad -> dominio conserva todos los campos")
        void conserva_todos_los_campos() {
            DiagnosticImaging original = DiagnosticImagingMother.persistida();

            DiagnosticImagingJpaEntity entity = mapper.toJpa(original, typeEntity, animalEntity,
                    consultationEntity, companyEntity);
            DiagnosticImaging vuelta = mapper.toDomain(entity, original.getDiagnosticImagingType(),
                    original.getAnimal(), original.getConsultation(), original.getCompany());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getDate()).isEqualTo(original.getDate());
            assertThat(vuelta.getClinicalSigns()).isEqualTo(original.getClinicalSigns());
            assertThat(vuelta.getStudyType()).isEqualTo(original.getStudyType());
            assertThat(vuelta.getDiagnosis()).isEqualTo(original.getDiagnosis());
            assertThat(vuelta.getObservations()).isEqualTo(original.getObservations());
            assertThat(vuelta.getStatus()).isEqualTo(original.getStatus());
            assertThat(vuelta.getDiagnosticImagingType())
                    .isEqualTo(original.getDiagnosticImagingType());
            assertThat(vuelta.getAnimal()).isEqualTo(original.getAnimal());
            assertThat(vuelta.getConsultation()).isEqualTo(original.getConsultation());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
