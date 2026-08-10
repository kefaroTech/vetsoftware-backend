package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productchargeopenaccount.application.command.UpdateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProductChargeOpenAccountService")
class UpdateProductChargeOpenAccountServiceTest {

    @Mock
    private ProductChargeOpenAccountRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ProductQueryPort productQueryPort;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private UpdateProductChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<ProductChargeOpenAccount> cargoCaptor;

    private void referenciasResueltas() {
        when(openAccountQueryPort.findById(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
        when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.ANIMAL));
        when(productQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.PRODUCTO.id(),
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.PRODUCTO));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda el cargo con las referencias resueltas y sin recalcular el dinero")
        void guarda_el_cargo_con_las_referencias_resueltas() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            referenciasResueltas();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoActualizar());

            verify(repository).save(cargoCaptor.capture());
            ProductChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getAnimal()).isEqualTo(ProductChargeOpenAccountMother.ANIMAL);
            assertThat(guardado.getProduct()).isEqualTo(ProductChargeOpenAccountMother.PRODUCTO);
            assertThat(guardado.getOpenAccount()).isEqualTo(ProductChargeOpenAccountMother.CUENTA);
            // El precio y el desglose se congelaron al crear: editar no los recalcula.
            assertThat(guardado.getTotalAmount()).isEqualByComparingTo("11900.00");
        }

        @Test
        @DisplayName("devuelve el DTO del cargo persistido")
        void devuelve_el_dto_del_cargo_persistido() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            referenciasResueltas();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            ProductChargeOpenAccountDto dto = service
                    .execute(ProductChargeOpenAccountMother.comandoActualizar());

            assertThat(dto.id()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
        }

        @Test
        @DisplayName("recalcula solo la cuenta destino cuando el cargo no se mueve")
        void recalcula_solo_la_cuenta_destino() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            referenciasResueltas();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoActualizar());

            verify(refresher).refresh(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
            verify(refresher, never()).refresh(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OTRA_CUENTA_ID);
        }

        @Test
        @DisplayName("al mover el cargo de cuenta recalcula la vieja y la nueva")
        void al_mover_el_cargo_recalcula_las_dos_cuentas() {
            // El cargo vivia en OTRA_CUENTA y el comando lo lleva a OPEN_ACCOUNT_ID.
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargoEnOtraCuenta()));
            referenciasResueltas();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoActualizar());

            verify(refresher).refresh(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
            verify(refresher).refresh(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OTRA_CUENTA_ID);
        }

        @Test
        @DisplayName("valida la version esperada de la cuenta destino")
        void valida_la_version_esperada() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            referenciasResueltas();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(new UpdateProductChargeOpenAccountCommand(
                    ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.PRODUCTO.id(),
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID, 7L));

            verify(versionGuard).assertVersion(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID, 7L);
        }
    }

    @Nested
    @DisplayName("la operacion se rechaza y no escribe nada")
    class Rechazos {

        @Test
        @DisplayName("cargo de otra empresa o inexistente")
        void cargo_inexistente() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoActualizar()))
                    .isInstanceOf(ProductChargeOpenAccountNotFoundException.class)
                    .hasMessageContaining("ProductChargeOpenAccount not found: "
                            + ProductChargeOpenAccountMother.CHARGE_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(openAccountQueryPort, animalQueryPort, productQueryPort, refresher,
                    versionGuard);
        }

        @Test
        @DisplayName("cuenta destino inexistente")
        void cuenta_destino_inexistente() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            when(openAccountQueryPort.findById(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: "
                            + ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, productQueryPort, refresher, versionGuard);
        }

        @Test
        @DisplayName("cuenta destino de otra empresa: aislamiento por tenant")
        void cuenta_destino_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            when(openAccountQueryPort.findById(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA_AJENA));

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("open account does not belong to company");

            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, productQueryPort, refresher, versionGuard);
        }

        @Test
        @DisplayName("animal inexistente")
        void animal_inexistente() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            when(openAccountQueryPort.findById(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Animal not found: " + ProductChargeOpenAccountMother.ANIMAL.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(productQueryPort, refresher);
        }

        @Test
        @DisplayName("producto inexistente")
        void producto_inexistente() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));
            when(openAccountQueryPort.findById(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.ANIMAL));
            when(productQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.PRODUCTO.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Product not found: " + ProductChargeOpenAccountMother.PRODUCTO.id());

            verify(repository, never()).save(any());
            verify(refresher, never()).refresh(anyLong(), anyLong());
        }
    }
}
