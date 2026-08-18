package com.vetsoftware.app.salesreport.application.usecase;

import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.BRANCH_ID;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.DESDE;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.HASTA;
import static com.vetsoftware.app.salesreport.testsupport.SalesDocumentMother.documentoConEstado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.salesreport.application.dto.ReconciliationDto;
import com.vetsoftware.app.salesreport.application.dto.ReconciliationDto.PendingDto;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.SalesDocumentView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La conciliacion cuenta los documentos del periodo por su estado DIAN y arma
 * el detalle de los que requieren atencion. El switch sobre {@code dianStatus}
 * y el filtro de "no validado" son toda la logica: si un estado nuevo cae en el
 * default sin que nadie lo note, un documento rechazado se contaria como
 * pendiente sin que se note en produccion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetReconciliationService — conciliacion DIAN")
class GetReconciliationServiceTest {

    @Mock
    private SalesDocumentQueryPort queryPort;

    @InjectMocks
    private GetReconciliationService service;

    @Nested
    @DisplayName("Agrupamientos vacios")
    class AgrupamientosVacios {

        @Test
        @DisplayName("un rango sin documentos deja los contadores en cero y needsAttention vacio")
        void un_rango_sin_documentos_deja_los_contadores_en_cero() {
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of());

            ReconciliationDto conciliacion = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(conciliacion.total()).isZero();
            assertThat(conciliacion.validados()).isZero();
            assertThat(conciliacion.rechazados()).isZero();
            assertThat(conciliacion.contingencia()).isZero();
            assertThat(conciliacion.pendientes()).isZero();
            assertThat(conciliacion.needsAttention()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Rango invertido")
    class RangoInvertido {

        /**
         * Igual que en el libro de ventas: una conciliacion con todos los contadores en
         * cero por un rango invertido es indistinguible de un periodo sin documentos, y
         * en reporting fiscal eso es peor que un error.
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

            ReconciliationDto conciliacion = service.get(COMPANY_ID, DESDE, DESDE, BRANCH_ID);

            assertThat(conciliacion.dateFrom()).isEqualTo(DESDE);
            assertThat(conciliacion.dateTo()).isEqualTo(DESDE);
        }

        @Test
        @DisplayName("un rango sin fecha inicial se rechaza antes de consultar")
        void un_rango_sin_fecha_inicial_se_rechaza() {
            assertThatThrownBy(() -> service.get(COMPANY_ID, null, HASTA, BRANCH_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'from' is required");

            verifyNoInteractions(queryPort);
        }
    }

    @Nested
    @DisplayName("Conteo por estado")
    class ConteoPorEstado {

        @Test
        @DisplayName("cada estado conocido incrementa su propio contador")
        void cada_estado_conocido_incrementa_su_propio_contador() {
            SalesDocumentView validado = documentoConEstado(1L, DESDE, "VALIDADO");
            SalesDocumentView rechazado = documentoConEstado(2L, DESDE, "RECHAZADO");
            SalesDocumentView contingencia = documentoConEstado(3L, DESDE, "CONTINGENCIA");
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(validado, rechazado, contingencia));

            ReconciliationDto conciliacion = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(conciliacion.total()).isEqualTo(3);
            assertThat(conciliacion.validados()).isEqualTo(1);
            assertThat(conciliacion.rechazados()).isEqualTo(1);
            assertThat(conciliacion.contingencia()).isEqualTo(1);
            assertThat(conciliacion.pendientes()).isZero();
        }

        @Test
        @DisplayName("un estado que no es de los tres nombrados cae en pendientes por defecto")
        void un_estado_no_nombrado_cae_en_pendientes_por_defecto() {
            SalesDocumentView pendiente = documentoConEstado(1L, DESDE, "PENDIENTE");
            SalesDocumentView noElectronico = documentoConEstado(2L, DESDE, "NO_ELECTRONICO");
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(pendiente, noElectronico));

            ReconciliationDto conciliacion = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(conciliacion.pendientes()).isEqualTo(2);
            assertThat(conciliacion.validados()).isZero();
            assertThat(conciliacion.rechazados()).isZero();
            assertThat(conciliacion.contingencia()).isZero();
        }
    }

    @Nested
    @DisplayName("Detalle que requiere atencion")
    class NecesitaAtencion {

        @Test
        @DisplayName("un documento validado no aparece en needsAttention")
        void un_documento_validado_no_aparece_en_needs_attention() {
            SalesDocumentView validado = documentoConEstado(1L, DESDE, "VALIDADO");
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(validado));

            ReconciliationDto conciliacion = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(conciliacion.needsAttention()).isEmpty();
        }

        @Test
        @DisplayName("rechazados, en contingencia y pendientes aparecen en needsAttention con su detalle")
        void los_no_validados_aparecen_en_needs_attention_con_su_detalle() {
            SalesDocumentView rechazado = documentoConEstado(2L, DESDE, "RECHAZADO");
            SalesDocumentView contingencia = documentoConEstado(3L, HASTA, "CONTINGENCIA");
            when(queryPort.findByCompanyAndDateRange(COMPANY_ID, DESDE, HASTA, BRANCH_ID))
                    .thenReturn(List.of(rechazado, contingencia));

            ReconciliationDto conciliacion = service.get(COMPANY_ID, DESDE, HASTA, BRANCH_ID);

            assertThat(conciliacion.needsAttention()).extracting(PendingDto::id).containsExactly(2L,
                    3L);
            assertThat(conciliacion.needsAttention()).extracting(PendingDto::dianStatus)
                    .containsExactly("RECHAZADO", "CONTINGENCIA");
        }
    }
}
