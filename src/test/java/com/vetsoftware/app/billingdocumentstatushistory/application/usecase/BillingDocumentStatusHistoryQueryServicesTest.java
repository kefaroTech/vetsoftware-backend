package com.vetsoftware.app.billingdocumentstatushistory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistoryNotFoundException;
import com.vetsoftware.app.billingdocumentstatushistory.testsupport.BillingDocumentStatusHistoryMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los cuatro caminos de lectura de la rodaja, reunidos porque comparten el
 * mismo unico colaborador y porque lo que hay que congelar de ellos es
 * <b>exactamente lo mismo</b>: que cada uno llama al metodo del repositorio que
 * le corresponde y no al de al lado.
 *
 * <p>
 * No es una comodidad. Los cuatro delegan y mapean, asi que el fallo posible no
 * es de logica sino de cableado: que la bandeja por estado acabe llamando a
 * {@code findAll} y sirva la historia de todos los tenants en vez de la de la
 * empresa, o que el barrido de plataforma llame a la acotada y le esconda a
 * tesoreria la mitad del mapa. Vistos juntos, esa confusion salta a la vista;
 * repartidos en cuatro clases, no.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Lecturas de la pelicula del documento de cobro")
class BillingDocumentStatusHistoryQueryServicesTest {

    private static final Long EMPRESA = BillingDocumentStatusHistoryMother.EMPRESA;
    private static final Long OTRA_EMPRESA = BillingDocumentStatusHistoryMother.OTRA_EMPRESA;
    private static final Long DOCUMENTO = BillingDocumentStatusHistoryMother.DOCUMENTO;

