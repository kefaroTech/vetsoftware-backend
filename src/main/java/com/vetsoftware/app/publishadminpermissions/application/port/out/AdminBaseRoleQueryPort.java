package com.vetsoftware.app.publishadminpermissions.application.port.out;

import java.util.Optional;

public interface AdminBaseRoleQueryPort {
  Optional<Long> findAdminBaseRoleId();
}
