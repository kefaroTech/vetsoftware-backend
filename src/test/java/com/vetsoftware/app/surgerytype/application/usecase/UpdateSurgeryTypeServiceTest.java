package com.vetsoftware.app.surgerytype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgerytype.application.command.UpdateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import com.vetsoftware.app.surgerytype.testsupport.SurgeryTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSurgeryTypeService")
class UpdateSurgeryTypeServiceTest {

    @Mock
    private SurgeryTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateSurgeryTypeService service;

    private void tipoExisteEnLaEmpresa() {
        when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                SurgeryTypeMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryTypeMother.propioDeEmpresa()));
    }

    /**
     * Rama SYSTEM del ternario: sin empresa en el command, la lectura no se acota.
     */
    private void tipoExisteSinEmpresa() {
        when(repository.findById(SurgeryTypeMother.SURGERY_TYPE_ID))
                .thenReturn(Optional.of(SurgeryTypeMother.propioDeEmpresa()));
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el tipo con la empresa resuelta por el puerto")
        void actualiza_el_tipo_con_la_empresa_resuelta() {
            tipoExisteEnLaEmpresa();
            when(companyQueryPort.findById(SurgeryTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryTypeMother.EMPRESA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SurgeryTypeDto dto = service.execute(SurgeryTypeMother.comandoActualizarPropio());

            assertThat(dto.name()).isEqualTo("Castracion avanzada");
            assertThat(dto.description()).isEqualTo("Nueva descripcion");
        }

        @Test
        @DisplayName("actualizar a un tipo general no consulta el puerto de empresa")
        void actualizar_a_general_no_consulta_el_puerto() {
            tipoExisteSinEmpresa();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UpdateSurgeryTypeCommand comando = new UpdateSurgeryTypeCommand(
                    SurgeryTypeMother.SURGERY_TYPE_ID, "Cirugia general", "Procedimiento estandar",
                    null, true);

            SurgeryTypeDto dto = service.execute(comando);

            verifyNoInteractions(companyQueryPort);
            assertThat(dto.general()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no actualiza un tipo que no existe")
        void no_actualiza_un_tipo_inexistente() {
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.comandoActualizarPropio()))
                    .isInstanceOf(SurgeryTypeNotFoundException.class).hasMessageContaining(
                            "SurgeryType not found: " + SurgeryTypeMother.SURGERY_TYPE_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no actualiza si la empresa no existe")
        void no_actualiza_si_la_empresa_no_existe() {
            tipoExisteEnLaEmpresa();
            when(companyQueryPort.findById(SurgeryTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.comandoActualizarPropio()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + SurgeryTypeMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("el tipo de OTRA empresa es 404 y no se apropia")
        void tipo_de_otra_empresa_es_not_found_y_no_escribe() {
            // El caso que @authz.isMyCompany NO cubre: el atacante declara SU empresa
            // (el gate pasa) y apunta al id ajeno. Sin fila que cargar no hay update que
            // le reescriba el company_id.
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.comandoActualizarPropio()))
                    .isInstanceOf(SurgeryTypeNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se apropia desde una empresa")
        void la_fila_general_no_se_apropia_desde_una_empresa() {
            // El finder de ESCRITURA excluye las generales: si se cargaran, el update
            // les pondria el company_id del llamador y dejarian de ser de todos.
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());
            UpdateSurgeryTypeCommand comando = new UpdateSurgeryTypeCommand(
                    SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID, "Mia ahora", "Robada",
                    SurgeryTypeMother.COMPANY_ID, false);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(SurgeryTypeNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }
    }
}
