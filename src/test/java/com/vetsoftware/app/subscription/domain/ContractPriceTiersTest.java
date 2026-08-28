package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La mitad «contrato» de D-66: la misma cuenta acumulativa que hace la
 * cotizacion, aplicada a las lineas que se firman.
 */
@DisplayName("ContractPriceTiers — reparto acumulativo en el contrato (D-66)")
class ContractPriceTiersTest {

    private static final ContractPriceTier TRAMO_BAJO = new ContractPriceTier(1, 8, 2,
            TaxTreatment.TAXED, new BigDecimal("12000.00"), new BigDecimal("19.00"));
    private static final ContractPriceTier TRAMO_ALTO = new ContractPriceTier(9, null, 0,
            TaxTreatment.TAXED, new BigDecimal("9000.00"), new BigDecimal("19.00"));
    private static final List<ContractPriceTier> ESCALERA = List.of(TRAMO_BAJO, TRAMO_ALTO);

    @Test
    @DisplayName("quince usuarios se firman como dos lineas de tramo que suman 141000")
    void quince_usuarios_se_firman_como_dos_lineas_de_tramo_que_suman_141000() {
        List<ContractTierLine> lineas = ContractPriceTiers.allocate(15, ESCALERA);

        assertThat(lineas).hasSize(2);
        assertThat(lineas.get(0).tier()).isEqualTo(TRAMO_BAJO);
        assertThat(lineas.get(1).tier()).isEqualTo(TRAMO_ALTO);

        BigDecimal cuota = lineas
                .stream().map(l -> SubscriptionItem.recurringSubtotalOf(l.quantity(),
                        l.includedQuantity(), l.tier().unitAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(cuota).isEqualByComparingTo("141000.00");
    }

    @Test
    @DisplayName("lo incluido viaja SOLO en el primer tramo: repetirlo lo regalaria una vez por "
            + "tramo")
    void lo_incluido_viaja_solo_en_el_primer_tramo() {
        List<ContractTierLine> lineas = ContractPriceTiers.allocate(15, ESCALERA);

        assertThat(lineas.get(0).includedQuantity()).isEqualTo(2);
        assertThat(lineas.get(0).quantity()).isEqualTo(10);
        assertThat(lineas.get(1).includedQuantity()).isZero();
        assertThat(lineas.get(1).quantity()).isEqualTo(5);
        // La suma de las cantidades devuelve exactamente lo contratado.
        assertThat(lineas.stream().mapToInt(ContractTierLine::quantity).sum()).isEqualTo(15);
    }

    @Test
    @DisplayName("cada linea factura las unidades de SU tramo, no lo contratado entero")
    void cada_linea_factura_las_unidades_de_su_tramo() {
        List<ContractTierLine> lineas = ContractPriceTiers.allocate(15, ESCALERA);

        assertThat(lineas.get(0).quantity() - lineas.get(0).includedQuantity()).isEqualTo(8);
        assertThat(lineas.get(1).quantity() - lineas.get(1).includedQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("si lo contratado no supera lo incluido se firma una sola linea sin nada que "
            + "cobrar, no cero lineas")
    void si_lo_contratado_no_supera_lo_incluido_se_firma_una_sola_linea() {
        List<ContractTierLine> lineas = ContractPriceTiers.allocate(2, ESCALERA);

        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0).quantity()).isEqualTo(2);
        assertThat(lineas.get(0).includedQuantity()).isEqualTo(2);
        assertThat(SubscriptionItem.recurringSubtotalOf(2, 2, new BigDecimal("12000.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("un articulo sin escalones produce una sola linea con toda la cantidad")
    void un_articulo_sin_escalones_produce_una_sola_linea() {
        ContractPriceTier unico = new ContractPriceTier(1, null, 0, TaxTreatment.TAXED,
                new BigDecimal("69000.00"), new BigDecimal("19.00"));

        List<ContractTierLine> lineas = ContractPriceTiers.allocate(1, List.of(unico));

        assertThat(lineas).hasSize(1);
        assertThat(lineas.get(0).quantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("una escalera que no arranca en uno se rechaza en vez de firmar de menos")
    void una_escalera_que_no_arranca_en_uno_se_rechaza() {
        assertThatThrownBy(() -> ContractPriceTiers.allocate(15, List.of(TRAMO_ALTO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start at 1");
    }

    @Test
    @DisplayName("sin ningun tramo no se firma: no hay precio que congelar")
    void sin_ningun_tramo_no_se_firma() {
        assertThatThrownBy(() -> ContractPriceTiers.allocate(15, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one price tier");
    }
}
