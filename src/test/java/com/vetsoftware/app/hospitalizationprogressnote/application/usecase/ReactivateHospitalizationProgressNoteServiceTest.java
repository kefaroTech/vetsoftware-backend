package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import com.vetsoftware.app.hospitalizationprogressnote.testsupport.HospitalizationProgressNoteMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateHospitalizationProgressNoteService")
class ReactivateHospitalizationProgressNoteServiceTest {

    @Mock
    private HospitalizationProgressNoteRepository repository;
    @InjectMocks
    private ReactivateHospitalizationProgressNoteService service;

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva la nota y devuelve el dto releido")
        void reactiva_y_devuelve_el_dto() {
            Long id = HospitalizationProgressNoteMother.NOTE_ID;
            Long empresa = HospitalizationProgressNoteMother.COMPANY_ID;
            when(repository.reactivate(id, empresa)).thenReturn(1);
            when(repository.findByIdAndCompanyId(id, empresa))
                    .thenReturn(Optional.of(HospitalizationProgressNoteMother.notaEvolucion()));

            HospitalizationProgressNoteDto dto = service.execute(id, empresa);

            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza NotFoundException sin volver a buscar si no actualizo ninguna fila")
        void lanza_not_found_sin_volver_a_buscar() {
            Long id = 99L;
            Long empresa = HospitalizationProgressNoteMother.COMPANY_ID;
            when(repository.reactivate(id, empresa)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(id, empresa))
                    .isInstanceOf(HospitalizationProgressNoteNotFoundException.class)
                    .hasMessageContaining("HospitalizationProgressNote not found: " + id);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    /**
     * Antes de BE-COV el UPDATE nativo era {@code WHERE id = :id} a secas: en
     * reactivacion no hay lectura previa que valide la propiedad, asi que el SQL
     * era la unica barrera y no existia. Ahora el {@code EXISTS} contra la
     * hospitalizacion padre acota por empresa y cero filas afectadas sale como 404.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una nota de otra empresa no se reactiva: 404 y no relee nada")
        void una_nota_de_otra_empresa_no_se_reactiva() {
            Long id = HospitalizationProgressNoteMother.NOTE_ID;
            Long ajena = HospitalizationProgressNoteMother.OTRA_COMPANY_ID;
            when(repository.reactivate(id, ajena)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(id, ajena))
                    .isInstanceOf(HospitalizationProgressNoteNotFoundException.class)
                    .hasMessageContaining("HospitalizationProgressNote not found: " + id);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }
}
