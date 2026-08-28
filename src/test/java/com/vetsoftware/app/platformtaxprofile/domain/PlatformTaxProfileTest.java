package com.vetsoftware.app.platformtaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("PlatformTaxProfile")
class PlatformTaxProfileTest {

    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 1, 8, 0);
    private static final EconomicActivityRef ACTIVIDAD = new EconomicActivityRef(11L, "6201",
            "Desarrollo de software");

    private static PlatformTaxProfile valido() {
        return new PlatformTaxProfile(null, PlatformDocumentType.NIT, "900123456", "7",
                "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                "VetSoftware", ACTIVIDAD, true, VALID_FROM, null, CREADO, null);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            PlatformTaxProfile perfil = valido();

            assertThat(perfil.getDocumentType()).isEqualTo(PlatformDocumentType.NIT);
            assertThat(perfil.getDocumentId()).isEqualTo("900123456");
            assertThat(perfil.getVerificationDigit()).isEqualTo("7");
            assertThat(perfil.getLegalName()).isEqualTo("VetSoftware SAS");
            assertThat(perfil.getTaxRegime()).isEqualTo(PlatformTaxRegime.RESPONSABLE_IVA);
            assertThat(perfil.getFiscalEmail()).isEqualTo("facturacion@vetsoftware.com");
            assertThat(perfil.getCommercialName()).isEqualTo("VetSoftware");
            assertThat(perfil.getEconomicActivity()).isEqualTo(ACTIVIDAD);
            assertThat(perfil.isSelfWithholder()).isTrue();
            assertThat(perfil.getValidFrom()).isEqualTo(VALID_FROM);
            assertThat(perfil.getValidTo()).isNull();
            assertThat(perfil.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("documentType es obligatorio")
        void documentType_es_obligatorio() {
            assertThatThrownBy(
                    () -> new PlatformTaxProfile(null, null, "900123456", "7", "VetSoftware SAS",
                            PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com", null,
                            null, true, VALID_FROM, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("documentType is required");
        }

        @Test
        @DisplayName("documentId no puede estar en blanco")
        void documentId_no_puede_estar_en_blanco() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT, "  ",
                    "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("documentId is required");
        }

        @Test
        @DisplayName("documentId de mas de 20 caracteres se rechaza")
        void documentId_de_mas_de_20_caracteres_se_rechaza() {
            String largo = "1".repeat(21);
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT, largo,
                    null, "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("documentId must be 20 chars or less");
        }

        @Test
        @DisplayName("documentId no ascii se rechaza aunque la columna lo admitiera")
        void documentId_no_ascii_se_rechaza() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456—", null, "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("documentId must be ASCII");
        }

        @Test
        @DisplayName("el NIT admite digito de verificacion")
        void el_nit_admite_digito_de_verificacion() {
            PlatformTaxProfile perfil = valido();

            assertThat(perfil.getVerificationDigit()).isEqualTo("7");
        }

        @Test
        @DisplayName("el NIT tambien puede no llevar digito de verificacion")
        void el_nit_puede_no_llevar_digito() {
            PlatformTaxProfile perfil = new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", null, "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null);

            assertThat(perfil.getVerificationDigit()).isNull();
        }

        @ParameterizedTest(name = "{0} rechaza el digito de verificacion")
        @EnumSource(value = PlatformDocumentType.class, names = "NIT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("solo el NIT admite digito de verificacion")
        void solo_el_nit_admite_digito_de_verificacion(PlatformDocumentType tipo) {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, tipo, "12345678", "3", "Alguien",
                    PlatformTaxRegime.RESPONSABLE_IVA, "correo@vetsoftware.com", null, null, true,
                    VALID_FROM, null, CREADO, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "verificationDigit must be absent unless the document type is NIT");
        }

        @Test
        @DisplayName("el digito de verificacion tiene que ser un solo caracter")
        void el_digito_de_verificacion_tiene_que_ser_un_solo_caracter() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "77", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verificationDigit must be a single character");
        }

        @Test
        @DisplayName("el digito de verificacion tiene que ser un digito")
        void el_digito_de_verificacion_tiene_que_ser_un_digito() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "x", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verificationDigit must be a digit");
        }

        @Test
        @DisplayName("legalName no puede estar en blanco")
        void legal_name_no_puede_estar_en_blanco() {
            assertThatThrownBy(
                    () -> new PlatformTaxProfile(null, PlatformDocumentType.NIT, "900123456", "7",
                            " ", PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                            null, null, true, VALID_FROM, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalName is required");
        }

        @Test
        @DisplayName("legalName de mas de 255 caracteres se rechaza")
        void legal_name_de_mas_de_255_caracteres_se_rechaza() {
            String largo = "A".repeat(256);
            assertThatThrownBy(
                    () -> new PlatformTaxProfile(null, PlatformDocumentType.NIT, "900123456", "7",
                            largo, PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                            null, null, true, VALID_FROM, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalName must be 255 chars or less");
        }

        @Test
        @DisplayName("taxRegime es obligatorio")
        void tax_regime_es_obligatorio() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", null, "facturacion@vetsoftware.com", null,
                    null, true, VALID_FROM, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxRegime is required");
        }

        @Test
        @DisplayName("fiscalEmail no puede estar en blanco")
        void fiscal_email_no_puede_estar_en_blanco() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA, " ",
                    null, null, true, VALID_FROM, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalEmail is required");
        }

        @Test
        @DisplayName("fiscalEmail sin forma de correo se rechaza")
        void fiscal_email_sin_forma_de_correo_se_rechaza() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "no-es-un-correo", null, null, true, VALID_FROM, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalEmail must look like an email address");
        }

        @Test
        @DisplayName("commercialName de mas de 150 caracteres se rechaza")
        void commercial_name_de_mas_de_150_caracteres_se_rechaza() {
            String largo = "A".repeat(151);
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", largo, null, true, VALID_FROM, null, CREADO,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("commercialName must be 150 chars or less");
        }

        @Test
        @DisplayName("commercialName es opcional")
        void commercial_name_es_opcional() {
            PlatformTaxProfile perfil = new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null);

            assertThat(perfil.getCommercialName()).isNull();
        }

        @Test
        @DisplayName("createdDate es obligatoria")
        void created_date_es_obligatoria() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdDate is required");
        }
    }

    @Nested
    @DisplayName("la actividad economica es opcional en los dos sentidos")
    class ActividadEconomicaOpcional {

        @Test
        @DisplayName("null entra sin construir un ref vacio")
        void null_entra_sin_construir_un_ref_vacio() {
            PlatformTaxProfile perfil = new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null);

            assertThat(perfil.getEconomicActivity()).isNull();
        }

        @Test
        @DisplayName("una actividad resuelta se conserva tal cual")
        void una_actividad_resuelta_se_conserva_tal_cual() {
            PlatformTaxProfile perfil = valido();

            assertThat(perfil.getEconomicActivity()).isEqualTo(ACTIVIDAD);
        }
    }

    @Nested
    @DisplayName("la ventana de vigencia")
    class VentanaDeVigencia {

        @Test
        @DisplayName("validFrom es obligatorio")
        void valid_from_es_obligatorio() {
            assertThatThrownBy(() -> new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, null, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validFrom is required");
        }

        @Test
        @DisplayName("validTo igual a validFrom no es representable")
        void valid_to_igual_a_valid_from_no_es_representable() {
            assertThatThrownBy(() -> new PlatformTaxProfile(PROFILE_ID_CUALQUIERA,
                    PlatformDocumentType.NIT, "900123456", "7", "VetSoftware SAS",
                    PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com", null, null,
                    true, VALID_FROM, VALID_FROM, CREADO, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validTo must be after validFrom");
        }

        @Test
        @DisplayName("validTo anterior a validFrom no es representable")
        void valid_to_anterior_a_valid_from_no_es_representable() {
            assertThatThrownBy(() -> new PlatformTaxProfile(PROFILE_ID_CUALQUIERA,
                    PlatformDocumentType.NIT, "900123456", "7", "VetSoftware SAS",
                    PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com", null, null,
                    true, VALID_FROM, VALID_FROM.minusDays(1), CREADO, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validTo must be after validFrom");
        }

        @Test
        @DisplayName("validTo estrictamente posterior si es una ventana valida")
        void valid_to_estrictamente_posterior_es_valido() {
            PlatformTaxProfile cerrada = new PlatformTaxProfile(PROFILE_ID_CUALQUIERA,
                    PlatformDocumentType.NIT, "900123456", "7", "VetSoftware SAS",
                    PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com", null, null,
                    true, VALID_FROM, VALID_FROM.plusDays(1), CREADO, 0L);

            assertThat(cerrada.isCurrent()).isFalse();
        }

        private static final Long PROFILE_ID_CUALQUIERA = 1L;
    }

    @Nested
    @DisplayName("open")
    class Open {

        @Test
        @DisplayName("nace vigente y sin id")
        void nace_vigente_y_sin_id() {
            PlatformTaxProfile perfil = PlatformTaxProfile.open(PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", "VetSoftware", ACTIVIDAD, true, VALID_FROM,
                    CREADO);

            assertThat(perfil.getId()).isNull();
            assertThat(perfil.getValidTo()).isNull();
            assertThat(perfil.isCurrent()).isTrue();
        }
    }

    @Nested
    @DisplayName("closeOn")
    class CloseOn {

        @Test
        @DisplayName("cierra la vigente en la fecha dada")
        void cierra_la_vigente_en_la_fecha_dada() {
            PlatformTaxProfile perfil = valido();
            LocalDate cierre = VALID_FROM.plusMonths(6);

            perfil.closeOn(cierre);

            assertThat(perfil.getValidTo()).isEqualTo(cierre);
            assertThat(perfil.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("una ficha ya cerrada no se puede volver a cerrar")
        void una_ficha_ya_cerrada_no_se_puede_volver_a_cerrar() {
            PlatformTaxProfile cerrada = new PlatformTaxProfile(1L, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM,
                    VALID_FROM.plusDays(30), CREADO, 0L);

            assertThatThrownBy(() -> cerrada.closeOn(VALID_FROM.plusDays(60)))
                    .isInstanceOf(PlatformTaxProfileAlreadyClosedException.class)
                    .hasMessageContaining("was already closed");
        }

        @Test
        @DisplayName("effectiveFrom es obligatorio")
        void effective_from_es_obligatorio() {
            PlatformTaxProfile perfil = valido();

            assertThatThrownBy(() -> perfil.closeOn(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("effectiveFrom is required");
        }

        @Test
        @DisplayName("la sucesion en el mismo dia no es representable")
        void la_sucesion_en_el_mismo_dia_no_es_representable() {
            PlatformTaxProfile perfil = valido();

            assertThatThrownBy(() -> perfil.closeOn(VALID_FROM))
                    .isInstanceOf(PlatformTaxProfileSuccessionNotAfterCurrentException.class)
                    .hasMessageContaining("cannot be succeeded");
        }

        @Test
        @DisplayName("la sucesion antes de validFrom tampoco es representable")
        void la_sucesion_antes_de_valid_from_tampoco_es_representable() {
            PlatformTaxProfile perfil = valido();

            assertThatThrownBy(() -> perfil.closeOn(VALID_FROM.minusDays(1)))
                    .isInstanceOf(PlatformTaxProfileSuccessionNotAfterCurrentException.class);
        }
    }

    @Nested
    @DisplayName("isEffectiveOn")
    class IsEffectiveOn {

        @Test
        @DisplayName("el limite inferior es inclusivo")
        void el_limite_inferior_es_inclusivo() {
            PlatformTaxProfile perfil = valido();

            assertThat(perfil.isEffectiveOn(VALID_FROM)).isTrue();
        }

        @Test
        @DisplayName("un dia antes de validFrom no aplica")
        void un_dia_antes_de_valid_from_no_aplica() {
            PlatformTaxProfile perfil = valido();

            assertThat(perfil.isEffectiveOn(VALID_FROM.minusDays(1))).isFalse();
        }

        @Test
        @DisplayName("el limite superior es exclusivo")
        void el_limite_superior_es_exclusivo() {
            LocalDate validTo = VALID_FROM.plusDays(30);
            PlatformTaxProfile cerrada = new PlatformTaxProfile(1L, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, validTo, CREADO,
                    0L);

            assertThat(cerrada.isEffectiveOn(validTo)).isFalse();
            assertThat(cerrada.isEffectiveOn(validTo.minusDays(1))).isTrue();
        }
    }

    @Nested
    @DisplayName("formattedDocumentId")
    class FormattedDocumentId {

        @Test
        @DisplayName("un NIT con digito lo imprime con guion")
        void un_nit_con_digito_lo_imprime_con_guion() {
            PlatformTaxProfile perfil = valido();

            assertThat(perfil.formattedDocumentId()).isEqualTo("900123456-7");
        }

        @Test
        @DisplayName("sin digito de verificacion se imprime pelado")
        void sin_digito_de_verificacion_se_imprime_pelado() {
            PlatformTaxProfile perfil = new PlatformTaxProfile(null, PlatformDocumentType.NIT,
                    "900123456", null, "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.com", null, null, true, VALID_FROM, null, CREADO,
                    null);

            assertThat(perfil.formattedDocumentId()).isEqualTo("900123456");
        }
    }
}
