package com.vetsoftware.app.companycontactchannel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelNotFoundException;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los tres caminos de lectura de la feature, juntos porque comparten el mismo
 * puerto y el mismo doble.
 *
 * <p>
 * <b>Lo que congelan</b> es que las lecturas <em>no</em> esconden los canales
 * revocados. Filtrarlos parece limpieza y es lo contrario: la bitacora existe
 * para responder si aquel aviso estaba permitido, y esa respuesta vive
 * justamente en las filas cerradas. La consulta que si los excluye es la
 * caliente, y es otra.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Lecturas de company contact channels")
class CompanyContactChannelQueryServicesTest {

    private static final Long ID = 8500L;
    private static final Long EMPRESA = CompanyContactChannelMother.COMPANY_ID;

    @Mock
    private CompanyContactChannelRepository repository;

    private FindCompanyContactChannelService findService;
    private ListUsableCompanyContactChannelsService listUsableService;
    private ListCompanyContactChannelsService listService;

    @BeforeEach
    void servicios() {
        findService = new FindCompanyContactChannelService(repository);
        listUsableService = new ListUsableCompanyContactChannelsService(repository);
        listService = new ListCompanyContactChannelsService(repository);
    }

    @Nested
    @DisplayName("Lectura por id")
    class LecturaPorId {

        @Test
        @DisplayName("devuelve el canal con cada campo en su sitio")
        void devuelve_el_canal_con_cada_campo_en_su_sitio() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.primario(ID)));

            CompanyContactChannelDto canal = findService.findById(ID, EMPRESA);

            assertThat(canal.id()).isEqualTo(ID);
            assertThat(canal.companyId()).isEqualTo(EMPRESA);
            assertThat(canal.address()).isEqualTo(CompanyContactChannelMother.CORREO);
            assertThat(canal.purpose()).isEqualTo(ContactPurpose.BILLING);
            assertThat(canal.primary()).isTrue();
        }

        @Test
        @DisplayName("un canal revocado tambien se lee: es la prueba, no un residuo")
        void un_canal_revocado_tambien_se_lee() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.revocado(ID)));

            CompanyContactChannelDto canal = findService.findById(ID, EMPRESA);

            assertThat(canal.revokedAt()).isEqualTo(CompanyContactChannelMother.REVOCADO_EL);
            assertThat(canal.revokedReason()).isEqualTo(CompanyContactChannelMother.MOTIVO);
        }

        @Test
        @DisplayName("un canal inexistente sale 404")
        void un_canal_inexistente_sale_404() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findService.findById(ID, EMPRESA))
                    .isInstanceOf(CompanyContactChannelNotFoundException.class)
                    .hasMessageContaining("8500");
        }

        @Test
        @DisplayName("el canal de otra empresa sale no encontrado, no prohibido")
        void el_canal_de_otra_empresa_sale_no_encontrado() {
            // Es la misma respuesta que para el inexistente, y a proposito: un 403
            // confirmaria que la fila existe, y con ids consecutivos eso es un censo de
            // por donde se le escribe a la competencia.
            when(repository.findByIdAndCompanyId(ID, CompanyContactChannelMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> findService.findById(ID, CompanyContactChannelMother.OTRA_COMPANY_ID))
                    .isInstanceOf(CompanyContactChannelNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Canales usables")
    class CanalesUsables {

        @ParameterizedTest
        @EnumSource(ContactPurpose.class)
        @DisplayName("pregunta por el proposito pedido, sin ampliarlo a los demas")
        void pregunta_por_el_proposito_pedido(ContactPurpose proposito) {
            // Autorizar un proposito no autoriza los demas: si este servicio ignorara el
            // parametro, la cobranza escribiria por un canal autorizado para otra cosa y
            // eso tiene sancion propia.
            when(repository.findAllUsableByCompanyIdAndPurpose(EMPRESA, proposito, 0, 20))
                    .thenReturn(PageResult.of(List.of(CompanyContactChannelMother.vivo(ID)), 0, 20,
                            1L));

            PageResult<CompanyContactChannelDto> pagina = listUsableService.listUsable(EMPRESA,
                    proposito, 0, 20);

            assertThat(pagina.content()).hasSize(1);
            verify(repository).findAllUsableByCompanyIdAndPurpose(EMPRESA, proposito, 0, 20);
        }

        @Test
        @DisplayName("los totales son los de la consulta, no los del contenido paginado")
        void los_totales_son_los_de_la_consulta() {
            // Recalcularlos sobre la pagina es como se acaba reportando 2 de 2 en un
            // listado de cuarenta.
            when(repository.findAllUsableByCompanyIdAndPurpose(EMPRESA, ContactPurpose.DUNNING, 0,
                    2))
                    .thenReturn(PageResult.of(List.of(CompanyContactChannelMother.vivo(ID),
                            CompanyContactChannelMother.vivo(8501L)), 0, 2, 40L));

            PageResult<CompanyContactChannelDto> pagina = listUsableService.listUsable(EMPRESA,
                    ContactPurpose.DUNNING, 0, 2);

            assertThat(pagina.content()).hasSize(2);
            assertThat(pagina.totalElements()).isEqualTo(40L);
            assertThat(pagina.totalPages()).isEqualTo(20);
            assertThat(pagina.pageSize()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Bitacora completa")
    class BitacoraCompleta {

        @Test
        @DisplayName("incluye los revocados junto a los vivos")
        void incluye_los_revocados_junto_a_los_vivos() {
            when(repository.findAllByCompanyId(EMPRESA, 0, 20))
                    .thenReturn(PageResult.of(List.of(CompanyContactChannelMother.vivo(ID),
                            CompanyContactChannelMother.revocado(8501L)), 0, 20, 2L));

            PageResult<CompanyContactChannelDto> pagina = listService.listByCompany(EMPRESA, 0, 20);

            assertThat(pagina.content()).extracting(CompanyContactChannelDto::revokedAt)
                    .containsExactly(null, CompanyContactChannelMother.REVOCADO_EL);
        }

        @Test
        @DisplayName("acota siempre por la empresa que se le pasa")
        void acota_siempre_por_la_empresa() {
            when(repository.findAllByCompanyId(CompanyContactChannelMother.OTRA_COMPANY_ID, 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            PageResult<CompanyContactChannelDto> pagina = listService
                    .listByCompany(CompanyContactChannelMother.OTRA_COMPANY_ID, 0, 20);

            assertThat(pagina.content()).isEmpty();
            verify(repository).findAllByCompanyId(CompanyContactChannelMother.OTRA_COMPANY_ID, 0,
                    20);
        }
    }
}
