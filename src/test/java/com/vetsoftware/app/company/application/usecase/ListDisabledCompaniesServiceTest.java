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
 * El ARCHIVO de empresas: el hermano de {@code ListCompaniesServiceTest} sobre
 * las filas que el {@code @SQLRestriction("enabled = true")} esconde.
 *
 * <p>
 * Mismo criterio de aserciones que su gemelo: el puerto se stubea con los tres
 * argumentos exactos y, con {@code STRICT_STUBS}, llamarlo con otros levanta
 * {@code PotentialStubbingProblem}. El stub <em>es</em> la asercion sobre lo
 * que se pidio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListDisabledCompaniesService — el archivo de empresas, una pagina cada vez")
class ListDisabledCompaniesServiceTest {

    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    private static final Long OTRA_COMPANY_ID = 77L;

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private ListDisabledCompaniesService service;

    private static PageResult<Company> pagina(List<Company> contenido, int page, int pageSize,
            long totalElements, int totalPages) {
        return new PageResult<>(contenido, page, pageSize, totalElements, totalPages);
    }

    @Nested
    @DisplayName("sin empresa: el archivo de plataforma")
    class SinEmpresa {

        /**
         * La razon de ser del caso de uso: estas filas no las devuelve ninguna otra
         * lectura de la rodaja, y sin ellas {@code PATCH /companies/{id}/enable} no
         * tiene forma de ser invocado desde la consola.
         */
        @Test
        @DisplayName("devuelve las archivadas con enabled=false en el DTO")
        void devuelve_las_archivadas_con_enabled_false() {
            when(repository.findAllDisabledVisibleTo(null, 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.deshabilitada()), 0, 20, 1L, 1));

            PageResult<CompanyDto> resultado = service.listDisabled(null, 0, 20);

            assertThat(resultado.content()).extracting(CompanyDto::id)
                    .containsExactly(CompanyMother.COMPANY_ID);
            // El distintivo «Deshabilitada» de la consola cuelga de este campo: si
            // saliera true, la pantalla pintaria el archivo como si estuviera activo.
            assertThat(resultado.content()).extracting(CompanyDto::enabled).containsExactly(false);
        }

        @Test
        @DisplayName("los totales son los de la consulta, no los de la pagina mapeada")
        void los_totales_son_los_de_la_consulta() {
            when(repository.findAllDisabledVisibleTo(null, 3, 20))
                    .thenReturn(pagina(List.of(CompanyMother.deshabilitada()), 3, 20, 137L, 7));

            PageResult<CompanyDto> resultado = service.listDisabled(null, 3, 20);

            assertThat(resultado.page()).isEqualTo(3);
            assertThat(resultado.pageSize()).isEqualTo(20);
            assertThat(resultado.totalElements()).isEqualTo(137L);
            assertThat(resultado.totalPages()).isEqualTo(7);
        }

        @Test
        @DisplayName("un archivo vacio es pagina vacia y conserva la posicion pedida, no un 404")
        void archivo_vacio_es_pagina_vacia() {
            when(repository.findAllDisabledVisibleTo(null, 2, 20))
                    .thenReturn(PageResult.empty(2, 20));

            PageResult<CompanyDto> resultado = service.listDisabled(null, 2, 20);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.totalElements()).isZero();
        }
    }

    /**
     * El SQL que sirve este caso de uso es NATIVO: se salta el
     * {@code @SQLRestriction} y con el cualquier otra red. El {@code companyId} que
     * llega aqui es lo unico que acaba en el {@code WHERE}, asi que ensancharlo a
     * {@code null} por el camino seria servir el archivo mercantil de todos los
     * tenants con un 200.
     */
    @Nested
    @DisplayName("con empresa: el empleado solo ve la suya")
    class Tenancy {

        @Test
        @DisplayName("la empresa que recibe es la que pide al puerto, sin ensancharla a null")
        void pide_al_puerto_exactamente_la_empresa_que_recibe() {
            when(repository.findAllDisabledVisibleTo(CompanyMother.COMPANY_ID, 0, 20))
                    .thenReturn(pagina(List.of(CompanyMother.deshabilitada()), 0, 20, 1L, 1));

            PageResult<CompanyDto> resultado = service.listDisabled(CompanyMother.COMPANY_ID, 0,
                    20);

            assertThat(resultado.content()).extracting(CompanyDto::id)
                    .containsExactly(CompanyMother.COMPANY_ID);
            verify(repository, never()).findAllDisabledVisibleTo(isNull(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("el alcance de otro tenant se propaga tal cual: no se mezclan empresas")
        void el_alcance_de_otro_tenant_se_propaga_tal_cual() {
            when(repository.findAllDisabledVisibleTo(OTRA_COMPANY_ID, 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.listDisabled(OTRA_COMPANY_ID, 0, 20).content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("la pagina viaja al puerto tal cual")
    class Paginacion {

        @ParameterizedTest(name = "page={0}, pageSize={1}")
        @DisplayName("el caso de uso no reescribe el indice ni el tamaño pedidos")
        @CsvSource({"0, 20", "5, 50", "0, 100000", "-1, 20", "3, 0"})
        void no_reescribe_el_indice_ni_el_tamano(int page, int pageSize) {
            when(repository.findAllDisabledVisibleTo(null, page, pageSize))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.listDisabled(null, page, pageSize).content()).isEmpty();
        }

        @Test
        @DisplayName("el tamaño efectivo que sale es el que decidio el servidor, no el pedido")
        void el_tamano_efectivo_es_el_del_servidor() {
            when(repository.findAllDisabledVisibleTo(null, 0, 100_000))
                    .thenReturn(pagina(List.of(CompanyMother.deshabilitada()), 0, 200, 1L, 1));

            assertThat(service.listDisabled(null, 0, 100_000).pageSize()).isEqualTo(200);
        }
    }
}
