package com.vetsoftware.app.catalogitemaihint.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import com.vetsoftware.app.catalogitemaihint.testsupport.CatalogItemAiHintMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Escrito desde el Javadoc de {@code ListCurrentCatalogItemAiHintsUseCase} y
 * del {@code CatalogItemQueryPort}, no desde el cuerpo del servicio.</b> Las
 * dos promesas que se afirman aqui son las que ninguna otra capa puede ver: que
 * los articulos de la pagina se resuelven con <b>una</b> consulta y no con una
 * por fila —{@code findAllByIds} existe exactamente para eso—, y que un
 * articulo que ya no esta vivo no desaparece del listado sino que sale sin
 * codigo ni nombre.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListCurrentCatalogItemAiHintsService — las vigentes, una consulta por pagina")
class ListCurrentCatalogItemAiHintsServiceTest {

    private static final Long PRIMERO = CatalogItemAiHintMother.ARTICULO_ID;
    private static final Long SEGUNDO = CatalogItemAiHintMother.OTRO_ARTICULO_ID;
    private static final Long TERCERO = CatalogItemAiHintMother.TERCER_ARTICULO_ID;

    @Mock
    private CatalogItemAiHintRepository repository;

    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    @InjectMocks
    private ListCurrentCatalogItemAiHintsService service;

    private static PageResult<CatalogItemAiHint> tresVigentes(int page, int pageSize,
            long totalElements, int totalPages) {
        return new PageResult<>(
                List.of(CatalogItemAiHintMother.vigente(7001L, PRIMERO, 1,
                        CatalogItemAiHintMother.texto(1)),
                        CatalogItemAiHintMother.vigente(7002L, SEGUNDO, 1,
                                CatalogItemAiHintMother.texto(1)),
                        CatalogItemAiHintMother.vigente(7003L, TERCERO, 2,
                                CatalogItemAiHintMother.texto(2))),
                page, pageSize, totalElements, totalPages);
    }

    private static Map<Long, CatalogItemRef> losTres() {
        return Map.of(PRIMERO, CatalogItemAiHintMother.ref(PRIMERO, "GROOMING", "Estetica"),
                SEGUNDO, CatalogItemAiHintMother.ref(SEGUNDO, "BOARDING", "Guarderia"), TERCERO,
                CatalogItemAiHintMother.ref(TERCERO, "CORE", "Nucleo"));
    }

    @Nested
    @DisplayName("Como resuelve los articulos")
    class Articulos {

        /**
         * &#9940; <b>El defecto que este caso existe para impedir.</b> Resolver el
         * articulo dentro del {@code map} —una llamada por fila— es un N+1 contra
         * {@code catalog_items} que solo se nota cuando el catalogo crece, que es justo
         * cuando el listado importa. La garantia es de <em>forma</em> de la consulta,
         * asi que se afirma que se pidieron los tres ids <b>de una vez</b> y que
         * {@code findById} no se toco.
         */
        @Test
        @DisplayName("pide los articulos de toda la pagina de una vez, no uno por fila")
        void una_sola_consulta_para_toda_la_pagina() {
            when(repository.findAllCurrent(0, 20)).thenReturn(tresVigentes(0, 20, 3L, 1));
            when(catalogItemQueryPort.findAllByIds(anyCollection())).thenReturn(losTres());

            PageResult<CatalogItemAiHintDto> resultado = service.listCurrent(0, 20);

            ArgumentCaptor<Collection<Long>> pedidos = ArgumentCaptor.captor();
            verify(catalogItemQueryPort).findAllByIds(pedidos.capture());
            assertThat(pedidos.getValue()).containsExactly(PRIMERO, SEGUNDO, TERCERO);
            verify(catalogItemQueryPort, never()).findById(any());
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::catalogItemCode)
                    .containsExactly("GROOMING", "BOARDING", "CORE");
        }

        /**
         * El puerto documenta que los articulos que no esten vivos «simplemente no
         * aparecen en el mapa». La pista sigue siendo una fila legitima: se sirve sin
         * codigo ni nombre, y no se cae ni se filtra fuera del listado.
         */
        @Test
        @DisplayName("un articulo ausente del mapa sale sin codigo y no desaparece del listado")
        void articulo_no_vivo_sale_sin_codigo() {
            when(repository.findAllCurrent(0, 20)).thenReturn(tresVigentes(0, 20, 3L, 1));
            when(catalogItemQueryPort.findAllByIds(anyCollection())).thenReturn(
                    Map.of(PRIMERO, CatalogItemAiHintMother.ref(PRIMERO, "GROOMING", "Estetica"),
                            TERCERO, CatalogItemAiHintMother.ref(TERCERO, "CORE", "Nucleo")));

            PageResult<CatalogItemAiHintDto> resultado = service.listCurrent(0, 20);

            assertThat(resultado.content()).hasSize(3);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::catalogItemId)
                    .containsExactly(PRIMERO, SEGUNDO, TERCERO);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::catalogItemCode)
                    .containsExactly("GROOMING", null, "CORE");
            assertThat(resultado.content().get(1).hintText())
                    .isEqualTo(CatalogItemAiHintMother.texto(1));
        }
    }

    @Nested
    @DisplayName("La pagina")
    class Paginacion {

        /**
         * Los totales son los de la consulta entera, no los de la lista ya recortada:
         * recalcularlos sobre el contenido paginado es como se acaba reportando «20 de
         * 20» en un listado de cincuenta mil.
         */
        @Test
        @DisplayName("conserva intactos los metadatos de paginacion del repositorio")
        void conserva_los_metadatos() {
            when(repository.findAllCurrent(2, 20)).thenReturn(tresVigentes(2, 20, 57L, 3));
            when(catalogItemQueryPort.findAllByIds(anyCollection())).thenReturn(losTres());

            PageResult<CatalogItemAiHintDto> resultado = service.listCurrent(2, 20);

            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.pageSize()).isEqualTo(20);
            assertThat(resultado.totalElements()).isEqualTo(57L);
            assertThat(resultado.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("solo lista vigentes: todas las filas vienen con current en true")
        void todas_las_filas_son_vigentes() {
            when(repository.findAllCurrent(0, 20)).thenReturn(tresVigentes(0, 20, 3L, 1));
            when(catalogItemQueryPort.findAllByIds(anyCollection())).thenReturn(losTres());

            PageResult<CatalogItemAiHintDto> resultado = service.listCurrent(0, 20);

            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::current)
                    .containsOnly(true);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::supersededAt)
                    .containsOnlyNulls();
        }

        @Test
        @DisplayName("una pagina vacia no es un error y conserva el total")
        void una_pagina_vacia_no_es_un_error() {
            when(repository.findAllCurrent(9, 20))
                    .thenReturn(new PageResult<>(List.of(), 9, 20, 57L, 3));
            when(catalogItemQueryPort.findAllByIds(anyCollection())).thenReturn(Map.of());

            PageResult<CatalogItemAiHintDto> resultado = service.listCurrent(9, 20);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.totalElements()).isEqualTo(57L);
        }
    }
}
