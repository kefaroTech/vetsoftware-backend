package com.vetsoftware.app.publishadminpermissions.infrastructure.web;

import com.vetsoftware.app.publishadminpermissions.application.port.in.PublishAdminPermissionsUseCase;
import com.vetsoftware.app.publishadminpermissions.infrastructure.web.response.PublishAdminPermissionsResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/admin-permissions")
public class AdminPermissionPublishController {

  private final PublishAdminPermissionsUseCase publishAdminPermissionsUseCase;

  public AdminPermissionPublishController(
      PublishAdminPermissionsUseCase publishAdminPermissionsUseCase) {
    this.publishAdminPermissionsUseCase = publishAdminPermissionsUseCase;
  }

  @PostMapping("/publish")
  public PublishAdminPermissionsResponse publish() {
    return PublishAdminPermissionsResponse.from(publishAdminPermissionsUseCase.execute());
  }
}
