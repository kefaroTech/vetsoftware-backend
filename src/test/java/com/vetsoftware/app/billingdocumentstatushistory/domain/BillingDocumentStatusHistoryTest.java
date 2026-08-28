package com.vetsoftware.app.billingdocumentstatushistory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.billingdocumentstatushistory.testsupport.BillingDocumentStatusHistoryMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("BillingDocumentStatusHistory — el fotograma de la pelicula del documento")
class BillingDocumentStatusHistoryTest {

    private static final Long EMPRESA = BillingDocumentStatusHistoryMother.EMPRESA;
    private static final Long DOCUMENTO = BillingDocumentStatusHistoryMother.DOCUMENTO;
    private static final LocalDateTime OCURRIO_EL = BillingDocumentStatusHistoryMother.OCURRIO_EL;
    private static final LocalDateTime CREADO_EL = BillingDocumentStatusHistoryMother.CREADO_EL;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("un cambio recien registrado nace sin id y con cada campo en su sitio")
        void un_cambio_recien_registrado_nace_sin_id() {
            BillingDocumentStatusHistory fotograma = BillingDocumentStatusHistoryMother
                    .haciaEsperaExterna();

            assertThat(fotograma.getId()).isNull();
            assertThat(fotograma.getCompanyId()).isEqualTo(EMPRESA);
            assertThat(fotograma.getBillingDocumentId()).isEqualTo(DOCUMENTO);
            assertThat(fotograma.getFromStatus()).isEqualTo(BillingDocumentStatus.DRAFT);
            assertThat(fotograma.getToStatus()).isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL);
            assertThat(fotograma.getActor())
                    .isEqualTo(BillingDocumentStatusHistoryMother.ACTOR_PERSONA);
            assertThat(fotograma.getReason()).isEqualTo(BillingDocumentStatusHistoryMother.MOTIVO);
        }

        @Test
        @DisplayName("el momento del cambio y el de escritura son campos distintos y no se cruzan")
        void el_momento_del_cambio_y_el_de_escritura_no_se_cruzan() {
            // Van seguidos en el constructor y son del mismo tipo: cruzarlos compila.
            // Con instantes distintos, el cruce se ve; con el mismo valor en los dos,
            // este caso pasaria en verde con el mapper roto.
            BillingDocumentStatusHistory fotograma = BillingDocumentStatusHistoryMother
                    .haciaEsperaExterna();

            assertThat(fotograma.getOccurredAt()).isEqualTo(OCURRIO_EL);
            assertThat(fotograma.getCreatedDate()).isEqualTo(CREADO_EL);
            assertThat(fotograma.getOccurredAt()).isNotEqualTo(fotograma.getCreatedDate());
        }

        @Test
        @DisplayName("reconoce el fotograma que deja el documento esperando factura externa")
        void reconoce_el_fotograma_que_deja_esperando_factura_externa() {
            assertThat(BillingDocumentStatusHistoryMother.haciaEsperaExterna()
                    .leavesAwaitingExternal()).isTrue();
            assertThat(BillingDocumentStatusHistoryMother.haciaRegistroExterno()
                    .leavesAwaitingExternal()).isFalse();
        }

        @Test
        @DisplayName("no ofrece ninguna forma de editar ni de desactivar el fotograma")
        void no_ofrece_ninguna_forma_de_editar_el_fotograma() {
            // La tabla solo se agrega, y la propiedad se congela aqui: si alguien
            // anadiera un setter, un update o un enabled a la entidad, este caso se
            // pone rojo y obliga a la conversacion antes que al commit.
            assertThat(BillingDocumentStatusHistory.class.getMethods())
                    .extracting(java.lang.reflect.Method::getName)
                    .noneMatch(nombre -> nombre.startsWith("set") || nombre.startsWith("update")
                            || nombre.startsWith("delete") || nombre.startsWith("disable"));
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @ParameterizedTest
        @EnumSource(BillingDocumentStatus.class)
        @DisplayName("una transicion al mismo estado se rechaza, sea cual sea el estado")
        void una_transicion_al_mismo_estado_se_rechaza(BillingDocumentStatus estado) {
            // Espejo de chk_bdsh_transition. Recorrer el enum entero es lo que caza el
            // dia que alguien anada un quinto valor y lo deje fuera de la comprobacion.
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    estado, estado, OCURRIO_EL, "Laura Restrepo", "sin cambio", CREADO_EL))
                    .isInstanceOf(SameStatusTransitionException.class)
                    .hasMessageContaining("did not change status")
                    .hasMessageContaining(estado.name());
        }

        @ParameterizedTest
        @CsvSource({"DRAFT,AWAITING_EXTERNAL", "AWAITING_EXTERNAL,EXTERNAL_REGISTERED",
                "EXTERNAL_REGISTERED,VOIDED", "VOIDED,DRAFT", "EXTERNAL_REGISTERED,DRAFT",
                "AWAITING_EXTERNAL,VOIDED"})
        @DisplayName("cualquier transicion hacia OTRO estado es legitima y no la juzga la bitacora")
        void cualquier_transicion_hacia_otro_estado_es_legitima(BillingDocumentStatus origen,
                BillingDocumentStatus destino) {
            // Que transiciones tienen sentido lo decide subscriptionbilling, no esta
            // ficha: la bitacora apunta lo que ocurrio. Lo unico que exige es que sea
            // un cambio. Por eso la lista incluye los pares que van "hacia atras"
            // -VOIDED a DRAFT, EXTERNAL_REGISTERED a DRAFT-: prohibirlos aqui dejaria
            // sin apuntar una correccion legitima hecha en la otra feature, y la
            // pelicula se quedaria con un salto que nadie puede explicar.
            assertThatCode(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO, origen,
                    destino, OCURRIO_EL, "Laura Restrepo", "correccion", CREADO_EL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sin estado de origen no hay fotograma: el tramo anterior no empalmaria")
        void sin_estado_de_origen_no_hay_fotograma() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO, null,
                    BillingDocumentStatus.VOIDED, OCURRIO_EL, "Laura Restrepo", "anulado",
                    CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fromStatus is required");
        }

        @Test
        @DisplayName("sin estado de destino tampoco")
        void sin_estado_de_destino_tampoco() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, null, OCURRIO_EL, "Laura Restrepo", "motivo",
                    CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("toStatus is required");
        }

        @Test
        @DisplayName("sin documento de cobro no hay pelicula a la que pertenecer")
        void sin_documento_de_cobro_no_hay_pelicula() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, null,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "Laura Restrepo", "anulado", CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billingDocumentId is required");
        }

        @Test
        @DisplayName("sin momento del cambio no se puede ordenar ni cortar a una fecha")
        void sin_momento_del_cambio_no_se_puede_ordenar() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, null,
                    "Laura Restrepo", "anulado", CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("occurredAt is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("un actor en blanco no identifica a nadie y se rechaza")
        void un_actor_en_blanco_se_rechaza(String actor) {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL, actor,
                    "anulado", CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actor is required");
        }

        @Test
        @DisplayName("un actor nulo se rechaza igual")
        void un_actor_nulo_se_rechaza_igual() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL, null,
                    "anulado", CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actor is required");
        }

        @Test
        @DisplayName("un actor de mas de 120 caracteres no cabe en la columna y se para antes")
        void un_actor_demasiado_largo_se_para_antes() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "a".repeat(121), "anulado", CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actor must be 120 chars or less");
        }

        @Test
        @DisplayName("el actor de exactamente 120 caracteres si cabe")
        void el_actor_de_exactamente_120_caracteres_cabe() {
            assertThatCode(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "a".repeat(120), "anulado", CREADO_EL)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("un motivo en blanco no explica nada seis meses despues y se rechaza")
        void un_motivo_en_blanco_se_rechaza(String motivo) {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "Laura Restrepo", motivo, CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        @DisplayName("un motivo de mas de 255 caracteres se para antes de llegar a la columna")
        void un_motivo_demasiado_largo_se_para_antes() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "Laura Restrepo", "m".repeat(256), CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason must be 255 chars or less");
        }

        @Test
        @DisplayName("las validaciones tambien corren al reconstruir la fila desde la base")
        void las_validaciones_corren_al_reconstruir_desde_la_base() {
            // El mapper de lectura pasa por el mismo constructor. Si una fila escrita
            // por SQL crudo se saltara chk_bdsh_transition, la lectura falla en vez de
            // devolver un fotograma imposible que despues descuadraria el informe.
            assertThatThrownBy(() -> new BillingDocumentStatusHistory(7L, EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.VOIDED, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "Laura Restrepo", "motivo", CREADO_EL))
                    .isInstanceOf(SameStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("sin empresa no hay fotograma: el filtro de tenant no tendria por donde")
        void sin_empresa_no_hay_fotograma() {
            assertThatThrownBy(() -> BillingDocumentStatusHistory.register(null, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "Laura Restrepo", "anulado", CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("la empresa del fotograma es la que se le dio y no se deriva del documento")
        void la_empresa_del_fotograma_es_la_que_se_le_dio() {
            // El id del documento es el mismo en las dos: si alguien dedujera la empresa
            // del documento en vez de exigirla, las dos saldrian iguales. Quien
            // comprueba que el par (empresa, documento) existe de verdad es el
            // ValidationPort del servicio, y la FK compuesta detras.
            BillingDocumentStatusHistory propio = BillingDocumentStatusHistory.register(EMPRESA,
                    DOCUMENTO, BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED,
                    OCURRIO_EL, "Laura Restrepo", "anulado", CREADO_EL);
            BillingDocumentStatusHistory ajeno = BillingDocumentStatusHistory.register(
                    BillingDocumentStatusHistoryMother.OTRA_EMPRESA, DOCUMENTO,
                    BillingDocumentStatus.DRAFT, BillingDocumentStatus.VOIDED, OCURRIO_EL,
                    "Laura Restrepo", "anulado", CREADO_EL);

            assertThat(propio.getCompanyId()).isEqualTo(EMPRESA);
            assertThat(ajeno.getCompanyId())
                    .isEqualTo(BillingDocumentStatusHistoryMother.OTRA_EMPRESA);
            assertThat(propio.getCompanyId()).isNotEqualTo(ajeno.getCompanyId());
        }
    }
}
