package com.vetsoftware.app.platformtaxprofile.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.platformtaxprofile.application.command.OpenPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.command.SucceedPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformEconomicActivitySummaryDto;
import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.in.FindCurrentPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.FindPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.ListPlatformTaxProfilesUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.OpenPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.SucceedPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.domain.NoCurrentPlatformTaxProfileException;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web de la identidad fiscal de la plataforma.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Los diez campos del cuerpo llegan al command en su posicion.</b> Cinco
 * son cadenas —{@code documentId}, {@code verificationDigit} y
 * {@code legalName} contiguas, y {@code fiscalEmail} con {@code commercialName}
 * detras del regimen— y cruzar dos de ellas compila sin una queja. El resultado
 * no seria un error visible: seria una factura con la razon social en el campo
 * del correo fiscal. Por eso el caso feliz captura el command y compara
 * componente a componente con valores todos distintos.</li>
 * <li><b>La tabla vacia contesta 503 y no 404 ni 409.</b> Es el codigo que
 * distingue «no hay identidad fiscal configurada todavia» de «el recurso que
 * pediste no existe», y el que le dice a quien opera que falta una decision
 * humana, no un id.</li>
 * <li><b>No existe {@code PUT} ni {@code DELETE}.</b> Ese hueco es el diseño,
 * no una omision: la ficha se sucede, no se edita ni se borra.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO prueba: la autorizacion.</b>
 * {@code WebMvcSliceConfig} sustituye la cadena de seguridad por una permisiva
 * —la real necesita Redis y base de datos—, asi que aqui un 403 no se puede
 * provocar. Que los cinco puertos esten cerrados a {@code hasRole('SYSTEM')} a
 * secas lo verifica ArchUnit sobre las interfaces de {@code port/in}, que es
 * donde vive el gate.
 */
