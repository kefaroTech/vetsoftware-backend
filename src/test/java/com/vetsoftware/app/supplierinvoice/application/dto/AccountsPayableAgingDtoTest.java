package com.vetsoftware.app.supplierinvoice.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AccountsPayableAgingDto")
class AccountsPayableAgingDtoTest {

    @Nested
    @DisplayName("Bucket.zero")
    class BucketZero {

        @Test
        @DisplayName("todos los tramos y el total nacen en cero")
        void todos_los_tramos_nacen_en_cero() {
            AccountsPayableAgingDto.Bucket bucket = AccountsPayableAgingDto.Bucket.zero();

            assertThat(bucket.current()).isEqualByComparingTo("0");
            assertThat(bucket.days1to30()).isEqualByComparingTo("0");
            assertThat(bucket.days31to60()).isEqualByComparingTo("0");
            assertThat(bucket.days61to90()).isEqualByComparingTo("0");
            assertThat(bucket.over90()).isEqualByComparingTo("0");
            assertThat(bucket.total()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("estructura del reporte")
    class Estructura {

        @Test
        @DisplayName("agrupa las filas por proveedor con su propio tramo")
        void agrupa_las_filas_por_proveedor() {
            AccountsPayableAgingDto.Bucket bucket = new AccountsPayableAgingDto.Bucket(
                    new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("100"));
            AccountsPayableAgingDto.SupplierRow fila = new AccountsPayableAgingDto.SupplierRow(7L,
                    "Distribuidora Sur", "800111222", bucket);

            AccountsPayableAgingDto dto = new AccountsPayableAgingDto(LocalDate.of(2026, 7, 31),
                    List.of(fila), bucket);

            assertThat(dto.suppliers()).containsExactly(fila);
            assertThat(dto.totals()).isEqualTo(bucket);
        }
    }
}
