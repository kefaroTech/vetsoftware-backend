package com.vetsoftware.app.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vetsoftware.app.consultation.application.port.in.DeleteConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.UpdateConsultationUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ChangeDiagnosticImagingStatusUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.DeleteMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.UpdateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.DeletePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.UpdatePrescriptionUseCase;
import com.vetsoftware.app.role.application.port.in.CreateRoleUseCase;
import com.vetsoftware.app.role.application.port.in.DeleteRoleUseCase;
import com.vetsoftware.app.role.application.port.in.ReactivateRoleUseCase;
import com.vetsoftware.app.role.application.port.in.UpdateRoleUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.CreateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.DeleteRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ReactivateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.UpdateRolePermissionUseCase;
import com.vetsoftware.app.spa.application.port.in.ChangeSpaStatusUseCase;
import com.vetsoftware.app.spa.application.port.in.DeleteSpaUseCase;
import com.vetsoftware.app.spa.application.port.in.UpdateSpaUseCase;
import com.vetsoftware.app.surgery.application.port.in.ChangeSurgeryStatusUseCase;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.security.access.prepost.PreAuthorize;

class GranularPermissionGateTest {

    @Test
    void activeAuthorizationCodeDoesNotUseTheLegacyWildcard() throws Exception {
        Path sourceRoot = Path.of("src", "main", "java");
        List<Path> offenders;
        try (Stream<Path> sources = Files.walk(sourceRoot)) {
            offenders = sources
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> {
                    try {
                        return Files.readString(path).contains("admin.all");
                    } catch (java.io.IOException exception) {
                        throw new java.io.UncheckedIOException(exception);
                    }
                })
                .toList();
        }
        assertTrue(offenders.isEmpty(), "Legacy wildcard remains in: " + offenders);
    }

    private record Gate(Class<?> type, String permission) {}

    @TestFactory
    Stream<DynamicTest> tenantMutationsHonorPublishedPermissionAndCompanyScope() {
        return Stream.of(
            new Gate(UpdateConsultationUseCase.class, "consultation.update"),
            new Gate(DeleteConsultationUseCase.class, "consultation.delete"),
            new Gate(UpdatePrescriptionUseCase.class, "prescription.update"),
            new Gate(DeletePrescriptionUseCase.class, "prescription.delete"),
            new Gate(UpdateMedicamentPrescriptionUseCase.class, "medicamentPrescription.update"),
            new Gate(DeleteMedicamentPrescriptionUseCase.class, "medicamentPrescription.delete"),
            new Gate(UpdateSpaUseCase.class, "spa.update"),
            new Gate(DeleteSpaUseCase.class, "spa.delete"),
            new Gate(ChangeSpaStatusUseCase.class, "spa.update"),
            new Gate(ChangeSurgeryStatusUseCase.class, "surgery.update"),
            new Gate(ChangeDiagnosticImagingStatusUseCase.class, "diagnosticimaging.update"),
            new Gate(CreateRoleUseCase.class, "rolePermissions.create"),
            new Gate(UpdateRoleUseCase.class, "rolePermissions.update"),
            new Gate(DeleteRoleUseCase.class, "rolePermissions.delete"),
            new Gate(ReactivateRoleUseCase.class, "rolePermissions.update"),
            new Gate(CreateRolePermissionUseCase.class, "rolePermissions.create"),
            new Gate(UpdateRolePermissionUseCase.class, "rolePermissions.update"),
            new Gate(DeleteRolePermissionUseCase.class, "rolePermissions.delete"),
            new Gate(ReactivateRolePermissionUseCase.class, "rolePermissions.update")
        ).map(gate -> DynamicTest.dynamicTest(gate.type().getSimpleName(), () -> {
            Method method = Stream.of(gate.type().getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("execute"))
                .findFirst()
                .orElseThrow();
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertNotNull(annotation);
            assertTrue(annotation.value().contains("hasRole('SYSTEM')"));
            assertFalse(annotation.value().contains("admin.all"));
            assertTrue(annotation.value().contains("hasAuthority('" + gate.permission() + "')"));
            assertTrue(annotation.value().contains("@authz.isMyCompany"));
            assertFalse(annotation.value().contains("role_permission.update"));
        }));
    }
}
