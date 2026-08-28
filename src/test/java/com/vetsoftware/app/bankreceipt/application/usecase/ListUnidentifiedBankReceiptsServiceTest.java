package com.vetsoftware.app.bankreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La bandeja de lo no identificado.
 *
 * <p>
 * <b>El estado lo fija el servicio y no quien llama</b>, y eso es lo que este
 * test congela: con {@code STRICT_STUBS}, el stub solo casa si el servicio pide
 * {@link BankReceiptStatus#UNIDENTIFIED}. El dia que alguien parametrice el
 * estado «para reutilizar», la consola podria pedir {@code DISCARDED} por la
 * ruta que la interfaz anuncia como «pendientes» y el caso se pondria rojo sin
 * un solo {@code verify}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListUnidentifiedBankReceiptsService — la bandeja del mes")
class ListUnidentifiedBankReceiptsServiceTest {

    @Mock
    private BankReceiptRepository repository;
    @InjectMocks
    private ListUnidentifiedBankReceiptsService service;

    @Nested
    @DisplayName("Bandeja")
    class Bandeja {

        @Test
        @DisplayName("pide al puerto exactamente las UNIDENTIFIED y conserva los metadatos")
        void pide_exactamente_las_unidentified() {
            List<BankReceipt> pendientes = List.of(BankReceiptMother.persistida(8720L));
            when(repository.findAllByStatus(BankReceiptStatus.UNIDENTIFIED, 0, 20))
                    .thenReturn(new PageResult<>(pendientes, 0, 20, 48L, 3));

            PageResult<BankReceiptDto> bandeja = service.listUnidentified(0, 20);

            assertThat(bandeja.content()).singleElement()
                    .satisfies(entrada -> assertThat(entrada.status())
                            .isEqualTo(BankReceiptStatus.UNIDENTIFIED));
            assertThat(bandeja.totalElements()).isEqualTo(48L);
            assertThat(bandeja.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("una bandeja vacia es el mes cuadrado, no un error")
        void una_bandeja_vacia_es_el_mes_cuadrado() {
            when(repository.findAllByStatus(BankReceiptStatus.UNIDENTIFIED, 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.listUnidentified(0, 20).content()).isEmpty();
        }
    }
}
