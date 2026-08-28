package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CompanyAuditPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El camino de vuelta del archivado, que hasta ahora no existia: archivar una
 * empresa era irreversible desde la aplicacion y deshacerlo exigia un
 * {@code UPDATE} a mano contra produccion.
 *
 * <p>
 * <b>Lo que este servicio NO puede hacer, y por que sus casos se escriben
 * asi.</b> {@code CompanyJpaEntity} lleva
 * {@code @SQLRestriction("enabled = true")}, de modo que una empresa archivada
 * no la ve ninguna consulta JPA —{@code findById} incluido—. Leer-y-luego-
 * guardar es imposible: hay que reactivar con un {@code UPDATE} y decidir el
 * «no existe» contando filas. Por eso aqui no se verifica ningun {@code save} y
 * si el conteo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateCompanyService — sacar del archivo una empresa borrada por error")
class ReactivateCompanyServiceTest {

    private static final Long ID = CompanyMother.COMPANY_ID;

    @Mock
    private CompanyRepository repository;
    @Mock
    private CompanyAuditPort audit;

    private ReactivateCompanyService service;

    @BeforeEach
    void montar() {
        service = new ReactivateCompanyService(repository, audit);
    }

    @Nested
    @DisplayName("Restauracion")
    class Restauracion {

        @Test
        @DisplayName("reactiva la fila y devuelve la ficha releida, ya habilitada")
        void reactiva_y_devuelve_la_ficha() {
            when(repository.reactivate(ID)).thenReturn(1);
            when(repository.findById(ID)).thenReturn(Optional.of(CompanyMother.clinicaNorte()));

            CompanyDto dto = service.execute(ID);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.enabled()).isTrue();
        }

        /**
         * El orden no es cosmetico: leer <b>despues</b> del {@code UPDATE} es lo unico
         * que devuelve una ficha, porque antes la restriccion de la entidad la
         * escondia.
         */
        @Test
        @DisplayName("primero reactiva y despues relee: al reves no habria nada que leer")
        void primero_reactiva_y_despues_relee() {
            when(repository.reactivate(ID)).thenReturn(1);
            when(repository.findById(ID)).thenReturn(Optional.of(CompanyMother.clinicaNorte()));

            service.execute(ID);

            InOrder orden = Mockito.inOrder(repository);
            orden.verify(repository).reactivate(ID);
            orden.verify(repository).findById(ID);
        }

        /**
         * Cero filas cubre los dos casos que para quien llama son el mismo: el id no
         * existe, o la empresa ya estaba activa. En ninguno hay nada que restaurar.
         */
        @Test
        @DisplayName("cero filas reactivadas es un 404 y no se audita nada")
        void cero_filas_es_un_404() {
            when(repository.reactivate(ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(CompanyNotFoundException.class);

            verifyNoInteractions(audit);
        }
    }

    @Nested
    @DisplayName("Auditoria")
    class Auditoria {

        /**
         * Restaurar devuelve el acceso a todos los empleados de esa clinica a la vez.
         * El {@code http_mutation} generico del borde solo dice que hubo un
         * {@code PATCH} con un 200; quien reclame por que la clinica volvio a estar
         * operativa necesita el id y el nombre.
         */
        @Test
        @DisplayName("deja el evento con id, nombre e identificador de la empresa")
        void deja_su_evento_de_auditoria() {
            when(repository.reactivate(ID)).thenReturn(1);
            when(repository.findById(ID)).thenReturn(Optional.of(CompanyMother.clinicaNorte()));

            service.execute(ID);

            verify(audit).companyReactivated(ID, "Clinica Norte", "NIT-900");
        }
    }
}
