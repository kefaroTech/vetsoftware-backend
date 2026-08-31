package com.vetsoftware.app.catalogitemaihint.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintNotFoundException;
import com.vetsoftware.app.catalogitemaihint.testsupport.CatalogItemAiHintMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Escrito desde el Javadoc de {@code FindCurrentCatalogItemAiHintUseCase} y
 * de {@code CatalogItemAiHintDto}, no desde el cuerpo del servicio.</b> Lo que
 * se afirma son sus dos promesas: que un articulo sin pista es un 404 y no una
 * respuesta vacia —«no tiene» y «no la encuentro» son el mismo estado, y el
 * front necesita distinguirlo de «tiene una y viene sin texto»—, y que el
 * codigo y el nombre del articulo son opcionales, porque la pista de un
 * articulo retirado sigue siendo una fila legitima que hay que poder leer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindCurrentCatalogItemAiHintService — la pista que rige hoy, o 404")
class FindCurrentCatalogItemAiHintServiceTest {

    private static final Long ARTICULO = CatalogItemAiHintMother.ARTICULO_ID;

    @Mock
    private CatalogItemAiHintRepository repository;

    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    @InjectMocks
    private FindCurrentCatalogItemAiHintService service;

    @Nested
    @DisplayName("Cuando el articulo tiene pista")
    class ConPista {

        @Test
        @DisplayName("sirve la revision vigente entera, con el codigo y el nombre resueltos")
        void sirve_la_vigente_con_el_articulo_resuelto() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.vigente(7003L, ARTICULO, 3,
                            CatalogItemAiHintMother.texto(3))));
            when(catalogItemQueryPort.findById(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.ref()));

            CatalogItemAiHintDto dto = service.findCurrentByCatalogItemId(ARTICULO);

            assertThat(dto.id()).isEqualTo(7003L);
            assertThat(dto.catalogItemId()).isEqualTo(ARTICULO);
            assertThat(dto.catalogItemCode()).isEqualTo("GROOMING");
            assertThat(dto.catalogItemName()).isEqualTo("Estetica");
            assertThat(dto.hintRevision()).isEqualTo(3);
            assertThat(dto.hintText()).isEqualTo(CatalogItemAiHintMother.texto(3));
            assertThat(dto.publishedAt()).isEqualTo(CatalogItemAiHintMother.PUBLICADA_EN);
            assertThat(dto.publishedBySystemUserId())
                    .isEqualTo(CatalogItemAiHintMother.FIRMANTE_ID);
            assertThat(dto.supersededAt()).isNull();
            assertThat(dto.current()).isTrue();
        }

        /**
         * El DTO documenta que {@code catalogItemCode} y {@code catalogItemName} pueden
         * venir nulos: el puerto solo resuelve articulos vivos, y esconder la pista de
         * uno retirado seria peor que servirla sin nombre —es justo la pantalla desde
         * la que se corrige—.
         */
        @Test
        @DisplayName("un articulo que ya no esta vivo se sirve sin codigo, no con un error")
        void articulo_no_vivo_sale_sin_codigo() {
            when(repository.findCurrentByCatalogItemId(ARTICULO))
                    .thenReturn(Optional.of(CatalogItemAiHintMother.vigente(7001L, ARTICULO, 1,
                            CatalogItemAiHintMother.TRES_PARTES)));
            when(catalogItemQueryPort.findById(ARTICULO)).thenReturn(Optional.empty());

            CatalogItemAiHintDto dto = service.findCurrentByCatalogItemId(ARTICULO);

            assertThat(dto.catalogItemCode()).isNull();
            assertThat(dto.catalogItemName()).isNull();
            assertThat(dto.catalogItemId()).isEqualTo(ARTICULO);
            assertThat(dto.hintText()).isEqualTo(CatalogItemAiHintMother.TRES_PARTES);
            assertThat(dto.current()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cuando no la tiene")
    class SinPista {

        /**
         * Un articulo al que nadie le publico pista, o al que se la retiraron, es un
         * 404. Devolver un DTO vacio dejaria al front sin forma de distinguir «este
         * articulo no tiene instrucciones» de «las tiene y llegaron en blanco», que son
         * dos incidencias distintas con dos arreglos distintos.
         */
        @Test
        @DisplayName("un articulo sin pista vigente es 404, no una respuesta vacia")
        void sin_pista_vigente_es_404() {
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findCurrentByCatalogItemId(ARTICULO))
                    .isInstanceOf(CatalogItemAiHintNotFoundException.class)
                    .hasMessageContaining("has no current AI hint");
        }

        /**
         * Y no paga por preguntarlo: resolver el articulo para una respuesta que no se
         * va a servir es una consulta contra {@code catalog_items} por cada 404.
         */
        @Test
        @DisplayName("no resuelve el articulo para un 404")
        void el_404_no_consulta_el_catalogo() {
            when(repository.findCurrentByCatalogItemId(ARTICULO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findCurrentByCatalogItemId(ARTICULO))
                    .isInstanceOf(CatalogItemAiHintNotFoundException.class);

            verifyNoInteractions(catalogItemQueryPort);
        }
    }
}
