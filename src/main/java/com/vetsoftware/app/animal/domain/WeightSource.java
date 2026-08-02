package com.vetsoftware.app.animal.domain;

/** Origen de un registro de peso: captura manual o derivado de un evento clínico. */
public enum WeightSource {
  MANUAL,
  CONSULTATION,
  HOSPITALIZATION
}
