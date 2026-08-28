package com.vetsoftware.app.taxreturn.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListTaxReturnsService")
class ListTaxReturnsServiceTest {

    @Mock
    private TaxReturnRepository repository;

    @InjectMocks
    private ListTaxReturnsService service;

    @Nested
    @DisplayName("listAll")
    class Todas {

        @Test
        @DisplayName("delega en el repositorio y traduce cada declaracion a dto")
        void delega_en_el_repositorio_y_traduce_cada_declaracion() {
            TaxReturn uno = TaxReturnMother.conId(1L, TaxReturnMother.borradorDeRetencion());
            TaxReturn dos = TaxReturnMother.conId(2L, TaxReturnMother.borradorDeRenta());
            PageResult<TaxReturn> pagina = PageResult.of(List.of(uno, dos), 0, 20, 2L);
            when(repository.findAll(0, 20)).thenReturn(pagina);

            PageResult<TaxReturnDto> resultado = service.listAll(0, 20);

            assertThat(resultado.content()).extracting(TaxReturnDto::id)
                    .containsExactly(uno.getId(), dos.getId());
            assertThat(resultado.totalElements()).isEqualTo(2L);
            assertThat(resultado.page()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("listByFiscalPeriod")
    class PorPeriodo {

        @Test
        @DisplayName("delega en el repositorio con la clave de periodo del llamador")
        void delega_en_el_repositorio_con_la_clave_de_periodo() {
            String clave = "2026-B02";
            TaxReturn uno = TaxReturnMother.conId(3L, TaxReturnMother.borradorDeIca());
            PageResult<TaxReturn> pagina = PageResult.of(List.of(uno), 1, 10, 1L);
            when(repository.findAllByFiscalPeriodKey(clave, 1, 10)).thenReturn(pagina);

            PageResult<TaxReturnDto> resultado = service.listByFiscalPeriod(clave, 1, 10);

            verify(repository).findAllByFiscalPeriodKey(clave, 1, 10);
            assertThat(resultado.content()).extracting(TaxReturnDto::id)
                    .containsExactly(uno.getId());
        }
    }

    @Nested
    @DisplayName("listBecomingFinalBefore")
    class BarridoDeFirmeza {

        @Test
        @DisplayName("delega en el repositorio con la fecha limite del llamador")
        void delega_en_el_repositorio_con_la_fecha_limite() {
            LocalDate limite = LocalDate.of(2029, 1, 1);
            TaxReturn uno = TaxReturnMother.retencionPresentada(9L);
            PageResult<TaxReturn> pagina = PageResult.of(List.of(uno), 0, 20, 1L);
            when(repository.findAllByFirmezaUntilBefore(limite, 0, 20)).thenReturn(pagina);

            PageResult<TaxReturnDto> resultado = service.listBecomingFinalBefore(limite, 0, 20);

            verify(repository).findAllByFirmezaUntilBefore(limite, 0, 20);
            assertThat(resultado.content()).extracting(TaxReturnDto::id)
                    .containsExactly(uno.getId());
            assertThat(resultado.content().get(0).firmezaUntil()).isEqualTo(uno.getFirmezaUntil());
        }
    }
}
