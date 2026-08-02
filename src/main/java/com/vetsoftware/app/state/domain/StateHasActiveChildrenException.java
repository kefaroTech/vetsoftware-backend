package com.vetsoftware.app.state.domain;

public class StateHasActiveChildrenException extends RuntimeException {
  public StateHasActiveChildrenException(Long id, String childType) {
    super("Cannot delete state " + id + ": has active " + childType + " children");
  }
}
