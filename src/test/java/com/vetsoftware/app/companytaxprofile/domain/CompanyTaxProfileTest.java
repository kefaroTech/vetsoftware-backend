package com.vetsoftware.app.companytaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Invariantes fiscales del agregado. Es el ultimo filtro antes de que un dato
 * llegue a una factura electronica: un NIT sin DV o un correo fiscal invalido
 * los rechaza la DIAN, no la base de datos.
 */
@DisplayName("CompanyTaxProfile")
class CompanyTaxProfileTest {

    private static final String DOCUMENTO = CompanyTaxProfileMother.NIT;
    private static final String DV = CompanyTaxProfileMother.NIT_DV;
    private static final String RAZON = CompanyTaxProfileMother.RAZON_SOCIAL;
    private static final String EMAIL = CompanyTaxProfileMother.EMAIL_FISCAL;

    /** Constructor de conveniencia: solo expone los campos que cada test varia. */
    private static CompanyTaxProfile perfil(CompanyRef company, CompanyDocumentType tipo,
            String documentId, String dv, String legalName, TaxRegime regimen, String email) {
        return new CompanyTaxProfile(CompanyTaxProfileMother.PROFILE_ID, company, tipo, documentId,
                dv, legalName, regimen, email, CompanyTaxProfileMother.NOMBRE_COMERCIAL,
                CompanyTaxProfileMother.VETERINARIA, List.of(CompanyTaxProfileMother.O13),
                CompanyTaxProfileMother.CREADO, null, true);
    }

