package com.vetsoftware.app.owner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Owner — invariantes fiscales colombianas. {@code validate(...)} implica dos
 * reglas cruzadas: NIT exige digito de verificacion de un solo caracter, y
 * persona JURIDICA exige NIT + razon social. La matriz de
 * {@link MatrizDocumentoPersona} recorre las diez combinaciones de
 * {@link OwnerDocumentType} x {@link PersonType} para que un nuevo tipo de
 * documento sin rama propia falle aqui y no en produccion.
 */
@DisplayName("Owner — invariantes y ciclo de vida del agregado")
class OwnerTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CityRef BOGOTA = new CityRef(5L, "Bogota");
    private static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Norte", "NIT-900123456");

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 100L;
        private String name = "Ana Ruiz";
        private String email = "ana@vet.com";
        private String document = "1020304050";
        private OwnerDocumentType documentType = OwnerDocumentType.CEDULA_CIUDADANIA;
        private PersonType personType = PersonType.NATURAL;
        private String verificationDigit;
        private String legalName;
        private String address = "Calle 1 # 2-3";
        private String phone = "3001112233";
        private CityRef city = BOGOTA;
        private CompanyRef company = CLINICA;
        private boolean withholdingAgent;
        private TaxRegime taxRegime = TaxRegime.NO_RESPONSABLE_IVA;
        private FiscalResponsibility fiscalResponsibility = FiscalResponsibility.NO_APLICA;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder email(String v) {
            this.email = v;
            return this;
        }

        private Builder document(String v) {
            this.document = v;
            return this;
        }

        private Builder documentType(OwnerDocumentType v) {
            this.documentType = v;
            return this;
        }

        private Builder personType(PersonType v) {
            this.personType = v;
            return this;
        }

        private Builder verificationDigit(String v) {
            this.verificationDigit = v;
            return this;
        }

        private Builder legalName(String v) {
            this.legalName = v;
            return this;
        }

        private Builder address(String v) {
            this.address = v;
            return this;
        }

        private Builder phone(String v) {
            this.phone = v;
            return this;
        }

        private Builder city(CityRef v) {
            this.city = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder taxRegime(TaxRegime v) {
            this.taxRegime = v;
            return this;
        }

        private Builder fiscalResponsibility(FiscalResponsibility v) {
            this.fiscalResponsibility = v;
            return this;
        }

        private Owner build() {
            return new Owner(id, name, email, document, documentType, personType, verificationDigit,
                    legalName, address, phone, city, company, withholdingAgent, taxRegime,
                    fiscalResponsibility, CREADO, true);
        }

        private void applyTo(Owner owner) {
            owner.update(name, email, document, documentType, personType, verificationDigit,
                    legalName, address, phone, city, company, withholdingAgent, taxRegime,
                    fiscalResponsibility);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Owner owner = valido().build();

            assertThat(owner.getId()).isEqualTo(100L);
            assertThat(owner.getName()).isEqualTo("Ana Ruiz");
            assertThat(owner.getEmail()).isEqualTo("ana@vet.com");
            assertThat(owner.getDocument()).isEqualTo("1020304050");
            assertThat(owner.getDocumentType()).isEqualTo(OwnerDocumentType.CEDULA_CIUDADANIA);
            assertThat(owner.getPersonType()).isEqualTo(PersonType.NATURAL);
            assertThat(owner.getVerificationDigit()).isNull();
            assertThat(owner.getLegalName()).isNull();
            assertThat(owner.getAddress()).isEqualTo("Calle 1 # 2-3");
            assertThat(owner.getPhone()).isEqualTo("3001112233");
            assertThat(owner.getCity()).isEqualTo(BOGOTA);
            assertThat(owner.getCompany()).isEqualTo(CLINICA);
            assertThat(owner.isWithholdingAgent()).isFalse();
            assertThat(owner.getTaxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
            assertThat(owner.getFiscalResponsibility()).isEqualTo(FiscalResponsibility.NO_APLICA);
            assertThat(owner.getCreatedDate()).isEqualTo(CREADO);
            assertThat(owner.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado, con createdDate cercano a ahora")
        void create_nace_sin_id_habilitado_y_con_created_date_cercano_a_ahora() {
            Owner owner = Owner.create("Ana Ruiz", "ana@vet.com", "1020304050",
                    OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null, null,
                    "Calle 1 # 2-3", "3001112233", BOGOTA, CLINICA, false,
                    TaxRegime.NO_RESPONSABLE_IVA, FiscalResponsibility.NO_APLICA);

            assertThat(owner.getId()).isNull();
            assertThat(owner.isEnabled()).isTrue();
            // Sin Clock inyectable en el factory: deuda anotada en "Determinismo" del
            // CLAUDE.md. La asercion es una ventana, no un valor exacto.
            assertThat(owner.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("email, address y phone son opcionales")
        void email_address_y_phone_son_opcionales() {
            assertThatCode(() -> valido().email(null).address(null).phone(null).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("el digito de verificacion en blanco se normaliza a null")
        void el_digito_de_verificacion_en_blanco_se_normaliza_a_null() {
            Owner owner = valido().verificationDigit("   ").build();

            assertThat(owner.getVerificationDigit()).isNull();
        }
    }

    @Nested
    @DisplayName("invariantes de longitud y presencia")
    class InvariantesGenerales {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valido().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de 151 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(151)).build(),
                            "name must be 150 chars or less"),
                    arguments("document null",
                            (ThrowingCallable) () -> valido().document(null).build(),
                            "document is required"),
                    arguments("document vacio",
                            (ThrowingCallable) () -> valido().document("").build(),
                            "document is required"),
                    arguments("document de 51 chars",
                            (ThrowingCallable) () -> valido().document("1".repeat(51)).build(),
                            "document must be 50 chars or less"),
                    arguments("email de 151 chars",
                            (ThrowingCallable) () -> valido().email("x".repeat(151)).build(),
                            "email must be 150 chars or less"),
                    arguments("address de 256 chars",
                            (ThrowingCallable) () -> valido().address("x".repeat(256)).build(),
                            "address must be 255 chars or less"),
                    arguments("phone de 31 chars",
                            (ThrowingCallable) () -> valido().phone("1".repeat(31)).build(),
                            "phone must be 30 chars or less"),
                    arguments("city null", (ThrowingCallable) () -> valido().city(null).build(),
                            "city is required"),
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"),
                    arguments("taxRegime null",
                            (ThrowingCallable) () -> valido().taxRegime(null).build(),
                            "taxRegime is required"),
                    arguments("fiscalResponsibility null",
                            (ThrowingCallable) () -> valido().fiscalResponsibility(null).build(),
                            "fiscalResponsibility is required"),
                    arguments("documentType null",
                            (ThrowingCallable) () -> valido().documentType(null).build(),
                            "documentType is required"),
                    arguments("personType null",
                            (ThrowingCallable) () -> valido().personType(null).build(),
                            "personType is required"),
                    arguments("legalName de 256 chars",
                            (ThrowingCallable) () -> valido().legalName("x".repeat(256)).build(),
                            "legalName must be 255 chars or less"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("name de 150 chars, document de 50 y legalName de 255 se aceptan en el limite")
        void los_limites_exactos_se_aceptan() {
            assertThatCode(() -> valido().name("x".repeat(150)).document("1".repeat(50))
                    .personType(PersonType.NATURAL).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("digito de verificacion — solo NIT lo exige")
    class DigitoDeVerificacion {

        @Test
        @DisplayName("NIT sin digito de verificacion se rechaza")
        void nit_sin_digito_se_rechaza() {
            assertThatThrownBy(() -> valido().documentType(OwnerDocumentType.NIT)
                    .document("900123456").verificationDigit(null).build())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "verificationDigit is required when document type is NIT");
        }

        @Test
        @DisplayName("NIT con digito de dos caracteres se rechaza")
        void nit_con_digito_de_dos_caracteres_se_rechaza() {
            assertThatThrownBy(() -> valido().documentType(OwnerDocumentType.NIT)
                    .document("900123456").verificationDigit("12").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verificationDigit must be a single digit 0-9");
        }

        @Test
        @DisplayName("NIT con digito no numerico se rechaza")
        void nit_con_digito_no_numerico_se_rechaza() {
            assertThatThrownBy(() -> valido().documentType(OwnerDocumentType.NIT)
                    .document("900123456").verificationDigit("A").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verificationDigit must be a single digit 0-9");
        }

        @Test
        @DisplayName("NIT con digito valido de un solo caracter se acepta")
        void nit_con_digito_valido_se_acepta() {
            assertThatCode(() -> valido().documentType(OwnerDocumentType.NIT).document("900123456")
                    .verificationDigit("7").build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un documento distinto de NIT con digito presente se rechaza")
        void documento_no_nit_con_digito_presente_se_rechaza() {
            assertThatThrownBy(() -> valido().documentType(OwnerDocumentType.CEDULA_CIUDADANIA)
                    .verificationDigit("7").build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "verificationDigit is only allowed when document type is NIT");
        }
    }

    @Nested
    @DisplayName("matriz OwnerDocumentType x PersonType — la regla cruzada de validate()")
    class MatrizDocumentoPersona {

        static Stream<Arguments> combinacionesValidas() {
            return Stream.of(
                    arguments(OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null, null),
                    arguments(OwnerDocumentType.CEDULA_EXTRANJERIA, PersonType.NATURAL, null, null),
                    arguments(OwnerDocumentType.PASAPORTE, PersonType.NATURAL, null, null),
                    arguments(OwnerDocumentType.PEP, PersonType.NATURAL, null, null),
                    arguments(OwnerDocumentType.NIT, PersonType.NATURAL, "7", null),
                    arguments(OwnerDocumentType.NIT, PersonType.JURIDICA, "7",
                            "Veterinaria Sur S.A.S."));
        }

        @ParameterizedTest(name = "{0} x {1} es una combinacion valida")
        @MethodSource("combinacionesValidas")
        @DisplayName("las combinaciones coherentes con la regla fiscal se aceptan")
        void las_combinaciones_coherentes_se_aceptan(OwnerDocumentType documentType,
                PersonType personType, String verificationDigit, String legalName) {
            assertThatCode(() -> valido().documentType(documentType).personType(personType)
                    .document(documentType == OwnerDocumentType.NIT ? "900123456" : "1020304050")
                    .verificationDigit(verificationDigit).legalName(legalName).build())
                    .doesNotThrowAnyException();
        }

        static Stream<Arguments> personaJuridicaSinNit() {
            return Stream.of(arguments(OwnerDocumentType.CEDULA_CIUDADANIA),
                    arguments(OwnerDocumentType.CEDULA_EXTRANJERIA),
                    arguments(OwnerDocumentType.PASAPORTE), arguments(OwnerDocumentType.PEP));
        }

        @ParameterizedTest(name = "JURIDICA con {0} (no NIT) se rechaza")
        @MethodSource("personaJuridicaSinNit")
        @DisplayName("una persona juridica sin documento NIT se rechaza, sea cual sea el tipo")
        void persona_juridica_sin_nit_se_rechaza(OwnerDocumentType documentType) {
            assertThatThrownBy(() -> valido().documentType(documentType)
                    .personType(PersonType.JURIDICA).legalName("Veterinaria Sur S.A.S.").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a juridical person must have document type NIT");
        }

        @Test
        @DisplayName("persona juridica con NIT pero sin razon social se rechaza")
        void persona_juridica_con_nit_sin_razon_social_se_rechaza() {
            assertThatThrownBy(() -> valido().documentType(OwnerDocumentType.NIT)
                    .document("900123456").personType(PersonType.JURIDICA).verificationDigit("7")
                    .legalName(null).build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalName is required for a juridical person");
        }

        @Test
        @DisplayName("persona juridica con NIT y razon social en blanco se rechaza")
        void persona_juridica_con_razon_social_en_blanco_se_rechaza() {
            assertThatThrownBy(() -> valido().documentType(OwnerDocumentType.NIT)
                    .document("900123456").personType(PersonType.JURIDICA).verificationDigit("7")
                    .legalName("   ").build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalName is required for a juridical person");
        }

        @Test
        @DisplayName("persona natural con NIT no exige razon social")
        void persona_natural_con_nit_no_exige_razon_social() {
            assertThatCode(() -> valido().documentType(OwnerDocumentType.NIT).document("900123456")
                    .personType(PersonType.NATURAL).verificationDigit("7").legalName(null).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Owner owner = valido().build();

            valido().name("Ana Maria Ruiz").email("anamaria@vet.com").document("9988776655")
                    .documentType(OwnerDocumentType.CEDULA_EXTRANJERIA).address("Carrera 9 # 8-7")
                    .phone("3005556677").taxRegime(TaxRegime.RESPONSABLE_IVA)
                    .fiscalResponsibility(FiscalResponsibility.AUTORRETENEDOR).applyTo(owner);

            assertThat(owner.getName()).isEqualTo("Ana Maria Ruiz");
            assertThat(owner.getEmail()).isEqualTo("anamaria@vet.com");
            assertThat(owner.getDocument()).isEqualTo("9988776655");
            assertThat(owner.getDocumentType()).isEqualTo(OwnerDocumentType.CEDULA_EXTRANJERIA);
            assertThat(owner.getAddress()).isEqualTo("Carrera 9 # 8-7");
            assertThat(owner.getPhone()).isEqualTo("3005556677");
            assertThat(owner.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
            assertThat(owner.getFiscalResponsibility())
                    .isEqualTo(FiscalResponsibility.AUTORRETENEDOR);
            assertThat(owner.getId()).isEqualTo(100L);
            assertThat(owner.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Owner owner = valido().build();

            // El nombre es valido y personType/documentType quedan inconsistentes: si
            // validate() no corriera ANTES de asignar, el owner se quedaria con el
            // nombre nuevo y los datos fiscales viejos a medio actualizar.
            assertThatThrownBy(() -> valido().name("Otro Nombre").personType(PersonType.JURIDICA)
                    .legalName(null).applyTo(owner)).isInstanceOf(IllegalArgumentException.class);

            assertThat(owner.getName()).isEqualTo("Ana Ruiz");
            assertThat(owner.getPersonType()).isEqualTo(PersonType.NATURAL);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Owner owner = valido().build();
            owner.disable();

            valido().name("Otro Nombre").applyTo(owner);

            assertThat(owner.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Owner owner = valido().build();

            owner.disable();
            assertThat(owner.isEnabled()).isFalse();
            owner.disable();
            assertThat(owner.isEnabled()).isFalse();

            owner.enable();
            assertThat(owner.isEnabled()).isTrue();
            owner.enable();
            assertThat(owner.isEnabled()).isTrue();
        }
    }
}
