package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico sitio que conoce a la vez el dominio y la entidad JPA,
 * y <b>sus defectos son todos silenciosos</b>: cruzar dos campos del mismo tipo
 * compila, pasa revision humana y solo se descubre cuadrando la caja. Esta
 * clase usa valores <b>todos distintos entre si</b> para que cualquier cruce
 * caiga.
 *
 * <p>
 * <b>El caso que de verdad protege algo es el de la version.</b> El adaptador
 * guarda con {@code save(...)} sobre una entidad JPA <em>nueva</em> construida
 * por {@code toJpa}: si la version leida no viajara de vuelta, Hibernate haria
 * un {@code merge} con version nula y sobreescribiria en silencio el cambio del
 * otro operador — exactamente lo que {@code @Version} existe para impedir, y
 * sin excepcion, sin log y sin 409.
 */
@DisplayName("ExternalInvoiceReconciliationJpaMapper — ida y vuelta sin cruzar campos")
class ExternalInvoiceReconciliationJpaMapperTest {

    private final ExternalInvoiceReconciliationJpaMapper mapper = new ExternalInvoiceReconciliationJpaMapper();

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("conserva los veintiun campos, la version incluida")
        void conserva_los_veintiun_campos() {
            ExternalInvoiceReconciliation original = completa();

            ExternalInvoiceReconciliation vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(41L);
            assertThat(vuelta.getCompanyId()).isEqualTo(900L);
            assertThat(vuelta.getBillingDocumentId()).isEqualTo(8600L);
            assertThat(vuelta.getExternalResolutionNumber()).isEqualTo("18764000000123");
            assertThat(vuelta.getExternalRangeFrom()).isEqualTo(1000);
            assertThat(vuelta.getExternalRangeTo()).isEqualTo(5000);
            assertThat(vuelta.getResolutionValidUntil()).isEqualTo(LocalDate.of(2027, 1, 31));
            assertThat(vuelta.getExternalInvoiceId()).isEqualTo("FE-1043");
            assertThat(vuelta.getExternalCufe()).isEqualTo("CUFE-0011");
            assertThat(vuelta.getComputedTotal()).isEqualByComparingTo("119000.00");
            assertThat(vuelta.getComputedTax()).isEqualByComparingTo("19000.00");
            assertThat(vuelta.getExternalTotal()).isEqualByComparingTo("118998.00");
            assertThat(vuelta.getExternalTax()).isEqualByComparingTo("18998.31");
            assertThat(vuelta.getDifference()).isEqualByComparingTo("2.00");
            assertThat(vuelta.getStatus())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE);
            assertThat(vuelta.getResolvedBySystemUserId()).isEqualTo(990L);
            assertThat(vuelta.getResolvedAt()).isEqualTo(LocalDateTime.of(2026, 4, 11, 9, 20, 45));
            assertThat(vuelta.getResolutionNote()).isEqualTo("Ajuste por redondeo del impuesto");
            assertThat(vuelta.getPostingPeriod()).isEqualTo("2026-03");
            assertThat(vuelta.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 3, 5, 14, 30, 15));
            assertThat(vuelta.getVersion()).isEqualTo(7L);
        }

        @Test
        @DisplayName("la version leida viaja de vuelta a la entidad JPA")
        void la_version_leida_viaja_de_vuelta() {
            // Sin este arrastre, cada save haria merge con version nula y el bloqueo
            // optimista dejaria de existir sin que nada lo dijera.
            assertThat(mapper.toJpa(completa()).getVersion()).isEqualTo(7L);
        }

        @Test
        @DisplayName("una MISSING_EXTERNAL viaja con sus cuatro huecos intactos")
        void una_missing_external_viaja_con_sus_huecos() {
            // Un mapper que rellenara alguno de los cuatro con un cero por defecto
            // haria que chk_eir_external_pair rechazara la fila.
            ExternalInvoiceReconciliation vuelta = mapper
                    .toDomain(mapper.toJpa(ExternalInvoiceReconciliationMother.abiertaConId(41L)));

            assertThat(vuelta.getStatus())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL);
            assertThat(vuelta.getExternalInvoiceId()).isNull();
            assertThat(vuelta.getExternalTotal()).isNull();
            assertThat(vuelta.getExternalTax()).isNull();
            assertThat(vuelta.getDifference()).isNull();
            assertThat(vuelta.getResolvedAt()).isNull();
            assertThat(vuelta.getPostingPeriod()).isNull();
        }
    }

    /**
     * Todos los campos poblados y todos con valores distintos entre si: las tres
     * fechas, los dos ids de FK, los cuatro importes y los dos enteros del rango.
     */
    private static ExternalInvoiceReconciliation completa() {
        return new ExternalInvoiceReconciliation(41L, 900L, 8600L, "18764000000123", 1000, 5000,
                LocalDate.of(2027, 1, 31), "FE-1043", "CUFE-0011", new BigDecimal("119000.00"),
                new BigDecimal("19000.00"), new BigDecimal("118998.00"), new BigDecimal("18998.31"),
                new BigDecimal("2.00"), ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE, 990L,
                LocalDateTime.of(2026, 4, 11, 9, 20, 45), "Ajuste por redondeo del impuesto",
                "2026-03", LocalDateTime.of(2026, 3, 5, 14, 30, 15), 7L);
    }
}
