package com.vetsoftware.app.submodule.domain;

public class SubModuleHasActiveChildrenException extends RuntimeException {
  public SubModuleHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete submodule " + id + ": has active " + childType + " children");
  }
}
