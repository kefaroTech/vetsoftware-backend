package com.vetsoftware.app.revenuerecognitionline.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.RevenueRecognitionLineRepository;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLineNotFoundException;
import com.vetsoftware.app.revenuerecognitionline.testsupport.RevenueRecognitionLineMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindRevenueRecognitionLineService")
class FindRevenueRecognitionLineServiceTest {

    @Mock
    private RevenueRecognitionLineRepository repository;

    @InjectMocks
    private FindRevenueRecognitionLineService service;

    @Test
    @DisplayName("devuelve el DTO del renglon por su id, llamando a findById y no a la variante "
            + "acotada: un principal SYSTEM no tiene empresa que pasar")
    void devuelve_el_dto_del_renglon_por_su_id() {
        when(repository.findById(RevenueRecognitionLineMother.LINE_ID))
                .thenReturn(Optional.of(RevenueRecognitionLineMother.renglon()));

        RevenueRecognitionLineDto dto = service.findById(RevenueRecognitionLineMother.LINE_ID);

        assertThat(dto.id()).isEqualTo(RevenueRecognitionLineMother.LINE_ID);
        assertThat(dto.companyId()).isEqualTo(RevenueRecognitionLineMother.COMPANY_ID);
        verify(repository).findById(RevenueRecognitionLineMother.LINE_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("un renglon inexistente lanza RevenueRecognitionLineNotFoundException")
    void un_renglon_inexistente_lanza_not_found() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(RevenueRecognitionLineNotFoundException.class)
                .hasMessageContaining("Revenue recognition line not found: 999");
    }
}
