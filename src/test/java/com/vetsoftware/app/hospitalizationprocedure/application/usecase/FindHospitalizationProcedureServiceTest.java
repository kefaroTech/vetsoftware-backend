package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import com.vetsoftware.app.hospitalizationprocedure.testsupport.HospitalizationProcedureMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * No estaba en la lista de JaCoCo del dia pero es la unica clase de
 * application/usecase de esta feature que se quedaba sin ningun test: se cubre
 * igual, con el mismo nivel de exigencia que el resto de la feature.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindHospitalizationProcedureService")
class FindHospitalizationProcedureServiceTest {

    @Mock
    private HospitalizationProcedureRepository repository;

    @InjectMocks
    private FindHospitalizationProcedureService service;

    @Test
    @DisplayName("devuelve el DTO de la orden acotada por empresa")
    void devuelve_el_dto_de_la_orden_acotada_por_empresa() {
        when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationProcedureMother.activo()));

        HospitalizationProcedureDto dto = service.findById(
                HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(HospitalizationProcedureMother.PROCEDURE_ID);
        assertThat(dto.name()).isEqualTo("Curacion de herida");
    }

    @Test
    @DisplayName("una orden de otra empresa no se encuentra")
    void una_orden_de_otra_empresa_no_se_encuentra() {
        when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID))
                .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                .hasMessageContaining("HospitalizationProcedure not found: "
                        + HospitalizationProcedureMother.PROCEDURE_ID);
    }
}
