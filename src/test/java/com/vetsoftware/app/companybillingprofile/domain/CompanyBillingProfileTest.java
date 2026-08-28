package com.vetsoftware.app.companybillingprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * El agregado que decide a quien se le factura.
 *
 * <p>
 * <b>Cada caso de {@code Validaciones} es el espejo de un {@code CHECK} de
 * {@code 316_create_company_billing_profiles.xml}</b>, y por eso los nombres de
 * las constraints aparecen en los {@code @DisplayName}: si alguien relaja el
 * dominio, el motor sigue rechazando la fila y el fallo aparece en produccion
 * en mitad de un alta; si alguien endurece el dominio por encima del esquema,
 * la aplicacion rechaza datos que la tabla si admite. Los dos sentidos
 * importan.
 */
@DisplayName("CompanyBillingProfile — la ficha de a quien se le factura")
class CompanyBillingProfileTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("una ficha recien abierta nace VIGENTE: sin fecha de cierre y sin id")
        void una_ficha_recien_abierta_nace_vigente() {
            CompanyBillingProfile ficha = CompanyBillingProfileMother.sociedad();

            assertThat(ficha.getId()).isNull();
            assertThat(ficha.getVersion()).isNull();
            assertThat(ficha.getValidTo()).isNull();
            assertThat(ficha.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("guarda cada dato en su sitio sin cruzar la razon social con los nombres")
        void guarda_cada_dato_en_su_sitio() {
            CompanyBillingProfile ficha = CompanyBillingProfileMother.sociedad();

            assertThat(ficha.getCompanyId()).isEqualTo(CompanyBillingProfileMother.COMPANY_ID);
            assertThat(ficha.getPersonKind()).isEqualTo(PersonKind.LEGAL);
            assertThat(ficha.getTaxIdKind()).isEqualTo(TaxIdKind.NIT);
            assertThat(ficha.getTaxId()).isEqualTo(CompanyBillingProfileMother.NIT);
            assertThat(ficha.getVerificationDigit())
                    .isEqualTo(CompanyBillingProfileMother.DIGITO_VERIFICACION);
            assertThat(ficha.getLegalName()).isEqualTo(CompanyBillingProfileMother.RAZON_SOCIAL);
            assertThat(ficha.getFirstName()).isNull();
            assertThat(ficha.getAddress()).isEqualTo(CompanyBillingProfileMother.DIRECCION);
            assertThat(ficha.getCity()).isEqualTo(CompanyBillingProfileMother.MEDELLIN);
            assertThat(ficha.getBillingEmail()).isEqualTo(CompanyBillingProfileMother.CORREO);
            assertThat(ficha.getTaxRegime()).isEqualTo(TaxRegime.COMMON);
            assertThat(ficha.getValidFrom()).isEqualTo(CompanyBillingProfileMother.RIGE_DESDE);
            assertThat(ficha.getCreatedDate()).isEqualTo(CompanyBillingProfileMother.CREADA_EL);
        }

        @Test
        @DisplayName("la persona natural conserva los CUATRO campos de nombre por separado")
        void la_persona_natural_conserva_los_cuatro_campos_por_separado() {
            // Es la razon de ser del modelo: la informacion exogena anual exige primer
            // nombre, otros nombres, primer apellido y segundo apellido en columnas
            // distintas, y eso no se rellena hacia atras.
            CompanyBillingProfile ficha = CompanyBillingProfileMother.personaNatural();

            assertThat(ficha.getFirstName()).isEqualTo(CompanyBillingProfileMother.PRIMER_NOMBRE);
            assertThat(ficha.getMiddleName()).isEqualTo(CompanyBillingProfileMother.OTROS_NOMBRES);
            assertThat(ficha.getLastName()).isEqualTo(CompanyBillingProfileMother.PRIMER_APELLIDO);
            assertThat(ficha.getSecondLastName())
                    .isEqualTo(CompanyBillingProfileMother.SEGUNDO_APELLIDO);
            assertThat(ficha.getLegalName()).isNull();
        }

        @Test
        @DisplayName("withholdingAgent guarda lo que es el CLIENTE y no se deduce de nada")
        void withholding_agent_guarda_lo_que_es_el_cliente() {
            // Que VetSoftware sea autorretenedor vive en platform_billing_config: la
            // norma dice que las dos condiciones coexisten, asi que deducir una de la
            // otra seria falso.
            assertThat(CompanyBillingProfileMother.sociedad().isWithholdingAgent()).isTrue();
            assertThat(CompanyBillingProfileMother.personaNatural().isWithholdingAgent()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(TaxRegime.class)
        @DisplayName("los cuatro regimenes del CHECK son construibles")
        void los_cuatro_regimenes_son_construibles(TaxRegime regimen) {
            // Espejo de chk_company_billing_profiles_tax_regime. Si alguien añade un
            // quinto valor al enum sin el changeset que amplie el CHECK, este caso pasa
            // y el INSERT muere en produccion: lo que esta prueba congela es que ninguno
            // de los cuatro que hoy admite la base este rechazado por el dominio.
            CompanyBillingProfile ficha = new CompanyBillingProfile(null,
                    CompanyBillingProfileMother.COMPANY_ID, PersonKind.LEGAL, TaxIdKind.NIT,
                    CompanyBillingProfileMother.NIT, null, CompanyBillingProfileMother.RAZON_SOCIAL,
                    null, null, null, null, CompanyBillingProfileMother.DIRECCION,
                    CompanyBillingProfileMother.MEDELLIN, CompanyBillingProfileMother.CORREO,
                    regimen, false, CompanyBillingProfileMother.RIGE_DESDE, null,
                    CompanyBillingProfileMother.CREADA_EL, null);

            assertThat(ficha.getTaxRegime()).isEqualTo(regimen);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @ParameterizedTest(name = "{0}: {6}")
        @CsvSource(nullValues = "NULO", value = {
                "LEGAL, NULO, NULO, NULO, NULO, NULO, legalName is required",
                "LEGAL, Inversiones Pet SAS, Ana, NULO, NULO, NULO, firstName must be absent",
                "LEGAL, Inversiones Pet SAS, NULO, Maria, NULO, NULO, middleName must be absent",
                "LEGAL, Inversiones Pet SAS, NULO, NULO, Ruiz, NULO, lastName must be absent",
                "LEGAL, Inversiones Pet SAS, NULO, NULO, NULO, Cardona, secondLastName must be absent",
                "NATURAL, Inversiones Pet SAS, Ana, NULO, Ruiz, NULO, legalName must be absent",
                "NATURAL, NULO, NULO, NULO, Ruiz, NULO, firstName is required",
                "NATURAL, NULO, Ana, NULO, NULO, NULO, lastName is required"})
        @DisplayName("chk_..._name_shape: la combinacion de nombres invalida se rechaza")
        void la_combinacion_de_nombres_invalida_se_rechaza(PersonKind personKind, String legalName,
                String firstName, String middleName, String lastName, String secondLastName,
                String mensaje) {
            // Las dos mitades de cada rama. Comprobar solo lo que TIENE que estar
            // dejaria entrar una sociedad que ademas trae apellidos: una ficha ambigua
            // sobre que juego de columnas hay que reportar a la administracion.
            assertThatThrownBy(() -> CompanyBillingProfileMother.conNombres(personKind, legalName,
                    firstName, middleName, lastName, secondLastName))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "{0} con legalName={1} firstName={2} lastName={4}")
        @CsvSource(nullValues = "NULO", value = {
                "LEGAL, Inversiones Pet SAS, NULO, NULO, NULO, NULO",
                "NATURAL, NULO, Ana, NULO, Ruiz, NULO", "NATURAL, NULO, Ana, Maria, Ruiz, Cardona"})
        @DisplayName("chk_..._name_shape: las tres combinaciones validas si entran")
        void las_combinaciones_validas_entran(PersonKind personKind, String legalName,
                String firstName, String middleName, String lastName, String secondLastName) {
            // La tercera fila importa tanto como las otras dos: middleName y
            // secondLastName son OPCIONALES en la rama natural —la constraint no los
            // exige y hay personas que no los tienen—, asi que exigirlos aqui seria ser
            // mas estricto que la base.
            CompanyBillingProfile ficha = CompanyBillingProfileMother.conNombres(personKind,
                    legalName, firstName, middleName, lastName, secondLastName);

            assertThat(ficha.getPersonKind()).isEqualTo(personKind);
            assertThat(ficha.getLegalName()).isEqualTo(legalName);
            assertThat(ficha.getFirstName()).isEqualTo(firstName);
        }

        @Test
        @DisplayName("una razon social en blanco cuenta como ausente")
        void una_razon_social_en_blanco_cuenta_como_ausente() {
            assertThatThrownBy(() -> CompanyBillingProfileMother.conNombres(PersonKind.LEGAL, "   ",
                    null, null, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalName is required");
        }

        @ParameterizedTest(name = "correo invalido: {0}")
        @CsvSource({"sin-arroba.com", "sin-dominio@", "@sin-usuario.com", "sin.punto@dominio"})
        @DisplayName("chk_..._email: el correo que no pasa el LIKE del esquema se rechaza")
        void el_correo_que_no_pasa_el_like_se_rechaza(String correo) {
            // El CHECK de la base es billing_email LIKE '%_@_%._%'. Esto no pretende
            // validar correos: es la misma criba minima, escrita donde el mensaje puede
            // nombrar el campo en vez de salir como un error del driver.
            assertThatThrownBy(() -> CompanyBillingProfileMother.conCorreo(correo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billingEmail must look like an email address");
        }

        @Test
        @DisplayName("un correo con subdominio y guiones si pasa: la criba es la del esquema, no un validador propio")
        void un_correo_con_subdominio_si_pasa() {
            // Escribir aqui una expresion mas estricta que el LIKE dejaria fuera
            // direcciones que la tabla si admite, y el usuario no tendria forma de saber
            // que corregir.
            assertThat(CompanyBillingProfileMother
                    .conCorreo("cuentas-por-pagar@mail.pet-sas.com.co").getBillingEmail())
                    .isEqualTo("cuentas-por-pagar@mail.pet-sas.com.co");
        }

        @Test
        @DisplayName("un documento con caracteres fuera de ASCII se rechaza aqui y no en el driver")
        void un_documento_no_ascii_se_rechaza() {
            // La columna es VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin: MySQL no
            // trunca, RECHAZA con un "Incorrect string value" que no dice de que fila ni
            // de que campo viene.
            assertThatThrownBy(() -> CompanyBillingProfileMother.conDocumento("900123456-Ñ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxId must be ASCII");
        }

        @Test
        @DisplayName("el documento es obligatorio y cabe en 50 caracteres")
        void el_documento_es_obligatorio_y_cabe_en_50() {
            assertThatThrownBy(() -> CompanyBillingProfileMother.conDocumento(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxId is required");
            assertThatThrownBy(() -> CompanyBillingProfileMother
                    .conDocumento(CompanyBillingProfileMother.cadenaDe(51)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxId must be 50 chars or less");
        }

        @Test
        @DisplayName("el digito de verificacion es UN digito, y una letra no lo es")
        void el_digito_de_verificacion_es_un_digito() {
            // Mas estricto que la columna (VARCHAR(1) sin CHECK) a proposito: el DV sale
            // del modulo 11 y siempre es una cifra. Una letra ahi viajaria intacta hasta
            // la informacion exogena.
            assertThatThrownBy(() -> CompanyBillingProfileMother.conDigitoDeVerificacion("K"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verificationDigit must be a digit");
            assertThatThrownBy(() -> CompanyBillingProfileMother.conDigitoDeVerificacion("12"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verificationDigit must be a single character");
        }

        @Test
        @DisplayName("el digito de verificacion ausente es valido: solo el NIT lo tiene")
        void el_digito_de_verificacion_ausente_es_valido() {
            assertThat(CompanyBillingProfileMother.conDigitoDeVerificacion(null)
                    .getVerificationDigit()).isNull();
        }

        @Test
        @DisplayName("la direccion es obligatoria y cabe en 255 caracteres")
        void la_direccion_es_obligatoria_y_cabe_en_255() {
            assertThatThrownBy(() -> CompanyBillingProfileMother.conDireccion("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("address is required");
            assertThatThrownBy(() -> CompanyBillingProfileMother
                    .conDireccion(CompanyBillingProfileMother.cadenaDe(256)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("address must be 255 chars or less");
        }

        @Test
        @DisplayName("chk_..._validity: una vigencia que termina el mismo dia en que empieza se rechaza")
        void una_vigencia_de_duracion_cero_se_rechaza() {
            // El CHECK es valid_to > valid_from ESTRICTO. Un >= aqui dejaria pasar una
            // ficha que la base rechaza, y esa diferencia de un signo es lo que hace que
            // la sucesion en el mismo dia no sea representable.
            LocalDate dia = CompanyBillingProfileMother.RIGE_DESDE;

            assertThatThrownBy(() -> CompanyBillingProfileMother.conVigencia(dia, dia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validTo must be after validFrom");
        }

        @Test
        @DisplayName("chk_..._validity: una vigencia que termina antes de empezar se rechaza")
        void una_vigencia_invertida_se_rechaza() {
            assertThatThrownBy(() -> CompanyBillingProfileMother.conVigencia(
                    CompanyBillingProfileMother.SUCEDE_DESDE,
                    CompanyBillingProfileMother.RIGE_DESDE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validTo must be after validFrom");
        }

        @Test
        @DisplayName("la fecha de inicio y el municipio son obligatorios")
        void la_fecha_de_inicio_y_el_municipio_son_obligatorios() {
            assertThatThrownBy(() -> CompanyBillingProfileMother.conVigencia(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validFrom is required");
            assertThatThrownBy(() -> CompanyBillingProfile.open(
                    CompanyBillingProfileMother.COMPANY_ID, PersonKind.LEGAL, TaxIdKind.NIT,
                    CompanyBillingProfileMother.NIT, null, CompanyBillingProfileMother.RAZON_SOCIAL,
                    null, null, null, null, CompanyBillingProfileMother.DIRECCION, null,
                    CompanyBillingProfileMother.CORREO, TaxRegime.COMMON, false,
                    CompanyBillingProfileMother.RIGE_DESDE, CompanyBillingProfileMother.CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("city is required");
        }

        @Test
        @DisplayName("un municipio sin nombre no es un municipio")
        void un_municipio_sin_nombre_no_es_un_municipio() {
            assertThatThrownBy(() -> new CityRef(900L, " "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("city name is required");
            assertThatThrownBy(() -> new CityRef(null, "Medellin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("city id is required");
        }
    }

    @Nested
    @DisplayName("Sucesion")
    class Sucesion {

        @Test
        @DisplayName("cerrar la ficha la saca de la vigencia y la sella con la fecha de la sucesora")
        void cerrar_la_ficha_la_sella_con_la_fecha_de_la_sucesora() {
            // La fecha es la del COMIENZO de la sucesora, no la del ultimo dia de esta:
            // el intervalo es semiabierto, asi que esta cubre hasta la vispera.
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L);

            vigente.closeOn(CompanyBillingProfileMother.SUCEDE_DESDE);

            assertThat(vigente.getValidTo()).isEqualTo(CompanyBillingProfileMother.SUCEDE_DESDE);
            assertThat(vigente.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("cerrar no toca ningun otro dato: la ficha vieja sigue diciendo a quien se facturo")
        void cerrar_no_toca_ningun_otro_dato() {
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L);

            vigente.closeOn(CompanyBillingProfileMother.SUCEDE_DESDE);

            assertThat(vigente.getTaxId()).isEqualTo(CompanyBillingProfileMother.NIT);
            assertThat(vigente.getLegalName()).isEqualTo(CompanyBillingProfileMother.RAZON_SOCIAL);
            assertThat(vigente.getValidFrom()).isEqualTo(CompanyBillingProfileMother.RIGE_DESDE);
        }

        @Test
        @DisplayName("no se puede cerrar dos veces la misma ficha")
        void no_se_puede_cerrar_dos_veces() {
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L);
            vigente.closeOn(CompanyBillingProfileMother.SUCEDE_DESDE);

            assertThatThrownBy(() -> vigente.closeOn(LocalDate.of(2026, 6, 1)))
                    .isInstanceOf(CompanyBillingProfileAlreadyClosedException.class)
                    .hasMessageContaining("already closed on 2026-04-01");
        }

        @Test
        @DisplayName("la sucesion EL MISMO DIA en que empezo la vigente se rechaza, no se corre al dia siguiente")
        void la_sucesion_el_mismo_dia_se_rechaza() {
            // Es la consecuencia del > estricto del CHECK, y esta decidida a proposito:
            // adelantar la fecha por cuenta propia escribiria que el dato nuevo rige
            // mañana cuando quien lo pidio dijo hoy, y esa fecha es la que decide a que
            // ficha apunta una factura emitida en el intervalo.
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L);

            assertThatThrownBy(() -> vigente.closeOn(CompanyBillingProfileMother.RIGE_DESDE))
                    .isInstanceOf(BillingProfileSuccessionNotAfterCurrentException.class)
                    .hasMessageContaining("the earliest possible date is 2026-01-16");
            assertThat(vigente.isCurrent()).as("la ficha rechazada sigue vigente").isTrue();
        }

        @Test
        @DisplayName("la sucesion con fecha ANTERIOR al inicio de la vigente se rechaza igual")
        void la_sucesion_con_fecha_anterior_se_rechaza() {
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L);

            assertThatThrownBy(() -> vigente.closeOn(LocalDate.of(2025, 12, 31)))
                    .isInstanceOf(BillingProfileSuccessionNotAfterCurrentException.class)
                    .satisfies(error -> {
                        BillingProfileSuccessionNotAfterCurrentException detalle = (BillingProfileSuccessionNotAfterCurrentException) error;
                        assertThat(detalle.getCurrentValidFrom())
                                .isEqualTo(CompanyBillingProfileMother.RIGE_DESDE);
                        assertThat(detalle.getRequestedEffectiveFrom())
                                .isEqualTo(LocalDate.of(2025, 12, 31));
                        assertThat(detalle.getEarliestEffectiveFrom())
                                .isEqualTo(LocalDate.of(2026, 1, 16));
                    });
        }

        @Test
        @DisplayName("el dia siguiente SI es representable: es la primera fecha posible")
        void el_dia_siguiente_si_es_representable() {
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L);

            vigente.closeOn(CompanyBillingProfileMother.RIGE_DESDE.plusDays(1));

            assertThat(vigente.getValidTo()).isEqualTo(LocalDate.of(2026, 1, 16));
        }

        @Test
        @DisplayName("cerrar sin fecha se rechaza: valid_to nulo significa vigente, no cerrada")
        void cerrar_sin_fecha_se_rechaza() {
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L);

            assertThatThrownBy(() -> vigente.closeOn(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("effectiveFrom is required");
            assertThat(vigente.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("la cadena queda sin hueco: la sucesora arranca el dia en que se cierra la anterior")
        void la_cadena_queda_sin_hueco() {
            // Intervalo semiabierto [valid_from, valid_to): en cualquier fecha hay
            // exactamente una ficha aplicable. Cerrar en la vispera dejaria el ultimo dia
            // sin ficha o, peor, con dos.
            CompanyBillingProfile anterior = CompanyBillingProfileMother.persistida(10L);
            anterior.closeOn(CompanyBillingProfileMother.SUCEDE_DESDE);
            CompanyBillingProfile sucesora = CompanyBillingProfileMother.sociedadDe(
                    CompanyBillingProfileMother.COMPANY_ID,
                    CompanyBillingProfileMother.SUCEDE_DESDE);

            assertThat(anterior.getValidTo()).isEqualTo(sucesora.getValidFrom());
            assertThat(anterior.isCurrent()).isFalse();
            assertThat(sucesora.isCurrent()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la ficha sin empresa no se puede construir")
        void la_ficha_sin_empresa_no_se_puede_construir() {
            assertThatThrownBy(() -> CompanyBillingProfileMother.sociedadDe(null,
                    CompanyBillingProfileMother.RIGE_DESDE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("guarda la empresa que se le da y no la deduce de ningun otro campo")
        void guarda_la_empresa_que_se_le_da() {
            assertThat(CompanyBillingProfileMother
                    .sociedadDe(CompanyBillingProfileMother.OTRA_COMPANY_ID,
                            CompanyBillingProfileMother.RIGE_DESDE)
                    .getCompanyId()).isEqualTo(CompanyBillingProfileMother.OTRA_COMPANY_ID);
        }

        @Test
        @DisplayName("cerrar una ficha no le cambia la empresa")
        void cerrar_una_ficha_no_le_cambia_la_empresa() {
            // La sucesion no es un cambio de dueño: si el cierre pudiera mover el
            // companyId, la ficha del historico dejaria de pertenecer a quien la abrio y
            // la factura vieja apuntaria a una fila de otro tenant.
            CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(10L,
                    CompanyBillingProfileMother.OTRA_COMPANY_ID,
                    CompanyBillingProfileMother.RIGE_DESDE, null);

            vigente.closeOn(CompanyBillingProfileMother.SUCEDE_DESDE);

            assertThat(vigente.getCompanyId())
                    .isEqualTo(CompanyBillingProfileMother.OTRA_COMPANY_ID);
        }
    }
}
