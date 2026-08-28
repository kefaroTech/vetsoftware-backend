package com.vetsoftware.app.companycontactchannel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * El canal de contacto como pieza de prueba: que se puede demostrar con el y
 * que no.
 *
 * <p>
 * <b>Lo que esta clase congela y una revision humana no ve</b> son las dos
 * mitades del comportamiento de la revocacion. Al leer el nombre de la columna,
 * lo natural seria que revocar bajara tambien {@code is_primary} —«ya no es el
 * principal»— y que reintentar la revocacion fuera inofensivo. Las dos
 * intuiciones son falsas y las dos hacen dano: bajar el marcador borra la
 * constancia de cual era el canal principal mientras estuvo vivo, y reescribir
 * {@code revoked_at} mueve la frontera entre los avisos que estaban permitidos
 * y los que no, que es lo unico que esta tabla existe para demostrar.
 */
@DisplayName("CompanyContactChannel — la bitacora de por donde se puede escribir")
class CompanyContactChannelTest {

    private static final Long EMPRESA = CompanyContactChannelMother.COMPANY_ID;
    private static final String CORREO = CompanyContactChannelMother.CORREO;
    private static final String EVIDENCIA = CompanyContactChannelMother.EVIDENCIA;
    private static final LocalDateTime AUTORIZADO_EL = CompanyContactChannelMother.AUTORIZADO_EL;
    private static final LocalDateTime REVOCADO_EL = CompanyContactChannelMother.REVOCADO_EL;
    private static final String MOTIVO = CompanyContactChannelMother.MOTIVO;

    private static final String LARGO_161 = "a".repeat(161);
    private static final String LARGO_256 = "a".repeat(256);

