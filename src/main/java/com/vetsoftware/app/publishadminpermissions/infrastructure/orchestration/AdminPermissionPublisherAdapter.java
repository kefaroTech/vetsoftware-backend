package com.vetsoftware.app.publishadminpermissions.infrastructure.orchestration;

import com.vetsoftware.app.basepermission.application.port.out.AdminPermissionPublisher;
import com.vetsoftware.app.publishadminpermissions.application.port.in.PublishAdminPermissionsUseCase;
import org.springframework.stereotype.Component;

@Component
public class AdminPermissionPublisherAdapter implements AdminPermissionPublisher {

    private final PublishAdminPermissionsUseCase publishUseCase;

    public AdminPermissionPublisherAdapter(PublishAdminPermissionsUseCase publishUseCase) {
        this.publishUseCase = publishUseCase;
    }

    @Override
    public void publish() {
        publishUseCase.execute();
    }
}
