package com.vetsoftware.app.medicamentprescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.medicamentprescription.application.port.out.PrescriptionQueryPort;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMedicamentPrescriptionService")
class CreateMedicamentPrescriptionServiceTest {

    @Mock
    private MedicamentPrescriptionRepository repository;
    @Mock
    private PrescriptionQueryPort prescriptionQueryPort;
    @Mock
    private MedicamentQueryPort medicamentQueryPort;

    @InjectMocks
    private CreateMedicamentPrescriptionService service;

    @Captor
    private ArgumentCaptor<MedicamentPrescription> captor;

    /**
     * Las dos referencias se resuelven acotadas por empresa: el caso de uso no
     * carga ninguna entidad propia, asi que estos dos puertos son lo unico que
     * separa una receta de la empresa del que escribe de la de otro tenant.
     */
    private void referenciasResueltas() {
        when(prescriptionQueryPort.findByIdAndCompanyId(
                MedicamentPrescriptionMother.PRESCRIPTION_ID,
                MedicamentPrescriptionMother.COMPANY_ID))
                .thenReturn(Optional.of(MedicamentPrescriptionMother.RECETA));
        when(medicamentQueryPort.findAvailableById(MedicamentPrescriptionMother.MEDICAMENT_ID,
                MedicamentPrescriptionMother.COMPANY_ID))
                .thenReturn(Optional.of(MedicamentPrescriptionMother.MEDICAMENTO));
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la linea con las referencias resueltas por los puertos")
        void persiste_la_linea_con_las_referencias_resueltas() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(MedicamentPrescriptionMother.persistida());

            service.execute(MedicamentPrescriptionMother.comandoCrear());

            verify(repository).save(captor.capture());
            MedicamentPrescription guardada = captor.getValue();
            assertThat(guardada.getMedicament())
                    .isEqualTo(MedicamentPrescriptionMother.MEDICAMENTO);
            assertThat(guardada.getPrescription()).isEqualTo(MedicamentPrescriptionMother.RECETA);
            assertThat(guardada.getPresentation()).isEqualTo("Tableta");
            assertThat(guardada.getQuantity()).isEqualTo(2.0);
            assertThat(guardada.getPosology()).isEqualTo("Cada 12 horas por 7 dias");
            assertThat(guardada.getObservation()).isEqualTo("Con alimento");
            assertThat(guardada.getId()).isNull();
            assertThat(guardada.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el DTO de la linea ya persistida, con su id")
        void devuelve_el_dto_de_la_linea_ya_persistida() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(MedicamentPrescriptionMother.persistida());

            MedicamentPrescriptionDto dto = service
                    .execute(MedicamentPrescriptionMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(MedicamentPrescriptionMother.ID);
            assertThat(dto.presentation()).isEqualTo("Tableta");
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("receta inexistente: no consulta el medicamento ni persiste")
        void receta_inexistente() {
            when(prescriptionQueryPort.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MedicamentPrescriptionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prescription not found: "
                            + MedicamentPrescriptionMother.PRESCRIPTION_ID);

            verifyNoInteractions(medicamentQueryPort, repository);
        }

        @Test
        @DisplayName("medicamento inexistente: no persiste")
        void medicamento_inexistente() {
            when(prescriptionQueryPort.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.RECETA));
            when(medicamentQueryPort.findAvailableById(MedicamentPrescriptionMother.MEDICAMENT_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MedicamentPrescriptionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Medicament not found: " + MedicamentPrescriptionMother.MEDICAMENT_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        private static final Long OTRA_EMPRESA = 999L;

        @Test
        @DisplayName("la receta de otra empresa no resuelve: no consulta el medicamento ni persiste")
        void receta_de_otra_empresa_no_resuelve() {
            // La fuga que cerro este caso: el servicio resolvia la receta con
            // findById a secas, asi que bastaba adivinar el prescriptionId de otro
            // tenant para colgarle un medicamento. No hay ninguna entidad propia
            // cargada que pudiera atrapar el error mas adelante.
            when(prescriptionQueryPort.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(MedicamentPrescriptionMother.comandoCrear(OTRA_EMPRESA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prescription not found: "
                            + MedicamentPrescriptionMother.PRESCRIPTION_ID);

            verifyNoInteractions(medicamentQueryPort, repository);
        }

        @Test
        @DisplayName("el medicamento de otra empresa tampoco resuelve: no persiste")
        void medicamento_de_otra_empresa_no_resuelve() {
            when(prescriptionQueryPort.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.RECETA));
            when(medicamentQueryPort.findAvailableById(MedicamentPrescriptionMother.MEDICAMENT_ID,
                    OTRA_EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(MedicamentPrescriptionMother.comandoCrear(OTRA_EMPRESA)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Medicament not found: " + MedicamentPrescriptionMother.MEDICAMENT_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sin empresa (camino SYSTEM) resuelve sin acotar y persiste")
        void sin_empresa_resuelve_sin_acotar() {
            // companyId null es lo que pone el controller via currentCompanyIdOrNull()
            // cuando quien llama es SYSTEM: ese si puede operar entre empresas.
            when(prescriptionQueryPort.findById(MedicamentPrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.RECETA));
            when(medicamentQueryPort.findById(MedicamentPrescriptionMother.MEDICAMENT_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.MEDICAMENTO));
            when(repository.save(any())).thenReturn(MedicamentPrescriptionMother.persistida());

            MedicamentPrescriptionDto dto = service
                    .execute(MedicamentPrescriptionMother.comandoCrear(null));

            assertThat(dto.id()).isEqualTo(MedicamentPrescriptionMother.ID);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una cantidad no positiva no llega a persistirse")
        void una_cantidad_no_positiva_no_llega_a_persistirse() {
            referenciasResueltas();
            var comando = new com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand(
                    MedicamentPrescriptionMother.MEDICAMENT_ID, "Tableta", -1.0,
                    "Cada 12 horas por 7 dias", "Con alimento",
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be positive");

            verify(repository, never()).save(any());
        }
    }
}
