package com.vetsoftware.app.quote.infrastructure.persistence;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.borrador;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.borradorDeProspecto;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.lineaModulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioConIncluidas;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioGravado;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.quote.domain.CompanyRef;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteItemType;
import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.domain.QuoteSummary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuoteJpaMapper: ida y vuelta dominio <-> entidad")
class QuoteJpaMapperTest {

    @Mock
    private CompanyJpaEntity companyEntity;

    private final QuoteJpaMapper mapper = new QuoteJpaMapper();

    @BeforeEach
    void stubsComunes() {
        // toJpa() solo asigna la referencia; toDomain() del camino de lectura si lee
        // estos getters. lenient() evita el fallo de STRICT_STUBS en los tests que
        // solo ejercitan una de las dos direcciones.
        lenient().when(companyEntity.getId()).thenReturn(42L);
        lenient().when(companyEntity.getName()).thenReturn("Clinica Norte");
        lenient().when(companyEntity.getIdentifier()).thenReturn("900123456");
    }

    @Nested
    @DisplayName("Ida: dominio a entidad")
    class Ida {

        @Test
        @DisplayName("copia la cabecera con sus cuatro totales guardados")
        void copia_la_cabecera() {
            QuoteJpaEntity entity = mapper.toJpa(borrador(), companyEntity);

            assertThat(entity.getQuoteNumber()).isEqualTo("COT-2026-00184");
            assertThat(entity.getStatus()).isEqualTo(QuoteStatus.DRAFT);
            assertThat(entity.getSubtotalAmount()).isEqualByComparingTo("100000.00");
            assertThat(entity.getTaxAmount()).isEqualByComparingTo("19000.00");
            assertThat(entity.getTotalAmount()).isEqualByComparingTo("119000.00");
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("una oferta a prospecto viaja sin empresa, que es un valor legitimo")
        void un_prospecto_viaja_sin_empresa() {
            QuoteJpaEntity entity = mapper.toJpa(borradorDeProspecto(), null);

            assertThat(entity.getCompany()).isNull();
            assertThat(entity.getProspectName()).isEqualTo("Veterinaria del Sur");
            assertThat(entity.getTrialDays()).isEqualTo(15);
        }

        @Test
        @DisplayName("copia las tres cantidades de la linea, no solo la que se cobra")
        void copia_las_tres_cantidades() {
            QuoteLine capacidad = QuoteLine.freeze(1, usuarioExtra(),
                    precioConIncluidas("12000.00", 2), 5, BigDecimal.ZERO, AHORA);

            QuoteJpaEntity entity = mapper.toJpa(borrador(List.of(capacidad)), companyEntity);

            assertThat(entity.getLines()).singleElement().satisfies(linea -> {
                assertThat(linea.getContractedQuantity()).isEqualTo(5);
                assertThat(linea.getIncludedQuantity()).isEqualTo(2);
                assertThat(linea.getQuantity()).isEqualTo(3);
            });
        }
    }

    @Nested
    @DisplayName("Vuelta: entidad a dominio")
    class Vuelta {

        @Test
        @DisplayName("reconstruye la cotizacion entera y sus totales siguen cuadrando")
        void reconstruye_la_cotizacion_entera() {
            QuoteJpaEntity entity = mapper.toJpa(borrador(), companyEntity);

            Quote quote = mapper.toDomain(entity);

            assertThat(quote.getQuoteNumber()).isEqualTo("COT-2026-00184");
            assertThat(quote.getCompany())
                    .isEqualTo(new CompanyRef(42L, "Clinica Norte", "900123456"));
            assertThat(quote.getTotalAmount()).isEqualByComparingTo("119000.00");
            assertThat(quote.getLines()).singleElement().satisfies(
                    linea -> assertThat(linea.getItemType()).isEqualTo(QuoteItemType.MODULE));
        }

        @Test
        @DisplayName("una entidad sin empresa vuelve al dominio con company nulo")
        void sin_empresa_vuelve_con_company_nulo() {
            QuoteJpaEntity entity = mapper.toJpa(borradorDeProspecto(), null);

            Quote quote = mapper.toDomain(entity);

            assertThat(quote.getCompany()).isNull();
            assertThat(quote.getCompanyId()).isNull();
            assertThat(quote.getProspectEmail()).isEqualTo("ana@ejemplo.com");
        }

        @Test
        @DisplayName("ordena las lineas por line_number: el orden impreso es un dato, no un azar")
        void ordena_las_lineas_por_numero() {
            QuoteLine primera = QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1,
                    BigDecimal.ZERO, AHORA);
            QuoteLine segunda = QuoteLine.freeze(2, usuarioExtra(), precioGravado("12000.00"), 1,
                    BigDecimal.ZERO, AHORA);
            QuoteJpaEntity entity = mapper.toJpa(borrador(List.of(segunda, primera)),
                    companyEntity);

            Quote quote = mapper.toDomain(entity);

            assertThat(quote.getLines()).extracting(QuoteLine::getLineNumber).containsExactly(1, 2);
        }

        @Test
        @DisplayName("el camino de escritura reusa el ref precargado sin tocar el proxy")
        void el_camino_de_escritura_reusa_el_ref() {
            QuoteJpaEntity entity = mapper.toJpa(borrador(), companyEntity);
            CompanyRef precargado = new CompanyRef(42L, "Clinica Norte", "900123456");

            Quote quote = mapper.toDomain(entity, precargado);

            assertThat(quote.getCompany()).isSameAs(precargado);
        }
    }

    @Nested
    @DisplayName("Proyeccion de listado")
    class Proyeccion {

        @Test
        @DisplayName("el resumen lleva los totales guardados y no toca las colecciones")
        void el_resumen_lleva_los_totales() {
            QuoteJpaEntity entity = mapper.toJpa(borrador(List.of(lineaModulo())), companyEntity);

            QuoteSummary summary = mapper.toSummary(entity);

            assertThat(summary.quoteNumber()).isEqualTo("COT-2026-00184");
            assertThat(summary.totalAmount()).isEqualByComparingTo("119000.00");
            assertThat(summary.status()).isEqualTo(QuoteStatus.DRAFT);
            assertThat(summary.company().identifier()).isEqualTo("900123456");
        }

        @Test
        @DisplayName("el resumen de un prospecto no tiene empresa y no revienta")
        void el_resumen_de_un_prospecto_no_revienta() {
            QuoteJpaEntity entity = mapper.toJpa(borradorDeProspecto(), null);

            QuoteSummary summary = mapper.toSummary(entity);

            assertThat(summary.company()).isNull();
            assertThat(summary.prospectName()).isEqualTo("Veterinaria del Sur");
        }
    }
}
