package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaxReturnJpaMapper")
class TaxReturnJpaMapperTest {

    private final TaxReturnJpaMapper mapper = new TaxReturnJpaMapper();

    /**
     * Una declaracion de ICA ya presentada y persistida: ejercita el municipio, las
     * cinco columnas de presentacion y una version distinta de null, que es lo que
     * hace interesante el redondeo por este camino.
     */
    private static TaxReturn declaracionPersistida(Long id, Long version) {
        return new TaxReturn(id, TaxKind.ICA, 2026, "2026-B02", 1, "05001", null,
                TaxReturnStatus.FILED, LocalDateTime.of(2026, 3, 20, 9, 0), 10L, "RAD-001",
                "s3://bucket/file.pdf", new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("800.00"), BigDecimal.ZERO, LocalDate.of(2029, 3, 20), null,
                LocalDateTime.of(2026, 3, 15, 8, 0), version);
    }

    @Test
    @DisplayName("toDomain despues de toJpa reconstruye la declaracion campo a campo")
    void to_domain_despues_de_to_jpa_reconstruye_la_declaracion() {
        TaxReturn origen = declaracionPersistida(30L, 4L);

        TaxReturn reconstruida = mapper.toDomain(mapper.toJpa(origen));

        assertThat(reconstruida.getId()).isEqualTo(origen.getId());
        assertThat(reconstruida.getTaxKind()).isEqualTo(origen.getTaxKind());
        assertThat(reconstruida.getFiscalYear()).isEqualTo(origen.getFiscalYear());
        assertThat(reconstruida.getFiscalPeriodKey()).isEqualTo(origen.getFiscalPeriodKey());
        assertThat(reconstruida.getSequenceNumber()).isEqualTo(origen.getSequenceNumber());
        assertThat(reconstruida.getMunicipalityCode()).isEqualTo(origen.getMunicipalityCode());
        assertThat(reconstruida.getVatFrequency()).isEqualTo(origen.getVatFrequency());
        assertThat(reconstruida.getStatus()).isEqualTo(origen.getStatus());
        assertThat(reconstruida.getFiledAt()).isEqualTo(origen.getFiledAt());
        assertThat(reconstruida.getFiledBySystemUserId())
                .isEqualTo(origen.getFiledBySystemUserId());
        assertThat(reconstruida.getReceiptRef()).isEqualTo(origen.getReceiptRef());
        assertThat(reconstruida.getFileRef()).isEqualTo(origen.getFileRef());
        assertThat(reconstruida.getTotalGenerated())
                .isEqualByComparingTo(origen.getTotalGenerated());
        assertThat(reconstruida.getTotalDeductible())
                .isEqualByComparingTo(origen.getTotalDeductible());
        assertThat(reconstruida.getBalancePayable())
                .isEqualByComparingTo(origen.getBalancePayable());
        assertThat(reconstruida.getBalanceCredit()).isEqualByComparingTo(origen.getBalanceCredit());
        assertThat(reconstruida.getFirmezaUntil()).isEqualTo(origen.getFirmezaUntil());
        assertThat(reconstruida.getCorrectsReturnId()).isEqualTo(origen.getCorrectsReturnId());
        assertThat(reconstruida.getCreatedDate()).isEqualTo(origen.getCreatedDate());
        assertThat(reconstruida.getVersion()).isEqualTo(origen.getVersion());
    }

    @Test
    @DisplayName("toJpa conserva la version en una declaracion ya persistida: no la deja en null")
    void to_jpa_conserva_la_version_en_una_declaracion_persistida() {
        TaxReturn origen = declaracionPersistida(55L, 12L);

        TaxReturnJpaEntity entidad = mapper.toJpa(origen);

        // Con version en null sobre una entidad que ya tiene id, Hibernate la
        // tomaria por transitoria y el merge se convertiria en un INSERT que
        // choca contra uq_tax_returns_case: esta es la unica linea que lo impide.
        assertThat(entidad.getId()).isEqualTo(55L);
        assertThat(entidad.getVersion()).isEqualTo(12L);
    }

    @Test
    @DisplayName("el año viaja como short en la entidad y vuelve a int en el dominio")
    void el_anio_viaja_como_short_en_la_entidad_y_vuelve_a_int() {
        TaxReturn origen = declaracionPersistida(70L, 1L);

        TaxReturnJpaEntity entidad = mapper.toJpa(origen);

        assertThat(entidad.getFiscalYear()).isEqualTo((short) 2026);
        assertThat(mapper.toDomain(entidad).getFiscalYear()).isEqualTo(2026);
    }
}
