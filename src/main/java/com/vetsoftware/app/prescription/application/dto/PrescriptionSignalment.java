package com.vetsoftware.app.prescription.application.dto;

/**
 * Datos de identificación (clínica, paciente y propietario) para la fórmula médica veterinaria.
 * Cadenas listas para mostrar; los nulos/ausentes se omiten en la plantilla.
 */
public record PrescriptionSignalment(
    String clinicName,
    String clinicIdentifier,
    String clinicAddress,
    String clinicPhone,
    String clinicCity,
    String patientName,
    String patientCode,
    String patientSpecies,
    String patientBreed,
    String patientSex,
    String patientColor,
    String patientAge,
    String patientWeight,
    String ownerName,
    String ownerDocument,
    String ownerPhone,
    String ownerEmail,
    String ownerAddress) {}
