package com.vetsoftware.app.companycontactchannel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companycontactchannel.application.command.DesignatePrimaryCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelNotFoundException;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.companycontactchannel.domain.RevokedContactChannelCannotBePrimaryException;
import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El relevo del canal principal.
 *
 * <p>
 * <b>Los dos casos que valen esta clase entera</b> son el proposito del
 * incumbente y el orden de las dos escrituras.
 *
 * <p>
 * El primero es el error que cometeria quien copie el patron de al lado: buscar
 * al primario <em>de la empresa</em> en vez de al primario <em>de ese
 * proposito</em>. Compilaria, pasaria una revision y dejaria a la clinica con
 * un unico canal principal en total, de modo que designar el correo de
 * facturacion bajaria el movil de mora sin que nadie se entere hasta que la
 * cobranza no salga.
 *
 * <p>
 * El segundo no se ve en ningun sitio salvo aqui: el indice unico se comprueba
 * sentencia a sentencia, asi que la bajada del incumbente tiene que llegar al
 * motor antes que la subida del sucesor. La rodaja de MySQL prueba que el
 * conjunto funciona; este caso prueba <em>por que</em>.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DesignatePrimaryCompanyContactChannelService — un primario por proposito")
class DesignatePrimaryCompanyContactChannelServiceTest {

    private static final Long ID = 8500L;
    private static final Long ID_INCUMBENTE = 8501L;
    private static final Long EMPRESA = CompanyContactChannelMother.COMPANY_ID;

    @Mock
    private CompanyContactChannelRepository repository;

    private DesignatePrimaryCompanyContactChannelService service;

    @BeforeEach
    void servicio() {
        service = new DesignatePrimaryCompanyContactChannelService(repository);
    }

    private static DesignatePrimaryCompanyContactChannelCommand comando() {
        return new DesignatePrimaryCompanyContactChannelCommand(ID, EMPRESA);
    }

    @Nested
    @DisplayName("Designacion")
    class Designacion {

