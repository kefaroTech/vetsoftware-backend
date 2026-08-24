package com.vetsoftware.app.entitlement.application.usecase;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contadorExistente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityLimitExceededException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityNotFoundException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityUnderflowException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdjustCompanyCapacityUsageService — mover el consumo sin carreras")
class AdjustCompanyCapacityUsageServiceTest {

    @Mock
    private CompanyCapacityRepository repository;
    @InjectMocks
    private AdjustCompanyCapacityUsageService service;

    private static AdjustCompanyCapacityUsageCommand alta() {
        return new AdjustCompanyCapacityUsageCommand(COMPANY_ID, CapacityUnit.USER, 1);
    }

    @Test
    @DisplayName("devuelve el contador ya movido")
    void devuelve_el_contador_ya_movido() {
        when(repository.addUsage(COMPANY_ID, CapacityUnit.USER, 1)).thenReturn(1);
        when(repository.findByCompanyIdAndUnit(COMPANY_ID, CapacityUnit.USER))
                .thenReturn(Optional.of(new CompanyCapacity(31L, COMPANY_ID, CapacityUnit.USER, 5,
                        3, SUBSCRIPTION_ID, AHORA, AHORA.minusDays(90))));

        CompanyCapacityDto contador = service.execute(alta());

        assertThat(contador.usedQuantity()).isEqualTo(3);
        assertThat(contador.exhausted()).isFalse();
    }

    @Test
    @DisplayName("si la empresa no tiene contratada esa unidad, lo dice con nombre y apellido")
    void sin_contador_lo_dice_con_nombre_y_apellido() {
        when(repository.addUsage(COMPANY_ID, CapacityUnit.USER, 1)).thenReturn(0);
        when(repository.findByCompanyIdAndUnit(COMPANY_ID, CapacityUnit.USER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(alta()))
                .isInstanceOf(CompanyCapacityNotFoundException.class).hasMessageContaining("USER");
    }

    @Test
    @DisplayName("un descuento que dejaria el consumo en negativo se rechaza")
    void un_descuento_que_dejaria_negativo_se_rechaza() {
        AdjustCompanyCapacityUsageCommand baja = new AdjustCompanyCapacityUsageCommand(COMPANY_ID,
                CapacityUnit.USER, -4);
        when(repository.addUsage(COMPANY_ID, CapacityUnit.USER, -4)).thenReturn(0);
        when(repository.findByCompanyIdAndUnit(COMPANY_ID, CapacityUnit.USER))
                .thenReturn(Optional.of(contadorExistente(31L, CapacityUnit.USER, 5, 1)));

        assertThatThrownBy(() -> service.execute(baja))
                .isInstanceOf(CompanyCapacityUnderflowException.class)
                .hasMessageContaining("below zero").hasMessageContaining("used 1")
                .hasMessageContaining("requested delta -4");
    }

    @Test
    @DisplayName("un alta que supera el límite contratado informa límite, uso y delta")
    void un_alta_que_supera_el_limite_contratado_informa_el_detalle() {
        AdjustCompanyCapacityUsageCommand altaDeDos = new AdjustCompanyCapacityUsageCommand(
                COMPANY_ID, CapacityUnit.USER, 2);
        when(repository.addUsage(COMPANY_ID, CapacityUnit.USER, 2)).thenReturn(0);
        when(repository.findByCompanyIdAndUnit(COMPANY_ID, CapacityUnit.USER))
                .thenReturn(Optional.of(contadorExistente(31L, CapacityUnit.USER, 5, 4)));

        assertThatThrownBy(() -> service.execute(altaDeDos))
                .isInstanceOf(CompanyCapacityLimitExceededException.class)
                .hasMessageContaining("limit 5").hasMessageContaining("used 4")
                .hasMessageContaining("requested delta 2");
    }

    @Test
    @DisplayName("un movimiento de cero no llega ni al repositorio")
    void un_movimiento_de_cero_no_llega_al_repositorio() {
        assertThatThrownBy(
                () -> new AdjustCompanyCapacityUsageCommand(COMPANY_ID, CapacityUnit.USER, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("delta");
    }
}
