package com.vetsoftware.app.numberingresolution.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.numberingresolution.application.command.CreateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.out.BranchQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import com.vetsoftware.app.numberingresolution.testsupport.NumberingResolutionMother;
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
@DisplayName("CreateNumberingResolutionService")
class CreateNumberingResolutionServiceTest {

    @Mock
    private NumberingResolutionRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;

    @InjectMocks
    private CreateNumberingResolutionService service;

    @Captor
    private ArgumentCaptor<NumberingResolution> resolucionCaptor;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("crea la resolucion de EMPRESA sin consultar el puerto de sedes")
        void crea_la_resolucion_de_empresa_sin_consultar_sedes() {
            when(companyQueryPort.findById(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.of(NumberingResolutionMother.EMPRESA));
            when(repository.existsActiveByCompanyBranchAndType(NumberingResolutionMother.COMPANY_ID,
                    null, ElectronicDocumentType.FE_VENTA)).thenReturn(false);
            when(repository.save(resolucionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            NumberingResolutionDto dto = service.execute(NumberingResolutionMother.comandoCrear());

            assertThat(resolucionCaptor.getValue().getBranchId()).isNull();
            assertThat(resolucionCaptor.getValue().getCurrentNumber()).isEqualTo(100L);
            assertThat(dto.resolutionNumber()).isEqualTo("18760000001");
            verifyNoInteractions(branchQueryPort);
        }

        @Test
        @DisplayName("crea la resolucion de una SEDE cuando la sede pertenece a la empresa")
        void crea_la_resolucion_de_una_sede_valida() {
            CreateNumberingResolutionCommand comando = new CreateNumberingResolutionCommand(
                    ElectronicDocumentType.FE_VENTA, "18760000001",
                    NumberingResolutionMother.EXPEDIDA, "SEDE", 100L, 199L,
                    NumberingResolutionMother.DESDE, NumberingResolutionMother.HASTA, "clave",
                    NumberingResolutionMother.BRANCH_ID, NumberingResolutionMother.COMPANY_ID);
            when(companyQueryPort.findById(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.of(NumberingResolutionMother.EMPRESA));
            when(branchQueryPort.existsByIdAndCompanyId(NumberingResolutionMother.BRANCH_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(true);
            when(repository.existsActiveByCompanyBranchAndType(NumberingResolutionMother.COMPANY_ID,
                    NumberingResolutionMother.BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(false);
            when(repository.save(resolucionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando);

            assertThat(resolucionCaptor.getValue().getBranchId())
                    .isEqualTo(NumberingResolutionMother.BRANCH_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no crea la resolucion si la empresa no existe")
        void no_crea_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + NumberingResolutionMother.COMPANY_ID);

            verifyNoInteractions(repository, branchQueryPort);
        }

        @Test
        @DisplayName("no crea la resolucion si la sede no pertenece a la empresa")
        void no_crea_si_la_sede_no_pertenece_a_la_empresa() {
            CreateNumberingResolutionCommand comando = new CreateNumberingResolutionCommand(
                    ElectronicDocumentType.FE_VENTA, "18760000001",
                    NumberingResolutionMother.EXPEDIDA, "SEDE", 100L, 199L,
                    NumberingResolutionMother.DESDE, NumberingResolutionMother.HASTA, "clave",
                    NumberingResolutionMother.BRANCH_ID, NumberingResolutionMother.COMPANY_ID);
            when(companyQueryPort.findById(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.of(NumberingResolutionMother.EMPRESA));
            when(branchQueryPort.existsByIdAndCompanyId(NumberingResolutionMother.BRANCH_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Branch not found: " + NumberingResolutionMother.BRANCH_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("no crea la resolucion si ya hay una activa en ese alcance")
        void no_crea_si_ya_hay_una_activa_en_el_alcance() {
            when(companyQueryPort.findById(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.of(NumberingResolutionMother.EMPRESA));
            when(repository.existsActiveByCompanyBranchAndType(NumberingResolutionMother.COMPANY_ID,
                    null, ElectronicDocumentType.FE_VENTA)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.comandoCrear()))
                    .isInstanceOf(NumberingResolutionAlreadyActiveException.class)
                    .hasMessageContaining("ya tiene una resolución de numeración activa");

            org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
        }
    }
}
