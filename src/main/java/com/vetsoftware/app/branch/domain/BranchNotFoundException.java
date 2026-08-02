package com.vetsoftware.app.branch.domain;

public class BranchNotFoundException extends RuntimeException {
  public BranchNotFoundException(Long id) {
    super("Branch not found: " + id);
  }
}
