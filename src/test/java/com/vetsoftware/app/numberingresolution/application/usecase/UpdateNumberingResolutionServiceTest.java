package com.vetsoftware.app.numberingresolution.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.numberingresolution.application.command.UpdateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.out.BranchQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import com.vetsoftware.app.numberingresolution.testsupport.NumberingResolutionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateNumberingResolutionService")
class UpdateNumberingResolutionServiceTest {

    @Mock
    private NumberingResolutionRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;

    @InjectMocks
    private UpdateNumberingResolutionService service;

    private void resolucionExiste() {
        when(repository.findByIdAndCompanyId(NumberingResolutionMother.RESOLUTION_ID,
                NumberingResolutionMother.COMPANY_ID))
                .thenReturn(Optional.of(NumberingResolutionMother.activaDeEmpresa()));
    }

    private void empresaExiste() {
        when(companyQueryPort.findById(NumberingResolutionMother.COMPANY_ID))
                .thenReturn(Optional.of(NumberingResolutionMother.EMPRESA));
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza sin revisar solapamiento si el alcance (empresa, sede, tipo) no cambia")
        void actualiza_sin_revisar_solapamiento_si_el_alcance_no_cambia() {
            resolucionExiste();
            empresaExiste();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            NumberingResolutionDto dto = service
                    .execute(NumberingResolutionMother.comandoActualizar());

            assertThat(dto.prefix()).isEqualTo("NUEV");
            assertThat(dto.rangeTo()).isEqualTo(299L);
            verify(repository, never()).existsActiveByCompanyBranchAndType(any(), any(), any());
        }

        @Test
        @DisplayName("actualiza cuando cambia de sede y no hay conflicto en el nuevo alcance")
        void actualiza_cuando_cambia_de_sede_sin_conflicto() {
            resolucionExiste();
            empresaExiste();
            when(branchQueryPort.existsByIdAndCompanyId(NumberingResolutionMother.BRANCH_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(true);
            when(repository.existsActiveByCompanyBranchAndType(NumberingResolutionMother.COMPANY_ID,
                    NumberingResolutionMother.BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UpdateNumberingResolutionCommand comando = new UpdateNumberingResolutionCommand(
                    NumberingResolutionMother.RESOLUTION_ID, ElectronicDocumentType.FE_VENTA,
                    "18760000002", NumberingResolutionMother.EXPEDIDA, "NUEV", 100L, 299L,
                    NumberingResolutionMother.DESDE, NumberingResolutionMother.HASTA, "otra-clave",
                    NumberingResolutionMother.BRANCH_ID, NumberingResolutionMother.COMPANY_ID);

            NumberingResolutionDto dto = service.execute(comando);

            assertThat(dto.branchId()).isEqualTo(NumberingResolutionMother.BRANCH_ID);
        }

        @Test
        @DisplayName("actualiza cuando cambia el tipo de documento y no hay conflicto en el nuevo alcance")
        void actualiza_cuando_cambia_el_tipo_de_documento_sin_conflicto() {
            resolucionExiste();
            empresaExiste();
            when(repository.existsActiveByCompanyBranchAndType(NumberingResolutionMother.COMPANY_ID,
                    null, ElectronicDocumentType.NOTA_CREDITO)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UpdateNumberingResolutionCommand comando = new UpdateNumberingResolutionCommand(
                    NumberingResolutionMother.RESOLUTION_ID, ElectronicDocumentType.NOTA_CREDITO,
                    "18760000002", NumberingResolutionMother.EXPEDIDA, "NUEV", 100L, 299L,
                    NumberingResolutionMother.DESDE, NumberingResolutionMother.HASTA, "otra-clave",
                    null, NumberingResolutionMother.COMPANY_ID);

            NumberingResolutionDto dto = service.execute(comando);

            assertThat(dto.documentType()).isEqualTo(ElectronicDocumentType.NOTA_CREDITO);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no actualiza una resolucion que no existe")
        void no_actualiza_una_resolucion_inexistente() {
            when(repository.findByIdAndCompanyId(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.comandoActualizar()))
                    .isInstanceOf(NumberingResolutionNotFoundException.class)
                    .hasMessageContaining("Numbering resolution not found: "
                            + NumberingResolutionMother.RESOLUTION_ID);

            verifyNoInteractions(companyQueryPort, branchQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no actualiza si la empresa no existe")
        void no_actualiza_si_la_empresa_no_existe() {
            resolucionExiste();
            when(companyQueryPort.findById(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + NumberingResolutionMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no actualiza si la nueva sede no pertenece a la empresa")
        void no_actualiza_si_la_sede_no_pertenece_a_la_empresa() {
            resolucionExiste();
            empresaExiste();
            when(branchQueryPort.existsByIdAndCompanyId(NumberingResolutionMother.BRANCH_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(false);
            UpdateNumberingResolutionCommand comando = new UpdateNumberingResolutionCommand(
                    NumberingResolutionMother.RESOLUTION_ID, ElectronicDocumentType.FE_VENTA,
                    "18760000002", NumberingResolutionMother.EXPEDIDA, "NUEV", 100L, 299L,
                    NumberingResolutionMother.DESDE, NumberingResolutionMother.HASTA, "otra-clave",
                    NumberingResolutionMother.BRANCH_ID, NumberingResolutionMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Branch not found: " + NumberingResolutionMother.BRANCH_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no actualiza si cambia de sede y ya hay una activa en el nuevo alcance")
        void no_actualiza_si_cambia_de_sede_y_ya_hay_activa() {
            resolucionExiste();
            empresaExiste();
            when(branchQueryPort.existsByIdAndCompanyId(NumberingResolutionMother.BRANCH_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(true);
            when(repository.existsActiveByCompanyBranchAndType(NumberingResolutionMother.COMPANY_ID,
                    NumberingResolutionMother.BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(true);
            UpdateNumberingResolutionCommand comando = new UpdateNumberingResolutionCommand(
                    NumberingResolutionMother.RESOLUTION_ID, ElectronicDocumentType.FE_VENTA,
                    "18760000001", NumberingResolutionMother.EXPEDIDA, "SETP", 100L, 199L,
                    NumberingResolutionMother.DESDE, NumberingResolutionMother.HASTA, "clave",
                    NumberingResolutionMother.BRANCH_ID, NumberingResolutionMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(NumberingResolutionAlreadyActiveException.class)
                    .hasMessageContaining("ya tiene una resolución de numeración activa");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("la resolucion de OTRA empresa es 404 y no se apropia")
        void resolucion_de_otra_empresa_es_not_found_y_no_escribe() {
            // El caso que la anotacion @authz.isMyCompany NO cubre: el atacante declara
            // SU empresa (asi que el gate pasa) y apunta al id de la resolucion ajena. Con
            // la lectura acotada la fila no aparece, asi que no hay update que reescriba su
            // company: la apropiacion se convierte en un 404.
            when(repository.findByIdAndCompanyId(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.comandoActualizar()))
                    .isInstanceOf(NumberingResolutionNotFoundException.class);

            verifyNoInteractions(companyQueryPort, branchQueryPort);
            verify(repository, never()).save(any());
            verify(repository, never()).findById(any());
        }
    }
}
