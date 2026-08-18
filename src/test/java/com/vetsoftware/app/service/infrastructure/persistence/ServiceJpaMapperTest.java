package com.vetsoftware.app.service.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.TaxRef;
import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.service.testsupport.ServiceMother;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ida y vuelta dominio &harr; entidad JPA.
 *
 * <p>
 * Las entidades de OTRAS features ({@code CompanyJpaEntity},
 * {@code ServiceCategoryJpaEntity}, {@code TaxJpaEntity}) declaran su
 * constructor sin argumentos como {@code protected} y viven en otro paquete: se
 * sustituyen por dobles que solo responden a los getters que el mapper lee. La
 * entidad propia {@link ServiceJpaEntity} SI se construye de verdad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceJpaMapper")
class ServiceJpaMapperTest {

    private final ServiceJpaMapper mapper = new ServiceJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private ServiceCategoryJpaEntity categoryEntity;
    @Mock
    private TaxJpaEntity taxEntity;

    private static ServiceJpaEntity entidadPoblada() {
        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setId(ServiceMother.SERVICE_ID);
        entity.setName("Consulta general");
        entity.setPrice(new BigDecimal("50000.00"));
        entity.setTaxTreatment(TaxTreatment.GRAVADO);
        entity.setNotes("Consulta veterinaria estandar");
        entity.setCreatedDate(ServiceMother.CREADO);
        entity.setUpdatedDate(LocalDateTime.of(2026, 3, 1, 8, 0));
        entity.setUpdatedBy(77L);
        entity.setVersion(4L);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa")
    class ADominioJpa {

        @Test
        @DisplayName("copia los campos planos del servicio")
        void copia_los_campos_planos() {
            ServiceJpaEntity entity = mapper.toJpa(ServiceMother.consultaGeneral(), categoryEntity,
                    taxEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(ServiceMother.SERVICE_ID);
            assertThat(entity.getName()).isEqualTo("Consulta general");
            assertThat(entity.getPrice()).isEqualByComparingTo("50000.00");
            assertThat(entity.getTaxTreatment()).isEqualTo(TaxTreatment.GRAVADO);
            assertThat(entity.getNotes()).isEqualTo("Consulta veterinaria estandar");
            assertThat(entity.getCreatedDate()).isEqualTo(ServiceMother.CREADO);
            assertThat(entity.getVersion()).isEqualTo(0L);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha las asociaciones recibidas, sin recrearlas")
        void engancha_las_asociaciones_recibidas() {
            ServiceJpaEntity entity = mapper.toJpa(ServiceMother.consultaGeneral(), categoryEntity,
                    taxEntity, companyEntity);

            // El repositorio pasa proxies de getReferenceById: el mapper debe reusarlos,
            // no construir entidades nuevas que Hibernate trataria como transitorias.
            assertThat(entity.getServiceCategory()).isSameAs(categoryEntity);
            assertThat(entity.getTax()).isSameAs(taxEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("acepta impuesto nulo para EXENTO/EXCLUIDO")
        void acepta_impuesto_nulo() {
            ServiceJpaEntity entity = mapper.toJpa(ServiceMother.exenta(), categoryEntity, null,
                    companyEntity);

            assertThat(entity.getTax()).isNull();
            assertThat(entity.getTaxTreatment()).isEqualTo(TaxTreatment.EXENTO);
        }

        @Test
        @DisplayName("un servicio nuevo viaja sin id, para que la BD lo genere")
        void servicio_nuevo_viaja_sin_id() {
            Service nuevo = Service.create("Cirugia", BigDecimal.TEN, TaxTreatment.EXCLUIDO, null,
                    ServiceMother.CONSULTAS, null, ServiceMother.CLINICA);

            ServiceJpaEntity entity = mapper.toJpa(nuevo, categoryEntity, null, companyEntity);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain leyendo las asociaciones de la fila")
    class ADominio {

        @Test
        @DisplayName("reconstruye el servicio con sus tres referencias")
        void reconstruye_el_servicio_completo() {
            when(categoryEntity.getId()).thenReturn(20L);
            when(categoryEntity.getName()).thenReturn("Consultas");
            when(taxEntity.getId()).thenReturn(30L);
            when(taxEntity.getName()).thenReturn("IVA 19%");
            when(taxEntity.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(companyEntity.getId()).thenReturn(ServiceMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Veterinaria de prueba");
            when(companyEntity.getIdentifier()).thenReturn("900123456");

            ServiceJpaEntity entity = entidadPoblada();
            entity.setServiceCategory(categoryEntity);
            entity.setTax(taxEntity);
            entity.setCompany(companyEntity);

            Service service = mapper.toDomain(entity);

            assertThat(service.getServiceCategory()).isEqualTo(ServiceMother.CONSULTAS);
            assertThat(service.getTax()).isEqualTo(ServiceMother.IVA_19);
            assertThat(service.getCompany()).isEqualTo(ServiceMother.CLINICA);
        }

        @Test
        @DisplayName("deja el impuesto en null cuando la fila no lo tiene")
        void impuesto_nulo_cuando_la_fila_no_lo_tiene() {
            when(categoryEntity.getId()).thenReturn(20L);
            when(categoryEntity.getName()).thenReturn("Consultas");
            when(companyEntity.getId()).thenReturn(ServiceMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Veterinaria de prueba");
            when(companyEntity.getIdentifier()).thenReturn("900123456");

            ServiceJpaEntity entity = entidadPoblada();
            entity.setTaxTreatment(TaxTreatment.EXCLUIDO);
            entity.setServiceCategory(categoryEntity);
            entity.setCompany(companyEntity);

            Service service = mapper.toDomain(entity);

            assertThat(service.getTax()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con las referencias ya resueltas")
    class ADominioConRefs {

        @Test
        @DisplayName("copia los campos planos de la fila")
        void copia_los_campos_planos() {
            Service service = mapper.toDomain(entidadPoblada(), ServiceMother.CONSULTAS,
                    ServiceMother.IVA_19, ServiceMother.CLINICA);

            assertThat(service.getId()).isEqualTo(ServiceMother.SERVICE_ID);
            assertThat(service.getName()).isEqualTo("Consulta general");
            assertThat(service.getPrice()).isEqualByComparingTo("50000.00");
            assertThat(service.getNotes()).isEqualTo("Consulta veterinaria estandar");
            assertThat(service.getCreatedDate()).isEqualTo(ServiceMother.CREADO);
            assertThat(service.getUpdatedDate()).isEqualTo(LocalDateTime.of(2026, 3, 1, 8, 0));
            assertThat(service.getUpdatedBy()).isEqualTo(77L);
            assertThat(service.getVersion()).isEqualTo(4L);
            assertThat(service.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("ida y vuelta: dominio -> entidad -> dominio conserva el servicio")
        void ida_y_vuelta_conserva_el_servicio() {
            Service original = ServiceMother.consultaGeneral();

            ServiceJpaEntity entity = mapper.toJpa(original, categoryEntity, taxEntity,
                    companyEntity);
            Service vuelta = mapper.toDomain(entity, ServiceMother.CONSULTAS, ServiceMother.IVA_19,
                    ServiceMother.CLINICA);

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getName()).isEqualTo(original.getName());
            assertThat(vuelta.getPrice()).isEqualByComparingTo(original.getPrice());
            assertThat(vuelta.getTaxTreatment()).isEqualTo(original.getTaxTreatment());
            assertThat(vuelta.getNotes()).isEqualTo(original.getNotes());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }

        @Test
        @DisplayName("las refs que se pasan mandan sobre las asociaciones de la fila")
        void las_refs_pasadas_mandan() {
            ServiceCategoryRef otraCategoria = new ServiceCategoryRef(999L, "Otra");
            TaxRef otroImpuesto = new TaxRef(998L, "IVA 5%", new BigDecimal("5.00"));
            CompanyRef otraEmpresa = new CompanyRef(997L, "Otra Clinica", "NIT-111");

            Service service = mapper.toDomain(entidadPoblada(), otraCategoria, otroImpuesto,
                    otraEmpresa);

            assertThat(service.getServiceCategory()).isEqualTo(otraCategoria);
            assertThat(service.getTax()).isEqualTo(otroImpuesto);
            assertThat(service.getCompany()).isEqualTo(otraEmpresa);
        }
    }
}
