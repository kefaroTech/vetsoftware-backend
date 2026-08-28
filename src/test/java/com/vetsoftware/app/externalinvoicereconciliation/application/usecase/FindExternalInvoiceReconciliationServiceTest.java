package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationNotFoundException;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La carga es ancha —{@code findById(id)} sin empresa— y aqui esa es la forma
 * correcta: el puerto esta cerrado a {@code hasRole('SYSTEM')} a secas y un
 * principal SYSTEM no tiene empresa propia contra la que acotar. Es la exencion
 * que {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) declara expresamente
 * para el servicio que solo alcanza SYSTEM.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindExternalInvoiceReconciliationService — lectura por id de plataforma")
class FindExternalInvoiceReconciliationServiceTest {

    private static final Long ID = 41L;

    @Mock
    private ExternalInvoiceReconciliationRepository repository;

    private FindExternalInvoiceReconciliationService service;

    @BeforeEach
    void servicio() {
        service = new FindExternalInvoiceReconciliationService(repository);
    }

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("devuelve los cuatro numeros enfrentados, no solo la diferencia")
        void devuelve_los_cuatro_numeros_enfrentados() {
            // Quien mira un descuadre necesita saber si la base cuadra y el impuesto no
            // -calculo- o si no cuadra ninguno -base-. Publicar solo difference
            // obligaria a abrir otra pantalla para responder la unica pregunta que
            // importa.
            when(repository.findById(ID)).thenReturn(Optional.of(ExternalInvoiceReconciliationMother
                    .conFacturaExterna(ID, new BigDecimal("118998.00"))));

            ExternalInvoiceReconciliationDto encontrada = service.findById(ID);

            assertThat(encontrada.id()).isEqualTo(ID);
            assertThat(encontrada.computedTotal()).isEqualByComparingTo("119000.00");
            assertThat(encontrada.computedTax()).isEqualByComparingTo("19000.00");
            assertThat(encontrada.externalTotal()).isEqualByComparingTo("118998.00");
            assertThat(encontrada.externalTax()).isEqualByComparingTo("19000.00");
            assertThat(encontrada.difference()).isEqualByComparingTo("2.00");
            assertThat(encontrada.status())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE);
        }

        @Test
        @DisplayName("un id que no existe sale como no encontrada")
        void un_id_que_no_existe_sale_como_no_encontrada() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(404L))
                    .isInstanceOf(ExternalInvoiceReconciliationNotFoundException.class)
                    .hasMessage("External invoice reconciliation not found: 404");
        }
    }
}