    @Mock
    private BillingDocumentStatusHistoryRepository repository;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("devuelve el fotograma pedido con su par de estados")
        void devuelve_el_fotograma_pedido() {
            when(repository.findByIdAndCompanyId(41L, EMPRESA))
                    .thenReturn(Optional.of(BillingDocumentStatusHistoryMother.yaRegistrado(41L)));

            BillingDocumentStatusHistoryDto encontrado = find().findById(41L, EMPRESA);

            assertThat(encontrado.id()).isEqualTo(41L);
            assertThat(encontrado.fromStatus()).isEqualTo(BillingDocumentStatus.DRAFT);
            assertThat(encontrado.toStatus()).isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL);
            assertThat(encontrado.actor())
                    .isEqualTo(BillingDocumentStatusHistoryMother.ACTOR_PERSONA);
        }

        @Test
        @DisplayName("la pelicula de un documento sale con sus fotogramas y sus totales")
        void la_pelicula_de_un_documento_sale_con_sus_totales() {
            when(repository.findAllByCompanyIdAndBillingDocumentId(EMPRESA, DOCUMENTO, 0, 20))
                    .thenReturn(pagina(BillingDocumentStatusHistoryMother.haciaEsperaExterna(),
                            BillingDocumentStatusHistoryMother.haciaRegistroExterno()));

            PageResult<BillingDocumentStatusHistoryDto> pelicula = porDocumento()
                    .listByDocument(EMPRESA, DOCUMENTO, 0, 20);

            assertThat(pelicula.content()).extracting(BillingDocumentStatusHistoryDto::toStatus)
                    .containsExactly(BillingDocumentStatus.AWAITING_EXTERNAL,
                            BillingDocumentStatus.EXTERNAL_REGISTERED);
            // Los totales son los de la consulta y no se recalculan sobre el contenido
            // ya paginado: son dos numeros distintos en cuanto hay mas de una pagina.
            assertThat(pelicula.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("la bandeja por estado pregunta por el estado que le pidieron")
        void la_bandeja_por_estado_pregunta_por_el_estado_pedido() {
            when(repository.findAllByCompanyIdAndToStatus(EMPRESA,
                    BillingDocumentStatus.AWAITING_EXTERNAL, 0, 20))
                    .thenReturn(pagina(BillingDocumentStatusHistoryMother.haciaEsperaExterna()));

            PageResult<BillingDocumentStatusHistoryDto> bandeja = porEstado()
                    .listByCompanyAndToStatus(EMPRESA, BillingDocumentStatus.AWAITING_EXTERNAL, 0,
                            20);

            assertThat(bandeja.content()).singleElement()
                    .satisfies(fila -> assertThat(fila.toStatus())
                            .isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL));
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un fotograma inexistente sale como no encontrado")
        void un_fotograma_inexistente_sale_como_no_encontrado() {
            when(repository.findByIdAndCompanyId(41L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> find().findById(41L, EMPRESA))
                    .isInstanceOf(BillingDocumentStatusHistoryNotFoundException.class)
                    .hasMessageContaining("41");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la carga por id usa la variante acotada por empresa y no existe otra")
        void la_carga_por_id_usa_la_variante_acotada() {
            // BE-COV, CARGA_POR_ID_ACOTADA_POR_EMPRESA: el puerto de salida no declara
            // findById(Long) ancho, asi que el servicio no puede equivocarse. Este caso
            // congela que la llamada lleva las dos columnas.
            when(repository.findByIdAndCompanyId(41L, EMPRESA))
                    .thenReturn(Optional.of(BillingDocumentStatusHistoryMother.yaRegistrado(41L)));

            find().findById(41L, EMPRESA);

            verify(repository).findByIdAndCompanyId(41L, EMPRESA);
        }

        @Test
        @DisplayName("el fotograma de otra empresa sale como no encontrado, no como prohibido")
        void el_fotograma_de_otra_empresa_sale_como_no_encontrado() {
            // Un 403 confirmaria que la fila existe, y con ids consecutivos eso es un
            // censo de los movimientos de cartera de la competencia.
            when(repository.findByIdAndCompanyId(41L, OTRA_EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> find().findById(41L, OTRA_EMPRESA))
                    .isInstanceOf(BillingDocumentStatusHistoryNotFoundException.class);
        }

        @Test
        @DisplayName("el barrido de plataforma con empresa acota y no toca la consulta ancha")
        void el_barrido_de_plataforma_con_empresa_acota() {
            when(repository.findAllByCompanyId(EMPRESA, 0, 20))
                    .thenReturn(pagina(BillingDocumentStatusHistoryMother.haciaEsperaExterna()));

            todos().listAll(EMPRESA, 0, 20);

            verify(repository).findAllByCompanyId(EMPRESA, 0, 20);
            verify(repository, never()).findAll(0, 20);
        }

        @Test
        @DisplayName("el barrido de plataforma sin empresa si usa la consulta cross-tenant")
        void el_barrido_de_plataforma_sin_empresa_usa_la_consulta_ancha() {
            // La rama ancha existe solo aqui, y solo la alcanza un puerto cerrado a
            // hasRole('SYSTEM') a secas: es la unica consulta de la feature que
            // devuelve filas de varias empresas.
            when(repository.findAll(0, 20))
                    .thenReturn(pagina(BillingDocumentStatusHistoryMother.haciaEsperaExterna()));

            todos().listAll(null, 0, 20);

            verify(repository).findAll(0, 20);
        }

        @Test
        @DisplayName("un listado por documento nunca pregunta sin la empresa delante")
        void un_listado_por_documento_nunca_pregunta_sin_la_empresa() {
            // BE-29: acotar por la FK ajena no cuenta como filtro de tenant. El puerto
            // de salida no ofrece un findAllByBillingDocumentId, y el
            // verifyNoMoreInteractions deja escrito que el servicio no se busca otra
            // via.
            when(repository.findAllByCompanyIdAndBillingDocumentId(EMPRESA, DOCUMENTO, 0, 20))
                    .thenReturn(pagina(BillingDocumentStatusHistoryMother.haciaEsperaExterna()));

            porDocumento().listByDocument(EMPRESA, DOCUMENTO, 0, 20);

            verify(repository).findAllByCompanyIdAndBillingDocumentId(EMPRESA, DOCUMENTO, 0, 20);
            verifyNoMoreInteractions(repository);
        }
    }

    // --- andamio ------------------------------------------------------------

    private FindBillingDocumentStatusHistoryService find() {
        return new FindBillingDocumentStatusHistoryService(repository);
    }

    private ListBillingDocumentStatusHistoryService porDocumento() {
        return new ListBillingDocumentStatusHistoryService(repository);
    }

    private ListBillingDocumentStatusChangesByStatusService porEstado() {
        return new ListBillingDocumentStatusChangesByStatusService(repository);
    }

    private ListAllBillingDocumentStatusHistoryService todos() {
        return new ListAllBillingDocumentStatusHistoryService(repository);
    }

    private static PageResult<BillingDocumentStatusHistory> pagina(
            BillingDocumentStatusHistory... filas) {
        List<BillingDocumentStatusHistory> contenido = List.of(filas);
        return new PageResult<>(contenido, 0, 20, contenido.size(), 1);
    }
}
