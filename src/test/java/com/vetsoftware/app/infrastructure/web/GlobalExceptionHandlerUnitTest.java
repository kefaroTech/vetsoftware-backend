package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionAlreadyOpenException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentAlreadyReversedException;
import com.vetsoftware.app.electronicdocument.domain.DocumentNotValidatedException;
import com.vetsoftware.app.employee.domain.AdminEmployeeCannotBeDisabledException;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.pdf.PdfRenderException;
import com.vetsoftware.app.infrastructure.storage.S3StorageException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import com.vetsoftware.app.openaccount.domain.InvalidOpenAccountStatusTransitionException;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import com.vetsoftware.app.openaccount.domain.OwnerAlreadyHasOpenAccountException;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogConflictException;
import com.vetsoftware.app.product.domain.ProductCodeAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNameAlreadyExistsException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.purchaseorder.domain.InvalidPurchaseOrderStatusTransitionException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;
import com.vetsoftware.app.registration.domain.EmployeeCodeAlreadyExistsException;
import com.vetsoftware.app.registration.domain.InvalidVerificationTokenException;
import com.vetsoftware.app.supplierinvoice.domain.InvalidSupplierInvoiceStateException;
import com.vetsoftware.app.auth.application.exception.EmailNotVerifiedException;
import com.vetsoftware.app.passwordreset.domain.InvalidPasswordResetTokenException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@code GlobalExceptionHandler} no es un {@code @RestController} (es
 * {@code @RestControllerAdvice}, ver CLAUDE.md): sus metodos {@code handleX}
 * son publicos y devuelven {@code ProblemDetail} directamente, asi que
 * invocarlos aqui es mas barato y mas preciso para cazar ramas que levantar un
 * {@code @WebMvcTest} por cada una de las ~40 familias de excepciones que
 * mapea. La rodaja {@code GlobalExceptionHandlerTest} (@WebMvcTest) ya prueba
 * que el {@code @ControllerAdvice} esta bien cableado end-to-end para un
 * subconjunto representativo; este test cubre el resto de metodos y el if-chain
 * de {@code handleDataIntegrity}, que ningun otro test ejercita.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — metodos no cubiertos por la rodaja web")
class GlobalExceptionHandlerUnitTest {

    @Mock
    private AuditLogger auditLogger;
    @Mock
    private io.micrometer.tracing.Tracer tracer;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Nested
    @DisplayName("conflictos (409) de una sola excepcion")
    class Conflictos409 {

