package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanyEntitlementJpaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El derecho a facturar sale ahora del contrato, no del plan: un solo lookup
 * sobre {@code company_entitlements} por {@code (companyId, subModuleCode)}.
 *
 * <p>
 * El puerto devuelve un {@code long} y no un {@code boolean} a proposito
 * ({@code PROYECCION_SIN_LITERAL_BOOLEANO}, #196), asi que estos tests stubean
 * conteos, no banderas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBillingEntitlementQueryPort — resuelve el derecho a facturacion electronica")
class JpaBillingEntitlementQueryPortTest {

    private static final List<String> SOLO_FULL = List.of("FULL");

    @Mock
    private CompanyEntitlementJpaRepository companyEntitlementJpaRepository;

    private JpaBillingEntitlementQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaBillingEntitlementQueryPort(companyEntitlementJpaRepository);
    }

    @Test
    @DisplayName("companyId null nunca esta habilitada, sin consultar nada")
    void company_id_null_nunca_esta_habilitada() {
        assertThat(port.isElectronicInvoicingEnabled(null)).isFalse();

        verifyNoInteractions(companyEntitlementJpaRepository);
    }

    @Nested
    @DisplayName("segun lo que el contrato de la empresa conceda sobre BILLING")
    class SegunElContrato {

        @Test
        @DisplayName("con BILLING concedido en nivel FULL, esta habilitada")
        void con_billing_concedido_en_full() {
            when(companyEntitlementJpaRepository.countGrantedByCompanyIdAndSubModuleCode(9L,
                    "BILLING", SOLO_FULL)).thenReturn(1L);

            assertThat(port.isElectronicInvoicingEnabled(9L)).isTrue();
        }

        /**
         * Cubre a la vez la empresa sin contrato, la que nunca compro facturacion y la
         * que la dio de baja: en las tres el conteo es cero, que es exactamente lo que
         * el modelo quiere decir con «este submodulo no existe para esta empresa».
         */
        @Test
        @DisplayName("sin ninguna concesion FULL sobre BILLING, no esta habilitada")
        void sin_concesion_full_sobre_billing() {
            when(companyEntitlementJpaRepository.countGrantedByCompanyIdAndSubModuleCode(9L,
                    "BILLING", SOLO_FULL)).thenReturn(0L);

            assertThat(port.isElectronicInvoicingEnabled(9L)).isFalse();
        }

        /**
         * READ_ONLY es consulta e impresion, no crear. Emitir ante la DIAN es crear, y
         * por eso el nivel solo lectura no entra en la consulta: si entrara, un modulo
         * de facturacion dado de baja seguiria emitiendo documentos fiscales.
         */
        @Test
        @DisplayName("el nivel READ_ONLY no cuenta: la consulta solo pregunta por FULL")
        void el_nivel_read_only_no_cuenta() {
            when(companyEntitlementJpaRepository.countGrantedByCompanyIdAndSubModuleCode(9L,
                    "BILLING", SOLO_FULL)).thenReturn(0L);

            assertThat(port.isElectronicInvoicingEnabled(9L)).isFalse();

            // El stub es estricto: si el adaptador consultara con otra lista de niveles
            // —incluyendo READ_ONLY— Mockito fallaria por argumentos no coincidentes.
        }
    }
}
