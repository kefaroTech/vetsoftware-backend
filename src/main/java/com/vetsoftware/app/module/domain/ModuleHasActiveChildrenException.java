package com.vetsoftware.app.module.domain;

public class ModuleHasActiveChildrenException extends RuntimeException {
  public ModuleHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete module " + id + ": has active " + childType + " children");
  }
}
