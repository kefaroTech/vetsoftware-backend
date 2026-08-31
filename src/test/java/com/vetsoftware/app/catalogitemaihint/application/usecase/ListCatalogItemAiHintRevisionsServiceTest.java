package com.vetsoftware.app.catalogitemaihint.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.testsupport.CatalogItemAiHintMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Escrito desde el Javadoc de {@code ListCatalogItemAiHintRevisionsUseCase},
 * no desde el cuerpo del servicio.</b>
 *
 * <p>
 * Este es el caso de uso que hace util al diseno append-only: sin esta lectura,
 * la revision reemplazada quedaria guardada y no la podria ver nadie, que a
 * efectos practicos es lo mismo que haberla borrado. Por eso lo primero que se
 * afirma aqui es que las reemplazadas <b>salen</b>, con su texto y su firmante,
 * y no solo la vigente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListCatalogItemAiHintRevisionsService — el historial entero, no solo la vigente")
class ListCatalogItemAiHintRevisionsServiceTest {

    private static final Long ARTICULO = CatalogItemAiHintMother.ARTICULO_ID;

    @Mock
    private CatalogItemAiHintRepository repository;

    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    @InjectMocks
    private ListCatalogItemAiHintRevisionsService service;

    /** Tres revisiones del mismo articulo, de la mas nueva a la mas vieja. */
    private static PageResult<CatalogItemAiHint> historial(int page, int pageSize,
            long totalElements, int totalPages) {
        return new PageResult<>(
                List.of(CatalogItemAiHintMother.vigente(7003L, ARTICULO, 3,
                        CatalogItemAiHintMother.texto(3)),
                        CatalogItemAiHintMother.reemplazada(7002L, ARTICULO, 2,
                                CatalogItemAiHintMother.texto(2)),
                        CatalogItemAiHintMother.reemplazada(7001L, ARTICULO, 1,
                                CatalogItemAiHintMother.texto(1))),
                page, pageSize, totalElements, totalPages);
    }

    @Nested
    @DisplayName("El historial")
    class Historial {

