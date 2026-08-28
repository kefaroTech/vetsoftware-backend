package com.vetsoftware.app.companybillingprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfileNotFoundException;
import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los tres servicios de lectura de la feature.
 *
 * <p>
 * <b>Van juntos porque comparten exactamente un doble y ninguna
 * orquestacion</b> —los tres son una consulta acotada por empresa y una
 * traduccion a DTO—, y repartirlos en tres clases seria triplicar el andamio
 * sin añadir ni un escenario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Lecturas de la ficha de facturacion")
class CompanyBillingProfileQueryServicesTest {

    @Mock
    private CompanyBillingProfileRepository repository;

    @InjectMocks
    private FindCurrentCompanyBillingProfileService findCurrentService;
    @InjectMocks
    private FindCompanyBillingProfileService findService;
    @InjectMocks
    private ListCompanyBillingProfilesService listService;

    @Nested
    @DisplayName("La ficha vigente")
    class LaFichaVigente {

        @Test
        @DisplayName("devuelve la que rige hoy, con su municipio y sin fecha de cierre")
        void devuelve_la_que_rige_hoy() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.persistida(42L)));

            CompanyBillingProfileDto dto = findCurrentService
                    .findCurrent(CompanyBillingProfileMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(42L);
            assertThat(dto.validTo()).isNull();
            assertThat(dto.city().name()).isEqualTo("Medellin");
        }

        @Test
        @DisplayName("una empresa sin ficha contesta 404 y no una ficha vacia")
        void una_empresa_sin_ficha_contesta_404() {
            // El front tiene que poder distinguir «no hay a quien facturar» de «hay ficha
            // con campos en blanco» para ofrecer «abrir ficha».
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> findCurrentService.findCurrent(CompanyBillingProfileMother.COMPANY_ID))
                    .isInstanceOf(CompanyBillingProfileNotFoundException.class)
                    .hasMessageContaining("has no current billing profile");
        }
    }

    @Nested
    @DisplayName("Una ficha del historico")
    class UnaFichaDelHistorico {

        @Test
        @DisplayName("devuelve la ficha cerrada con su fecha de fin: es la que nombra una factura vieja")
        void devuelve_la_ficha_cerrada_con_su_fecha_de_fin() {
            when(repository.findByIdAndCompanyId(7L, CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.persistida(7L,
                            CompanyBillingProfileMother.COMPANY_ID,
                            CompanyBillingProfileMother.RIGE_DESDE,
                            CompanyBillingProfileMother.SUCEDE_DESDE)));

            CompanyBillingProfileDto dto = findService.findById(7L,
                    CompanyBillingProfileMother.COMPANY_ID);

            assertThat(dto.validFrom()).isEqualTo(CompanyBillingProfileMother.RIGE_DESDE);
            assertThat(dto.validTo()).isEqualTo(CompanyBillingProfileMother.SUCEDE_DESDE);
        }

        @Test
        @DisplayName("una ficha inexistente contesta 404")
        void una_ficha_inexistente_contesta_404() {
            when(repository.findByIdAndCompanyId(99L, CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> findService.findById(99L, CompanyBillingProfileMother.COMPANY_ID))
                    .isInstanceOf(CompanyBillingProfileNotFoundException.class)
                    .hasMessageContaining("Company billing profile not found: 99");
        }
    }

    @Nested
    @DisplayName("El historico")
    class ElHistorico {

        @Test
        @DisplayName("traduce el contenido y conserva los totales de la consulta")
        void traduce_el_contenido_y_conserva_los_totales() {
            // Los totales son los de la consulta, no los del contenido ya paginado:
            // recalcularlos sobre la pagina es como se acaba reportando «2 de 2» en un
            // historico de treinta fichas.
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(42L);
            CompanyBillingProfile anterior = CompanyBillingProfileMother.persistida(41L,
                    CompanyBillingProfileMother.COMPANY_ID,
                    CompanyBillingProfileMother.RIGE_DESDE.minusYears(1),
                    CompanyBillingProfileMother.RIGE_DESDE);
            when(repository.findAllByCompanyId(CompanyBillingProfileMother.COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(vigente, anterior), 0, 20, 30L, 2));

            PageResult<CompanyBillingProfileDto> pagina = listService
                    .listByCompany(CompanyBillingProfileMother.COMPANY_ID, 0, 20);

            assertThat(pagina.content()).extracting(CompanyBillingProfileDto::id)
                    .containsExactly(42L, 41L);
            assertThat(pagina.totalElements()).isEqualTo(30L);
            assertThat(pagina.totalPages()).isEqualTo(2);
            assertThat(pagina.pageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("la consulta va acotada por la empresa recibida")
        void la_consulta_va_acotada_por_la_empresa() {
            // No existe una variante ancha en el puerto de salida: un listado sin empresa
            // publicaria el NIT y la direccion de facturacion de cada clinica de la
            // plataforma.
            when(repository.findAllByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID, 1, 5))
                    .thenReturn(PageResult.empty(1, 5));

            listService.listByCompany(CompanyBillingProfileMother.OTRA_COMPANY_ID, 1, 5);

            verify(repository).findAllByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID, 1,
                    5);
        }
    }
}
