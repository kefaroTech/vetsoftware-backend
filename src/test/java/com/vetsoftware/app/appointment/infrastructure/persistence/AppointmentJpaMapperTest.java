package com.vetsoftware.app.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ida y vuelta dominio &harr; entidad.
 *
 * <p>
 * <b>Por que se mockean las entidades vecinas.</b> {@code AnimalJpaEntity},
 * {@code OwnerJpaEntity}, {@code EmployeeJpaEntity}, {@code CompanyJpaEntity} y
 * {@code BranchJpaEntity} viven en otros paquetes raiz y declaran su
 * constructor sin argumentos como {@code protected} (lo exige JPA), asi que
 * este test no puede instanciarlas. Solo se leen tres accesores de cada una, de
 * modo que un doble es suficiente y no oculta ninguna invariante: las
 * invariantes que importan son las del dominio, y esas se construyen de verdad.
 * {@code AppointmentJpaEntity} si se instancia: el test vive en su mismo
 * paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentJpaMapper — dominio y entidad de persistencia")
class AppointmentJpaMapperTest {

    private final AppointmentJpaMapper mapper = new AppointmentJpaMapper();

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private OwnerJpaEntity ownerEntity;
    @Mock
    private EmployeeJpaEntity employeeEntity;
    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private BranchJpaEntity branchEntity;

    private AppointmentJpaEntity entidadCompleta() {
        AppointmentJpaEntity entity = new AppointmentJpaEntity();
        entity.setId(AppointmentMother.APPOINTMENT_ID);
        entity.setStartAt(AppointmentMother.INICIO);
        entity.setType(AppointmentType.SURGERY.name());
        entity.setStatus(AppointmentStatus.CONFIRMED.name());
        entity.setNotes("Ayuno previo");
        entity.setCancellationReason(null);
        entity.setAnimal(animalEntity);
        entity.setOwner(ownerEntity);
        entity.setClientName("Walk-in");
        entity.setClientPhone("3001234567");
        entity.setClientEmail("walkin@example.com");
        entity.setEmployee(employeeEntity);
        entity.setCompany(companyEntity);
        entity.setBranch(branchEntity);
        entity.setVersion(7L);
        entity.setEnabled(true);
        entity.setCreatedDate(AppointmentMother.CREADA);
        return entity;
    }

    @Nested
    @DisplayName("Dominio a entidad")
    class DominioAEntidad {

        @Test
        @DisplayName("copia cada campo de la cita y guarda los enums por su nombre")
        void copia_cada_campo_y_guarda_los_enums_por_nombre() {
            Appointment cita = AppointmentMother.conEstado(AppointmentStatus.IN_PROGRESS);

            AppointmentJpaEntity entity = mapper.toJpa(cita, animalEntity, ownerEntity,
                    employeeEntity, companyEntity, branchEntity);

            assertThat(entity.getId()).isEqualTo(AppointmentMother.APPOINTMENT_ID);
            assertThat(entity.getStartAt()).isEqualTo(AppointmentMother.INICIO);
            assertThat(entity.getType()).isEqualTo("CONSULTATION");
            assertThat(entity.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(entity.getNotes()).isEqualTo("Control anual");
            assertThat(entity.getCancellationReason()).isNull();
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getOwner()).isSameAs(ownerEntity);
            assertThat(entity.getEmployee()).isSameAs(employeeEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getBranch()).isSameAs(branchEntity);
            assertThat(entity.getVersion()).isEqualTo(3L);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getCreatedDate()).isEqualTo(AppointmentMother.CREADA);
        }

        @Test
        @DisplayName("acepta una cita de contacto libre con animal y propietario nulos")
        void acepta_una_cita_de_contacto_libre() {
            Appointment cita = AppointmentMother.deContactoLibre("walkin@example.com");

            AppointmentJpaEntity entity = mapper.toJpa(cita, null, null, employeeEntity,
                    companyEntity, branchEntity);

            assertThat(entity.getAnimal()).isNull();
            assertThat(entity.getOwner()).isNull();
            assertThat(entity.getClientName()).isEqualTo("Walk-in");
            assertThat(entity.getClientPhone()).isEqualTo("3001234567");
            assertThat(entity.getClientEmail()).isEqualTo("walkin@example.com");
        }

