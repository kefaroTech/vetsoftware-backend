package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultationtype.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeHasActiveChildrenException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteConsultationTypeService")
class DeleteConsultationTypeServiceTest {

    @Mock
    private ConsultationTypeRepository repository;
    @Mock
    private ConsultationChildrenQueryPort consultationChildrenQueryPort;

    @InjectMocks
    private DeleteConsultationTypeService service;

    private void tipoExiste() {
        when(repository.findById(ConsultationTypeMother.ID))
                .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));
    }

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin consultas activas hijas, borra el tipo")
        void sin_consultas_activas_borra_el_tipo() {
            tipoExiste();
            when(consultationChildrenQueryPort
                    .existsActiveByConsultationTypeId(ConsultationTypeMother.ID)).thenReturn(false);

            service.execute(ConsultationTypeMother.ID);

            verify(repository).delete(ConsultationTypeMother.ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("un id inexistente lanza y no consulta hijos ni borra")
        void un_id_inexistente_lanza_y_no_consulta_hijos() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L))
                    .isInstanceOf(ConsultationTypeNotFoundException.class)
                    .hasMessageContaining("ConsultationType not found: 99");

            verifyNoInteractions(consultationChildrenQueryPort);
            verify(repository, never()).delete(anyLong());
        }

        @Test
        @DisplayName("con consultas activas hijas, lanza y no borra")
        void con_consultas_activas_lanza_y_no_borra() {
            tipoExiste();
            when(consultationChildrenQueryPort
                    .existsActiveByConsultationTypeId(ConsultationTypeMother.ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(ConsultationTypeMother.ID))
                    .isInstanceOf(ConsultationTypeHasActiveChildrenException.class)
                    .hasMessageContaining("consultationtype " + ConsultationTypeMother.ID)
                    .hasMessageContaining("has active consultation children");

            verify(repository, never()).delete(anyLong());
        }
    }
}
