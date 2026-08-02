package com.vetsoftware.app.generalchargeopenaccount.domain;

public class GeneralChargeOpenAccountAlreadyVoidedException extends RuntimeException {
  public GeneralChargeOpenAccountAlreadyVoidedException(Long id) {
    super("General charge open account already voided: " + id);
  }
}
