package com.vetsoftware.app.clinicalhistory.application.dto;

import java.util.List;

/**
 * Tabla de detalle de un evento (p. ej. medicamentos de una receta o
 * medicaciones de una hospitalización). El adaptador de PDF la renderiza como
 * tabla con encabezado y filas.
 */
public record DetailTable(String title, List<String> columns, List<List<String>> rows) {
}
