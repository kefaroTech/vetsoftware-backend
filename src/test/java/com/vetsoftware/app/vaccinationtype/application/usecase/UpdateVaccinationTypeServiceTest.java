package com.vetsoftware.app.vaccinationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateVaccinationTypeService")
class UpdateVaccinationTypeServiceTest {

    @Mock
    private VaccinationTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateVaccinationTypeService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("resuelve la empresa por el puerto y persiste los cambios")
        void resuelve_la_empresa_y_persiste_los_cambios() {
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.propia()));
            when(companyQueryPort.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VaccinationTypeDto dto = service.execute(VaccinationTypeMother.comandoActualizar());

            ArgumentCaptor<VaccinationType> guardado = ArgumentCaptor
                    .forClass(VaccinationType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Moquillo");
            assertThat(guardado.getValue().getDescription()).isEqualTo("Vacuna contra el moquillo");
            assertThat(dto.name()).isEqualTo("Moquillo");
        }

        @Test
        @DisplayName("un comando general sin companyId no consulta el puerto de empresa")
        void un_comando_general_sin_company_id_no_consulta_el_puerto() {
            when(repository.findById(VaccinationTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.general()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VaccinationTypeDto dto = service
                    .execute(new UpdateVaccinationTypeCommand(VaccinationTypeMother.TYPE_ID,
                            "Vacuna universal", "Disponible para todas", null, true));

            verifyNoInteractions(companyQueryPort);
            assertThat(dto.general()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("un id inexistente lanza y no consulta la empresa ni guarda")
        void un_id_inexistente_lanza_y_no_guarda() {
            when(repository.findOwnedByIdAndCompanyId(99L, VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new UpdateVaccinationTypeCommand(99L, "Rabia",
                    "Vacuna antirrabica", VaccinationTypeMother.COMPANY_ID, false)))
                    .isInstanceOf(VaccinationTypeNotFoundException.class)
                    .hasMessageContaining("VaccinationType not found: 99");

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no persiste si la empresa nueva no existe")
        void no_persiste_si_la_empresa_nueva_no_existe() {
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.propia()));
            when(companyQueryPort.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + VaccinationTypeMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un nombre invalido lanza y no guarda")
        void un_nombre_invalido_lanza_y_no_guarda() {
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.propia()));
            when(companyQueryPort.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.CLINICA));

            assertThatThrownBy(() -> service
                    .execute(new UpdateVaccinationTypeCommand(VaccinationTypeMother.TYPE_ID, "",
                            "Vacuna antirrabica", VaccinationTypeMother.COMPANY_ID, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

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
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.comandoActualizar()))
                    .isInstanceOf(VaccinationTypeNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se apropia desde una empresa")
        void la_fila_general_no_se_apropia_desde_una_empresa() {
            // El finder de ESCRITURA excluye las generales: si se cargaran, el update
            // les pondria el company_id del llamador y dejarian de ser de todos.
            when(repository.findOwnedByIdAndCompanyId(51L, VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new UpdateVaccinationTypeCommand(51L,
                    "Mia ahora", "Robada", VaccinationTypeMother.COMPANY_ID, false)))
                    .isInstanceOf(VaccinationTypeNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }
    }
}
