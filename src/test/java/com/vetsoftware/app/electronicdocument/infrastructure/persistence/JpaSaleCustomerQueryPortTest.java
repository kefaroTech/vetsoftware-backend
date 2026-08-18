package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleCustomerQueryPort.SaleCustomer;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSaleCustomerQueryPort — congela el owner de la venta directa de POS")
class JpaSaleCustomerQueryPortTest {

    @Mock
    private OwnerJpaRepository ownerJpaRepository;

    private JpaSaleCustomerQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaSaleCustomerQueryPort(ownerJpaRepository);
    }

    private static CompanyJpaEntity empresa(Long id) throws Exception {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        Field idField = CompanyJpaEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(company, id);
        return company;
    }

    private static OwnerJpaEntity owner(Long companyId, boolean withholdingAgent) throws Exception {
        OwnerJpaEntity owner = ReflectionEntities.newInstance(OwnerJpaEntity.class);
        owner.setDocumentType(OwnerDocumentType.CEDULA_CIUDADANIA);
        owner.setDocument("1020304050");
        owner.setVerificationDigit("3");
        owner.setPersonType(PersonType.NATURAL);
        owner.setLegalName(null);
        owner.setName("Ana Perez");
        owner.setEmail("ana@correo.co");
        CityJpaEntity city = ReflectionEntities.newInstance(CityJpaEntity.class);
        city.setDaneCode("05001");
        owner.setCity(city);
        owner.setWithholdingAgent(withholdingAgent);
        owner.setCompany(empresa(companyId));
        return owner;
    }

    @Test
    @DisplayName("un owner de la empresa se congela en CustomerSnapshot con su flag de retencion")
    void owner_de_la_empresa_se_congela_en_snapshot() throws Exception {
        OwnerJpaEntity owner = owner(9L, true);
        when(ownerJpaRepository.findById(5L)).thenReturn(Optional.of(owner));

        Optional<SaleCustomer> result = port.findOwner(5L, 9L);

        assertThat(result).isPresent();
        SaleCustomer saleCustomer = result.get();
        assertThat(saleCustomer.withholdingAgent()).isTrue();
        assertThat(saleCustomer.snapshot().documentId()).isEqualTo("1020304050");
        assertThat(saleCustomer.snapshot().name()).isEqualTo("Ana Perez");
        assertThat(saleCustomer.snapshot().email()).isEqualTo("ana@correo.co");
    }

    @Nested
    @DisplayName("ausencias — devuelve vacio sin lanzar")
    class Ausencias {

        @Test
        @DisplayName("un owner inexistente devuelve vacio")
        void owner_inexistente_devuelve_vacio() {
            when(ownerJpaRepository.findById(5L)).thenReturn(Optional.empty());

            assertThat(port.findOwner(5L, 9L)).isEmpty();
        }

        @Test
        @DisplayName("un owner de otra empresa devuelve vacio (no fuga cross-tenant)")
        void owner_de_otra_empresa_devuelve_vacio() throws Exception {
            OwnerJpaEntity owner = owner(77L, false);
            when(ownerJpaRepository.findById(5L)).thenReturn(Optional.of(owner));

            assertThat(port.findOwner(5L, 9L)).isEmpty();
        }

        @Test
        @DisplayName("un owner sin empresa asociada devuelve vacio")
        void owner_sin_empresa_devuelve_vacio() throws Exception {
            OwnerJpaEntity owner = ReflectionEntities.newInstance(OwnerJpaEntity.class);
            owner.setDocumentType(OwnerDocumentType.CEDULA_CIUDADANIA);
            owner.setDocument("1020304050");
            owner.setName("Ana Perez");
            owner.setPersonType(PersonType.NATURAL);
            owner.setCompany(null);
            when(ownerJpaRepository.findById(5L)).thenReturn(Optional.of(owner));

            assertThat(port.findOwner(5L, 9L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("datos ausentes del owner quedan null en el snapshot, sin lanzar")
    class DatosAusentes {

        @Test
        @DisplayName("sin tipo de documento, tipo de persona, ciudad, regimen ni responsabilidad, todo queda null")
        void owner_con_datos_minimos_congela_snapshot_con_nulls() throws Exception {
            OwnerJpaEntity owner = ReflectionEntities.newInstance(OwnerJpaEntity.class);
            owner.setDocument("1020304050");
            owner.setName("Ana Perez");
            // TaxRegime/FiscalResponsibility traen default no-null en la entidad: hay que
            // limpiarlos a mano para ejercitar la rama null de fromName(...).
            owner.setTaxRegime(null);
            owner.setFiscalResponsibility(null);
            owner.setCompany(empresa(9L));
            when(ownerJpaRepository.findById(5L)).thenReturn(Optional.of(owner));

            SaleCustomer result = port.findOwner(5L, 9L).orElseThrow();

            assertThat(result.snapshot().documentType()).isNull();
            assertThat(result.snapshot().personType()).isNull();
            assertThat(result.snapshot().cityDaneCode()).isNull();
            assertThat(result.snapshot().taxRegime()).isNull();
            assertThat(result.snapshot().fiscalResponsibility()).isNull();
        }
    }
}
