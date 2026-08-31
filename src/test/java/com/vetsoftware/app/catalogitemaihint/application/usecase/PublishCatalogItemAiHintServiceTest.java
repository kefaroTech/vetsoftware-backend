package com.vetsoftware.app.catalogitemaihint.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintAlreadyPublishedException;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintTextAlreadyPublishedException;
import com.vetsoftware.app.catalogitemaihint.domain.HintCatalogItemNotFoundException;
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
 * <b>Escrito desde el Javadoc de {@code PublishCatalogItemAiHintUseCase}, de su
 * command y de las tres excepciones, no desde el cuerpo del servicio.</b>
 *
 * <p>
 * Las tres guardas que se afirman aqui son las tres restricciones de la tabla,
 * cada una preguntada <em>antes</em> de que el motor la imponga: la clave
 * foranea del articulo, la unicidad de la vigente y la del texto. Que se
 * pregunten aqui y no se dejen saltar no es un detalle de estilo —una violacion
 * de integridad sale como 500 y el cliente no la puede distinguir de una
 * caida—, y ninguna de las tres se puede afirmar en la rodaja de persistencia,
 * donde solo se ve el efecto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublishCatalogItemAiHintService — publica la primera, y solo la primera")
class PublishCatalogItemAiHintServiceTest {

    private static final Long ARTICULO = CatalogItemAiHintMother.ARTICULO_ID;

    @Mock
    private CatalogItemAiHintRepository repository;

    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    private PublishCatalogItemAiHintService service;

    @BeforeEach
    void servicio() {
        service = new PublishCatalogItemAiHintService(repository, catalogItemQueryPort,
                CatalogItemAiHintMother.relojFijo());
    }

    /**
     * Unico sitio del test que invoca el caso de uso: si el command gana un campo,
     * el cambio es aqui y en el mother, no repartido por diez metodos.
     */
    private CatalogItemAiHintDto publicar(String texto) {
        return service.execute(CatalogItemAiHintMother.comandoDePublicacion(texto));
    }

    private ArgumentCaptor<CatalogItemAiHint> loGuardado() {
        ArgumentCaptor<CatalogItemAiHint> guardada = ArgumentCaptor.captor();
        verify(repository).save(guardada.capture());
        return guardada;
    }

    @Nested
    @DisplayName("Publicacion")
    class Publicacion {

        @Test
        @DisplayName("guarda la revision 1 firmada por el usuario del command y vigente")
        void guarda_la_primera_revision() {
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());
            when(repository.existsPublishedText(ARTICULO, CatalogItemAiHintMother.TRES_PARTES))
                    .thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            publicar(CatalogItemAiHintMother.TRES_PARTES);

            CatalogItemAiHint guardada = loGuardado().getValue();
            assertThat(guardada.getId()).isNull();
            assertThat(guardada.getCatalogItemId()).isEqualTo(ARTICULO);
            assertThat(guardada.getHintRevision()).isEqualTo(1);
            assertThat(guardada.getHintText()).isEqualTo(CatalogItemAiHintMother.TRES_PARTES);
            assertThat(guardada.getPublishedBySystemUserId())
                    .isEqualTo(CatalogItemAiHintMother.FIRMANTE_ID);
            assertThat(guardada.getPublishedAt()).isEqualTo(CatalogItemAiHintMother.AHORA);
            assertThat(guardada.getSupersededAt()).isNull();
            assertThat(guardada.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("responde con el codigo y el nombre del articulo ya resueltos")
        void responde_con_el_articulo_resuelto() {
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());
            when(repository.existsPublishedText(ARTICULO, CatalogItemAiHintMother.TRES_PARTES))
                    .thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            CatalogItemAiHintDto dto = publicar(CatalogItemAiHintMother.TRES_PARTES);

            assertThat(dto.catalogItemCode()).isEqualTo("GROOMING");
            assertThat(dto.catalogItemName()).isEqualTo("Estetica");
            assertThat(dto.hintRevision()).isEqualTo(1);
            assertThat(dto.current()).isTrue();
        }

        /**
         * &#9940; <b>El defecto que este caso existe para impedir.</b> El numero de
         * revision sale del ultimo <em>publicado</em> —vigente o no—, no de cuantas hay
         * vigentes. La diferencia solo se ve aqui, y es la que rompe: despues de
         * retirar, el articulo no tiene vigente pero su historial sigue teniendo la
         * revision 2, asi que reiniciar en 1 chocaria contra
         * {@code uq_catalog_item_ai_hints_revision}. Republicar continua en 3.
         */
        @Test
        @DisplayName("tras retirar, continua la numeracion del historial en vez de reiniciarla")
        void republicar_continua_la_numeracion() {
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.of(2));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            CatalogItemAiHintDto dto = publicar(CatalogItemAiHintMother.TRES_PARTES);

