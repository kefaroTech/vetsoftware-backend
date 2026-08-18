package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteHospitalizationProcedureService")
class DeleteHospitalizationProcedureServiceTest {

    @Mock
    private HospitalizationProcedureRepository repository;

    @InjectMocks
    private DeleteHospitalizationProcedureService service;

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra la orden encontrada dentro de la empresa del contexto")
        void borra_la_orden_encontrada() {
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationProcedureMother.activo()));

            service.execute(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID);

            verify(repository).delete(HospitalizationProcedureMother.PROCEDURE_ID);
        }
    }

    @Nested
    @DisplayName("orden inexistente")
    class OrdenInexistente {

        @Test
        @DisplayName("no borra nada")
        void no_borra_nada() {
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID))
                    .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                    .hasMessageContaining("HospitalizationProcedure not found: "
                            + HospitalizationProcedureMother.PROCEDURE_ID);

            verify(repository, never()).delete(anyLong());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * La existencia se comprueba acotada por empresa: para el tenant equivocado la
         * orden simplemente no existe, sale 404 y el delete no llega a ejecutarse.
         * Antes se buscaba con {@code findById(id)} a secas y cualquier empleado con
         * {@code hospitalization.delete} borraba la orden de otra empresa adivinando el
         * id.
         */
        @Test
        @DisplayName("una orden de otra empresa no se borra: 404 y el repositorio no escribe")
        void una_orden_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.OTRA_COMPANY_ID))
                    .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                    .hasMessageContaining("HospitalizationProcedure not found: "
                            + HospitalizationProcedureMother.PROCEDURE_ID);

            verify(repository, never()).delete(anyLong());
        }
    }
}
