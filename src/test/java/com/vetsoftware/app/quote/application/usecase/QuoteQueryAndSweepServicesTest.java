package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.lineaModulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.persistida;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.resumen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.quote.application.dto.QuoteSummaryDto;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.InvalidQuoteStatusTransitionException;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Listados, baja logica y barrido de vencimiento")
class QuoteQueryAndSweepServicesTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());
    private static final Long ID = 55L;
    private static final Long EMPRESA = 42L;

    @Mock
    private QuoteRepository repository;

    @Nested
    @DisplayName("ListQuotesByCompanyService")
    class ListadoDelTenant {

        @Test
        @DisplayName("proyecta la cabecera con sus totales guardados, sin tocar las lineas")
        void proyecta_la_cabecera_con_totales() {
            when(repository.findAllByCompanyId(EMPRESA, 0, 20))
                    .thenReturn(PageResult.of(List.of(resumen(ID, QuoteStatus.SENT)), 0, 20, 1));

            PageResult<QuoteSummaryDto> pagina = new ListQuotesByCompanyService(repository)
                    .listByCompany(EMPRESA, 0, 20);

            assertThat(pagina.content()).singleElement().satisfies(fila -> {
                assertThat(fila.id()).isEqualTo(ID);
                assertThat(fila.status()).isEqualTo("SENT");
                assertThat(fila.totalAmount()).isEqualByComparingTo("119000.00");
            });
        }

        @Test
        @DisplayName("conserva los metadatos de la pagina en vez de recalcularlos")
        void conserva_los_metadatos_de_la_pagina() {
            when(repository.findAllByCompanyId(EMPRESA, 2, 20))
                    .thenReturn(PageResult.of(List.of(resumen(ID, QuoteStatus.DRAFT)), 2, 20, 41));

            PageResult<QuoteSummaryDto> pagina = new ListQuotesByCompanyService(repository)
                    .listByCompany(EMPRESA, 2, 20);

            assertThat(pagina.page()).isEqualTo(2);
            assertThat(pagina.pageSize()).isEqualTo(20);
            assertThat(pagina.totalElements()).isEqualTo(41);
            assertThat(pagina.totalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("ListQuotesService")
    class ListadoDePlataforma {

        @Test
        @DisplayName("lee el embudo completo sin acotar por empresa")
        void lee_el_embudo_completo() {
            when(repository.findAll(0, 20)).thenReturn(PageResult.of(
                    List.of(resumen(ID, QuoteStatus.SENT), resumen(56L, QuoteStatus.DRAFT)), 0, 20,
                    2));

            PageResult<QuoteSummaryDto> pagina = new ListQuotesService(repository).listAll(0, 20);

            assertThat(pagina.content()).hasSize(2);
            assertThat(pagina.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("una pagina vacia no revienta ni inventa filas")
        void una_pagina_vacia_no_revienta() {
            when(repository.findAll(9, 20)).thenReturn(PageResult.empty(9, 20));

            PageResult<QuoteSummaryDto> pagina = new ListQuotesService(repository).listAll(9, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("DeleteQuoteService")
    class BajaLogica {

        @Test
        @DisplayName("con empresa carga acotado y borra por la sobrecarga acotada")
        void con_empresa_usa_la_sobrecarga_acotada() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.DRAFT)));

            new DeleteQuoteService(repository).execute(ID, EMPRESA);

            verify(repository).softDelete(ID, EMPRESA);
            verify(repository, never()).softDelete(anyLong());
        }

        @Test
        @DisplayName("sin empresa usa la ancha: una oferta a prospecto tiene company_id nulo")
        void sin_empresa_usa_la_sobrecarga_ancha() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.DRAFT)));

            new DeleteQuoteService(repository).execute(ID, null);

            verify(repository).softDelete(ID);
            verify(repository, never()).softDelete(anyLong(), anyLong());
        }

        @Test
        @DisplayName("una oferta ya enviada no se da de baja: borraria la prueba de lo ofrecido")
        void una_enviada_no_se_da_de_baja() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));

            assertThatThrownBy(() -> new DeleteQuoteService(repository).execute(ID, EMPRESA))
                    .isInstanceOf(InvalidQuoteStatusTransitionException.class);

            verify(repository, never()).softDelete(anyLong(), anyLong());
            verify(repository, never()).softDelete(anyLong());
        }

        @Test
        @DisplayName("un id de otro tenant no ejecuta ningun UPDATE a ciegas")
        void un_id_ajeno_no_ejecuta_update() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> new DeleteQuoteService(repository).execute(ID, EMPRESA))
                    .isInstanceOf(QuoteNotFoundException.class);

            verify(repository, never()).softDelete(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("ExpireOverdueQuotesService")
    class BarridoDeVencimiento {

        private static final LocalDate YA_VENCIDA = LocalDate.of(2026, 8, 21);

        @Test
        @DisplayName("marca EXPIRED cada una de las vencidas y devuelve cuantas movio")
        void marca_las_vencidas() {
            Quote unaVencida = persistida(ID, QuoteStatus.SENT, YA_VENCIDA, List.of(lineaModulo()));
            Quote otraVencida = persistida(56L, QuoteStatus.DRAFT, YA_VENCIDA,
                    List.of(lineaModulo()));
            when(repository.findExpirable(AHORA.toLocalDate(), 200))
                    .thenReturn(List.of(unaVencida, otraVencida));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            int movidas = new ExpireOverdueQuotesService(repository, RELOJ).expireOverdue(200);

            assertThat(movidas).isEqualTo(2);
            assertThat(unaVencida.getStatus()).isEqualTo(QuoteStatus.EXPIRED);
            assertThat(otraVencida.getStatus()).isEqualTo(QuoteStatus.EXPIRED);
        }

        @Test
        @DisplayName("sin nada que vencer no escribe: cero es el estado normal")
        void sin_nada_que_vencer_no_escribe() {
            when(repository.findExpirable(AHORA.toLocalDate(), 200)).thenReturn(List.of());

            int movidas = new ExpireOverdueQuotesService(repository, RELOJ).expireOverdue(200);

            assertThat(movidas).isZero();
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("usa la fecha del reloj inyectado, no la del reloj de la maquina")
        void usa_la_fecha_del_reloj_inyectado() {
            LocalDate otroDia = LocalDate.of(2027, 3, 1);
            Clock enOtroDia = Clock.fixed(otroDia.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault());
            when(repository.findExpirable(otroDia, 50)).thenReturn(List.of());

            new ExpireOverdueQuotesService(repository, enOtroDia).expireOverdue(50);

            verify(repository).findExpirable(otroDia, 50);
        }
    }
}
