package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.WeightRecordNotFoundException;
import com.vetsoftware.app.animalalert.domain.AnimalAlertNotFoundException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorHasActiveChildrenException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
import com.vetsoftware.app.appointment.domain.InvalidAppointmentTransitionException;
import com.vetsoftware.app.auth.application.exception.EmailNotVerifiedException;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.infrastructure.security.BranchAccessDeniedException;
import com.vetsoftware.app.basepermission.domain.BasePermissionHasActiveChildrenException;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import com.vetsoftware.app.baserole.domain.BaseRoleHasActiveChildrenException;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import com.vetsoftware.app.breed.domain.BreedHasActiveChildrenException;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import com.vetsoftware.app.cashregister.domain.CashSessionAlreadyOpenException;
import com.vetsoftware.app.cashregister.domain.CashSessionClosedException;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionAlreadyOpenException;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionRequiredException;
import com.vetsoftware.app.cashregister.domain.NoOpenCashSessionException;
import com.vetsoftware.app.catalogitem.domain.BundleComponentAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.BundleComponentNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemCodeAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyCycleException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemHasActiveChildrenException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModuleAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModuleNotFoundException;
import com.vetsoftware.app.catalogitem.domain.InvalidBundleCompositionException;
import com.vetsoftware.app.city.domain.CityHasActiveChildrenException;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import com.vetsoftware.app.company.domain.CompanyHasActiveChildrenException;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.configurator.domain.ConditionalQuestionCycleException;
import com.vetsoftware.app.configurator.domain.ConfiguratorCodeAlreadyExistsException;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectAlreadyExistsException;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionHasActiveChildrenException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import com.vetsoftware.app.configurator.domain.MissingRequiredAnswerException;
import com.vetsoftware.app.configurator.domain.NumberQuestionCannotHaveOptionsException;
import com.vetsoftware.app.configurator.domain.QuantityFromAnswerRequiresNumberQuestionException;
import com.vetsoftware.app.configurator.domain.UnreachableAnswerException;
import com.vetsoftware.app.consultation.domain.ConsultationHasActiveChildrenException;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeHasActiveChildrenException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import com.vetsoftware.app.country.domain.CountryHasActiveChildrenException;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeHasActiveChildrenException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfigNotFoundException;
import com.vetsoftware.app.dunning.domain.DunningEventNotFoundException;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import com.vetsoftware.app.electronicdocument.domain.DocumentAlreadyReversedException;
import com.vetsoftware.app.electronicdocument.domain.DocumentNotValidatedException;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import com.vetsoftware.app.electronicdocument.domain.NumberingResolutionNotEffectiveException;
import com.vetsoftware.app.electronicdocument.domain.NumberingResolutionRangeExhaustedException;
import com.vetsoftware.app.employee.domain.AdminEmployeeCannotBeDisabledException;
import com.vetsoftware.app.employee.domain.EmployeeHasActiveChildrenException;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityNotFoundException;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlementNotFoundException;
import com.vetsoftware.app.entitlement.domain.CompanyWithoutContractException;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.logging.LogRedactor;
import com.vetsoftware.app.infrastructure.pdf.PdfRenderException;
import com.vetsoftware.app.infrastructure.storage.S3StorageException;
import com.vetsoftware.app.inventory.domain.InsufficientStockException;
import com.vetsoftware.app.inventory.domain.InventoryCountNotFoundException;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeHasActiveChildrenException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import com.vetsoftware.app.medicament.domain.MedicamentHasActiveChildrenException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import com.vetsoftware.app.module.domain.ModuleHasActiveChildrenException;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import com.vetsoftware.app.openaccount.domain.InvalidOpenAccountStatusTransitionException;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import com.vetsoftware.app.openaccount.domain.OwnerAlreadyHasOpenAccountException;
import com.vetsoftware.app.owner.domain.OwnerHasActiveChildrenException;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import com.vetsoftware.app.passwordreset.domain.InvalidPasswordResetTokenException;
import com.vetsoftware.app.permission.domain.PermissionHasActiveChildrenException;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogConflictException;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogNotFoundException;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfigNotConfiguredException;
import com.vetsoftware.app.prescription.domain.PrescriptionHasActiveChildrenException;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import com.vetsoftware.app.pricelist.domain.CatalogPriceNotFoundException;
import com.vetsoftware.app.pricelist.domain.CatalogPriceTierGapException;
import com.vetsoftware.app.pricelist.domain.CatalogPriceTierOverlapException;
import com.vetsoftware.app.pricelist.domain.InvalidPriceListTransitionException;
import com.vetsoftware.app.pricelist.domain.PriceListCodeAlreadyExistsException;
import com.vetsoftware.app.pricelist.domain.PriceListHasActivePricesException;
import com.vetsoftware.app.pricelist.domain.PriceListNotEditableException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.problem.domain.ProblemNotFoundException;
import com.vetsoftware.app.product.domain.ProductCodeAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNameAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryHasActiveChildrenException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNameAlreadyExistsException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import com.vetsoftware.app.purchaseorder.domain.InvalidPurchaseOrderStatusTransitionException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import com.vetsoftware.app.quote.domain.InvalidQuoteStatusTransitionException;
import com.vetsoftware.app.quote.domain.QuoteExpiredException;
import com.vetsoftware.app.quote.domain.QuoteLineArithmeticException;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import com.vetsoftware.app.quote.domain.QuoteTotalsMismatchException;
import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;
import com.vetsoftware.app.registration.domain.EmployeeCodeAlreadyExistsException;
import com.vetsoftware.app.registration.domain.InvalidVerificationTokenException;
import com.vetsoftware.app.registration.domain.OwnerWithoutBranchException;
import com.vetsoftware.app.registration.domain.PlatformCatalogNotConfiguredException;
import com.vetsoftware.app.registration.domain.PlatformRoleCatalogNotConfiguredException;
import com.vetsoftware.app.registration.infrastructure.security.CaptchaConfigurationException;
import com.vetsoftware.app.registration.infrastructure.security.CaptchaProviderUnavailableException;
import com.vetsoftware.app.role.domain.RoleHasActiveChildrenException;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryHasActiveChildrenException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNameAlreadyExistsException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.spatype.domain.SpaTypeHasActiveChildrenException;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import com.vetsoftware.app.specie.domain.SpecieHasActiveChildrenException;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import com.vetsoftware.app.state.domain.StateHasActiveChildrenException;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import com.vetsoftware.app.submodule.domain.SubModuleHasActiveChildrenException;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import com.vetsoftware.app.subscription.domain.CompanyAlreadyHasActiveSubscriptionException;
import com.vetsoftware.app.subscription.domain.InvalidSubscriptionStatusTransitionException;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemAlreadyEndedException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapException;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentAlreadyIssuedException;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentAlreadyVoidedException;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequenceAlreadyExistsException;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequenceNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.DuplicateBillingCycleException;
import com.vetsoftware.app.subscriptionbilling.domain.EmptyBillingDocumentException;
import com.vetsoftware.app.subscriptionbilling.domain.MixedSignChargesException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionChargeAlreadyInvoicedException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionChargeNotFoundException;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplicationNotFoundException;
import com.vetsoftware.app.subscriptionpayment.domain.InvalidSubscriptionPaymentStatusTransitionException;
import com.vetsoftware.app.subscriptionpayment.domain.OverAppliedSourceException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotConfirmedException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentHasActiveApplicationsException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import com.vetsoftware.app.supplier.domain.SupplierNameAlreadyExistsException;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.InvalidSupplierInvoiceStateException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNumberAlreadyExistsException;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeHasActiveChildrenException;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import com.vetsoftware.app.systempermission.domain.SystemPermissionHasActiveChildrenException;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import com.vetsoftware.app.systemuser.domain.SystemUserHasActiveChildrenException;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.tax.domain.TaxHasActiveChildrenException;
import com.vetsoftware.app.tax.domain.TaxNameAlreadyExistsException;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeHasActiveChildrenException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfigNotFoundException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

