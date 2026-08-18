package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.debtopenaccount.infrastructure.persistence.DebtOpenAccountJpaEntity;
import com.vetsoftware.app.debtopenaccount.infrastructure.persistence.DebtOpenAccountJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort.CompanyFiscalProfile;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort.SaleSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence.GeneralChargeOpenAccountJpaEntity;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence.GeneralChargeOpenAccountJpaRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence.ProductChargeOpenAccountJpaEntity;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence.ProductChargeOpenAccountJpaRepository;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence.ServiceChargeOpenAccountJpaEntity;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence.ServiceChargeOpenAccountJpaRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * JpaSaleSnapshotQueryPort — congela open account + los 3 tipos de cargo +
 * abonos + perfil fiscal en el read model de la feature. Es el unico punto que
 * conoce esas otras features (cruce permitido de vertical slicing); las
 * entidades JPA ajenas se instancian por reflexion via
 * {@link ReflectionEntities} porque su constructor es protegido.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSaleSnapshotQueryPort — congela open account + cargos + abonos + perfil fiscal")
class JpaSaleSnapshotQueryPortTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OPEN_ACCOUNT_ID = 100L;
    private static final Long BRANCH_ID = 7L;

    @Mock
    private OpenAccountJpaRepository openAccountRepository;
    @Mock
    private ProductChargeOpenAccountJpaRepository productChargeRepository;
    @Mock
    private ServiceChargeOpenAccountJpaRepository serviceChargeRepository;
    @Mock
    private GeneralChargeOpenAccountJpaRepository generalChargeRepository;
    @Mock
    private DebtOpenAccountJpaRepository debtRepository;
    @Mock
    private CompanyFiscalProfileQueryPort fiscalProfileQueryPort;

    private JpaSaleSnapshotQueryPort port;

    private static IssuerSnapshot issuer() {
        return new IssuerSnapshot("NIT", "900123456", "7", "Veterinaria Vet SAS", "RESPONSABLE",
                "facturacion@vet.co", List.of("O-13"));
    }

    private static CompanyFiscalProfile perfilFiscal() {
        return new CompanyFiscalProfile(issuer(), new BigDecimal("2.50"), new BigDecimal("15.00"),
                new BigDecimal("1.00"));
    }

    private static CompanyJpaEntity empresa(Long id) throws Exception {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        Field idField = CompanyJpaEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(company, id);
        return company;
    }

    private static OwnerJpaEntity owner(boolean withholdingAgent,
            com.vetsoftware.app.owner.domain.TaxRegime taxRegime,
            com.vetsoftware.app.owner.domain.FiscalResponsibility fiscalResponsibility,
            String daneCode) throws Exception {
        OwnerJpaEntity owner = ReflectionEntities.newInstance(OwnerJpaEntity.class);
        owner.setId(50L);
        owner.setDocumentType(OwnerDocumentType.CEDULA_CIUDADANIA);
        owner.setDocument("1020304050");
        owner.setVerificationDigit("3");
        owner.setPersonType(PersonType.NATURAL);
        owner.setLegalName(null);
        owner.setName("Ana Perez");
        owner.setEmail("ana@correo.co");
        if (daneCode != null) {
            CityJpaEntity city = ReflectionEntities.newInstance(CityJpaEntity.class);
            city.setDaneCode(daneCode);
            owner.setCity(city);
        }
        owner.setWithholdingAgent(withholdingAgent);
        owner.setTaxRegime(taxRegime);
        owner.setFiscalResponsibility(fiscalResponsibility);
        owner.setCompany(empresa(COMPANY_ID));
        return owner;
    }

    private static OwnerJpaEntity ownerBasico() throws Exception {
        return owner(true, com.vetsoftware.app.owner.domain.TaxRegime.NO_RESPONSABLE_IVA,
                com.vetsoftware.app.owner.domain.FiscalResponsibility.NO_APLICA, "05001");
    }

    private static OpenAccountJpaEntity cuenta(OwnerJpaEntity owner, OpenAccountStatus status)
            throws Exception {
        OpenAccountJpaEntity account = ReflectionEntities.newInstance(OpenAccountJpaEntity.class);
        account.setId(OPEN_ACCOUNT_ID);
        account.setOwner(owner);
        account.setStatus(status);
        BranchJpaEntity branch = ReflectionEntities.newInstance(BranchJpaEntity.class);
        branch.setId(BRANCH_ID);
        account.setBranch(branch);
        account.setCompany(empresa(COMPANY_ID));
        return account;
    }

    private static ProductChargeOpenAccountJpaEntity productCharge(String productName, int quantity,
            BigDecimal unitPrice, BigDecimal baseAmount, boolean hasTax, BigDecimal taxPercentage,
            BigDecimal taxAmount, BigDecimal totalAmount, String taxScheme, String taxTreatment,
            boolean voided) throws Exception {
        ProductChargeOpenAccountJpaEntity charge = ReflectionEntities
                .newInstance(ProductChargeOpenAccountJpaEntity.class);
        if (productName != null) {
            ProductJpaEntity product = ReflectionEntities.newInstance(ProductJpaEntity.class);
            product.setName(productName);
            charge.setProduct(product);
        }
        charge.setUnitPrice(unitPrice);
        charge.setQuantity(quantity);
        charge.setHasTax(hasTax);
        charge.setTaxPercentage(taxPercentage);
        charge.setTaxScheme(taxScheme);
        charge.setTaxTreatment(taxTreatment);
        charge.setBaseAmount(baseAmount);
        charge.setTaxAmount(taxAmount);
        charge.setTotalAmount(totalAmount);
        charge.setVoided(voided);
        charge.setEnabled(true);
        return charge;
    }

    private static ServiceChargeOpenAccountJpaEntity serviceCharge(String serviceName,
            BigDecimal unitPrice, BigDecimal baseAmount, boolean hasTax, BigDecimal taxPercentage,
            BigDecimal taxAmount, BigDecimal totalAmount, String taxScheme, String taxTreatment,
            boolean voided) throws Exception {
        ServiceChargeOpenAccountJpaEntity charge = ReflectionEntities
                .newInstance(ServiceChargeOpenAccountJpaEntity.class);
        if (serviceName != null) {
            ServiceJpaEntity service = ReflectionEntities.newInstance(ServiceJpaEntity.class);
            service.setName(serviceName);
            charge.setService(service);
        }
        charge.setUnitPrice(unitPrice);
        charge.setHasTax(hasTax);
        charge.setTaxPercentage(taxPercentage);
        charge.setTaxScheme(taxScheme);
        charge.setTaxTreatment(taxTreatment);
        charge.setBaseAmount(baseAmount);
        charge.setTaxAmount(taxAmount);
        charge.setTotalAmount(totalAmount);
        charge.setVoided(voided);
        charge.setEnabled(true);
        return charge;
    }

    private static GeneralChargeOpenAccountJpaEntity generalCharge(String name, BigDecimal quantity,
            BigDecimal unitAmount, BigDecimal baseAmount, boolean hasTax, BigDecimal taxPercentage,
            BigDecimal taxAmount, BigDecimal totalAmount, String taxScheme, boolean voided)
            throws Exception {
        GeneralChargeOpenAccountJpaEntity charge = ReflectionEntities
                .newInstance(GeneralChargeOpenAccountJpaEntity.class);
        charge.setName(name);
        charge.setQuantity(quantity);
        charge.setUnitAmount(unitAmount);
        charge.setHasTax(hasTax);
        charge.setTaxPercentage(taxPercentage);
        charge.setTaxScheme(taxScheme);
        charge.setBaseAmount(baseAmount);
        charge.setTaxAmount(taxAmount);
        charge.setTotalAmount(totalAmount);
        charge.setVoided(voided);
        charge.setEnabled(true);
        return charge;
    }

    private static DebtOpenAccountJpaEntity abono(BigDecimal amount, PaymentMethod method,
            boolean voided) throws Exception {
        DebtOpenAccountJpaEntity debt = ReflectionEntities
                .newInstance(DebtOpenAccountJpaEntity.class);
        debt.setAmount(amount);
        debt.setPaymentMethod(method);
        debt.setVoided(voided);
        debt.setEnabled(true);
        return debt;
    }

    private void sinCargosNiAbonos() {
        when(productChargeRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                COMPANY_ID)).thenReturn(List.of());
        when(serviceChargeRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                COMPANY_ID)).thenReturn(List.of());
        when(generalChargeRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                COMPANY_ID)).thenReturn(List.of());
        when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                COMPANY_ID)).thenReturn(List.of());
    }

    @BeforeEach
    void montar() {
        port = new JpaSaleSnapshotQueryPort(openAccountRepository, productChargeRepository,
                serviceChargeRepository, generalChargeRepository, debtRepository,
                fiscalProfileQueryPort);
    }

    @Nested
    @DisplayName("ausencias — no fugan datos ni tocan repos que no hacen falta")
    class Ausencias {

        @Test
        @DisplayName("una cuenta inexistente o de otra empresa devuelve vacio sin tocar el resto")
        void cuenta_inexistente_devuelve_vacio() {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            Optional<SaleSnapshot> result = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(fiscalProfileQueryPort, productChargeRepository,
                    serviceChargeRepository, generalChargeRepository, debtRepository);
        }

        @Test
        @DisplayName("una empresa sin perfil fiscal lanza IllegalArgumentException sin tocar los cargos")
        void sin_perfil_fiscal_lanza() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no tiene perfil fiscal");

            verifyNoInteractions(productChargeRepository, serviceChargeRepository,
                    generalChargeRepository, debtRepository);
        }
    }

    @Nested
    @DisplayName("ensamblado del snapshot")
    class Ensamblado {

        @Test
        @DisplayName("una cuenta CERRADA produce un snapshot cerrado con la sede y el agente retenedor de la cuenta")
        void cuenta_cerrada_produce_snapshot_cerrado() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.CLOSE)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            sinCargosNiAbonos();

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.companyId()).isEqualTo(COMPANY_ID);
            assertThat(snapshot.openAccountId()).isEqualTo(OPEN_ACCOUNT_ID);
            assertThat(snapshot.accountClosed()).isTrue();
            assertThat(snapshot.branchId()).isEqualTo(BRANCH_ID);
            assertThat(snapshot.customerWithholdingAgent()).isTrue();
            assertThat(snapshot.reteFuenteRate()).isEqualByComparingTo("2.50");
            assertThat(snapshot.reteIvaRate()).isEqualByComparingTo("15.00");
            assertThat(snapshot.reteIcaRate()).isEqualByComparingTo("1.00");
            assertThat(snapshot.issuer()).isEqualTo(issuer());
            assertThat(snapshot.lines()).isEmpty();
            assertThat(snapshot.payments()).isEmpty();
        }

        @Test
        @DisplayName("una cuenta ABIERTA produce un snapshot no cerrado")
        void cuenta_abierta_produce_snapshot_no_cerrado() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            sinCargosNiAbonos();

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.accountClosed()).isFalse();
        }

        @Test
        @DisplayName("un owner sin ciudad congela un customer sin cityDaneCode")
        void owner_sin_ciudad_congela_customer_sin_dane() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(owner(false,
                            com.vetsoftware.app.owner.domain.TaxRegime.RESPONSABLE_IVA,
                            com.vetsoftware.app.owner.domain.FiscalResponsibility.GRAN_CONTRIBUYENTE,
                            null), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            sinCargosNiAbonos();

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.customer().cityDaneCode()).isNull();
            assertThat(snapshot.customer().taxRegime()).isEqualTo(
                    com.vetsoftware.app.electronicdocument.domain.TaxRegime.RESPONSABLE_IVA);
            assertThat(snapshot.customer().fiscalResponsibility()).isEqualTo(
                    com.vetsoftware.app.electronicdocument.domain.FiscalResponsibility.GRAN_CONTRIBUYENTE);
        }

        @Test
        @DisplayName("un owner con regimen y responsabilidad fiscal null los congela como null en el customer")
        void owner_sin_regimen_ni_responsabilidad_deja_null() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional
                            .of(cuenta(owner(false, null, null, "05001"), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            sinCargosNiAbonos();

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.customer().taxRegime()).isNull();
            assertThat(snapshot.customer().fiscalResponsibility()).isNull();
        }
    }

    @Nested
    @DisplayName("lineas — cargos de producto, servicio y generales")
    class Lineas {

        @Test
        @DisplayName("un cargo de producto anulado no aparece en las lineas")
        void cargo_anulado_se_excluye() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(productCharge("Vacuna", 1, new BigDecimal("50000"),
                            new BigDecimal("50000"), false, null, BigDecimal.ZERO,
                            new BigDecimal("50000"), null, null, true)));
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.lines()).isEmpty();
        }

        @Test
        @DisplayName("un cargo de servicio anulado no aparece en las lineas")
        void cargo_de_servicio_anulado_se_excluye() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(serviceCharge("Consulta", new BigDecimal("50000"),
                            new BigDecimal("50000"), false, null, BigDecimal.ZERO,
                            new BigDecimal("50000"), null, null, true)));
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.lines()).isEmpty();
        }

        @Test
        @DisplayName("un cargo general anulado no aparece en las lineas")
        void cargo_general_anulado_se_excluye() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(generalCharge("Recargo", BigDecimal.ONE,
                            new BigDecimal("5000"), new BigDecimal("5000"), false, null,
                            BigDecimal.ZERO, new BigDecimal("5000"), null, true)));
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.lines()).isEmpty();
        }

        @Test
        @DisplayName("un producto sin catalogo usa la descripcion generica 'Producto'")
        void producto_sin_catalogo_usa_descripcion_generica() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(productCharge(null, 2, new BigDecimal("1000"),
                            new BigDecimal("2000"), false, null, BigDecimal.ZERO,
                            new BigDecimal("2000"), null, null, false)));
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.lines()).hasSize(1);
            ElectronicDocumentLine linea = snapshot.lines().get(0);
            assertThat(linea.getDescription()).isEqualTo("Producto");
            assertThat(linea.getQuantity()).isEqualByComparingTo("2");
            assertThat(linea.getTaxCategory()).isEqualTo(TaxCategory.EXCLUIDO);
            assertThat(linea.getTaxScheme()).isNull();
            assertThat(linea.getTaxRate()).isNull();
        }

        @Test
        @DisplayName("un servicio sin catalogo usa la descripcion generica 'Servicio' con cantidad fija 1")
        void servicio_sin_catalogo_usa_descripcion_generica() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(serviceCharge(null, new BigDecimal("80000"),
                            new BigDecimal("80000"), false, null, BigDecimal.ZERO,
                            new BigDecimal("80000"), null, null, false)));
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.lines()).hasSize(1);
            ElectronicDocumentLine linea = snapshot.lines().get(0);
            assertThat(linea.getDescription()).isEqualTo("Servicio");
            assertThat(linea.getQuantity()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("EXENTO conserva esquema IVA con tarifa cero, sin importar hasTax/porcentaje")
        void exento_conserva_esquema_iva_tarifa_cero() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(productCharge("Medicamento exento", 1,
                            new BigDecimal("30000"), new BigDecimal("30000"), true,
                            new BigDecimal("19"), BigDecimal.ZERO, new BigDecimal("30000"), "IVA",
                            "EXENTO", false)));
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            ElectronicDocumentLine linea = snapshot.lines().get(0);
            assertThat(linea.getTaxCategory()).isEqualTo(TaxCategory.EXENTO);
            assertThat(linea.getTaxScheme()).isEqualTo(TaxScheme.IVA);
            assertThat(linea.getTaxRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("un cargo gravado con esquema INC congelado produce categoria y esquema INC")
        void gravado_con_esquema_inc_congelado() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(serviceCharge("Comida rapida gravada",
                            new BigDecimal("20000"), new BigDecimal("20000"), true,
                            new BigDecimal("8"), new BigDecimal("1600"), new BigDecimal("21600"),
                            "INC", null, false)));
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            ElectronicDocumentLine linea = snapshot.lines().get(0);
            assertThat(linea.getTaxCategory()).isEqualTo(TaxCategory.INC);
            assertThat(linea.getTaxScheme()).isEqualTo(TaxScheme.INC);
            assertThat(linea.getTaxRate()).isEqualByComparingTo("8");
        }

        @Test
        @DisplayName("un cargo general gravado sin esquema congelado cae a IVA por defecto (heuristica)")
        void general_gravado_cae_a_iva_por_defecto() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(generalCharge("Recargo domicilio", new BigDecimal("1"),
                            new BigDecimal("5000"), new BigDecimal("5000"), true,
                            new BigDecimal("19"), new BigDecimal("950"), new BigDecimal("5950"),
                            null, false)));
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            ElectronicDocumentLine linea = snapshot.lines().get(0);
            assertThat(linea.getDescription()).isEqualTo("Recargo domicilio");
            assertThat(linea.getTaxCategory()).isEqualTo(TaxCategory.GRAVADO);
            assertThat(linea.getTaxScheme()).isEqualTo(TaxScheme.IVA);
        }

        @Test
        @DisplayName("las lineas de producto, servicio y general se numeran de forma consecutiva y unica")
        void lineas_se_numeran_consecutivamente() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(productCharge("Producto A", 1, new BigDecimal("1000"),
                            new BigDecimal("1000"), false, null, BigDecimal.ZERO,
                            new BigDecimal("1000"), null, null, false)));
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(serviceCharge("Servicio B", new BigDecimal("2000"),
                            new BigDecimal("2000"), false, null, BigDecimal.ZERO,
                            new BigDecimal("2000"), null, null, false)));
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(generalCharge("General C", BigDecimal.ONE,
                            new BigDecimal("3000"), new BigDecimal("3000"), false, null,
                            BigDecimal.ZERO, new BigDecimal("3000"), null, false)));
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of());

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.lines()).hasSize(3);
            assertThat(snapshot.lines()).extracting(ElectronicDocumentLine::getLineNumber)
                    .containsExactly(1, 2, 3);
            assertThat(snapshot.lines()).extracting(ElectronicDocumentLine::getDescription)
                    .containsExactly("Producto A", "Servicio B", "General C");
        }
    }

    @Nested
    @DisplayName("abonos — medio de pago codificado DIAN")
    class Abonos {

        @ParameterizedTest
        @CsvSource({"CASH, EFECTIVO", "CARD, TARJETA_CREDITO", "BANK_TRANSFER, TRANSFERENCIA"})
        @DisplayName("cada medio de pago local mapea a su codigo DIAN")
        void medio_de_pago_mapea_a_codigo_dian(PaymentMethod metodo, PaymentMeans esperado)
                throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID)).thenReturn(List.of(abono(new BigDecimal("1000"), metodo, false)));

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.payments()).hasSize(1);
            assertThat(snapshot.payments().get(0).getPaymentMeans()).isEqualTo(esperado);
            assertThat(snapshot.payments().get(0).getAmount()).isEqualByComparingTo("1000");
        }

        @Test
        @DisplayName("un abono anulado no aparece entre los pagos")
        void abono_anulado_se_excluye() throws Exception {
            when(openAccountRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cuenta(ownerBasico(), OpenAccountStatus.OPEN)));
            when(fiscalProfileQueryPort.findByCompany(COMPANY_ID))
                    .thenReturn(Optional.of(perfilFiscal()));
            when(productChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(serviceChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(generalChargeRepository
                    .findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());
            when(debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(OPEN_ACCOUNT_ID,
                    COMPANY_ID))
                    .thenReturn(List.of(abono(new BigDecimal("500"), PaymentMethod.CASH, true)));

            SaleSnapshot snapshot = port.findByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)
                    .orElseThrow();

            assertThat(snapshot.payments()).isEmpty();
        }
    }
}
