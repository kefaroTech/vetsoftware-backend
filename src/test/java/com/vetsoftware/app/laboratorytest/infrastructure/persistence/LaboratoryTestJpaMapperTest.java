package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unico lugar que conoce dominio y entidad JPA a la vez. Las entidades JPA de
 * otras features se mockean porque su constructor sin argumentos es
 * {@code protected} y no son instanciables desde este paquete: no tienen
 * logica, son portadores de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LaboratoryTestJpaMapper")
class LaboratoryTestJpaMapperTest {

    private final LaboratoryTestJpaMapper mapper = new LaboratoryTestJpaMapper();

    @Mock
    private LaboratoryTestTypeJpaEntity testTypeEntity;
    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private ConsultationJpaEntity consultationEntity;
    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private EmployeeJpaEntity employeeEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            LaboratoryTest muestra = LaboratoryTestMother.validada();

            LaboratoryTestJpaEntity entity = mapper.toJpa(muestra, testTypeEntity, animalEntity,
                    consultationEntity, companyEntity, employeeEntity);

            assertThat(entity.getId()).isEqualTo(LaboratoryTestMother.ID);
            assertThat(entity.getDate()).isEqualTo(LaboratoryTestMother.FECHA);
            assertThat(entity.getQuantity()).isEqualTo(2);
            assertThat(entity.getDiagnosis()).isEqualTo("Anemia regenerativa");
            assertThat(entity.getStatus()).isEqualTo("COMPLETED");
            assertThat(entity.getPrioridad()).isEqualTo("URGENTE");
            assertThat(entity.getBranchId()).isEqualTo(LaboratoryTestMother.BRANCH_ID);
            assertThat(entity.getProcessedDate()).isEqualTo(LaboratoryTestMother.PROCESADO);
            assertThat(entity.getCreatedDate()).isEqualTo(LaboratoryTestMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            LaboratoryTestJpaEntity entity = mapper.toJpa(LaboratoryTestMother.validada(),
                    testTypeEntity, animalEntity, consultationEntity, companyEntity,
                    employeeEntity);

            assertThat(entity.getTestType()).isSameAs(testTypeEntity);
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getConsultation()).isSameAs(consultationEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getProcessedBy()).isSameAs(employeeEntity);
        }

        @Test
        @DisplayName("sin consulta ni procesador deja esas asociaciones en null")
        void sin_consulta_ni_procesador_deja_esas_asociaciones_en_null() {
            LaboratoryTestJpaEntity entity = mapper.toJpa(
                    LaboratoryTestMother.sinAsociacionesOpcionales(), testTypeEntity, animalEntity,
                    null, companyEntity, null);

            assertThat(entity.getConsultation()).isNull();
            assertThat(entity.getProcessedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            LaboratoryTest original = LaboratoryTestMother.validada();
            LaboratoryTestJpaEntity entity = mapper.toJpa(original, testTypeEntity, animalEntity,
                    consultationEntity, companyEntity, employeeEntity);

            // Este overload existe para no inicializar los proxies de getReferenceById:
            // si leyera entity.getAnimal(), Hibernate lanzaria un SELECT extra en save.
            LaboratoryTest vuelta = mapper.toDomain(entity, original.getTestType(),
                    original.getAnimal(), original.getConsultation(), original.getCompany(),
                    original.getProcessedBy());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }

        @Test
        @DisplayName("la ida y vuelta dominio a entidad a dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            LaboratoryTest original = LaboratoryTestMother.sinAsociacionesOpcionales();
            LaboratoryTestJpaEntity entity = mapper.toJpa(original, testTypeEntity, animalEntity,
                    null, companyEntity, null);

            LaboratoryTest vuelta = mapper.toDomain(entity, original.getTestType(),
                    original.getAnimal(), null, original.getCompany(), null);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(testTypeEntity.getId()).thenReturn(LaboratoryTestMother.HEMOGRAMA.id());
            when(testTypeEntity.getName()).thenReturn(LaboratoryTestMother.HEMOGRAMA.name());
            when(animalEntity.getId()).thenReturn(LaboratoryTestMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(LaboratoryTestMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(LaboratoryTestMother.FIRULAIS.code());
            when(consultationEntity.getId()).thenReturn(LaboratoryTestMother.CONSULTA.id());
            when(consultationEntity.getDate()).thenReturn(LaboratoryTestMother.CONSULTA.date());
            when(companyEntity.getId()).thenReturn(LaboratoryTestMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(LaboratoryTestMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(LaboratoryTestMother.CLINICA.identifier());
            when(employeeEntity.getId()).thenReturn(LaboratoryTestMother.BACTERIOLOGA.id());
            when(employeeEntity.getEmployeeCode())
                    .thenReturn(LaboratoryTestMother.BACTERIOLOGA.employeeCode());
            when(employeeEntity.getName()).thenReturn(LaboratoryTestMother.BACTERIOLOGA.name());

            LaboratoryTestJpaEntity entity = mapper.toJpa(LaboratoryTestMother.validada(),
                    testTypeEntity, animalEntity, consultationEntity, companyEntity,
                    employeeEntity);
            entity.setTestType(testTypeEntity);
            entity.setAnimal(animalEntity);
            entity.setConsultation(consultationEntity);
            entity.setCompany(companyEntity);
            entity.setProcessedBy(employeeEntity);

            LaboratoryTest muestra = mapper.toDomain(entity);

            assertThat(muestra.getTestType()).isEqualTo(LaboratoryTestMother.HEMOGRAMA);
            assertThat(muestra.getAnimal()).isEqualTo(LaboratoryTestMother.FIRULAIS);
            assertThat(muestra.getConsultation()).isEqualTo(LaboratoryTestMother.CONSULTA);
            assertThat(muestra.getCompany()).isEqualTo(LaboratoryTestMother.CLINICA);
            assertThat(muestra.getProcessedBy()).isEqualTo(LaboratoryTestMother.BACTERIOLOGA);
        }

        @Test
        @DisplayName("sin consulta ni procesador construye el agregado con esos bloques en null")
        void sin_consulta_ni_procesador_construye_con_bloques_en_null() {
            when(testTypeEntity.getId()).thenReturn(LaboratoryTestMother.HEMOGRAMA.id());
            when(testTypeEntity.getName()).thenReturn(LaboratoryTestMother.HEMOGRAMA.name());
            when(animalEntity.getId()).thenReturn(LaboratoryTestMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(LaboratoryTestMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(LaboratoryTestMother.FIRULAIS.code());
            when(companyEntity.getId()).thenReturn(LaboratoryTestMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(LaboratoryTestMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(LaboratoryTestMother.CLINICA.identifier());

            LaboratoryTestJpaEntity entity = mapper.toJpa(
                    LaboratoryTestMother.sinAsociacionesOpcionales(), testTypeEntity, animalEntity,
                    null, companyEntity, null);
            entity.setTestType(testTypeEntity);
            entity.setAnimal(animalEntity);
            entity.setCompany(companyEntity);
            entity.setConsultation(null);
            entity.setProcessedBy(null);
            LocalDate fecha = entity.getDate();
            assertThat(fecha).isEqualTo(LaboratoryTestMother.FECHA);

            LaboratoryTest muestra = mapper.toDomain(entity);

            assertThat(muestra.getConsultation()).isNull();
            assertThat(muestra.getProcessedBy()).isNull();
        }
    }
}
