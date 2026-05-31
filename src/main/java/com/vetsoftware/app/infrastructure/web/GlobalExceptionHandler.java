package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorHasActiveChildrenException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
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
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import com.vetsoftware.app.membership.domain.MembershipHasActiveChildrenException;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import com.vetsoftware.app.module.domain.ModuleHasActiveChildrenException;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import com.vetsoftware.app.owner.domain.OwnerHasActiveChildrenException;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import com.vetsoftware.app.permission.domain.PermissionHasActiveChildrenException;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.prescription.domain.PrescriptionHasActiveChildrenException;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
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
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogger auditLogger;

    public GlobalExceptionHandler(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
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
            AnimalColorNotFoundException.class,
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
            MedicamentPrescriptionNotFoundException.class,
            SurgeryTypeNotFoundException.class, SurgeryNotFoundException.class,
            DiagnosticImagingTypeNotFoundException.class, DiagnosticImagingNotFoundException.class
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
            EmployeeHasActiveChildrenException.class
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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleUnauthorized(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Unauthorized: {}", ex.getMessage());
        auditLogger.loginFailure(request.getRequestURI(), "invalid_credentials");
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
                .toList();
        log.warn("Validation failed: {}", errors);
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // 409 = conflicto atribuible al cliente (p.ej. valor duplicado) → WARN, no ERROR.
        log.warn("Data integrity violation: {}", ex.getMessage());
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