        @Test
        @DisplayName("sin incumbente, sube el canal y escribe una sola vez")
        void sin_incumbente_sube_el_canal() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.vivo(ID)));
            when(repository.findPrimaryByCompanyIdAndPurpose(EMPRESA, ContactPurpose.BILLING))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto primario = service.execute(comando());

            assertThat(primario.primary()).isTrue();
            verify(repository, times(1)).save(any());
        }

        @Test
        @DisplayName("baja al incumbente ANTES de subir al sucesor")
        void baja_al_incumbente_antes_de_subir_al_sucesor() {
            // El orden no es cosmetico: uq_company_contact_channels_primary se comprueba
            // sentencia a sentencia y el incumbente sigue ocupando el hueco en la base
            // hasta que su UPDATE llega. Al reves, el relevo muere con un duplicado que
            // no menciona el relevo por ningun lado.
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.vivo(ID)));
            when(repository.findPrimaryByCompanyIdAndPurpose(EMPRESA, ContactPurpose.BILLING))
                    .thenReturn(Optional.of(CompanyContactChannelMother.primario(ID_INCUMBENTE)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando());

            ArgumentCaptor<CompanyContactChannel> guardados = ArgumentCaptor
                    .forClass(CompanyContactChannel.class);
            verify(repository, times(2)).save(guardados.capture());
            List<CompanyContactChannel> enOrden = guardados.getAllValues();
            assertThat(enOrden.get(0).getId()).isEqualTo(ID_INCUMBENTE);
            assertThat(enOrden.get(0).isPrimary()).isFalse();
            assertThat(enOrden.get(1).getId()).isEqualTo(ID);
            assertThat(enOrden.get(1).isPrimary()).isTrue();
        }

        @Test
        @DisplayName("el incumbente que baja sigue autorizado: no se revoca de paso")
        void el_incumbente_que_baja_sigue_autorizado() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.vivo(ID)));
            when(repository.findPrimaryByCompanyIdAndPurpose(EMPRESA, ContactPurpose.BILLING))
                    .thenReturn(Optional.of(CompanyContactChannelMother.primario(ID_INCUMBENTE)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando());

            ArgumentCaptor<CompanyContactChannel> guardados = ArgumentCaptor
                    .forClass(CompanyContactChannel.class);
            verify(repository, times(2)).save(guardados.capture());
            assertThat(guardados.getAllValues().get(0).isUsable()).isTrue();
            assertThat(guardados.getAllValues().get(0).getRevokedAt()).isNull();
        }

        @ParameterizedTest
        @EnumSource(ContactPurpose.class)
        @DisplayName("busca al incumbente del proposito del canal, nunca al de la empresa a secas")
        void busca_al_incumbente_del_proposito_del_canal(ContactPurpose proposito) {
            // ESTE es el caso que sostiene el modelo. Con el stub atado al proposito
            // exacto, un servicio que preguntara por otro —o por la empresa sin
            // proposito— se quedaria sin respuesta y la estrictez de Mockito lo delata.
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.of(
                    CompanyContactChannelMother.canal(ID, EMPRESA, proposito, false, null, null)));
            when(repository.findPrimaryByCompanyIdAndPurpose(EMPRESA, proposito))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto primario = service.execute(comando());

            assertThat(primario.purpose()).isEqualTo(proposito);
            assertThat(primario.primary()).isTrue();
            verify(repository).findPrimaryByCompanyIdAndPurpose(EMPRESA, proposito);
        }

        @Test
        @DisplayName("un canal que ya es primario no se vuelve a escribir")
        void un_canal_que_ya_es_primario_no_se_vuelve_a_escribir() {
            // Es idempotente a proposito: repetir la designacion desde una pantalla que
            // se quedo vieja no puede costar dos UPDATE sobre una fila versionada ni un
            // relevo consigo mismo.
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.primario(ID)));

            CompanyContactChannelDto primario = service.execute(comando());

            assertThat(primario.primary()).isTrue();
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("un canal inexistente sale 404 y no escribe")
        void un_canal_inexistente_no_escribe() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(CompanyContactChannelNotFoundException.class)
                    .hasMessageContaining("8500");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("designar un canal revocado sale 409 y no toca al incumbente")
        void designar_un_canal_revocado_no_toca_al_incumbente() {
            // El motor dejaria pasar el UPDATE —con revoked_at puesta, primary_marker es
            // NULL y el hueco sigue libre— y la empresa se quedaria SIN primario
            // creyendo que acaba de designarlo. Y lo peor: si el incumbente ya se
            // hubiera bajado, el relevo dejaria el proposito huerfano.
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.revocado(ID)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(RevokedContactChannelCannotBePrimaryException.class)
                    .hasMessageContaining("8500");

            verify(repository, never()).save(any());
            verify(repository, never()).findPrimaryByCompanyIdAndPurpose(any(), any());
        }

        @Test
        @DisplayName("un revocado que conserva el marcador tampoco se puede redesignar")
        void un_revocado_que_conserva_el_marcador_tampoco_se_redesigna() {
            // Es el estado real que deja la revocacion: is_primary sigue en TRUE. Si la
            // guarda de idempotencia mirara solo el marcador y no la revocacion, este
            // canal saldria por la salida rapida como si estuviera todo en orden.
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(
                    Optional.of(CompanyContactChannelMother.revocadoQueFuePrimario(ID)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(RevokedContactChannelCannotBePrimaryException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("carga el canal acotado por empresa y busca al incumbente en la misma")
        void carga_y_busca_en_la_misma_empresa() {
            // Si el incumbente se buscara sin empresa, el relevo bajaria el canal
            // principal de otra clinica: no es leer dato ajeno, es dejar a la vecina sin
            // canal de cobro.
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.vivo(ID)));
            when(repository.findPrimaryByCompanyIdAndPurpose(EMPRESA, ContactPurpose.BILLING))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando());

            verify(repository).findByIdAndCompanyId(ID, EMPRESA);
            verify(repository).findPrimaryByCompanyIdAndPurpose(EMPRESA, ContactPurpose.BILLING);
        }

        @Test
        @DisplayName("el canal de otra empresa sale no encontrado y no escribe")
        void el_canal_de_otra_empresa_sale_no_encontrado() {
            when(repository.findByIdAndCompanyId(ID, CompanyContactChannelMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new DesignatePrimaryCompanyContactChannelCommand(ID,
                            CompanyContactChannelMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(CompanyContactChannelNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }
}
