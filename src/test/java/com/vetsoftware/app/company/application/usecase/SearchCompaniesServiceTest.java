package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La busqueda de empresas nace con el listado paginado: una vez que
 * {@code GET /companies} deja de devolver el censo entero, encontrar una
 * empresa dejaba de ser posible filtrando en cliente.
 *
 * <p>
 * <b>El riesgo propio de esta clase.</b> Buscar es leer el mismo registro con
 * un {@code WHERE} mas. Si el alcance se cayera por el camino —o se sustituyera
 * por el termino en vez de sumarse a el— la busqueda seria el atajo para leer
 * exactamente lo que el listado niega. Por eso el nido {@code Tenancy} es el
 * grueso del fichero.
 *
 * <p>
 * Como en el listado, el puerto se stubea con los cuatro argumentos exactos:
 * bajo {@code STRICT_STUBS} una llamada con otro alcance u otro termino no
 * encuentra el stub y el test falla.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchCompaniesService — buscar no ensancha lo que el actor puede ver")
class SearchCompaniesServiceTest {

    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    private static final Long OTRA_COMPANY_ID = 77L;

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private SearchCompaniesService service;

    private static PageResult<Company> pagina(List<Company> contenido, int page, int pageSize,
            long totalElements, int totalPages) {
        return new PageResult<>(contenido, page, pageSize, totalElements, totalPages);
    }

    @Nested
    @DisplayName("sin empresa: la consola de plataforma busca en todo el registro")
    class SinEmpresa {

        @Test
        @DisplayName("devuelve el DTO de cada coincidencia")
        void devuelve_el_dto_de_cada_coincidencia() {
            when(repository.searchVisibleTo(null, "clinica", 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte(),
                            CompanyMother.clinicaNorte(OTRA_COMPANY_ID)), 0, 20, 2L, 1));

            PageResult<CompanyDto> resultado = service.search(null, "clinica", 0, 20);

            assertThat(resultado.content()).extracting(CompanyDto::id)
                    .containsExactly(CompanyMother.COMPANY_ID, OTRA_COMPANY_ID);
        }

