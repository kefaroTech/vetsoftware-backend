package com.vetsoftware.app.vaccinationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
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
@DisplayName("CreateVaccinationTypeService")
class CreateVaccinationTypeServiceTest {

    @Mock
    private VaccinationTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateVaccinationTypeService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("resuelve la empresa por el puerto y persiste el tipo no general")
        void resuelve_la_empresa_y_persiste_el_tipo_no_general() {
            when(companyQueryPort.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VaccinationTypeDto dto = service.execute(VaccinationTypeMother.comandoCrear());

            ArgumentCaptor<VaccinationType> guardado = ArgumentCaptor
                    .forClass(VaccinationType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Rabia");
            assertThat(guardado.getValue().getCompany()).isEqualTo(VaccinationTypeMother.CLINICA);
            assertThat(guardado.getValue().isGeneral()).isFalse();
            assertThat(guardado.getValue().getId()).isNull();
            assertThat(dto.name()).isEqualTo("Rabia");
        }

        @Test
        @DisplayName("un tipo general sin companyId no consulta el puerto de empresa")
        void un_tipo_general_sin_company_id_no_consulta_el_puerto() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VaccinationTypeDto dto = service.execute(new CreateVaccinationTypeCommand(
                    "Vacuna universal", "Disponible para todas", null, true));

            verifyNoInteractions(companyQueryPort);
            assertThat(dto.general()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no toca el repositorio si la empresa no existe")
        void no_toca_el_repositorio_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + VaccinationTypeMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un tipo general con compania resuelta lanza y no guarda")
        void un_tipo_general_con_compania_resuelta_lanza_y_no_guarda() {
            when(companyQueryPort.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.CLINICA));

            assertThatThrownBy(
                    () -> service.execute(new CreateVaccinationTypeCommand("Vacuna universal",
                            "Disponible para todas", VaccinationTypeMother.COMPANY_ID, true)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general type cannot have company");

            verifyNoInteractions(repository);
        }
    }
}