            assertThat(loGuardado().getValue().getHintRevision()).isEqualTo(3);
            assertThat(dto.hintRevision()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("La regla de las tres partes")
    class TresPartes {

        /**
         * La convencion exige <b>estructura y no vocabulario</b>: al menos tres bloques
         * separados por linea en blanco. De las catorce pistas que siembra el changeset
         * 382, trece cierran con «NO se necesita si…» y {@code CORE} con «NUNCA lo
         * devuelvas…»; un predicado por literal habria rechazado a la decimocuarta y a
         * cualquier forma legitima de decir lo mismo.
         */
        @Test
        @DisplayName("acepta tres bloques aunque no usen la formula de cierre habitual")
        void acepta_otro_cierre_con_la_misma_estructura() {
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            publicar(CatalogItemAiHintMother.TRES_PARTES_OTRO_CIERRE);

            assertThat(loGuardado().getValue().getHintText())
                    .isEqualTo(CatalogItemAiHintMother.TRES_PARTES_OTRO_CIERRE);
        }

        /**
         * Sin el bloque del contraejemplo —cuando NO aplica— el modelo mete de todo. La
         * comprobacion vive en {@code publish} y no en el constructor a proposito: el
         * constructor lo ejecuta tambien el mapeador al <em>leer</em>, y una guarda
         * editorial ahi reventaria al abrir la pantalla que existe para corregir una
         * pista incompleta.
         */
        @Test
        @DisplayName("un texto de dos bloques se rechaza y no llega a guardarse")
        void dos_bloques_no_se_publican() {
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicar(CatalogItemAiHintMother.DOS_PARTES))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 3 blocks");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Lo que no se escribe")
    class NoSeEscribe {

        /**
         * Sin esta guarda la peticion llegaria a la base y la clave foranea
         * {@code fk_catalog_item_ai_hints_item} la pararia con un
         * {@code DataIntegrityViolation}, que el cliente lee como un 500. Y es la
         * primera de las tres: al repositorio no se le pregunta nada.
         */
        @Test
        @DisplayName("un articulo inexistente se rechaza sin tocar el repositorio")
        void articulo_inexistente_no_toca_el_repositorio() {
            when(catalogItemQueryPort.findById(ARTICULO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicar(CatalogItemAiHintMother.TRES_PARTES))
                    .isInstanceOf(HintCatalogItemNotFoundException.class)
                    .hasMessageContaining("not found");

            verifyNoInteractions(repository);
        }

        /**
         * &#9940; Publicar <b>no</b> sucede en silencio a la que hubiera. Un
         * {@code POST} repetido por un doble clic dejaria la revision 1 marcada como
         * reemplazada y una revision 2 identica en el historial, sin que nadie lo
         * hubiera pedido. Quien quiere cambiar el texto usa el camino que lo dice.
         */
        @Test
        @DisplayName("un articulo que ya tiene vigente es conflicto: no la sucede ni guarda")
        void con_vigente_no_publica_ni_sucede() {
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.vigente()));

            assertThatThrownBy(() -> publicar(CatalogItemAiHintMother.TRES_PARTES))
                    .isInstanceOf(CatalogItemAiHintAlreadyPublishedException.class)
                    .hasMessageContaining("already has a current AI hint");

            verify(repository, never()).save(any());
            verify(repository, never()).supersede(any());
            verify(repository, never()).existsPublishedText(anyLong(), anyString());
        }

        /**
         * Espejo de {@code uq_catalog_item_ai_hints_text}: si dos revisiones dicen
         * exactamente lo mismo, «con que texto se genero esta propuesta» deja de tener
         * una respuesta util.
         */
        @Test
        @DisplayName("un texto ya publicado bajo ese articulo es conflicto y no se guarda")
        void texto_repetido_no_se_publica() {
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());
            when(repository.existsPublishedText(ARTICULO, CatalogItemAiHintMother.TRES_PARTES))
                    .thenReturn(true);

            assertThatThrownBy(() -> publicar(CatalogItemAiHintMother.TRES_PARTES))
                    .isInstanceOf(CatalogItemAiHintTextAlreadyPublishedException.class)
                    .hasMessageContaining("was already published for catalog item");

            verify(repository, never()).save(any());
        }
    }
}
