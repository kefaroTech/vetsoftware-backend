package com.vetsoftware.app.membership.domain;

public class MembershipNotFoundException extends RuntimeException {
  public MembershipNotFoundException(Long id) {
    super("Membership not found: " + id);
  }
}