        @Test
        @DisplayName("no se puede deshabilitar un empleado ADMIN")
        void no_se_puede_deshabilitar_un_empleado_admin() {
            ProblemDetail pd = handler.handleAdminEmployeeCannotBeDisabled(
                    new AdminEmployeeCannotBeDisabledException(3L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "ADMIN_EMPLOYEE_CANNOT_BE_DISABLED");
            assertThat(pd.getDetail()).contains("ADMIN role");
        }

        @Test
        @DisplayName("no se puede borrar una entidad con hijos activos")
        void no_se_puede_borrar_una_entidad_con_hijos_activos() {
            ProblemDetail pd = handler.handleHasActiveChildren(
                    new AnimalHasActiveChildrenException(7L, "weight_records"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "ENTITY_HAS_ACTIVE_CHILDREN");
            assertThat(pd.getDetail()).contains("active");
        }

        @Test
        @DisplayName("transicion de estado invalida en orden de compra")
        void transicion_de_estado_invalida_en_orden_de_compra() {
            ProblemDetail pd = handler.handlePurchaseStatusTransition(
                    new InvalidPurchaseOrderStatusTransitionException(PurchaseOrderStatus.DRAFT,
                            PurchaseOrderStatus.CANCELLED));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "INVALID_PURCHASE_ORDER_STATUS_TRANSITION");
        }

        @Test
        @DisplayName("estado invalido de la factura de proveedor")
        void estado_invalido_de_la_factura_de_proveedor() {
            ProblemDetail pd = handler.handleInvalidSupplierInvoiceState(
                    new InvalidSupplierInvoiceStateException("La factura ya fue anulada"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "INVALID_SUPPLIER_INVOICE_STATE");
            assertThat(pd.getDetail()).isEqualTo("La factura ya fue anulada");
        }

        @Test
        @DisplayName("perfil tributario ya existe para la empresa")
        void perfil_tributario_ya_existe_para_la_empresa() {
            ProblemDetail pd = handler.handleCompanyTaxProfileAlreadyExists(
                    new CompanyTaxProfileAlreadyExistsException(5L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "COMPANY_TAX_PROFILE_ALREADY_EXISTS");
        }

        @Test
        @DisplayName("codigo de producto ya existe")
        void codigo_de_producto_ya_existe() {
            ProblemDetail pd = handler
                    .handleProductCodeAlreadyExists(new ProductCodeAlreadyExistsException("SKU-1"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "PRODUCT_CODE_ALREADY_EXISTS");
        }

        @Test
        @DisplayName("nombre ya existe: el codigo se deriva de la clase concreta")
        void nombre_ya_existe_el_codigo_se_deriva_de_la_clase_concreta() {
            ProblemDetail pd = handler
                    .handleNameAlreadyExists(new ProductNameAlreadyExistsException("Collar"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "PRODUCT_NAME_ALREADY_EXISTS");
        }

        @Test
        @DisplayName("el propietario ya tiene una cuenta abierta")
        void el_propietario_ya_tiene_una_cuenta_abierta() {
            ProblemDetail pd = handler
                    .handleOwnerAlreadyHasOpenAccount(new OwnerAlreadyHasOpenAccountException(9L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "OWNER_ALREADY_HAS_OPEN_ACCOUNT");
        }

        @Test
        @DisplayName("ya hay una resolucion de numeracion activa para ese tipo")
        void ya_hay_una_resolucion_de_numeracion_activa() {
            ProblemDetail pd = handler.handleNumberingResolutionAlreadyActive(
                    new NumberingResolutionAlreadyActiveException(2L,
                            com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType.FE_VENTA));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "NUMBERING_RESOLUTION_ALREADY_ACTIVE");
        }

        @Test
        @DisplayName("transicion de estado invalida en cuenta abierta")
        void transicion_de_estado_invalida_en_cuenta_abierta() {
            ProblemDetail pd = handler.handleInvalidOpenAccountStatusTransition(
                    new InvalidOpenAccountStatusTransitionException(OpenAccountStatus.CLOSE,
                            OpenAccountStatus.OPEN));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "INVALID_OPEN_ACCOUNT_STATUS_TRANSITION");
        }

        @Test
        @DisplayName("abono ya anulado")
        void abono_ya_anulado() {
            ProblemDetail pd = handler.handleDebtOpenAccountAlreadyVoided(
                    new DebtOpenAccountAlreadyVoidedException(7L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "DEBT_OPEN_ACCOUNT_ALREADY_VOIDED");
        }

        @Test
        @DisplayName("cargo ya anulado")
        void cargo_ya_anulado() {
            ProblemDetail pd = handler.handleChargeOpenAccountAlreadyVoided(
                    new ProductChargeOpenAccountAlreadyVoidedException(8L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "CHARGE_OPEN_ACCOUNT_ALREADY_VOIDED");
        }

        @Test
        @DisplayName("documento no validado: no se puede corregir")
        void documento_no_validado_no_se_puede_corregir() {
            ProblemDetail pd = handler.handleDocumentNotValidated(
                    new DocumentNotValidatedException(4L, DianStatus.PENDIENTE));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "DOCUMENT_NOT_VALIDATED");
            assertThat(pd.getDetail()).contains("PENDIENTE");
        }

        @Test
        @DisplayName("documento ya reversado por otra nota credito")
        void documento_ya_reversado_por_otra_nota_credito() {
            ProblemDetail pd = handler
                    .handleDocumentAlreadyReversed(new DocumentAlreadyReversedException(6L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "DOCUMENT_ALREADY_REVERSED");
        }

        @Test
        @DisplayName("conflicto de sesion de caja: el codigo se deriva de la clase concreta")
        void conflicto_de_sesion_de_caja() {
            ProblemDetail pd = handler
                    .handleCashSessionConflict(new EmployeeCashSessionAlreadyOpenException());

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code",
                    "EMPLOYEE_CASH_SESSION_ALREADY_OPEN");
        }

        @Test
        @DisplayName("estado ilegal (guard de inmutabilidad de cuentas no-OPEN)")
        void estado_ilegal() {
            ProblemDetail pd = handler
                    .handleConflictState(new IllegalStateException("cuenta no esta abierta"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "INVALID_STATE");
            assertThat(pd.getDetail()).isEqualTo("cuenta no esta abierta");
        }

        @Test
        @DisplayName("bloqueo optimista: el detail es fijo, no el mensaje interno")
        void bloqueo_optimista() {
            ProblemDetail pd = handler.handleOptimisticLock(
                    new ObjectOptimisticLockingFailureException(Object.class, 1L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "CONCURRENT_MODIFICATION");
            assertThat(pd.getDetail())
                    .isEqualTo("El registro fue modificado por otra operación. Reintenta.");
        }

        @Test
        @DisplayName("conflicto de catalogo petshop: usa el codigo propio de la excepcion")
        void conflicto_de_catalogo_petshop() {
            ProblemDetail pd = handler.handlePetshopCatalogConflict(
                    new PetshopCatalogConflictException("PETSHOP_DUP", "ya existe en el catalogo"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "PETSHOP_DUP");
            assertThat(pd.getDetail()).isEqualTo("ya existe en el catalogo");
        }

        @Test
        @DisplayName("version de cuenta desactualizada: el detail es fijo")
        void version_de_cuenta_desactualizada() {
            ProblemDetail pd = handler.handleOpenAccountVersionConflict(
                    new OpenAccountVersionConflictException(1L, 2L, 3L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "CONCURRENT_MODIFICATION");
            assertThat(pd.getDetail())
                    .isEqualTo("La cuenta fue modificada por otra operación. Reintenta.");
        }
    }

    @Nested
    @DisplayName("appointment overlap: la lista de citas visibles viaja en la propiedad")
    class AppointmentOverlap {

        @Test
        @DisplayName("expone overlappingAppointmentIds y el codigo APPOINTMENT_OVERLAP")
        void expone_overlapping_appointment_ids() {
            var ex = new com.vetsoftware.app.appointment.domain.AppointmentOverlapException(11L,
                    "Dra. Ana", java.time.LocalDateTime.of(2026, 8, 17, 10, 0),
                    java.time.LocalDateTime.of(2026, 8, 17, 10, 30), List.of(101L, 102L), 3);

            ProblemDetail pd = handler.handleAppointmentOverlap(ex);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "APPOINTMENT_OVERLAP");
            assertThat(pd.getProperties()).containsEntry("overlappingAppointmentIds",
                    List.of(101L, 102L));
        }
    }

    @Nested
    @DisplayName("400 / 401 / 403 con efecto de auditoria")
    class AutenticacionYRegistro {

        @Test
        @DisplayName("correo no verificado: audita con el identificador y responde 403")
        void correo_no_verificado_audita_y_responde_403() {
            ProblemDetail pd = handler
                    .handleEmailNotVerified(new EmailNotVerifiedException("ana01"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(pd.getProperties()).containsEntry("code", "EMAIL_NOT_VERIFIED");
            verify(auditLogger).loginBlockedEmailNotVerified("ana01");
        }

        @Test
        @DisplayName("captcha fallido responde 400 con detail fijo")
        void captcha_fallido_responde_400() {
            ProblemDetail pd = handler
                    .handleCaptchaFailed(new CaptchaVerificationException("score bajo"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "CAPTCHA_FAILED");
        }

        @Test
        @DisplayName("token de verificacion invalido responde 400")
        void token_de_verificacion_invalido_responde_400() {
            ProblemDetail pd = handler.handleInvalidVerificationToken(
                    new InvalidVerificationTokenException("expirado"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "INVALID_VERIFICATION_TOKEN");
        }

        @Test
        @DisplayName("token de restablecimiento invalido responde 400")
        void token_de_restablecimiento_invalido_responde_400() {
            ProblemDetail pd = handler.handleInvalidPasswordResetToken(
                    new InvalidPasswordResetTokenException("usado"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "INVALID_PASSWORD_RESET_TOKEN");
        }

        @Test
        @DisplayName("correo ya registrado responde 409")
        void correo_ya_registrado_responde_409() {
            ProblemDetail pd = handler.handleEmailAlreadyRegistered(
                    new EmployeeCodeAlreadyExistsException("ana@x.co"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "EMAIL_ALREADY_REGISTERED");
        }

        @Test
        @DisplayName("acceso denegado generico: audita metodo y ruta, detail fijo sin motivo")
        void acceso_denegado_generico_audita_metodo_y_ruta() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("DELETE");
            request.setRequestURI("/products/9");

            ProblemDetail pd = handler
                    .handleAccessDenied(new AccessDeniedException("motivo interno"), request);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(pd.getProperties()).containsEntry("code", "FORBIDDEN");
            assertThat(pd.getDetail()).isEqualTo("Access denied");
            ArgumentCaptor<String> method = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> uri = ArgumentCaptor.forClass(String.class);
            verify(auditLogger).accessDenied(method.capture(), uri.capture());
            assertThat(method.getValue()).isEqualTo("DELETE");
            assertThat(uri.getValue()).isEqualTo("/products/9");
        }

        @Test
        @DisplayName("fallo de autenticacion: audita y responde 401 sin filtrar el motivo")
        void fallo_de_autenticacion_audita_y_responde_401() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/auth/login");

            ProblemDetail pd = handler
                    .handleAuthenticationFailure(new BadCredentialsException("bad creds"), request);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(pd.getProperties()).containsEntry("code", "UNAUTHENTICATED");
            assertThat(pd.getDetail()).isEqualTo("Authentication required");
            verify(auditLogger).loginFailure(eq("/auth/login"), eq("authentication_failed"));
        }
    }

    @Nested
    @DisplayName("errores de infraestructura (502) marcan la observacion de error")
    class ErroresDeInfraestructura {

        @Test
        @DisplayName("fallo al generar el PDF responde 502 FILE_STORAGE aparte, con codigo propio")
        void fallo_al_generar_el_pdf_responde_502() {
            ProblemDetail pd = handler.handlePdfRender(new PdfRenderException("template roto"),
                    new MockHttpServletRequest());

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
            assertThat(pd.getProperties()).containsEntry("code", "PDF_RENDER_FAILED");
        }

        @Test
        @DisplayName("fallo de S3 responde 502 sin filtrar el detalle interno del SDK")
        void fallo_de_s3_responde_502() {
            ProblemDetail pd = handler.handleS3Storage(
                    new S3StorageException("Failed to upload object to S3: k"),
                    new MockHttpServletRequest());

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
            assertThat(pd.getProperties()).containsEntry("code", "FILE_STORAGE_FAILED");
            assertThat(pd.getDetail()).isEqualTo("Failed to access file storage");
        }
    }

    @Nested
    @DisplayName("handleDataIntegrity: el if-chain que traduce la constraint de MySQL")
    class HandleDataIntegrity {

        static Stream<Arguments> causasConocidas() {
            return Stream.of(
                    Arguments.of("uq_open_accounts_active_owner_branch",
                            "OWNER_ALREADY_HAS_OPEN_ACCOUNT"),
                    Arguments.of("uq_open_accounts_active_owner", "OWNER_ALREADY_HAS_OPEN_ACCOUNT"),
                    Arguments.of("uq_products_company_active_code", "PRODUCT_CODE_ALREADY_EXISTS"),
                    Arguments.of("uq_products_company_active_name", "PRODUCT_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_product_categories_company_active_name",
                            "PRODUCT_CATEGORY_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_service_categories_company_active_name",
                            "SERVICE_CATEGORY_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_taxes_company_active_name", "TAX_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_suppliers_company_active_name",
                            "SUPPLIER_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_supplier_invoices_active_number",
                            "SUPPLIER_INVOICE_NUMBER_ALREADY_EXISTS"),
                    Arguments.of("uq_electronic_documents_open_account",
                            "DOCUMENT_ALREADY_EMITTED"),
                    Arguments.of("uq_debt_open_accounts_request", "DUPLICATE_PAYMENT_REQUEST"),
                    Arguments.of("uq_product_charge_open_accounts_request",
                            "DUPLICATE_CHARGE_REQUEST"),
                    Arguments.of("uq_service_charge_open_accounts_request",
                            "DUPLICATE_CHARGE_REQUEST"),
                    Arguments.of("uq_general_charge_open_accounts_request",
                            "DUPLICATE_CHARGE_REQUEST"),
                    Arguments.of("uq_numbering_resolutions_active",
                            "NUMBERING_RESOLUTION_ALREADY_ACTIVE"),
                    Arguments.of("employee_code", "EMPLOYEE_CODE_TAKEN"),
                    Arguments.of("uq_cash_session_employee_open",
                            "EMPLOYEE_CASH_SESSION_ALREADY_OPEN"),
                    Arguments.of("uq_cash_session_open", "CASH_SESSION_ALREADY_OPEN"));
        }

        @ParameterizedTest(name = "causa \"{0}\" -> {1}")
        @MethodSource("causasConocidas")
        @DisplayName("cada constraint conocida se traduce a su codigo de negocio, todas 409")
        void cada_constraint_conocida_se_traduce_a_su_codigo(String causa, String codigoEsperado) {
            ProblemDetail pd = handler
                    .handleDataIntegrity(new DataIntegrityViolationException(causa));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", codigoEsperado);
        }

        @Test
        @DisplayName("una constraint no reconocida cae al codigo generico, sin filtrar el mensaje de MySQL")
        void una_constraint_no_reconocida_cae_al_codigo_generico() {
            ProblemDetail pd = handler
                    .handleDataIntegrity(new DataIntegrityViolationException("uq_algo_no_mapeado"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "DATA_INTEGRITY_VIOLATION");
            assertThat(pd.getDetail()).isEqualTo("Database constraint violation");
        }

        @Test
        @DisplayName("sin causa mas especifica (mensaje null) tambien cae al codigo generico")
        void sin_causa_mas_especifica_tambien_cae_al_generico() {
            ProblemDetail pd = handler
                    .handleDataIntegrity(new DataIntegrityViolationException(null));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "DATA_INTEGRITY_VIOLATION");
        }
    }

    @Nested
    @DisplayName("handleExceptionInternal: logging por rango de estado y marcado de observacion")
    class HandleExceptionInternal {

        @Test
        @DisplayName("un 5xx sobre un ServletWebRequest marca el error en la observacion")
        void un_5xx_marca_el_error_en_la_observacion() {
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            var observation = new org.springframework.http.server.observation.ServerRequestObservationContext(
                    servletRequest, new org.springframework.mock.web.MockHttpServletResponse());
            servletRequest.setAttribute(
                    org.springframework.web.filter.ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE,
                    observation);
            ServletWebRequest webRequest = new ServletWebRequest(servletRequest);
            RuntimeException failure = new RuntimeException("boom");

            handler.handleExceptionInternal(failure, null, new HttpHeaders(),
                    HttpStatus.INTERNAL_SERVER_ERROR, webRequest);

            assertThat(observation.getError()).isSameAs(failure);
        }

        @Test
        @DisplayName("un 4xx no marca la observacion pero igual delega en el manejador base")
        void un_4xx_no_marca_la_observacion() {
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            var observation = new org.springframework.http.server.observation.ServerRequestObservationContext(
                    servletRequest, new org.springframework.mock.web.MockHttpServletResponse());
            servletRequest.setAttribute(
                    org.springframework.web.filter.ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE,
                    observation);
            ServletWebRequest webRequest = new ServletWebRequest(servletRequest);

            ResponseEntity<Object> response = handler.handleExceptionInternal(
                    new RuntimeException("bad input"), null, new HttpHeaders(),
                    HttpStatus.BAD_REQUEST, webRequest);

            assertThat(observation.getError()).isNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("un estado que no es 4xx ni 5xx no registra nada y solo delega")
        void un_estado_ni_4xx_ni_5xx_solo_delega() {
            ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

            ResponseEntity<Object> response = handler.handleExceptionInternal(
                    new RuntimeException("redirect"), null, new HttpHeaders(),
                    HttpStatus.NOT_MODIFIED, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        }
    }

    @Nested
    @DisplayName("overrides de validacion / binding heredados de ResponseEntityExceptionHandler")
    class OverridesDeValidacion {

        @Test
        @DisplayName("errores de @Valid se listan por campo bajo VALIDATION_FAILED")
        void errores_de_valid_se_listan_por_campo() throws Exception {
            Method target = OverridesDeValidacion.class.getDeclaredMethod("objetivo", String.class);
            MethodParameter parameter = new MethodParameter(target, 0);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(),
                    "command");
            bindingResult.addError(new FieldError("command", "name", "must not be blank"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter,
                    bindingResult);
            ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

            ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex,
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ProblemDetail pd = (ProblemDetail) response.getBody();
            assertThat(pd.getProperties()).containsEntry("code", "VALIDATION_FAILED");
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, String>> errors = (List<java.util.Map<String, String>>) pd
                    .getProperties().get("errors");
            assertThat(errors)
                    .contains(java.util.Map.of("field", "name", "message", "must not be blank"));
        }

        @Test
        @DisplayName("un body JSON ilegible responde 400 con codigo MALFORMED_REQUEST")
        void un_body_ilegible_responde_400() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "JSON parse error", new InputMessageVacio());
            ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

            ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(ex,
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ProblemDetail pd = (ProblemDetail) response.getBody();
            assertThat(pd.getProperties()).containsEntry("code", "MALFORMED_REQUEST");
        }

        // Solo existe para que `objetivo_target` tenga un Method con un parametro del
        // que colgar el MethodParameter de arriba; no se invoca nunca.
        @SuppressWarnings("unused")
        private static void objetivo(String name) {
        }

        /**
         * Body vacío: a {@code handleHttpMessageNotReadable} no le hace falta leerlo.
         */
        private static final class InputMessageVacio
                implements
                    org.springframework.http.HttpInputMessage {
            @Override
            public java.io.InputStream getBody() {
                return java.io.InputStream.nullInputStream();
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        }
    }
}
