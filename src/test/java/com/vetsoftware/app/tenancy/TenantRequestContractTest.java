package com.vetsoftware.app.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animal.infrastructure.web.request.CreateAnimalRequest;
import com.vetsoftware.app.animal.infrastructure.web.request.UpdateAnimalRequest;
import com.vetsoftware.app.consultation.infrastructure.web.request.UpdateConsultationRequest;
import com.vetsoftware.app.employee.infrastructure.web.request.CreateEmployeeRequest;
import com.vetsoftware.app.owner.infrastructure.web.request.UpdateOwnerRequest;
import com.vetsoftware.app.permission.infrastructure.web.request.CreatePermissionRequest;
import com.vetsoftware.app.permission.infrastructure.web.request.UpdatePermissionRequest;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TenantRequestContractTest {

    private static final List<Class<?>> TENANT_SCOPED_REQUESTS = List.of(CreateAnimalRequest.class,
            UpdateAnimalRequest.class, UpdateConsultationRequest.class, CreateEmployeeRequest.class,
            UpdateOwnerRequest.class, CreatePermissionRequest.class, UpdatePermissionRequest.class);

    @Test
    void tenantScopedRequestsDoNotAcceptCompanyIdFromTheClient() {
        for (Class<?> requestType : TENANT_SCOPED_REQUESTS) {
            assertThat(Stream.of(requestType.getRecordComponents()).map(RecordComponent::getName))
                    .as("record components of %s", requestType.getSimpleName())
                    .doesNotContain("companyId");
        }
    }
}
