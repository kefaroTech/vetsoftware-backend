package com.vetsoftware.app.platformbillingconfig.infrastructure.persistence;

import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.CREADA;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.TARIFA;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.configurada;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.sinTarifa;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlatformBillingConfigJpaMapper — ida y vuelta fila ↔ agregado")
class PlatformBillingConfigJpaMapperTest {

    private final PlatformBillingConfigJpaMapper mapper = new PlatformBillingConfigJpaMapper();

    @Test
    @DisplayName("copia cada campo en su columna y aplana la tarifa a su id")
    void copia_cada_campo_en_su_columna() {
        PlatformBillingConfigJpaEntity entity = mapper.toJpa(configurada());

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getDefaultPriceListId()).isEqualTo(7L);
        assertThat(entity.getDefaultGraceDays()).isEqualTo(5);
        assertThat(entity.getDefaultTrialDays()).isEqualTo(14);
        assertThat(entity.getInvoiceDayOfMonth()).isEqualTo(1);
        assertThat(entity.getDefaultPaymentTermDays()).isEqualTo(5);
        assertThat(entity.getExternalBillingProvider()).isEqualTo("SIIGO");
        assertThat(entity.getCreatedDate()).isEqualTo(CREADA);
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    @DisplayName("toda entidad que sale del mapper lleva singleton = 1")
    void toda_entidad_que_sale_del_mapper_lleva_singleton_1() {
        assertThat(mapper.toJpa(configurada()).getSingleton()).isEqualTo((byte) 1);
        assertThat(mapper.toJpa(sinTarifa()).getSingleton()).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("sin tarifa por defecto deja la columna de la FK en null")
    void sin_tarifa_por_defecto_deja_la_columna_de_la_fk_en_null() {
        assertThat(mapper.toJpa(sinTarifa()).getDefaultPriceListId()).isNull();
    }

    @Test
    @DisplayName("reconstruye el agregado sin perder ningún campo")
    void reconstruye_el_agregado_sin_perder_ningun_campo() {
        PlatformBillingConfig ida = configurada();

        PlatformBillingConfig vuelta = mapper.toDomain(mapper.toJpa(ida), TARIFA);

        assertThat(vuelta.getId()).isEqualTo(ida.getId());
        assertThat(vuelta.getDefaultPriceList()).isEqualTo(TARIFA);
        assertThat(vuelta.getDefaultGraceDays()).isEqualTo(ida.getDefaultGraceDays());
        assertThat(vuelta.getDefaultTrialDays()).isEqualTo(ida.getDefaultTrialDays());
        assertThat(vuelta.getInvoiceDayOfMonth()).isEqualTo(ida.getInvoiceDayOfMonth());
        assertThat(vuelta.getDefaultPaymentTermDays()).isEqualTo(ida.getDefaultPaymentTermDays());
        assertThat(vuelta.getExternalBillingProvider()).isEqualTo(ida.getExternalBillingProvider());
        assertThat(vuelta.getCreatedDate()).isEqualTo(ida.getCreatedDate());
        assertThat(vuelta.getVersion()).isEqualTo(ida.getVersion());
    }

    @Test
    @DisplayName("acepta que la tarifa no se pueda resolver y deja el agregado sin ella")
    void acepta_que_la_tarifa_no_se_pueda_resolver() {
        PlatformBillingConfig vuelta = mapper.toDomain(mapper.toJpa(configurada()), null);

        assertThat(vuelta.getDefaultPriceList()).isNull();
        assertThat(vuelta.getDefaultGraceDays()).isEqualTo(5);
    }
}
