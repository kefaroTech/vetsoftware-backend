package com.vetsoftware.app.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.ManyToOne;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Condiciones de arquitectura propias del proyecto, las que no se expresan con
 * la API fluida de ArchUnit.
 */
final class VetSoftwareConditions {

    private static final String APP_PACKAGE = "com.vetsoftware.app";

    /**
     * Profundidad máxima de la búsqueda transitiva; evita recorrer el grafo entero.
     */
    private static final int MAX_DEPTH = 6;

    private VetSoftwareConditions() {
    }

    /**
     * Detecta si un método alcanza —directamente o a través de la cadena de
     * llamadas del propio proyecto— un cliente HTTP externo.
     *
     * <p>
     * La búsqueda es transitiva a propósito. El caso real que motivó la regla
     * (BE-02) no llamaba al cliente HTTP desde el método transaccional, sino desde
     * un provider dos saltos más abajo; una regla de llamada directa no lo habría
     * visto. Cuando el salto es a una interfaz, se siguen también sus
     * implementaciones, que es donde vive la llamada.
     *
     * @param httpClientTypes
     *            tipos considerados cliente HTTP (p. ej. {@code RestClient})
     */
    static ArchCondition<JavaMethod> alcanzarUnClienteHttp(Class<?>... httpClientTypes) {
        Set<String> clientNames = new HashSet<>();
        for (Class<?> type : httpClientTypes) {
            clientNames.add(type.getName());
        }
        String description = "alcanzar un cliente HTTP externo (" + String.join(", ", clientNames)
                + ") directamente o a traves de la cadena de llamadas";

        return new ArchCondition<>(description) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<String> path = findPathToHttpClient(method, clientNames);
                if (!path.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(method, method.getFullName()
                            + " alcanza un cliente HTTP: " + String.join(" -> ", path)));
                }
            }
        };
    }

    /**
     * Recorre en anchura las llamadas salientes y devuelve la primera ruta que
     * termina en un cliente HTTP, o una lista vacía si no hay ninguna.
     */
    private static List<String> findPathToHttpClient(JavaMethod origin, Set<String> clientNames) {
        Set<String> visited = new HashSet<>();
        Deque<List<JavaMethod>> queue = new ArrayDeque<>();
        queue.add(List.of(origin));
        visited.add(origin.getFullName());

        while (!queue.isEmpty()) {
            List<JavaMethod> path = queue.poll();
            JavaMethod current = path.get(path.size() - 1);

            for (JavaMethodCall call : current.getMethodCallsFromSelf()) {
                JavaClass targetOwner = call.getTargetOwner();
                if (clientNames.contains(targetOwner.getFullName())) {
                    return describe(path, call);
                }
                if (path.size() >= MAX_DEPTH || !isOwnCode(targetOwner)) {
                    continue;
                }
                for (JavaMethod next : resolveTargets(call)) {
                    if (saltaDeHilo(next) || !visited.add(next.getFullName())) {
                        continue;
                    }
                    List<JavaMethod> extended = new ArrayList<>(path);
                    extended.add(next);
                    queue.add(extended);
                }
            }
        }
        return List.of();
    }

    /**
     * Resuelve el destino de una llamada. Si apunta a una interfaz, devuelve además
     * el método homónimo de cada implementación: es ahí donde está el cuerpo que
     * interesa.
     */
    private static List<JavaMethod> resolveTargets(JavaMethodCall call) {
        List<JavaMethod> targets = new ArrayList<>();
        Optional<JavaMethod> resolved = call.getTarget().resolveMember();
        if (resolved.isEmpty()) {
            return targets;
        }
        JavaMethod target = resolved.get();
        targets.add(target);

        JavaClass owner = target.getOwner();
        if (owner.isInterface()) {
            String[] parameterTypeNames = target.getRawParameterTypes().stream()
                    .map(JavaClass::getName).toArray(String[]::new);
            for (JavaClass implementation : owner.getAllSubclasses()) {
                if (!isOwnCode(implementation)) {
                    continue;
                }
                implementation.tryGetMethod(target.getName(), parameterTypeNames)
                        .ifPresent(targets::add);
            }
        }
        return targets;
    }

    private static boolean isOwnCode(JavaClass javaClass) {
        return javaClass.getPackageName().startsWith(APP_PACKAGE);
    }

    /**
     * {@code true} si la llamada cruza a otro hilo vía {@code @Async}.
     *
     * <p>
     * Ahí se corta la búsqueda: el proxy encola la ejecución en otro pool y
     * devuelve inmediatamente, así que lo que pase al otro lado ya no corre dentro
     * de la transacción del caller ni retiene su conexión. Seguir el grafo más allá
     * reporta el envío de correos —que es asíncrono a propósito— como si bloqueara
     * la transacción.
     */
    private static boolean saltaDeHilo(JavaMethod method) {
        return method.isAnnotatedWith(Async.class)
                || method.getOwner().isAnnotatedWith(Async.class);
    }

    /**
     * Exige que un puerto cuyo command transporta {@code companyId} valide el
     * tenant en su {@code @PreAuthorize}.
     *
     * <p>
     * El {@code companyId} lo inyecta el controller desde el principal, pero el
     * puerto es invocable por cualquier otro caller; sin {@code @authz.isMyCompany}
     * un caller que pase otra empresa opera sobre datos ajenos. Es la defensa en
     * profundidad que pide el CLAUDE.md.
     */
    static ArchCondition<JavaMethod> validarElTenantCuandoRecibeCompanyId() {
        return new ArchCondition<>("validar el tenant con @authz.isMyCompany") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!transportaCompanyId(method)) {
                    return;
                }
                Optional<PreAuthorize> gate = method.tryGetAnnotationOfType(PreAuthorize.class);
                boolean valida = gate.isPresent() && gate.get().value().contains("isMyCompany");
                events.add(new SimpleConditionEvent(method, valida,
                        method.getFullName() + " recibe companyId pero su @PreAuthorize "
                                + (gate.isEmpty() ? "no existe" : "no invoca @authz.isMyCompany")));
            }
        };
    }

    /**
     * {@code true} si algún parámetro del método lleva un campo {@code companyId}.
     */
    private static boolean transportaCompanyId(JavaMethod method) {
        for (JavaClass parameter : method.getRawParameterTypes()) {
            if (!isOwnCode(parameter)) {
                continue;
            }
            for (JavaField field : parameter.getAllFields()) {
                if ("companyId".equals(field.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Exige {@code @EntityGraph} en todo finder que devuelva una entidad JPA con
     * asociaciones {@code @ManyToOne}: sin él, cada fila hidrata su proxy LAZY con
     * una consulta aparte (N+1).
     *
     * <p>
     * La regla del CLAUDE.md habla de {@code findAll}/{@code findById}, pero en
     * este código los repositorios multi-tenant no los sobreescriben —declaran su
     * equivalente por empresa, del tipo {@code findAllByCompany_Id}—. Mirar solo
     * los dos nombres canónicos dejaría fuera justamente los finders que se usan.
     * Una {@code @Query} propia también vale: ahí el {@code JOIN FETCH} se escribe
     * a mano.
     */
    static ArchCondition<JavaClass> declararEntityGraphEnLosFinders() {
        return new ArchCondition<>(
                "declarar @EntityGraph en los finders que devuelven la entidad") {
            @Override
            public void check(JavaClass repository, ConditionEvents events) {
                Optional<JavaClass> entity = entidadDe(repository);
                if (entity.isEmpty() || !tieneManyToOne(entity.get())) {
                    return;
                }
                for (JavaMethod finder : repository.getMethods()) {
                    if (!devuelveLaEntidad(finder, entity.get())) {
                        continue;
                    }
                    boolean resuelto = finder.isAnnotatedWith(EntityGraph.class)
                            || finder.isAnnotatedWith(Query.class);
                    events.add(new SimpleConditionEvent(finder, resuelto,
                            repository.getSimpleName() + "." + finder.getName() + "() devuelve "
                                    + entity.get().getSimpleName()
                                    + " (con @ManyToOne) sin @EntityGraph ni @Query: N+1"));
                }
            }
        };
    }

    /**
     * {@code true} si el método devuelve la entidad, sola o envuelta en
     * Optional/List/Page.
     */
    private static boolean devuelveLaEntidad(JavaMethod method, JavaClass entity) {
        JavaType returnType = method.getReturnType();
        if (returnType.toErasure().equals(entity)) {
            return true;
        }
        if (returnType instanceof JavaParameterizedType parameterized) {
            return parameterized.getActualTypeArguments().stream()
                    .anyMatch(argument -> argument.toErasure().equals(entity));
        }
        return false;
    }

    /**
     * Primer argumento genérico de {@code JpaRepository<Entidad, Id>}, si se
     * declara directo.
     */
    private static Optional<JavaClass> entidadDe(JavaClass repository) {
        for (JavaType implemented : repository.getInterfaces()) {
            if (!(implemented instanceof JavaParameterizedType parameterized)) {
                continue;
            }
            if (!parameterized.toErasure().isAssignableTo(JpaRepository.class)) {
                continue;
            }
            List<JavaType> arguments = parameterized.getActualTypeArguments();
            if (!arguments.isEmpty()) {
                return Optional.of(arguments.get(0).toErasure());
            }
        }
        return Optional.empty();
    }

    private static boolean tieneManyToOne(JavaClass entity) {
        return entity.getAllFields().stream().anyMatch(f -> f.isAnnotatedWith(ManyToOne.class));
    }

    private static List<String> describe(List<JavaMethod> path, JavaMethodCall finalCall) {
        List<String> steps = new ArrayList<>();
        for (JavaMethod method : path) {
            steps.add(method.getOwner().getSimpleName() + "." + method.getName() + "()");
        }
        steps.add(finalCall.getTargetOwner().getSimpleName() + "." + finalCall.getTarget().getName()
                + "()");
        return steps;
    }
}