        @Test
        @DisplayName("persiste el motivo de cancelacion de una cita cancelada")
        void persiste_el_motivo_de_cancelacion() {
            AppointmentJpaEntity entity = mapper.toJpa(
                    AppointmentMother.cancelada("El dueno aviso"), animalEntity, ownerEntity,
                    employeeEntity, companyEntity, branchEntity);

            assertThat(entity.getStatus()).isEqualTo("CANCELLED");
            assertThat(entity.getCancellationReason()).isEqualTo("El dueno aviso");
            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Entidad a dominio")
    class EntidadADominio {

        @Test
        @DisplayName("reconstruye la cita hidratando las referencias desde las entidades vecinas")
        void reconstruye_la_cita_hidratando_las_referencias() {
            when(animalEntity.getId()).thenReturn(AppointmentMother.ANIMAL_ID);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");
            when(ownerEntity.getId()).thenReturn(AppointmentMother.OWNER_ID);
            when(ownerEntity.getName()).thenReturn("Ana Ruiz");
            when(employeeEntity.getId()).thenReturn(AppointmentMother.EMPLOYEE_ID);
            when(employeeEntity.getName()).thenReturn("Dra. Vet");
            when(companyEntity.getId()).thenReturn(AppointmentMother.COMPANY_ID);
            when(branchEntity.getId()).thenReturn(AppointmentMother.BRANCH_ID);
            when(branchEntity.getName()).thenReturn("Principal");
            when(branchEntity.getCode()).thenReturn("PRINCIPAL");

            Appointment cita = mapper.toDomain(entidadCompleta());

            assertThat(cita.getId()).isEqualTo(AppointmentMother.APPOINTMENT_ID);
            assertThat(cita.getType()).isEqualTo(AppointmentType.SURGERY);
            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            assertThat(cita.getAnimal()).isEqualTo(AppointmentMother.FIRULAIS);
            assertThat(cita.getOwner()).isEqualTo(AppointmentMother.DUENO);
            assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.VETERINARIA);
            assertThat(cita.getCompany()).isEqualTo(AppointmentMother.CLINICA);
            assertThat(cita.getBranch()).isEqualTo(AppointmentMother.PRINCIPAL);
            assertThat(cita.getVersion()).isEqualTo(7L);
            assertThat(cita.getCreatedDate()).isEqualTo(AppointmentMother.CREADA);
        }

        @Test
        @DisplayName("deja animal y propietario en null cuando la fila no los tiene")
        void deja_animal_y_propietario_en_null_cuando_la_fila_no_los_tiene() {
            when(employeeEntity.getId()).thenReturn(AppointmentMother.EMPLOYEE_ID);
            when(employeeEntity.getName()).thenReturn("Dra. Vet");
            when(companyEntity.getId()).thenReturn(AppointmentMother.COMPANY_ID);
            when(branchEntity.getId()).thenReturn(AppointmentMother.BRANCH_ID);
            when(branchEntity.getName()).thenReturn("Principal");
            when(branchEntity.getCode()).thenReturn("PRINCIPAL");
            AppointmentJpaEntity entity = entidadCompleta();
            entity.setAnimal(null);
            entity.setOwner(null);

            Appointment cita = mapper.toDomain(entity);

            assertThat(cita.getAnimal()).isNull();
            assertThat(cita.getOwner()).isNull();
            assertThat(cita.getClientName()).isEqualTo("Walk-in");
        }

        @Test
        @DisplayName("la sobrecarga con referencias ya cargadas no toca las entidades vecinas")
        void la_sobrecarga_con_referencias_no_toca_las_entidades_vecinas() {
            Appointment cita = mapper.toDomain(entidadCompleta(), AppointmentMother.FIRULAIS,
                    AppointmentMother.DUENO, AppointmentMother.VETERINARIA,
                    AppointmentMother.CLINICA, AppointmentMother.NORTE);

            assertThat(cita.getBranch()).isEqualTo(AppointmentMother.NORTE);
            assertThat(cita.getStartAt()).isEqualTo(AppointmentMother.INICIO);
            assertThat(cita.getClientEmail()).isEqualTo("walkin@example.com");
        }
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("una cita que va a la entidad y vuelve conserva todos sus datos")
        void una_cita_que_va_y_vuelve_conserva_sus_datos() {
            Appointment original = AppointmentMother.conEstado(AppointmentStatus.ARRIVED);

            AppointmentJpaEntity entity = mapper.toJpa(original, animalEntity, ownerEntity,
                    employeeEntity, companyEntity, branchEntity);
            Appointment vuelta = mapper.toDomain(entity, original.getAnimal(), original.getOwner(),
                    original.getEmployee(), original.getCompany(), original.getBranch());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getStartAt()).isEqualTo(original.getStartAt());
            assertThat(vuelta.getType()).isEqualTo(original.getType());
            assertThat(vuelta.getStatus()).isEqualTo(original.getStatus());
            assertThat(vuelta.getNotes()).isEqualTo(original.getNotes());
            assertThat(vuelta.getAnimal()).isEqualTo(original.getAnimal());
            assertThat(vuelta.getOwner()).isEqualTo(original.getOwner());
            assertThat(vuelta.getEmployee()).isEqualTo(original.getEmployee());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getBranch()).isEqualTo(original.getBranch());
            assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
        }

        @ParameterizedTest
        @EnumSource(AppointmentType.class)
        @DisplayName("todo tipo de cita sobrevive al viaje por su nombre")
        void todo_tipo_de_cita_sobrevive_al_viaje(AppointmentType tipo) {
            AppointmentJpaEntity entity = entidadCompleta();
            entity.setType(tipo.name());

            Appointment cita = mapper.toDomain(entity, AppointmentMother.FIRULAIS,
                    AppointmentMother.DUENO, AppointmentMother.VETERINARIA,
                    AppointmentMother.CLINICA, AppointmentMother.PRINCIPAL);

            assertThat(cita.getType()).isEqualTo(tipo);
        }

        @ParameterizedTest
        @EnumSource(AppointmentStatus.class)
        @DisplayName("todo estado de cita sobrevive al viaje por su nombre")
        void todo_estado_de_cita_sobrevive_al_viaje(AppointmentStatus estado) {
            AppointmentJpaEntity entity = entidadCompleta();
            entity.setStatus(estado.name());

            Appointment cita = mapper.toDomain(entity, AppointmentMother.FIRULAIS,
                    AppointmentMother.DUENO, AppointmentMother.VETERINARIA,
                    AppointmentMother.CLINICA, AppointmentMother.PRINCIPAL);

            assertThat(cita.getStatus()).isEqualTo(estado);
        }
    }
}
