package com.vetsoftware.app.companybillingprofile.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.companybillingprofile.application.command.OpenCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.command.SucceedCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.in.FindCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.FindCurrentCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.ListCompanyBillingProfilesUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.OpenCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.SucceedCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.util.List;
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
 * Rodaja HTTP de la ficha de facturacion: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <h2>Tres cosas congela esta clase que ningun test de servicio ve</h2>
 *
 * <ul>
 * <li><b>Que NO existe un {@code PUT} ni un {@code DELETE}.</b> La ausencia es
 * el diseño —una ficha no se edita ni se borra, se sucede y se cierra— y una
 * ausencia no la protege nada salvo un caso que la afirme. El dia que alguien
 * añada el {@code PUT} «que faltaba», estos dos casos se ponen rojos y le
 * enseñan el javadoc del controller.</li>
 * <li><b>Que el {@code companyId} del cuerpo se ignora.</b> Aunque un cliente
 * lo mande, el command sale con el de {@code Authz}. Es la defensa que
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} describe, ejercitada de verdad.</li>
 * <li><b>Que {@code withholdingAgent} ausente es un 400 y no un {@code false}
 * silencioso.</b> Con un {@code boolean} primitivo en el request, el binder lo
 * enlazaria a {@code false} y la ficha quedaria afirmando algo sobre dinero que
 * nadie dijo. El caso lo caza en el binder, que es donde el defecto
 * viviria.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta rodaja NO comprueba todavia</b> son los codigos 404 y 409 de
 * las excepciones de dominio nuevas: dependen de que
 * {@code GlobalExceptionHandler} las registre, y ese fichero es compartido. En
 * cuanto esten anotadas alli, este es el sitio donde añadir esos casos.
 */
