package com.vetsoftware.app.documentwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholdingNotFoundException;
import com.vetsoftware.app.documentwithholding.testsupport.DocumentWithholdingMother;
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
 * Los cinco caminos de lectura de la rodaja, reunidos porque comparten el mismo
 * unico colaborador y porque lo que hay que congelar de ellos es <b>exactamente
 * lo mismo</b>: que cada uno llama al metodo del repositorio que le corresponde
 * y no al de al lado.
 *
 * <p>
 * No es una comodidad. Los cinco delegan y mapean, asi que el fallo posible no
 * es de logica sino de cableado: que
 * {@code ListUncertifiedDocumentWithholdingsService} acabe llamando a
 * {@code findAll} y sirva la tabla entera en vez de la bandeja de
 * reclamaciones, o que la variante de tenant llame a la ancha y devuelva filas
 * de todos. Vistos juntos, esa confusion salta a la vista; repartidos en cinco
 * clases, no.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Lecturas de document withholdings — cada servicio llama a lo suyo")
class DocumentWithholdingQueryServicesTest {

    private static final int ANO = 2026;

    @Mock
    private DocumentWithholdingRepository repository;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("la consulta por id devuelve la retencion con su estado de respaldo")
        void la_consulta_por_id_devuelve_la_retencion() {
            when(repository.findByIdAndCompanyId(41L, DocumentWithholdingMother.EMPRESA))
                    .thenReturn(Optional.of(DocumentWithholdingMother.yaCertificada(41L, 8410L)));

            DocumentWithholdingDto devuelta = new FindDocumentWithholdingService(repository)
                    .findById(41L, DocumentWithholdingMother.EMPRESA);

            assertThat(devuelta.id()).isEqualTo(41L);
            assertThat(devuelta.certificateId()).isEqualTo(8410L);
            assertThat(devuelta.fiscalPeriodKey()).isEqualTo("2026-A");
        }

        @Test
        @DisplayName("el listado por empresa conserva los totales de la consulta")
        void el_listado_por_empresa_conserva_los_totales() {
            when(repository.findAllByCompanyId(DocumentWithholdingMother.EMPRESA, 2, 5))
                    .thenReturn(unaPagina(2, 5, 11L));

            PageResult<DocumentWithholdingDto> pagina = new ListDocumentWithholdingsService(
                    repository).listByCompany(DocumentWithholdingMother.EMPRESA, 2, 5);

            // Los totales son los de la consulta, no los del contenido mapeado: es el
            // error que produce el clasico "20 de 20" sobre cincuenta mil filas.
            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.page()).isEqualTo(2);
            assertThat(pagina.pageSize()).isEqualTo(5);
            assertThat(pagina.totalElements()).isEqualTo(11L);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una retencion inexistente sale como no encontrada")
        void una_retencion_inexistente_sale_como_no_encontrada() {
            when(repository.findByIdAndCompanyId(404L, DocumentWithholdingMother.EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> new FindDocumentWithholdingService(repository).findById(404L,
                    DocumentWithholdingMother.EMPRESA))
                    .isInstanceOf(DocumentWithholdingNotFoundException.class)
                    .hasMessageContaining("Document withholding not found: 404");
        }

        @Test
        @DisplayName("el barrido de vigilancia va por el finder de lo NO certificado")
        void el_barrido_de_vigilancia_va_por_el_finder_de_lo_no_certificado() {
            when(repository.findAllUncertifiedByFiscalYear(ANO, 0, 20))
                    .thenReturn(unaPagina(0, 20, 1L));

            new ListUncertifiedDocumentWithholdingsService(repository).listUncertified(ANO, 0, 20);

            // Si alguien lo cableara a findAll, la bandeja de reclamaciones mostraria
            // tambien lo ya certificado y nadie sabria que sobra.
            verify(repository).findAllUncertifiedByFiscalYear(ANO, 0, 20);
            verify(repository, never()).findAll(anyInt(), anyInt());
        }

        @Test
        @DisplayName("la vigilancia del cliente va por el finder acotado por empresa")
        void la_vigilancia_del_cliente_va_por_el_finder_acotado() {
            when(repository.findAllUncertifiedByCompanyIdAndFiscalYear(
                    DocumentWithholdingMother.EMPRESA, ANO, 0, 20))
                    .thenReturn(unaPagina(0, 20, 1L));

            new ListUncertifiedDocumentWithholdingsByCompanyService(repository)
                    .listUncertifiedByCompany(DocumentWithholdingMother.EMPRESA, ANO, 0, 20);

            verify(repository).findAllUncertifiedByCompanyIdAndFiscalYear(
                    DocumentWithholdingMother.EMPRESA, ANO, 0, 20);
            // Ni el barrido de plataforma ni el listado ancho: la empresa acota de
            // verdad y no solo en la firma.
            verify(repository, never()).findAllUncertifiedByFiscalYear(anyInt(), anyInt(),
                    anyInt());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el barrido de plataforma sin empresa recorre todas las clinicas")
        void el_barrido_sin_empresa_recorre_todas_las_clinicas() {
            when(repository.findAll(0, 20)).thenReturn(unaPagina(0, 20, 1L));

            new ListAllDocumentWithholdingsService(repository).listAll(null, 0, 20);

            verify(repository).findAll(0, 20);
            verify(repository, never()).findAllByCompanyId(anyLong(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("el barrido de plataforma con empresa acota, y no barre")
        void el_barrido_con_empresa_acota() {
            when(repository.findAllByCompanyId(DocumentWithholdingMother.OTRA_EMPRESA, 3, 7))
                    .thenReturn(unaPagina(3, 7, 0L));

            new ListAllDocumentWithholdingsService(repository)
                    .listAll(DocumentWithholdingMother.OTRA_EMPRESA, 3, 7);

            // El ternario legitimo del camino SYSTEM: con companyId acota, sin el
            // barre. Que llame a las dos variantes es lo que
            // CARGA_POR_ID_ACOTADA_POR_EMPRESA (BE-COV) exige de esta clase.
            verify(repository).findAllByCompanyId(DocumentWithholdingMother.OTRA_EMPRESA, 3, 7);
            verify(repository, never()).findAll(anyInt(), anyInt());
        }
    }

    // --- andamio ------------------------------------------------------------

    private static PageResult<DocumentWithholding> unaPagina(int page, int pageSize, long total) {
        List<DocumentWithholding> contenido = total == 0L
                ? List.of()
                : List.of(DocumentWithholdingMother.yaRegistrada(41L));
        return PageResult.of(contenido, page, pageSize, total);
    }
}
