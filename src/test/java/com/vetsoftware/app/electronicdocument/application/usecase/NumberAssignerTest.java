package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.BRANCH_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.NumberingAllocationPort;
import com.vetsoftware.app.electronicdocument.application.port.out.NumberingAllocationPort.AllocatedNumber;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La numeracion fiscal es un consecutivo continuo ante la DIAN: un numero
 * repetido o un hueco son un problema con la autoridad tributaria, no un bug de
 * pantalla. Lo que se prueba aqui es que el documento acabe con el numero que
 * entrego la resolucion y que el rechazo devuelva el consecutivo solo cuando es
 * seguro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NumberAssigner — la numeracion fiscal del documento")
class NumberAssignerTest {

    private static final AllocatedNumber NUMERO = new AllocatedNumber("18760000001", "SETP", 991L);

    @Mock
    private NumberingAllocationPort numberingPort;

    @InjectMocks
    private NumberAssigner assigner;

    @Nested
    @DisplayName("assign — consumir consecutivo")
    class Asignar {

        @Test
        @DisplayName("estampa resolucion, prefijo y consecutivo en el documento")
        void estampa_la_numeracion_en_el_documento() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.allocate(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.of(NUMERO));

            assigner.assign(documento);

            assertThat(documento.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(documento.getPrefix()).isEqualTo("SETP");
            assertThat(documento.getConsecutive()).isEqualTo(991L);
        }

        @Test
        @DisplayName("pide la resolucion de la sede del documento, no una cualquiera")
        void pide_la_resolucion_de_la_sede_del_documento() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.allocate(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.of(NUMERO));

            assigner.assign(documento);

            // Multi-sucursal: cada sede puede tener su propia resolucion, y mezclarlas
            // rompe la continuidad del consecutivo de las dos.
            verify(numberingPort).allocate(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA);
        }

        @Test
        @DisplayName("sin resolucion activa falla y el documento se queda sin numerar")
        void sin_resolucion_activa_falla() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.allocate(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> assigner.assign(documento))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no tiene una resolución de numeración activa")
                    .hasMessageContaining("FE_VENTA");

            assertThat(documento.getConsecutive()).isNull();
        }
    }

    @Nested
    @DisplayName("assignResolutionOnly — el numero lo pone el proveedor")
    class SoloResolucion {

        @Test
        @DisplayName("estampa resolucion y prefijo sin consumir consecutivo")
        void estampa_resolucion_y_prefijo_sin_consumir() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.peekActive(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.of(new AllocatedNumber("18760000001", "SETP", null)));

            assigner.assignResolutionOnly(documento);

            // El POS lo numera la DIAN, pero el request exige igual resolucion y prefijo.
            // Consumir aqui un consecutivo dejaria un hueco en la secuencia.
            assertThat(documento.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(documento.getPrefix()).isEqualTo("SETP");
            assertThat(documento.getConsecutive()).isNull();
            verify(numberingPort, never()).allocate(any(), any(), any());
        }

        @Test
        @DisplayName("sin resolucion activa falla igual")
        void sin_resolucion_activa_falla_igual() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.peekActive(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> assigner.assignResolutionOnly(documento))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no tiene una resolución de numeración activa");
        }
    }

    @Nested
    @DisplayName("release — devolver el consecutivo tras un rechazo")
    class Liberar {

        @Test
        @DisplayName("si la resolucion lo recupera, el documento se queda sin numero")
        void si_la_resolucion_lo_recupera_el_documento_se_queda_sin_numero() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.allocate(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.of(NUMERO));
            assigner.assign(documento);
            documento.markRejected();
            when(numberingPort.release(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA,
                    991L)).thenReturn(true);

            assigner.release(documento);

            // Si el numero volvio al mostrador, el documento no puede seguir llevandolo:
            // quedarian dos filas con el mismo consecutivo fiscal.
            assertThat(documento.getConsecutive()).isNull();
        }

        @Test
        @DisplayName("si ya no era seguro recuperarlo, el documento conserva su numero")
        void si_no_era_seguro_el_documento_conserva_su_numero() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.allocate(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.of(NUMERO));
            assigner.assign(documento);
            documento.markRejected();
            when(numberingPort.release(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA,
                    991L)).thenReturn(false);

            assigner.release(documento);

            // Otro documento ya tomo el siguiente: recuperarlo duplicaria el numero. Se
            // prefiere el hueco en la secuencia, que es explicable ante la DIAN.
            assertThat(documento.getConsecutive()).isEqualTo(991L);
        }

        @Test
        @DisplayName("un documento que nunca se numero no toca la resolucion")
        void un_documento_sin_numerar_no_toca_la_resolucion() {
            assigner.release(facturaPendiente());

            // Sin este corte, un rechazo antes de numerar devolveria un consecutivo que
            // nunca se pidio y desalinearia la resolucion.
            verifyNoInteractions(numberingPort);
        }

        @Test
        @DisplayName("libera el consecutivo exacto del documento, no el ultimo de la resolucion")
        void libera_el_consecutivo_exacto_del_documento() {
            ElectronicDocument documento = facturaPendiente();
            when(numberingPort.allocate(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA))
                    .thenReturn(Optional.of(NUMERO));
            assigner.assign(documento);
            documento.markRejected();
            when(numberingPort.release(anyLong(), anyLong(), any(), anyLong())).thenReturn(true);

            assigner.release(documento);

            verify(numberingPort).release(COMPANY_ID, BRANCH_ID, ElectronicDocumentType.FE_VENTA,
                    991L);
        }
    }
}
