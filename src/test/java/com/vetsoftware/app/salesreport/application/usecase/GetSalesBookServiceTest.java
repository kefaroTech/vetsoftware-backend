package com.vetsoftware.app.salesreport.application.usecase;

import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.BRANCH_ID;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.DESDE;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.HASTA;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.documento;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.incLinea;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.ivaLinea;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.pagoEfectivo;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.pagoTarjeta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.salesreport.application.dto.SalesBookDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto.RecaudoDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto.TaxByRateDto;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.SalesDocumentView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El libro de ventas es pura agregacion sobre lo que entrega el puerto: no hay
 * reglas de negocio propias, asi que lo que hay que fijar es que las sumas por
 * tarifa de IVA/INC y por medio de pago acumulen entre documentos en vez de
 * pisarse, y que un periodo sin documentos entregue colecciones vacias en vez
 * de null.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetSalesBookService — libro de ventas")
class GetSalesBookServiceTest {

    @Mock
    private SalesDocumentQueryPort queryPort;

    @InjectMocks
    private GetSalesBookService service;

    @Nested
    @DisplayName("Agrupamientos vacios")
    class AgrupamientosVacios {

        @Test
        @DisplayName("un rango sin documentos entrega entradas, tarifas y recaudo vacios, nunca null")
        void un_rango_sin_documentos_entrega_colecciones_vacias() {
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of());

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.entries()).isEmpty();
            assertThat(libro.taxByRate()).isEmpty();
            assertThat(libro.recaudoByMeans()).isEmpty();
            assertThat(libro.totals().documentCount()).isZero();
            assertThat(libro.totals().base()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(libro.totals().payable()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("una empresa sin ninguna venda en la sede pedida no aporta filas de recaudo")
        void una_empresa_sin_ventas_en_la_sede_no_aporta_recaudo() {
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of());

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.dateFrom()).isEqualTo(DESDE);
            assertThat(libro.dateTo()).isEqualTo(HASTA);
        }
    }

    @Nested
    @DisplayName("Rango invertido")
    class RangoInvertido {

        /**
         * Sin esta validacion el rango invertido no daba error: producia un libro de
         * ventas formalmente valido con todo en cero, indistinguible de un periodo real
         * sin ventas. Que no llegue a consultar el puerto es la mitad de la asercion.
         */
        @Test
        @DisplayName("un rango con 'to' antes que 'from' se rechaza y no consulta el puerto")
        void un_rango_invertido_se_rechaza() {
            assertThatThrownBy(() -> service.get(COMPANY_ID, HASTA, DESDE, BRANCH_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'from' must not be after 'to'");

            verifyNoInteractions(queryPort);
        }

        @Test
        @DisplayName("un rango de un solo dia (from == to) es valido")
        void un_rango_de_un_solo_dia_es_valido() {
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, DESDE, BRANCH_ID))
                    .thenReturn(List.of());

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, DESDE, BRANCH_ID);

            assertThat(libro.dateFrom()).isEqualTo(DESDE);
            assertThat(libro.dateTo()).isEqualTo(DESDE);
        }

        @Test
        @DisplayName("un rango sin fecha final se rechaza antes de consultar")
        void un_rango_sin_fecha_final_se_rechaza() {
            assertThatThrownBy(() -> service.get(COMPANY_ID, DESDE, null, BRANCH_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'to' is required");

            verifyNoInteractions(queryPort);
        }
    }

    @Nested
    @DisplayName("Acumulacion por tarifa y por medio de pago")
    class Acumulacion {

        @Test
        @DisplayName("dos documentos con la misma tarifa de IVA suman en una sola fila")
        void dos_documentos_con_la_misma_tarifa_de_iva_suman_en_una_fila() {
            SalesDocumentView doc1 = documento(1L, DESDE, "VALIDADO", new BigDecimal("100000.00"),
                    new BigDecimal("119000.00"),
                    List.of(ivaLinea(new BigDecimal("100000.00"), new BigDecimal("19000.00"))),
                    List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            SalesDocumentView doc2 = documento(2L, DESDE, "VALIDADO", new BigDecimal("50000.00"),
                    new BigDecimal("59500.00"),
                    List.of(ivaLinea(new BigDecimal("50000.00"), new BigDecimal("9500.00"))),
                    List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(doc1, doc2));

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.taxByRate()).hasSize(1);
            TaxByRateDto fila = libro.taxByRate().get(0);
            assertThat(fila.taxableAmount()).isEqualByComparingTo("150000.00");
            assertThat(fila.taxAmount()).isEqualByComparingTo("28500.00");
        }

        @Test
        @DisplayName("tarifas distintas de IVA se separan en filas distintas")
        void tarifas_distintas_se_separan_en_filas_distintas() {
            SalesDocumentView doc = documento(1L, DESDE, "VALIDADO", new BigDecimal("100000.00"),
                    new BigDecimal("127000.00"),
                    List.of(ivaLinea(new BigDecimal("100000.00"), new BigDecimal("19000.00")),
                            incLinea(new BigDecimal("100000.00"), new BigDecimal("8000.00"))),
                    List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(doc));

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.taxByRate()).hasSize(2);
            assertThat(libro.entries().get(0).iva()).isEqualByComparingTo("19000.00");
            assertThat(libro.entries().get(0).inc()).isEqualByComparingTo("8000.00");
        }

        @Test
        @DisplayName("el mismo medio de pago en dos documentos acumula el recaudo")
        void el_mismo_medio_de_pago_en_dos_documentos_acumula_el_recaudo() {
            SalesDocumentView doc1 = documento(1L, DESDE, "VALIDADO", new BigDecimal("100000.00"),
                    new BigDecimal("100000.00"), List.of(),
                    List.of(pagoEfectivo(new BigDecimal("100000.00"))), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
            SalesDocumentView doc2 = documento(2L, DESDE, "VALIDADO", new BigDecimal("50000.00"),
                    new BigDecimal("50000.00"), List.of(),
                    List.of(pagoEfectivo(new BigDecimal("50000.00"))), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(doc1, doc2));

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.recaudoByMeans()).hasSize(1);
            RecaudoDto fila = libro.recaudoByMeans().get(0);
            assertThat(fila.dianCode()).isEqualTo("10");
            assertThat(fila.amount()).isEqualByComparingTo("150000.00");
        }

        @Test
        @DisplayName("medios de pago distintos se separan en filas distintas de recaudo")
        void medios_de_pago_distintos_se_separan_en_filas_distintas() {
            SalesDocumentView doc = documento(1L, DESDE, "VALIDADO", new BigDecimal("150000.00"),
                    new BigDecimal("150000.00"), List.of(),
                    List.of(pagoEfectivo(new BigDecimal("100000.00")),
                            pagoTarjeta(new BigDecimal("50000.00"))),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(doc));

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.recaudoByMeans()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Valores nulos y totales")
    class ValoresNulos {

        @Test
        @DisplayName("las retenciones nulas se tratan como cero en la entrada y en los totales")
        void las_retenciones_nulas_se_tratan_como_cero() {
            SalesDocumentView doc = documento(1L, DESDE, "VALIDADO", new BigDecimal("100000.00"),
                    new BigDecimal("119000.00"), List.of(), List.of(), null, null, null);
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(doc));

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.entries().get(0).reteFuente()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(libro.entries().get(0).reteIva()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(libro.entries().get(0).reteIca()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(libro.totals().reteFuente()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("los totales suman todos los documentos del periodo y cuentan cada uno")
        void los_totales_suman_todos_los_documentos_del_periodo() {
            SalesDocumentView doc1 = documento(1L, DESDE, "VALIDADO", new BigDecimal("100000.00"),
                    new BigDecimal("119000.00"), List.of(), List.of(), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
            SalesDocumentView doc2 = documento(2L, HASTA, "RECHAZADO", new BigDecimal("50000.00"),
                    new BigDecimal("59500.00"), List.of(), List.of(), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(doc1, doc2));

            SalesBookDto libro = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(libro.totals().documentCount()).isEqualTo(2);
            assertThat(libro.totals().base()).isEqualByComparingTo("150000.00");
            assertThat(libro.totals().payable()).isEqualByComparingTo("178500.00");
        }
    }
}