// @Order(HIGHEST_PRECEDENCE): gana sobre el ProblemDetailsExceptionHandler interno de
// Spring Boot (registrado con @Order(0) por spring.mvc.problemdetails.enabled=true), que
// de otro modo resolvía las excepciones estándar de MVC en silencio y eclipsaba estos
// handlers. Al extender ResponseEntityExceptionHandler, este advice pasa a manejar (y
// loguear) validación de body, JSON ilegible, 405, 415, parámetros faltantes, etc.
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Nombre de la constraint en el mensaje del driver: MySQL y PostgreSQL. */
    private static final Pattern CONSTRAINT_NAME = Pattern
            .compile("(?i)(?:for key|constraint)\\s*[\"'\\[]?([A-Za-z0-9_$.]{1,128})");

    /**
     * Detalle de los 400 de validación. Es el mismo texto en los cuatro caminos que
     * producen errores por campo —cuerpo validado, cuerpo no deserializable,
     * parámetros validados y parámetros no convertibles—, porque el front pinta el
     * detalle como cabecera del formulario y no debe cambiar según por dónde falló.
     */
    private static final String VALIDATION_DETAIL = "La información enviada no es válida. Revisa los campos marcados.";

    /**
     * Tope de valores admitidos que se enumeran en el mensaje de un enum. Por
     * encima, el mensaje deja de ser una ayuda y pasa a ser un volcado: se cae al
     * texto genérico.
     */
    private static final int MAX_ENUM_VALUES_IN_MESSAGE = 12;

    private final AuditLogger auditLogger;
    private final Tracer tracer;

    public GlobalExceptionHandler(AuditLogger auditLogger, Tracer tracer) {
        this.auditLogger = auditLogger;
        this.tracer = tracer;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()
                && request instanceof ServletWebRequest servletWebRequest) {
            markObservationError(servletWebRequest.getRequest(), ex);
        }
        if (statusCode.is5xxServerError()) {
            log.error("Server error {} on {}", statusCode.value(), request.getDescription(false),
                    ex);
        } else if (statusCode.is4xxClientError()) {
            // INFO y no WARN: un 4xx es funcionamiento normal de una API —contraseña
            // equivocada, recurso inexistente, cuerpo mal formado— y la culpa es del
            // cliente, no del servidor. En WARN copaban el nivel (15 warn contra 6 info en
            // Loki, casi todos de aquí) y enterraban lo que sí exige mirar.
            //
            // No se pierde señal de seguridad: los eventos que la llevan —access_denied,
            // unauthenticated, login_failure— los emite AuditLogger en WARN y con contexto
            // (actor, ip, motivo). Esta línea solo los duplicaba sin ese contexto.
            //
            // El detalle sale por clientErrorDetail y no de ex.getMessage(): el de una
            // excepción de binding lleva los valores rechazados —el nombre, el documento o
            // el correo que el cliente tecleó— y la redacción por patrones no cubre nombres
            // propios (ASVS V7.1.1).
            log.info("Client error {} on {}: {}", statusCode.value(), request.getDescription(false),
                    clientErrorDetail(ex));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // ---------------------------------------------------------------------------------------------
    // Severidad de los 4xx de dominio: INFO. (#89)
    //
    // Es el mismo criterio ya escrito en handleExceptionInternal para los 4xx de
    // Spring —arriba, con su justificación— aplicado ahora también a los de
    // negocio,
    // que son la otra mitad del mismo hecho. Un 404 porque el id no existe o un 409
    // porque la caja ya está abierta no son un fallo del sistema: son el sistema
    // funcionando y rechazando lo que debe rechazar. La cuenta era de 36 WARN
    // contra
    // 1 INFO en este archivo, así que el nivel WARN describía «lo normal» y el
    // operador no tenía forma de distinguirlo de lo que sí exige mirar.
    //
    // Lo que NO baja, y por qué:
    // - los cuatro log.error (5xx: PDF, S3, inesperado, y el 5xx de
    // handleExceptionInternal) son fallos del servidor y siguen en ERROR;
    // - el WARN de «Unmapped data integrity violation» se queda en WARN a
    // propósito:
    // ahí la respuesta al cliente es genérica y ese evento es el único rastro para
    // poder mapear la constraint nueva, así que sí pide acción humana;
    // - los eventos de seguridad no pierden severidad: access_denied,
    // unauthenticated, login_failure y login_blocked_email_not_verified los emite
    // AuditLogger por el canal AUDIT con actor, ip y motivo. Las líneas de aquí
    // solo los duplicaban sin ese contexto.
    // ---------------------------------------------------------------------------------------------

    @ExceptionHandler({CompanyNotFoundException.class, EmployeeNotFoundException.class,
            ModuleNotFoundException.class, PermissionNotFoundException.class,
            SubModuleNotFoundException.class, BasePermissionNotFoundException.class,
            BaseRoleNotFoundException.class, BaseRolePermissionNotFoundException.class,
            CountryNotFoundException.class, StateNotFoundException.class,
            CityNotFoundException.class, RoleNotFoundException.class,
            RolePermissionNotFoundException.class, EmployeeRoleNotFoundException.class,
            SystemUserNotFoundException.class, SystemPermissionNotFoundException.class,
            SystemUserPermissionNotFoundException.class, SpecieNotFoundException.class,
            BreedNotFoundException.class, OwnerNotFoundException.class,
            AnimalNotFoundException.class, WeightRecordNotFoundException.class,
            AnimalColorNotFoundException.class, ProblemNotFoundException.class,
            AnimalAlertNotFoundException.class, AppointmentNotFoundException.class,
            ConsultationTypeNotFoundException.class, ConsultationNotFoundException.class,
            VaccinationTypeNotFoundException.class, VaccinationNotFoundException.class,
            HospitalizationNotFoundException.class,
            HospitalizationObservationNotFoundException.class,
            HospitalizationProgressNoteNotFoundException.class,
            HospitalizationMedicationNotFoundException.class,
            HospitalizationProcedureNotFoundException.class,
            LaboratoryTestTypeNotFoundException.class, LaboratoryTestNotFoundException.class,
            LaboratoryTestFileNotFoundException.class, PrescriptionNotFoundException.class,
            DewormingNotFoundException.class, DayCareNotFoundException.class,
            SpaTypeNotFoundException.class, SpaNotFoundException.class,
            MedicamentPrescriptionNotFoundException.class, MedicamentNotFoundException.class,
            SurgeryTypeNotFoundException.class, SurgeryNotFoundException.class,
            DiagnosticImagingTypeNotFoundException.class, DiagnosticImagingNotFoundException.class,
            TaxNotFoundException.class, ProductCategoryNotFoundException.class,
            ServiceCategoryNotFoundException.class, ProductNotFoundException.class,
            ServiceNotFoundException.class, PromotionNotFoundException.class,
            OpenAccountNotFoundException.class, DebtOpenAccountNotFoundException.class,
            ProductChargeOpenAccountNotFoundException.class,
            ServiceChargeOpenAccountNotFoundException.class,
            GeneralChargeOpenAccountNotFoundException.class,
            EconomicActivityNotFoundException.class, CompanyTaxProfileNotFoundException.class,
            NumberingResolutionNotFoundException.class, ElectronicDocumentNotFoundException.class,
            DianProviderConfigNotFoundException.class, WithholdingConfigNotFoundException.class,
            BranchNotFoundException.class, InventoryCountNotFoundException.class,
            CashSessionNotFoundException.class, SupplierNotFoundException.class,
            PurchaseOrderNotFoundException.class, GoodsReceiptNotFoundException.class,
            SupplierInvoiceNotFoundException.class, PetshopCatalogNotFoundException.class,
            ConfiguratorQuestionNotFoundException.class, ConfiguratorOptionNotFoundException.class,
            ConfiguratorEffectNotFoundException.class, PriceListNotFoundException.class,
            CatalogPriceNotFoundException.class, QuoteNotFoundException.class,
            CatalogItemNotFoundException.class, CatalogItemSubModuleNotFoundException.class,
            CatalogItemDependencyNotFoundException.class, BundleComponentNotFoundException.class,
            CompanyEntitlementNotFoundException.class, CompanyCapacityNotFoundException.class,
            SubscriptionNotFoundException.class, SubscriptionItemNotFoundException.class,
            SubscriptionPaymentNotFoundException.class,
            BillingDocumentApplicationNotFoundException.class, DunningEventNotFoundException.class,
            SubscriptionChargeNotFoundException.class,
            SubscriptionBillingDocumentNotFoundException.class,
            BillingDocumentSequenceNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        log.info("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, errorCode(ex), ex.getMessage());
    }

    @ExceptionHandler({ConsultationTypeHasActiveChildrenException.class,
            VaccinationTypeHasActiveChildrenException.class,
            SurgeryTypeHasActiveChildrenException.class,
            LaboratoryTestTypeHasActiveChildrenException.class,
            MedicamentHasActiveChildrenException.class,
            DiagnosticImagingTypeHasActiveChildrenException.class,
            SpaTypeHasActiveChildrenException.class, AnimalColorHasActiveChildrenException.class,
            SpecieHasActiveChildrenException.class, BreedHasActiveChildrenException.class,
            OwnerHasActiveChildrenException.class, AnimalHasActiveChildrenException.class,
            ConsultationHasActiveChildrenException.class,
            PrescriptionHasActiveChildrenException.class, CountryHasActiveChildrenException.class,
            StateHasActiveChildrenException.class, CityHasActiveChildrenException.class,
            ModuleHasActiveChildrenException.class, SubModuleHasActiveChildrenException.class,
            BasePermissionHasActiveChildrenException.class,
            BaseRoleHasActiveChildrenException.class, RoleHasActiveChildrenException.class,
            PermissionHasActiveChildrenException.class, SystemUserHasActiveChildrenException.class,
            SystemPermissionHasActiveChildrenException.class,
            CompanyHasActiveChildrenException.class, EmployeeHasActiveChildrenException.class,
            TaxHasActiveChildrenException.class, ProductCategoryHasActiveChildrenException.class,
            ServiceCategoryHasActiveChildrenException.class,
            ConfiguratorQuestionHasActiveChildrenException.class,
            CatalogItemHasActiveChildrenException.class})
    public ProblemDetail handleHasActiveChildren(RuntimeException ex) {
        log.info("Cannot delete entity with active children: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "ENTITY_HAS_ACTIVE_CHILDREN", ex.getMessage());
    }

    @ExceptionHandler(AdminEmployeeCannotBeDisabledException.class)
    public ProblemDetail handleAdminEmployeeCannotBeDisabled(
            AdminEmployeeCannotBeDisabledException ex) {
        log.info("Cannot disable admin employee: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "ADMIN_EMPLOYEE_CANNOT_BE_DISABLED", ex.getMessage());
    }

    @ExceptionHandler(InvalidAppointmentTransitionException.class)
    public ProblemDetail handleInvalidAppointmentTransition(
            InvalidAppointmentTransitionException ex) {
        log.info("Invalid appointment status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_APPOINTMENT_TRANSITION", ex.getMessage());
    }

    // BE-17: la cita se cruza con otra del mismo veterinario. El detail va escrito
    // para el usuario final (a quién y a qué hora) porque el front lo pinta tal
    // cual; overlappingAppointmentIds acompaña para que pueda enlazar las citas en
    // conflicto. Se supera reenviando la operación con forceOverlap=true (que
    // exige appointment.overlap.force).
    //
    // Los dos datos que salen ya vienen RECORTADOS al alcance de sede del caller
    // desde el caso de uso: el cruce se calcula por veterinario, sin sede, pero el
    // listado sí está acotado, y devolver el id de una cita de otra sede la hacía
    // legible entera por GET /appointments/{id}.
    //
    // El log NO lleva el mensaje: arrastra el nombre del veterinario y el horario a
    // un canal que no los necesita para diagnosticar. employeeId + número de cruces
    // dicen lo mismo sin PII.
    @ExceptionHandler(AppointmentOverlapException.class)
    public ProblemDetail handleAppointmentOverlap(AppointmentOverlapException ex) {
        log.info("Appointment overlap: employeeId={} overlapCount={}", ex.getEmployeeId(),
                ex.getOverlapCount());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "APPOINTMENT_OVERLAP", ex.getMessage());
        pd.setProperty("overlappingAppointmentIds", ex.getOverlappingAppointmentIds());
        return pd;
    }

    // Compras: transición de estado inválida en orden de compra o recepción (p. ej.
    // editar una PO ya
    // recibida,
    // confirmar una recepción no-borrador, cancelar una recepción no-confirmada).
    // Código propio
    // derivado por clase
    // (INVALID_PURCHASE_ORDER_STATUS_TRANSITION /
    // INVALID_GOODS_RECEIPT_STATUS_TRANSITION) para el
    // front.
    @ExceptionHandler({InvalidPurchaseOrderStatusTransitionException.class,
            InvalidGoodsReceiptStatusTransitionException.class})
    public ProblemDetail handlePurchaseStatusTransition(RuntimeException ex) {
        log.info("Invalid purchase status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, errorCode(ex), ex.getMessage());
    }

    // CxP (F3): operación no válida para el estado de la factura de proveedor
    // (editar/anular con
    // abonos, abonar una
    // anulada/pagada, sobrepago). 409 con código propio
    // INVALID_SUPPLIER_INVOICE_STATE para el front.
    @ExceptionHandler(InvalidSupplierInvoiceStateException.class)
    public ProblemDetail handleInvalidSupplierInvoiceState(
            InvalidSupplierInvoiceStateException ex) {
        log.info("Invalid supplier invoice state: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_SUPPLIER_INVOICE_STATE", ex.getMessage());
    }

    @ExceptionHandler(CompanyTaxProfileAlreadyExistsException.class)
    public ProblemDetail handleCompanyTaxProfileAlreadyExists(
            CompanyTaxProfileAlreadyExistsException ex) {
        log.info("Company tax profile already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "COMPANY_TAX_PROFILE_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ProductCodeAlreadyExistsException.class)
    public ProblemDetail handleProductCodeAlreadyExists(ProductCodeAlreadyExistsException ex) {
        log.info("Product code already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "PRODUCT_CODE_ALREADY_EXISTS", ex.getMessage());
    }

    // Unicidad de NOMBRE por empresa (migraciones 151-154). errorCode(ex) deriva el
    // código correcto
    // por
    // clase: PRODUCT_NAME_ALREADY_EXISTS / PRODUCT_CATEGORY_NAME_ALREADY_EXISTS /
    // etc.
    @ExceptionHandler({ProductNameAlreadyExistsException.class,
            ProductCategoryNameAlreadyExistsException.class,
            ServiceCategoryNameAlreadyExistsException.class, TaxNameAlreadyExistsException.class,
            SupplierNameAlreadyExistsException.class,
            SupplierInvoiceNumberAlreadyExistsException.class})
    public ProblemDetail handleNameAlreadyExists(RuntimeException ex) {
        log.info("Name already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, errorCode(ex), ex.getMessage());
    }

    @ExceptionHandler(OwnerAlreadyHasOpenAccountException.class)
    public ProblemDetail handleOwnerAlreadyHasOpenAccount(OwnerAlreadyHasOpenAccountException ex) {
        log.info("Owner already has an open account: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "OWNER_ALREADY_HAS_OPEN_ACCOUNT", ex.getMessage());
    }

    @ExceptionHandler(NumberingResolutionAlreadyActiveException.class)
    public ProblemDetail handleNumberingResolutionAlreadyActive(
            NumberingResolutionAlreadyActiveException ex) {
        log.info("Numbering resolution already active: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "NUMBERING_RESOLUTION_ALREADY_ACTIVE", ex.getMessage());
    }

    // ---------------------------------------------------------------------------------------------
    // Numeración DIAN agotada o vencida: dos códigos propios, no INVALID_STATE.
    // (#125)
    //
    // Los dos casos los detecta JpaNumberingAllocationPort al pedir el consecutivo,
    // y
    // los dos lanzaban IllegalStateException pelada, así que salían por el handler
    // genérico como INVALID_STATE — el mismo código con el que sale «la cuenta no
    // está
    // abierta» y otra veintena de guardas de estado. Consecuencias medidas: el
    // front no
    // podía decirle al cajero qué hacer (son acciones distintas: ampliar la
    // vigencia o
    // pedir rango nuevo a la DIAN), y el operador no podía contar en Grafana
    // cuántas
    // empresas se estaban quedando sin numeración, porque el hecho no tenía nombre.
    //
    // El detail es constante y los datos van como propiedades del ProblemDetail
    // —mismo
    // patrón que APPOINTMENT_OVERLAP—, así que el mensaje de la excepción no llega
    // nunca al cliente (#118). Lo que se publica es de la propia empresa del
    // caller: el
    // puerto resuelve la resolución por el companyId del contexto, no por uno que
    // venga
    // en el request.
    // ---------------------------------------------------------------------------------------------

    @ExceptionHandler(NumberingResolutionNotEffectiveException.class)
    public ProblemDetail handleNumberingResolutionNotEffective(
            NumberingResolutionNotEffectiveException ex) {
        log.info("Numbering resolution not effective: validFrom={} validTo={}", ex.getValidFrom(),
                ex.getValidTo());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "NUMBERING_RESOLUTION_NOT_EFFECTIVE",
                "La resolución de numeración no está vigente. Revisa sus fechas de vigencia"
                        + " o activa una resolución nueva antes de emitir.");
        setIfPresent(pd, "resolutionNumber", ex.getResolutionNumber());
        setIfPresent(pd, "validFrom", ex.getValidFrom());
        setIfPresent(pd, "validTo", ex.getValidTo());
        return pd;
    }

    @ExceptionHandler(NumberingResolutionRangeExhaustedException.class)
    public ProblemDetail handleNumberingResolutionRangeExhausted(
            NumberingResolutionRangeExhaustedException ex) {
        log.info("Numbering resolution range exhausted: rangeTo={}", ex.getRangeTo());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "NUMBERING_RESOLUTION_RANGE_EXHAUSTED",
                "La resolución de numeración agotó su rango de consecutivos. Solicita un rango"
                        + " nuevo a la DIAN y actívalo antes de emitir.");
        setIfPresent(pd, "resolutionNumber", ex.getResolutionNumber());
        setIfPresent(pd, "rangeTo", ex.getRangeTo());
        return pd;
    }

    @ExceptionHandler(InvalidOpenAccountStatusTransitionException.class)
    public ProblemDetail handleInvalidOpenAccountStatusTransition(
            InvalidOpenAccountStatusTransitionException ex) {
        log.info("Invalid open account status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_OPEN_ACCOUNT_STATUS_TRANSITION",
                ex.getMessage());
    }

    @ExceptionHandler(DebtOpenAccountAlreadyVoidedException.class)
    public ProblemDetail handleDebtOpenAccountAlreadyVoided(
            DebtOpenAccountAlreadyVoidedException ex) {
        log.info("Debt open account payment already voided: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DEBT_OPEN_ACCOUNT_ALREADY_VOIDED", ex.getMessage());
    }

    @ExceptionHandler({ProductChargeOpenAccountAlreadyVoidedException.class,
            ServiceChargeOpenAccountAlreadyVoidedException.class,
            GeneralChargeOpenAccountAlreadyVoidedException.class})
    public ProblemDetail handleChargeOpenAccountAlreadyVoided(RuntimeException ex) {
        log.info("Charge open account already voided: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CHARGE_OPEN_ACCOUNT_ALREADY_VOIDED", ex.getMessage());
    }

    // F5: correccion por nota credito/debito sobre un documento en estado invalido
    // (no VALIDADO o ya
    // reversado).
    @ExceptionHandler(DocumentNotValidatedException.class)
    public ProblemDetail handleDocumentNotValidated(DocumentNotValidatedException ex) {
        log.info("Document not validated for correction: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DOCUMENT_NOT_VALIDATED", ex.getMessage());
    }

    @ExceptionHandler(DocumentAlreadyReversedException.class)
    public ProblemDetail handleDocumentAlreadyReversed(DocumentAlreadyReversedException ex) {
        log.info("Document already reversed: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_REVERSED", ex.getMessage());
    }

    // Inventario: la venta/consumo no alcanza y la empresa no permite stock
    // negativo. 409 con código
    // propio
    // para que el front distinga "sin existencias" de otros conflictos y lo muestre
    // al usuario.
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        log.info("Insufficient stock: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage());
    }

    // Caja: conflictos de estado de la sesión. Códigos propios (derivados por
    // clase) para que el
    // front distinga
    // "ya hay caja abierta" / "la caja está cerrada" / "no hay caja abierta para
    // cobrar".
    @ExceptionHandler({CashSessionAlreadyOpenException.class,
            EmployeeCashSessionAlreadyOpenException.class,
            EmployeeCashSessionRequiredException.class, CashSessionClosedException.class,
            NoOpenCashSessionException.class})
    public ProblemDetail handleCashSessionConflict(RuntimeException ex) {
        log.info("Cash session conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, errorCode(ex), ex.getMessage());
    }

    // ---------------------------------------------------------------------------------------------
    // Modelo de suscripciones: conflictos de negocio (409) y respuestas inválidas
    // del configurador (400).
    //
    // Todas estas reglas son las que la base NO puede imponer —solapes por rango de
    // fechas, ciclos en grafos de dependencias, transiciones de estado, unicidad
    // condicionada— y por eso llegan hasta aquí en vez de salir como
    // DATA_INTEGRITY_VIOLATION. Cada una lleva código propio: quien firma un
    // contrato necesita distinguir "esa empresa ya tiene uno activo" de "el ítem se
    // solapa con otro periodo", y un INVALID_STATE común no le dice ninguna de las
    // dos.
    //
    // INFO en todas: son 4xx atribuibles a quien llama, el mismo criterio del resto
    // del archivo (#89).
    // ---------------------------------------------------------------------------------------------

    // Los cuatro conflictos de pricelist salen con SUS DATOS como propiedades, no
    // solo interpolados en la frase (#407). El argumento es el mismo que ya
    // sostiene
    // handleCatalogItemDependencyCycle: sacar el id del precio en conflicto de un
    // texto como "Tier [1, 10] overlaps catalog price 44 for price list 3" obliga
    // al
    // front a parsearlo, y se rompe el dia que alguien reescriba el mensaje. Son
    // ids
    // del catalogo global de plataforma, no datos de ningun tenant.
    @ExceptionHandler(PriceListNotEditableException.class)
    public ProblemDetail handlePriceListNotEditable(PriceListNotEditableException ex) {
        log.info("Price list is not editable: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "PRICE_LIST_NOT_EDITABLE", ex.getMessage());
        pd.setProperty("priceListId", ex.getPriceListId());
        pd.setProperty("status", ex.getStatus());
        return pd;
    }

    @ExceptionHandler(InvalidPriceListTransitionException.class)
    public ProblemDetail handleInvalidPriceListTransition(InvalidPriceListTransitionException ex) {
        log.info("Invalid price list transition: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "INVALID_PRICE_LIST_TRANSITION",
                ex.getMessage());
        pd.setProperty("from", ex.getFrom());
        pd.setProperty("to", ex.getTo());
        return pd;
    }

    // conflictingPriceId es el que de verdad importa: con el la consola puede
    // ofrecer "ver el tramo que estorba"; sin el, el administrador lo busca a mano
    // entre los tramos de la lista.
    @ExceptionHandler(CatalogPriceTierOverlapException.class)
    public ProblemDetail handleCatalogPriceTierOverlap(CatalogPriceTierOverlapException ex) {
        log.info("Catalog price tier overlap: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "CATALOG_PRICE_TIER_OVERLAP",
                ex.getMessage());
        pd.setProperty("priceListId", ex.getPriceListId());
        pd.setProperty("catalogItemId", ex.getCatalogItemId());
        pd.setProperty("billingCycle", ex.getBillingCycle());
        pd.setProperty("conflictingPriceId", ex.getConflictingPriceId());
        return pd;
    }

    // Sale del @ExceptionHandler agrupado de ENTITY_HAS_ACTIVE_CHILDREN para poder
    // leer sus getters —aquel recibe un RuntimeException pelado— pero CONSERVA ese
    // mismo errorCode: el front ya sabe tratarlo y cambiarlo por uno propio le
    // obligaria a escribir un segundo camino para el mismo suceso. Lo que gana es
    // activePrices, que es lo que separa un "no se puede borrar" sin salida de un
    // "tiene 3 precios activos, ¿los archivo?" (#407).
    @ExceptionHandler(PriceListHasActivePricesException.class)
    public ProblemDetail handlePriceListHasActivePrices(PriceListHasActivePricesException ex) {
        log.info("Cannot delete price list with active prices: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "ENTITY_HAS_ACTIVE_CHILDREN",
                ex.getMessage());
        pd.setProperty("priceListId", ex.getPriceListId());
        pd.setProperty("activePrices", ex.getActivePrices());
        return pd;
    }

    // R9 tiene dos mitades y esta es la segunda: los tramos de un articulo no se
    // pisan (arriba) y no dejan huecos (aqui). 409 y no 400 porque lo que no encaja
    // no es el cuerpo de la peticion de publicar —que va vacio— sino el estado de
    // la
    // tarifa que se pide congelar. El hueco viaja como dato para que la consola
    // pueda senalar el articulo y el rango en vez de decir "hay un hueco" (#378).
    @ExceptionHandler(CatalogPriceTierGapException.class)
    public ProblemDetail handleCatalogPriceTierGap(CatalogPriceTierGapException ex) {
        log.info("Catalog price tier gap: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "CATALOG_PRICE_TIER_GAP", ex.getMessage());
        pd.setProperty("priceListId", ex.getPriceListId());
        pd.setProperty("catalogItemId", ex.getCatalogItemId());
        pd.setProperty("billingCycle", ex.getBillingCycle());
        pd.setProperty("gapFrom", ex.getGapFrom());
        pd.setProperty("gapTo", ex.getGapTo());
        return pd;
    }

    @ExceptionHandler(ConfiguratorCodeAlreadyExistsException.class)
    public ProblemDetail handleConfiguratorCodeAlreadyExists(
            ConfiguratorCodeAlreadyExistsException ex) {
        log.info("Configurator code already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONFIGURATOR_CODE_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ConfiguratorEffectAlreadyExistsException.class)
    public ProblemDetail handleConfiguratorEffectAlreadyExists(
            ConfiguratorEffectAlreadyExistsException ex) {
        log.info("Configurator effect already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONFIGURATOR_EFFECT_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(PriceListCodeAlreadyExistsException.class)
    public ProblemDetail handlePriceListCodeAlreadyExists(PriceListCodeAlreadyExistsException ex) {
        log.info("Price list code already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "PRICE_LIST_CODE_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ConditionalQuestionCycleException.class)
    public ProblemDetail handleConditionalQuestionCycle(ConditionalQuestionCycleException ex) {
        log.info("Configurator question cycle: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONFIGURATOR_QUESTION_CYCLE", ex.getMessage());
    }

    @ExceptionHandler(QuantityFromAnswerRequiresNumberQuestionException.class)
    public ProblemDetail handleQuantityFromAnswerRequiresNumber(
            QuantityFromAnswerRequiresNumberQuestionException ex) {
        log.info("Quantity from answer requires a NUMBER question: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "QUANTITY_FROM_ANSWER_REQUIRES_NUMBER",
                ex.getMessage());
    }

    // 409 y no 400: lo que está en conflicto no es el cuerpo que acaba de llegar
    // —una opción o un answerType perfectamente válidos por sí solos— sino el
    // estado guardado del cuestionario contra el que se aplican. Es el mismo
    // criterio con el que va QUANTITY_FROM_ANSWER_REQUIRES_NUMBER, y el contrario
    // al de CONFIGURATOR_ANSWER_UNREACHABLE, que sí culpa al cuerpo.
    @ExceptionHandler(NumberQuestionCannotHaveOptionsException.class)
    public ProblemDetail handleNumberQuestionCannotHaveOptions(
            NumberQuestionCannotHaveOptionsException ex) {
        log.info("NUMBER question cannot have options: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONFIGURATOR_NUMBER_QUESTION_CANNOT_HAVE_OPTIONS",
                ex.getMessage());
    }

    @ExceptionHandler(CatalogItemCodeAlreadyExistsException.class)
    public ProblemDetail handleCatalogItemCodeAlreadyExists(
            CatalogItemCodeAlreadyExistsException ex) {
        log.info("Catalog item code already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CATALOG_ITEM_CODE_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(CatalogItemSubModuleAlreadyExistsException.class)
    public ProblemDetail handleCatalogItemSubModuleAlreadyExists(
            CatalogItemSubModuleAlreadyExistsException ex) {
        log.info("Catalog item sub-module link already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CATALOG_ITEM_SUB_MODULE_ALREADY_EXISTS",
                ex.getMessage());
    }

    @ExceptionHandler(CatalogItemDependencyAlreadyExistsException.class)
    public ProblemDetail handleCatalogItemDependencyAlreadyExists(
            CatalogItemDependencyAlreadyExistsException ex) {
        log.info("Catalog item dependency already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CATALOG_ITEM_DEPENDENCY_ALREADY_EXISTS",
                ex.getMessage());
    }

    @ExceptionHandler(BundleComponentAlreadyExistsException.class)
    public ProblemDetail handleBundleComponentAlreadyExists(
            BundleComponentAlreadyExistsException ex) {
        log.info("Bundle component already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "BUNDLE_COMPONENT_ALREADY_EXISTS", ex.getMessage());
    }

    // El ciclo sale también como dato estructurado, no solo dentro del mensaje: el
    // front necesita enlazar los artículos del bucle uno a uno, y sacarlos de un
    // texto con formato "12 > 44 > 12" obliga a parsearlo y se rompe en cuanto el
    // mensaje cambie. Son ids del catálogo de plataforma, no datos de ningún
    // tenant.
    @ExceptionHandler(CatalogItemDependencyCycleException.class)
    public ProblemDetail handleCatalogItemDependencyCycle(CatalogItemDependencyCycleException ex) {
        log.info("Catalog item dependency cycle: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "CATALOG_ITEM_DEPENDENCY_CYCLE",
                ex.getMessage());
        pd.setProperty("cycle", ex.getCycle());
        return pd;
    }

    @ExceptionHandler(InvalidBundleCompositionException.class)
    public ProblemDetail handleInvalidBundleComposition(InvalidBundleCompositionException ex) {
        log.info("Invalid bundle composition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_BUNDLE_COMPOSITION", ex.getMessage());
    }

    @ExceptionHandler(CompanyWithoutContractException.class)
    public ProblemDetail handleCompanyWithoutContract(CompanyWithoutContractException ex) {
        log.info("Company has no contract: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "COMPANY_WITHOUT_CONTRACT", ex.getMessage());
    }

    @ExceptionHandler(CompanyAlreadyHasActiveSubscriptionException.class)
    public ProblemDetail handleCompanyAlreadyHasActiveSubscription(
            CompanyAlreadyHasActiveSubscriptionException ex) {
        log.info("Company already has an active subscription: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "COMPANY_ALREADY_HAS_ACTIVE_SUBSCRIPTION",
                ex.getMessage());
    }

    @ExceptionHandler(SubscriptionItemOverlapException.class)
    public ProblemDetail handleSubscriptionItemOverlap(SubscriptionItemOverlapException ex) {
        log.info("Subscription item overlap: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "SUBSCRIPTION_ITEM_OVERLAP", ex.getMessage());
    }

    @ExceptionHandler(SubscriptionItemAlreadyEndedException.class)
    public ProblemDetail handleSubscriptionItemAlreadyEnded(
            SubscriptionItemAlreadyEndedException ex) {
        log.info("Subscription item already ended: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "SUBSCRIPTION_ITEM_ALREADY_ENDED", ex.getMessage());
    }

    @ExceptionHandler(InvalidSubscriptionStatusTransitionException.class)
    public ProblemDetail handleInvalidSubscriptionStatusTransition(
            InvalidSubscriptionStatusTransitionException ex) {
        log.info("Invalid subscription status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_SUBSCRIPTION_STATUS_TRANSITION",
                ex.getMessage());
    }

    // 409 y no 503, al revés que su gemela de registration (abajo, con los 5xx):
    // aquí el catálogo mínimo sí existe y lo que no encaja es el contrato concreto
    // que se pide firmar sobre él. Comparte errorCode con aquella por decisión de
    // producto — ver el comentario de handlePlatformNotConfigured.
    @ExceptionHandler(PlatformCatalogNotConfiguredForSubscriptionException.class)
    public ProblemDetail handlePlatformCatalogNotConfiguredForSubscription(
            PlatformCatalogNotConfiguredForSubscriptionException ex) {
        log.info("Platform catalog not configured for subscription: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "PLATFORM_CATALOG_NOT_CONFIGURED", ex.getMessage());
    }

    // available y requested salen como propiedades porque "no cabe" no es
    // accionable: quien concilia necesita saber cuánto quedaba del pago o de la
    // nota
    // crédito para aplicar la diferencia sin volver a consultarlo.
    @ExceptionHandler(OverAppliedSourceException.class)
    public ProblemDetail handleOverAppliedSource(OverAppliedSourceException ex) {
        log.info("Source over-applied: available={} requested={}", ex.getAvailable(),
                ex.getRequested());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "SOURCE_OVER_APPLIED", ex.getMessage());
        pd.setProperty("available", ex.getAvailable());
        pd.setProperty("requested", ex.getRequested());
        return pd;
    }

    @ExceptionHandler(InvalidSubscriptionPaymentStatusTransitionException.class)
    public ProblemDetail handleInvalidSubscriptionPaymentStatusTransition(
            InvalidSubscriptionPaymentStatusTransitionException ex) {
        log.info("Invalid subscription payment status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATUS_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(SubscriptionPaymentNotConfirmedException.class)
    public ProblemDetail handleSubscriptionPaymentNotConfirmed(
            SubscriptionPaymentNotConfirmedException ex) {
        log.info("Subscription payment is not confirmed: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "PAYMENT_NOT_CONFIRMED", ex.getMessage());
    }

    @ExceptionHandler(SubscriptionPaymentHasActiveApplicationsException.class)
    public ProblemDetail handleSubscriptionPaymentHasActiveApplications(
            SubscriptionPaymentHasActiveApplicationsException ex) {
        log.info("Subscription payment still has active applications: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "PAYMENT_HAS_ACTIVE_APPLICATIONS", ex.getMessage());
    }

    // DOCUMENT_ALREADY_ISSUED, no BILLING_DOCUMENT_ALREADY_ISSUED: el código lo
    // fija
    // la especificación y NO se deriva del nombre de la clase. Quien lo cambie a
    // errorCode(ex) "para unificar" rompe al front en silencio.
    @ExceptionHandler(BillingDocumentAlreadyIssuedException.class)
    public ProblemDetail handleBillingDocumentAlreadyIssued(
            BillingDocumentAlreadyIssuedException ex) {
        log.info("Billing document already issued: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_ISSUED", ex.getMessage());
    }

    @ExceptionHandler(BillingDocumentAlreadyVoidedException.class)
    public ProblemDetail handleBillingDocumentAlreadyVoided(
            BillingDocumentAlreadyVoidedException ex) {
        log.info("Billing document already voided: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_VOIDED", ex.getMessage());
    }

    @ExceptionHandler(SubscriptionChargeAlreadyInvoicedException.class)
    public ProblemDetail handleSubscriptionChargeAlreadyInvoiced(
            SubscriptionChargeAlreadyInvoicedException ex) {
        log.info("Subscription charge already invoiced: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CHARGE_ALREADY_INVOICED", ex.getMessage());
    }

    @ExceptionHandler(MixedSignChargesException.class)
    public ProblemDetail handleMixedSignCharges(MixedSignChargesException ex) {
        log.info("Mixed sign charges in one document: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "MIXED_SIGN_CHARGES", ex.getMessage());
    }

    @ExceptionHandler(DuplicateBillingCycleException.class)
    public ProblemDetail handleDuplicateBillingCycle(DuplicateBillingCycleException ex) {
        log.info("Duplicate billing cycle: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "DUPLICATE_BILLING_CYCLE", ex.getMessage());
    }

    @ExceptionHandler(EmptyBillingDocumentException.class)
    public ProblemDetail handleEmptyBillingDocument(EmptyBillingDocumentException ex) {
        log.info("Empty billing document: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "EMPTY_BILLING_DOCUMENT", ex.getMessage());
    }

    @ExceptionHandler(BillingDocumentSequenceAlreadyExistsException.class)
    public ProblemDetail handleBillingDocumentSequenceAlreadyExists(
            BillingDocumentSequenceAlreadyExistsException ex) {
        log.info("Billing document sequence already exists: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "BILLING_DOCUMENT_SEQUENCE_ALREADY_EXISTS",
                ex.getMessage());
    }

    @ExceptionHandler(InvalidQuoteStatusTransitionException.class)
    public ProblemDetail handleInvalidQuoteStatusTransition(
            InvalidQuoteStatusTransitionException ex) {
        log.info("Invalid quote status transition: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_QUOTE_STATUS_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(QuoteExpiredException.class)
    public ProblemDetail handleQuoteExpired(QuoteExpiredException ex) {
        log.info("Quote expired: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "QUOTE_EXPIRED", ex.getMessage());
    }

    // Configurador: las dos son 400 y no 409 porque el conflicto está en el cuerpo
    // que acaba de enviarse —una respuesta a una pregunta que las condiciones del
    // propio envío dejan inalcanzable, o una obligatoria que falta—, no en el
    // estado
    // de nada guardado. Se arregla corrigiendo el envío, que es la definición de un
    // 400.
    // Las dos llevan la pregunta y la opcion como propiedades ademas de dentro del
    // mensaje (#449). El detail sale en ingles y nombrando ids internos —"Answer
    // refers to option 42, which does not exist…"— y el front lo pinta tal cual
    // dentro de un aviso en español, a un operador de la consola y tambien al
    // PROSPECTO ANONIMO, porque /configurator/resolve es publico por diseño. Nadie
    // puede actuar sobre "la opcion 42". Con questionCode el cliente escribe la
    // frase nombrando la pregunta con las mismas palabras que hay en pantalla, sin
    // volver a pedir nada al servidor. No se traduce el backend: se le dan los
    // datos
    // a quien sí sabe el idioma de su usuario.
    //
    // Las propiedades solo se ponen si existen: un ProblemDetail con
    // "questionCode": null le hace creer al front que la pregunta no tiene codigo,
    // en vez de que este rechazo no señala a ninguna pregunta concreta.
    @ExceptionHandler(UnreachableAnswerException.class)
    public ProblemDetail handleUnreachableAnswer(UnreachableAnswerException ex) {
        log.info("Unreachable configurator answer: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "CONFIGURATOR_ANSWER_UNREACHABLE",
                ex.getMessage());
        setIfPresent(pd, "questionId", ex.getQuestionId());
        setIfPresent(pd, "questionCode", ex.getQuestionCode());
        setIfPresent(pd, "optionId", ex.getOptionId());
        return pd;
    }

    @ExceptionHandler(MissingRequiredAnswerException.class)
    public ProblemDetail handleMissingRequiredAnswer(MissingRequiredAnswerException ex) {
        log.info("Missing required configurator answer: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "CONFIGURATOR_REQUIRED_ANSWER_MISSING",
                ex.getMessage());
        setIfPresent(pd, "questionId", ex.getQuestionId());
        setIfPresent(pd, "questionCode", ex.getQuestionCode());
        return pd;
    }

    // Red de seguridad de los guards de estado que todavía lanzan
    // IllegalStateException
    // pelada (el de inmutabilidad de cargos/abonos sobre cuentas no-OPEN, entre
    // otros).
    //
    // El detail es CONSTANTE y no ex.getMessage() (#118). Este handler es el
    // desagüe de
    // ~70 `new IllegalStateException(` de src/main escritas por decenas de manos y
    // sin
    // ningún contrato sobre qué llevan dentro: el mensaje se redactó para un
    // operador,
    // no para un cliente, y devolverlo tal cual convertía cada uno de esos
    // literales en
    // superficie de API no revisada —nombres de columna, ids de otras entidades,
    // estados
    // internos— (ASVS V7.4.1). El diagnóstico no se pierde: sigue entero en el log,
    // que
    // pasa por RedactingAppender.
    //
    // Que el detail sea constante es además lo que permite que INVALID_STATE
    // signifique
    // algo: cuando un caso concreto necesita decirle al usuario qué hacer, la
    // salida es
    // darle su excepción de dominio y su handler —como se hizo con la numeración
    // DIAN en
    // #125—, no reabrir el paso del mensaje crudo.
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflictState(IllegalStateException ex) {
        log.info("Illegal state: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "INVALID_STATE",
                "La operación no es válida para el estado actual del registro.");
    }

    // Concurrencia: dos transacciones tocaron la misma entidad versionada
    // (optimistic lock).
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.info("Optimistic lock conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "El registro fue modificado por otra operación. Reintenta.");
    }

    @ExceptionHandler(PetshopCatalogConflictException.class)
    public ProblemDetail handlePetshopCatalogConflict(PetshopCatalogConflictException ex) {
        log.info("Petshop catalog conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    // Detección temprana del mismo conflicto: la versión que envió el front
    // (expectedVersion) ya no
    // es
    // la actual de la cuenta. Mismo código que el optimistic lock para que el front
    // lo trate igual.
    @ExceptionHandler(OpenAccountVersionConflictException.class)
    public ProblemDetail handleOpenAccountVersionConflict(OpenAccountVersionConflictException ex) {
        log.info("Open account version conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "La cuenta fue modificada por otra operación. Reintenta.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleUnauthorized(InvalidCredentialsException ex,
            HttpServletRequest request) {
        log.info("Unauthorized: {}", ex.getMessage());
        auditLogger.loginFailure(request.getRequestURI(), "invalid_credentials");
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(SessionReplacedException.class)
    public ProblemDetail handleSessionReplaced(SessionReplacedException ex) {
        log.info("Authentication session replaced");
        return problem(HttpStatus.UNAUTHORIZED, "SESSION_REPLACED", ex.getMessage());
    }

    // Auto-registro Opción B: login rechazado por correo sin verificar. 403 con
    // código propio para
    // que el front distinga de credenciales inválidas y ofrezca reenviar la
    // verificación.
    // El identificador se enmascara AQUÍ, antes de entregarlo (#180). En el
    // auto-registro el código de empleado ES el correo del dueño, y
    // AuditLogger.loginBlockedEmailNotVerified lo pone en dos sitios de la misma
    // línea:
    // en el texto del mensaje y en el campo estructurado actor.identifier. Solo el
    // primero quedaba protegido — RedactingAppender redacta el mensaje formateado
    // por
    // patrones, pero actor.identifier está en la allowlist VERBATIM de
    // LogFieldPolicy y
    // sale tal cual—, así que la línea se leía "id=***@clinica.com" y traía el
    // correo
    // entero en el campo de al lado. Enmascarado y en claro, en el mismo evento,
    // hasta
    // Loki.
    //
    // La redacción se delega en LogRedactor y no se reimplementa aquí para que
    // exista
    // una sola definición de "correo enmascarado"; es idempotente, así que el
    // appender
    // volviendo a pasar por encima no cambia nada. Un código de empleado que no sea
    // un
    // correo no casa con ningún patrón y sigue saliendo entero, que es lo que
    // documenta
    // AuditLogger y lo que necesita quien investiga.
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ProblemDetail handleEmailNotVerified(EmailNotVerifiedException ex) {
        String identifier = LogRedactor.redact(ex.getIdentifier());
        log.info("Login blocked, email not verified: {}", identifier);
        auditLogger.loginBlockedEmailNotVerified(identifier);
        return problem(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                "Debes verificar tu correo antes de iniciar sesión.");
    }

    // ---------------------------------------------------------------------------------------------
    // Captcha del registro: tres poblaciones, tres severidades, UN solo punto de
    // registro cada una. (#99)
    //
    // Antes el fallo se registraba dos veces y en desacuerdo consigo mismo: el
    // adapter
    // hacía log.error y a continuación lanzaba, y este handler volvía a registrarlo
    // en
    // log.warn. Dos líneas del mismo hecho con severidades contradictorias, y la
    // que
    // llevaba el diagnóstico era la del adapter, porque la excepción se construía
    // sin
    // causa: al handler no le llegaba nada que registrar. Ahora el adapter no
    // registra
    // —solo clasifica y lanza con causa— y quien registra es el handler, que es el
    // único
    // que sabe además con qué respuesta terminó el request.
    //
    // La respuesta HTTP es la misma en los tres casos (400 CAPTCHA_FAILED, detalle
    // neutro): quien envía el formulario no debe poder distinguir "fallaste el
    // captcha"
    // de "el servidor lo tiene mal configurado", porque lo segundo es un dato de
    // reconocimiento. Lo que cambia es a quién despierta cada uno.
    // ---------------------------------------------------------------------------------------------

    /**
     * Captcha mal configurado: sin secreto, o el proveedor rechaza la credencial
     * con un 4xx. ERROR y con la causa, porque no falla para un usuario sino para
     * todos — ningún registro se completa mientras dure— y solo lo arregla un
     * operador tocando la configuración del despliegue. Marca la observación como
     * error para que el request salga como fallido en las métricas pese a responder
     * 400.
     */
    @ExceptionHandler(CaptchaConfigurationException.class)
    public ProblemDetail handleCaptchaMisconfigured(CaptchaConfigurationException ex,
            HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("Captcha is misconfigured; every registration is being rejected", ex);
        return problem(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED",
                "No pudimos verificar el captcha. Inténtalo de nuevo.");
    }

    /**
     * El proveedor de captcha no contesta (timeout, corte de red, 5xx suyo). WARN:
     * hay que enterarse si se sostiene, pero no hay nada que arreglar en este
     * despliegue y se resuelve solo. Con la causa, que es lo único que separa un
     * read timeout de un 503.
     */
    @ExceptionHandler(CaptchaProviderUnavailableException.class)
    public ProblemDetail handleCaptchaProviderUnavailable(CaptchaProviderUnavailableException ex) {
        log.warn("Captcha provider unavailable", ex);
        return problem(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED",
                "No pudimos verificar el captcha. Inténtalo de nuevo.");
    }

    /**
     * Captcha no superado por quien envió el formulario: token ausente, caducado,
     * ya usado o score por debajo del mínimo. INFO — es un 4xx atribuible al
     * cliente, el mismo criterio del resto del archivo, y es además la población
     * dominante.
     */
    @ExceptionHandler(CaptchaVerificationException.class)
    public ProblemDetail handleCaptchaFailed(CaptchaVerificationException ex) {
        log.info("Captcha verification failed: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED",
                "No pudimos verificar el captcha. Inténtalo de nuevo.");
    }

    // Token de verificación de correo inválido, expirado o ya usado.
    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ProblemDetail handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        log.info("Invalid email verification token: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN",
                "El enlace de verificación no es válido o expiró.");
    }

    // Token de restablecimiento de contraseña inválido, expirado o ya usado.
    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ProblemDetail handleInvalidPasswordResetToken(InvalidPasswordResetTokenException ex) {
        log.info("Invalid password reset token: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_RESET_TOKEN",
                "El enlace de restablecimiento no es válido o expiró.");
    }

    // El usuario de acceso es el correo (un email = una veterinaria): correo ya
    // registrado.
    @ExceptionHandler(EmployeeCodeAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmployeeCodeAlreadyExistsException ex) {
        log.info("Email already registered: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                "Ese correo ya está registrado. Inicia sesión o usa otro correo.");
    }

    // Más específico que el handler de AccessDeniedException de abajo: Spring elige
    // este para la
    // subclase.
    // Devuelve el motivo concreto (la sede) y un código propio para que el front lo
    // distinga del 403
    // genérico.
    @ExceptionHandler(BranchAccessDeniedException.class)
    public ProblemDetail handleBranchAccessDenied(BranchAccessDeniedException ex,
            HttpServletRequest request) {
        log.info("Branch access denied: {}", ex.getMessage());
        auditLogger.accessDenied(request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.FORBIDDEN, "BRANCH_NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.info("Access denied: {}", ex.getMessage());
        auditLogger.accessDenied(request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(AuthenticationException ex,
            HttpServletRequest request) {
        log.info("Authentication failed: {}", ex.getMessage());
        auditLogger.loginFailure(request.getRequestURI(), "authentication_failed");
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required");
    }

    // Mismo criterio que handleConflictState, y aquí el volumen lo hace más grave:
    // alimentan este handler ~1.500 `new IllegalArgumentException(` de src/main
    // —las
    // invariantes de constructor de dominio que exige el CLAUDE.md, más las FK no
    // resueltas de los servicios—. Ninguna de esas 1.500 cadenas se escribió
    // pensando
    // en un cliente HTTP, y varias interpolan datos internos: el caso testigo es
    // CreateAppointmentService, que lanza "Employee not found: " + employeeId y con
    // eso
    // convertía el endpoint de crear cita en un oráculo para enumerar empleados de
    // otras
    // empresas probando ids (#118).
    //
    // El detail constante es lo correcto también de cara al front: la validación
    // que el
    // usuario puede corregir campo a campo NO llega por aquí, llega por
    // handleMethodArgumentNotValid con VALIDATION_FAILED y su lista de errores por
    // campo. Lo que cae en este handler es una invariante de dominio, que el front
    // no
    // sabe atribuir a ningún campo concreto.
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        log.info("Bad request: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "Los datos enviados no son válidos.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        ProblemDetail pd = validationProblem(fieldErrors(ex.getBindingResult().getFieldErrors()));
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    /**
     * El 400 de validación, con la forma que el front ya sabe consumir: el
     * {@code code} {@code VALIDATION_FAILED}, el {@code traceId} del span vivo y la
     * lista {@code errors} de pares {@code {field, message}}.
     *
     * <p>
     * Existe para que esa forma tenga <b>un solo sitio</b> donde se construye. Bean
     * Validation no es el único camino por el que un dato del cliente se rechaza
     * campo a campo: un valor de enum desconocido muere en el deserializador (#326)
     * y un parámetro con restricciones lo valida Spring MVC por su cuenta (#327),
     * los dos <em>antes</em> de que exista un BindingResult. Cuando cada camino
     * redactaba su propia respuesta, el front recibía tres formas distintas para el
     * mismo problema del usuario y solo sabía marcar el control en una.
     */
    private ProblemDetail validationProblem(List<Map<String, String>> errors) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", VALIDATION_DETAIL);
        pd.setProperty("errors", errors);
        return pd;
    }

    /**
     * Errores de validación agrupados por campo, en el orden en que los produjo el
     * validador y con <b>una sola entrada por campo</b>.
     *
     * <p>
     * Lo de "una sola" no es cosmético. El front indexa esta lista por
     * {@code field} para anclar cada mensaje a su control
     * ({@code getProblemDetailFieldErrors}), así que dos entradas del mismo campo
     * hacen que la segunda pise a la primera. Y el caso es corriente: una
     * contraseña vacía viola a la vez {@code @NotBlank} y el {@code min} de
     * {@code @Size}, y Hibernate Validator no garantiza en qué orden entrega las
     * dos. Sin agrupar, cuál de los dos mensajes ve el usuario dependía del
     * recorrido del validador.
     *
     * <p>
     * El campo se conserva tal cual lo da Spring —el nombre de la propiedad del
     * record, con la ruta completa en los anidados ({@code lines[0].quantity})—: es
     * lo que el formulario del front usa como nombre de control, y no expone
     * nombres de tabla ni de columna.
     */
    private static List<Map<String, String>> fieldErrors(List<FieldError> rawErrors) {
        Map<String, String> byField = new LinkedHashMap<>();
        for (FieldError fieldError : rawErrors) {
            mergeFieldError(byField, fieldError.getField(), fieldError.getDefaultMessage());
        }
        return asFieldErrors(byField);
    }

    /**
     * Acumula un mensaje bajo su campo respetando la invariante de una sola entrada
     * por campo descrita arriba. Un campo en blanco se descarta: una entrada sin
     * {@code field} no ancla a ningún control y el front la perdería igual.
     */
    private static void mergeFieldError(Map<String, String> byField, String field, String message) {
        if (field == null || field.isBlank()) {
            return;
        }
        String text = message == null || message.isBlank()
                ? "El valor enviado no es válido."
                : message;
        byField.merge(field, text,
                (first, second) -> first.contains(second) ? first : first + " " + second);
    }

    /** El mapa campo → mensaje con la forma que viaja en el JSON. */
    private static List<Map<String, String>> asFieldErrors(Map<String, String> byField) {
        return byField.entrySet().stream()
                .map(entry -> Map.of("field", entry.getKey(), "message", entry.getValue()))
                .toList();
    }

    // Body ilegible / no deserializable (JSON malformado, enum inválido, campo
    // requerido ausente que rompe el binding). El logueo lo hace
    // handleExceptionInternal.
    //
    // Un valor de enum desconocido NO pasa por Bean Validation (#326): falla en el
    // deserializador, antes de que el record exista, así que ningún `message =` del
    // DTO lo alcanza y hasta ahora la respuesta salía sin nombrar el campo. El alta
    // de mascota tiene cinco selects de enum —gender, weightType, animalType,
    // reproductiveState y size—, de modo que el usuario recibía un error genérico
    // para los cinco y tenía que adivinar cuál corregir. Cuando Jackson sabe decir
    // en qué propiedad se rompió, la respuesta pasa a tener la MISMA forma que la
    // de
    // Bean Validation; cuando no lo sabe —JSON sintácticamente roto, cuerpo
    // ausente—, no hay control que marcar y se conserva MALFORMED_REQUEST.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors = bodyBindingErrors(ex);
        ProblemDetail pd = errors.isEmpty()
                ? problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Invalid request content.")
                : validationProblem(errors);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    /**
     * Error por campo de un cuerpo que el deserializador no pudo convertir, o lista
     * vacía si no hay ninguna propiedad a la que atribuirlo.
     */
    private static List<Map<String, String>> bodyBindingErrors(HttpMessageNotReadableException ex) {
        if (!(ex.getCause() instanceof MismatchedInputException mismatch)) {
            return List.of();
        }
        Map<String, String> byField = new LinkedHashMap<>();
        mergeFieldError(byField, jsonFieldPath(mismatch.getPath()),
                unconvertibleValueMessage(mismatch.getTargetType()));
        return asFieldErrors(byField);
    }

    /**
     * Ruta de la propiedad tal como la ve el front: el nombre del componente del
     * record ({@code gender}), con la misma sintaxis de anidados que Spring usa en
     * los errores de Bean Validation ({@code lines[0].quantity}). Se leen solo
     * nombres de propiedad e índices — el {@code from()} de cada tramo es la clase
     * Java y no se toca.
     */
    private static String jsonFieldPath(List<JacksonException.Reference> path) {
        StringBuilder field = new StringBuilder();
        for (JacksonException.Reference reference : path) {
            String property = reference.getPropertyName();
            if (property != null && !property.isBlank()) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(property);
            } else if (reference.getIndex() >= 0 && !field.isEmpty()) {
                field.append('[').append(reference.getIndex()).append(']');
            }
        }
        return field.toString();
    }

    /**
     * Mensaje para un valor que no se pudo convertir al tipo del campo. De un enum
     * se enumeran los valores admitidos: son parte del contrato publicado en
     * {@code api/openapi.json} y decirlos es justo lo que permite corregir. De
     * cualquier otro tipo el mensaje es genérico, porque nombrar el tipo Java
     * ({@code java.time.LocalDate}, {@code BigDecimal}) filtra interioridades sin
     * dar nada accionable a cambio.
     */
    private static String unconvertibleValueMessage(Class<?> targetType) {
        List<String> allowed = enumValues(targetType);
        if (allowed.isEmpty() || allowed.size() > MAX_ENUM_VALUES_IN_MESSAGE) {
            return "El valor enviado no es válido para este campo.";
        }
        return "El valor enviado no es válido. Valores admitidos: " + String.join(", ", allowed)
                + ".";
    }

    /**
     * Nombres de las constantes de {@code type} si es un enum, lista vacía si no lo
     * es. Nunca devuelve el nombre de la clase. Contempla la constante con cuerpo
     * propio, que es una subclase anónima y responde {@code false} a
     * {@code isEnum()}.
     */
    private static List<String> enumValues(Class<?> type) {
        if (type == null) {
            return List.of();
        }
        Class<?> enumType = type.isEnum() ? type : type.getSuperclass();
        if (enumType == null || !enumType.isEnum()) {
            return List.of();
        }
        return Arrays.stream(enumType.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name()).toList();
    }

    /**
     * Validación de los argumentos del método del controller: un
     * {@code @RequestParam} o un {@code @PathVariable} con restricciones
     * ({@code @Min}, {@code @Size}) los valida Spring MVC por su cuenta, sin
     * BindingResult y sin FieldError, así que {@code handleMethodArgumentNotValid}
     * no los ve (#327). Sin este override la respuesta salía 400 pero sin
     * {@code code}, sin {@code traceId} y sin nombrar el parámetro: el front no
     * tenía ni con qué clasificarla ni qué marcar.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        return handleExceptionInternal(ex, validationProblem(parameterErrors(ex)), headers, status,
                request);
    }

    /**
     * La misma validación de parámetros cuando el proxy la <b>adapta</b>: con
     * {@code spring.validation.method.adapt-constraint-violations=true}, un bean
     * {@code @Validated} deja de lanzar el {@code ConstraintViolationException} de
     * Jakarta y lanza el {@code MethodValidationException} de Spring (#330).
     *
     * <p>
     * Es el mismo fallo del cliente con otro envoltorio, así que da la misma
     * respuesta: cubrir las dos formas es lo que hace que <b>el valor de la
     * propiedad deje de importar</b>. La alternativa —dejarlo atado al valor de hoy
     * y vigilarlo— convierte una línea de configuración en una bomba de relojería:
     * quien la active dentro de seis meses por un motivo ajeno reabriría el 500 de
     * #327 sin relación aparente con lo que tocó. Sale gratis porque
     * {@code MethodValidationException} y {@code HandlerMethodValidationException}
     * implementan la misma interfaz, {@code MethodValidationResult}: no hay una
     * segunda lógica que mantener en paralelo, solo una segunda puerta a la misma.
     *
     * <p>
     * <b>Responde 400 aunque el contrato del método proponga 500</b>, que es el
     * valor con el que Spring lo invoca. Lo que falló son los datos que escribió el
     * cliente —los mismos que por la puerta de Jakarta ya dan 400—, y contarlo como
     * caída del servidor es justo el defecto de #327: mancha la tasa de error,
     * marca el span en rojo y despierta la alarma por un parámetro mal tecleado.
     */
    @Override
    protected ResponseEntity<Object> handleMethodValidationException(MethodValidationException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        return handleExceptionInternal(ex, validationProblem(parameterErrors(ex)), headers,
                HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Los errores por campo de un resultado de validación de método, sea cual sea
     * su envoltorio.
     */
    private static List<Map<String, String>> parameterErrors(MethodValidationResult result) {
        Map<String, String> byField = new LinkedHashMap<>();
        for (ParameterValidationResult parameterResult : result.getParameterValidationResults()) {
            collectParameterErrors(parameterResult, byField);
        }
        return asFieldErrors(byField);
    }

    private static void collectParameterErrors(ParameterValidationResult result,
            Map<String, String> byField) {
        // Argumento-objeto (@ModelAttribute, @RequestPart): sus errores ya vienen con
        // el nombre de la propiedad, igual que en Bean Validation.
        if (result instanceof ParameterErrors errors) {
            for (FieldError fieldError : errors.getFieldErrors()) {
                mergeFieldError(byField, fieldError.getField(), fieldError.getDefaultMessage());
            }
            return;
        }
        String name = requestParameterName(result.getMethodParameter());
        for (MessageSourceResolvable error : result.getResolvableErrors()) {
            mergeFieldError(byField, name, error.getDefaultMessage());
        }
    }

    /**
     * Nombre del parámetro tal como lo escribe el cliente: el declarado en la
     * anotación de binding y, si va sin nombre explícito, el del código fuente —el
     * compilador conserva los reales, porque spring-boot-starter-parent activa
     * {@code -parameters}—. Nunca el nombre del método ni el de la clase.
     */
    private static String requestParameterName(MethodParameter parameter) {
        String annotated = annotatedParameterName(parameter);
        if (!annotated.isEmpty()) {
            return annotated;
        }
        String declared = parameter.getParameterName();
        return declared == null ? "" : declared;
    }

    private static String annotatedParameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            return firstNonBlank(requestParam.value(), requestParam.name());
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return firstNonBlank(pathVariable.value(), pathVariable.name());
        }
        RequestHeader header = parameter.getParameterAnnotation(RequestHeader.class);
        if (header != null) {
            return firstNonBlank(header.value(), header.name());
        }
        return "";
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : "";
    }

    /**
     * {@code @Validated} sobre la clase del controller no produce un
     * {@code HandlerMethodValidationException}: el proxy de validación de método
     * lanza el {@code ConstraintViolationException} crudo de Jakarta, que sin
     * handler propio caía en {@code handleUnexpected} y salía como <b>500</b>
     * (#327) — un error del cliente contado como caída del servidor, con su span
     * marcado en rojo y su línea en ERROR.
     *
     * <p>
     * Del {@code Path} de cada violación se publica solo el último tramo nombrado
     * ({@code listAll.page} → {@code page}): el primero es el nombre del método
     * Java, que no es un control del formulario ni algo que convenga publicar.
     *
     * <p>
     * <b>No depende de la configuración</b>: esta es la forma que lanza el proxy
     * por defecto, y la otra —{@code MethodValidationException}, la que aparece con
     * {@code spring.validation.method.adapt-constraint-violations=true}— la atiende
     * {@code handleMethodValidationException} con esta misma respuesta (#330). Da
     * igual cómo esté la propiedad.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {
        Map<String, String> byField = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            mergeFieldError(byField, violatedPropertyName(violation.getPropertyPath()),
                    violation.getMessage());
        }
        // Solo los nombres de campo. El mensaje interpolado puede arrastrar el valor
        // rechazado y esta clase no registra valores del cliente (ASVS V7.1.1).
        log.info("Client error 400 on validated parameters: fields={}", byField.keySet());
        return validationProblem(asFieldErrors(byField));
    }

    /**
     * Último tramo nombrado de la ruta de una violación, saltándose los nodos que
     * describen la invocación (método, constructor, parámetros cruzados, valor
     * devuelto). Un anidado dentro de un contenedor pierde el prefijo —
     * {@code lines[0].quantity} sale como {@code quantity}—: por aquí solo llegan
     * parámetros validados, donde ese caso no se da, y el cuerpo con colecciones
     * viaja por {@code handleMethodArgumentNotValid}, que sí conserva la ruta
     * completa.
     */
    private static String violatedPropertyName(Path propertyPath) {
        String field = "";
        for (Path.Node node : propertyPath) {
            ElementKind kind = node.getKind();
            if (kind == ElementKind.METHOD || kind == ElementKind.CONSTRUCTOR
                    || kind == ElementKind.CROSS_PARAMETER || kind == ElementKind.RETURN_VALUE) {
                continue;
            }
            if (node.getName() != null && !node.getName().isBlank()) {
                field = node.getName();
            }
        }
        return field;
    }

    /**
     * Conversión imposible de un parámetro o de una variable de ruta:
     * {@code ?status=NINGUNO} sobre un enum, {@code /animals/abc} sobre un
     * {@code Long}. Es el mismo defecto que el enum del cuerpo visto desde la query
     * string —falla en el binding, antes de Bean Validation— y también salía 400
     * sin {@code code} ni campo culpable.
     *
     * <p>
     * El detalle que Spring trae de serie no se publica: dice
     * {@code Failed to convert value of type 'java.lang.String' to required type…},
     * que nombra tipos Java y repite el valor rechazado.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> byField = new LinkedHashMap<>();
        mergeFieldError(byField, typeMismatchFieldName(ex),
                unconvertibleValueMessage(ex.getRequiredType()));
        ProblemDetail pd = byField.isEmpty()
                ? problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Invalid request content.")
                : validationProblem(asFieldErrors(byField));
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    private static String typeMismatchFieldName(TypeMismatchException ex) {
        return ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        String constraintName = constraintNameOf(ex, cause);
        ProblemDetail mapped = mapConstraint(cause);
        if (mapped != null) {
            // 409 = conflicto atribuible al cliente (p.ej. valor duplicado) → WARN, no
            // ERROR.
            //
            // Se registra el nombre de la constraint y jamás el mensaje: el de Hibernate
            // arrastra la sentencia y el valor duplicado —el documento, el correo o el
            // nombre de un propietario real— y la redacción por patrones no reconoce
            // nombres propios ni prosa (ASVS V7.1.1). La constraint es un identificador del
            // esquema, y es además lo único que este handler usa.
            log.info("Data integrity violation: constraint={} type={}", constraintName,
                    ex.getClass().getSimpleName());
            return mapped;
        }
        // Constraint sin mapeo de negocio: la respuesta es genérica, así que el único
        // rastro para poder mapearla después es este evento. Aquí sí va el throwable,
        // porque en esta ruta lo redacta RedactedThrowable — RedactingAppender envuelve
        // a TODOS los appenders de la raíz en logback-spring.xml, y redacta el mensaje
        // de cada excepción de la cadena antes de que salga del proceso. Es redacción
        // por patrones, no una garantía: un nombre propio dentro del Duplicate entry
        // sobrevive. Se acepta porque esta rama es la cola rara y sin ella la
        // constraint nueva no se puede diagnosticar.
        log.warn("Unmapped data integrity violation: constraint={} type={}", constraintName,
                ex.getClass().getSimpleName(), ex);
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Database constraint violation");
    }

    /**
     * Traduce la constraint violada a un conflicto de negocio, o devuelve null si
     * no hay ninguna que le corresponda.
     */
    private ProblemDetail mapConstraint(String cause) {
        // Carrera en "un veterinario no puede tener dos citas activas a la misma hora"
        // (issue #114). Hasta que existió el índice, la detección de solape era un
        // SELECT
        // y un filtro en Java: dos peticiones simultáneas sobre el mismo hueco pasaban
        // las dos el check, se guardaban las dos y salían las dos confirmaciones por
        // correo. El índice cierra la carrera; este mapeo es lo que convierte su error
        // de integridad en el mismo 409 que ya emite el check síncrono.
        //
        // MISMO código que AppointmentOverlapException, a propósito: al front le da
        // igual
        // si el cruce lo detectó Java o lo detectó la base, y un código distinto le
        // obligaría a escribir dos veces el mismo tratamiento.
        //
        // Lo que NO puede llevar esta rama es overlappingAppointmentIds. Aquí lo único
        // que hay es el nombre de la constraint —del documento que chocó no se sabe ni
        // con qué chocó— y el front tiene que tolerar su ausencia: por eso el detail
        // habla del cruce sin prometer citas concretas que enlazar.
        //
        // Se mapea por NOMBRE de constraint, así que la columna generada
        // active_slot_employee_id —el truco para simular un índice parcial en MySQL,
        // que
        // vale NULL en las citas canceladas o no compareció— no afecta a este código
        // aunque el driver la nombre en el mensaje.
        if (cause != null && cause.contains("uq_appointments_active_employee_start")) {
            return problem(HttpStatus.CONFLICT, "APPOINTMENT_OVERLAP",
                    "El veterinario seleccionado acaba de quedar ocupado en ese horario."
                            + " Elige otra hora o vuelve a intentarlo.");
        }
        // Carrera en "1 cuenta abierta por propietario y sede": la constraint única
        // atrapa
        // la 2ª inserción concurrente que pasó el check del service. Se mapea al mismo
        // código que
        // el guard de negocio para que el front lo trate igual.
        if (cause != null && (cause.contains("uq_open_accounts_active_owner_branch")
                || cause.contains("uq_open_accounts_active_owner"))) {
            return problem(HttpStatus.CONFLICT, "OWNER_ALREADY_HAS_OPEN_ACCOUNT",
                    "El propietario ya tiene una cuenta abierta en esta sede.");
        }
        // Carrera en la unicidad de SKU por empresa (constraint de la migración 133):
        // la 2ª inserción
        // concurrente que pasó el check del service la atrapa la BD. Se mapea al mismo
        // código de
        // negocio.
        // Las seis constraints del catalogo de plataforma (#437). Las altas de
        // configurator y pricelist ya tienen guarda previa —consultan la fila ignorando
        // el borrado logico y la reactivan en vez de insertar—, pero una guarda previa
        // no puede cerrar la CARRERA: dos administradores dan de alta a la vez el mismo
        // codigo de pregunta desde dos pestañas, los dos leen antes de que el otro
        // escriba, y el segundo INSERT muere contra el indice. Sin mapeo eso sale como
        // un 409 con el detail "Database constraint violation" —sobre catalog_prices,
        // hablando de dinero— y deja un WARN indistinguible de un problema real de
        // integridad.
        //
        // MISMO errorCode que el guard sincrono de cada caso, igual que en las tres
        // ramas de arriba: al front le da igual si el choque lo detecto Java o lo
        // detecto la base, y un codigo distinto le obligaria a escribir dos veces el
        // mismo tratamiento.
        //
        // uq_catalog_prices_tier va incluida aunque CreateCatalogPriceService bloquee
        // la lista con PESSIMISTIC_WRITE y ahi la carrera este serializada: es red, y
        // cuesta un if. En las tres tablas de configurator y en price_lists no hay
        // bloqueo y la carrera esta abierta de verdad.
        if (cause != null && (cause.contains("uq_configurator_questions_code")
                || cause.contains("uq_configurator_options_code"))) {
            return problem(HttpStatus.CONFLICT, "CONFIGURATOR_CODE_ALREADY_EXISTS",
                    "Ese código ya está en uso en el cuestionario.");
        }
        if (cause != null && (cause.contains("uq_configurator_effects_option")
                || cause.contains("uq_configurator_effects_question"))) {
            return problem(HttpStatus.CONFLICT, "CONFIGURATOR_EFFECT_ALREADY_EXISTS",
                    "Esa respuesta ya tiene un efecto sobre ese artículo.");
        }
        if (cause != null && cause.contains("uq_price_lists_code")) {
            return problem(HttpStatus.CONFLICT, "PRICE_LIST_CODE_ALREADY_EXISTS",
                    "Ya existe una lista de precios con ese código.");
        }
        if (cause != null && cause.contains("uq_catalog_prices_tier")) {
            return problem(HttpStatus.CONFLICT, "CATALOG_PRICE_TIER_OVERLAP",
                    "Ese tramo de precio acaba de ser creado por otra persona."
                            + " Recarga la tarifa y revisa los tramos del artículo.");
        }
        if (cause != null && cause.contains("uq_products_company_active_code")) {
            return problem(HttpStatus.CONFLICT, "PRODUCT_CODE_ALREADY_EXISTS",
                    "Ya existe un producto activo con ese código en esta empresa.");
        }
        // Carrera en la unicidad de NOMBRE por empresa (constraints de las migraciones
        // 151-154).
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
        if (cause != null && cause.contains("uq_suppliers_company_active_name")) {
            return problem(HttpStatus.CONFLICT, "SUPPLIER_NAME_ALREADY_EXISTS",
                    "Ya existe un proveedor activo con ese nombre en esta empresa.");
        }
        // Carrera en la unicidad del número de factura de proveedor por (empresa,
        // proveedor) (migración
        // 203).
        if (cause != null && cause.contains("uq_supplier_invoices_active_number")) {
            return problem(HttpStatus.CONFLICT, "SUPPLIER_INVOICE_NUMBER_ALREADY_EXISTS",
                    "Ya existe una factura activa con ese número para ese proveedor.");
        }
        // Carrera en "un documento por cuenta cerrada" (constraint de la migración
        // 134): dos cierres
        // concurrentes que pasaron el check `existsByOpenAccountId`; la BD impide la 2ª
        // emisión fiscal.
        if (cause != null && cause.contains("uq_electronic_documents_open_account")) {
            return problem(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_EMITTED",
                    "La venta ya tiene un documento electrónico emitido.");
        }
        // Carrera en la idempotencia de abonos (constraint de la migración 135):
        // doble-submit
        // concurrente con
        // la misma clave; la BD rechaza el 2º. El cliente reintenta y el check de
        // idempotencia devuelve
        // el abono.
        if (cause != null && cause.contains("uq_debt_open_accounts_request")) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT_REQUEST",
                    "El abono ya fue registrado (solicitud duplicada).");
        }
        // Carrera en la idempotencia de cargos (constraints de las migraciones
        // 139/140/141):
        // doble-submit
        // concurrente con la misma clave; la BD rechaza el 2º. El cliente reintenta y
        // el check de
        // idempotencia
        // devuelve el cargo ya creado.
        if (cause != null && (cause.contains("uq_product_charge_open_accounts_request")
                || cause.contains("uq_service_charge_open_accounts_request")
                || cause.contains("uq_general_charge_open_accounts_request"))) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_CHARGE_REQUEST",
                    "El cargo ya fue registrado (solicitud duplicada).");
        }
        // Carrera/reactivación en la unicidad de "una sola resolución activa por
        // (company, tipo)"
        // (migración
        // 144). La 2ª inserción/reactivación concurrente que pasó el check del service
        // la atrapa la BD.
        if (cause != null && cause.contains("uq_numbering_resolutions_active")) {
            return problem(HttpStatus.CONFLICT, "NUMBERING_RESOLUTION_ALREADY_ACTIVE",
                    "La empresa ya tiene una resolución de numeración activa para ese tipo de documento.");
        }
        // Carrera en la unicidad de employee_code (código de empleado / correo del
        // dueño en el
        // registro):
        // el 2º INSERT concurrente lo rechaza la BD. Código neutral (el mensaje
        // específico "correo ya
        // registrado" lo emite el pre-check del registro vía EMAIL_ALREADY_REGISTERED).
        if (cause != null && cause.contains("employee_code")) {
            return problem(HttpStatus.CONFLICT, "EMPLOYEE_CODE_TAKEN",
                    "Ese usuario ya está en uso. Elige otro.");
        }
        // Carrera en "una sola caja OPEN por empleado". Mismo código que la validación
        // del servicio.
        if (cause != null && cause.contains("uq_cash_session_employee_open")) {
            return problem(HttpStatus.CONFLICT, "EMPLOYEE_CASH_SESSION_ALREADY_OPEN",
                    "Ya tienes una caja abierta. Debes cerrarla antes de abrir otra.");
        }
        // Carrera en "una sola caja OPEN por (empresa, sede, terminal)" (índice único
        // condicional de la
        // migración
        // 195): la 2ª apertura concurrente que pasó el check del service la atrapa la
        // BD. Mismo código
        // de negocio.
        if (cause != null && cause.contains("uq_cash_session_open")) {
            return problem(HttpStatus.CONFLICT, "CASH_SESSION_ALREADY_OPEN",
                    "La terminal seleccionada ya tiene una caja abierta.");
        }
        return null;
    }

    /**
     * Nombre de la constraint violada, o unknown si no se puede determinar. No
     * devuelve nunca texto del mensaje del driver: solo el identificador de
     * esquema, que describe la estructura y no la fila.
     */
    private static String constraintNameOf(Throwable ex, String causeMessage) {
        // Hibernate lo expone estructurado cuando reconoce el error del driver; esa
        // vía no analiza texto y no puede arrastrar datos de la fila.
        Throwable current = ex;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (current instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null
                    && !violation.getConstraintName().isBlank()) {
                return violation.getConstraintName();
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        if (causeMessage != null) {
            Matcher matcher = CONSTRAINT_NAME.matcher(causeMessage);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "unknown";
    }

    /**
     * Detalle publicable de un 4xx. El mensaje de una excepción de binding o de
     * deserialización lleva dentro el payload del cliente, así que de esas se
     * registra el tipo y —cuando los hay— los nombres de campo, que son esquema. El
     * resto de mensajes de Spring MVC son constantes del framework y se conservan
     * tal cual.
     */
    private static String clientErrorDetail(Exception ex) {
        if (ex instanceof BindException binding) {
            List<String> fields = binding.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getField).distinct().toList();
            return ex.getClass().getSimpleName() + " fields=" + fields;
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return ex.getClass().getSimpleName();
        }
        // El mensaje de una conversión fallida lleva dentro el valor que tecleó el
        // cliente ("For input string: \"...\"") y el tipo Java esperado; del mismo
        // modo que en las de binding, se registra el tipo y el campo.
        if (ex instanceof TypeMismatchException mismatch) {
            return ex.getClass().getSimpleName() + " field=" + typeMismatchFieldName(mismatch);
        }
        return ex.getMessage();
    }

    // ---------------------------------------------------------------------------------------------
    // La plataforma no está configurada para servir la operación: 503 en las dos, y
    // el mismo código.
    //
    // Son la misma condición vista desde dos slices —falta la fila única de
    // platform_billing_config, o falta el catálogo comercial mínimo con el que
    // firmar el contrato inicial—, y responder con dos códigos distintos a lo mismo
    // obliga a quien depura a aprenderse dos vocabularios para un solo problema.
    //
    // 503 y no 500: la petición del cliente es correcta y el servidor no se ha
    // caído; falta sembrar un dato de despliegue, y el intento vuelve a funcionar
    // en
    // cuanto un operador lo siembre. 503 y no 404: no falta el recurso de negocio
    // que se pidió, falta el suelo sobre el que la operación se apoya, y un 404
    // mandaría a buscar el registro equivocado.
    //
    // El detail propaga el mensaje TAL CUAL a propósito: las dos excepciones lo
    // redactaron con el INSERT (o con los cinco pasos) que lo arreglan, y ese texto
    // es lo único que separa un 503 opaco de uno accionable.
    //
    // log.error y markObservationError: es un despliegue incompleto, pide acción
    // humana y tiene que contar como request fallido en las métricas, no diluirse
    // entre los 4xx normales.
    // ---------------------------------------------------------------------------------------------

    @ExceptionHandler(PlatformBillingConfigNotConfiguredException.class)
    public ProblemDetail handlePlatformBillingConfigNotConfigured(
            PlatformBillingConfigNotConfiguredException ex, HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("Platform billing config row is missing; billing cannot run", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "PLATFORM_BILLING_CONFIG_NOT_CONFIGURED",
                ex.getMessage());
    }

    @ExceptionHandler(PlatformCatalogNotConfiguredException.class)
    public ProblemDetail handlePlatformNotConfigured(PlatformCatalogNotConfiguredException ex,
            HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("Platform commercial catalog is not seeded; registration cannot complete", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "PLATFORM_CATALOG_NOT_CONFIGURED",
                ex.getMessage());
    }

    // La gemela de la de arriba en la dimension de los permisos, y va aqui —503,
    // log.error, markObservationError— por lo mismo: es un despliegue incompleto,
    // no una entrada mala del cliente, y pide accion humana antes de que llegue el
    // siguiente registro.
    //
    // errorCode PROPIO y no el compartido de las dos de catalogo comercial: no es
    // una decision de producto sino de diagnostico. Aquellas se arreglan sembrando
    // catalog_items/price_lists; esta se arregla en base_roles, que es otra tabla,
    // otro changeset y otro dueño. Compartir codigo mandaria a quien lo lea a
    // sembrar el catalogo comercial —que puede estar perfecto— y a no mirar la
    // tabla que de verdad esta vacia. Razonado en #500.
    @ExceptionHandler(PlatformRoleCatalogNotConfiguredException.class)
    public ProblemDetail handlePlatformRoleCatalogNotConfigured(
            PlatformRoleCatalogNotConfiguredException ex, HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("Platform base roles are not seeded; registration cannot complete", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "PLATFORM_ROLE_CATALOG_NOT_CONFIGURED",
                ex.getMessage());
    }

    // La tercera de la familia, y la unica que NO es 503: ver el javadoc de
    // OwnerWithoutBranchException. Las dos de arriba denuncian un despliegue
    // incompleto —faltan filas de catalogo, un humano las siembra y el siguiente
    // registro pasa—; esta denuncia que el alta dejo de cuadrar consigo misma sobre
    // dos filas que ella misma acaba de crear. No hay nada que sembrar ni nada que
    // el cliente pueda reintentar, asi que 500 y no 503, con el mismo log.error y
    // el mismo markObservationError porque pide accion humana igual de urgente:
    // mientras dure, TODA empresa nueva nace sin poder invitar a nadie (#510).
    @ExceptionHandler(OwnerWithoutBranchException.class)
    public ProblemDetail handleOwnerWithoutBranch(OwnerWithoutBranchException ex,
            HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("Registered owner ended with no branch assigned; registration rolled back", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "REGISTRATION_OWNER_WITHOUT_BRANCH",
                ex.getMessage());
    }

    // ---------------------------------------------------------------------------------------------
    // Autochequeos de integridad de una cotización: 500, no 409.
    //
    // Las dos saltan cuando un documento YA GUARDADO deja de cuadrar consigo mismo:
    // los cuatro totales de la cabecera no son la suma de las líneas, o la
    // aritmética congelada de una línea no se sostiene. Nadie envía esos importes —
    // los calcula el servidor desde las líneas —, así que si no cuadran es
    // corrupción del dato o un defecto propio, nunca una entrada mala.
    //
    // No es 409 aunque las dos hereden de IllegalStateException, y por eso llevan
    // handler propio: es más específico que handleConflictState y Spring lo elige.
    // Un 409 le dice al cliente que el estado cambió y que reintente, y aquí el
    // reintento vuelve a fallar igual porque no hay nada que el cliente pueda
    // hacer.
    //
    // log.error y no el log.info de los 4xx de negocio: esto pide que un humano
    // mire
    // la fila. El detalle que sale al cliente es constante —los importes solo
    // sirven
    // para diagnosticar y se quedan en el log—, igual que en el resto de 5xx.
    // ---------------------------------------------------------------------------------------------

    @ExceptionHandler({QuoteTotalsMismatchException.class, QuoteLineArithmeticException.class})
    public ProblemDetail handleQuoteIntegrity(IllegalStateException ex,
            HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("Quote integrity self-check failed", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "QUOTE_DATA_CORRUPTED",
                "La cotización tiene importes inconsistentes y no se puede usar. Contacta a"
                        + " soporte.");
    }

    @ExceptionHandler(PdfRenderException.class)
    public ProblemDetail handlePdfRender(PdfRenderException ex, HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("PDF render failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "PDF_RENDER_FAILED",
                "Failed to generate PDF document");
    }

    @ExceptionHandler(S3StorageException.class)
    public ProblemDetail handleS3Storage(S3StorageException ex, HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("S3 storage operation failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "FILE_STORAGE_FAILED",
                "Failed to access file storage");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        markObservationError(request, ex);
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            pd.setProperty("traceId", currentSpan.context().traceId());
        }
        return pd;
    }

    /**
     * Añade una propiedad al ProblemDetail solo si tiene valor. Un
     * {@code setProperty(k, null)} deja la clave presente con {@code null} en el
     * JSON, y el front no distingue "no aplica" de "vino vacío".
     */
    private static void setIfPresent(ProblemDetail pd, String name, Object value) {
        if (value != null) {
            pd.setProperty(name, value);
        }
    }

    private static void markObservationError(HttpServletRequest request, Throwable error) {
        ServerHttpObservationFilter.findObservationContext(request)
                .ifPresent(context -> context.setError(error));
    }

    private static String errorCode(RuntimeException ex) {
        String name = ex.getClass().getSimpleName().replace("Exception", "");
        return camelToSnakeUpper(name);
    }

    private static String camelToSnakeUpper(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0)
                out.append('_');
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }
}
