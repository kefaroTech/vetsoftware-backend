package com.vetsoftware.app.catalogitemaihint.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintNotFoundException;
import com.vetsoftware.app.catalogitemaihint.testsupport.CatalogItemAiHintMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Escrito desde el Javadoc de {@code RetireCatalogItemAiHintUseCase} y del
 * {@code CatalogItemAiHintRepository}, no desde el cuerpo del servicio.</b>
 *
 * <p>
 * Retirar es <em>media</em> correccion: la misma primera escritura que
 * {@code ReviseCatalogItemAiHintService} sin la segunda. Que el efecto de
 * negocio sea el opuesto —el modelo deja de proponer el articulo— y la
 * escritura sea la misma es lo que obliga a que sean puertos distintos, y es lo
 * que se afirma aqui: que cierra la vigencia y que <b>no</b> publica sucesora.
 *
 * <p>
 * &#9888; El {@code DELETE} de la ruta es la semantica HTTP del recurso «la
 * pista vigente de este articulo», no un borrado de fila: la revision retirada
 * se queda en el historial con su texto y su fecha. Por eso el puerto no
 * declara ningun {@code delete} y aqui se comprueba que la fila que viaja a
 * {@code supersede} llega entera.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetireCatalogItemAiHintService — cierra la vigencia y no borra la fila")
class RetireCatalogItemAiHintServiceTest {

    private static final Long ARTICULO = CatalogItemAiHintMother.ARTICULO_ID;

    @Mock
    private CatalogItemAiHintRepository repository;

    private RetireCatalogItemAiHintService service;

    @BeforeEach
    void servicio() {
        service = new RetireCatalogItemAiHintService(repository,
                CatalogItemAiHintMother.relojFijo());
    }

    /**
     * Unico sitio del test que invoca el caso de uso: si el puerto gana un
     * parametro, el cambio es esta linea y no diez.
     */
    private void retirar() {
        service.retire(CatalogItemAiHintMother.comandoDeRetirada());
    }

    private CatalogItemAiHint loCerrado() {
        ArgumentCaptor<CatalogItemAiHint> cerrada = ArgumentCaptor.captor();
        verify(repository).supersede(cerrada.capture());
        return cerrada.getValue();
    }

    @Nested
    @DisplayName("La retirada")
    class Retirada {

