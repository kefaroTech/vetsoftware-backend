package com.vetsoftware.app.companycontactchannel.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companycontactchannel.application.command.AuthorizeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.command.DesignatePrimaryCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.command.RevokeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.port.in.AuthorizeCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.DesignatePrimaryCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.FindCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.ListCompanyContactChannelsUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.ListUsableCompanyContactChannelsUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.RevokeCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * Rodaja web de los canales de contacto del tenant.
 *
 * <p>
 * Las tres cosas que esta clase congela y que ningun test de servicio ve:
 *
 * <ul>
 * <li><b>La empresa la pone el contexto de autorizacion, no el cliente.</b>
 * Ninguno de los seis endpoints acepta una empresa por parametro ni por cuerpo;
 * si alguien anadiera un {@code companyId} «para la consola», el captor con el
 * valor exacto de {@code authz.currentCompanyId()} lo caza.</li>
 * <li><b>Las restricciones del cuerpo se evaluan de verdad.</b> Sin
 * {@code @Valid} delante del {@code @RequestBody}, el {@code @NotBlank} del DTO
 * esta escrito y no se dispara nunca —y el OpenAPI seguiria anunciandolo al
 * front igual—. Los casos de 400 se ponen rojos el dia que alguien lo
 * quite.</li>
 * <li><b>Un canal vigente publica {@code revokedAt} como nulo y no lo
 * omite.</b> Ese nulo es el estado «sigue autorizado»; si alguien configurara
 * la serializacion para saltarse los nulos, el front no podria distinguirlo de
 * un campo que no vino.</li>
 * </ul>
 */
