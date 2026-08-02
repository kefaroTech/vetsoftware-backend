package com.vetsoftware.app.servicechargeopenaccount.domain;

public class ServiceChargeOpenAccountNotFoundException extends RuntimeException {
  public ServiceChargeOpenAccountNotFoundException(Long id) {
    super("ServiceChargeOpenAccount not found: " + id);
  }
}
