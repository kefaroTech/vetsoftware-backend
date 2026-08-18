package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import com.vetsoftware.app.hospitalizationprocedure.testsupport.HospitalizationProcedureMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateHospitalizationProcedureService")
class ReactivateHospitalizationProcedureServiceTest {

    @Mock
    private HospitalizationProcedureRepository repository;

    @InjectMocks
    private ReactivateHospitalizationProcedureService service;

    @Test
    @DisplayName("reactiva con el companyId del contexto y devuelve la orden ya habilitada")
    void reactiva_y_devuelve_la_orden_ya_habilitada() {
        when(repository.reactivate(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationProcedureMother.activo()));

        HospitalizationProcedureDto dto = service.execute(
                HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(HospitalizationProcedureMother.PROCEDURE_ID);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        when(repository.reactivate(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID))
                .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                .hasMessageContaining("HospitalizationProcedure not found: "
                        + HospitalizationProcedureMother.PROCEDURE_ID);

        verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("si la orden desaparece entre el UPDATE y el SELECT, falla como no-encontrada")
    void si_la_orden_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID))
                .isInstanceOf(HospitalizationProcedureNotFoundException.class);
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * Este es el peor de los cuatro casos de uso de la feature: no hay ninguna
         * lectura previa que valide la propiedad, asi que el {@code AND company_id} de
         * la query nativa es LA barrera, no defensa en profundidad. Cero filas
         * afectadas responde igual a «no existe» que a «es de otro tenant»: un 404 que
         * no revela que el id existe.
         */
        @Test
        @DisplayName("una orden de otra empresa no se reactiva: 404 y no relee nada")
        void una_orden_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.OTRA_COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.OTRA_COMPANY_ID))
                    .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                    .hasMessageContaining("HospitalizationProcedure not found: "
                            + HospitalizationProcedureMother.PROCEDURE_ID);

            verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
        }
    }
}
