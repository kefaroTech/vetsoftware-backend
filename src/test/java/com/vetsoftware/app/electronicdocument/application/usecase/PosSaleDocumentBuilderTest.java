package com.vetsoftware.app.electronicdocument.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SalePayment;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.port.out.BranchResolverPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort.CatalogItem;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort.CompanyFiscalProfile;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleCustomerQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.ApplicationType;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.PromotionType;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.SalePromotion;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.ValueType;
import com.vetsoftware.app.electronicdocument.application.port.out.UvtQueryPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.infrastructure.config.ClockConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Construcción del documento desde una venta de POS. Aquí se decide cuánto se
 * transmite a la DIAN a partir de un payload que viene del navegador, así que
 * se verifica lo que evita un fraude de precio (el servidor recalcula contra el
 * catálogo) y la matemática de extracción de base por tarifa.
 */
@ExtendWith(MockitoExtension.class)
class PosSaleDocumentBuilderTest {

    private static final Long COMPANY = 9L;
    private static final BigDecimal UVT = new BigDecimal("49799");

    @Mock
    private CompanyFiscalProfileQueryPort fiscalProfileQueryPort;
    @Mock
    private SaleCustomerQueryPort saleCustomerQueryPort;
    @Mock
    private CatalogLineQueryPort catalogLineQueryPort;
    @Mock
    private SalePromotionQueryPort salePromotionQueryPort;
    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private UvtQueryPort uvtQueryPort;
    @Mock
    private BranchResolverPort branchResolverPort;

    private PosSaleDocumentBuilder builder;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void setUp() {
        PosTicketLimitValidator limitValidator = new PosTicketLimitValidator(uvtQueryPort);
        builder = new PosSaleDocumentBuilder(fiscalProfileQueryPort, saleCustomerQueryPort,
                catalogLineQueryPort, salePromotionQueryPort, repository, limitValidator,
                uvtQueryPort, branchResolverPort, Clock.system(ClockConfig.BUSINESS_ZONE));

        lenient().when(fiscalProfileQueryPort.findByCompany(COMPANY))
                .thenReturn(Optional.of(new CompanyFiscalProfile(issuer(), null, null, null)));
        lenient().when(salePromotionQueryPort.findActive(anyLong(), any())).thenReturn(List.of());
        lenient().when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(UVT));
        lenient().when(branchResolverPort.resolve(anyLong(), any())).thenReturn(Optional.of(7L));
        lenient().when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static IssuerSnapshot issuer() {
        return new IssuerSnapshot("NIT", "900123456", "7", "Vet SAS", "RESPONSABLE", "vet@x.co",
                List.of("O-13"));
    }

    private static CatalogItem gravado19(String basePrice) {
        return new CatalogItem("Alimento premium", TaxCategory.GRAVADO, TaxScheme.IVA, bd("19"),
                bd(basePrice), 30L);
    }

    private RegisterPosSaleCommand sale(List<SaleLine> lines, List<SalePayment> payments) {
        return new RegisterPosSaleCommand(COMPANY, ElectronicDocumentType.FE_VENTA, true, null,
                lines, payments, "req-1", 4L, null);
    }

