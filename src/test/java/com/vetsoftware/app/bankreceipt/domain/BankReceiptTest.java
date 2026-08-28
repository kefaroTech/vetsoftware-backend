package com.vetsoftware.app.bankreceipt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las invariantes del extracto, en el sitio donde el CLAUDE.md las pide: el
 * constructor de la entidad.
 *
 * <p>
 * <b>El caso que mas vale de esta clase es el del importe negativo.</b> Todas
 * las demas tablas de dinero del proyecto guardan magnitudes y su {@code CHECK}
 * es {@code > 0}; el del extracto es {@code <> 0}, porque un cargo del banco o
 * la devolucion de un cheque entran con signo. Un {@code amount > 0} escrito
 * aqui por reflejo compilaria, pasaria el resto de la suite y volveria
 * inexpresable la mitad de un extracto real. Los dos casos —el negativo que
 * entra y el cero que no— son la red de esa decision.
 */
@DisplayName("BankReceipt — la linea del extracto y sus invariantes")
class BankReceiptTest {

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("nace en la bandeja, sin sellar y con cada campo en su sitio")
        void nace_en_la_bandeja_sin_sellar() {
            BankReceipt entrada = BankReceiptMother.enLaBandeja();

            assertThat(entrada.getId()).isNull();
            assertThat(entrada.getBankAccountRef()).isEqualTo(BankReceiptMother.CUENTA);
            assertThat(entrada.getBankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
            assertThat(entrada.getReceivedOn()).isEqualTo(BankReceiptMother.RECIBIDA_EL);
            assertThat(entrada.getAmount()).isEqualByComparingTo(BankReceiptMother.IMPORTE);
            assertThat(entrada.getDescription()).isEqualTo(BankReceiptMother.DESCRIPCION);
            assertThat(entrada.getStatus()).isEqualTo(BankReceiptStatus.UNIDENTIFIED);
            assertThat(entrada.getIdentifiedAt()).isNull();
            assertThat(entrada.getCreatedDate()).isEqualTo(BankReceiptMother.CREADA_EL);
            assertThat(entrada.isUnidentified()).isTrue();
        }

        @Test
        @DisplayName("la descripcion es opcional: el banco no siempre manda concepto")
        void la_descripcion_es_opcional() {
            BankReceipt entrada = BankReceipt.register(BankReceiptMother.CUENTA,
                    BankReceiptMother.REFERENCIA, BankReceiptMother.RECIBIDA_EL,
                    BankReceiptMother.IMPORTE, null, BankReceiptMother.CREADA_EL);

            assertThat(entrada.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("Importe")
    class Importe {

        @Test
        @DisplayName("un importe NEGATIVO es valido: un cargo del banco entra con signo")
        void un_importe_negativo_es_valido() {
            // El CHECK del esquema es `amount <> 0`, no `amount > 0`. Este caso es la
            // red de esa diferencia: si alguien escribe aqui el `> 0` que llevan las
            // demas tablas de dinero, la mitad de un extracto real deja de poder
            // cargarse y el operario no sabe por que.
            BankReceipt cargo = BankReceiptMother.conImporte(new BigDecimal("-45000.00"));

            assertThat(cargo.getAmount()).isEqualByComparingTo("-45000.00");
            assertThat(cargo.getAmount().signum()).isNegative();
        }

        @Test
        @DisplayName("un importe de CERO se rechaza: es la unica cifra que no dice nada")
        void un_importe_de_cero_se_rechaza() {
            assertThatThrownBy(() -> BankReceiptMother.conImporte(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount cannot be zero");
        }

        @Test
        @DisplayName("un cero escrito con decimales tambien se rechaza")
        void un_cero_con_decimales_tambien_se_rechaza() {
            // 0.00 no es igual a BigDecimal.ZERO por equals, solo por compareTo. Una
            // comprobacion escrita con equals dejaria pasar esta fila y la base la
            // pararia despues, con un error que no nombra la columna.
            assertThatThrownBy(() -> BankReceiptMother.conImporte(new BigDecimal("0.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount cannot be zero");
        }

        @Test
        @DisplayName("un tercer decimal se rechaza en vez de dejar que la base lo redondee")
        void un_tercer_decimal_se_rechaza() {
            // DECIMAL(19,2): MySQL no falla, redondea. Un centavo perdido en silencio
            // en el extracto es un cuadre que no cierra y nadie sabe por que.
            assertThatThrownBy(() -> BankReceiptMother.conImporte(new BigDecimal("100.005")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2 decimals or less");
        }

        @Test
        @DisplayName("los ceros a la derecha no cuentan como decimales de mas")
        void los_ceros_a_la_derecha_no_cuentan() {
            // 100.5000 tiene escala 4 pero vale 100.50: la base lo guarda sin perder
            // nada. Rechazarlo seria inventarse una restriccion que el esquema no tiene.
            assertThatCode(() -> BankReceiptMother.conImporte(new BigDecimal("100.5000")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("el importe es obligatorio")
        void el_importe_es_obligatorio() {
            assertThatThrownBy(() -> BankReceiptMother.conImporte(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount is required");
        }

        @ParameterizedTest
        @EnumSource(BankReceiptStatus.class)
        @DisplayName("ningun estado admite un importe de cero")
        void ningun_estado_admite_importe_cero(BankReceiptStatus estado) {
            assertThatThrownBy(() -> BankReceiptMother.enEstadoConImporte(estado, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount cannot be zero");
        }
    }

    @Nested
    @DisplayName("Referencias")
    class Referencias {

        @ParameterizedTest(name = "referencia [{0}]")
        @ValueSource(strings = {"", "   "})
        @DisplayName("la referencia del banco no puede venir vacia")
        void la_referencia_no_puede_venir_vacia(String referencia) {
            assertThatThrownBy(() -> BankReceiptMother.conReferencia(referencia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bankReference is required");
        }

        @Test
        @DisplayName("la referencia no puede pasar de 120 caracteres")
        void la_referencia_no_puede_pasar_de_120() {
            assertThatThrownBy(() -> BankReceiptMother.conReferencia("R".repeat(121)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bankReference must be 120 chars or less");
        }

        @Test
        @DisplayName("un caracter fuera de ASCII se rechaza aqui y no en el driver")
        void un_caracter_fuera_de_ascii_se_rechaza() {
            // La columna es CHARACTER SET ascii: MySQL no trunca, rechaza con un
            // "Incorrect string value" que no dice de que fila viene. En una carga
            // masiva eso es un fichero entero que no entra sin saber por cual linea.
            assertThatThrownBy(() -> BankReceiptMother.conReferencia("TRX-2026-CONSIGNACIÓN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bankReference must be ASCII");
        }

        @Test
        @DisplayName("la cuenta bancaria es obligatoria y tambien va en ASCII")
        void la_cuenta_bancaria_es_obligatoria_y_ascii() {
            assertThatThrownBy(() -> BankReceipt.register(null, BankReceiptMother.REFERENCIA,
                    BankReceiptMother.RECIBIDA_EL, BankReceiptMother.IMPORTE, null,
                    BankReceiptMother.CREADA_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bankAccountRef is required");
            assertThatThrownBy(() -> BankReceipt.register("CUENTA-Ñ", BankReceiptMother.REFERENCIA,
                    BankReceiptMother.RECIBIDA_EL, BankReceiptMother.IMPORTE, null,
                    BankReceiptMother.CREADA_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bankAccountRef must be ASCII");
        }

        @Test
        @DisplayName("dos referencias que solo difieren en mayusculas son entradas distintas")
        void dos_capitalizaciones_son_entradas_distintas() {
            // La columna es ascii_bin y el dominio no normaliza: guarda lo que el banco
            // escribio. Si alguien metiera aqui un toUpperCase, la segunda consignacion
            // del dia se descartaria como duplicada de la primera.
            assertThat(BankReceiptMother.conReferencia("ab12cd").getBankReference())
                    .isEqualTo("ab12cd")
                    .isNotEqualTo(BankReceiptMother.conReferencia("AB12CD").getBankReference());
        }

        @Test
        @DisplayName("la fecha de recepcion es obligatoria")
        void la_fecha_de_recepcion_es_obligatoria() {
            assertThatThrownBy(() -> BankReceiptMother.recibidaEl(null, "TRX-SIN-FECHA"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("receivedOn is required");
        }

        @Test
        @DisplayName("la descripcion no puede pasar de 255 caracteres")
        void la_descripcion_no_puede_pasar_de_255() {
            assertThatThrownBy(() -> BankReceipt.register(BankReceiptMother.CUENTA,
                    BankReceiptMother.REFERENCIA, BankReceiptMother.RECIBIDA_EL,
                    BankReceiptMother.IMPORTE, "D".repeat(256), BankReceiptMother.CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description must be 255 chars or less");
        }
    }

    @Nested
    @DisplayName("El sello de identificacion")
    class ElSelloDeIdentificacion {

        @Test
        @DisplayName("una entrada en la bandeja NO puede llevar fecha de sellado")
        void una_entrada_en_la_bandeja_no_lleva_sello() {
            // Primera mitad del bicondicional de chk_bank_receipts_identified.
            assertThatThrownBy(() -> new BankReceipt(1L, BankReceiptMother.CUENTA,
                    BankReceiptMother.REFERENCIA, BankReceiptMother.RECIBIDA_EL,
                    BankReceiptMother.IMPORTE, null, BankReceiptStatus.UNIDENTIFIED,
                    BankReceiptMother.SELLADA_EL, BankReceiptMother.CREADA_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("identifiedAt must be absent while unidentified");
        }

        @ParameterizedTest
        @EnumSource(value = BankReceiptStatus.class, names = {"IDENTIFIED", "DISCARDED"})
        @DisplayName("los dos estados resueltos exigen fecha de sellado")
        void los_estados_resueltos_exigen_sello(BankReceiptStatus resuelto) {
            // Segunda mitad del mismo bicondicional. Las dos importan: sin esta, una
            // entrada archivada pierde la fecha en que se dejo de buscar.
            assertThatThrownBy(() -> new BankReceipt(1L, BankReceiptMother.CUENTA,
                    BankReceiptMother.REFERENCIA, BankReceiptMother.RECIBIDA_EL,
                    BankReceiptMother.IMPORTE, null, resuelto, null, BankReceiptMother.CREADA_EL,
                    0L)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("identifiedAt is required once resolved");
        }

        @Test
        @DisplayName("el estado es obligatorio")
        void el_estado_es_obligatorio() {
            assertThatThrownBy(() -> new BankReceipt(1L, BankReceiptMother.CUENTA,
                    BankReceiptMother.REFERENCIA, BankReceiptMother.RECIBIDA_EL,
                    BankReceiptMother.IMPORTE, null, null, null, BankReceiptMother.CREADA_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");
        }
    }

    @Nested
    @DisplayName("Transiciones")
    class Transiciones {

        private static final LocalDateTime AHORA = LocalDateTime.of(2026, 4, 1, 11, 5, 0);

        @Test
        @DisplayName("identificar saca de la bandeja y sella la hora que le den")
        void identificar_saca_de_la_bandeja_y_sella() {
            BankReceipt entrada = BankReceiptMother.enLaBandeja();

            entrada.identify(AHORA);

            assertThat(entrada.getStatus()).isEqualTo(BankReceiptStatus.IDENTIFIED);
            assertThat(entrada.getIdentifiedAt()).isEqualTo(AHORA);
            assertThat(entrada.isUnidentified()).isFalse();
        }

        @Test
        @DisplayName("descartar sella la MISMA columna que identificar")
        void descartar_sella_la_misma_columna() {
            // El CHECK trata los dos estados finales por igual: lo que la base exige no
            // es que se sepa el dueño, sino que conste cuando se dejo de buscar.
            BankReceipt entrada = BankReceiptMother.enLaBandeja();

            entrada.discard(AHORA);

            assertThat(entrada.getStatus()).isEqualTo(BankReceiptStatus.DISCARDED);
            assertThat(entrada.getIdentifiedAt()).isEqualTo(AHORA);
        }

        @ParameterizedTest
        @EnumSource(value = BankReceiptStatus.class, names = {"IDENTIFIED", "DISCARDED"})
        @DisplayName("una entrada ya resuelta no se puede volver a identificar")
        void una_entrada_resuelta_no_se_vuelve_a_identificar(BankReceiptStatus resuelto) {
            BankReceipt entrada = BankReceiptMother.enEstado(resuelto);

            assertThatThrownBy(() -> entrada.identify(AHORA))
                    .isInstanceOf(BankReceiptAlreadyResolvedException.class)
                    .hasMessageContaining("is already resolved with status " + resuelto);
        }

        @ParameterizedTest
        @EnumSource(value = BankReceiptStatus.class, names = {"IDENTIFIED", "DISCARDED"})
        @DisplayName("una entrada ya resuelta tampoco se puede volver a descartar")
        void una_entrada_resuelta_no_se_vuelve_a_descartar(BankReceiptStatus resuelto) {
            BankReceipt entrada = BankReceiptMother.enEstado(resuelto);

            assertThatThrownBy(() -> entrada.discard(AHORA))
                    .isInstanceOf(BankReceiptAlreadyResolvedException.class);
        }

        @Test
        @DisplayName("el rechazo deja la entrada intacta y no a medio resolver")
        void el_rechazo_deja_la_entrada_intacta() {
            // La guarda va ANTES de tocar los campos. Al reves, la entrada quedaria con
            // el estado nuevo y la excepcion lanzada, que es el peor de los dos mundos.
            BankReceipt yaResuelta = BankReceiptMother.enEstado(BankReceiptStatus.IDENTIFIED);

            assertThatThrownBy(() -> yaResuelta.discard(AHORA))
                    .isInstanceOf(BankReceiptAlreadyResolvedException.class);

            assertThat(yaResuelta.getStatus()).isEqualTo(BankReceiptStatus.IDENTIFIED);
            assertThat(yaResuelta.getIdentifiedAt()).isEqualTo(BankReceiptMother.SELLADA_EL);
        }

        @Test
        @DisplayName("resolver sin fecha de sellado se rechaza")
        void resolver_sin_fecha_de_sellado_se_rechaza() {
            BankReceipt entrada = BankReceiptMother.enLaBandeja();

            assertThatThrownBy(() -> entrada.identify(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("identifiedAt is required");
            assertThat(entrada.getStatus()).isEqualTo(BankReceiptStatus.UNIDENTIFIED);
        }
    }
}
