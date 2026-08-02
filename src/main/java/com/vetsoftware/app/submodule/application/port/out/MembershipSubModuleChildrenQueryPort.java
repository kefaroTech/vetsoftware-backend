package com.vetsoftware.app.submodule.application.port.out;

public interface MembershipSubModuleChildrenQueryPort {
  boolean existsActiveBySubModuleId(Long parentId);
}
