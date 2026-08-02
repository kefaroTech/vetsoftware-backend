package com.vetsoftware.app.registration.application.port.out;

public interface CompanyIdentifierChecker {
  boolean exists(String identifier);
}
