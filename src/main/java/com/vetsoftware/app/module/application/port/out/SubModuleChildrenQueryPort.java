package com.vetsoftware.app.module.application.port.out;

public interface SubModuleChildrenQueryPort {
  boolean existsActiveByModuleId(Long parentId);
}