@WebMvcTest(CompanyContactChannelController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyContactChannelController — contrato HTTP del tenant")
class CompanyContactChannelControllerTest {

    /**
     * Distinto del {@code COMPANY_ID} por defecto para que se vea de donde sale.
     */
    private static final Long EMPRESA_DEL_TOKEN = 77L;

    private static final Long ID = 8500L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthorizeCompanyContactChannelUseCase authorizeUseCase;
    @MockitoBean
    private RevokeCompanyContactChannelUseCase revokeUseCase;
    @MockitoBean
    private DesignatePrimaryCompanyContactChannelUseCase designatePrimaryUseCase;
    @MockitoBean
    private FindCompanyContactChannelUseCase findUseCase;
    @MockitoBean
    private ListUsableCompanyContactChannelsUseCase listUsableUseCase;
    @MockitoBean
    private ListCompanyContactChannelsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_TOKEN);
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("devuelve 201 con el canal y cada campo en su lugar del JSON")
        void devuelve_201_con_el_canal() throws Exception {
            when(authorizeUseCase.execute(any())).thenReturn(CompanyContactChannelMother.dto(ID));

            mockMvc.perform(post("/company-contact-channels")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {
                                      "channelType": "EMAIL",
                                      "address": "facturacion@clinicasanroque.co",
                                      "purpose": "BILLING",
                                      "authorizationEvidence": "Clausula 7 del contrato firmado el 2026-01-15"
                                    }
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(8500))
                    .andExpect(jsonPath("$.companyId").value(900))
                    .andExpect(jsonPath("$.channelType").value("EMAIL"))
                    .andExpect(jsonPath("$.address").value("facturacion@clinicasanroque.co"))
                    .andExpect(jsonPath("$.purpose").value("BILLING"))
                    .andExpect(jsonPath("$.authorizedAt").value("2026-03-05T09:30:00"))
                    .andExpect(jsonPath("$.primary").value(false))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-07T08:45:00"));
        }

        @Test
        @DisplayName("la empresa la inyecta el servidor: el cuerpo no la lleva")
        void la_empresa_la_inyecta_el_servidor() throws Exception {
            when(authorizeUseCase.execute(any())).thenReturn(CompanyContactChannelMother.dto(ID));

            mockMvc.perform(post("/company-contact-channels")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "channelType": "SMS",
                              "address": "+573001234567",
                              "purpose": "DUNNING",
                              "authorizationEvidence": "Formulario de consentimiento 4471"
                            }
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<AuthorizeCompanyContactChannelCommand> comando = ArgumentCaptor
                    .forClass(AuthorizeCompanyContactChannelCommand.class);
            verify(authorizeUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA_DEL_TOKEN);
            assertThat(comando.getValue().channelType()).isEqualTo(ContactChannelType.SMS);
            assertThat(comando.getValue().purpose()).isEqualTo(ContactPurpose.DUNNING);
            assertThat(comando.getValue().address()).isEqualTo("+573001234567");
        }

        @Test
        @DisplayName("una direccion en blanco sale 400 y no llega al caso de uso")
        void una_direccion_en_blanco_sale_400() throws Exception {
            mockMvc.perform(post("/company-contact-channels")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "channelType": "EMAIL",
                              "address": "   ",
                              "purpose": "BILLING",
                              "authorizationEvidence": "Formulario 4471"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(authorizeUseCase);
        }

        @Test
        @DisplayName("una autorizacion sin evidencia sale 400 y no llega al caso de uso")
        void una_autorizacion_sin_evidencia_sale_400() throws Exception {
            // Sin @Valid delante del @RequestBody el @NotBlank no se evaluaria y la fila
            // llegaria al dominio para morir alli con otra forma de error: el front
            // pintaria un mensaje generico en vez del error de campo bajo el input.
            mockMvc.perform(post("/company-contact-channels")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "channelType": "EMAIL",
                              "address": "facturacion@clinicasanroque.co",
                              "purpose": "BILLING"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(authorizeUseCase);
        }
    }

    @Nested
    @DisplayName("Revocacion")
    class Revocacion {

        @Test
        @DisplayName("manda el id, la empresa del token y el motivo, y devuelve el canal cerrado")
        void manda_el_id_la_empresa_y_el_motivo() throws Exception {
            when(revokeUseCase.execute(any()))
                    .thenReturn(CompanyContactChannelMother.dtoRevocado(ID));

            mockMvc.perform(patch("/company-contact-channels/{id}/revoke", ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "reason": "El cliente retiro el consentimiento por escrito"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.revokedAt").value("2026-06-18T14:05:45"))
                    .andExpect(jsonPath("$.revokedReason")
                            .value("El cliente retiro el consentimiento por escrito"));

            ArgumentCaptor<RevokeCompanyContactChannelCommand> comando = ArgumentCaptor
                    .forClass(RevokeCompanyContactChannelCommand.class);
            verify(revokeUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(ID);
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA_DEL_TOKEN);
            assertThat(comando.getValue().reason())
                    .isEqualTo("El cliente retiro el consentimiento por escrito");
        }

        @Test
        @DisplayName("revocar sin motivo sale 400 y no llega al caso de uso")
        void revocar_sin_motivo_sale_400() throws Exception {
            // El motivo es lo unico que este cuerpo transporta, y es obligatorio en el
            // esquema: una baja sin por que obliga a quien audite el ano siguiente a
            // adivinar si el cliente se dio de baja o si fue un error de captura.
            mockMvc.perform(patch("/company-contact-channels/{id}/revoke", ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "reason": "  "
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(revokeUseCase);
        }

        @Test
        @DisplayName("no hay DELETE: el canal se cierra, no se borra")
        void no_hay_delete() throws Exception {
            // La ausencia de borrado ES la feature, asi que se prueba. Con la ruta
            // mapeada para GET, un DELETE sale 405 y no 404: si alguien anadiera el
            // endpoint «para limpiar», este caso se pone rojo antes de que la primera
            // fila desaparezca.
            mockMvc.perform(delete("/company-contact-channels/{id}", ID))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(revokeUseCase);
        }
    }

    @Nested
    @DisplayName("Designacion del primario")
    class DesignacionDelPrimario {

        @Test
        @DisplayName("manda el id y la empresa del token, sin cuerpo y sin proposito")
        void manda_el_id_y_la_empresa_sin_cuerpo() throws Exception {
            // El proposito no viaja: es el que ya tiene el canal. Aceptarlo permitiria
            // pedir que un canal de marketing pase a ser el primario de facturacion, que
            // no es una designacion sino una reescritura del consentimiento.
            when(designatePrimaryUseCase.execute(any()))
                    .thenReturn(CompanyContactChannelMother.dto(ID));

            mockMvc.perform(patch("/company-contact-channels/{id}/primary", ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(8500));

            ArgumentCaptor<DesignatePrimaryCompanyContactChannelCommand> comando = ArgumentCaptor
                    .forClass(DesignatePrimaryCompanyContactChannelCommand.class);
            verify(designatePrimaryUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(ID);
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA_DEL_TOKEN);
        }
    }

    @Nested
    @DisplayName("Lecturas")
    class Lecturas {

        @Test
        @DisplayName("un canal vigente publica revokedAt como nulo, no lo omite")
        void un_canal_vigente_publica_revoked_at_como_nulo() throws Exception {
            // La clave tiene que ESTAR y valer null. Por eso hasJsonPath y no exists: el
            // exists() de Spring falla sobre un valor nulo, asi que no distingue «la
            // clave vino con null» de «la clave no vino», que es justo la distincion que
            // aqui importa.
            when(findUseCase.findById(ID, EMPRESA_DEL_TOKEN))
                    .thenReturn(CompanyContactChannelMother.dto(ID));

            mockMvc.perform(get("/company-contact-channels/{id}", ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.revokedAt").hasJsonPath())
                    .andExpect(jsonPath("$.revokedAt").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.revokedReason").hasJsonPath())
                    .andExpect(jsonPath("$.authorizationEvidence")
                            .value("Clausula 7 del contrato firmado el 2026-01-15"));
        }

        @Test
        @DisplayName("la lectura por id lleva la empresa del token, nunca una de la URL")
        void la_lectura_por_id_lleva_la_empresa_del_token() throws Exception {
            when(findUseCase.findById(ID, EMPRESA_DEL_TOKEN))
                    .thenReturn(CompanyContactChannelMother.dto(ID));

            mockMvc.perform(get("/company-contact-channels/{id}", ID)).andExpect(status().isOk());

            verify(findUseCase).findById(ID, EMPRESA_DEL_TOKEN);
        }

        @Test
        @DisplayName("los canales usables piden el proposito exacto de la query")
        void los_usables_piden_el_proposito_exacto() throws Exception {
            when(listUsableUseCase.listUsable(EMPRESA_DEL_TOKEN, ContactPurpose.DUNNING, 0, 20))
                    .thenReturn(
                            PageResult.of(List.of(CompanyContactChannelMother.dto(ID)), 0, 20, 1L));

            mockMvc.perform(get("/company-contact-channels/usable").param("purpose", "DUNNING"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(8500))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listUsableUseCase).listUsable(EMPRESA_DEL_TOKEN, ContactPurpose.DUNNING, 0, 20);
        }

        @Test
        @DisplayName("los canales usables sin proposito salen 400: no hay defecto silencioso")
        void los_usables_sin_proposito_salen_400() throws Exception {
            // Un valor por defecto convertiria esta consulta en la lista de a quien se
            // puede escribir para cualquier cosa, que es la confusion que la columna
            // purpose existe para evitar.
            mockMvc.perform(get("/company-contact-channels/usable"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listUsableUseCase);
        }

        @Test
        @DisplayName("la bitacora completa pagina con los totales de la consulta")
        void la_bitacora_completa_pagina() throws Exception {
            when(listUseCase.listByCompany(EMPRESA_DEL_TOKEN, 1, 2))
                    .thenReturn(PageResult.of(List.of(CompanyContactChannelMother.dto(ID),
                            CompanyContactChannelMother.dtoRevocado(8501L)), 1, 2, 40L));

            mockMvc.perform(
                    get("/company-contact-channels").param("page", "1").param("pageSize", "2"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(2))
                    .andExpect(jsonPath("$.totalElements").value(40))
                    .andExpect(jsonPath("$.totalPages").value(20))
                    .andExpect(jsonPath("$.content[1].revokedAt").value("2026-06-18T14:05:45"));
        }
    }
}
