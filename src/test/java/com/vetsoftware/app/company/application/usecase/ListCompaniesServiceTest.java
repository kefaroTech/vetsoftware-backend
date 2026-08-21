package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCompaniesService")
class ListCompaniesServiceTest {

    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    private static final Long OTRA_COMPANY_ID = 77L;

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private ListCompaniesService service;

    /**
     * {@code companyId == null} solo lo produce un principal de plataforma
     * ({@code Authz.currentCompanyIdOrNull()} devuelve null para SYSTEM), y es el
     * unico camino al registro completo.
     */
    @Nested
    @DisplayName("sin empresa: el listado de plataforma")
    class SinEmpresa {

        @Test
        @DisplayName("devuelve el DTO de cada empresa del repositorio")
        void devuelve_el_dto_de_cada_empresa() {
            when(repository.findAllVisibleTo(null)).thenReturn(
                    List.of(CompanyMother.clinicaNorte(), CompanyMother.clinicaNorte(77L)));

            List<CompanyDto> dtos = service.listAll(null);

            assertThat(dtos).extracting(CompanyDto::id).containsExactly(CompanyMother.COMPANY_ID,
                    77L);
        }

        @Test
        @DisplayName("sin empresas registradas, devuelve lista vacia")
        void sin_empresas_devuelve_lista_vacia() {
            when(repository.findAllVisibleTo(null)).thenReturn(List.of());

            assertThat(service.listAll(null)).isEmpty();
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
    class ConEmpresa {

        @Test
        @DisplayName("la empresa que recibe es la que pide al repositorio, sin ensancharla a null")
        void pide_al_repositorio_exactamente_la_empresa_que_recibe() {
            when(repository.findAllVisibleTo(CompanyMother.COMPANY_ID))
                    .thenReturn(List.of(CompanyMother.clinicaNorte()));

            List<CompanyDto> dtos = service.listAll(CompanyMother.COMPANY_ID);

            assertThat(dtos).extracting(CompanyDto::id).containsExactly(CompanyMother.COMPANY_ID);
            verify(repository).findAllVisibleTo(CompanyMother.COMPANY_ID);
        }

        @Test
        @DisplayName("el alcance de otro tenant se propaga tal cual: no se mezclan empresas")
        void el_alcance_de_otro_tenant_se_propaga_tal_cual() {
            when(repository.findAllVisibleTo(OTRA_COMPANY_ID))
                    .thenReturn(List.of(CompanyMother.clinicaNorte(OTRA_COMPANY_ID)));

            assertThat(service.listAll(OTRA_COMPANY_ID)).extracting(CompanyDto::id)
                    .containsExactly(OTRA_COMPANY_ID);
        }

        /**
         * Es un listado, y un listado sin resultados no es un error: la empresa borrada
         * entre la autenticacion y esta lectura devuelve vacio, no 404.
         */
        @Test
        @DisplayName("una empresa que ya no existe devuelve lista vacia, no un error")
        void empresa_inexistente_devuelve_lista_vacia() {
            when(repository.findAllVisibleTo(CompanyMother.COMPANY_ID)).thenReturn(List.of());

            assertThat(service.listAll(CompanyMother.COMPANY_ID)).isEmpty();
        }
    }
}
