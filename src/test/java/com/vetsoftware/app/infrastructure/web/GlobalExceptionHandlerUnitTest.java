package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionAlreadyOpenException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentAlreadyReversedException;
import com.vetsoftware.app.electronicdocument.domain.DocumentNotValidatedException;
import com.vetsoftware.app.electronicdocument.domain.NumberingResolutionNotEffectiveException;
import com.vetsoftware.app.electronicdocument.domain.NumberingResolutionRangeExhaustedException;
import com.vetsoftware.app.employee.domain.AdminEmployeeCannotBeDisabledException;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.pdf.PdfRenderException;
import com.vetsoftware.app.infrastructure.storage.S3StorageException;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
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
import com.vetsoftware.app.shared.pricing.PriceListNotEffectiveException;
import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;
import com.vetsoftware.app.registration.domain.EmployeeCodeAlreadyExistsException;
import com.vetsoftware.app.registration.domain.InvalidVerificationTokenException;
import com.vetsoftware.app.registration.infrastructure.security.CaptchaConfigurationException;
import com.vetsoftware.app.registration.infrastructure.security.CaptchaProviderUnavailableException;
import com.vetsoftware.app.supplierinvoice.domain.InvalidSupplierInvoiceStateException;
import com.vetsoftware.app.auth.application.exception.EmailNotVerifiedException;
import com.vetsoftware.app.passwordreset.domain.InvalidPasswordResetTokenException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

        /**
         * La guarda del service y el indice de la base emiten el MISMO errorCode a
         * proposito: al front le da igual quien detecto el choque, y un codigo distinto
         * le obligaria a escribir dos veces el mismo tratamiento. Este caso cierra el
         * par con el {@code uq_medicaments_*} del if-chain de mas abajo.
         */
        @Test
        @DisplayName("el catalogo clinico deriva su codigo igual: MEDICAMENT_NAME_ALREADY_EXISTS")
        void el_catalogo_clinico_deriva_su_codigo_igual() {
            ProblemDetail pd = handler.handleNameAlreadyExists(
                    new MedicamentNameAlreadyExistsException("Amoxicilina"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "MEDICAMENT_NAME_ALREADY_EXISTS");
            assertThat(pd.getDetail()).contains("Amoxicilina");
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
        @DisplayName("estado ilegal: el detail es fijo y el mensaje de dominio NO llega al cliente")
        void estado_ilegal_no_devuelve_el_mensaje_de_dominio() {
            // Este handler es el desague de ~70 `new IllegalStateException(` de src/main,
            // escritas para un operador y sin ningun contrato sobre lo que llevan dentro
            // (#118). El mensaje de ejemplo interpola dos datos internos a proposito: si
            // alguien reabre el paso del mensaje crudo, esa fuga tiene que romper aqui.
            ProblemDetail pd = handler.handleConflictState(
                    new IllegalStateException("la cuenta 4210 de la sede NORTE no esta abierta"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "INVALID_STATE");
            assertThat(pd.getDetail())
                    .isEqualTo("La operación no es válida para el estado actual del registro.")
                    .doesNotContain("4210", "NORTE", "no esta abierta");
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

        @Test
        @DisplayName("cuando el cruce lo caza la constraint, mismo codigo pero SIN lista de citas")
        void la_constraint_da_el_mismo_codigo_sin_lista_de_citas() {
            // Carrera que el check sincrono no ve: dos peticiones simultaneas sobre el
            // mismo hueco pasaban las dos el SELECT y se guardaban las dos (#114). El
            // indice unico cierra la carrera y esta rama traduce su error de integridad
            // al MISMO codigo que emite el check, para que el front no escriba dos veces
            // el mismo tratamiento.
            ProblemDetail pd = handler.handleDataIntegrity(new DataIntegrityViolationException(
                    "could not execute statement [Duplicate entry '7-2026-08-17 10:00:00'"
                            + " for key 'uq_appointments_active_employee_start']"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "APPOINTMENT_OVERLAP");
            // Por construccion NO puede llevarla: aqui lo unico que hay es el nombre de
            // la constraint, no se sabe ni con que choco. El front tiene que tolerar su
            // ausencia, y si algun dia alguien la rellena con una lista inventada, esto
            // rompe.
            assertThat(pd.getProperties()).doesNotContainKey("overlappingAppointmentIds");
            assertThat(pd.getDetail()).contains("ocupado")
                    .doesNotContain("uq_appointments_active_employee_start", "Duplicate entry");
        }
    }

    @Nested
    @DisplayName("tarifa fuera de vigencia (D-73): codigo propio y la ventana en propiedades")
    class TarifaNoVigente {

        @Test
        @DisplayName("COT-021: 409 con su propio codigo, la ventana y el dia con el que se comparo")
        void tarifa_fuera_de_vigencia() {
            ProblemDetail pd = handler
                    .handlePriceListNotEffective(new PriceListNotEffectiveException(7L,
                            "LISTA-2025-01", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                            LocalDate.of(2026, 8, 22)));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "PRICE_LIST_NOT_EFFECTIVE")
                    .containsEntry("priceListId", 7L)
                    .containsEntry("priceListCode", "LISTA-2025-01")
                    .containsEntry("validFrom", LocalDate.of(2025, 1, 1))
                    .containsEntry("validTo", LocalDate.of(2025, 12, 31))
                    .containsEntry("quotedOn", LocalDate.of(2026, 8, 22));
            // El detail lo compone el handler; el mensaje de la excepcion no sale (#118).
            assertThat(pd.getDetail()).contains("no está vigente")
                    .doesNotContain("Price list is not effective");
        }

        @Test
        @DisplayName("COT-020: una lista sin fecha de fin no inventa un validTo en la respuesta")
        void una_lista_sin_cierre_no_inventa_validTo() {
            ProblemDetail pd = handler.handlePriceListNotEffective(
                    new PriceListNotEffectiveException(9L, "LISTA-ABIERTA",
                            LocalDate.of(2027, 1, 1), null, LocalDate.of(2026, 8, 22)));

            assertThat(pd.getProperties()).containsEntry("validFrom", LocalDate.of(2027, 1, 1))
                    .doesNotContainKey("validTo");
        }
    }

    @Nested
    @DisplayName("numeracion DIAN (#125): codigo propio por hecho y los datos en propiedades")
    class NumeracionDian {

        @Test
        @DisplayName("resolucion fuera de vigencia: 409 con su codigo y las fechas de vigencia")
        void resolucion_fuera_de_vigencia() {
            ProblemDetail pd = handler.handleNumberingResolutionNotEffective(
                    new NumberingResolutionNotEffectiveException("18760000005",
                            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties())
                    .containsEntry("code", "NUMBERING_RESOLUTION_NOT_EFFECTIVE")
                    .containsEntry("resolutionNumber", "18760000005")
                    .containsEntry("validFrom", LocalDate.of(2020, 1, 1))
                    .containsEntry("validTo", LocalDate.of(2020, 12, 31));
            // El detail lo compone el handler; el mensaje de la excepcion no sale (#118).
            assertThat(pd.getDetail()).contains("no está vigente")
                    .doesNotContain("Numbering resolution is not effective");
        }

        @Test
        @DisplayName("rango agotado: OTRO codigo distinto, porque la accion del operador es otra")
        void rango_agotado() {
            ProblemDetail pd = handler.handleNumberingResolutionRangeExhausted(
                    new NumberingResolutionRangeExhaustedException("18760000007", 100L));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties())
                    .containsEntry("code", "NUMBERING_RESOLUTION_RANGE_EXHAUSTED")
                    .containsEntry("resolutionNumber", "18760000007")
                    .containsEntry("rangeTo", 100L);
            assertThat(pd.getDetail()).contains("rango")
                    .doesNotContain("Numbering resolution range is exhausted");
        }

        @Test
        @DisplayName("ninguno de los dos sale ya como INVALID_STATE")
        void ninguno_sale_como_invalid_state() {
            ProblemDetail vigencia = handler.handleNumberingResolutionNotEffective(
                    new NumberingResolutionNotEffectiveException("1", LocalDate.of(2020, 1, 1),
                            LocalDate.of(2020, 12, 31)));
            ProblemDetail rango = handler.handleNumberingResolutionRangeExhausted(
                    new NumberingResolutionRangeExhaustedException("1", 9L));

            // La regresion que #125 tiene que impedir: volver a colapsar los dos hechos
            // en el codigo generico con el que salian «la cuenta no esta abierta» y otra
            // veintena de guardas de estado.
            assertThat(vigencia.getProperties().get("code")).isNotEqualTo("INVALID_STATE")
                    .isNotEqualTo(rango.getProperties().get("code"));
        }

        @Test
        @DisplayName("un campo nulo no se publica como propiedad nula")
        void un_campo_nulo_no_se_publica() {
            ProblemDetail vigencia = handler.handleNumberingResolutionNotEffective(
                    new NumberingResolutionNotEffectiveException(null, LocalDate.of(2020, 1, 1),
                            LocalDate.of(2020, 12, 31)));
            ProblemDetail rango = handler.handleNumberingResolutionRangeExhausted(
                    new NumberingResolutionRangeExhaustedException(null, null));

            assertThat(vigencia.getProperties()).doesNotContainKey("resolutionNumber")
                    .containsKey("validFrom");
            assertThat(rango.getProperties()).doesNotContainKey("resolutionNumber")
                    .doesNotContainKey("rangeTo");
        }
    }

    @Nested
    @DisplayName("400 / 401 / 403: efecto de auditoria y detail constante")
    class AutenticacionYRegistro {

        @Test
        @DisplayName("correo no verificado: un identificador que no es correo se audita entero")
        void correo_no_verificado_audita_y_responde_403() {
            ProblemDetail pd = handler
                    .handleEmailNotVerified(new EmailNotVerifiedException("ana01"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(pd.getProperties()).containsEntry("code", "EMAIL_NOT_VERIFIED");
            // Un codigo de empleado que no es un correo no casa con ningun patron y sale
            // tal cual: es lo que documenta AuditLogger y lo que necesita quien investiga.
            verify(auditLogger).loginBlockedEmailNotVerified("ana01");
        }

        @Test
        @DisplayName("correo no verificado: si el identificador ES un correo, se enmascara antes de auditar")
        void correo_no_verificado_enmascara_el_identificador_que_es_un_correo() {
            // En el auto-registro el codigo de empleado ES el correo del dueño, que es el
            // caso mayoritario y el que el test viejo con "ana01" no ejercitaba (#180).
            // AuditLogger lo pone en dos sitios de la misma linea, y actor.identifier
            // esta en la allowlist VERBATIM de LogFieldPolicy: sin enmascarar AQUI, el
            // correo entero llegaba a Loki al lado de su propia version redactada.
            ProblemDetail pd = handler.handleEmailNotVerified(
                    new EmailNotVerifiedException("ana.gomez@clinicanorte.com"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(pd.getProperties()).containsEntry("code", "EMAIL_NOT_VERIFIED");
            assertThat(pd.getDetail()).doesNotContain("ana.gomez");

            ArgumentCaptor<String> auditado = ArgumentCaptor.forClass(String.class);
            verify(auditLogger).loginBlockedEmailNotVerified(auditado.capture());
            // El dominio se conserva a proposito: no es dato personal y sirve para
            // diagnosticar. Lo que no puede sobrevivir es la parte local.
            assertThat(auditado.getValue()).isEqualTo("***@clinicanorte.com")
                    .doesNotContain("ana.gomez");
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

        @Test
        @DisplayName("argumento ilegal: el detail es fijo, no el mensaje que enumeraba empleados")
        void argumento_ilegal_no_devuelve_el_mensaje_de_dominio() {
            // Caso testigo de #118: CreateAppointmentService lanzaba
            // "Employee not found: " + employeeId, y devolverlo convertia el endpoint de
            // crear cita en un oraculo para enumerar empleados de otras empresas
            // probando ids. Alimentan este handler ~1.500 IllegalArgumentException de
            // src/main, asi que el volumen lo hace mas grave que su hermano de 409.
            ProblemDetail pd = handler
                    .handleBadRequest(new IllegalArgumentException("Employee not found: 4210"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "INVALID_INPUT");
            assertThat(pd.getDetail()).isEqualTo("Los datos enviados no son válidos.")
                    .doesNotContain("4210", "Employee not found");
        }
    }

    /**
     * Las tres poblaciones del captcha (#99). Lo que se afirma aquí y no en la
     * rodaja web es la <b>severidad</b>: las tres responden el mismo 400 con el
     * mismo código, así que la respuesta HTTP no distingue una caída total de la
     * configuración de un token caducado. Lo único que las separa es el nivel del
     * evento, y por eso el {@code ListAppender} cuelga del logger de la clase.
     */
    @Nested
    @DisplayName("captcha (#99): tres poblaciones, tres severidades, un solo punto de registro")
    class CaptchaTresPoblaciones {

        private static final String DETAIL_FIJO = "No pudimos verificar el captcha. Inténtalo de nuevo.";

        private Logger canal;
        private ListAppender<ILoggingEvent> sumidero;
        private Level nivelPrevio;

        @BeforeEach
        void engancharElCanal() {
            LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            sumidero = new ListAppender<>();
            sumidero.setContext(context);
            sumidero.start();

            canal = context.getLogger(GlobalExceptionHandler.class);
            nivelPrevio = canal.getLevel();
            canal.setLevel(Level.INFO);
            canal.addAppender(sumidero);
        }

        @AfterEach
        void soltarElCanal() {
            canal.detachAppender(sumidero);
            canal.setLevel(nivelPrevio);
            sumidero.stop();
        }

        private ILoggingEvent emitido() {
            assertThat(sumidero.list).as("el handler no emitio ningun evento de log").hasSize(1);
            return sumidero.list.getFirst();
        }

        @Test
        @DisplayName("mal configurado: ERROR con la causa adjunta y la observacion marcada")
        void mal_configurado_se_registra_en_error() {
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            var observation = new org.springframework.http.server.observation.ServerRequestObservationContext(
                    servletRequest, new org.springframework.mock.web.MockHttpServletResponse());
            servletRequest.setAttribute(
                    org.springframework.web.filter.ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE,
                    observation);
            var ex = new CaptchaConfigurationException(
                    "reCAPTCHA siteverify rejected the request with 403 FORBIDDEN;"
                            + " check 'vetsoftware.recaptcha.secret'",
                    new RuntimeException("403 Forbidden"));

            ProblemDetail pd = handler.handleCaptchaMisconfigured(ex, servletRequest);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "CAPTCHA_FAILED");
            assertThat(pd.getDetail()).isEqualTo(DETAIL_FIJO)
                    .doesNotContain("vetsoftware.recaptcha.secret");
            // La observacion se marca aunque la respuesta sea 400: el request salio
            // fallido de verdad y en las metricas tiene que contarse como tal.
            assertThat(observation.getError()).isSameAs(ex);
            assertThat(emitido().getLevel()).isEqualTo(Level.ERROR);
            assertThat(emitido().getThrowableProxy())
                    .as("sin la causa no hay nada que diagnosticar").isNotNull();
        }

        @Test
        @DisplayName("proveedor caido: WARN con la causa, que es lo unico que separa un timeout de un 503")
        void proveedor_caido_se_registra_en_warn() {
            var ex = new CaptchaProviderUnavailableException(
                    "reCAPTCHA siteverify call failed: ResourceAccessException",
                    new RuntimeException("read timed out"));

            ProblemDetail pd = handler.handleCaptchaProviderUnavailable(ex);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "CAPTCHA_FAILED");
            assertThat(pd.getDetail()).isEqualTo(DETAIL_FIJO);
            assertThat(emitido().getLevel()).isEqualTo(Level.WARN);
            assertThat(emitido().getThrowableProxy()).isNotNull();
        }

        @Test
        @DisplayName("captcha no superado por el usuario: INFO, que es la poblacion dominante")
        void captcha_no_superado_se_registra_en_info() {
            ProblemDetail pd = handler
                    .handleCaptchaFailed(new CaptchaVerificationException("Captcha score too low"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getProperties()).containsEntry("code", "CAPTCHA_FAILED");
            assertThat(pd.getDetail()).isEqualTo(DETAIL_FIJO).doesNotContain("score too low");
            // Un 4xx atribuible al cliente no puede copar el nivel: en WARN estas lineas
            // enterraban lo que si exige mirar (#89).
            assertThat(emitido().getLevel()).isEqualTo(Level.INFO);
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
                    Arguments.of("uq_cash_session_open", "CASH_SESSION_ALREADY_OPEN"),
                    // Los siete catalogos clinicos de #559. Cada uno entra DOS veces a
                    // proposito: el nombre definitivo (uq_<tabla>_owner_active_name, que
                    // llega con los changesets 285/288) y el que hay hoy en la base, que
                    // en seis de las siete tablas se llama literalmente `name` y el driver
                    // reporta como `<tabla>.name`. Los dos tienen que salir por el MISMO
                    // errorCode: si solo estuviera mapeado el nuevo, un rollback del
                    // changeset devolveria el 409 generico en ingles justo cuando el
                    // despliegue ya ha ido mal.
                    Arguments.of("uq_vaccination_types_owner_active_name",
                            "VACCINATION_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("vaccination_types.name", "VACCINATION_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_surgery_types_owner_active_name",
                            "SURGERY_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("surgery_types.name", "SURGERY_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_laboratory_test_types_owner_active_name",
                            "LABORATORY_TEST_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("laboratory_test_types.name",
                            "LABORATORY_TEST_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_diagnostic_imaging_types_owner_active_name",
                            "DIAGNOSTIC_IMAGING_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("diagnostic_imaging_types.name",
                            "DIAGNOSTIC_IMAGING_TYPE_NAME_ALREADY_EXISTS"),
                    // medicaments es la excepcion: su indice de hoy ya tiene nombre propio
                    // desde la migracion 173, asi que el par es uq_medicaments_name (viejo)
                    // / uq_medicaments_owner_active_name (nuevo), y NO `medicaments.name`.
                    Arguments.of("uq_medicaments_owner_active_name",
                            "MEDICAMENT_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_medicaments_name", "MEDICAMENT_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_consultation_types_owner_active_name",
                            "CONSULTATION_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("consultation_types.name",
                            "CONSULTATION_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("uq_spa_types_owner_active_name", "SPA_TYPE_NAME_ALREADY_EXISTS"),
                    Arguments.of("spa_types.name", "SPA_TYPE_NAME_ALREADY_EXISTS"));
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

        /**
         * El if-chain compara por {@code contains} sobre el mensaje CRUDO del driver,
         * no sobre un nombre de constraint ya extraido. Alimentarlo con el nombre
         * pelado —como hace el caso parametrizado— no demuestra que el mensaje real de
         * MySQL, con su prefijo de tabla y su valor duplicado delante, tambien case.
         * Este caso lo fija con el texto tal y como llega de Connector/J.
         */
        @Test
        @DisplayName("el mensaje crudo de MySQL, con prefijo de tabla y valor duplicado, tambien casa")
        void el_mensaje_crudo_de_mysql_tambien_casa() {
            ProblemDetail pd = handler.handleDataIntegrity(new DataIntegrityViolationException(
                    "Duplicate entry '900-Amoxicilina' for key 'medicaments.uq_medicaments_owner_active_name'"));

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(pd.getProperties()).containsEntry("code", "MEDICAMENT_NAME_ALREADY_EXISTS");
            // El detail que sale al cliente es el de negocio, en español: el mensaje de
            // MySQL arrastra el valor duplicado y jamas se publica.
            assertThat(pd.getDetail()).doesNotContain("Duplicate entry").doesNotContain("900-");
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