    private static CompanyTaxProfile perfilValido() {
        return perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT, DOCUMENTO, DV,
                RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL);
    }

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo tal y como se le entrega")
        void conserva_cada_campo_tal_y_como_se_le_entrega() {
            CompanyTaxProfile profile = CompanyTaxProfileMother.perfilNit();

            assertThat(profile.getId()).isEqualTo(CompanyTaxProfileMother.PROFILE_ID);
            assertThat(profile.getCompany()).isEqualTo(CompanyTaxProfileMother.CLINICA);
            assertThat(profile.getCompanyDocumentType()).isEqualTo(CompanyDocumentType.NIT);
            assertThat(profile.getCompanyDocumentId()).isEqualTo(DOCUMENTO);
            assertThat(profile.getCompanyDocumentVerificationDigit()).isEqualTo(DV);
            assertThat(profile.getLegalName()).isEqualTo(RAZON);
            assertThat(profile.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
            assertThat(profile.getFiscalEmail()).isEqualTo(EMAIL);
            assertThat(profile.getCommercialName())
                    .isEqualTo(CompanyTaxProfileMother.NOMBRE_COMERCIAL);
            assertThat(profile.getEconomicActivity())
                    .isEqualTo(CompanyTaxProfileMother.VETERINARIA);
            assertThat(profile.getResponsibilities()).containsExactly(CompanyTaxProfileMother.O13,
                    CompanyTaxProfileMother.O15);
            assertThat(profile.getCreatedDate()).isEqualTo(CompanyTaxProfileMother.CREADO);
            assertThat(profile.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create deja el id sin asignar, el perfil habilitado y con fecha de creacion")
        void create_deja_el_id_sin_asignar_y_el_perfil_habilitado() {
            CompanyTaxProfile profile = CompanyTaxProfile.create(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL,
                    CompanyTaxProfileMother.NOMBRE_COMERCIAL, CompanyTaxProfileMother.VETERINARIA,
                    List.of(CompanyTaxProfileMother.O13));

            assertThat(profile.getId()).isNull();
            assertThat(profile.isEnabled()).isTrue();
            assertThat(profile.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("create valida igual que el constructor y rechaza el correo fiscal invalido")
        void create_valida_igual_que_el_constructor() {
            assertThatThrownBy(() -> CompanyTaxProfile.create(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA,
                    "sin-arroba", null, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalEmail");
        }

        @Test
        @DisplayName("acepta perfil sin nombre comercial ni actividad economica")
        void acepta_perfil_sin_nombre_comercial_ni_actividad() {
            CompanyTaxProfile profile = new CompanyTaxProfile(null, CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL,
                    null, null, null, CompanyTaxProfileMother.CREADO, null, true);

            assertThat(profile.getCommercialName()).isNull();
            assertThat(profile.getEconomicActivity()).isNull();
        }
    }

    @Nested
    @DisplayName("Invariantes")
    class Invariantes {

        @Test
        @DisplayName("rechaza el perfil sin empresa")
        void rechaza_el_perfil_sin_empresa() {
            assertThatThrownBy(() -> perfil(null, CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON,
                    TaxRegime.RESPONSABLE_IVA, EMAIL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }

        @Test
        @DisplayName("rechaza el perfil sin tipo de documento")
        void rechaza_el_perfil_sin_tipo_de_documento() {
            assertThatThrownBy(() -> perfil(CompanyTaxProfileMother.CLINICA, null, DOCUMENTO, null,
                    RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyDocumentType is required");
        }

        @Test
        @DisplayName("rechaza el perfil sin regimen tributario")
        void rechaza_el_perfil_sin_regimen_tributario() {
            assertThatThrownBy(() -> perfil(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, null, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxRegime is required");
        }

        @ParameterizedTest(name = "documento=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("rechaza el numero de documento vacio o en blanco")
        void rechaza_el_numero_de_documento_en_blanco(String documento) {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            documento, DV, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyDocumentId is required");
        }

        @Test
        @DisplayName("rechaza un numero de documento de mas de 20 caracteres")
        void rechaza_un_numero_de_documento_demasiado_largo() {
            assertThatThrownBy(() -> perfil(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.CEDULA_CIUDADANIA, "1".repeat(21), null, RAZON,
                    TaxRegime.RESPONSABLE_IVA, EMAIL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("20 chars or less");
        }

        @Test
        @DisplayName("acepta un numero de documento de exactamente 20 caracteres")
        void acepta_un_numero_de_documento_de_veinte_caracteres() {
            CompanyTaxProfile profile = perfil(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.CEDULA_CIUDADANIA, "1".repeat(20), null, RAZON,
                    TaxRegime.RESPONSABLE_IVA, EMAIL);

            assertThat(profile.getCompanyDocumentId()).hasSize(20);
        }

        @ParameterizedTest(name = "razon social=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("rechaza la razon social vacia o en blanco")
        void rechaza_la_razon_social_en_blanco(String razon) {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, DV, razon, TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalName is required");
        }

        @Test
        @DisplayName("rechaza una razon social de mas de 255 caracteres")
        void rechaza_una_razon_social_demasiado_larga() {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, DV, "A".repeat(256), TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("255 chars or less");
        }

        @ParameterizedTest(name = "email=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("rechaza el correo fiscal vacio o en blanco")
        void rechaza_el_correo_fiscal_en_blanco(String email) {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, email))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalEmail is required");
        }

        @ParameterizedTest(name = "email=[{0}]")
        @ValueSource(strings = {"sin-arroba", "a@b", "@dominio.com", "usuario@", "usuario@dominio.",
                "usuario dos@dominio.com", "usuario@dom inio.com", "usuario@@dominio.com"})
        @DisplayName("rechaza los correos fiscales con formato invalido")
        void rechaza_los_correos_fiscales_con_formato_invalido(String email) {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, email))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valid email");
        }

        @ParameterizedTest(name = "email=[{0}]")
        @ValueSource(strings = {"a@b.c", "facturacion@vetnorte.com",
                "contabilidad.fiscal@sub.dominio.com.co"})
        @DisplayName("acepta los correos fiscales bien formados")
        void acepta_los_correos_fiscales_bien_formados(String email) {
            CompanyTaxProfile profile = perfil(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA,
                    email);

            assertThat(profile.getFiscalEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("rechaza el correo fiscal de mas de 255 caracteres antes de mirar el formato")
        void rechaza_el_correo_fiscal_demasiado_largo() {
            String email = "a".repeat(244) + "@dominio.com";

            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, email))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("255 chars or less");
        }

        @ParameterizedTest
        @EnumSource(TaxRegime.class)
        @DisplayName("acepta cualquier regimen tributario y lo conserva")
        void acepta_cualquier_regimen_tributario(TaxRegime regimen) {
            CompanyTaxProfile profile = perfil(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, regimen, EMAIL);

            assertThat(profile.getTaxRegime()).isEqualTo(regimen);
        }
    }

    @Nested
    @DisplayName("Digito de verificacion")
    class DigitoDeVerificacion {

        @Test
        @DisplayName("exige DV cuando el tipo de documento es NIT")
        void exige_dv_cuando_el_tipo_de_documento_es_nit() {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, null, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required when document type is NIT");
        }

        @ParameterizedTest(name = "dv=[{0}]")
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("un DV en blanco cuenta como ausente y el NIT lo rechaza")
        void un_dv_en_blanco_cuenta_como_ausente(String dv) {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, dv, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required when document type is NIT");
        }

        @ParameterizedTest(name = "dv=[{0}]")
        @ValueSource(strings = {"12", "a", "-", "08", "10"})
        @DisplayName("rechaza un DV que no sea un unico digito del 0 al 9")
        void rechaza_un_dv_que_no_sea_un_unico_digito(String dv) {
            assertThatThrownBy(
                    () -> perfil(CompanyTaxProfileMother.CLINICA, CompanyDocumentType.NIT,
                            DOCUMENTO, dv, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single digit 0-9");
        }

        @ParameterizedTest(name = "dv={0}")
        @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"})
        @DisplayName("acepta cualquier digito del 0 al 9 como DV del NIT")
        void acepta_cualquier_digito_del_0_al_9(String dv) {
            CompanyTaxProfile profile = perfil(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, dv, RAZON, TaxRegime.RESPONSABLE_IVA,
                    EMAIL);

            assertThat(profile.getCompanyDocumentVerificationDigit()).isEqualTo(dv);
        }

        @ParameterizedTest
        @EnumSource(value = CompanyDocumentType.class, names = "NIT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("prohibe el DV en cualquier tipo de documento que no sea NIT")
        void prohibe_el_dv_fuera_del_nit(CompanyDocumentType tipo) {
            assertThatThrownBy(() -> perfil(CompanyTaxProfileMother.CLINICA, tipo, DOCUMENTO, "8",
                    RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only allowed when document type is NIT");
        }

        @ParameterizedTest
        @EnumSource(value = CompanyDocumentType.class, names = "NIT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("acepta sin DV cualquier tipo de documento que no sea NIT")
        void acepta_sin_dv_fuera_del_nit(CompanyDocumentType tipo) {
            CompanyTaxProfile profile = perfil(CompanyTaxProfileMother.CLINICA, tipo, DOCUMENTO,
                    null, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL);

            assertThat(profile.getCompanyDocumentVerificationDigit()).isNull();
        }

        @Test
        @DisplayName("normaliza a null el DV en blanco de un documento que no es NIT")
        void normaliza_a_null_el_dv_en_blanco() {
            CompanyTaxProfile profile = perfil(CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.PASAPORTE, "AB12345", "   ", RAZON,
                    TaxRegime.RESPONSABLE_IVA, EMAIL);

            assertThat(profile.getCompanyDocumentVerificationDigit()).isNull();
        }
    }

    @Nested
    @DisplayName("Responsabilidades")
    class Responsabilidades {

        @Test
        @DisplayName("una lista nula se guarda como lista vacia, nunca como null")
        void una_lista_nula_se_guarda_como_lista_vacia() {
            CompanyTaxProfile profile = new CompanyTaxProfile(null, CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL,
                    null, null, null, CompanyTaxProfileMother.CREADO, null, true);

            assertThat(profile.getResponsibilities()).isEmpty();
        }

        @Test
        @DisplayName("copia la lista recibida: mutarla despues no altera el perfil")
        void copia_la_lista_recibida() {
            List<CompanyTaxProfileResponsibility> origen = new ArrayList<>(
                    List.of(CompanyTaxProfileMother.O13));
            CompanyTaxProfile profile = new CompanyTaxProfile(null, CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL,
                    null, null, origen, CompanyTaxProfileMother.CREADO, null, true);

            origen.add(CompanyTaxProfileMother.O15);

            assertThat(profile.getResponsibilities()).containsExactly(CompanyTaxProfileMother.O13);
        }

        @Test
        @DisplayName("expone las responsabilidades como lista inmutable")
        void expone_las_responsabilidades_como_lista_inmutable() {
            List<CompanyTaxProfileResponsibility> expuestas = CompanyTaxProfileMother.perfilNit()
                    .getResponsibilities();

            assertThatThrownBy(() -> expuestas.add(CompanyTaxProfileMother.O15))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("conserva el orden de las responsabilidades")
        void conserva_el_orden_de_las_responsabilidades() {
            CompanyTaxProfile profile = new CompanyTaxProfile(null, CompanyTaxProfileMother.CLINICA,
                    CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA, EMAIL,
                    null, null, List.of(CompanyTaxProfileMother.O15, CompanyTaxProfileMother.O13),
                    CompanyTaxProfileMother.CREADO, null, true);

            assertThat(profile.getResponsibilities()).containsExactly(CompanyTaxProfileMother.O15,
                    CompanyTaxProfileMother.O13);
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza los datos fiscales editables")
        void reemplaza_los_datos_fiscales_editables() {
            CompanyTaxProfile profile = perfilValido();

            profile.update(CompanyDocumentType.CEDULA_CIUDADANIA, "1020304050", null, "Ana Ruiz",
                    TaxRegime.NO_RESPONSABLE_IVA, "ana@ruiz.com", "Consultorio Ana",
                    CompanyTaxProfileMother.COMERCIO, List.of(CompanyTaxProfileMother.O15));

            assertThat(profile.getCompanyDocumentType())
                    .isEqualTo(CompanyDocumentType.CEDULA_CIUDADANIA);
            assertThat(profile.getCompanyDocumentId()).isEqualTo("1020304050");
            assertThat(profile.getCompanyDocumentVerificationDigit()).isNull();
            assertThat(profile.getLegalName()).isEqualTo("Ana Ruiz");
            assertThat(profile.getTaxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
            assertThat(profile.getFiscalEmail()).isEqualTo("ana@ruiz.com");
            assertThat(profile.getCommercialName()).isEqualTo("Consultorio Ana");
            assertThat(profile.getEconomicActivity()).isEqualTo(CompanyTaxProfileMother.COMERCIO);
            assertThat(profile.getResponsibilities()).containsExactly(CompanyTaxProfileMother.O15);
        }

        @Test
        @DisplayName("no toca id, empresa, fecha de creacion ni el estado habilitado")
        void no_toca_id_empresa_fecha_ni_estado() {
            CompanyTaxProfile profile = perfilValido();

            profile.update(CompanyDocumentType.NIT, "830053800", "4", "Otra Razon S.A.S.",
                    TaxRegime.NO_RESPONSABLE_IVA, "otro@correo.com", null, null, null);

            assertThat(profile.getId()).isEqualTo(CompanyTaxProfileMother.PROFILE_ID);
            assertThat(profile.getCompany()).isEqualTo(CompanyTaxProfileMother.CLINICA);
            assertThat(profile.getCreatedDate()).isEqualTo(CompanyTaxProfileMother.CREADO);
            assertThat(profile.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("permite limpiar nombre comercial, actividad economica y responsabilidades")
        void permite_limpiar_los_campos_opcionales() {
            CompanyTaxProfile profile = perfilValido();

            profile.update(CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA,
                    EMAIL, null, null, null);

            assertThat(profile.getCommercialName()).isNull();
            assertThat(profile.getEconomicActivity()).isNull();
            assertThat(profile.getResponsibilities()).isEmpty();
        }

        @Test
        @DisplayName("rechaza la actualizacion que deja el perfil sin DV siendo NIT")
        void rechaza_la_actualizacion_sin_dv_siendo_nit() {
            CompanyTaxProfile profile = perfilValido();

            assertThatThrownBy(() -> profile.update(CompanyDocumentType.NIT, DOCUMENTO, null, RAZON,
                    TaxRegime.RESPONSABLE_IVA, EMAIL, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required when document type is NIT");
        }

        @Test
        @DisplayName("una actualizacion invalida deja el perfil intacto")
        void una_actualizacion_invalida_deja_el_perfil_intacto() {
            CompanyTaxProfile profile = perfilValido();

            assertThatThrownBy(() -> profile.update(CompanyDocumentType.NIT, "830053800", "4",
                    "Otra Razon S.A.S.", TaxRegime.NO_RESPONSABLE_IVA, "sin-arroba", null, null,
                    null)).isInstanceOf(IllegalArgumentException.class);

            assertThat(profile.getCompanyDocumentId()).isEqualTo(DOCUMENTO);
            assertThat(profile.getLegalName()).isEqualTo(RAZON);
            assertThat(profile.getFiscalEmail()).isEqualTo(EMAIL);
            assertThat(profile.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
        }

        @Test
        @DisplayName("normaliza a null el DV en blanco al actualizar a un documento que no es NIT")
        void normaliza_a_null_el_dv_en_blanco_al_actualizar() {
            CompanyTaxProfile profile = perfilValido();

            profile.update(CompanyDocumentType.PASAPORTE, "AB12345", "  ", RAZON,
                    TaxRegime.RESPONSABLE_IVA, EMAIL, null, null, null);

            assertThat(profile.getCompanyDocumentVerificationDigit()).isNull();
        }

        @Test
        @DisplayName("copia la lista de responsabilidades recibida al actualizar")
        void copia_la_lista_de_responsabilidades_al_actualizar() {
            CompanyTaxProfile profile = perfilValido();
            List<CompanyTaxProfileResponsibility> origen = new ArrayList<>(
                    List.of(CompanyTaxProfileMother.O15));

            profile.update(CompanyDocumentType.NIT, DOCUMENTO, DV, RAZON, TaxRegime.RESPONSABLE_IVA,
                    EMAIL, null, null, origen);
            origen.clear();

            assertThat(profile.getResponsibilities()).containsExactly(CompanyTaxProfileMother.O15);
        }
    }

    @Nested
    @DisplayName("Habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable deja el perfil deshabilitado")
        void disable_deja_el_perfil_deshabilitado() {
            CompanyTaxProfile profile = perfilValido();

            profile.disable();

            assertThat(profile.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable reactiva un perfil deshabilitado")
        void enable_reactiva_un_perfil_deshabilitado() {
            CompanyTaxProfile profile = CompanyTaxProfileMother.perfilDeshabilitado();

            profile.enable();

            assertThat(profile.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("enable sobre un perfil ya habilitado es idempotente")
        void enable_sobre_un_perfil_ya_habilitado_es_idempotente() {
            CompanyTaxProfile profile = perfilValido();

            profile.enable();
            profile.enable();

            assertThat(profile.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("disable no borra los datos fiscales")
        void disable_no_borra_los_datos_fiscales() {
            CompanyTaxProfile profile = perfilValido();

            profile.disable();

            assertThat(profile.getCompanyDocumentId()).isEqualTo(DOCUMENTO);
            assertThat(profile.getCompanyDocumentVerificationDigit()).isEqualTo(DV);
            assertThat(profile.getLegalName()).isEqualTo(RAZON);
        }
    }
}
