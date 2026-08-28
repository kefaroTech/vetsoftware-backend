package com.vetsoftware.app.companylimitoverride.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.ListCompanyLimitOverridesUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companylimitoverride.domain.LimitSource;
import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Rodaja HTTP de lo que la clínica ve de las excepciones negociadas a su favor.
 *
 * <p>
 * Dos cosas se comprueban aquí y no en otro sitio. La primera, que la empresa
 * la pone el servidor y no hay superficie por la que el cliente pueda nombrar
 * otra. La segunda, que este controller <strong>no expone ninguna
 * escritura</strong>: si conceder o revocar se colaran en la ruta del tenant,
 * la administradora se subiría el techo cada vez que topa y el cupo dejaría de
 * ser un cupo.
 */
@WebMvcTest(CompanyLimitOverrideController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyLimitOverrideController — contrato HTTP")
class CompanyLimitOverrideControllerTest {

    private static final Long MI_EMPRESA = 9L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCompanyLimitOverridesUseCase listUseCase;
    @MockitoBean
    private ResolveEffectiveLimitUseCase resolveUseCase;
    @MockitoBean
    private Authz authz;

    private static CompanyLimitOverrideDto viva() {
        return new CompanyLimitOverrideDto(31L, MI_EMPRESA, 4L, 250, LocalDate.of(2026, 3, 1), null,
                OverrideReasonCode.RETENTION, "Retencion pactada con la cuenta", 6L, null, null,
                null, null, true);
    }

    private static CompanyLimitOverrideDto revocada() {
        return new CompanyLimitOverrideDto(30L, MI_EMPRESA, 4L, 150, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28), OverrideReasonCode.MIGRATION,
                "Migracion desde el sistema anterior", 6L, 6L, LocalDateTime.of(2026, 2, 28, 11, 0),
                OverrideReasonCode.COMMERCIAL_AGREEMENT, "Sustituida por el acuerdo nuevo", false);
    }

    @Nested
    @DisplayName("Lectura del tenant")
    class LecturaDelTenant {

        /**
         * El listado es la <em>historia</em>, revocadas incluidas: es lo que responde
         * «¿qué techo tenía el 14 de marzo?» sin reconstruir nada. Una respuesta que
         * solo trajera las vivas convertiría esa pregunta en arqueología.
         */
        @Test
        @DisplayName("GET /company-limit-overrides trae la historia, revocadas incluidas")
        void get_trae_la_historia_completa() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of(viva(), revocada()));

            mockMvc.perform(get("/company-limit-overrides")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].limitQuantity").value(250))
                    .andExpect(jsonPath("$[0].reasonCode").value("RETENTION"))
                    .andExpect(jsonPath("$[0].validFrom").value("2026-03-01"))
                    .andExpect(jsonPath("$[0].alive").value(true))
                    .andExpect(jsonPath("$[1].alive").value(false))
                    .andExpect(jsonPath("$[1].validTo").value("2026-02-28"))
                    .andExpect(jsonPath("$[1].revokedReasonCode").value("COMMERCIAL_AGREEMENT"));
        }

        /**
         * La firma sale en la respuesta y no se oculta: un techo distinto del de
         * catálogo sin nadie detrás es exactamente el número inexplicable que esta
         * tabla existe para evitar.
         */
        @Test
        @DisplayName("la respuesta lleva quien concedio la excepcion")
        void la_respuesta_lleva_la_firma() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of(viva()));

            mockMvc.perform(get("/company-limit-overrides")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].grantedBySystemUserId").value(6))
                    .andExpect(jsonPath("$[0].reason").value("Retencion pactada con la cuenta"));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa la pone el servidor: un companyId en la URL se ignora")
        void la_empresa_la_pone_el_servidor() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(listUseCase.listByCompanyId(MI_EMPRESA)).thenReturn(List.of());

            mockMvc.perform(get("/company-limit-overrides").param("companyId", "77"))
                    .andExpect(status().isOk());

            verify(listUseCase).listByCompanyId(MI_EMPRESA);
        }

        @Test
        @DisplayName("un principal sin empresa responde 403 y no llega al caso de uso")
        void principal_sin_empresa_responde_403() throws Exception {
            when(authz.currentCompanyId())
                    .thenThrow(new AccessDeniedException("No employee context"));

            mockMvc.perform(get("/company-limit-overrides")).andExpect(status().isForbidden());

            verifyNoInteractions(listUseCase);
        }

        /**
         * La ruta del tenant no ofrece {@code /companies/{companyId}} en ninguna forma.
         * Si mañana alguien la colgara de ahí, el gate seguiría verde —admite la propia
         * empresa— pero la URL invitaría a probar la del vecino, que es como empieza
         * cada una de las fugas que BE-COV catalogó.
         */
        @Test
        @DisplayName("la ruta del controller del tenant no lleva companyId")
        void la_ruta_no_lleva_company_id() {
            assertThat(CompanyLimitOverrideController.class.getAnnotation(RequestMapping.class)
                    .value()).containsExactly("/company-limit-overrides");
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("el puerto de lectura admite SYSTEM o la propia empresa, y revalida el tenant")
        void el_puerto_admite_system_o_la_propia_empresa() throws Exception {
            String gate = ListCompanyLimitOverridesUseCase.class
                    .getMethod("listByCompanyId", Long.class).getAnnotation(PreAuthorize.class)
                    .value();

            assertThat(gate).contains("hasRole('SYSTEM')")
                    .contains("@authz.isMyCompany(#companyId)");
        }

        @Test
        @DisplayName("el parametro del puerto se llama companyId, que es lo que nombra el SpEL")
        void el_parametro_se_llama_company_id() throws Exception {
            assertThat(ListCompanyLimitOverridesUseCase.class
                    .getMethod("listByCompanyId", Long.class).getParameters()[0].getName())
                    .isEqualTo("companyId");
        }

        /**
         * El controller del tenant es de lectura entera. Esta aserción es la que se
         * rompe el día que alguien añada aquí un {@code POST} «para que el cliente
         * pueda pedir más cupo»: subir un techo es una decisión de plataforma con
         * firma, y su sitio es {@code SystemCompanyLimitOverrideController}.
         *
         * <p>
         * <strong>Mira las anotaciones, no los nombres de los métodos.</strong> Un
         * {@code containsExactly("listMine")} sobre {@code getDeclaredMethods()} parece
         * equivalente y no lo es: con el agente de JaCoCo puesto, la clase declara
         * además un {@code $jacocoInit} sintético, así que esa versión pasa suelta y
         * falla en el build completo. El defecto no estaría en el controller sino en el
         * instrumentador, que es la peor clase de test frágil.
         */
        @Test
        @DisplayName("el controller del tenant no expone ninguna escritura")
        void el_controller_del_tenant_no_expone_escrituras() {
            assertThat(CompanyLimitOverrideController.class.getDeclaredMethods())
                    .noneMatch(metodo -> metodo.isAnnotationPresent(PostMapping.class)
                            || metodo.isAnnotationPresent(PutMapping.class)
                            || metodo.isAnnotationPresent(PatchMapping.class)
                            || metodo.isAnnotationPresent(DeleteMapping.class));
        }
    }

    @Nested
    @DisplayName("Techo efectivo")
    class TechoEfectivo {

        /**
         * <strong>Es la mitad que le faltaba al cliente.</strong> Con el listado de
         * arriba lee sus excepciones y en {@code /subscription-item-limits} sus techos
         * congelados; ninguno de los dos le dice cuál manda. Aquí sale el número
         * <em>con su procedencia dentro</em>, que es la línea que la pantalla de cupos
         * necesita para explicarlo.
         */
        @Test
        @DisplayName("GET /effective-limits/{eje} devuelve el techo y de donde sale")
        void get_devuelve_el_techo_y_su_origen() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(resolveUseCase.resolve(MI_EMPRESA, 4L)).thenReturn(new EffectiveLimitDto(
                    MI_EMPRESA, 4L, 250, LimitSource.COMPANY_OVERRIDE, 31L, false));

            mockMvc.perform(get("/company-limit-overrides/effective-limits/4"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.limitQuantity").value(250))
                    .andExpect(jsonPath("$.source").value("COMPANY_OVERRIDE"))
                    .andExpect(jsonPath("$.overrideId").value(31))
                    .andExpect(jsonPath("$.unlimited").value(false));
        }

        /**
         * Cuando el techo viene del contrato, no hay excepción que nombrar: el
         * {@code overrideId} llega vacío y el origen lo dice. Enseñar un id ahí sería
         * ofrecerle al usuario un papel que no existe.
         */
        @Test
        @DisplayName("un techo del contrato no nombra ninguna excepcion")
        void techo_del_contrato_no_nombra_excepcion() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(resolveUseCase.resolve(MI_EMPRESA, 4L)).thenReturn(new EffectiveLimitDto(
                    MI_EMPRESA, 4L, 100, LimitSource.SUBSCRIPTION, null, false));

            mockMvc.perform(get("/company-limit-overrides/effective-limits/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.source").value("SUBSCRIPTION"))
                    .andExpect(jsonPath("$.overrideId").doesNotExist());
        }

        /**
         * <strong>«Sin techo» y «techo cero» son cosas distintas</strong>, y la
         * respuesta las separa con dos campos: {@code limitQuantity} vacío más
         * {@code unlimited} en {@code true}. Confundirlas es la diferencia entre una
         * clínica bloqueada y una clínica sin límite, y con un solo campo la interfaz
         * pinta «0 de 0» donde la verdad es «este límite no te aplica».
         */
        @Test
        @DisplayName("sin techo llega vacio y marcado, nunca como un cero")
        void sin_techo_no_es_cero() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(resolveUseCase.resolve(MI_EMPRESA, 5L)).thenReturn(
                    new EffectiveLimitDto(MI_EMPRESA, 5L, null, LimitSource.NONE, null, true));

            mockMvc.perform(get("/company-limit-overrides/effective-limits/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.limitQuantity").doesNotExist())
                    .andExpect(jsonPath("$.unlimited").value(true))
                    .andExpect(jsonPath("$.source").value("NONE"));
        }

        @Test
        @DisplayName("la empresa la pone el servidor: en la ruta solo viaja el eje")
        void la_empresa_la_pone_el_servidor() throws Exception {
            when(authz.currentCompanyId()).thenReturn(MI_EMPRESA);
            when(resolveUseCase.resolve(MI_EMPRESA, 4L)).thenReturn(new EffectiveLimitDto(
                    MI_EMPRESA, 4L, 100, LimitSource.SUBSCRIPTION, null, false));

            mockMvc.perform(
                    get("/company-limit-overrides/effective-limits/4").param("companyId", "77"))
                    .andExpect(status().isOk());

            verify(resolveUseCase).resolve(MI_EMPRESA, 4L);
        }

        @Test
        @DisplayName("el puerto admite SYSTEM o la propia empresa, y revalida el tenant")
        void el_puerto_admite_system_o_la_propia_empresa() throws Exception {
            String gate = ResolveEffectiveLimitUseCase.class
                    .getMethod("resolve", Long.class, Long.class).getAnnotation(PreAuthorize.class)
                    .value();

            assertThat(gate).contains("hasRole('SYSTEM')")
                    .contains("@authz.isMyCompany(#companyId)");
        }

        @Test
        @DisplayName("el primer parametro del puerto se llama companyId, como nombra el SpEL")
        void el_parametro_se_llama_company_id() throws Exception {
            assertThat(ResolveEffectiveLimitUseCase.class
                    .getMethod("resolve", Long.class, Long.class).getParameters()[0].getName())
                    .isEqualTo("companyId");
        }
    }
}
