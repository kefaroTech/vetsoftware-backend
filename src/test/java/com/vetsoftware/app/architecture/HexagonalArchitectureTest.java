package com.vetsoftware.app.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Las reglas del {@code CLAUDE.md}, ejecutables.
 *
 * <p>
 * Hay dos clases de regla aquí. Las que el código ya cumple van tal cual:
 * cualquier violación rompe el build. Las que encontraron deuda preexistente
 * van envueltas en {@link FreezingArchRule}, que tolera lo que está registrado
 * en {@code config/archunit/violation-store} y falla ante cualquier violación
 * nueva. Congelar no es perdonar: el store se versiona, se ve en el diff y solo
 * puede encoger.
 *
 * <p>
 * Para bajar deuda basta con arreglar el código y volver a correr el test:
 * ArchUnit quita del store las violaciones resueltas. Cuando una regla llegue a
 * cero, se le quita el {@code FreezingArchRule.freeze(...)} y pasa a ser dura.
 */
@AnalyzeClasses(packages = "com.vetsoftware.app", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // ── Reglas duras: el código ya las cumple ────────────────────────────────

    @ArchTest
    static final ArchRule DOMINIO_SIN_FRAMEWORK = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                    "jakarta.validation..", "com.fasterxml.jackson..", "tools.jackson..")
            .because("el dominio debe poder testearse sin contexto y sin BD");

    // ignoreDependency(origen, destino): lo que se exceptua es depender DE shared,
    // no que shared dependa de otros. RF-11 traia los argumentos al reves.
    @ArchTest
    static final ArchRule SIN_CRUCE_DE_DOMINIOS = slices()
            .matching("com.vetsoftware.app.(*)..domain..").should().notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage("..shared.."))
            .because("cross-feature se resuelve con companion VO (XxxRef), no importando");

    /**
     * El cierre de BE-03: sin esta regla, un puerto nuevo sin gate queda alcanzable
     * por cualquier JWT válido y nada lo detecta. La única excepción admitida es
     * {@link NoAuthorizationRequired}, que obliga a escribir el motivo.
     */
    @ArchTest
    static final ArchRule PUERTOS_AUTORIZADOS = methods().that().areDeclaredInClassesThat()
            .resideInAPackage("..application.port.in..").and().areDeclaredInClassesThat()
            .areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class).should()
            .beAnnotatedWith(PreAuthorize.class)
            .because("la autorizacion vive en el puerto; un puerto nuevo sin gate queda abierto");

    // ── Reglas congeladas: deuda preexistente, cero violaciones nuevas ───────

    @ArchTest
    static final ArchRule APPLICATION_NO_CONOCE_INFRASTRUCTURE = FreezingArchRule
            .freeze(noClasses().that().resideInAPackage("..application..").should()
                    .dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("infrastructure -> application -> domain, nunca al reves"));

    @ArchTest
    static final ArchRule TENANT_DEFENSA_EN_PROFUNDIDAD = FreezingArchRule.freeze(
            methods().that().areDeclaredInClassesThat().resideInAPackage("..application.port.in..")
                    .and().areDeclaredInClassesThat().areInterfaces().and()
                    .areDeclaredInClassesThat().areNotAnnotatedWith(NoAuthorizationRequired.class)
                    .should(VetSoftwareConditions.validarElTenantCuandoRecibeCompanyId())
                    .because("un companyId que no viene del principal es una fuga entre empresas"));

    @ArchTest
    static final ArchRule REPOS_CON_ENTITYGRAPH = FreezingArchRule.freeze(classes().that()
            .areAssignableTo(JpaRepository.class).and().haveSimpleNameEndingWith("JpaRepository")
            .should(VetSoftwareConditions.declararEntityGraphEnLosFinders())
            .because("la regla del CLAUDE.md sobre N+1 debe ser verificable"));

    /**
     * Dura: ya no queda ninguna. La búsqueda se detiene en los saltos
     * {@code @Async}, así que el envío de correos —asíncrono a propósito— no cuenta
     * como HTTP dentro de la transacción.
     */
    @ArchTest
    static final ArchRule SIN_IO_EXTERNO_EN_TRANSACCION = noMethods().that()
            .areAnnotatedWith(Transactional.class)
            .should(VetSoftwareConditions.alcanzarUnClienteHttp(RestClient.class))
            .because("una llamada HTTP retiene la conexion y los locks hasta el commit");

    private static final DescribedPredicate<JavaMethodCall> FIND_ALL_SIN_ARGS = DescribedPredicate
            .describe("es findAll() sin argumentos",
                    call -> "findAll".equals(call.getTarget().getName())
                            && call.getTarget().getRawParameterTypes().isEmpty());

    @ArchTest
    static final ArchRule SIN_FINDALL_SIN_TENANT = FreezingArchRule.freeze(noClasses().that()
            .resideInAPackage("..application.usecase..").should().callMethodWhere(FIND_ALL_SIN_ARGS)
            .because("un findAll() sin companyId es una fuga entre empresas esperando a ocurrir"));
}
