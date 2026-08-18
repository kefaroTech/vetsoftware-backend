package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
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
@DisplayName("CreateMedicamentService")
class CreateMedicamentServiceTest {

    private static final CompanyRef COMPANY = new CompanyRef(9L, "Clinica Norte", "900123456");

    @Mock
    private MedicamentRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateMedicamentService service;

    @Nested
    @DisplayName("medicamento propio de una empresa (general = false)")
    class MedicamentoDeEmpresa {

        @Test
        @DisplayName("resuelve la empresa y persiste el medicamento")
        void resuelve_la_empresa_y_persiste() {
            when(companyQueryPort.findById(9L)).thenReturn(Optional.of(COMPANY));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service
                    .execute(new CreateMedicamentCommand("Suero", "Formula propia", 9L, false));

            assertThat(dto.name()).isEqualTo("Suero");
            assertThat(dto.company().id()).isEqualTo(9L);
            ArgumentCaptor<Medicament> guardado = ArgumentCaptor.forClass(Medicament.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("no guarda si la empresa no existe")
        void no_guarda_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new CreateMedicamentCommand("Suero", null, 9L, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: 9");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("medicamento general (companyId nulo)")
    class MedicamentoGeneral {

        @Test
        @DisplayName("no consulta la empresa si companyId es nulo")
        void no_consulta_la_empresa_si_companyid_es_nulo() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service
                    .execute(new CreateMedicamentCommand("Amoxicilina", null, null, true));

            assertThat(dto.company()).isNull();
            verify(companyQueryPort, never()).findById(any());
        }

        @Test
        @DisplayName("companyId nulo con general=false viola la invariante del dominio")
        void companyid_nulo_con_general_false_viola_la_invariante() {
            assertThatThrownBy(
                    () -> service.execute(new CreateMedicamentCommand("Suero", null, null, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-general medicament requires company");

            verify(repository, never()).save(any());
        }
    }
}