        @Test
        @DisplayName("los totales son los de la consulta filtrada, no los de la pagina")
        void los_totales_son_los_de_la_consulta_filtrada() {
            when(repository.searchVisibleTo(null, "clinica", 1, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 1, 20, 41L, 3));

            PageResult<CompanyDto> resultado = service.search(null, "clinica", 1, 20);

            assertThat(resultado.page()).isEqualTo(1);
            assertThat(resultado.totalElements()).isEqualTo(41L);
            assertThat(resultado.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("un termino sin coincidencias devuelve pagina vacia, no un error")
        void termino_sin_coincidencias_devuelve_pagina_vacia() {
            when(repository.searchVisibleTo(null, "no-existe", 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.search(null, "no-existe", 0, 20).content()).isEmpty();
        }

        /**
         * Lo que espera un buscador cuando se borra lo escrito: el mismo listado, no un
         * fallo ni un cero.
         */
        @Test
        @DisplayName("un termino vacio se traslada tal cual y devuelve lo mismo que el listado")
        void termino_vacio_se_traslada_tal_cual() {
            when(repository.searchVisibleTo(null, "", 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 0, 20, 1L, 1));

            assertThat(service.search(null, "", 0, 20).content()).extracting(CompanyDto::id)
                    .containsExactly(CompanyMother.COMPANY_ID);
        }
    }

    /**
     * <b>El filtro de empresa se aplica ADEMAS del termino, nunca en su lugar.</b>
     * Los dos viajan juntos al puerto, y el que manda cuando compiten es el
     * alcance: un empleado que escribe el nombre de otra veterinaria recibe una
     * pagina vacia, no la ficha de esa veterinaria.
     */
    @Nested
    @DisplayName("con empresa: el termino filtra dentro del alcance, nunca lo sustituye")
    class Tenancy {

        @Test
        @DisplayName("el alcance y el termino llegan juntos al puerto, no uno en lugar del otro")
        void el_alcance_y_el_termino_llegan_juntos() {
            when(repository.searchVisibleTo(CompanyMother.COMPANY_ID, "clinica", 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 0, 20, 1L, 1));

            PageResult<CompanyDto> resultado = service.search(CompanyMother.COMPANY_ID, "clinica",
                    0, 20);

            assertThat(resultado.content()).extracting(CompanyDto::id)
                    .containsExactly(CompanyMother.COMPANY_ID);
            // La rama sin acotar del puerto —la unica que recorre el registro completo—
            // no se toca jamas desde un actor con empresa. Si el alcance se perdiera y
            // quedara solo el termino, esta linea es la que lo caza.
            verify(repository, never()).searchVisibleTo(isNull(), anyString(), anyInt(), anyInt());
        }

        /**
         * El escenario concreto del enunciado: un empleado de la Clinica Norte busca
         * «Clinica Sur». La fila existe en el registro, pero no en su alcance, asi que
         * lo que recibe es una pagina vacia con total cero.
         */
        @Test
        @DisplayName("buscar el nombre de otra veterinaria devuelve pagina vacia")
        void buscar_otra_veterinaria_devuelve_pagina_vacia() {
            when(repository.searchVisibleTo(CompanyMother.COMPANY_ID, "Clinica Sur", 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            PageResult<CompanyDto> resultado = service.search(CompanyMother.COMPANY_ID,
                    "Clinica Sur", 0, 20);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.totalElements()).isZero();
        }

        @Test
        @DisplayName("el alcance de otro tenant se propaga tal cual: no se mezclan empresas")
        void el_alcance_de_otro_tenant_se_propaga_tal_cual() {
            when(repository.searchVisibleTo(OTRA_COMPANY_ID, "clinica", 0, 20)).thenReturn(
                    pagina(List.of(CompanyMother.clinicaNorte(OTRA_COMPANY_ID)), 0, 20, 1L, 1));

            assertThat(service.search(OTRA_COMPANY_ID, "clinica", 0, 20).content())
                    .extracting(CompanyDto::id).containsExactly(OTRA_COMPANY_ID);
        }

        /**
         * Ni siquiera el termino vacio abre la puerta: sin {@code WHERE} util, lo unico
         * que queda filtrando es el alcance, y sigue siendo una sola fila.
         */
        @Test
        @DisplayName("un termino vacio tampoco ensancha el alcance del empleado")
        void termino_vacio_no_ensancha_el_alcance() {
            when(repository.searchVisibleTo(CompanyMother.COMPANY_ID, "", 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 0, 20, 1L, 1));

            assertThat(service.search(CompanyMother.COMPANY_ID, "", 0, 20).content())
                    .extracting(CompanyDto::id).containsExactly(CompanyMother.COMPANY_ID);
            verify(repository, never()).searchVisibleTo(isNull(), anyString(), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("la pagina viaja al puerto tal cual")
    class Paginacion {

        @ParameterizedTest(name = "page={0}, pageSize={1}")
        @DisplayName("el caso de uso no reescribe el indice ni el tamaño pedidos")
        @CsvSource({"0, 20", "5, 50", "0, 100000", "-1, 20", "3, 0"})
        void no_reescribe_el_indice_ni_el_tamano(int page, int pageSize) {
            when(repository.searchVisibleTo(null, "clinica", page, pageSize))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.search(null, "clinica", page, pageSize).content()).isEmpty();
        }

        @Test
        @DisplayName("el tamaño efectivo que sale es el que decidio el servidor, no el pedido")
        void el_tamano_efectivo_es_el_del_servidor() {
            when(repository.searchVisibleTo(null, "clinica", 0, 100_000))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 0, 200, 1L, 1));

            assertThat(service.search(null, "clinica", 0, 100_000).pageSize()).isEqualTo(200);
        }
    }
}
