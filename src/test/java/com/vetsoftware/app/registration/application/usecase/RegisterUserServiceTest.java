package com.vetsoftware.app.registration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider.BaseRoleData;
import com.vetsoftware.app.registration.application.port.out.BranchCreator;
import com.vetsoftware.app.registration.application.port.out.CaptchaVerifier;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator.CompanyResult;
import com.vetsoftware.app.registration.application.port.out.CompanyIdentifierChecker;
import com.vetsoftware.app.registration.application.port.out.CompanyTaxProfileCreator;
import com.vetsoftware.app.registration.application.port.out.EmailVerificationTokenRepository;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator.EmployeeResult;
import com.vetsoftware.app.registration.application.port.out.EmployeeRoleAssigner;
import com.vetsoftware.app.registration.application.port.out.InitialSubscriptionCreator;
import com.vetsoftware.app.registration.application.port.out.OwnerBranchAssigner;
import com.vetsoftware.app.registration.application.port.out.RoleCreator;
import com.vetsoftware.app.registration.application.port.out.RoleCreator.RoleResult;
import com.vetsoftware.app.registration.application.port.out.RolePermissionInitializationPort;
import com.vetsoftware.app.registration.application.port.out.VerificationEmailSender;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import com.vetsoftware.app.registration.domain.EmployeeCodeAlreadyExistsException;
import com.vetsoftware.app.registration.domain.OwnerWithoutBranchException;
import com.vetsoftware.app.registration.domain.PlatformCatalogNotConfiguredException;
import com.vetsoftware.app.registration.domain.PlatformRoleCatalogNotConfiguredException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Auto-registro público (Opción B): crea empresa, perfil fiscal, sede
 * Principal, el dueño y sus roles, y deja la cuenta PENDIENTE de verificar el
 * correo. Es un endpoint sin autenticar, así que además del cableado se fija el
 * orden anti-abuso (captcha primero) y que en BD solo queda el hash del token.
 */
