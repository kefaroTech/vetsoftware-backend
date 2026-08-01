package com.vetsoftware.app.electronicdocument.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.UvtQueryPort;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tope de 5 UVT del documento equivalente POS (Res. DIAN 000165/2023). Es el enforcement de backend
 * frente al gating del front: por encima del tope hay que emitir factura electrónica con adquiriente
 * identificado. Un fallo aquí produce documentos que la DIAN rechaza o sanciona.
 */
@ExtendWith(MockitoExtension.class)
class PosTicketLimitValidatorTest {

    private static final BigDecimal UVT = new BigDecimal("49799");
    /** 5 · UVT = 248.995 */
    private static final BigDecimal LIMIT = UVT.multiply(BigDecimal.valueOf(5));

    @Mock private UvtQueryPort uvtQueryPort;
    @InjectMocks private PosTicketLimitValidator validator;

    private static ElectronicDocument document(ElectronicDocumentType type, BigDecimal total) {
        BigDecimal base = total.divide(new BigDecimal("1.19"), 2, java.math.RoundingMode.HALF_UP);
        ElectronicDocumentLine line = new ElectronicDocumentLine(null, 1, "Venta", BigDecimal.ONE, "94",
                total, base, TaxCategory.GRAVADO, TaxScheme.IVA, new BigDecimal("19"),
                total.subtract(base), total);
        return ElectronicDocument.createPending(9L, null, type,
                new IssuerSnapshot("NIT", "900123456", "7", "Vet SAS", "RESPONSABLE", "vet@x.co", List.of()),
                CustomerSnapshot.finalConsumer(), List.of(line), List.of(), PaymentForm.CONTADO,
                false, null, null, null, null, null, 4L, 7L);
    }

    @Test
    void un_tiquete_pos_por_encima_de_5_uvt_se_rechaza() {
        when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(UVT));

        assertThatThrownBy(() -> validator.validate(
                document(ElectronicDocumentType.DOC_EQUIV_POS, LIMIT.add(BigDecimal.ONE))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supera el límite de 5 UVT")
                .hasMessageContaining("FE_VENTA");
    }

    @Test
    void un_tiquete_pos_exactamente_en_el_limite_se_acepta() {
        when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(UVT));

        assertThatCode(() -> validator.validate(
                document(ElectronicDocumentType.DOC_EQUIV_POS, LIMIT)))
                .doesNotThrowAnyException();
    }

    @Test
    void un_tiquete_pos_por_debajo_del_limite_se_acepta() {
        when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(UVT));

        assertThatCode(() -> validator.validate(
                document(ElectronicDocumentType.DOC_EQUIV_POS, new BigDecimal("100000"))))
                .doesNotThrowAnyException();
    }

    @Test
    void una_factura_electronica_no_tiene_tope_y_ni_siquiera_consulta_el_uvt() {
        assertThatCode(() -> validator.validate(
                document(ElectronicDocumentType.FE_VENTA, new BigDecimal("99000000"))))
                .doesNotThrowAnyException();

        org.mockito.Mockito.verify(uvtQueryPort, org.mockito.Mockito.never()).currentUvt();
    }

    @Test
    void sin_uvt_configurado_no_se_puede_validar_y_falla_explicitamente() {
        when(uvtQueryPort.currentUvt()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(
                document(ElectronicDocumentType.DOC_EQUIV_POS, new BigDecimal("1000"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system_configurations");
    }

    @Test
    void el_limite_sigue_al_uvt_configurado() {
        // Si el UVT sube, el mismo ticket que antes fallaba pasa a ser válido: la regla no está hardcodeada.
        BigDecimal ticket = new BigDecimal("250000");
        when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(UVT));
        assertThatThrownBy(() -> validator.validate(
                document(ElectronicDocumentType.DOC_EQUIV_POS, ticket)))
                .isInstanceOf(IllegalArgumentException.class);

        when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(new BigDecimal("60000")));
        assertThatCode(() -> validator.validate(
                document(ElectronicDocumentType.DOC_EQUIV_POS, ticket)))
                .doesNotThrowAnyException();

        assertThat(LIMIT).isEqualByComparingTo("248995");
    }
}
