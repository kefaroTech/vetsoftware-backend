package com.vetsoftware.app.bankreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
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
 * El extracto completo, paginado.
 *
 * <p>
 * <b>Lo unico que puede romperse aqui es la aritmetica de la pagina</b>, y se
 * rompe en silencio: recalcular los totales sobre el contenido ya mapeado
 * reportaria «2 de 2» en un extracto de cincuenta mil lineas. Por eso el caso
 * feliz usa un total deliberadamente distinto del tamaño del contenido.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListBankReceiptsService — el extracto completo")
class ListBankReceiptsServiceTest {

    @Mock
    private BankReceiptRepository repository;
    @InjectMocks
    private ListBankReceiptsService service;

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("mapea el contenido y conserva intactos los metadatos de la consulta")
        void mapea_el_contenido_y_conserva_los_metadatos() {
            List<BankReceipt> contenido = List.of(BankReceiptMother.persistida(8710L),
                    BankReceiptMother.persistida(8711L));
            when(repository.findAll(2, 25)).thenReturn(new PageResult<>(contenido, 2, 25, 137L, 6));

            PageResult<BankReceiptDto> pagina = service.listAll(2, 25);

            assertThat(pagina.content()).extracting(BankReceiptDto::id).containsExactly(8710L,
                    8711L);
            // 137 y 6 son los de la consulta, no los del contenido: dos elementos.
            assertThat(pagina.totalElements()).isEqualTo(137L);
            assertThat(pagina.totalPages()).isEqualTo(6);
            assertThat(pagina.page()).isEqualTo(2);
            assertThat(pagina.pageSize()).isEqualTo(25);
        }

        @Test
        @DisplayName("una pagina vacia conserva la posicion pedida")
        void una_pagina_vacia_conserva_la_posicion() {
            when(repository.findAll(4, 10)).thenReturn(PageResult.empty(4, 10));

            PageResult<BankReceiptDto> pagina = service.listAll(4, 10);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.page()).isEqualTo(4);
            assertThat(pagina.totalElements()).isZero();
        }
    }
}