    private ElectronicDocument buildProductSale(String catalogPrice, String clientPrice,
            String quantity) {
        when(catalogLineQueryPort.findProduct(1L, COMPANY))
                .thenReturn(Optional.of(gravado19(catalogPrice)));
        BigDecimal total = bd(clientPrice).multiply(bd(quantity)).setScale(2,
                java.math.RoundingMode.HALF_UP);
        return builder
                .build(sale(
                        List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, bd(quantity),
                                bd(clientPrice))),
                        List.of(new SalePayment(PaymentMeans.EFECTIVO, total))));
    }

    @Nested
    class ExtraccionDeBase {

        @Test
        void separa_base_e_iva_de_un_precio_con_impuesto_incluido() {
            ElectronicDocument doc = buildProductSale("119000", "119000", "1");

            ElectronicDocumentLine line = doc.getLines().getFirst();
            assertThat(line.getLineExtensionAmount()).isEqualByComparingTo("100000.00");
            assertThat(line.getTaxAmount()).isEqualByComparingTo("19000.00");
            assertThat(line.getTotalAmount()).isEqualByComparingTo("119000.00");
        }

        @Test
        void base_mas_iva_siempre_reconstruye_el_total_de_la_linea() {
            ElectronicDocument doc = buildProductSale("77777", "77777", "3");

            ElectronicDocumentLine line = doc.getLines().getFirst();
            assertThat(line.getLineExtensionAmount().add(line.getTaxAmount()))
                    .isEqualByComparingTo(line.getTotalAmount());
        }

        @Test
        void multiplica_por_la_cantidad_antes_de_extraer_la_base() {
            ElectronicDocument doc = buildProductSale("119000", "119000", "3");

            ElectronicDocumentLine line = doc.getLines().getFirst();
            assertThat(line.getTotalAmount()).isEqualByComparingTo("357000.00");
            assertThat(line.getLineExtensionAmount()).isEqualByComparingTo("300000.00");
        }

        @Test
        void una_linea_exenta_conserva_esquema_iva_con_tarifa_cero_y_sin_impuesto() {
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(new CatalogItem("Medicamento exento",
                            TaxCategory.EXENTO, TaxScheme.IVA, BigDecimal.ZERO, bd("40000"), 30L)));

            ElectronicDocument doc = builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("40000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("40000.00")))));

            ElectronicDocumentLine line = doc.getLines().getFirst();
            assertThat(line.getTaxScheme()).as("EXENTO debe distinguirse de EXCLUIDO en el XML")
                    .isEqualTo(TaxScheme.IVA);
            assertThat(line.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(line.getLineExtensionAmount()).isEqualByComparingTo("40000.00");
        }

        @Test
        void una_linea_excluida_viaja_sin_esquema_tributario() {
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(new CatalogItem("Producto excluido",
                            TaxCategory.EXCLUIDO, null, null, bd("40000"), 30L)));

            ElectronicDocument doc = builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("40000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("40000.00")))));

            ElectronicDocumentLine line = doc.getLines().getFirst();
            assertThat(line.getTaxScheme()).isNull();
            assertThat(line.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void una_linea_de_servicio_del_catalogo_extrae_base_e_iva_igual_que_un_producto() {
            when(catalogLineQueryPort.findService(2L, COMPANY))
                    .thenReturn(Optional.of(new CatalogItem("Consulta general", TaxCategory.GRAVADO,
                            TaxScheme.IVA, bd("19"), bd("119000"), 30L)));

            ElectronicDocument doc = builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.SERVICE, 2L, null, BigDecimal.ONE,
                            bd("119000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("119000.00")))));

            ElectronicDocumentLine line = doc.getLines().getFirst();
            assertThat(line.getDescription()).isEqualTo("Consulta general");
            assertThat(line.getLineExtensionAmount()).isEqualByComparingTo("100000.00");
            assertThat(line.getTaxAmount()).isEqualByComparingTo("19000.00");
        }

        @Test
        void una_linea_general_del_pos_es_excluida_y_sin_esquema() {
            ElectronicDocument doc = builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "Cargo libre", BigDecimal.ONE,
                            bd("15000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("15000.00")))));

            ElectronicDocumentLine line = doc.getLines().getFirst();
            assertThat(line.getTaxCategory()).isEqualTo(TaxCategory.EXCLUIDO);
            assertThat(line.getTaxScheme()).isNull();
            assertThat(line.getDescription()).isEqualTo("Cargo libre");
        }
    }

    @Nested
    class ElServidorEsLaAutoridadDelPrecio {

        @Test
        void rechaza_un_precio_por_debajo_del_catalogo() {
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(gravado19("119000")));

            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no coincide con el catalogo");

            verify(repository, never()).save(any());
        }

        @Test
        void rechaza_un_precio_por_encima_del_catalogo() {
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(gravado19("119000")));

            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("500000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("500000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void tolera_una_desviacion_de_un_peso_por_el_redondeo_del_front() {
            ElectronicDocument doc = buildProductSale("119000", "118999", "1");

            assertThat(doc.getPayableAmount()).isEqualByComparingTo("118999.00");
        }

        @Test
        void acepta_el_precio_con_la_promocion_activa_aplicada() {
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(gravado19("119000")));
            when(salePromotionQueryPort.findActive(anyLong(), any()))
                    .thenReturn(List.of(new SalePromotion(PromotionType.DISCOUNT,
                            ApplicationType.PRODUCT, 1L, ValueType.PERCENTAGE, bd("20"))));

            ElectronicDocument doc = builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("95200"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("95200.00")))));

            assertThat(doc.getPayableAmount()).isEqualByComparingTo("95200.00");
        }

        @Test
        void rechaza_el_precio_de_lista_cuando_hay_una_promocion_que_lo_baja() {
            // El cliente no puede "renunciar" a la promo para inflar el documento.
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(gravado19("119000")));
            when(salePromotionQueryPort.findActive(anyLong(), any()))
                    .thenReturn(List.of(new SalePromotion(PromotionType.DISCOUNT,
                            ApplicationType.PRODUCT, 1L, ValueType.PERCENTAGE, bd("20"))));

            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("119000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("119000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void un_producto_de_otra_empresa_no_se_puede_vender() {
            when(catalogLineQueryPort.findProduct(1L, COMPANY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no pertenece a la empresa");
        }

        @Test
        void un_adquiriente_identificado_que_no_existe_rechaza_la_venta() {
            when(saleCustomerQueryPort.findOwner(55L, COMPANY)).thenReturn(Optional.empty());
            RegisterPosSaleCommand command = new RegisterPosSaleCommand(COMPANY,
                    ElectronicDocumentType.FE_VENTA, false, 55L,
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00"))), "req-1", 4L,
                    null);

            assertThatThrownBy(() -> builder.build(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Adquiriente (owner) no encontrado");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class ValidacionDeLinea {

        @Test
        void rechaza_cantidad_cero_o_negativa() {
            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", BigDecimal.ZERO,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("0.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be > 0");

            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", bd("-1"), bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("0.00"))))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rechaza_precio_unitario_negativo() {
            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", BigDecimal.ONE,
                            bd("-1"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("-1.00"))))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unitPrice");
        }

        @Test
        void una_linea_de_catalogo_exige_refId() {
            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.PRODUCT, null, null, BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires refId");
        }

        @Test
        void una_linea_general_exige_descripcion() {
            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "  ", BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires a description");
        }

        @Test
        void numera_las_lineas_de_forma_consecutiva_desde_uno() {
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(gravado19("119000")));

            ElectronicDocument doc = builder.build(sale(List.of(
                    new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE, bd("119000")),
                    new SaleLine(SaleLineKind.GENERAL, null, "Cargo", BigDecimal.ONE, bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("120000.00")))));

            assertThat(doc.getLines()).extracting(ElectronicDocumentLine::getLineNumber)
                    .containsExactly(1, 2);
        }
    }

    @Nested
    class ContextoFiscal {

        @Test
        void sin_perfil_fiscal_la_empresa_no_puede_vender() {
            when(fiscalProfileQueryPort.findByCompany(COMPANY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("perfil fiscal");
        }

        @Test
        void sin_sucursal_resoluble_la_venta_se_rechaza() {
            when(branchResolverPort.resolve(anyLong(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no tiene sucursal");
        }

        @Test
        void el_consumidor_final_nunca_es_agente_retenedor() {
            ElectronicDocument doc = builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", BigDecimal.ONE,
                            bd("10000000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("10000000.00")))));

            assertThat(doc.getReteFuenteAmount()).isEqualByComparingTo("0");
            assertThat(doc.getReteIvaAmount()).isEqualByComparingTo("0");
            assertThat(doc.getReteIcaAmount()).isEqualByComparingTo("0");
        }

        @Test
        void un_adquiriente_identificado_agente_retenedor_si_genera_retencion() {
            when(fiscalProfileQueryPort.findByCompany(COMPANY)).thenReturn(
                    Optional.of(new CompanyFiscalProfile(issuer(), bd("4"), bd("15"), bd("9.66"))));
            when(catalogLineQueryPort.findProduct(1L, COMPANY))
                    .thenReturn(Optional.of(gravado19("1190000")));
            when(saleCustomerQueryPort.findOwner(55L, COMPANY))
                    .thenReturn(Optional.of(new SaleCustomerQueryPort.SaleCustomer(
                            com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot
                                    .finalConsumer(),
                            true)));

            RegisterPosSaleCommand command = new RegisterPosSaleCommand(COMPANY,
                    ElectronicDocumentType.FE_VENTA, false, 55L,
                    List.of(new SaleLine(SaleLineKind.PRODUCT, 1L, null, BigDecimal.ONE,
                            bd("1190000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1190000.00"))), "req-1", 4L,
                    null);

            ElectronicDocument doc = builder.build(command);

            assertThat(doc.getReteFuenteAmount()).isEqualByComparingTo("40000.00");
            assertThat(doc.getReteIvaAmount()).isEqualByComparingTo("28500.00");
        }

        @Test
        void persiste_el_documento_con_la_sede_resuelta_y_el_actor() {
            when(branchResolverPort.resolve(COMPANY, null)).thenReturn(Optional.of(42L));

            builder.build(sale(
                    List.of(new SaleLine(SaleLineKind.GENERAL, null, "x", BigDecimal.ONE,
                            bd("1000"))),
                    List.of(new SalePayment(PaymentMeans.EFECTIVO, bd("1000.00")))));

            ArgumentCaptor<ElectronicDocument> captor = ArgumentCaptor
                    .forClass(ElectronicDocument.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getBranchId()).isEqualTo(42L);
            assertThat(captor.getValue().getIssuedByEmployeeId()).isEqualTo(4L);
            assertThat(captor.getValue().getClientRequestId()).isEqualTo("req-1");
        }
    }
}