        /**
         * &#9940; Corregir deja la anterior <b>marcada</b>, no la borra: las dos
         * revisiones cerradas viajan con su propio texto, su propia revision y su
         * {@code supersededAt} puesto. Si alguien sustituyera el append-only por un
         * update en sitio, o filtrara por vigencia en este camino, aqui se veria.
         */
        @Test
        @DisplayName("sirve las revisiones reemplazadas con su texto, no solo la vigente")
        void el_historial_sirve_las_reemplazadas() {
            when(repository.findAllByCatalogItemId(ARTICULO, 0, 20))
                    .thenReturn(historial(0, 20, 3L, 1));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 0,
                    20);

            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::hintRevision)
                    .containsExactly(3, 2, 1);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::hintText)
                    .containsExactly(CatalogItemAiHintMother.texto(3),
                            CatalogItemAiHintMother.texto(2), CatalogItemAiHintMother.texto(1));
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::current)
                    .containsExactly(true, false, false);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::supersededAt)
                    .containsExactly(null, CatalogItemAiHintMother.REEMPLAZADA_EN,
                            CatalogItemAiHintMother.REEMPLAZADA_EN);
        }

        /**
         * &#9940; <b>El historial es donde se LEE la firma de retirada, y por eso el
         * DTO tiene que traerla.</b> Guardar en {@code superseded_by_system_user_id}
         * quien apago una pista y que ninguna lectura la sirva es tener la evidencia y
         * no poder ensenarla —una columna con escritor y sin lector—, que es justo el
         * hueco que el changeset 393 vino a cerrar. Este caso fija el tramo dominio →
         * DTO.
         *
         * <p>
         * Las tres filas se afirman juntas porque el <b>nulo de la vigente es
         * informacion</b>: significa «no la ha retirado nadie», y la pantalla tiene que
         * poder distinguirlo de «la retiro fulano». Y el valor esperado es
         * {@code RETIRADOR_ID} y no {@code FIRMANTE_ID}: si el mapeo sirviera el
         * firmante de publicacion —el que esta a mano en la misma fila— este caso lo
         * caza.
         */
        @Test
        @DisplayName("cada revision reemplazada dice quien la retiro; la vigente, nadie")
        void el_historial_dice_quien_retiro_cada_revision() {
            when(repository.findAllByCatalogItemId(ARTICULO, 0, 20))
                    .thenReturn(historial(0, 20, 3L, 1));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 0,
                    20);

            assertThat(resultado.content())
                    .extracting(CatalogItemAiHintDto::supersededBySystemUserId)
                    .as("la vigente no la ha retirado nadie; las dos cerradas si")
                    .containsExactly(null, CatalogItemAiHintMother.RETIRADOR_ID,
                            CatalogItemAiHintMother.RETIRADOR_ID);
            assertThat(resultado.content())
                    .extracting(CatalogItemAiHintDto::publishedBySystemUserId)
                    .as("y la firma de publicacion sigue siendo la suya, sin mezclarse")
                    .containsOnly(CatalogItemAiHintMother.FIRMANTE_ID);
        }

        /**
         * &#9940; <b>Los dos nulos significan cosas distintas, y el historial tiene que
         * poder servirlos a la vez.</b> Una fila reemplazada <em>antes</em> del
         * changeset 393 tiene fecha de cierre y no tiene firmante: su actor real nunca
         * se escribio y no se puede reconstruir. La vigente tampoco tiene firmante,
         * pero porque nadie la ha retirado.
         *
         * <p>
         * Para la pantalla no son lo mismo —«no consta quien la retiro» frente a «sigue
         * rigiendo»— y lo que los separa <b>no</b> es este campo sino
         * {@code supersededAt}, que en la historica esta puesto. Por eso el caso afirma
         * los dos campos juntos: si alguien intentara "arreglar" el nulo inventandole
         * un firmante a las filas viejas, aqui se veria — y eso convertiria una laguna
         * conocida en un dato falso indistinguible de una firma real.
         */
        @Test
        @DisplayName("una retirada anterior al changeset 393 sale sin firmante pero con fecha")
        void una_retirada_anterior_al_changeset_sale_sin_firmante() {
            when(repository.findAllByCatalogItemId(ARTICULO, 0, 20))
                    .thenReturn(new PageResult<>(List.of(
                            CatalogItemAiHintMother.vigente(7003L, ARTICULO, 3,
                                    CatalogItemAiHintMother.texto(3)),
                            CatalogItemAiHintMother.reemplazadaSinFirma(7001L, ARTICULO, 1,
                                    CatalogItemAiHintMother.texto(1))),
                            0, 20, 2L, 1));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 0,
                    20);

            assertThat(resultado.content())
                    .extracting(CatalogItemAiHintDto::supersededBySystemUserId)
                    .as("las dos sin firmante, por motivos distintos").containsExactly(null, null);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::supersededAt)
                    .as("y es supersededAt quien los separa: no consta vs no retirada")
                    .containsExactly(null, CatalogItemAiHintMother.REEMPLAZADA_EN);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::current)
                    .containsExactly(true, false);
        }

        /**
         * El orden lo fija el repositorio —revision descendente— y este servicio no lo
         * reordena: {@code PageResult.map} conserva la secuencia. Servirlo al reves
         * pondria la revision mas vieja arriba en una pantalla que existe para ver que
         * se le esta diciendo al modelo <em>ahora</em>.
         */
        @Test
        @DisplayName("respeta el orden que devuelve el repositorio y no lo reordena")
        void respeta_el_orden_del_repositorio() {
            when(repository.findAllByCatalogItemId(ARTICULO, 0, 20))
                    .thenReturn(new PageResult<>(List.of(
                            CatalogItemAiHintMother.reemplazada(7001L, ARTICULO, 1,
                                    CatalogItemAiHintMother.texto(1)),
                            CatalogItemAiHintMother.vigente(7003L, ARTICULO, 3,
                                    CatalogItemAiHintMother.texto(3))),
                            0, 20, 2L, 1));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 0,
                    20);

            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::hintRevision)
                    .containsExactly(1, 3);
        }

        @Test
        @DisplayName("conserva intactos los metadatos de paginacion del repositorio")
        void conserva_los_metadatos() {
            when(repository.findAllByCatalogItemId(ARTICULO, 1, 20))
                    .thenReturn(historial(1, 20, 41L, 3));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 1,
                    20);

            assertThat(resultado.page()).isEqualTo(1);
            assertThat(resultado.pageSize()).isEqualTo(20);
            assertThat(resultado.totalElements()).isEqualTo(41L);
            assertThat(resultado.totalPages()).isEqualTo(3);
        }

        /**
         * Un articulo sin ninguna revision es una pagina vacia, no un 404. Es la
         * diferencia deliberada con {@code FindCurrentCatalogItemAiHintService}: alli
         * «no tiene pista» es un estado del recurso pedido; aqui es una coleccion que
         * resulta estar vacia.
         */
        @Test
        @DisplayName("un articulo sin revisiones es una pagina vacia, no un 404")
        void historial_vacio_no_es_un_404() {
            when(repository.findAllByCatalogItemId(ARTICULO, 0, 20))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 0,
                    20);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("El articulo de la cabecera")
    class Articulo {

        /**
         * &#9940; Todas las revisiones son del <b>mismo</b> articulo, asi que
         * resolverlo dentro del {@code map} pediria n veces la misma fila de
         * {@code catalog_items}. Se afirma la unica llamada —y que no se recurre al
         * {@code findAllByIds} del listado general, que aqui seria pedir un lote de un
         * elemento repetido—.
         */
        @Test
        @DisplayName("resuelve el articulo una sola vez para la pagina entera")
        void resuelve_el_articulo_una_sola_vez() {
            when(repository.findAllByCatalogItemId(ARTICULO, 0, 20))
                    .thenReturn(historial(0, 20, 3L, 1));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 0,
                    20);

            verify(catalogItemQueryPort, times(1)).findById(ARTICULO);
            verify(catalogItemQueryPort, never()).findAllByIds(anyCollection());
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::catalogItemCode)
                    .containsOnly("GROOMING");
        }

        /**
         * El articulo retirado del catalogo no puede tapar su historial: es justo el
         * caso en el que alguien entra a ver que se le estuvo diciendo al modelo.
         */
        @Test
        @DisplayName("si el articulo ya no esta vivo, el historial sale sin codigo ni nombre")
        void articulo_no_vivo_no_esconde_el_historial() {
            when(repository.findAllByCatalogItemId(ARTICULO, 0, 20))
                    .thenReturn(historial(0, 20, 3L, 1));
            when(catalogItemQueryPort.findById(any())).thenReturn(Optional.empty());

            PageResult<CatalogItemAiHintDto> resultado = service.listByCatalogItemId(ARTICULO, 0,
                    20);

            assertThat(resultado.content()).hasSize(3);
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::catalogItemCode)
                    .containsOnlyNulls();
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::catalogItemName)
                    .containsOnlyNulls();
            assertThat(resultado.content()).extracting(CatalogItemAiHintDto::hintText)
                    .containsExactly(CatalogItemAiHintMother.texto(3),
                            CatalogItemAiHintMother.texto(2), CatalogItemAiHintMother.texto(1));
        }
    }
}
