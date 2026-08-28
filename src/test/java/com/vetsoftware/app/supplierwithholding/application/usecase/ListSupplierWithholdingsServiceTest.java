package com.vetsoftware.app.supplierwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.testsupport.SupplierWithholdingMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSupplierWithholdingsService")
class ListSupplierWithholdingsServiceTest {

    @Mock
    private SupplierWithholdingRepository repository;

    @InjectMocks
    private ListSupplierWithholdingsService service;

    @Nested
    @DisplayName("listAll")
    class Todas {

        @Test
        @DisplayName("delega en el repositorio y traduce cada retencion a dto")
        void delega_en_el_repositorio_y_traduce_cada_retencion() {
            SupplierWithholding uno = SupplierWithholdingMother.conId(1L,
                    SupplierWithholdingMother.renta());
            SupplierWithholding dos = SupplierWithholdingMother.conId(2L,
                    SupplierWithholdingMother.ica());
            PageResult<SupplierWithholding> pagina = PageResult.of(List.of(uno, dos), 0, 20, 2L);
            when(repository.findAll(0, 20)).thenReturn(pagina);

            PageResult<SupplierWithholdingDto> resultado = service.listAll(0, 20);

            assertThat(resultado.content()).extracting(SupplierWithholdingDto::id)
                    .containsExactly(uno.getId(), dos.getId());
            assertThat(resultado.totalElements()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("listByFiscalPeriod")
    class PorPeriodo {

        @Test
        @DisplayName("delega en el repositorio con la clave de periodo del llamador")
        void delega_en_el_repositorio_con_la_clave_de_periodo() {
            String clave = "2026-B02";
            SupplierWithholding uno = SupplierWithholdingMother.conId(3L,
                    SupplierWithholdingMother.ica());
            PageResult<SupplierWithholding> pagina = PageResult.of(List.of(uno), 1, 10, 1L);
            when(repository.findAllByFiscalPeriodKey(clave, 1, 10)).thenReturn(pagina);

            PageResult<SupplierWithholdingDto> resultado = service.listByFiscalPeriod(clave, 1, 10);

            verify(repository).findAllByFiscalPeriodKey(clave, 1, 10);
            assertThat(resultado.content()).extracting(SupplierWithholdingDto::id)
                    .containsExactly(uno.getId());
        }
    }

    @Nested
    @DisplayName("listBySupplierAndYear")
    class CertificadoAnual {

        @Test
        @DisplayName("delega en el repositorio con el proveedor y el año del llamador")
        void delega_en_el_repositorio_con_el_proveedor_y_el_anio() {
            SupplierWithholding uno = SupplierWithholdingMother.conId(9L,
                    SupplierWithholdingMother.renta());
            PageResult<SupplierWithholding> pagina = PageResult.of(List.of(uno), 0, 20, 1L);
            when(repository.findAllBySupplierTaxIdAndFiscalYear("900123456",
                    SupplierWithholdingMother.ANIO, 0, 20)).thenReturn(pagina);

            PageResult<SupplierWithholdingDto> resultado = service
                    .listBySupplierAndYear("900123456", SupplierWithholdingMother.ANIO, 0, 20);

            verify(repository).findAllBySupplierTaxIdAndFiscalYear("900123456",
                    SupplierWithholdingMother.ANIO, 0, 20);
            assertThat(resultado.content()).extracting(SupplierWithholdingDto::id)
                    .containsExactly(uno.getId());
        }
    }
}