        /**
         * &#9940; <b>El defecto que este caso existe para impedir.</b> Retirar no puede
         * publicar nada: si esto derivara hacia {@code save}, el articulo se quedaria
         * con una revision nueva —o con dos vigentes, que
         * {@code uq_catalog_item_ai_hints_current} rechaza— en una operacion cuyo
         * proposito es exactamente el contrario.
         */
        @Test
        @DisplayName("cierra la vigencia sin publicar sucesora")
        void cierra_la_vigencia_sin_sucesora() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.vigente()));

            retirar();

            CatalogItemAiHint cerrada = loCerrado();
            assertThat(cerrada.getSupersededAt()).isEqualTo(CatalogItemAiHintMother.AHORA);
            assertThat(cerrada.isCurrent()).isFalse();
            verify(repository, never()).save(any());
        }

        /**
         * &#9940; La fila no se borra ni se vacia: sigue en el historial con su texto,
         * su revision, su fecha de publicacion y el firmante que la publico. Si alguien
         * sustituyera el cierre por un borrado —o reescribiera el texto al retirar—, se
         * perderia la unica evidencia de que se le estaba diciendo al modelo cuando
         * genero una propuesta pasada.
         */
        @Test
        @DisplayName("la revision retirada conserva su texto, su revision y su firmante")
        void la_retirada_sigue_entera_en_el_historial() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.vigente(7002L, ARTICULO, 2,
                            CatalogItemAiHintMother.texto(2))));

            retirar();

            CatalogItemAiHint cerrada = loCerrado();
            assertThat(cerrada.getId()).isEqualTo(7002L);
            assertThat(cerrada.getCatalogItemId()).isEqualTo(ARTICULO);
            assertThat(cerrada.getHintRevision()).isEqualTo(2);
            assertThat(cerrada.getHintText()).isEqualTo(CatalogItemAiHintMother.texto(2));
            assertThat(cerrada.getPublishedAt()).isEqualTo(CatalogItemAiHintMother.PUBLICADA_EN);
            assertThat(cerrada.getPublishedBySystemUserId())
                    .isEqualTo(CatalogItemAiHintMother.FIRMANTE_ID);
        }

        /**
         * &#9940; <b>La firma que faltaba, y el caso que la sostiene.</b> Hasta el
         * changeset 393 retirar no escribia ningun actor: la fila retirada conservaba
         * el {@code published_by_system_user_id} de quien la habia publicado, asi que
         * el historial afirmaba —sin decirlo— que la habia apagado quien la escribio.
         * Quien de verdad decidio que el modulo dejara de proponerse no constaba en
         * ningun sitio, y el generador de recomendaciones a un prospecto anonimo
         * quedaba sin rastro de esa decision.
         *
         * <p>
         * Las dos aserciones son distintas a proposito. La primera exige que el actor
         * sea el del command; la segunda, que <b>no</b> sea el firmante de publicacion.
         * Sin la segunda, un
         * {@code supersede(ahora, vigente.getPublishedBySystemUserId())} —el atajo mas
         * facil de escribir, porque el dato esta a mano en el agregado— pasaria en
         * verde y volveria a atribuir la retirada al autor del texto.
         */
        @Test
        @DisplayName("la retirada queda firmada por quien la pidio, no por quien publico")
        void la_retirada_queda_firmada_por_quien_la_pidio() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.vigente()));

            retirar();

            CatalogItemAiHint cerrada = loCerrado();
            assertThat(cerrada.getSupersededBySystemUserId())
                    .as("el actor sale del command, que lo trae de la sesion")
                    .isEqualTo(CatalogItemAiHintMother.RETIRADOR_ID);
            assertThat(cerrada.getSupersededBySystemUserId())
                    .as("y NO es el firmante de publicacion, que es el actor equivocado")
                    .isNotEqualTo(CatalogItemAiHintMother.FIRMANTE_ID);
            assertThat(cerrada.getPublishedBySystemUserId())
                    .as("la columna de publicacion sigue intacta")
                    .isEqualTo(CatalogItemAiHintMother.FIRMANTE_ID);
        }
    }

    @Nested
    @DisplayName("Cuando no hay nada que retirar")
    class SinVigente {

        /**
         * El articulo sin pista no es un no-op silencioso: quien pulsa «retirar» sobre
         * algo que ya no rige tiene que enterarse, y sobre todo no puede quedar ninguna
         * escritura.
         */
        @Test
        @DisplayName("un articulo sin pista vigente es 404 y no escribe nada")
        void sin_vigente_es_404_y_no_escribe() {
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> retirar())
                    .isInstanceOf(CatalogItemAiHintNotFoundException.class)
                    .hasMessageContaining("has no current AI hint");

            verify(repository, never()).supersede(any());
            verify(repository, never()).save(any());
        }

        /**
         * &#9940; Retirar dos veces. Tras la primera el articulo ya no tiene vigente,
         * asi que la segunda no encuentra nada que cerrar: 404 y una sola escritura en
         * total. Si el servicio releyera una fila ya cerrada, el segundo
         * {@code supersede} moveria {@code superseded_at} y falsearia la fecha en la
         * que el modulo dejo de proponerse.
         */
        @Test
        @DisplayName("retirar dos veces: la segunda es 404 y solo queda una escritura")
        void retirar_dos_veces_solo_escribe_una() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.vigente()), Optional.empty());

            retirar();

            assertThatThrownBy(() -> retirar())
                    .isInstanceOf(CatalogItemAiHintNotFoundException.class);

            verify(repository, times(1)).supersede(any());
            verify(repository, never()).save(any());
        }
    }
}
