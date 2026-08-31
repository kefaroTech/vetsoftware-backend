package com.vetsoftware.app.catalogitemaihint.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitemaihint.application.command.ReviseCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintNotFoundException;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintTextAlreadyPublishedException;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Escrito desde el Javadoc de {@code CatalogItemAiHintRepository}, no desde
 * el cuerpo del servicio.</b> Lo que se afirma aqui son las dos promesas que el
 * puerto hace por escrito y que ningun otro nivel puede comprobar: que el
 * cierre de la vigente se escribe <em>antes</em> del alta de la sucesora, y que
 * las dos escrituras comparten un unico instante. La rodaja de persistencia ve
 * el efecto en la base; esto ve la decision.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviseCatalogItemAiHintService — corregir es suceder y publicar, en ese orden")
class ReviseCatalogItemAiHintServiceTest {

    private static final Long ARTICULO = 4400L;
    private static final Long FIRMANTE = 990L;

    /**
     * Quien publico el texto viejo y quien lo corrige hoy no tienen por que ser la
     * misma persona: con un unico id, un servicio que firmara el cierre con
     * {@code publishedBySystemUserId} pasaria en verde.
     */
    private static final Long OTRO_FIRMANTE = 991L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 1, 12, 0, 0);

    private static final String TEXTO_V1 = """
            Se necesita cuando el negocio ofrece bano y peluqueria.

            Senales en el texto: "peluqueria", "bano".

            NO se necesita si el negocio es solo clinico.""";

    private static final String TEXTO_V2 = """
            Se necesita cuando el negocio ofrece bano, peluqueria o guarderia.

            Senales en el texto: "peluqueria", "spa", "guarderia".

            NO se necesita si el animal se queda por estar enfermo.""";

    @Mock
    private CatalogItemAiHintRepository repository;

    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    private ReviseCatalogItemAiHintService service;

    @BeforeEach
    void servicio() {
        Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ReviseCatalogItemAiHintService(repository, catalogItemQueryPort, reloj);
    }

    private CatalogItemAiHint vigente() {
        return new CatalogItemAiHint(7001L, ARTICULO, 1, TEXTO_V1,
                LocalDateTime.of(2026, 3, 1, 12, 0), FIRMANTE, null, null,
                LocalDateTime.of(2026, 3, 1, 12, 0), 0L);
    }

    private ReviseCatalogItemAiHintCommand command() {
        return new ReviseCatalogItemAiHintCommand(ARTICULO, TEXTO_V2, FIRMANTE);
    }

    @Nested
    @DisplayName("El orden de las dos escrituras")
    class Orden {

        /**
         * &#9940; <b>El defecto que este caso existe para impedir.</b> El puerto
         * documenta que Hibernate emite los INSERT antes que los UPDATE: si el servicio
         * dejara las dos escrituras al flush de la transaccion, la revision nueva
         * entraria mientras la vieja sigue vigente y
         * {@code uq_catalog_item_ai_hints_current} abortaria la correccion entera. La
         * garantia es de <em>secuencia</em>, y por eso se comprueba con un
         * {@code InOrder} y no con dos {@code verify} sueltos —que pasarian igual con
         * las llamadas al reves—.
         */
        @Test
        @DisplayName("cierra la vigente ANTES de guardar la sucesora")
        void cierra_antes_de_guardar() {
            CatalogItemAiHint anterior = vigente();
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.of(anterior));
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.of(1));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(command());

            InOrder orden = inOrder(repository);
            orden.verify(repository).supersede(any());
            orden.verify(repository).save(any());
        }

        /**
         * La anterior se marca, no se reescribe: al puerto le llega el mismo agregado
         * con su texto y su revision intactos, y lo unico que cambio es
         * {@code supersededAt}. Si alguien sustituyera esto por un update del texto en
         * sitio, aqui se veria.
         */
        @Test
        @DisplayName("a supersede le llega la anterior con su texto intacto y solo la fecha puesta")
        void la_anterior_conserva_su_texto() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(vigente()));
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.of(1));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(command());

            ArgumentCaptor<CatalogItemAiHint> cerrada = ArgumentCaptor
                    .forClass(CatalogItemAiHint.class);
            verify(repository).supersede(cerrada.capture());
            assertThat(cerrada.getValue().getHintText()).isEqualTo(TEXTO_V1);
            assertThat(cerrada.getValue().getHintRevision()).isEqualTo(1);
            assertThat(cerrada.getValue().getSupersededAt()).isEqualTo(AHORA);
            assertThat(cerrada.getValue().isCurrent()).isFalse();
        }

        /**
         * Un solo instante para las dos escrituras. Con dos lecturas del reloj queda un
         * hueco en el que ninguna revision rigio, y
         * {@code chk_catalog_item_ai_hints_supersede} no lo detecta porque solo compara
         * dentro de una fila.
         */
        @Test
        @DisplayName("el superseded_at de la vieja y el published_at de la nueva son el mismo")
        void un_solo_instante_para_las_dos() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(vigente()));
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.of(1));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(command());

            ArgumentCaptor<CatalogItemAiHint> cerrada = ArgumentCaptor
                    .forClass(CatalogItemAiHint.class);
            ArgumentCaptor<CatalogItemAiHint> nueva = ArgumentCaptor
                    .forClass(CatalogItemAiHint.class);
            verify(repository).supersede(cerrada.capture());
            verify(repository).save(nueva.capture());
            assertThat(nueva.getValue().getPublishedAt())
                    .isEqualTo(cerrada.getValue().getSupersededAt());
        }

        /**
         * &#9940; <b>Corregir tiene que seguir dejando su firma, y en las DOS
         * columnas.</b> Desde el changeset 393 la revision que se cierra guarda
         * {@code superseded_by_system_user_id} ademas de la fecha, y la que entra
         * guarda su {@code published_by_system_user_id}. Es lo que permite leer en el
         * historial dos cosas distintas: quien escribio cada texto y quien decidio que
         * cada uno dejara de regir.
         *
         * <p>
         * Este servicio <b>no cambio de firma</b> para ganarlo —el actor ya venia en el
         * command—, y por eso el modo de fallo realista no es que falte el dato sino
         * que se pase el equivocado: un {@code supersede(ahora, null)} o, peor, un
         * {@code supersede(ahora, vigente.getPublishedBySystemUserId())}, que atribuye
         * el cierre a quien escribio el texto viejo y pasaria inadvertido en cuanto
         * coincidieran. Las dos aserciones de aqui abajo lo cazan por separado.
         */
        @Test
        @DisplayName("la revision cerrada queda firmada por quien corrige, no sin firma")
        void la_cerrada_queda_firmada_por_quien_corrige() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(vigente()));
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.of(1));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(new ReviseCatalogItemAiHintCommand(ARTICULO, TEXTO_V2, OTRO_FIRMANTE));

            ArgumentCaptor<CatalogItemAiHint> cerrada = ArgumentCaptor
                    .forClass(CatalogItemAiHint.class);
            ArgumentCaptor<CatalogItemAiHint> nueva = ArgumentCaptor
                    .forClass(CatalogItemAiHint.class);
            verify(repository).supersede(cerrada.capture());
            verify(repository).save(nueva.capture());
            assertThat(cerrada.getValue().getSupersededBySystemUserId())
                    .as("cierra quien corrige ahora, no quien publico el texto viejo")
                    .isEqualTo(OTRO_FIRMANTE).isNotEqualTo(FIRMANTE);
            assertThat(cerrada.getValue().getPublishedBySystemUserId())
                    .as("y la columna de publicacion de la vieja NO se toca").isEqualTo(FIRMANTE);
            assertThat(nueva.getValue().getPublishedBySystemUserId()).isEqualTo(OTRO_FIRMANTE);
        }
    }

    @Nested
    @DisplayName("La revision nueva")
    class RevisionNueva {

        /**
         * El numero sale del ultimo publicado, no de cuantas hay vigentes. Aqui el
         * historial va por la 4 aunque solo haya una vigente: la siguiente es la 5.
         */
        @Test
        @DisplayName("continua la numeracion del historial y la firma el usuario del command")
        void continua_la_numeracion() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(vigente()));
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.of(4));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(new CatalogItemRef(ARTICULO, "GROOMING", "Estetica")));

            CatalogItemAiHintDto resultado = service.execute(command());

            assertThat(resultado.hintRevision()).isEqualTo(5);
            assertThat(resultado.hintText()).isEqualTo(TEXTO_V2);
            assertThat(resultado.publishedBySystemUserId()).isEqualTo(FIRMANTE);
            assertThat(resultado.current()).isTrue();
            assertThat(resultado.catalogItemCode()).isEqualTo("GROOMING");
        }

        /**
         * El articulo retirado no impide leer su historial: el DTO sale sin codigo ni
         * nombre en vez de reventar.
         */
        @Test
        @DisplayName("si el articulo ya no esta vivo, el DTO sale sin codigo en vez de fallar")
        void articulo_retirado_no_rompe_la_respuesta() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(vigente()));
            when(repository.existsPublishedText(anyLong(), anyString())).thenReturn(false);
            when(repository.findLastRevision(ARTICULO)).thenReturn(Optional.of(1));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
            when(catalogItemQueryPort.findById(ARTICULO)).thenReturn(Optional.empty());

            CatalogItemAiHintDto resultado = service.execute(command());

            assertThat(resultado.catalogItemCode()).isNull();
            assertThat(resultado.catalogItemName()).isNull();
            assertThat(resultado.hintText()).isEqualTo(TEXTO_V2);
        }
    }

    @Nested
    @DisplayName("Lo que no se escribe")
    class NoSeEscribe {

        @Test
        @DisplayName("sin pista vigente no hay nada que corregir: 404 y ninguna escritura")
        void sin_vigente_no_escribe_nada() {
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(CatalogItemAiHintNotFoundException.class);

            verify(repository, never()).supersede(any());
            verify(repository, never()).save(any());
        }

        /**
         * &#9940; La guarda del texto repetido corre <b>antes</b> de cerrar la vigente.
         * Si corriera despues, el articulo se quedaria sin pista vigente por una
         * peticion rechazada —y como la transaccion revierte, el sintoma seria
         * intermitente y no senalaria aqui.
         */
        @Test
        @DisplayName("un texto ya publicado sale 409 sin haber cerrado la vigente")
        void texto_repetido_no_cierra_la_vigente() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(vigente()));
            when(repository.existsPublishedText(ARTICULO, TEXTO_V2)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(CatalogItemAiHintTextAlreadyPublishedException.class);

            verify(repository, never()).supersede(any());
            verify(repository, never()).save(any());
        }
    }
}
