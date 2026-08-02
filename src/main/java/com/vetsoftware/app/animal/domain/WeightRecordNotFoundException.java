package com.vetsoftware.app.animal.domain;

public class WeightRecordNotFoundException extends RuntimeException {
  public WeightRecordNotFoundException(Long id) {
    super("WeightRecord not found: " + id);
  }
}
