package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

/**
 * Sin lineas en la worklist de la campana (el vecino de creacion, busqueda por
 * id), pero es comportamiento real sin ninguna red: se cubre igual.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindHospitalizationProgressNoteService")
class FindHospitalizationProgressNoteServiceTest {

    @Mock
    private HospitalizationProgressNoteRepository repository;
    @InjectMocks
    private FindHospitalizationProgressNoteService service;

    @Nested
    @DisplayName("Busqueda")
    class Busqueda {

        @Test
        @DisplayName("acota la busqueda a la company del contexto")
        void acota_la_busqueda_a_la_company() {
            Long id = HospitalizationProgressNoteMother.NOTE_ID;
            Long companyId = HospitalizationProgressNoteMother.COMPANY_ID;
            when(repository.findByIdAndCompanyId(id, companyId))
                    .thenReturn(Optional.of(HospitalizationProgressNoteMother.notaEvolucion()));

            HospitalizationProgressNoteDto dto = service.findById(id, companyId);

            assertThat(dto.id()).isEqualTo(id);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza NotFoundException si la nota no existe o es de otra empresa")
        void lanza_not_found_si_no_existe_o_es_de_otra_empresa() {
            Long id = 99L;
            Long companyId = HospitalizationProgressNoteMother.COMPANY_ID;
            when(repository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(id, companyId))
                    .isInstanceOf(HospitalizationProgressNoteNotFoundException.class)
                    .hasMessageContaining("HospitalizationProgressNote not found: " + id);
        }
    }
}
