package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListBaseRolesUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  List<BaseRoleDto> listAll();
}
