package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
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
 * El listado de empresas, ya paginado (VUE-06).
 *
 * <p>
 * <b>Como se afirma aqui el alcance sin verificar consultas.</b> El puerto se
 * stubea con los tres argumentos exactos. Con {@code STRICT_STUBS}, llamarlo
 * con otros —otra empresa, otra pagina, otro tamaño— no devuelve el stub:
 * levanta {@code PotentialStubbingProblem} y el test falla. El stub <em>es</em>
 * la asercion sobre lo que se pidio; el valor devuelto es la asercion sobre lo
 * que se respondio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListCompaniesService — el registro de empresas, una pagina cada vez")
class ListCompaniesServiceTest {

    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    private static final Long OTRA_COMPANY_ID = 77L;

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private ListCompaniesService service;

    /** Pagina del adaptador: contenido de dominio y metadatos de la consulta. */
    private static PageResult<Company> pagina(List<Company> contenido, int page, int pageSize,
            long totalElements, int totalPages) {
        return new PageResult<>(contenido, page, pageSize, totalElements, totalPages);
    }

    /**
     * {@code companyId == null} solo lo produce un principal de plataforma
     * ({@code Authz.currentCompanyIdOrNull()} devuelve null para SYSTEM), y es el
     * unico camino al registro completo.
     */
    @Nested
    @DisplayName("sin empresa: el listado de plataforma")
    class SinEmpresa {

        @Test
        @DisplayName("devuelve el DTO de cada empresa de la pagina")
        void devuelve_el_dto_de_cada_empresa() {
            when(repository.findAllVisibleTo(null, 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte(),
                            CompanyMother.clinicaNorte(OTRA_COMPANY_ID)), 0, 20, 2L, 1));

            PageResult<CompanyDto> resultado = service.listAll(null, 0, 20);

            assertThat(resultado.content()).extracting(CompanyDto::id)
                    .containsExactly(CompanyMother.COMPANY_ID, OTRA_COMPANY_ID);
        }

        /**
         * El defecto que esta linea impide: recalcular los totales sobre el contenido
         * ya paginado es como se acaba reportando «20 de 20» en un registro de mil.
         * {@link PageResult#map} conserva los metadatos de la consulta intactos.
         */
        @Test
        @DisplayName("los totales son los de la consulta, no los de la pagina mapeada")
        void los_totales_son_los_de_la_consulta() {
            when(repository.findAllVisibleTo(null, 3, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 3, 20, 137L, 7));

            PageResult<CompanyDto> resultado = service.listAll(null, 3, 20);

            assertThat(resultado.content()).hasSize(1);
            assertThat(resultado.page()).isEqualTo(3);
            assertThat(resultado.pageSize()).isEqualTo(20);
            assertThat(resultado.totalElements()).isEqualTo(137L);
            assertThat(resultado.totalPages()).isEqualTo(7);
        }

        @Test
        @DisplayName("sin empresas registradas devuelve pagina vacia conservando la posicion pedida")
        void sin_empresas_devuelve_pagina_vacia() {
            when(repository.findAllVisibleTo(null, 2, 20)).thenReturn(PageResult.empty(2, 20));

            PageResult<CompanyDto> resultado = service.listAll(null, 2, 20);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.totalElements()).isZero();
        }
    }

    /**
     * El defecto que cierra esta prueba: {@code GET /companies} devolvia el
     * registro mercantil entero —nombre, NIT, direccion, telefono y plan contratado
     * de <b>todos</b> los tenants— a cualquier empleado con {@code company.read},
     * que es el permiso que necesita para ver la ficha de su propia veterinaria.
     * Con la empresa acotada, ese mismo empleado ve exactamente una fila: la suya.
     */
    @Nested
    @DisplayName("con empresa: el empleado solo ve la suya")
    class Tenancy {

        @Test
        @DisplayName("la empresa que recibe es la que pide al repositorio, sin ensancharla a null")
        void pide_al_repositorio_exactamente_la_empresa_que_recibe() {
            when(repository.findAllVisibleTo(CompanyMother.COMPANY_ID, 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 0, 20, 1L, 1));

            PageResult<CompanyDto> resultado = service.listAll(CompanyMother.COMPANY_ID, 0, 20);

            assertThat(resultado.content()).extracting(CompanyDto::id)
                    .containsExactly(CompanyMother.COMPANY_ID);
            // La rama ancha del puerto es la unica que lee el registro completo: que no
            // se toque nunca desde un actor con empresa es la mitad de la barrera.
            verify(repository, never()).findAllVisibleTo(isNull(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("el alcance de otro tenant se propaga tal cual: no se mezclan empresas")
        void el_alcance_de_otro_tenant_se_propaga_tal_cual() {
            when(repository.findAllVisibleTo(OTRA_COMPANY_ID, 0, 20)).thenReturn(
                    pagina(List.of(CompanyMother.clinicaNorte(OTRA_COMPANY_ID)), 0, 20, 1L, 1));

            assertThat(service.listAll(OTRA_COMPANY_ID, 0, 20).content()).extracting(CompanyDto::id)
                    .containsExactly(OTRA_COMPANY_ID);
        }

        /**
         * Es un listado, y un listado sin resultados no es un error: la empresa borrada
         * entre la autenticacion y esta lectura devuelve vacio, no 404.
         */
        @Test
        @DisplayName("una empresa que ya no existe devuelve pagina vacia, no un error")
        void empresa_inexistente_devuelve_pagina_vacia() {
            when(repository.findAllVisibleTo(CompanyMother.COMPANY_ID, 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.listAll(CompanyMother.COMPANY_ID, 0, 20).content()).isEmpty();
        }
    }

    /**
     * El caso de uso no interpreta la pagina: la traslada. Topar el tamaño y
     * normalizar el indice es trabajo de {@code Pages}, en el adaptador, y ahi se
     * prueba de verdad contra la base ({@code CompanyPersistenceIT}). Lo que se
     * fija aqui es que nadie meta una segunda normalizacion por el camino, que es
     * como acaban discrepando el tope declarado y el aplicado.
     */
    @Nested
    @DisplayName("la pagina viaja al puerto tal cual")
    class Paginacion {

        @ParameterizedTest(name = "page={0}, pageSize={1}")
        @DisplayName("el caso de uso no reescribe el indice ni el tamaño pedidos")
        @CsvSource({"0, 20", "5, 50", "0, 100000", "-1, 20", "3, 0"})
        void no_reescribe_el_indice_ni_el_tamano(int page, int pageSize) {
            when(repository.findAllVisibleTo(null, page, pageSize))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.listAll(null, page, pageSize).content()).isEmpty();
        }

        /**
         * El cliente pide 100000 y la respuesta dice 200: el tamaño efectivo lo fija el
         * servidor y baja desde el adaptador, no desde el query param.
         */
        @Test
        @DisplayName("el tamaño efectivo que sale es el que decidio el servidor, no el pedido")
        void el_tamano_efectivo_es_el_del_servidor() {
            when(repository.findAllVisibleTo(null, 0, 100_000))
                    .thenReturn(pagina(List.of(CompanyMother.clinicaNorte()), 0, 200, 1L, 1));

            assertThat(service.listAll(null, 0, 100_000).pageSize()).isEqualTo(200);
        }
    }
}
