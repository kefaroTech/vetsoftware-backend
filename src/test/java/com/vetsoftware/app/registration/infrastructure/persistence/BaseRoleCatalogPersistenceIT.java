package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider.BaseRoleData;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * El catalogo de roles base <b>tal como lo dejan las migraciones reales</b>,
 * sobre MySQL 8.4 de verdad.
 *
 * <p>
 * <b>Esta es la prueba que habria cazado el defecto del issue #500, y un doble
 * no la puede dar.</b> El bug no estaba en como {@code JpaBaseRoleProvider}
 * mapea una fila —eso ya lo fija {@code JpaBaseRoleProviderTest} con mocks, y
 * estaba en verde mientras el alta entregaba cuentas inservibles—: estaba en
 * que <em>no habia ninguna fila que mapear</em>. {@code base_roles} se crea en
 * {@code 005_create_base_roles.xml} y se altera en 068 y 225, pero durante mas
 * de 260 changesets no la sembro ninguno. Un mock del repositorio no puede
 * observar eso, porque el conjunto vacio se lo inventa el propio test; solo lo
 * ve quien pregunta al schema migrado.
 *
 * <p>
 * Por eso la asercion es la que es: no «el provider mapea bien», sino
 * <b>«despues de correr las migraciones existe al menos un rol base obligatorio
 * que el alta pueda asignarle al dueño»</b>. Escrita antes del changeset 266
 * habria estado roja, que es exactamente lo que se le pide a una red.
 *
 * <p>
 * Es tambien la mitad de datos del par: la mitad de codigo la cubre la guarda
 * de {@code RegisterUserService.requireOwnerRole}, que hace fallar el alta
 * entera cuando esta consulta viene vacia. Las dos hacen falta — sembrar
 * arregla la base de hoy, la guarda impide que el proximo entorno vuelva a
 * producir inquilinos sin administrador en silencio.
 */
@Import(JpaBaseRoleProvider.class)
@DisplayName("Catálogo de roles base — el schema migrado contra MySQL real")
class BaseRoleCatalogPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaBaseRoleProvider provider;

    @Nested
    @DisplayName("Mínimo estructural")
    class MinimoEstructural {

        @Test
        @DisplayName("las migraciones dejan al menos un rol base habilitado")
        void las_migraciones_dejan_al_menos_un_rol_base() {
            assertThat(provider.findAll())
                    .as("base_roles vacía deja el reparto de roles del alta como un no-op "
                            + "silencioso: el dueño nace sin ningún rol y el alta devuelve 201")
                    .isNotEmpty();
        }

        @Test
        @DisplayName("hay un rol obligatorio que el alta pueda auto-asignar al dueño")
        void hay_un_rol_obligatorio_para_el_dueno() {
            List<BaseRoleData> baseRoles = provider.findAll();

            assertThat(baseRoles)
                    .as("sin ninguno mandatory el alta crearía las plantillas y no le asignaría "
                            + "ninguna al dueño: misma cuenta inservible, mismo 201")
                    .anyMatch(BaseRoleData::mandatory);
        }

        @Test
        @DisplayName("el rol obligatorio es ADMIN, que es el código que buscan los seeds de permisos")
        void el_rol_obligatorio_es_admin() {
            // Los seeds 184/191/196/199/202/204/205/209/223/256/257/259/260 atan sus
            // permisos con 'WHERE br.code = ADMIN'. Si el obligatorio se llamara de otra
            // forma insertarían cero filas sin error y el dueño tendría un rol vacío.
            assertThat(provider.findAll()).filteredOn(BaseRoleData::mandatory)
                    .extracting(BaseRoleData::code).contains("ADMIN");
        }
    }
}
