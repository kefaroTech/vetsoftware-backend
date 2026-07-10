package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.WeightRecordNotFoundException;
import com.vetsoftware.app.animalalert.domain.AnimalAlertNotFoundException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorHasActiveChildrenException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.InvalidAppointmentTransitionException;
import com.vetsoftware.app.auth.application.exception.EmailNotVerifiedException;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.basepermission.domain.BasePermissionHasActiveChildrenException;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import com.vetsoftware.app.baserole.domain.BaseRoleHasActiveChildrenException;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import com.vetsoftware.app.breed.domain.BreedHasActiveChildrenException;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import com.vetsoftware.app.city.domain.CityHasActiveChildrenException;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import com.vetsoftware.app.company.domain.CompanyHasActiveChildrenException;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import com.vetsoftware.app.consultation.domain.ConsultationHasActiveChildrenException;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeHasActiveChildrenException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import com.vetsoftware.app.country.domain.CountryHasActiveChildrenException;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeHasActiveChildrenException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import com.vetsoftware.app.employee.domain.AdminEmployeeCannotBeDisabledException;
import com.vetsoftware.app.employee.domain.EmployeeHasActiveChildrenException;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.pdf.PdfRenderException;
import com.vetsoftware.app.infrastructure.storage.S3StorageException;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeHasActiveChildrenException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import com.vetsoftware.app.medicament.domain.MedicamentHasActiveChildrenException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import com.vetsoftware.app.membership.domain.MembershipHasActiveChildrenException;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import com.vetsoftware.app.module.domain.ModuleHasActiveChildrenException;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;
import com.vetsoftware.app.registration.domain.InvalidVerificationTokenException;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import com.vetsoftware.app.electronicdocument.domain.DocumentAlreadyReversedException;
import com.vetsoftware.app.electronicdocument.domain.DocumentNotValidatedException;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfigNotFoundException;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfigNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.InvalidOpenAccountStatusTransitionException;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import com.vetsoftware.app.openaccount.domain.OwnerAlreadyHasOpenAccountException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import com.vetsoftware.app.owner.domain.OwnerHasActiveChildrenException;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import com.vetsoftware.app.permission.domain.PermissionHasActiveChildrenException;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.prescription.domain.PrescriptionHasActiveChildrenException;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import com.vetsoftware.app.problem.domain.ProblemNotFoundException;
import com.vetsoftware.app.role.domain.RoleHasActiveChildrenException;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.spatype.domain.SpaTypeHasActiveChildrenException;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import com.vetsoftware.app.specie.domain.SpecieHasActiveChildrenException;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeHasActiveChildrenException;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import com.vetsoftware.app.state.domain.StateHasActiveChildrenException;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import com.vetsoftware.app.submodule.domain.SubModuleHasActiveChildrenException;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import com.vetsoftware.app.systempermission.domain.SystemPermissionHasActiveChildrenException;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import com.vetsoftware.app.systemuser.domain.SystemUserHasActiveChildrenException;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeHasActiveChildrenException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import com.vetsoftware.app.product.domain.ProductCodeAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNameAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryHasActiveChildrenException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNameAlreadyExistsException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryHasActiveChildrenException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNameAlreadyExistsException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import com.vetsoftware.app.tax.domain.TaxHasActiveChildrenException;
import com.vetsoftware.app.tax.domain.TaxNameAlreadyExistsException;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// @Order(HIGHEST_PRECEDENCE): gana sobre el ProblemDetailsExceptionHandler interno de
// Spring Boot (registrado con @Order(0) por spring.mvc.problemdetails.enabled=true), que
// de otro modo resolvía las excepciones estándar de MVC en silencio y eclipsaba estos
// handlers. Al extender ResponseEntityExceptionHandler, este advice pasa a manejar (y
// loguear) validación de body, JSON ilegible, 405, 415, parámetros faltantes, etc.
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogger auditLogger;

    public GlobalExceptionHandler(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Server error {} on {}", statusCode.value(), request.getDescription(false), ex);
        } else if (statusCode.is4xxClientError()) {
            log.warn("Client error {} on {}: {}",
                    statusCode.value(), request.getDescription(false), ex.getMessage());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    @ExceptionHandler({
            CompanyNotFoundException.class, EmployeeNotFoundException.class,
            MembershipNotFoundException.class, MembershipSubModuleNotFoundException.class,
            ModuleNotFoundException.class, PermissionNotFoundException.class,
            SubModuleNotFoundException.class, BasePermissionNotFoundException.class,
            BaseRoleNotFoundException.class, BaseRolePermissionNotFoundException.class,
            CountryNotFoundException.class, StateNotFoundException.class,
            CityNotFoundException.class, RoleNotFoundException.class,
            RolePermissionNotFoundException.class, EmployeeRoleNotFoundException.class,
            SystemUserNotFoundException.class, SystemPermissionNotFoundException.class,
            SystemUserPermissionNotFoundException.class,
            SpecieNotFoundException.class, BreedNotFoundException.class,
            OwnerNotFoundException.class, AnimalNotFoundException.class,
            WeightRecordNotFoundException.class,
            AnimalColorNotFoundException.class,
            ProblemNotFoundException.class, AnimalAlertNotFoundException.class,
            AppointmentNotFoundException.class,
            ConsultationTypeNotFoundException.class, ConsultationNotFoundException.class,
            VaccinationTypeNotFoundException.class, VaccinationNotFoundException.class,
            HospitalizationNotFoundException.class,
            HospitalizationObservationNotFoundException.class,
            HospitalizationProgressNoteNotFoundException.class,
            HospitalizationMedicationNotFoundException.class,
            HospitalizationProcedureNotFoundException.class,
            LaboratoryTestTypeNotFoundException.class, LaboratoryTestNotFoundException.class,
            LaboratoryTestFileNotFoundException.class,
            PrescriptionNotFoundException.class, DewormingNotFoundException.class,
            DayCareNotFoundException.class,
            SpaTypeNotFoundException.class, SpaNotFoundException.class,
            MedicamentPrescriptionNotFoundException.class, MedicamentNotFoundException.class,
            SurgeryTypeNotFoundException.class, SurgeryNotFoundException.class,
            DiagnosticImagingTypeNotFoundException.class, DiagnosticImagingNotFoundException.class,
            TaxNotFoundException.class, ProductCategoryNotFoundException.class,
            ServiceCategoryNotFoundException.class, ProductNotFoundException.class,
            ServiceNotFoundException.class, PromotionNotFoundException.class,
            OpenAccountNotFoundException.class, DebtOpenAccountNotFoundException.class,
            ProductChargeOpenAccountNotFoundException.class, ServiceChargeOpenAccountNotFoundException.class,
            GeneralChargeOpenAccountNotFoundException.class,
            EconomicActivityNotFoundException.class, CompanyTaxProfileNotFoundException.class,
            NumberingResolutionNotFoundException.class, ElectronicDocumentNotFoundException.class,
            DianProviderConfigNotFoundException.class, WithholdingConfigNotFoundException.class
    })
    public ProblemDetail handleNotFound(RuntimeException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, errorCode(ex), ex.getMessage());
    }

    @ExceptionHandler({
            ConsultationTypeHasActiveChildrenException.class,
            VaccinationTypeHasActiveChildrenException.class,
            SurgeryTypeHasActiveChildrenException.class,
            LaboratoryTestTypeHasActiveChildrenException.class,
            MedicamentHasActiveChildrenException.class,
            DiagnosticImagingTypeHasActiveChildrenException.class,
            SpaTypeHasActiveChildrenException.class,
            AnimalColorHasActiveChildrenException.class,
            SpecieHasActiveChildrenException.class,
            BreedHasActiveChildrenException.class,
            OwnerHasActiveChildrenException.class,
            AnimalHasActiveChildrenException.class,
            ConsultationHasActiveChildrenException.class,
            PrescriptionHasActiveChildrenException.class,
            CountryHasActiveChildrenException.class,
            StateHasActiveChildrenException.class,
            CityHasActiveChildrenException.class,
            ModuleHasActiveChildrenException.class,
            SubModuleHasActiveChildrenException.class,
            MembershipHasActiveChildrenException.class,
            BasePermissionHasActiveChildrenException.class,
            BaseRoleHasActiveChildrenException.class,
            RoleHasActiveChildrenException.class,
            PermissionHasActiveChildrenException.class,
            SystemUserHasActiveChildrenException.class,
            SystemPermissionHasActiveChildrenException.class,
            CompanyHasActiveChildrenException.class,
            EmployeeHasActiveChildrenException.class,
            TaxHasActiveChildrenException.class,
            ProductCategoryHasActiveChildrenException.class,
            ServiceCategoryHasActiveChildrenException.class
    })
    public ProblemDetail handleHasActiveChildren(RuntimeException ex) {
        log.warn("Cannot delete entity with active children: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "ENTITY_HAS_ACTIVE_CHILDREN", ex.getMessage());
    }

    @ExceptionHandler(AdminEmployeeCannotBeDisabledException.class)
    public ProblemDetail handleAdminEmployeeCannotBeDisabled(AdminEmployeeCannotBeDisabledException ex) {
        log.warn("Cannot disable admin employee: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "ADMIN_EMPLOYEE_CANNOT_BE_DISABLED", ex.getMessage());
    }

    @ExceptionHandler(InvalidAppointmentTransitionException.class)
    public ProblemDetail handleInvalidAppointmentTransition(InvalidAppointmentTransitionException ex) {
        log.warn("Invalid appointment status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_APPOINTMENT_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(CompanyTaxProfileAlreadyExistsException.class)
    public ProblemDetail handleCompanyTaxProfileAlreadyExists(CompanyTaxProfileAlreadyExistsException ex) {
        log.warn("Company tax profile already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "COMPANY_TAX_PROFILE_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ProductCodeAlreadyExistsException.class)
    public ProblemDetail handleProductCodeAlreadyExists(ProductCodeAlreadyExistsException ex) {
        log.warn("Product code already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "PRODUCT_CODE_ALREADY_EXISTS", ex.getMessage());
    }

    // Unicidad de NOMBRE por empresa (migraciones 151-154). errorCode(ex) deriva el código correcto por
    // clase: PRODUCT_NAME_ALREADY_EXISTS / PRODUCT_CATEGORY_NAME_ALREADY_EXISTS / etc.
    @ExceptionHandler({
        ProductNameAlreadyExistsException.class,
        ProductCategoryNameAlreadyExistsException.class,
        ServiceCategoryNameAlreadyExistsException.class,
        TaxNameAlreadyExistsException.class
    })
    public ProblemDetail handleNameAlreadyExists(RuntimeException ex) {
        log.warn("Name already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, errorCode(ex), ex.getMessage());
    }

    @ExceptionHandler(OwnerAlreadyHasOpenAccountException.class)
    public ProblemDetail handleOwnerAlreadyHasOpenAccount(OwnerAlreadyHasOpenAccountException ex) {
        log.warn("Owner already has an open account: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "OWNER_ALREADY_HAS_OPEN_ACCOUNT", ex.getMessage());
    }

    @ExceptionHandler(NumberingResolutionAlreadyActiveException.class)
    public ProblemDetail handleNumberingResolutionAlreadyActive(NumberingResolutionAlreadyActiveException ex) {
        log.warn("Numbering resolution already active: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "NUMBERING_RESOLUTION_ALREADY_ACTIVE", ex.getMessage());
    }

    @ExceptionHandler(InvalidOpenAccountStatusTransitionException.class)
    public ProblemDetail handleInvalidOpenAccountStatusTransition(InvalidOpenAccountStatusTransitionException ex) {
        log.warn("Invalid open account status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_OPEN_ACCOUNT_STATUS_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(DebtOpenAccountAlreadyVoidedException.class)
    public ProblemDetail handleDebtOpenAccountAlreadyVoided(DebtOpenAccountAlreadyVoidedException ex) {
        log.warn("Debt open account payment already voided: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DEBT_OPEN_ACCOUNT_ALREADY_VOIDED", ex.getMessage());
    }

    @ExceptionHandler({
        ProductChargeOpenAccountAlreadyVoidedException.class,
        ServiceChargeOpenAccountAlreadyVoidedException.class,
        GeneralChargeOpenAccountAlreadyVoidedException.class
    })
    public ProblemDetail handleChargeOpenAccountAlreadyVoided(RuntimeException ex) {
        log.warn("Charge open account already voided: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CHARGE_OPEN_ACCOUNT_ALREADY_VOIDED", ex.getMessage());
    }

    // F5: correccion por nota credito/debito sobre un documento en estado invalido (no VALIDADO o ya reversado).
    @ExceptionHandler(DocumentNotValidatedException.class)
    public ProblemDetail handleDocumentNotValidated(DocumentNotValidatedException ex) {
        log.warn("Document not validated for correction: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DOCUMENT_NOT_VALIDATED", ex.getMessage());
    }

    @ExceptionHandler(DocumentAlreadyReversedException.class)
    public ProblemDetail handleDocumentAlreadyReversed(DocumentAlreadyReversedException ex) {
        log.warn("Document already reversed: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_REVERSED", ex.getMessage());
    }

    // Cubre el guard de inmutabilidad de cargos/abonos sobre cuentas no-OPEN (IllegalStateException).
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflictState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage());
    }

    // Concurrencia: dos transacciones tocaron la misma entidad versionada (optimistic lock).
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
            "El registro fue modificado por otra operación. Reintenta.");
    }

    // Detección temprana del mismo conflicto: la versión que envió el front (expectedVersion) ya no es
    // la actual de la cuenta. Mismo código que el optimistic lock para que el front lo trate igual.
    @ExceptionHandler(OpenAccountVersionConflictException.class)
    public ProblemDetail handleOpenAccountVersionConflict(OpenAccountVersionConflictException ex) {
        log.warn("Open account version conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
            "La cuenta fue modificada por otra operación. Reintenta.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleUnauthorized(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Unauthorized: {}", ex.getMessage());
        auditLogger.loginFailure(request.getRequestURI(), "invalid_credentials");
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    // Auto-registro Opción B: login rechazado por correo sin verificar. 403 con código propio para
    // que el front distinga de credenciales inválidas y ofrezca reenviar la verificación.
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ProblemDetail handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        log.warn("Login blocked, email not verified");
        auditLogger.loginFailure(request.getRequestURI(), "email_not_verified");
        return problem(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
            "Debes verificar tu correo antes de iniciar sesión.");
    }

    // Captcha del registro no superado (o mal configurado).
    @ExceptionHandler(CaptchaVerificationException.class)
    public ProblemDetail handleCaptchaFailed(CaptchaVerificationException ex) {
        log.warn("Captcha verification failed: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED",
            "No pudimos verificar el captcha. Inténtalo de nuevo.");
    }

    // Token de verificación de correo inválido, expirado o ya usado.
    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ProblemDetail handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        log.warn("Invalid email verification token: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN",
            "El enlace de verificación no es válido o expiró.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        auditLogger.accessDenied(request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        auditLogger.loginFailure(request.getRequestURI(), "authentication_failed");
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
                .toList();
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
        pd.setProperty("errors", errors);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    // Body ilegible / no deserializable (JSON malformado, enum inválido, campo
    // requerido ausente que rompe el binding). El logueo lo hace handleExceptionInternal.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Invalid request content.");
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // 409 = conflicto atribuible al cliente (p.ej. valor duplicado) → WARN, no ERROR.
        log.warn("Data integrity violation: {}", ex.getMessage());
        String cause = ex.getMostSpecificCause().getMessage();
        // Carrera en "1 cuenta abierta por propietario": la constraint única (migración 106) atrapa
        // la 2ª inserción concurrente que pasó el check del service. Se mapea al mismo código que
        // el guard de negocio para que el front lo trate igual.
        if (cause != null && cause.contains("uq_open_accounts_active_owner")) {
            return problem(HttpStatus.CONFLICT, "OWNER_ALREADY_HAS_OPEN_ACCOUNT",
                "El propietario ya tiene una cuenta abierta.");
        }
        // Carrera en la unicidad de SKU por empresa (constraint de la migración 133): la 2ª inserción
        // concurrente que pasó el check del service la atrapa la BD. Se mapea al mismo código de negocio.
        if (cause != null && cause.contains("uq_products_company_active_code")) {
            return problem(HttpStatus.CONFLICT, "PRODUCT_CODE_ALREADY_EXISTS",
                "Ya existe un producto activo con ese código en esta empresa.");
        }
        // Carrera en la unicidad de NOMBRE por empresa (constraints de las migraciones 151-154).
        if (cause != null && cause.contains("uq_products_company_active_name")) {
            return problem(HttpStatus.CONFLICT, "PRODUCT_NAME_ALREADY_EXISTS",
                "Ya existe un producto activo con ese nombre en esta empresa.");
        }
        if (cause != null && cause.contains("uq_product_categories_company_active_name")) {
            return problem(HttpStatus.CONFLICT, "PRODUCT_CATEGORY_NAME_ALREADY_EXISTS",
                "Ya existe una categoría de producto activa con ese nombre en esta empresa.");
        }
        if (cause != null && cause.contains("uq_service_categories_company_active_name")) {
            return problem(HttpStatus.CONFLICT, "SERVICE_CATEGORY_NAME_ALREADY_EXISTS",
                "Ya existe una categoría de servicio activa con ese nombre en esta empresa.");
        }
        if (cause != null && cause.contains("uq_taxes_company_active_name")) {
            return problem(HttpStatus.CONFLICT, "TAX_NAME_ALREADY_EXISTS",
                "Ya existe un impuesto activo con ese nombre en esta empresa.");
        }
        // Carrera en "un documento por cuenta cerrada" (constraint de la migración 134): dos cierres
        // concurrentes que pasaron el check `existsByOpenAccountId`; la BD impide la 2ª emisión fiscal.
        if (cause != null && cause.contains("uq_electronic_documents_open_account")) {
            return problem(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_EMITTED",
                "La venta ya tiene un documento electrónico emitido.");
        }
        // Carrera en la idempotencia de abonos (constraint de la migración 135): doble-submit concurrente con
        // la misma clave; la BD rechaza el 2º. El cliente reintenta y el check de idempotencia devuelve el abono.
        if (cause != null && cause.contains("uq_debt_open_accounts_request")) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT_REQUEST",
                "El abono ya fue registrado (solicitud duplicada).");
        }
        // Carrera en la idempotencia de cargos (constraints de las migraciones 139/140/141): doble-submit
        // concurrente con la misma clave; la BD rechaza el 2º. El cliente reintenta y el check de idempotencia
        // devuelve el cargo ya creado.
        if (cause != null && (cause.contains("uq_product_charge_open_accounts_request")
                || cause.contains("uq_service_charge_open_accounts_request")
                || cause.contains("uq_general_charge_open_accounts_request"))) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_CHARGE_REQUEST",
                "El cargo ya fue registrado (solicitud duplicada).");
        }
        // Carrera/reactivación en la unicidad de "una sola resolución activa por (company, tipo)" (migración
        // 144). La 2ª inserción/reactivación concurrente que pasó el check del service la atrapa la BD.
        if (cause != null && cause.contains("uq_numbering_resolutions_active")) {
            return problem(HttpStatus.CONFLICT, "NUMBERING_RESOLUTION_ALREADY_ACTIVE",
                "La empresa ya tiene una resolución de numeración activa para ese tipo de documento.");
        }
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Database constraint violation");
    }

    @ExceptionHandler(PdfRenderException.class)
    public ProblemDetail handlePdfRender(PdfRenderException ex) {
        log.error("PDF render failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "PDF_RENDER_FAILED",
                "Failed to generate PDF document");
    }

    @ExceptionHandler(S3StorageException.class)
    public ProblemDetail handleS3Storage(S3StorageException ex) {
        log.error("S3 storage operation failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "FILE_STORAGE_FAILED",
                "Failed to access file storage");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        String traceId = MDC.get("traceId");
        if (traceId != null) pd.setProperty("traceId", traceId);
        return pd;
    }

    private static String errorCode(RuntimeException ex) {
        String name = ex.getClass().getSimpleName().replace("Exception", "");
        return camelToSnakeUpper(name);
    }

    private static String camelToSnakeUpper(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) out.append('_');
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }
}
