package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteHospitalizationProgressNoteService")
class DeleteHospitalizationProgressNoteServiceTest {

    @Mock
    private HospitalizationProgressNoteRepository repository;
    @InjectMocks
    private DeleteHospitalizationProgressNoteService service;

    @Nested
    @DisplayName("Borrado")
    class Borrado {

        @Test
        @DisplayName("borra la nota si existe")
        void borra_la_nota_si_existe() {
            Long id = HospitalizationProgressNoteMother.NOTE_ID;
            Long empresa = HospitalizationProgressNoteMother.COMPANY_ID;
            when(repository.findByIdAndCompanyId(id, empresa))
                    .thenReturn(Optional.of(HospitalizationProgressNoteMother.notaEvolucion()));

            service.execute(id, empresa);

            verify(repository).delete(id);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza NotFoundException y no borra si la nota no existe")
        void lanza_not_found_y_no_borra() {
            Long id = 99L;
            Long empresa = HospitalizationProgressNoteMother.COMPANY_ID;
            when(repository.findByIdAndCompanyId(id, empresa)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(id, empresa))
                    .isInstanceOf(HospitalizationProgressNoteNotFoundException.class)
                    .hasMessageContaining("HospitalizationProgressNote not found: " + id);

            verify(repository, never()).delete(any());
        }
    }

    /**
     * Antes de BE-COV el puerto no recibia companyId y el servicio comprobaba la
     * existencia con {@code findById(id)}: cualquier empleado con
     * {@code hospitalization.delete} borraba la nota de otra empresa adivinando el
     * id.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una nota de otra empresa no se borra: 404 y el repositorio no escribe")
        void una_nota_de_otra_empresa_no_se_borra() {
            Long id = HospitalizationProgressNoteMother.NOTE_ID;
            Long ajena = HospitalizationProgressNoteMother.OTRA_COMPANY_ID;
            when(repository.findByIdAndCompanyId(id, ajena)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(id, ajena))
                    .isInstanceOf(HospitalizationProgressNoteNotFoundException.class)
                    .hasMessageContaining("HospitalizationProgressNote not found: " + id);

            verify(repository, never()).delete(any());
        }
    }
}
