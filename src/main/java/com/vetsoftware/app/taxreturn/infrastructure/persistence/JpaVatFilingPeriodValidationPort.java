package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import com.vetsoftware.app.taxreturn.application.port.out.VatFilingPeriodValidationPort;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
import com.vetsoftware.app.vatfilingperiod.infrastructure.persistence.VatFilingPeriodJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code vatfilingperiod}.
 *
 * <p>
 * <strong>Compara por NOMBRE y no importando el enum de la otra feature, y esa
 * es la unica linea interesante de la clase.</strong> Alli la periodicidad se
 * llama {@code VatFilingFrequency} y aqui {@link VatFrequency}: son dos enums
 * distintos con los mismos tres valores, deliberadamente separados —el vertical
 * slicing prohibe importar el dominio de otra feature, y ademas springdoc funde
 * los esquemas del contrato por nombre simple, asi que dos enums homonimos
 * publicarian uno solo—. Lo unico que comparten es el literal, que es tambien
 * lo unico que comparte la clave foranea compuesta
 * {@code fk_tax_returns_vat_frequency}: la base guarda el nombre.
 *
 * <p>
 * {@code String.valueOf} sobre el valor devuelto evita nombrar el tipo ajeno
 * —en Java no hace falta importar una clase para usar el valor a traves de
 * {@code toString()}—, que es lo que mantiene este archivo sin una sola
 * dependencia hacia {@code vatfilingperiod.domain}.
 *
 * <p>
 * <strong>Si las dos listas dejaran de coincidir</strong> —una periodicidad
 * nueva publicada solo en un lado— este metodo devolveria {@code false} y el
 * alta fallaria con un mensaje legible, en vez de con una violacion de clave
 * foranea compuesta. Es el modo de fallar correcto para una divergencia que
 * nadie puede impedir desde aqui.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features.
 */
@Component("taxReturnJpaVatFilingPeriodValidationPort")
public class JpaVatFilingPeriodValidationPort implements VatFilingPeriodValidationPort {

    private final VatFilingPeriodJpaRepository vatFilingPeriodJpaRepository;

    public JpaVatFilingPeriodValidationPort(
            VatFilingPeriodJpaRepository vatFilingPeriodJpaRepository) {
        this.vatFilingPeriodJpaRepository = vatFilingPeriodJpaRepository;
    }

    @Override
    public boolean existsByFiscalYearAndFrequency(int fiscalYear, VatFrequency frequency) {
        if (frequency == null)
            return false;
        return vatFilingPeriodJpaRepository.findByFiscalYear((short) fiscalYear)
                .map(period -> String.valueOf(period.getFrequency()).equals(frequency.name()))
                .orElse(false);
    }
}
