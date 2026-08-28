package com.vetsoftware.app.supplierwithholding.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SupplierWithholdingJpaMapper")
class SupplierWithholdingJpaMapperTest {

    private final SupplierWithholdingJpaMapper mapper = new SupplierWithholdingJpaMapper();

    /**
     * Una retencion de ICA ya persistida y certificada: ejercita el municipio, los
     * dos campos de certificado, el acuse de pago y una version distinta de null,
     * que es lo que hace interesante el round-trip por este camino.
     */
    private static SupplierWithholding retencionPersistida(Long id, Long version) {
        return new SupplierWithholding(id, "900123456", "Proveedor SAS", SupplierDocumentKind.NIT,
                "FV-2026-001", SupplierWithholdingType.ICA, "Servicios veterinarios",
                new BigDecimal("1000.00"), new BigDecimal("6.900000"), new BigDecimal("69.00"),
                "05001", 2026, "2026-B02", LocalDate.of(2026, 3, 10),
                LocalDateTime.of(2026, 4, 1, 9, 0), "CERT-2026-001", "PAGO-2026-001",
                LocalDateTime.of(2026, 3, 10, 8, 0), version);
    }

    @Test
    @DisplayName("toDomain despues de toJpa reconstruye la retencion campo a campo")
    void to_domain_despues_de_to_jpa_reconstruye_la_retencion() {
        SupplierWithholding origen = retencionPersistida(40L, 3L);

        SupplierWithholding reconstruida = mapper.toDomain(mapper.toJpa(origen));

        assertThat(reconstruida.getId()).isEqualTo(origen.getId());
        assertThat(reconstruida.getSupplierTaxId()).isEqualTo(origen.getSupplierTaxId());
        assertThat(reconstruida.getSupplierName()).isEqualTo(origen.getSupplierName());
        assertThat(reconstruida.getSupplierDocType()).isEqualTo(origen.getSupplierDocType());
        assertThat(reconstruida.getSupplierInvoiceRef()).isEqualTo(origen.getSupplierInvoiceRef());
        assertThat(reconstruida.getWithholdingType()).isEqualTo(origen.getWithholdingType());
        assertThat(reconstruida.getConcept()).isEqualTo(origen.getConcept());
        assertThat(reconstruida.getTaxableBase()).isEqualByComparingTo(origen.getTaxableBase());
        assertThat(reconstruida.getRatePercent()).isEqualByComparingTo(origen.getRatePercent());
        assertThat(reconstruida.getAmount()).isEqualByComparingTo(origen.getAmount());
        assertThat(reconstruida.getMunicipalityCode()).isEqualTo(origen.getMunicipalityCode());
        assertThat(reconstruida.getFiscalYear()).isEqualTo(origen.getFiscalYear());
        assertThat(reconstruida.getFiscalPeriodKey()).isEqualTo(origen.getFiscalPeriodKey());
        assertThat(reconstruida.getPracticedOn()).isEqualTo(origen.getPracticedOn());
        assertThat(reconstruida.getCertificateIssuedAt())
                .isEqualTo(origen.getCertificateIssuedAt());
        assertThat(reconstruida.getCertificateRef()).isEqualTo(origen.getCertificateRef());
        assertThat(reconstruida.getPaymentReceiptRef()).isEqualTo(origen.getPaymentReceiptRef());
        assertThat(reconstruida.getCreatedDate()).isEqualTo(origen.getCreatedDate());
        assertThat(reconstruida.getVersion()).isEqualTo(origen.getVersion());
    }

    @Test
    @DisplayName("toJpa conserva la version en una retencion ya persistida: no la deja en null")
    void to_jpa_conserva_la_version_en_una_retencion_persistida() {
        SupplierWithholding origen = retencionPersistida(88L, 9L);

        SupplierWithholdingJpaEntity entidad = mapper.toJpa(origen);

        // Con version en null sobre una entidad que ya tiene id, Hibernate la
        // tomaria por transitoria y el merge se convertiria en un INSERT que
        // choca contra uq_supplier_withholdings_case: esta linea lo impide.
        assertThat(entidad.getId()).isEqualTo(88L);
        assertThat(entidad.getVersion()).isEqualTo(9L);
    }

    @Test
    @DisplayName("el año viaja como short en la entidad y vuelve a int en el dominio")
    void el_anio_viaja_como_short_en_la_entidad_y_vuelve_a_int() {
        SupplierWithholding origen = retencionPersistida(95L, 1L);

        SupplierWithholdingJpaEntity entidad = mapper.toJpa(origen);

        assertThat(entidad.getFiscalYear()).isEqualTo((short) 2026);
        assertThat(mapper.toDomain(entidad).getFiscalYear()).isEqualTo(2026);
    }
}