@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    private static final Long COMPANY = 9L;
    private static final Long EMPLOYEE = 55L;
    private static final Long BRANCH = 7L;

    @Mock
    private CaptchaVerifier captchaVerifier;
    @Mock
    private CompanyCreator companyCreator;
    @Mock
    private CompanyTaxProfileCreator companyTaxProfileCreator;
    @Mock
    private BranchCreator branchCreator;
    @Mock
    private EmployeeCreator employeeCreator;
    @Mock
    private CompanyIdentifierChecker companyIdentifierChecker;
    @Mock
    private EmployeeCodeChecker employeeCodeChecker;
    @Mock
    private BaseRoleProvider baseRoleProvider;
    @Mock
    private RoleCreator roleCreator;
    @Mock
    private EmployeeRoleAssigner employeeRoleAssigner;
    @Mock
    private OwnerBranchAssigner ownerBranchAssigner;
    @Mock
    private InitialSubscriptionCreator initialSubscriptionCreator;
    @Mock
    private RolePermissionInitializationPort rolePermissionInitializationPort;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private VerificationEmailSender verificationEmailSender;

    private RegisterUserService service;

    /**
     * {@code TransactionTemplate} que ejecuta el callback en el sitio, sin
     * transacción real: aquí no hay BD que abrir. Sirve para comprobar el orden —el
     * captcha va antes de entrar— sin mockear el template, que dejaría el cuerpo
     * del registro sin ejecutar.
     */
    private static TransactionTemplate directTransactionTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                // sin transaccion real que confirmar
            }

            @Override
            public void rollback(TransactionStatus status) {
                // sin transaccion real que revertir
            }
        });
    }

    private static RegisterUserCommand command() {
        return new RegisterUserCommand("Veterinaria Vetrina", "NIT", "900123456", "Calle 1 # 2-3",
                "3001234567", 11001L, "Orlando Velásquez", "orlando@vetrina.co", "Orlando1997*",
                "RESPONSABLE_IVA", "fiscal@vetrina.co", "captcha-token", "10.0.0.1");
    }

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(captchaVerifier, companyCreator, companyTaxProfileCreator,
                branchCreator, employeeCreator, companyIdentifierChecker, employeeCodeChecker,
                baseRoleProvider, roleCreator, employeeRoleAssigner, ownerBranchAssigner,
                initialSubscriptionCreator, rolePermissionInitializationPort,
                emailVerificationTokenRepository, verificationEmailSender,
                directTransactionTemplate(), 24L);

        lenient().when(companyIdentifierChecker.exists(anyString())).thenReturn(false);
        lenient().when(employeeCodeChecker.exists(anyString())).thenReturn(false);
        lenient()
                .when(companyCreator.create(anyString(), anyString(), anyString(), anyString(),
                        anyLong()))
                .thenReturn(new CompanyResult(COMPANY, "Veterinaria Vetrina", "900123456"));
        lenient().when(employeeCreator.create(anyString(), anyString(), anyString(), anyString(),
                anyLong())).thenReturn(new EmployeeResult(EMPLOYEE));
        lenient().when(baseRoleProvider.findAll())
                .thenReturn(List.of(new BaseRoleData(1L, "Administrador", "ADMIN", true),
                        new BaseRoleData(2L, "Veterinario", "VET", false)));
        lenient().when(roleCreator.create(anyString(), anyString(), anyLong()))
                .thenAnswer(i -> new RoleResult("ADMIN".equals(i.getArgument(1)) ? 100L : 200L));
        lenient().when(ownerBranchAssigner.assignAllCompanyBranches(anyLong(), anyLong()))
                .thenReturn(List.of(BRANCH));
    }

    @Nested
    class AltaCompleta {

        @Test
        void deja_la_cuenta_pendiente_de_verificar_el_correo() {
            RegistrationDto dto = service.execute(command());

            assertThat(dto.companyId()).isEqualTo(COMPANY);
            assertThat(dto.employeeId()).isEqualTo(EMPLOYEE);
            assertThat(dto.email()).isEqualTo("orlando@vetrina.co");
            assertThat(dto.status()).isEqualTo("PENDING_VERIFICATION");
        }

        @Test
        void el_usuario_de_acceso_es_el_correo_del_administrador() {
            service.execute(command());

            verify(employeeCreator).create("orlando@vetrina.co", "Orlando1997*",
                    "Orlando Velásquez", "orlando@vetrina.co", COMPANY);
        }

        @Test
        void entrega_la_contrasena_cruda_al_creador_para_que_se_hashee_una_sola_vez() {
            // Pre-hashear aquí dejaría el hash doblemente aplicado y el login nunca haría
            // match.
            service.execute(command());

            verify(employeeCreator).create(anyString(),
                    org.mockito.ArgumentMatchers.eq("Orlando1997*"), anyString(), anyString(),
                    anyLong());
        }

        @Test
        void toda_empresa_nace_con_su_sede_principal() {
            service.execute(command());

            verify(branchCreator).create("Principal", "PRINCIPAL", "Calle 1 # 2-3", "3001234567",
                    11001L, COMPANY);
        }

        @Test
        void crea_el_perfil_fiscal_del_emisor_con_el_identificador_de_la_empresa() {
            service.execute(command());

            verify(companyTaxProfileCreator).create(COMPANY, "NIT", "900123456",
                    "Veterinaria Vetrina", "RESPONSABLE_IVA", "fiscal@vetrina.co");
        }

        @Test
        void instancia_todos_los_base_roles_pero_solo_autoasigna_los_obligatorios() {
            service.execute(command());

            verify(roleCreator).create("Administrador", "ADMIN", COMPANY);
            verify(roleCreator).create("Veterinario", "VET", COMPANY);
            verify(rolePermissionInitializationPort).initializeForRole(100L, COMPANY, 1L);
            verify(rolePermissionInitializationPort).initializeForRole(200L, COMPANY, 2L);

            verify(employeeRoleAssigner).assign(EMPLOYEE, 100L);
            verify(employeeRoleAssigner, never()).assign(EMPLOYEE, 200L);
        }
    }

    /**
     * <b>Regla del modelo, no negociable:</b> toda empresa nace con un contrato, y
     * nace con el en la misma transaccion del alta. Estos dos tests fijan el orden
     * y la consecuencia de que el contrato no se pueda crear.
     */
    @Nested
    class ContratoInicial {

        @Test
        void crea_el_contrato_en_el_acto_y_antes_de_repartir_permisos() {
            service.execute(command());

            InOrder order = inOrder(companyCreator, initialSubscriptionCreator, roleCreator,
                    rolePermissionInitializationPort);
            order.verify(companyCreator).create(anyString(), anyString(), anyString(), anyString(),
                    anyLong());
            order.verify(initialSubscriptionCreator).createInitialContract(COMPANY,
                    "Veterinaria Vetrina");
            order.verify(roleCreator).create(anyString(), anyString(), anyLong());
            order.verify(rolePermissionInitializationPort).initializeForRole(anyLong(), anyLong(),
                    anyLong());
        }

        /**
         * El escenario del dia 1 con el catalogo sin sembrar: el alta falla entera con
         * un error legible en vez de dejar una empresa sin contrato que entra al
         * sistema y no puede hacer nada. Nada posterior al contrato llega a ejecutarse,
         * y la transaccion revierte lo anterior.
         */
        @Test
        void sin_catalogo_comercial_el_alta_falla_entera_y_no_deja_nada_a_medias() {
            org.mockito.Mockito
                    .doThrow(new PlatformCatalogNotConfiguredException("Veterinaria Vetrina"))
                    .when(initialSubscriptionCreator).createInitialContract(anyLong(), anyString());

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(PlatformCatalogNotConfiguredException.class)
                    .hasMessageContaining("Veterinaria Vetrina")
                    .hasMessageContaining("catalog_items").hasMessageContaining("price_lists")
                    .hasMessageContaining("platform_billing_config");

            verify(companyTaxProfileCreator, never()).create(anyLong(), anyString(), anyString(),
                    anyString(), anyString(), anyString());
            verify(branchCreator, never()).create(anyString(), anyString(), anyString(),
                    anyString(), anyLong(), anyLong());
            verify(employeeCreator, never()).create(anyString(), anyString(), anyString(),
                    anyString(), anyLong());
            verify(roleCreator, never()).create(anyString(), anyString(), anyLong());
            verify(emailVerificationTokenRepository, never()).save(any());
            verify(verificationEmailSender, never()).send(anyString(), anyString(), anyString(),
                    anyString());
        }
    }

    /**
     * <b>La otra mitad de la misma regla, y el defecto del issue #500.</b> El
     * catalogo comercial tenia guarda desde #364; el de roles no la tenia, asi que
     * el bucle que reparte los base roles iteraba sobre una lista vacia, no lanzaba
     * nada y el alta devolvia 201 con el dueño sin un solo rol. Estos tests fijan
     * que ahora falla igual de ruidoso que su hermano de tres lineas mas arriba, y
     * que nada posterior al reparto llega a ejecutarse.
     */
    @Nested
    class CatalogoDeRoles {

        @Test
        void sin_ningun_rol_base_el_alta_falla_entera_y_enumera_lo_que_falta() {
            when(baseRoleProvider.findAll()).thenReturn(List.of());

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(PlatformRoleCatalogNotConfiguredException.class)
                    .hasMessageContaining("Veterinaria Vetrina").hasMessageContaining("base_roles")
                    .hasMessageContaining("ADMIN").hasMessageContaining("base_role_permissions");

            verify(roleCreator, never()).create(anyString(), anyString(), anyLong());
            verify(rolePermissionInitializationPort, never()).initializeForRole(anyLong(),
                    anyLong(), anyLong());
            verify(employeeRoleAssigner, never()).assign(anyLong(), anyLong());
            verify(emailVerificationTokenRepository, never()).save(any());
            verify(verificationEmailSender, never()).send(anyString(), anyString(), anyString(),
                    anyString());
        }

        /**
         * La mitad del defecto que comprobar solo {@code isEmpty()} dejaria viva: con
         * plantillas pero sin ninguna obligatoria el dueño se queda igual de vacio, y
         * el mensaje tiene que decir que la tabla NO esta vacia para que nadie vuelva a
         * sembrarla.
         */
        @Test
        void con_roles_base_pero_ninguno_obligatorio_tampoco_nace_la_empresa() {
            when(baseRoleProvider.findAll())
                    .thenReturn(List.of(new BaseRoleData(2L, "Veterinario", "VET", false),
                            new BaseRoleData(3L, "Recepción", "RECEP", false)));

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(PlatformRoleCatalogNotConfiguredException.class)
                    .hasMessageContaining("Veterinaria Vetrina").hasMessageContaining("VET")
                    .hasMessageContaining("RECEP").hasMessageContaining("mandatory");

            verify(roleCreator, never()).create(anyString(), anyString(), anyLong());
            verify(employeeRoleAssigner, never()).assign(anyLong(), anyLong());
        }

        /**
         * La guarda se evalua ANTES de crear el primer rol: si fallara a mitad del
         * reparto, el error describiria el rol numero tres en vez de la causa.
         */
        @Test
        void la_guarda_corre_antes_de_crear_el_primer_rol() {
            when(baseRoleProvider.findAll()).thenReturn(List.of());

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(PlatformRoleCatalogNotConfiguredException.class);

            InOrder order = inOrder(employeeCreator, baseRoleProvider, roleCreator);
            order.verify(employeeCreator).create(anyString(), anyString(), anyString(), anyString(),
                    anyLong());
            order.verify(baseRoleProvider).findAll();
            order.verify(roleCreator, never()).create(anyString(), anyString(), anyLong());
        }
    }

    /**
     * <b>El defecto del issue #510.</b> El alta creaba la sede "Principal" y creaba
     * al dueño, y nadie escribia la fila de {@code employee_branches} que los une:
     * {@code Authz.currentBranchIds()} —que sale de esa tabla y de ninguna otra—
     * devolvia el conjunto vacio, y la primera invitacion de personal moria con 403
     * BRANCH_NOT_ALLOWED rechazando la unica sede que existia. Estos tests fijan
     * que la atadura se escribe, cuando, y que si no queda escrita el alta se
     * cancela entera en vez de devolver un 201 que falla tres pantallas despues.
     */
    @Nested
    class SedeDelDueno {

        @Test
        void ata_al_dueno_con_todas_las_sedes_de_su_empresa() {
            service.execute(command());

            verify(ownerBranchAssigner).assignAllCompanyBranches(EMPLOYEE, COMPANY);
        }

        /**
         * La sede tiene que existir antes de poder atarla, y el dueño tambien: el orden
         * no es cosmetico.
         */
        @Test
        void la_atadura_va_despues_de_crear_la_sede_y_el_empleado() {
            service.execute(command());

            InOrder order = inOrder(branchCreator, employeeCreator, ownerBranchAssigner);
            order.verify(branchCreator).create(anyString(), anyString(), anyString(), anyString(),
                    anyLong(), anyLong());
            order.verify(employeeCreator).create(anyString(), anyString(), anyString(), anyString(),
                    anyLong());
            order.verify(ownerBranchAssigner).assignAllCompanyBranches(anyLong(), anyLong());
        }

        /**
         * La guarda gemela de {@code requireOwnerRole}: si el dueño se queda sin sede,
         * no nace la empresa. Y no llega a enviarse el correo de verificacion, porque
         * invitaria a entrar a una cuenta que no puede trabajar.
         */
        @Test
        void si_el_dueno_queda_sin_ninguna_sede_el_alta_falla_entera() {
            when(ownerBranchAssigner.assignAllCompanyBranches(anyLong(), anyLong()))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(OwnerWithoutBranchException.class)
                    .hasMessageContaining("Veterinaria Vetrina")
                    .hasMessageContaining("employee_branches")
                    .hasMessageContaining("BRANCH_NOT_ALLOWED");

            verify(emailVerificationTokenRepository, never()).save(any());
            verify(verificationEmailSender, never()).send(anyString(), anyString(), anyString(),
                    anyString());
        }

        /**
         * Un puerto que devuelve {@code null} es el mismo defecto, no una excepcion.
         */
        @Test
        void una_lista_nula_de_sedes_se_trata_igual_que_una_vacia() {
            when(ownerBranchAssigner.assignAllCompanyBranches(anyLong(), anyLong()))
                    .thenReturn(null);

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(OwnerWithoutBranchException.class);
        }
    }

    @Nested
    class TokenDeVerificacion {

        @Test
        void persiste_solo_el_hash_del_token_y_envia_el_valor_plano_por_correo() {
            ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<EmailVerificationToken> stored = ArgumentCaptor
                    .forClass(EmailVerificationToken.class);

            service.execute(command());

            verify(emailVerificationTokenRepository).save(stored.capture());
            verify(verificationEmailSender).send(anyString(), anyString(), anyString(),
                    rawToken.capture());

            assertThat(stored.getValue().getTokenHash())
                    .as("en BD nunca puede quedar el token usable")
                    .isNotEqualTo(rawToken.getValue()).hasSize(64);
            assertThat(stored.getValue().getTokenHash())
                    .isEqualTo(VerificationTokens.hash(rawToken.getValue()));
        }

        @Test
        void el_token_se_emite_para_el_empleado_y_la_empresa_recien_creados() {
            ArgumentCaptor<EmailVerificationToken> stored = ArgumentCaptor
                    .forClass(EmailVerificationToken.class);

            service.execute(command());

            verify(emailVerificationTokenRepository).save(stored.capture());
            assertThat(stored.getValue().getEmployeeId()).isEqualTo(EMPLOYEE);
            assertThat(stored.getValue().getCompanyId()).isEqualTo(COMPANY);
            assertThat(stored.getValue().getConsumedAt()).isNull();
        }

        @Test
        void aplica_la_vigencia_configurada() {
            ArgumentCaptor<EmailVerificationToken> stored = ArgumentCaptor
                    .forClass(EmailVerificationToken.class);
            LocalDateTime before = LocalDateTime.now().plusHours(24);

            service.execute(command());

            verify(emailVerificationTokenRepository).save(stored.capture());
            assertThat(stored.getValue().getExpiresAt()).isBetween(before.minusMinutes(1),
                    before.plusMinutes(1));
        }

        @Test
        void dos_registros_no_producen_el_mismo_token() {
            ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);

            service.execute(command());
            service.execute(command());

            verify(verificationEmailSender, org.mockito.Mockito.times(2)).send(anyString(),
                    anyString(), anyString(), tokens.capture());
            assertThat(tokens.getAllValues()).doesNotHaveDuplicates();
        }
    }

    @Nested
    class AntiAbusoYUnicidad {

        @Test
        void verifica_el_captcha_antes_de_tocar_la_base_de_datos() {
            service.execute(command());

            InOrder order = inOrder(captchaVerifier, companyIdentifierChecker, companyCreator);
            order.verify(captchaVerifier).verify("captcha-token", "10.0.0.1");
            order.verify(companyIdentifierChecker).exists("900123456");
            order.verify(companyCreator).create(anyString(), anyString(), anyString(), anyString(),
                    anyLong());
        }

        @Test
        void un_captcha_invalido_aborta_el_registro_completo() {
            org.mockito.Mockito.doThrow(new IllegalArgumentException("captcha failed"))
                    .when(captchaVerifier).verify(anyString(), anyString());

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(companyCreator, never()).create(anyString(), anyString(), anyString(),
                    anyString(), anyLong());
            verify(employeeCreator, never()).create(anyString(), anyString(), anyString(),
                    anyString(), anyLong());
        }

        @Test
        void un_nit_ya_registrado_no_crea_una_segunda_empresa() {
            when(companyIdentifierChecker.exists("900123456")).thenReturn(true);

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already in use");

            verify(companyCreator, never()).create(anyString(), anyString(), anyString(),
                    anyString(), anyLong());
        }

        @Test
        void un_correo_ya_usado_como_codigo_de_acceso_se_rechaza() {
            when(employeeCodeChecker.exists("orlando@vetrina.co")).thenReturn(true);

            assertThatThrownBy(() -> service.execute(command()))
                    .isInstanceOf(EmployeeCodeAlreadyExistsException.class);

            verify(employeeCreator, never()).create(anyString(), anyString(), anyString(),
                    anyString(), anyLong());
            verify(emailVerificationTokenRepository, never()).save(any());
        }

        @Test
        void el_correo_se_normaliza_quitando_espacios_antes_de_usarlo_como_codigo() {
            RegisterUserCommand withSpaces = new RegisterUserCommand("Veterinaria Vetrina", "NIT",
                    "900123456", "Calle 1 # 2-3", "3001234567", 11001L, "Orlando",
                    "  orlando@vetrina.co  ", "Orlando1997*", "RESPONSABLE_IVA",
                    "fiscal@vetrina.co", "captcha-token", "10.0.0.1");

            service.execute(withSpaces);

            verify(employeeCodeChecker).exists("orlando@vetrina.co");
        }
    }
}
