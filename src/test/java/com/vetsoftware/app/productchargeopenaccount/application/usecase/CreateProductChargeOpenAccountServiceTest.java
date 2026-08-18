package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productchargeopenaccount.application.command.CreateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.BranchResolverPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductChargeOpenAccountService")
class CreateProductChargeOpenAccountServiceTest {

    @Mock
    private ProductChargeOpenAccountRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ProductQueryPort productQueryPort;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;
    @Mock
    private InventoryLedgerPort inventoryLedger;
    @Mock
    private BranchResolverPort branchResolver;

    @InjectMocks
    private CreateProductChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<ProductChargeOpenAccount> cargoCaptor;

    /** Deja la cuenta abierta, propia, y todas las referencias resueltas. */
    private void todoResuelto() {
        when(openAccountQueryPort.findByIdAndCompanyId(
                ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
        when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                .thenReturn(true);
        when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.ANIMAL));
        when(productQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.PRODUCTO.id(),
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.PRODUCTO));
        when(employeeQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.EMPLEADO.id(),
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.EMPLEADO));
        when(branchResolver.resolve(ProductChargeOpenAccountMother.COMPANY_ID,
                ProductChargeOpenAccountMother.BRANCH_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.BRANCH_ID));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda el cargo con las referencias que devolvieron los puertos")
        void guarda_el_cargo_con_las_referencias_de_los_puertos() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoCrear());

            verify(repository).save(cargoCaptor.capture());
            ProductChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.getAnimal()).isEqualTo(ProductChargeOpenAccountMother.ANIMAL);
            assertThat(guardado.getProduct()).isEqualTo(ProductChargeOpenAccountMother.PRODUCTO);
            assertThat(guardado.getOpenAccount()).isEqualTo(ProductChargeOpenAccountMother.CUENTA);
            assertThat(guardado.getCreatedBy()).isEqualTo(ProductChargeOpenAccountMother.EMPLEADO);
            assertThat(guardado.getQuantity()).isEqualTo(2);
            assertThat(guardado.isEnabled()).isTrue();
            assertThat(guardado.isVoided()).isFalse();
        }

        @Test
        @DisplayName("guarda el desglose tributario congelado del catalogo")
        void guarda_el_desglose_tributario_congelado() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoCrear());

            verify(repository).save(cargoCaptor.capture());
            ProductChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(guardado.getTotalAmount()).isEqualByComparingTo("23800.00");
            assertThat(guardado.getBaseAmount()).isEqualByComparingTo("20000.00");
            assertThat(guardado.getTaxAmount()).isEqualByComparingTo("3800.00");
            assertThat(guardado.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(guardado.getTaxName()).isEqualTo("IVA 19%");
            assertThat(guardado.getTaxScheme()).isEqualTo("IVA");
            assertThat(guardado.getTaxTreatment()).isEqualTo("GRAVADO");
        }

        @Test
        @DisplayName("devuelve el DTO del cargo ya persistido, con su id")
        void devuelve_el_dto_del_cargo_ya_persistido() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            ProductChargeOpenAccountDto dto = service
                    .execute(ProductChargeOpenAccountMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
            assertThat(dto.totalAmount()).isEqualByComparingTo("11900.00");
            assertThat(dto.openAccount().id())
                    .isEqualTo(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("registra la salida de inventario con el id del cargo ya persistido")
        void registra_la_salida_de_inventario_con_el_id_persistido() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoCrear());

            // Si el kardex se alimentara del cargo de entrada, el chargeId seria null y
            // la compensacion al anular no encontraria los movimientos.
            verify(inventoryLedger).recordSale(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.BRANCH_ID,
                    ProductChargeOpenAccountMother.PRODUCTO.id(), 2,
                    ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.EMPLEADO.id());
        }

        @Test
        @DisplayName("recalcula el total de la cuenta al terminar")
        void recalcula_el_total_de_la_cuenta() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoCrear());

            verify(refresher).refresh(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("toma el bloqueo de la cuenta antes de leer su estado")
        void toma_el_bloqueo_antes_de_leer_el_estado() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoCrear());

            // El lock es un efecto (FOR UPDATE), no una consulta: si cae despues del
            // isOpen vuelve a abrirse el TOCTOU que cierra.
            InOrder orden = inOrder(openAccountQueryPort);
            orden.verify(openAccountQueryPort).lockForUpdate(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID);
            orden.verify(openAccountQueryPort)
                    .isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("valida la version esperada de la cuenta antes de guardar")
        void valida_la_version_esperada_antes_de_guardar() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());
            CreateProductChargeOpenAccountCommand comando = new CreateProductChargeOpenAccountCommand(
                    ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.PRODUCTO.id(), 2,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.EMPLEADO.id(),
                    ProductChargeOpenAccountMother.BRANCH_ID, null, 4L);

            service.execute(comando);

            verify(versionGuard).assertVersion(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID, 4L);
        }
    }

    @Nested
    @DisplayName("idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("con la misma clave devuelve el cargo ya registrado sin duplicarlo")
        void con_la_misma_clave_devuelve_el_cargo_ya_registrado() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(repository.findByOpenAccountIdAndClientRequestId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargoConClave("req-1")));

            ProductChargeOpenAccountDto dto = service
                    .execute(ProductChargeOpenAccountMother.comandoCrear("req-1"));

            // El reintento legitimo del mismo cliente sigue devolviendo EL MISMO cargo:
            // acotar la via de idempotencia no puede costar la idempotencia.
            assertThat(dto.id()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, productQueryPort, employeeQueryPort,
                    branchResolver, inventoryLedger, refresher, versionGuard);
        }

        @Test
        @DisplayName("un reintento con la clave de otra empresa no devuelve su cargo")
        void un_reintento_con_la_clave_de_otra_empresa_no_devuelve_su_cargo() {
            // La cuenta del comando es de otro tenant, asi que la resolucion acotada no
            // la encuentra. Con el chequeo de idempotencia por delante —como estaba— el
            // finder no lleva empresa (el cargo no tiene company_id propio) y bastaba
            // acertar el clientRequestId para que el servicio devolviera el DTO del cargo
            // ajeno sin pasar por ninguna comprobacion.
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoCrear("req-1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: "
                            + ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);

            verify(repository, never()).findByOpenAccountIdAndClientRequestId(any(), any());
            verifyNoInteractions(inventoryLedger, refresher, versionGuard, animalQueryPort,
                    productQueryPort, employeeQueryPort, branchResolver);
        }

        @Test
        @DisplayName("bloquea y resuelve la cuenta ANTES de mirar la clave de idempotencia")
        void bloquea_y_resuelve_la_cuenta_antes_de_mirar_la_clave() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(repository.findByOpenAccountIdAndClientRequestId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargoConClave("req-1")));

            service.execute(ProductChargeOpenAccountMother.comandoCrear("req-1"));

            // El lock sigue siendo la primera sentencia (el reintento que llega segundo
            // tiene que leer el cargo ya committeado por el rival) y la resolucion acotada
            // va antes del finder: es lo unico que demuestra que la cuenta es de esta
            // empresa antes de leer sus cargos.
            InOrder orden = inOrder(openAccountQueryPort, repository);
            orden.verify(openAccountQueryPort).lockForUpdate(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID);
            orden.verify(openAccountQueryPort).findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID);
            orden.verify(repository).findByOpenAccountIdAndClientRequestId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID, "req-1");
            verifyNoInteractions(versionGuard);
        }

        @Test
        @DisplayName("una clave en blanco no deduplica: el cargo se registra igual")
        void una_clave_en_blanco_no_deduplica() {
            todoResuelto();
            when(repository.save(any())).thenReturn(ProductChargeOpenAccountMother.cargo());

            service.execute(ProductChargeOpenAccountMother.comandoCrear("   "));

            verify(repository, never()).findByOpenAccountIdAndClientRequestId(any(), any());
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("la operacion se rechaza y no escribe nada")
    class Rechazos {

        @Test
        @DisplayName("cuenta inexistente")
        void cuenta_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: "
                            + ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);

            verifyNoInteractions(repository, inventoryLedger, refresher, animalQueryPort,
                    productQueryPort, employeeQueryPort, branchResolver);
        }

        @Test
        @DisplayName("cuenta de otra empresa: aislamiento por tenant")
        void cuenta_de_otra_empresa() {
            // La cuenta existe, pero es de otra empresa: la consulta acotada no la
            // resuelve, asi que el cargo se rechaza igual que si no existiera. Antes la
            // cuenta ajena SI se cargaba y solo un if posterior evitaba el cargo.
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: "
                            + ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);

            // Si el servicio volviera a la variante ancha, la cuenta ajena entraria de
            // nuevo en el flujo: el importe quedaria en la cuenta del otro tenant.
            verify(openAccountQueryPort, never()).findById(any());
            verifyNoInteractions(repository, inventoryLedger, refresher, versionGuard);
        }

        @Test
        @DisplayName("cuenta que ya no esta abierta")
        void cuenta_que_ya_no_esta_abierta() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.comandoCrear()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verifyNoInteractions(repository, inventoryLedger, refresher);
        }

        @Test
        @DisplayName("animal de otra empresa o inexistente")
        void animal_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(true);
            when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Animal not found: " + ProductChargeOpenAccountMother.ANIMAL.id());

            verifyNoInteractions(repository, inventoryLedger, refresher, productQueryPort,
                    employeeQueryPort, branchResolver);
        }

        @Test
        @DisplayName("producto de otra empresa o inexistente")
        void producto_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(true);
            when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.ANIMAL));
            when(productQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.PRODUCTO.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Product not found: " + ProductChargeOpenAccountMother.PRODUCTO.id());

            verifyNoInteractions(repository, inventoryLedger, refresher, employeeQueryPort,
                    branchResolver);
        }

        @Test
        @DisplayName("empleado de otra empresa o inexistente")
        void empleado_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(true);
            when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.ANIMAL));
            when(productQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.PRODUCTO.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.PRODUCTO));
            when(employeeQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.EMPLEADO.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Employee not found: " + ProductChargeOpenAccountMother.EMPLEADO.id());

            verifyNoInteractions(repository, inventoryLedger, refresher, branchResolver);
        }

        @Test
        @DisplayName("sin sede resoluble no se descuenta inventario a ciegas")
        void sin_sede_resoluble() {
            when(openAccountQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.CUENTA));
            when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(true);
            when(animalQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.ANIMAL.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.ANIMAL));
            when(productQueryPort.findByIdAndCompanyId(ProductChargeOpenAccountMother.PRODUCTO.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.PRODUCTO));
            when(employeeQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.EMPLEADO.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductChargeOpenAccountMother.EMPLEADO));
            when(branchResolver.resolve(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.BRANCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No se pudo resolver la sede");

            verifyNoInteractions(repository, inventoryLedger, refresher);
        }
    }
}
