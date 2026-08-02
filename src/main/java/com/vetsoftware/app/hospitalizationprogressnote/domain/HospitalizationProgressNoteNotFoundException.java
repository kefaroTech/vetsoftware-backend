package com.vetsoftware.app.hospitalizationprogressnote.domain;

public class HospitalizationProgressNoteNotFoundException extends RuntimeException {
  public HospitalizationProgressNoteNotFoundException(Long id) {
    super("HospitalizationProgressNote not found: " + id);
  }
}