@WebMvcTest(CompanyBillingProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyBillingProfileController — contrato HTTP de la ficha de facturacion")
class CompanyBillingProfileControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_SOCIEDAD = """
            {"personKind":"LEGAL","taxIdKind":"NIT","taxId":"900123456",
             "verificationDigit":"7","legalName":"Inversiones Pet SAS",
             "address":"Calle 10 # 43-51 oficina 704","cityId":900,
             "billingEmail":"facturacion@inversionespet.com.co","taxRegime":"COMMON",
             "withholdingAgent":true,"validFrom":"2026-01-15"}
            """;

    private static final String CUERPO_SUCESION = """
            {"personKind":"NATURAL","taxIdKind":"CC","taxId":"43215678",
             "firstName":"Ana","middleName":"Maria","lastName":"Ruiz",
             "secondLastName":"Cardona","address":"Carrera 43A # 1-50",
             "cityId":900,"billingEmail":"ana@correo.com","taxRegime":"SIMPLE",
             "withholdingAgent":false,"effectiveFrom":"2026-04-01"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenCompanyBillingProfileUseCase openUseCase;
    @MockitoBean
    private SucceedCompanyBillingProfileUseCase succeedUseCase;
    @MockitoBean
    private FindCurrentCompanyBillingProfileUseCase findCurrentUseCase;
    @MockitoBean
    private FindCompanyBillingProfileUseCase findUseCase;
    @MockitoBean
    private ListCompanyBillingProfilesUseCase listUseCase;

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("POST responde 201 y traslada cada campo del cuerpo al command sin cruzarlos")
        void post_responde_201_y_traslada_cada_campo() throws Exception {
            when(openUseCase.execute(any())).thenReturn(unaSociedad());

            mockMvc.perform(post("/company-billing-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_SOCIEDAD)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.taxId").value("900123456"))
                    .andExpect(jsonPath("$.personKind").value("LEGAL"))
                    .andExpect(jsonPath("$.city.name").value("Medellin"))
                    .andExpect(jsonPath("$.withholdingAgent").value(true))
                    .andExpect(jsonPath("$.validTo").doesNotExist());

            ArgumentCaptor<OpenCompanyBillingProfileCommand> command = ArgumentCaptor
                    .forClass(OpenCompanyBillingProfileCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.personKind()).isEqualTo(PersonKind.LEGAL);
                assertThat(cmd.taxIdKind()).isEqualTo(TaxIdKind.NIT);
                assertThat(cmd.taxId()).isEqualTo("900123456");
                assertThat(cmd.verificationDigit()).isEqualTo("7");
                assertThat(cmd.legalName()).isEqualTo("Inversiones Pet SAS");
                assertThat(cmd.address()).isEqualTo("Calle 10 # 43-51 oficina 704");
                assertThat(cmd.cityId()).isEqualTo(900L);
                assertThat(cmd.billingEmail()).isEqualTo("facturacion@inversionespet.com.co");
                assertThat(cmd.taxRegime()).isEqualTo(TaxRegime.COMMON);
                assertThat(cmd.withholdingAgent()).isTrue();
                assertThat(cmd.validFrom()).isEqualTo(LocalDate.of(2026, 1, 15));
            });
        }

        @Test
        @DisplayName("la respuesta NO publica el companyId, ni el enabled, ni la version")
        void la_respuesta_no_publica_el_company_id_ni_el_enabled() throws Exception {
            // El cliente ya sabe de que empresa es —el backend lo deriva del token— y
            // devolverlo invita a reenviarlo. `enabled` no se publica porque no hay forma
            // de cambiarlo: lo que cierra una ficha es validTo. Y `version` es la
            // barandilla del bloqueo optimista, no un dato del expediente.
            when(findCurrentUseCase.findCurrent(COMPANY_ID)).thenReturn(unaSociedad());

            mockMvc.perform(get("/company-billing-profile")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyId").doesNotExist())
                    .andExpect(jsonPath("$.enabled").doesNotExist())
                    .andExpect(jsonPath("$.version").doesNotExist());
        }

        @Test
        @DisplayName("la persona natural viaja con sus cuatro campos de nombre separados")
        void la_persona_natural_viaja_con_sus_cuatro_campos() throws Exception {
            when(succeedUseCase.execute(any())).thenReturn(unaPersonaNatural());

            mockMvc.perform(post("/company-billing-profile/succession")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_SUCESION))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.firstName").value("Ana"))
                    .andExpect(jsonPath("$.middleName").value("Maria"))
                    .andExpect(jsonPath("$.lastName").value("Ruiz"))
                    .andExpect(jsonPath("$.secondLastName").value("Cardona"))
                    .andExpect(jsonPath("$.legalName").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Sucesion")
    class Sucesion {

        @Test
        @DisplayName("POST /succession responde 201 y traslada effectiveFrom al command")
        void post_succession_responde_201_y_traslada_effective_from() throws Exception {
            when(succeedUseCase.execute(any())).thenReturn(unaPersonaNatural());

            mockMvc.perform(post("/company-billing-profile/succession")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_SUCESION))
                    .andExpect(status().isCreated());

            ArgumentCaptor<SucceedCompanyBillingProfileCommand> command = ArgumentCaptor
                    .forClass(SucceedCompanyBillingProfileCommand.class);
            verify(succeedUseCase).execute(command.capture());
            assertThat(command.getValue().effectiveFrom()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(command.getValue().personKind()).isEqualTo(PersonKind.NATURAL);
            assertThat(command.getValue().withholdingAgent()).isFalse();
        }

        @Test
        @DisplayName("NO existe un PUT sobre la ficha: una ficha no se reescribe")
        void no_existe_un_put_sobre_la_ficha() throws Exception {
            // La ausencia es el diseño: reescribir la fila cambiaria hacia atras a quien
            // se le emitieron las facturas anteriores. Este caso es lo unico que protege
            // esa ausencia.
            mockMvc.perform(put("/company-billing-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_SOCIEDAD)).andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(openUseCase, succeedUseCase);
        }

        @Test
        @DisplayName("NO existe un PATCH sobre la ficha")
        void no_existe_un_patch_sobre_la_ficha() throws Exception {
            mockMvc.perform(patch("/company-billing-profile")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_SOCIEDAD))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(openUseCase, succeedUseCase);
        }

        @Test
        @DisplayName("NO existe un DELETE: una ficha se cierra con valid_to, no se borra")
        void no_existe_un_delete() throws Exception {
            mockMvc.perform(delete("/company-billing-profile/7"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("Lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET devuelve la ficha vigente de la empresa del contexto")
        void get_devuelve_la_ficha_vigente() throws Exception {
            when(findCurrentUseCase.findCurrent(COMPANY_ID)).thenReturn(unaSociedad());

            mockMvc.perform(get("/company-billing-profile")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.city.id").value(900))
                    .andExpect(jsonPath("$.validTo").doesNotExist());

            verify(findCurrentUseCase).findCurrent(COMPANY_ID);
        }

        @Test
        @DisplayName("GET /history devuelve la pagina con sus totales y NO cae en el mapeo de /{id}")
        void get_history_devuelve_la_pagina() throws Exception {
            // Son dos rutas que compiten y la resolucion la decide Spring, no el orden en
            // que estan escritas: si algun dia cambiara, el historico contestaria un 400
            // de conversion de tipo sobre el id "history".
            when(listUseCase.listByCompany(COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(unaSociedad()), 0, 20, 3L, 1));

            mockMvc.perform(get("/company-billing-profile/history")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(42))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20));

            verifyNoInteractions(findUseCase);
        }

        @Test
        @DisplayName("GET /history traslada page y pageSize tal cual al caso de uso")
        void get_history_traslada_la_paginacion() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID, 2, 5)).thenReturn(PageResult.empty(2, 5));

            mockMvc.perform(get("/company-billing-profile/history").param("page", "2")
                    .param("pageSize", "5")).andExpect(status().isOk());

            verify(listUseCase).listByCompany(COMPANY_ID, 2, 5);
        }

        @Test
        @DisplayName("GET /{id} acota por la empresa del contexto, no por lo que mande el cliente")
        void get_por_id_acota_por_la_empresa_del_contexto() throws Exception {
            when(findUseCase.findById(7L, COMPANY_ID)).thenReturn(unaFichaCerrada());

            mockMvc.perform(get("/company-billing-profile/7")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.validTo").value("2026-04-01"));

            verify(findUseCase).findById(7L, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Validaciones del cuerpo")
    class ValidacionesDelCuerpo {

        @Test
        @DisplayName("sin correo de facturacion responde 400 y no llega al caso de uso")
        void sin_correo_responde_400() throws Exception {
            mockMvc.perform(post("/company-billing-profile").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"personKind":"LEGAL","taxIdKind":"NIT","taxId":"900123456",
                             "legalName":"Inversiones Pet SAS","address":"Calle 10",
                             "cityId":900,"taxRegime":"COMMON","withholdingAgent":true,
                             "validFrom":"2026-01-15"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("sin withholdingAgent responde 400 y NO lo enlaza a false en silencio")
        void sin_withholding_agent_responde_400() throws Exception {
            // El campo es Boolean con @NotNull justo por esto: con un boolean primitivo,
            // omitirlo dejaria la ficha afirmando que el cliente no es agente de
            // retencion, que es una afirmacion sobre dinero que nadie hizo.
            mockMvc.perform(post("/company-billing-profile").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"personKind":"LEGAL","taxIdKind":"NIT","taxId":"900123456",
                             "legalName":"Inversiones Pet SAS","address":"Calle 10",
                             "cityId":900,"billingEmail":"facturacion@pet.com",
                             "taxRegime":"COMMON","validFrom":"2026-01-15"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un documento de mas de 50 caracteres responde 400 en el binder")
        void un_documento_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/company-billing-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_SOCIEDAD.replace("900123456",
                            CompanyBillingProfileMother.cadenaDe(51))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("la sucesion sin effectiveFrom responde 400: sin fecha no hay sucesion posible")
        void la_sucesion_sin_effective_from_responde_400() throws Exception {
            mockMvc.perform(post("/company-billing-profile/succession")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_SUCESION
                            .replace("\"effectiveFrom\":\"2026-04-01\"", "\"effectiveFrom\":null")))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(succeedUseCase);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el companyId lo pone Authz y no el cuerpo, aunque el cliente lo mande")
        void el_company_id_lo_pone_authz_y_no_el_cuerpo() throws Exception {
            // El request no declara companyId, asi que un cliente que lo incluya solo
            // consigue que Jackson lo descarte. Sin esto, cambiar a nombre de quien se
            // factura en otra clinica seria un campo de JSON.
            when(openUseCase.execute(any())).thenReturn(unaSociedad());

            mockMvc.perform(post("/company-billing-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_SOCIEDAD.replace("\"personKind\":\"LEGAL\"",
                            "\"companyId\":424242,\"personKind\":\"LEGAL\"")))
                    .andExpect(status().isCreated());

            ArgumentCaptor<OpenCompanyBillingProfileCommand> command = ArgumentCaptor
                    .forClass(OpenCompanyBillingProfileCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(COMPANY_ID);
        }

        @Test
        @DisplayName("la sucesion tambien toma el companyId del contexto")
        void la_sucesion_toma_el_company_id_del_contexto() throws Exception {
            when(succeedUseCase.execute(any())).thenReturn(unaPersonaNatural());

            mockMvc.perform(post("/company-billing-profile/succession")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_SUCESION))
                    .andExpect(status().isCreated());

            ArgumentCaptor<SucceedCompanyBillingProfileCommand> command = ArgumentCaptor
                    .forClass(SucceedCompanyBillingProfileCommand.class);
            verify(succeedUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(COMPANY_ID);
        }

        @Test
        @DisplayName("las lecturas paginadas tambien van acotadas por la empresa del contexto")
        void las_lecturas_paginadas_van_acotadas() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID, 0, 20)).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/company-billing-profile/history")).andExpect(status().isOk());

            verify(listUseCase).listByCompany(COMPANY_ID, 0, 20);
        }
    }

    private static CompanyBillingProfileDto unaSociedad() {
        return CompanyBillingProfileDto.from(CompanyBillingProfileMother.persistida(42L));
    }

    private static CompanyBillingProfileDto unaFichaCerrada() {
        return CompanyBillingProfileDto.from(CompanyBillingProfileMother.persistida(7L,
                CompanyBillingProfileMother.COMPANY_ID, CompanyBillingProfileMother.RIGE_DESDE,
                CompanyBillingProfileMother.SUCEDE_DESDE));
    }

    private static CompanyBillingProfileDto unaPersonaNatural() {
        return CompanyBillingProfileDto.from(CompanyBillingProfileMother.personaNatural());
    }
}