@WebMvcTest(SystemPlatformTaxProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemPlatformTaxProfileController — contrato HTTP de la identidad fiscal")
class SystemPlatformTaxProfileControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate RELEVO = LocalDate.of(2027, 1, 1);

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OpenPlatformTaxProfileUseCase openUseCase;
    @MockitoBean
    private SucceedPlatformTaxProfileUseCase succeedUseCase;
    @MockitoBean
    private FindCurrentPlatformTaxProfileUseCase findCurrentUseCase;
    @MockitoBean
    private FindPlatformTaxProfileUseCase findUseCase;
    @MockitoBean
    private ListPlatformTaxProfilesUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("responde 201 y traslada los diez campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_diez_campos_sin_cruzarlos() throws Exception {
            when(openUseCase.execute(any())).thenReturn(dto(1L, "VetSoftware S.A.S.", DESDE, null));

            mockMvc.perform(post("/system/platform-tax-profiles")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"documentType":"NIT","documentId":"900123456",
                             "verificationDigit":"7","legalName":"VetSoftware S.A.S.",
                             "taxRegime":"RESPONSABLE_IVA",
                             "fiscalEmail":"facturacion@vetsoftware.co",
                             "commercialName":"VetSoftware","economicActivityId":55,
                             "selfWithholder":true,"validFrom":"2026-01-01"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.legalName").value("VetSoftware S.A.S."))
                    .andExpect(jsonPath("$.validTo").doesNotExist());

            ArgumentCaptor<OpenPlatformTaxProfileCommand> captor = ArgumentCaptor
                    .forClass(OpenPlatformTaxProfileCommand.class);
            verify(openUseCase).execute(captor.capture());
            assertThat(captor.getValue()).satisfies(command -> {
                assertThat(command.documentType()).isEqualTo(PlatformDocumentType.NIT);
                assertThat(command.documentId()).isEqualTo("900123456");
                assertThat(command.verificationDigit()).isEqualTo("7");
                assertThat(command.legalName()).isEqualTo("VetSoftware S.A.S.");
                assertThat(command.taxRegime()).isEqualTo(PlatformTaxRegime.RESPONSABLE_IVA);
                assertThat(command.fiscalEmail()).isEqualTo("facturacion@vetsoftware.co");
                assertThat(command.commercialName()).isEqualTo("VetSoftware");
                assertThat(command.economicActivityId()).isEqualTo(55L);
                assertThat(command.selfWithholder()).isTrue();
                assertThat(command.validFrom()).isEqualTo(DESDE);
            });
        }

        @Test
        @DisplayName("la actividad economica es opcional: sin ella el command la lleva nula")
        void la_actividad_economica_es_opcional() throws Exception {
            when(openUseCase.execute(any())).thenReturn(dto(1L, "VetSoftware S.A.S.", DESDE, null));

            mockMvc.perform(post("/system/platform-tax-profiles")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"documentType":"NIT","documentId":"900123456",
                             "verificationDigit":"7","legalName":"VetSoftware S.A.S.",
                             "taxRegime":"RESPONSABLE_IVA",
                             "fiscalEmail":"facturacion@vetsoftware.co",
                             "selfWithholder":false,"validFrom":"2026-01-01"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.economicActivity").doesNotExist());

            ArgumentCaptor<OpenPlatformTaxProfileCommand> captor = ArgumentCaptor
                    .forClass(OpenPlatformTaxProfileCommand.class);
            verify(openUseCase).execute(captor.capture());
            assertThat(captor.getValue().economicActivityId()).isNull();
            assertThat(captor.getValue().selfWithholder()).isFalse();
        }

        @Test
        @DisplayName("un cuerpo sin tipo, sin documento, sin razon social ni fecha sale 400 nombrandolos")
        void un_cuerpo_incompleto_sale_400_nombrando_los_campos() throws Exception {
            mockMvc.perform(post("/system/platform-tax-profiles")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"taxRegime":"RESPONSABLE_IVA",
                             "fiscalEmail":"facturacion@vetsoftware.co"}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    // hasItems y NO containsInAnyOrder, y el motivo es un hallazgo:
                    // cuando el cuerpo omite un componente PRIMITIVO del record, el 400
                    // lo nombra tambien -selfWithholder aqui-, aunque no lleve
                    // @NotNull (un primitivo no puede llevarlo). Exigir la lista exacta
                    // ataria este caso a ese detalle del binder en vez de a lo que de
                    // verdad afirma: que los obligatorios salen nombrados.
                    .andExpect(jsonPath("$.errors[*].field", Matchers.hasItems("documentType",
                            "documentId", "legalName", "validFrom")));

            // El @Valid para el cuerpo ANTES de llegar al caso de uso. Sin la
            // anotacion, el binder no dispara el validador y estas restricciones
            // estarian escritas sin evaluarse nunca (#135).
            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un digito de verificacion que no es una cifra sale 400 y no llega a la factura")
        void un_digito_de_verificacion_no_numerico_sale_400() throws Exception {
            mockMvc.perform(post("/system/platform-tax-profiles")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"documentType":"NIT","documentId":"900123456",
                             "verificationDigit":"X","legalName":"VetSoftware S.A.S.",
                             "taxRegime":"RESPONSABLE_IVA",
                             "fiscalEmail":"facturacion@vetsoftware.co",
                             "selfWithholder":true,"validFrom":"2026-01-01"}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(
                            jsonPath("$.errors[*].field", Matchers.hasItem("verificationDigit")));

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un regimen que no existe se rechaza en el binder")
        void un_regimen_inexistente_se_rechaza_en_el_binder() throws Exception {
            mockMvc.perform(post("/system/platform-tax-profiles")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"documentType":"NIT","documentId":"900123456",
                             "legalName":"VetSoftware S.A.S.","taxRegime":"COMUN",
                             "fiscalEmail":"facturacion@vetsoftware.co",
                             "selfWithholder":true,"validFrom":"2026-01-01"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(openUseCase);
        }
    }

    @Nested
    @DisplayName("Sucesion")
    class Sucesion {

        @Test
        @DisplayName("POST /succession responde 201 y devuelve la sucesora, no la ficha reemplazada")
        void la_sucesion_responde_201_con_la_sucesora() throws Exception {
            // 201 y no 200: la operacion CREA una fila y deja la anterior intacta.
            // Un PUT con 200 anunciaria a la consola que la ficha se reemplaza, que
            // es justo lo que no pasa.
            when(succeedUseCase.execute(any()))
                    .thenReturn(dto(2L, "VetSoftware Colombia S.A.S.", RELEVO, null));

            mockMvc.perform(post("/system/platform-tax-profiles/succession")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"documentType":"NIT","documentId":"900123456",
                             "verificationDigit":"7",
                             "legalName":"VetSoftware Colombia S.A.S.",
                             "taxRegime":"RESPONSABLE_IVA",
                             "fiscalEmail":"facturacion@vetsoftware.co",
                             "selfWithholder":true,"effectiveFrom":"2027-01-01"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.legalName").value("VetSoftware Colombia S.A.S."))
                    .andExpect(jsonPath("$.validFrom").value("2027-01-01"));

            ArgumentCaptor<SucceedPlatformTaxProfileCommand> captor = ArgumentCaptor
                    .forClass(SucceedPlatformTaxProfileCommand.class);
            verify(succeedUseCase).execute(captor.capture());
            assertThat(captor.getValue().effectiveFrom()).isEqualTo(RELEVO);
            assertThat(captor.getValue().legalName()).isEqualTo("VetSoftware Colombia S.A.S.");
        }

        @Test
        @DisplayName("una sucesion sin fecha de relevo sale 400: de ella depende que razon social se imprime")
        void una_sucesion_sin_fecha_sale_400() throws Exception {
            mockMvc.perform(post("/system/platform-tax-profiles/succession")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"documentType":"NIT","documentId":"900123456",
                             "legalName":"VetSoftware Colombia S.A.S.",
                             "taxRegime":"RESPONSABLE_IVA",
                             "fiscalEmail":"facturacion@vetsoftware.co",
                             "selfWithholder":true}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("effectiveFrom")));

            verifyNoInteractions(succeedUseCase);
        }
    }

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("GET /current devuelve la vigente con validTo ausente")
        void current_devuelve_la_vigente() throws Exception {
            when(findCurrentUseCase.findCurrent()).thenReturn(dto(1L, "VetSoftware S.A.S.", DESDE,
                    new PlatformEconomicActivitySummaryDto(55L, "6201", "Software")));

            mockMvc.perform(get("/system/platform-tax-profiles/current")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.legalName").value("VetSoftware S.A.S."))
                    .andExpect(jsonPath("$.validTo").doesNotExist())
                    .andExpect(jsonPath("$.economicActivity.code").value("6201"))
                    // La columna generada current_profile_marker NO sale por HTTP:
                    // es detalle del motor. Lo que el front usa es validTo == null.
                    .andExpect(jsonPath("$.currentProfileMarker").doesNotExist())
                    // Ni la version: es barandilla del que escribe, no un dato.
                    .andExpect(jsonPath("$.version").doesNotExist());
        }

        @Test
        @DisplayName("sin identidad sembrada, GET /current sale 503 — no 404 ni 409")
        void sin_identidad_sembrada_sale_503() throws Exception {
            // Es el estado REAL de la tabla hoy: el changeset 367 no la sembro
            // porque no habia razon social ni NIT reales de VetSoftware y no se
            // inventaron. 503 y no 404 porque no falta el recurso que se pidio,
            // falta el suelo sobre el que se apoya la emision — el mismo criterio
            // que PlatformBillingConfigNotConfiguredException y
            // PlatformCatalogNotConfiguredException.
            when(findCurrentUseCase.findCurrent())
                    .thenThrow(new NoCurrentPlatformTaxProfileException());

            mockMvc.perform(get("/system/platform-tax-profiles/current"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("PLATFORM_TAX_PROFILE_NOT_CONFIGURED"));
        }

        @Test
        @DisplayName("GET /{id} devuelve una ficha del historico, con su validTo escrito")
        void find_by_id_devuelve_una_ficha_cerrada() throws Exception {
            // Es el camino por el que una factura vieja resuelve con que identidad
            // se emitio, a traves de subscription_billing_documents.
            PlatformTaxProfileDto cerrada = new PlatformTaxProfileDto(1L, PlatformDocumentType.NIT,
                    "900123456", "7", "VetSoftware S.A.S.", PlatformTaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetsoftware.co", "VetSoftware", null, true, DESDE, RELEVO,
                    LocalDateTime.of(2026, 1, 1, 8, 30));
            when(findUseCase.findById(1L)).thenReturn(cerrada);

            mockMvc.perform(get("/system/platform-tax-profiles/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.legalName").value("VetSoftware S.A.S."))
                    .andExpect(jsonPath("$.validTo").value("2027-01-01"));
        }

        @Test
        @DisplayName("el historico sale paginado y con la vigente primero")
        void el_historico_sale_paginado() throws Exception {
            when(listUseCase.listAll(0, 20)).thenReturn(
                    new PageResult<>(List.of(dto(2L, "VetSoftware Colombia S.A.S.", RELEVO, null),
                            dto(1L, "VetSoftware S.A.S.", DESDE, null)), 0, 20, 2, 1));

            mockMvc.perform(get("/system/platform-tax-profiles")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(
                            jsonPath("$.content[0].legalName").value("VetSoftware Colombia S.A.S."))
                    .andExpect(jsonPath("$.content[1].legalName").value("VetSoftware S.A.S."));
        }
    }

    private static PlatformTaxProfileDto dto(Long id, String razonSocial, LocalDate desde,
            PlatformEconomicActivitySummaryDto actividad) {
        return new PlatformTaxProfileDto(id, PlatformDocumentType.NIT, "900123456", "7",
                razonSocial, PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.co",
                "VetSoftware", actividad, true, desde, null, LocalDateTime.of(2026, 1, 1, 8, 30));
    }
}
