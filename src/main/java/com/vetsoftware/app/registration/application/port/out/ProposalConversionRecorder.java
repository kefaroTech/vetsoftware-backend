package com.vetsoftware.app.registration.application.port.out;

/**
 * Deja escrito que <strong>esta empresa nacio de esta propuesta</strong>.
 *
 * <p>
 * <strong>Es lo que impide que la purga se lleve una propuesta que acabo en
 * cliente.</strong> Tres consultas del barrido de retencion de
 * {@code aiproposal} descartan con {@code NOT EXISTS} las propuestas que tienen
 * fila aqui, y la clave foranea va {@code ON DELETE RESTRICT} para que esa
 * proteccion no dependa del {@code WHERE} del job. Hasta este cambio nadie
 * escribia esa fila: las tres guardas protegian un hecho que no se registraba.
 *
 * <p>
 * <strong>Idempotente y silencioso ante lo ya escrito.</strong> Los dos unicos
 * de la tabla —una propuesta convierte una vez, una empresa nace de una sola
 * propuesta— convertirian un reintento en un 500 sobre un alta que hizo
 * exactamente lo que se le pidio.
 */
public interface ProposalConversionRecorder {

    void record(Long proposalId, Long companyId);
}