    /**
     * Cada invariante del constructor es un caso, y el mensaje esperado nombra el
     * campo: sin eso, la matriz pasaria igual el dia que dos validaciones distintas
     * se fundieran en una.
     */
    static Stream<Arguments> invariantesRotas() {
        return Stream.of(
                Arguments.of("companyId is required",
                        (ThrowingCallable) () -> canal(null, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA, null, null)),
                Arguments.of("channelType is required",
                        (ThrowingCallable) () -> canal(EMPRESA, null, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA, null, null)),
                Arguments.of("address is required",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, "   ",
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA, null, null)),
                Arguments.of("address must be 160 chars or less",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, LARGO_161,
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA, null, null)),
                Arguments.of("purpose is required",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                null, AUTORIZADO_EL, EVIDENCIA, null, null)),
                Arguments.of("authorizedAt is required",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, null, EVIDENCIA, null, null)),
                Arguments.of("authorizationEvidence is required",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, " ", null, null)),
                Arguments.of("authorizationEvidence must be 255 chars or less",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, LARGO_256, null, null)),
                Arguments.of("revokedReason is required when revokedAt is set",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA, REVOCADO_EL,
                                null)),
                Arguments.of("revokedAt is required when revokedReason is set",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA, null, MOTIVO)),
                Arguments.of("revokedReason must be 255 chars or less",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA, REVOCADO_EL,
                                LARGO_256)),
                Arguments.of("revokedAt cannot be before authorizedAt",
                        (ThrowingCallable) () -> canal(EMPRESA, ContactChannelType.EMAIL, CORREO,
                                ContactPurpose.BILLING, AUTORIZADO_EL, EVIDENCIA,
                                AUTORIZADO_EL.minusDays(1), MOTIVO)));
    }

    private static CompanyContactChannel canal(Long companyId, ContactChannelType tipo,
            String address, ContactPurpose proposito, LocalDateTime autorizadoEl, String evidencia,
            LocalDateTime revocadoEl, String motivo) {
        return new CompanyContactChannel(1L, companyId, tipo, address, proposito, autorizadoEl,
                evidencia, revocadoEl, motivo, false, CompanyContactChannelMother.CREADO_EL, 0L);
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("nace vivo, sin revocar y NO primario")
        void nace_vivo_y_no_primario() {
            // El marcador no se pone en el alta a proposito: designar el principal es
            // una decision declarada, con su propio caso de uso y su propio permiso. Si
            // naciera primario, un alta rutinaria desviaria la facturacion de la empresa
            // sin que nadie lo lea como lo que es.
            CompanyContactChannel canal = CompanyContactChannelMother.nuevo();

            assertThat(canal.isPrimary()).isFalse();
            assertThat(canal.isUsable()).isTrue();
            assertThat(canal.isRevoked()).isFalse();
            assertThat(canal.getRevokedAt()).isNull();
            assertThat(canal.getRevokedReason()).isNull();
        }

        @Test
        @DisplayName("sella la fecha de autorizacion y la evidencia que la respalda")
        void sella_la_fecha_y_la_evidencia() {
            CompanyContactChannel canal = CompanyContactChannelMother.nuevo();

            assertThat(canal.getAuthorizedAt()).isEqualTo(AUTORIZADO_EL);
            assertThat(canal.getCreatedDate()).isEqualTo(AUTORIZADO_EL);
            assertThat(canal.getAuthorizationEvidence()).isEqualTo(EVIDENCIA);
            assertThat(canal.getAddress()).isEqualTo(CORREO);
        }

        @Test
        @DisplayName("un canal nuevo no trae id ni version: los pone la base")
        void un_canal_nuevo_no_trae_id_ni_version() {
            assertThat(CompanyContactChannelMother.nuevo().getId()).isNull();
            assertThat(CompanyContactChannelMother.nuevo().getVersion()).isNull();
        }

        @ParameterizedTest
        @EnumSource(ContactChannelType.class)
        @DisplayName("se puede autorizar cualquiera de los tipos que admite el CHECK")
        void se_puede_autorizar_cualquier_tipo(ContactChannelType tipo) {
            CompanyContactChannel canal = CompanyContactChannel.authorize(EMPRESA, tipo, CORREO,
                    ContactPurpose.BILLING, EVIDENCIA, AUTORIZADO_EL);

            assertThat(canal.getChannelType()).isEqualTo(tipo);
        }

        @ParameterizedTest
        @EnumSource(ContactPurpose.class)
        @DisplayName("autorizar un proposito no autoriza los demas: cada canal guarda el suyo")
        void cada_canal_guarda_su_proposito(ContactPurpose proposito) {
            CompanyContactChannel canal = CompanyContactChannel.authorize(EMPRESA,
                    ContactChannelType.EMAIL, CORREO, proposito, EVIDENCIA, AUTORIZADO_EL);

            assertThat(canal.getPurpose()).isEqualTo(proposito);
        }

        @Test
        @DisplayName("los nombres de los enums son espejo literal de los dos CHECK del esquema")
        void los_enums_son_espejo_de_los_check() {
            // @Enumerated(STRING) guarda el name() tal cual. Renombrar una constante
            // aqui compila, pasa los tests de dominio y revienta en el INSERT con un
            // error del motor que no menciona ni la columna ni el valor.
            assertThat(Arrays.stream(ContactChannelType.values()).map(Enum::name))
                    .containsExactly("EMAIL", "SMS", "WHATSAPP", "PHONE", "IN_APP");
            assertThat(Arrays.stream(ContactPurpose.values()).map(Enum::name))
                    .containsExactly("BILLING", "DUNNING", "OPERATIONAL", "MARKETING");
        }
    }

    @Nested
    @DisplayName("Revocacion")
    class Revocacion {

        @Test
        @DisplayName("escribe la fecha y el motivo, y el canal deja de ser usable")
        void escribe_la_fecha_y_el_motivo() {
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            canal.revoke(REVOCADO_EL, MOTIVO);

            assertThat(canal.getRevokedAt()).isEqualTo(REVOCADO_EL);
            assertThat(canal.getRevokedReason()).isEqualTo(MOTIVO);
            assertThat(canal.isRevoked()).isTrue();
            assertThat(canal.isUsable()).isFalse();
        }

        @Test
        @DisplayName("la fila se queda entera: revocar NO es un borrado ni logico ni fisico")
        void la_fila_se_queda_entera() {
            // Es la razon de ser de la tabla: hay que poder demostrar que el aviso de
            // marzo iba a una direccion autorizada en marzo. Si alguien convirtiera esto
            // en una baja, la empresa perderia la prueba justo cuando se la piden.
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            canal.revoke(REVOCADO_EL, MOTIVO);

            assertThat(canal.getId()).isEqualTo(8500L);
            assertThat(canal.getAddress()).isEqualTo(CORREO);
            assertThat(canal.getAuthorizedAt()).isEqualTo(AUTORIZADO_EL);
            assertThat(canal.getAuthorizationEvidence()).isEqualTo(EVIDENCIA);
        }

        @Test
        @DisplayName("revocar NO baja el marcador de primario: el hueco lo libera la columna generada")
        void revocar_no_baja_el_marcador_de_primario() {
            // primary_marker vale NULL en cuanto hay revoked_at, asi que el hueco de
            // uq_company_contact_channels_primary queda libre sin tocar is_primary. Y
            // conservarlo deja escrito cual era el canal principal mientras estuvo vivo,
            // que es lo que se le pregunta a una bitacora probatoria.
            CompanyContactChannel canal = CompanyContactChannelMother.primario(8500L);

            canal.revoke(REVOCADO_EL, MOTIVO);

            assertThat(canal.isPrimary()).isTrue();
            assertThat(canal.isUsable()).isFalse();
        }

        @Test
        @DisplayName("revocar dos veces se rechaza y NO reescribe la fecha original")
        void revocar_dos_veces_se_rechaza() {
            CompanyContactChannel canal = CompanyContactChannelMother.revocado(8500L);

            assertThatThrownBy(() -> canal.revoke(REVOCADO_EL.plusMonths(2), "otro motivo"))
                    .isInstanceOf(CompanyContactChannelAlreadyRevokedException.class)
                    .hasMessageContaining("8500");

            assertThat(canal.getRevokedAt()).isEqualTo(REVOCADO_EL);
            assertThat(canal.getRevokedReason()).isEqualTo(MOTIVO);
        }

        @Test
        @DisplayName("revocar sin motivo se rechaza: una baja sin por que no se puede auditar")
        void revocar_sin_motivo_se_rechaza() {
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            assertThatThrownBy(() -> canal.revoke(REVOCADO_EL, "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("revokedReason is required");

            assertThat(canal.isUsable()).isTrue();
        }

        @Test
        @DisplayName("revocar antes de autorizar se rechaza, igual que el CHECK del esquema")
        void revocar_antes_de_autorizar_se_rechaza() {
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            assertThatThrownBy(() -> canal.revoke(AUTORIZADO_EL.minusSeconds(1), MOTIVO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("revokedAt cannot be before authorizedAt");
        }

        @Test
        @DisplayName("revocar en el mismo instante de la autorizacion si vale: el CHECK usa >=")
        void revocar_en_el_mismo_instante_vale() {
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            canal.revoke(AUTORIZADO_EL, MOTIVO);

            assertThat(canal.getRevokedAt()).isEqualTo(AUTORIZADO_EL);
        }
    }

    @Nested
    @DisplayName("Primario")
    class Primario {

        @Test
        @DisplayName("designar marca el canal sin tocar su autorizacion")
        void designar_marca_el_canal() {
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            canal.designateAsPrimary();

            assertThat(canal.isPrimary()).isTrue();
            assertThat(canal.getAuthorizedAt()).isEqualTo(AUTORIZADO_EL);
            assertThat(canal.isUsable()).isTrue();
        }

        @Test
        @DisplayName("designar un canal revocado se rechaza, que es lo que el motor deja pasar")
        void designar_un_canal_revocado_se_rechaza() {
            // El UPDATE no violaria uq_company_contact_channels_primary: con revoked_at
            // puesta, primary_marker vale NULL y el hueco sigue libre. La empresa se
            // quedaria SIN primario creyendo que acaba de designarlo, y el aviso de
            // cobro saldria por el canal que no era.
            CompanyContactChannel canal = CompanyContactChannelMother.revocado(8500L);

            assertThatThrownBy(canal::designateAsPrimary)
                    .isInstanceOf(RevokedContactChannelCannotBePrimaryException.class)
                    .hasMessageContaining("8500");

            assertThat(canal.isPrimary()).isFalse();
        }

        @Test
        @DisplayName("liberar el marcador no revoca el canal: sigue autorizado")
        void liberar_el_marcador_no_revoca_el_canal() {
            CompanyContactChannel canal = CompanyContactChannelMother.primario(8500L);

            canal.releasePrimary();

            assertThat(canal.isPrimary()).isFalse();
            assertThat(canal.isUsable()).isTrue();
            assertThat(canal.getRevokedAt()).isNull();
        }

        @ParameterizedTest
        @EnumSource(ContactPurpose.class)
        @DisplayName("cada proposito tiene su propio primario: el marcador no depende del fin")
        void cada_proposito_tiene_su_propio_primario(ContactPurpose proposito) {
            // El indice unico del esquema es (primary_marker, purpose), no la generada
            // sola: un correo primario de facturacion y un movil primario de mora
            // conviven. Si alguien escribiera aqui una regla que dependa del proposito,
            // esta matriz la caza.
            CompanyContactChannel canal = CompanyContactChannelMother.canal(8500L, EMPRESA,
                    proposito, false, null, null);

            canal.designateAsPrimary();

            assertThat(canal.isPrimary()).isTrue();
            assertThat(canal.getPurpose()).isEqualTo(proposito);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelTest#invariantesRotas")
        @DisplayName("el constructor rechaza cada invariante rota, nombrando el campo")
        void el_constructor_rechaza_cada_invariante_rota(String mensaje,
                ThrowingCallable construccion) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("una direccion de exactamente 160 caracteres entra: el limite es el de la columna")
        void una_direccion_de_160_entra() {
            CompanyContactChannel canal = CompanyContactChannel.authorize(EMPRESA,
                    ContactChannelType.EMAIL, "a".repeat(160), ContactPurpose.BILLING, EVIDENCIA,
                    AUTORIZADO_EL);

            assertThat(canal.getAddress()).hasSize(160);
        }

        @Test
        @DisplayName("un canal vivo se construye con las dos columnas de revocacion nulas")
        void un_canal_vivo_lleva_las_dos_columnas_nulas() {
            // Es la otra mitad de la bicondicional de
            // chk_company_contact_channels_revocation, y la que no lanza: las dos nulas
            // es un estado legitimo y el mas comun.
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            assertThat(canal.getRevokedAt()).isNull();
            assertThat(canal.getRevokedReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un canal sin empresa no se puede construir")
        void un_canal_sin_empresa_no_se_puede_construir() {
            assertThatThrownBy(() -> CompanyContactChannel.authorize(null, ContactChannelType.EMAIL,
                    CORREO, ContactPurpose.BILLING, EVIDENCIA, AUTORIZADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("la empresa es inmutable: ni revocar ni designar la mueven")
        void la_empresa_es_inmutable() {
            // No hay setter ni metodo que la cambie, y eso es lo que impide la
            // apropiacion que describe BE-COV: reapuntar una fila propia a otra empresa
            // no es un rechazo, es un cambio de dueno que nadie ve.
            CompanyContactChannel canal = CompanyContactChannelMother.vivo(8500L);

            canal.designateAsPrimary();
            canal.revoke(REVOCADO_EL, MOTIVO);

            assertThat(canal.getCompanyId()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("dos canales de empresas distintas no comparten nada mas que la forma")
        void dos_canales_de_empresas_distintas() {
            assertThat(CompanyContactChannelMother.vivo(8500L).getCompanyId()).isEqualTo(EMPRESA);
            assertThat(CompanyContactChannelMother.deOtraEmpresa(8501L).getCompanyId())
                    .isEqualTo(CompanyContactChannelMother.OTRA_COMPANY_ID);
        }
    }
}
