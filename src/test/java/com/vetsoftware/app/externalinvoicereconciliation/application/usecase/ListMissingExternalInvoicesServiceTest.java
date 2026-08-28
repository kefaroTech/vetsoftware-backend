package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>El estado va fijado en el servicio y no llega por parametro.</b>
 *
 * <p>
 * Ese es todo el valor de este caso de uso, y por eso el test lo captura en vez
 * de darlo por hecho: si el llamante pudiera elegir el estado, esta bandeja
 * seria otro barrido general mas y la consulta que importa volveria a depender
 * de que alguien se acuerde de filtrar por {@code MISSING_EXTERNAL}. Y esa es
 * la consulta que no se puede olvidar: los otros tres estados saltan solos en
 * cualquier listado de descuadres porque nacen de comparar dos numeros; este no
 * produce ninguna diferencia que llame la atencion, asi que si no se pregunta
 * por el explicitamente no aparece en ningun sitio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListMissingExternalInvoicesService — la bandeja de lo que nadie facturo")
class ListMissingExternalInvoicesServiceTest {

    @Mock
    private ExternalInvoiceReconciliationRepository repository;

    private ListMissingExternalInvoicesService service;

    @BeforeEach
    void servicio() {
        service = new ListMissingExternalInvoicesService(repository);
    }

    @Nested
    @DisplayName("Bandeja")
    class Bandeja {

        @Test
        @DisplayName("pregunta siempre por MISSING_EXTERNAL, sin recibirlo de nadie")
        void pregunta_siempre_por_missing_external() {
            when(repository.findAllByStatus(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, 0,
                    20))
                    .thenReturn(PageResult.of(
                            List.of(ExternalInvoiceReconciliationMother.abiertaConId(41L)), 0, 20,
                            1L));

            PageResult<ExternalInvoiceReconciliationDto> bandeja = service.listMissing(0, 20);

            assertThat(bandeja.content()).singleElement().satisfies(fila -> {
                assertThat(fila.status())
                        .isEqualTo(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL);
                assertThat(fila.externalInvoiceId()).isNull();
                assertThat(fila.difference()).isNull();
            });

            ArgumentCaptor<ExternalInvoiceReconciliationStatus> estado = ArgumentCaptor
                    .forClass(ExternalInvoiceReconciliationStatus.class);
            verify(repository).findAllByStatus(estado.capture(), org.mockito.ArgumentMatchers.eq(0),
                    org.mockito.ArgumentMatchers.eq(20));
            assertThat(estado.getValue())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL);
        }

        @Test
        @DisplayName("traslada pagina y tamano tal cual y conserva el total de la consulta")
        void traslada_pagina_y_tamano_tal_cual() {
            when(repository.findAllByStatus(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, 3,
                    7)).thenReturn(PageResult.of(List.of(), 3, 7, 214L));

            PageResult<ExternalInvoiceReconciliationDto> bandeja = service.listMissing(3, 7);

            assertThat(bandeja.page()).isEqualTo(3);
            assertThat(bandeja.pageSize()).isEqualTo(7);
            // 214 documentos devengados que nadie facturo. El numero es lo unico que
            // convierte esto en una alarma en vez de en una lista.
            assertThat(bandeja.totalElements()).isEqualTo(214L);
        }
    }
}
