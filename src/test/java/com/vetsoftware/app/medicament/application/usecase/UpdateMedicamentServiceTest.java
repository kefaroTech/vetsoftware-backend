package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
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
@DisplayName("UpdateMedicamentService")
class UpdateMedicamentServiceTest {

    private static final CompanyRef COMPANY = new CompanyRef(9L, "Clinica Norte", "900123456");
    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private UpdateMedicamentService service;

    @Test
    @DisplayName("actualiza name/description conservando el scope general/company existente")
    void actualiza_conservando_el_scope_existente() {
        Medicament existente = Medicament.create("Suero", "Original", COMPANY, false);
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicamentDto dto = service.execute(
                new UpdateMedicamentCommand(1L, "Suero fisiologico", "Actualizado", COMPANY_ID));

        assertThat(dto.name()).isEqualTo("Suero fisiologico");
        assertThat(dto.description()).isEqualTo("Actualizado");
        ArgumentCaptor<Medicament> guardado = ArgumentCaptor.forClass(Medicament.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getCompany()).isEqualTo(COMPANY);
        assertThat(guardado.getValue().isGeneral()).isFalse();
    }

    @Test
    @DisplayName("sin empresa (camino SYSTEM) carga sin acotar")
    void sin_empresa_carga_sin_acotar() {
        Medicament existente = Medicament.create("Suero", "Original", COMPANY, false);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new UpdateMedicamentCommand(1L, "Suero fisiologico", "Actualizado", null));

        verify(repository, never()).findByIdAndCompanyId(any(), any());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("lanza MedicamentNotFoundException si el medicamento no existe")
    void lanza_not_found_si_no_existe() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .execute(new UpdateMedicamentCommand(1L, "Suero", "Actualizado", COMPANY_ID)))
                .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

        verify(repository, never()).save(any());
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El {@code isMyCompany} del puerto solo prueba que el llamante declara SU
         * empresa. Cargando por id a secas el efecto no era un rechazo sino una edicion
         * del vademecum ajeno, asi que el corte tiene que estar en la carga.
         */
        @Test
        @DisplayName("el medicamento de otra empresa no se carga ni se guarda")
        void el_medicamento_de_otra_empresa_no_se_guarda() {
            when(repository.findByIdAndCompanyId(1L, OTRA_EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new UpdateMedicamentCommand(1L, "Robado", "Robado", OTRA_EMPRESA)))
                    .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }
    }
}
