package com.vetsoftware.app.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.Constraint;
import jakarta.validation.Valid;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

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

    /** Captura la expresión que recibe {@code @authz.isMyCompany(#…)}. */
    private static final Pattern ISMYCOMPANY_REF = Pattern
            .compile("isMyCompany\\(\\s*#([A-Za-z0-9_.]+)");

    /** Disyunto que deja pasar al principal SYSTEM, cross-tenant por diseño. */
    private static final String SYSTEM_DISJUNCT = "hasRole('SYSTEM')";

    private static final String PORT_IN_PACKAGE = ".application.port.in";

    private static final String PORT_OUT_PACKAGE = ".application.port.out";

    /** Envoltorio de página de la capa de aplicación; hay exactamente uno. */
    private static final String PAGE_RESULT = "PageResult";

    /**
     * Los nombres con los que se ha declarado —o se podría volver a declarar— el
     * concepto «página». No es una lista de prohibidos: es la firma del hallazgo
     * BE-21, donde treinta y cinco features escribieron el mismo record con el
     * mismo nombre en su propio paquete.
     */
    private static final Set<String> NOMBRES_DE_PAGINA = Set.of(PAGE_RESULT, "PageResponse",
            "PagedResult", "PagedResponse", "PageDto", "Slice", "SliceResult");

    /** El {@code PageRequest} de Spring Data: el que hay que acotar. */
    private static final String PAGE_REQUEST = "org.springframework.data.domain.PageRequest";

    /** La API del commit: quien la toca sabe a qué lado del commit está. */
    private static final String TRANSACTION_SYNCHRONIZATION_MANAGER = TransactionSynchronizationManager.class
            .getName();

    private VetSoftwareConditions() {
    }

    /**
     * Las clases que declaran el concepto «página» por su nombre. Mira el nombre y
     * no la forma a propósito: la copia 37 se llamará igual que las 36 anteriores,
     * y para cuando difiera en un campo ya será otra deuda distinta.
     */
    static DescribedPredicate<JavaClass> declaranElConceptoDePagina() {
        return DescribedPredicate.describe("declaran el concepto «pagina»",
                clazz -> NOMBRES_DE_PAGINA.contains(clazz.getSimpleName()));
    }

    /**
     * Toda llamada a {@code PageRequest.of(...)}. Fuera del kernel de paginación no
     * hay ninguna legítima: el índice hay que normalizarlo y el tamaño hay que
     * toparlo, y eso ya lo hace {@code Pages.request(...)}.
     */
    static DescribedPredicate<JavaMethodCall> esUnPageRequestSinAcotar() {
        return DescribedPredicate.describe("construye un PageRequest fuera del kernel",
                call -> PAGE_REQUEST.equals(call.getTargetOwner().getFullName())
                        && "of".equals(call.getTarget().getName()));
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
                List<String> path = rutaHasta(method,
                        call -> clientNames.contains(call.getTargetOwner().getFullName()),
                        VetSoftwareConditions::saltaDeHilo);
                if (!path.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(method, method.getFullName()
                            + " alcanza un cliente HTTP: " + String.join(" -> ", path)));
                }
            }
        };
    }

    /**
     * La otra mitad de {@link #alcanzarUnClienteHttp(Class...)}, y el cierre de
     * BE-18: detecta el efecto {@code @Async} que se dispara <em>dentro</em> de una
     * transacción sin esperar a que confirme.
     *
     * <p>
     * Las dos reglas miran el mismo salto y sacan conclusiones opuestas, a
     * propósito. Aquella se detiene ahí porque lo que cruza de hilo ya no retiene
     * la conexión ni los locks del caller: deja de ser su problema. Esta empieza
     * justo ahí porque lo que cruza de hilo <b>tampoco vuelve</b>. El proxy encola
     * la tarea al instante y el pool la ejecuta cuando quiere —normalmente antes de
     * que el caller haya hecho flush—, así que un rollback posterior no deshace
     * nada: el correo ya salió.
     *
     * <p>
     * El caso real: {@code CreateAppointmentService.execute} enviaba la
     * confirmación desde dentro de la transacción, y {@code AppointmentJpaEntity}
     * tiene {@code @Version}, cuyo choque optimista salta en el flush,
     * <i>después</i> de la última línea del método. El cliente recibía la
     * confirmación de una cita que nunca existió.
     *
     * <p>
     * <b>Dónde se detiene la búsqueda.</b> En el primer método que habla con
     * {@code TransactionSynchronizationManager} —el origen incluido—. Ese método
     * está decidiendo explícitamente qué ocurre a cada lado del commit; si lo
     * decide mal, eso lo ve una revisión, no una regla de arquitectura. Sin este
     * corte la regla marcaría el propio patrón correcto: la rama de guarda de
     * {@code sendAfterCommit} —la que envía en el acto cuando no hay transacción
     * activa, porque {@code registerSynchronization} lanzaría ahí— es una llamada
     * directa al port como cualquier otra.
     *
     * <p>
     * <b>Por qué el callback no se comprueba.</b> No hace falta: el cuerpo de un
     * {@code afterCommit} vive en una clase anónima aparte, así que no cuelga del
     * método transaccional y el recorrido nunca llega. Es la misma razón por la que
     * el patrón es correcto en ejecución. El día que alguien difiera con un
     * <i>lambda</i>, ArchUnit atribuirá su cuerpo al método que lo declara y la
     * regla dará un falso positivo: la salida es extraer el diferido a su propio
     * método, como hacen hoy los dos que existen.
     */
    static ArchCondition<JavaMethod> alcanzarUnEfectoAsincrono() {
        return new ArchCondition<>("disparar un efecto @Async sin esperar al commit") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (difiereAlCommit(method)) {
                    return;
                }
                List<String> path = rutaHasta(method, VetSoftwareConditions::saltaAOtroHilo,
                        VetSoftwareConditions::difiereAlCommit);
                if (!path.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(method,
                            method.getFullName() + " dispara un efecto @Async dentro de la"
                                    + " transaccion: " + String.join(" -> ", path)));
                }
            }
        };
    }

    /**
     * Recorre en anchura las llamadas salientes del proyecto y devuelve la primera
     * ruta que llega al destino, o una lista vacía si no hay ninguna.
     *
     * <p>
     * La búsqueda es transitiva a propósito. El caso real que motivó la primera
     * regla que la usa (BE-02) no llamaba al cliente HTTP desde el método
     * transaccional, sino desde un provider dos saltos más abajo; una regla de
     * llamada directa no lo habría visto. Cuando el salto es a una interfaz, se
     * siguen también sus implementaciones, que es donde vive la llamada.
     *
     * @param esElDestino
     *            qué llamada cierra la búsqueda
     * @param cortaLaBusqueda
     *            métodos en los que no se sigue bajando
     */
    private static List<String> rutaHasta(JavaMethod origin, Predicate<JavaMethodCall> esElDestino,
            Predicate<JavaMethod> cortaLaBusqueda) {
        Set<String> visited = new HashSet<>();
        Deque<List<JavaMethod>> queue = new ArrayDeque<>();
        queue.add(List.of(origin));
        visited.add(origin.getFullName());

        while (!queue.isEmpty()) {
            List<JavaMethod> path = queue.poll();
            JavaMethod current = path.get(path.size() - 1);

            for (JavaMethodCall call : current.getMethodCallsFromSelf()) {
                if (esElDestino.test(call)) {
                    return describe(path, call);
                }
                if (path.size() >= MAX_DEPTH || !isOwnCode(call.getTargetOwner())) {
                    continue;
                }
                for (JavaMethod next : resolveTargets(call)) {
                    if (cortaLaBusqueda.test(next) || !visited.add(next.getFullName())) {
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
     * {@code true} si la llamada entrega el trabajo a otro hilo vía {@code @Async}.
     */
    private static boolean saltaAOtroHilo(JavaMethodCall call) {
        return resolveTargets(call).stream().anyMatch(VetSoftwareConditions::saltaDeHilo);
    }

    /**
     * {@code true} si el método habla con
     * {@code TransactionSynchronizationManager}, en cualquiera de sus dos formas:
     * registrar el callback o preguntar si hay sincronización activa. Las dos
     * delatan a un método que sabe dónde está el commit.
     */
    private static boolean difiereAlCommit(JavaMethod method) {
        for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
            if (TRANSACTION_SYNCHRONIZATION_MANAGER.equals(call.getTargetOwner().getFullName())) {
                return true;
            }
        }
        return false;
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
     * El proxy encola la ejecución en otro pool y devuelve inmediatamente, así que
     * lo que pase al otro lado ya no corre dentro de la transacción del caller. De
     * ahí que las dos reglas que usan este predicado lo lean al revés:
     * {@link #alcanzarUnClienteHttp(Class...)} <b>corta</b> aquí, porque más allá
     * del salto ya no se retiene la conexión ni los locks;
     * {@link #alcanzarUnEfectoAsincrono()} <b>reporta</b> aquí, porque más allá del
     * salto el efecto ya no se puede deshacer.
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
                if (gate.isPresent() && soloAlcanzablePorSystem(gate.get().value())) {
                    return;
                }
                if (gate.isEmpty()) {
                    events.add(new SimpleConditionEvent(method, false,
                            method.getFullName() + " recibe companyId y no tiene @PreAuthorize"));
                    return;
                }
                String motivo = revisarGate(method, gate.get().value());
                events.add(new SimpleConditionEvent(method, motivo == null, method.getFullName()
                        + " recibe companyId pero su @PreAuthorize " + motivo));
            }
        };
    }

    /**
     * {@code null} si el gate valida el tenant correctamente; si no, qué le pasa.
     *
     * <p>
     * No basta con que aparezca {@code isMyCompany}: hay que comprobar a qué
     * apunta. SpEL resuelve un {@code #parametroInexistente} a {@code null} sin
     * avisar, {@code isMyCompany(null)} devuelve {@code false} y la regla queda
     * siempre falsa — el endpoint se cierra para todos y nadie entiende por qué. Es
     * la trampa que advierte el CLAUDE.md, y con decenas de anotaciones editadas a
     * la vez es fácil de sembrar.
     */
    private static String revisarGate(JavaMethod method, String expression) {
        Matcher reference = ISMYCOMPANY_REF.matcher(expression);
        if (!reference.find()) {
            return "no invoca @authz.isMyCompany";
        }
        String root = reference.group(1).split("\\.")[0];
        Optional<Set<String>> parameterNames = nombresDeParametro(method);
        if (parameterNames.isPresent() && !parameterNames.get().contains(root)) {
            return "invoca @authz.isMyCompany(#" + reference.group(1) + "), y '" + root
                    + "' no es un parametro del metodo: SpEL lo resuelve a null y el gate"
                    + " queda siempre en false";
        }
        return null;
    }

    /**
     * {@code true} si al gate solo llega un principal SYSTEM, que es cross-tenant
     * por diseño. Ahí el conjunto del tenant seria codigo muerto: ningun empleado
     * alcanza el metodo.
     */
    private static boolean soloAlcanzablePorSystem(String expression) {
        return Arrays.stream(expression.split("\\bor\\b")).map(part -> part.replaceAll("\\s", ""))
                .allMatch(part -> part.isEmpty() || SYSTEM_DISJUNCT.equals(part));
    }

    /**
     * Vacío si la clase no se puede reflexionar: sin nombres no hay nada que
     * validar.
     */
    private static Optional<Set<String>> nombresDeParametro(JavaMethod method) {
        try {
            Set<String> names = new HashSet<>();
            for (Parameter parameter : method.reflect().getParameters()) {
                names.add(parameter.getName());
            }
            return Optional.of(names);
        } catch (RuntimeException | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
    }

    /**
     * {@code true} si el método recibe la empresa, de cualquiera de las dos formas
     * que se usan en el proyecto: dentro de un command
     * ({@code execute(CreateXxxCommand)}, con un campo {@code companyId}) o como
     * parámetro suelto ({@code find(Long companyId, Long id)}).
     *
     * <p>
     * Las dos cuentan. Mirar solo los commands deja fuera los Find/List/Delete, que
     * es justo donde estaban los hallazgos financieros y clínicos de BE-08; mirar
     * solo los parámetros sueltos deja fuera los Create/Update. Son conjuntos casi
     * disjuntos.
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
        return tieneParametroLlamadoCompanyId(method);
    }

    /**
     * ArchUnit no expone los nombres de parámetro, así que se reflexiona la clase
     * real. Funciona porque Spring Boot compila con {@code -parameters}; si no
     * estuviera, los nombres serían {@code arg0}, {@code arg1}… y este chequeo
     * simplemente no encontraría nada.
     */
    private static boolean tieneParametroLlamadoCompanyId(JavaMethod method) {
        try {
            for (Parameter parameter : method.reflect().getParameters()) {
                if ("companyId".equals(parameter.getName())) {
                    return true;
                }
            }
        } catch (RuntimeException | NoClassDefFoundError ignored) {
            // La clase no se puede cargar; el chequeo por command sigue aplicando.
        }
        return false;
    }

    /**
     * Exige que un listado que no filtra por empresa quede cerrado a
     * {@code ROLE_SYSTEM}.
     *
     * <p>
     * Es el hueco por el que se coló BE-29.
     * {@link #validarElTenantCuandoRecibeCompanyId()} mira los puertos <em>que
     * reciben</em> un {@code companyId}: si el método no recibe ninguno no hay nada
     * que validar, así que el puerto pasa limpio aunque por debajo esté sirviendo
     * filas de todas las empresas. Desde esa regla, un catálogo global legítimo y
     * una fuga entre empresas se ven exactamente igual.
     *
     * <p>
     * Lo que los distingue es el repositorio. Si <strong>sabe</strong> filtrar por
     * empresa —declara algún método que recibe {@code companyId}, del tipo
     * {@code findByIdAndCompanyId}— entonces sus filas son de alguien, y servirlas
     * sin ese filtro a un permiso de empleado es una fuga. Los catálogos maestros
     * (ciudades, razas, especies) no declaran ningún método así y la regla ni los
     * mira.
     *
     * <p>
     * A propósito no se comprueba la <em>forma</em> de la llamada: da igual que sea
     * {@code findAll()}, {@code findAll(page, size)} o
     * {@code findAllByAnimalId(id, …)}. Paginar un listado no lo hace multi-tenant,
     * y acotarlo por una FK ajena tampoco: mientras el {@code WHERE} no nombre la
     * empresa, las filas que salen son de cualquiera.
     */
    static ArchCondition<JavaClass> cerrarASystemLosListadosSinEmpresa() {
        return new ArchCondition<>("cerrar a ROLE_SYSTEM los listados que no filtran por empresa") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                for (JavaMethod method : service.getMethods()) {
                    Optional<JavaMethod> puerto = puertoQueImplementa(method);
                    if (puerto.isEmpty() || transportaCompanyId(puerto.get())) {
                        continue;
                    }
                    for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                        Optional<JavaMethod> listado = listadoSinEmpresa(call);
                        if (listado.isPresent()) {
                            events.add(evaluarElGate(puerto.get(), method, listado.get()));
                        }
                    }
                }
            }
        };
    }

    /**
     * El método homónimo del puerto de entrada que este service implementa, si lo
     * hay. Un método que no implementa ningún puerto no es alcanzable desde fuera:
     * su gate es el del puerto que acabe llamándolo.
     */
    private static Optional<JavaMethod> puertoQueImplementa(JavaMethod method) {
        String[] parameterTypeNames = method.getRawParameterTypes().stream().map(JavaClass::getName)
                .toArray(String[]::new);
        for (JavaClass implemented : method.getOwner().getAllRawInterfaces()) {
            if (!implemented.getPackageName().contains(PORT_IN_PACKAGE)) {
                continue;
            }
            Optional<JavaMethod> candidato = implemented.tryGetMethod(method.getName(),
                    parameterTypeNames);
            if (candidato.isPresent()) {
                return candidato;
            }
        }
        return Optional.empty();
    }

    /**
     * La llamada, si es a un listado sin empresa de un repositorio que sí sabe
     * filtrar por ella.
     */
    private static Optional<JavaMethod> listadoSinEmpresa(JavaMethodCall call) {
        JavaClass repositorio = call.getTargetOwner();
        if (!isOwnCode(repositorio) || !repositorio.getPackageName().contains(PORT_OUT_PACKAGE)) {
            return Optional.empty();
        }
        Optional<JavaMethod> destino = call.getTarget().resolveMember();
        if (destino.isEmpty() || !esUnFinderDeVariasFilas(destino.get())
                || filtraPorEmpresa(destino.get())) {
            return Optional.empty();
        }
        boolean sabeFiltrarPorEmpresa = repositorio.getMethods().stream()
                .anyMatch(VetSoftwareConditions::filtraPorEmpresa);
        return sabeFiltrarPorEmpresa ? destino : Optional.empty();
    }

    private static SimpleConditionEvent evaluarElGate(JavaMethod puerto, JavaMethod servicio,
            JavaMethod listado) {
        Optional<PreAuthorize> gate = puerto.tryGetAnnotationOfType(PreAuthorize.class);
        boolean cerrado = gate.isPresent() && soloAlcanzablePorSystem(gate.get().value());
        String hecho = servicio.getOwner().getSimpleName() + "." + servicio.getName() + "() sirve "
                + listado.getOwner().getSimpleName() + "." + listado.getName()
                + "(), que no filtra por empresa, y su puerto no recibe companyId";
        return new SimpleConditionEvent(servicio, cerrado,
                cerrado
                        ? hecho + ", pero solo lo alcanza ROLE_SYSTEM"
                        : hecho + " ni esta cerrado a ROLE_SYSTEM: "
                                + gate.map(PreAuthorize::value).orElse("sin @PreAuthorize"));
    }

    /**
     * {@code true} si el método lleva la empresa consigo. El nombre cuenta porque
     * los repositorios la declaran ahí ({@code findByIdAndCompanyId},
     * {@code findAllAvailableForCompany}) y así el chequeo no depende de que la
     * clase se pueda reflexionar para leer los nombres de parámetro.
     */
    private static boolean filtraPorEmpresa(JavaMethod method) {
        return transportaCompanyId(method) || method.getName().contains("Company");
    }

    /**
     * Solo los <em>finders</em> que devuelven varias filas. El nombre importa: un
     * {@code saveAll} también devuelve una colección y no es un listado.
     */
    private static boolean esUnFinderDeVariasFilas(JavaMethod method) {
        if (!method.getName().startsWith("find")) {
            return false;
        }
        JavaClass returnType = method.getRawReturnType();
        return returnType.isAssignableTo(Collection.class)
                || PAGE_RESULT.equals(returnType.getSimpleName());
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

    // ── BE-COV: la familia de fugas «por id», hermana de BE-29 ───────────────

    /**
     * La entidad que <em>es</em> el tenant. Nada se acota contra sí mismo: un
     * {@code UPDATE companies … WHERE id = :id} no puede llevar {@code company_id}
     * porque esa columna no existe, y el id de la fila ya es el de la empresa.
     */
    private static final String COMPANY_ENTITY = "CompanyJpaEntity";

    private static final String SUFIJO_ENTIDAD_JPA = "JpaEntity";

    /**
     * Un statement que escribe. El {@code SELECT … FOR UPDATE} de
     * {@code NumberingResolutionJpaRepository} no empieza por aquí, que es
     * exactamente lo que se busca: bloquea filas, no las modifica.
     */
    private static final Pattern SENTENCIA_QUE_ESCRIBE = Pattern.compile("^(update|delete)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * La empresa nombrada en la consulta, en cualquiera de las tres formas que usa
     * el proyecto: {@code company_id} en SQL nativo —también dentro de un
     * {@code EXISTS} correlacionado o tras un alias, {@code r.company_id}—,
     * {@code companyId} en JPQL y {@code company.id} en una ruta de propiedad.
     */
    private static final Pattern EMPRESA_EN_LA_CONSULTA = Pattern
            .compile("company_id|companyId|company\\.id", Pattern.CASE_INSENSITIVE);

    /** Sufijo con el que los puertos de salida declaran la variante acotada. */
    private static final String SUFIJO_ACOTADO = "AndCompanyId";

    /**
     * Saltos de asociación que se siguen buscando la empresa desde una entidad.
     * Cinco cubre la cadena más larga del esquema
     * ({@code medication_schedules → hospitalization_medications → hospitalizations
     * → companies}) con margen, y evita recorrer el grafo entero.
     */
    private static final int MAX_SALTOS_DE_ASOCIACION = 5;

    /**
     * Exige que un puerto de entrada que señala <em>una fila concreta por su
     * id</em> reciba también la empresa, salvo que solo lo alcance
     * {@code ROLE_SYSTEM}.
     *
     * <p>
     * Es la tercera pieza del trío, y la que faltaba.
     * {@link #validarElTenantCuandoRecibeCompanyId()} cubre los puertos que
     * <em>sí</em> reciben un {@code companyId} y obliga a validarlo;
     * {@link #cerrarASystemLosListadosSinEmpresa()} cubre los que no reciben
     * ninguno y devuelven <em>varias</em> filas. Entre las dos quedaba un hueco del
     * tamaño de una campaña: las operaciones que no reciben empresa y actúan sobre
     * <em>una</em> fila, señalada por un id que el atacante escribe en la URL.
     * ArchUnit pasó 13/13 en verde con sesenta y cinco de esas repartidas por
     * veintisiete features.
     *
     * <p>
     * <b>El defecto, con nombre.</b> {@code DELETE /employee-roles/{id}} revocaba
     * el rol del administrador de otra empresa;
     * {@code PATCH /employees/{id}/enable} le devolvía el login a quien otro tenant
     * había despedido; y {@code GET /laboratory-test-files/{id}/download} —una
     * lectura, no una escritura— entregaba el PDF de un resultado de laboratorio
     * ajeno. Los tres llevaban su {@code @PreAuthorize} con su permiso correcto: el
     * permiso dice <em>qué</em> puede hacer el empleado, nunca <em>sobre qué
     * filas</em>.
     *
     * <p>
     * <b>Por qué no se mira el verbo.</b> Una lista de prefijos ({@code Delete…},
     * {@code Reactivate…}, {@code Suspend…}) envejece mal: se rompe el día que
     * alguien escribe {@code ArchiveXxxUseCase}, y además dejaría fuera el
     * {@code Download…} y los dos
     * {@code List…ByHospitalization(Long hospitalizationId)}, que leen filas de
     * otro tenant sin escribir nada. Lo que se mira es la firma: un parámetro
     * {@code Long} —el CLAUDE.md fija que todo id de entidad es {@code Long}, así
     * que el {@code int page} de la paginación no cuenta— o un command/query con un
     * campo {@code id}.
     *
     * <p>
     * <b>El discriminador que evita los catálogos maestros.</b> Igual que en BE-29,
     * lo que separa una fuga de una operación global legítima no es el nombre sino
     * el dato: si ninguna entidad JPA de la feature alcanza
     * {@code CompanyJpaEntity} —ni directamente ni por asociaciones—, sus filas no
     * son de nadie y la regla ni las mira. Así {@code ReactivateCountryUseCase},
     * {@code ReactivateModuleUseCase} o {@code ReactivateSpaTypeUseCase} (los
     * {@code spa_types} son globales, no por empresa) quedan fuera sin enumerar
     * excepciones, mientras {@code DeleteDiagnosticImagingTypeUseCase} —cuya tabla
     * sí tiene {@code company_id}— entra.
     */
    static ArchCondition<JavaMethod> acotarPorEmpresaLasOperacionesPorId() {
        return new ArchCondition<>(
                "recibir la empresa cuando la operacion senala una fila" + " concreta por su id") {
            @Override
            public void check(JavaMethod puerto, ConditionEvents events) {
                if (transportaCompanyId(puerto) || !senalaUnaFilaPorId(puerto)) {
                    return;
                }
                Optional<PreAuthorize> gate = puerto.tryGetAnnotationOfType(PreAuthorize.class);
                if (gate.isPresent() && soloAlcanzablePorSystem(gate.get().value())) {
                    return;
                }
                if (!laFeatureTieneDatosDeEmpresa(puerto.getOwner())) {
                    return;
                }
                events.add(new SimpleConditionEvent(puerto, false,
                        puerto.getFullName()
                                + " senala una fila por id y no recibe companyId, pero sus filas"
                                + " pertenecen a una empresa y el gate deja entrar a un empleado: "
                                + gate.map(PreAuthorize::value).orElse("sin @PreAuthorize")));
            }
        };
    }

    /**
     * {@code true} si la firma basta para señalar una fila concreta: un
     * {@code Long} suelto —todo id de entidad lo es, por la regla del CLAUDE.md— o
     * un command/query con un campo {@code id}.
     *
     * <p>
     * El {@code int} queda fuera a propósito: en este proyecto los enteros
     * primitivos de un puerto son {@code page} y {@code pageSize}, nunca un id.
     */
    private static boolean senalaUnaFilaPorId(JavaMethod method) {
        for (JavaClass parameter : method.getRawParameterTypes()) {
            if (Long.class.getName().equals(parameter.getName())) {
                return true;
            }
            if (isOwnCode(parameter)
                    && parameter.getAllFields().stream().anyMatch(f -> "id".equals(f.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code true} si alguna entidad JPA de la feature a la que pertenece esta
     * clase alcanza la empresa. Es la pregunta «¿estas filas son de alguien?» hecha
     * al esquema en vez de al nombre del paquete.
     */
    private static boolean laFeatureTieneDatosDeEmpresa(JavaClass clazz) {
        return paqueteDeLaFeature(clazz).map(feature -> feature.getClassesInPackageTree().stream()
                .filter(c -> c.getSimpleName().endsWith(SUFIJO_ENTIDAD_JPA))
                .anyMatch(VetSoftwareConditions::perteneceAUnaEmpresa)).orElse(false);
    }

    /**
     * El paquete raíz de la rodaja vertical
     * ({@code com.vetsoftware.app.spa.application.port.in} →
     * {@code com.vetsoftware.app.spa}).
     */
    private static Optional<JavaPackage> paqueteDeLaFeature(JavaClass clazz) {
        String nombre = clazz.getPackageName();
        if (!nombre.startsWith(APP_PACKAGE + ".")) {
            return Optional.empty();
        }
        int fin = nombre.indexOf('.', APP_PACKAGE.length() + 1);
        String feature = fin < 0 ? nombre : nombre.substring(0, fin);
        JavaPackage paquete = clazz.getPackage();
        while (!paquete.getName().equals(feature)) {
            Optional<JavaPackage> padre = paquete.getParent();
            if (padre.isEmpty()) {
                return Optional.empty();
            }
            paquete = padre.get();
        }
        return Optional.of(paquete);
    }

    /**
     * {@code true} si las filas de esta entidad son de una empresa: o lleva la
     * empresa encima ({@code company}/{@code companyId}) o la alcanza siguiendo sus
     * asociaciones a otras entidades JPA.
     *
     * <p>
     * La búsqueda es transitiva porque el esquema no es plano: una
     * {@code medication_schedules} no tiene {@code company_id} —su empresa vive
     * tres saltos más arriba, en {@code hospitalizations}— y es justo una de las
     * tablas donde hubo fuga. Mirar solo la columna propia habría dado por «global»
     * a la mitad de las tablas clínicas.
     *
     * <p>
     * {@code CompanyJpaEntity} sale {@code false} y eso es correcto: la empresa no
     * pertenece a ninguna empresa, y sus operaciones son territorio de SYSTEM. Lo
     * mismo los catálogos maestros —{@code countries}, {@code states},
     * {@code cities}, {@code modules}, {@code memberships}, {@code base_roles}, los
     * {@code system_*}—, que no llegan a la empresa por ningún camino.
     */
    private static boolean perteneceAUnaEmpresa(JavaClass entidad) {
        Set<String> visitadas = new HashSet<>();
        Deque<JavaClass> nivel = new ArrayDeque<>();
        nivel.add(entidad);
        visitadas.add(entidad.getFullName());

        for (int salto = 0; salto < MAX_SALTOS_DE_ASOCIACION && !nivel.isEmpty(); salto++) {
            Deque<JavaClass> siguiente = new ArrayDeque<>();
            for (JavaClass actual : nivel) {
                for (JavaField campo : actual.getAllFields()) {
                    JavaClass tipo = campo.getRawType();
                    if (COMPANY_ENTITY.equals(tipo.getSimpleName())
                            || "companyId".equals(campo.getName())) {
                        return true;
                    }
                    if (isOwnCode(tipo) && tipo.getSimpleName().endsWith(SUFIJO_ENTIDAD_JPA)
                            && visitadas.add(tipo.getFullName())) {
                        siguiente.add(tipo);
                    }
                }
            }
            nivel = siguiente;
        }
        return false;
    }

    /**
     * Exige que toda {@code @Query} que escribe nombre la empresa, en los
     * repositorios cuyas filas son de alguien.
     *
     * <p>
     * <b>Por qué esta es la peor de las tres.</b> En un {@code delete} o un
     * {@code update} corriente hay una lectura previa que valida la propiedad, así
     * que el SQL es la segunda barrera. En un {@code reactivate} no la hay: el
     * servicio decide si la fila existe mirando <em>cuántas filas actualizó</em>,
     * de modo que el {@code WHERE} <b>es</b> toda la seguridad. Un
     * {@code UPDATE employee_roles SET enabled = true WHERE id = :id} devolvía el
     * rol revocado a un empleado de otra empresa y además invalidaba su caché de
     * permisos: escalada de privilegios cross-tenant escrita en cuatro líneas de
     * SQL.
     *
     * <p>
     * El valor de la anotación viaja en el bytecode, así que ArchUnit puede leer el
     * statement. Vale cualquiera de las formas correctas del proyecto: la columna
     * directa ({@code RoleJpaRepository}), el {@code JOIN} contra la tabla padre
     * ({@code RolePermissionJpaRepository}) y el {@code EXISTS} correlacionado para
     * cuando la fila no tiene {@code company_id} propio
     * ({@code DebtOpenAccountJpaRepository},
     * {@code HospitalizationProcedureJpaRepository}).
     *
     * <p>
     * <b>Las dos exenciones, y por qué son estructurales y no una lista.</b>
     * <ul>
     * <li><b>El repositorio cuyas filas no son de nadie.</b> Si su entidad no
     * alcanza {@code CompanyJpaEntity}, no hay empresa que nombrar:
     * {@code UPDATE countries …} o {@code UPDATE companies …} no pueden acotarse y
     * la regla ni los mira.</li>
     * <li><b>La hermana acotada del mismo nombre.</b> El camino SYSTEM necesita un
     * statement sin filtro, y el proyecto lo declara como sobrecarga:
     * {@code reactivate(id)} junto a {@code reactivate(id, companyId)}, elegidas
     * por el {@code companyId == null} del servicio. Sin esta exención la regla
     * marcaría el patrón corregido —{@code EmployeeJpaRepository} y
     * {@code RolePermissionJpaRepository} lo tienen ya— y nadie podría ponerla en
     * verde. Cuando la sobrecarga acotada <em>no</em> existe, el statement sin
     * filtro es el único camino y por él pasan todos, SYSTEM y empleado.</li>
     * </ul>
     */
    static ArchCondition<JavaClass> acotarPorEmpresaElSqlQueEscribe() {
        return new ArchCondition<>("nombrar la empresa en toda @Query que escribe") {
            @Override
            public void check(JavaClass repositorio, ConditionEvents events) {
                Optional<JavaClass> entidad = entidadDe(repositorio);
                if (entidad.isEmpty() || !perteneceAUnaEmpresa(entidad.get())) {
                    return;
                }
                for (JavaMethod metodo : repositorio.getMethods()) {
                    Optional<String> sentencia = sentenciaQueEscribe(metodo);
                    if (sentencia.isEmpty() || nombraLaEmpresa(sentencia.get())) {
                        continue;
                    }
                    boolean caminoSystem = tieneHermanaAcotada(repositorio, metodo);
                    String desenlace = caminoSystem
                            ? ", pero declara la sobrecarga acotada: es el camino SYSTEM"
                            : " y no hay sobrecarga acotada que lo separe del camino del"
                                    + " empleado: " + unaLinea(sentencia.get());
                    events.add(new SimpleConditionEvent(metodo, caminoSystem,
                            repositorio.getSimpleName() + "." + metodo.getName()
                                    + "() escribe sin nombrar la empresa" + desenlace));
                }
            }
        };
    }

    /** El statement de una {@code @Query} que escribe, si el método declara una. */
    private static Optional<String> sentenciaQueEscribe(JavaMethod method) {
        return method.tryGetAnnotationOfType(Query.class).map(Query::value).map(String::strip)
                .filter(sql -> SENTENCIA_QUE_ESCRIBE.matcher(sql).find());
    }

    private static boolean nombraLaEmpresa(String sentencia) {
        return EMPRESA_EN_LA_CONSULTA.matcher(sentencia).find();
    }

    /**
     * {@code true} si el repositorio declara otro método <em>del mismo nombre</em>
     * cuya {@code @Query} sí nombra la empresa. Esa sobrecarga es la prueba de que
     * el camino del tenant existe y va por otro sitio.
     */
    private static boolean tieneHermanaAcotada(JavaClass repositorio, JavaMethod metodo) {
        for (JavaMethod otro : repositorio.getMethods()) {
            if (otro.equals(metodo) || !otro.getName().equals(metodo.getName())) {
                continue;
            }
            if (sentenciaQueEscribe(otro).filter(VetSoftwareConditions::nombraLaEmpresa)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static String unaLinea(String sentencia) {
        return sentencia.replaceAll("\\s+", " ").strip();
    }

    /**
     * Exige que un caso de uso que carga por un puerto de salida use la variante
     * acotada por empresa <em>cuando el puerto la ofrece</em>.
     *
     * <p>
     * <b>Este es el que ningún humano ve en revisión.</b> Doce {@code Update…}
     * llevaban {@code @authz.isMyCompany(#command.companyId)} y eran vulnerables
     * igualmente. Esa anotación solo prueba que el atacante declara <em>su
     * propia</em> empresa —cosa que siempre hace, porque el controller la inyecta
     * desde el principal—; no dice nada sobre de quién es la fila que se va a
     * cargar. Como el servicio hacía {@code findById(command.id())} y después
     * {@code entidad.update(…, company)}, el efecto no era un rechazo sino una
     * <b>apropiación</b>: la fila de la empresa B pasaba a ser de A. La anotación
     * «se ve bien» y el defecto está debajo.
     *
     * <p>
     * <b>El matiz que distingue el patrón bueno de la fuga.</b> El camino SYSTEM
     * legítimo es un ternario —{@code companyId == null ? findById(id) :
     * findByIdAndCompanyId(id, companyId)}— que llama a las <em>dos</em> variantes.
     * Por eso la regla no prohíbe {@code findById}: exige que la clase llame además
     * a la acotada. La fuga es la clase que solo conoce la variante ancha.
     * Referencia canónica: {@code spa/application/usecase/UpdateSpaService}.
     *
     * <p>
     * Solo se mira si el <em>puerto</em> ofrece la variante acotada —misma
     * sobrecarga con {@code companyId}, o el mismo nombre con sufijo
     * {@code AndCompanyId}—. Cuando no la ofrece no hay nada que exigir aquí: ese
     * caso es de {@link #acotarPorEmpresaLasOperacionesPorId()} si la fila es de la
     * propia feature, o de {@link #acotarPorEmpresaLasReferenciasCrossFeature()} si
     * es una referencia a otra, y arreglarlo pasa por <em>crear</em> el finder
     * acotado (es lo que les faltaba a {@code medicationschedule} y
     * {@code procedureschedule}).
     *
     * <p>
     * <b>El servicio que solo alcanza SYSTEM está exento</b>, igual que en
     * {@link #validarElTenantCuandoRecibeCompanyId()}. Ahí la carga ancha no es un
     * descuido sino el comportamiento correcto: un principal SYSTEM no tiene
     * empresa, así que exigirle la variante acotada sería exigirle pasar
     * {@code null} y no encontrar nunca nada. El caso llegó solo: al añadir
     * {@code findByIdAndCompanyId} a {@code EmployeeRoleRepository} y
     * {@code PermissionRepository} —para arreglar otras fugas— seis servicios que
     * la regla antes ni miraba se volvieron visibles, y cinco de ellos
     * ({@code Find}/{@code Update}{@code EmployeeRole},
     * {@code Find}/{@code Update}/{@code DeletePermission}) son puertos de
     * administración de plataforma cerrados a {@code hasRole('SYSTEM')} a secas.
     * Registrarlos como deuda habría envenenado el indicador: el store solo vale si
     * cada línea es algo que alguien debería arreglar algún día.
     * {@code CreateEmployeeRoleService} <em>no</em> queda exento y sigue rojo,
     * porque su gate añade {@code hasAuthority('employee.create')}.
     */
    static ArchCondition<JavaClass> cargarPorIdAcotandoLaEmpresa() {
        return new ArchCondition<>(
                "usar la variante acotada por empresa cuando el puerto" + " de salida la ofrece") {
            @Override
            public void check(JavaClass servicio, ConditionEvents events) {
                if (sinEmpresaDeLaQueTirar(servicio)) {
                    return;
                }
                Set<String> invocadas = new HashSet<>();
                List<JavaMethodCall> aPuertos = new ArrayList<>();
                for (JavaMethod metodo : servicio.getMethods()) {
                    for (JavaMethodCall llamada : metodo.getMethodCallsFromSelf()) {
                        JavaClass puerto = llamada.getTargetOwner();
                        if (!isOwnCode(puerto)
                                || !puerto.getPackageName().contains(PORT_OUT_PACKAGE)) {
                            continue;
                        }
                        invocadas.add(clave(puerto, llamada.getTarget().getName()));
                        aPuertos.add(llamada);
                    }
                }
                Set<String> reportadas = new HashSet<>();
                for (JavaMethodCall llamada : aPuertos) {
                    Optional<JavaMethod> destino = llamada.getTarget().resolveMember();
                    if (destino.isEmpty() || filtraPorEmpresa(destino.get())) {
                        continue;
                    }
                    JavaClass puerto = llamada.getTargetOwner();
                    List<JavaMethod> acotadas = hermanasAcotadasDelPuerto(puerto, destino.get());
                    if (acotadas.isEmpty()
                            || !reportadas.add(clave(puerto, destino.get().getName()))) {
                        continue;
                    }
                    Optional<JavaMethod> usada = acotadas.stream()
                            .filter(a -> invocadas.contains(clave(puerto, a.getName())))
                            .findFirst();
                    String nombres = acotadas.stream().map(a -> a.getName() + "()")
                            .collect(Collectors.joining(" ni "));
                    events.add(new SimpleConditionEvent(servicio, usada.isPresent(),
                            servicio.getSimpleName() + " llama a " + puerto.getSimpleName() + "."
                                    + destino.get().getName() + "(), que no acota por empresa,"
                                    + (usada.isPresent()
                                            ? " pero tambien a " + usada.get().getName()
                                                    + "(): es el ternario del camino SYSTEM"
                                            : " y nunca a " + nombres
                                                    + ", que el puerto si ofrece: carga la fila"
                                                    + " de cualquier empresa")));
                }
            }
        };
    }

    private static String clave(JavaClass puerto, String metodo) {
        return puerto.getFullName() + "#" + metodo;
    }

    /**
     * {@code true} si a este caso de uso solo llega un principal SYSTEM: implementa
     * al menos un puerto de entrada y <em>todos</em> los que implementa están
     * cerrados a {@code hasRole('SYSTEM')} a secas.
     *
     * <p>
     * El criterio es por clase y no por método a propósito. La señal vive en el
     * {@code port/in} y la violación se detecta en el {@code usecase}, así que hay
     * que cruzar servicio → puerto que implementa; pero un servicio puede repartir
     * la carga entre el método público y un helper privado, y ahí el helper no
     * implementa nada. Exigir que <em>todos</em> los puertos de la clase sean
     * SYSTEM cubre las dos formas sin depender de cuál de sus métodos hizo la
     * llamada. En la práctica cada service implementa exactamente un caso de uso.
     *
     * <p>
     * Un servicio que no implementa ningún puerto <b>no</b> queda exento: no es
     * alcanzable desde fuera, así que su gate es el del puerto que acabe llamándolo
     * y esta clase no puede afirmar que sea SYSTEM.
     */
    /**
     * {@code true} si a este caso de uso no llega ningún principal del que sacar
     * una empresa, por cualquiera de las dos vías que el proyecto reconoce.
     *
     * <p>
     * La primera es {@link #soloAlcanzablePorSystem(JavaClass)}: un principal
     * SYSTEM es cross-tenant por diseño y no tiene empresa.
     *
     * <p>
     * La segunda es {@link NoAuthorizationRequired}, y estaba ya en el proyecto sin
     * que esta condición la mirara: {@link #acotarPorEmpresaLasOperacionesPorId()}
     * la trata como exención desde su primera línea —el propio
     * {@code HexagonalArchitectureTest} la excluye en el {@code .that()} de la
     * regla—, así que no mirarla aquí era una asimetría entre dos reglas de la
     * misma familia, no una decisión. Un puerto anotado así declara por escrito que
     * su autorización <em>no es un JWT de empleado</em>: el flujo público de
     * verificación de correo se autoriza por la posesión de un token de un solo uso
     * y el webhook del proveedor por su firma HMAC. En ninguno de los dos hay
     * empresa en la petición —en el webhook la empresa es una <em>salida</em> de la
     * búsqueda, se deriva del documento encontrado—, así que exigirles la variante
     * acotada es exigirles pasar {@code null} y no encontrar nunca nada, que es
     * exactamente el argumento con el que ya se eximió a SYSTEM.
     *
     * <p>
     * La anotación no es una vía silenciosa: exige escribir el motivo y la
     * comprueba otra regla dura. Se paga el mismo precio que en las demás
     * exenciones del fichero — que la salida esté declarada en el código y sea
     * legible en revisión.
     */
    private static boolean sinEmpresaDeLaQueTirar(JavaClass servicio) {
        return soloAlcanzablePorSystem(servicio) || sinAutorizacionDeEmpleado(servicio);
    }

    /**
     * {@code true} si el servicio implementa al menos un puerto de entrada y
     * <em>todos</em> los que implementa están anotados con
     * {@link NoAuthorizationRequired}. Mismo criterio por clase que
     * {@link #soloAlcanzablePorSystem(JavaClass)}, y por el mismo motivo: la señal
     * vive en el {@code port/in} y la violación se detecta en el {@code usecase}.
     */
    private static boolean sinAutorizacionDeEmpleado(JavaClass servicio) {
        boolean implementaAlguno = false;
        for (JavaMethod metodo : servicio.getMethods()) {
            Optional<JavaMethod> puerto = puertoQueImplementa(metodo);
            if (puerto.isEmpty()) {
                continue;
            }
            implementaAlguno = true;
            if (!puerto.get().getOwner().isAnnotatedWith(NoAuthorizationRequired.class)) {
                return false;
            }
        }
        return implementaAlguno;
    }

    private static boolean soloAlcanzablePorSystem(JavaClass servicio) {
        boolean implementaAlguno = false;
        for (JavaMethod metodo : servicio.getMethods()) {
            Optional<JavaMethod> puerto = puertoQueImplementa(metodo);
            if (puerto.isEmpty()) {
                continue;
            }
            implementaAlguno = true;
            Optional<PreAuthorize> gate = puerto.get().tryGetAnnotationOfType(PreAuthorize.class);
            if (gate.isEmpty() || !soloAlcanzablePorSystem(gate.get().value())) {
                return false;
            }
        }
        return implementaAlguno;
    }

    /**
     * Exige que el puerto con el que un caso de uso resuelve una referencia a
     * <em>otra</em> feature ofrezca —y use— la variante acotada por empresa.
     *
     * <p>
     * Es la cuarta forma del defecto, y la que sobrevive a las otras tres. Con la
     * carga propia ya acotada, un {@code UpdateSurgeryService} no puede apropiarse
     * de una cirugía ajena: el {@code findByIdAndCompanyId} de su repositorio lo
     * impide. Lo que sí puede es <b>reapuntar la suya a una entidad de otro
     * tenant</b>, porque resuelve el animal con
     * {@code animalQueryPort.findById(command.animalId())} y ese puerto no filtra
     * nada — una cirugía de mi empresa colgada del animal de la vecina, con su
     * historia clínica contaminada. Afecta a {@code laboratorytest},
     * {@code surgery}, {@code diagnosticimaging} y {@code daycare}; {@code spa},
     * {@code prescription} y {@code consultation} lo hacen bien y son el modelo.
     *
     * <p>
     * <b>Por qué es regla aparte y no una ampliación de
     * {@link #cargarPorIdAcotandoLaEmpresa()}.</b> Aquella ya mira los
     * {@code *QueryPort} —su filtro es el paquete {@code port.out}, no el nombre
     * del tipo—, así que ampliarla no habría añadido nada: el problema es que estos
     * puertos <b>no declaran</b> ninguna variante acotada que exigirles llamar, y
     * una condición que dice «llama a la acotada» no puede reportar «declárala
     * primero». Son dos afirmaciones distintas y el mensaje de fallo tiene que
     * decir cuál es. Las cuatro reglas quedan disjuntas por construcción: si el
     * puerto ofrece la acotada, el caso es de aquella; si no la ofrece y la fila es
     * de la propia feature, es de {@link #acotarPorEmpresaLasOperacionesPorId()};
     * esta se queda solo con las referencias cross-feature.
     *
     * <p>
     * <b>De dónde sale «esta referencia es de una empresa».</b> No del nombre. Se
     * sigue la cadena que el propio código ya declara: puerto → implementación
     * ({@code surgery.infrastructure.persistence.JpaAnimalQueryPort}) → el
     * {@code XxxJpaRepository} que inyecta → su entidad ({@code AnimalJpaEntity}) →
     * {@code perteneceAUnaEmpresa}. Se exige además que la entidad sea la del
     * núcleo del nombre del puerto ({@code AnimalQueryPort} →
     * {@code AnimalJpaEntity}), para que un adaptador que inyecte varios
     * repositorios no haga responder a la regla por el equivocado. Así
     * {@code CompanyQueryPort} (la empresa no pertenece a ninguna),
     * {@code SpaTypeQueryPort} y {@code SpecieQueryPort} (catálogos globales)
     * quedan fuera sin enumerar excepciones.
     */
    static ArchCondition<JavaClass> acotarPorEmpresaLasReferenciasCrossFeature() {
        return new ArchCondition<>("resolver las referencias a otra feature con la variante"
                + " acotada por empresa") {
            @Override
            public void check(JavaClass servicio, ConditionEvents events) {
                if (sinEmpresaDeLaQueTirar(servicio)) {
                    return;
                }
                Optional<JavaPackage> propia = paqueteDeLaFeature(servicio);
                if (propia.isEmpty() || !yaSabeAcotar(servicio)) {
                    return;
                }
                Set<String> reportadas = new HashSet<>();
                for (JavaMethod metodo : servicio.getMethods()) {
                    for (JavaMethodCall llamada : metodo.getMethodCallsFromSelf()) {
                        JavaClass puerto = llamada.getTargetOwner();
                        if (!isOwnCode(puerto)
                                || !puerto.getPackageName().contains(PORT_OUT_PACKAGE)) {
                            continue;
                        }
                        Optional<JavaMethod> destino = llamada.getTarget().resolveMember();
                        if (destino.isEmpty() || !resuelveUnaReferencia(destino.get())
                                || filtraPorEmpresa(destino.get())
                                || hermanaAcotadaDelPuerto(puerto, destino.get()).isPresent()) {
                            continue;
                        }
                        Optional<JavaClass> referida = entidadReferidaPor(puerto);
                        if (referida.isEmpty() || !perteneceAUnaEmpresa(referida.get())
                                || esDeLaMismaFeature(referida.get(), propia.get())
                                || nombraLaReferenciaComoAutor(servicio, referida.get())) {
                            continue;
                        }
                        if (!reportadas.add(clave(puerto, destino.get().getName()))) {
                            continue;
                        }
                        events.add(new SimpleConditionEvent(servicio, false,
                                servicio.getSimpleName() + " resuelve una referencia a "
                                        + referida.get().getSimpleName() + " con "
                                        + puerto.getSimpleName() + "." + destino.get().getName()
                                        + "(), que no acota por empresa y no ofrece variante"
                                        + " acotada: puede reapuntar su fila a una entidad de"
                                        + " otro tenant"));
                    }
                }
            }
        };
    }

    /**
     * {@code true} si el servicio demuestra en alguna parte que tiene la empresa en
     * la mano: llama a una variante acotada de algún puerto de salida.
     *
     * <p>
     * Es el discriminador que hace la regla usable, y no es un truco de recuento.
     * Sin él la condición marcaba 89 puntos, la mayoría {@code Create…Service}: ahí
     * la referencia sin acotar es el mismo defecto, pero la regla no puede
     * distinguir un id que llega del cliente de uno que llega del principal, así
     * que arrastraba consigo los quince {@code EmployeeQueryPort.findById} que
     * resuelven el <em>empleado autenticado</em> —imposibles de «arreglar», porque
     * ahí no hay nada roto—. Exigiendo que el servicio ya use una acotada, lo que
     * queda es la afirmación defendible y comprobable: <b>este servicio ya tiene el
     * {@code companyId} y ya demostró que sabe usarlo; que lo use también para la
     * referencia</b>.
     */
    /**
     * {@code true} si el servicio no recibe la entidad referida como un
     * <em>recurso</em>, sino solo como el <em>autor</em> de lo que está
     * escribiendo. Entonces el id no lo elige el cliente y no hay nada que acotar.
     *
     * <p>
     * <b>El falso positivo que esto cierra, y por qué no se cerró antes.</b> Siete
     * servicios resuelven {@code EmployeeQueryPort.findById(...)} para guardar
     * quién hizo la cosa —{@code createdById}, {@code processedById},
     * {@code suspendedById}—, y el controller rellena ese campo desde el principal
     * (`authz.currentEmployeeId…`), nunca desde el request. El autor de la regla
     * los dejó dentro a propósito: excluirlos <em>enumerando el puerto por su
     * nombre</em> habría taparlo también el día que un {@code employeeId} llegue de
     * verdad en el request, y este fichero evita las listas de nombres en todas las
     * demás reglas.
     *
     * <p>
     * <b>La señal estructural que los distingue.</b> No el nombre del puerto ni el
     * del campo de autoría, sino la <b>ausencia del nombre de recurso</b>: si la
     * referencia es a {@code EmployeeJpaEntity} y ningún command ni parámetro del
     * servicio declara un {@code employeeId}, el servicio no tiene por dónde
     * recibir «sobre qué empleado actúo» — los únicos ids de empleado que le llegan
     * son de autoría. En cuanto alguien añada {@code employeeId} al command, la
     * regla vuelve a marcarlo, que es literalmente la preocupación que el autor
     * quería no tapar.
     *
     * <p>
     * Y discrimina de verdad, no solo de recuento: en
     * {@code CreateOpenAccountService} exime el {@code createdById} y <b>deja
     * rojo</b> el {@code OwnerQueryPort}, porque {@code CreateOpenAccountCommand}
     * sí declara {@code ownerId}; en {@code employeerole}, cuyos commands declaran
     * {@code employeeId} porque ahí el empleado <em>es</em> el recurso, no exime
     * nada.
     */
    private static boolean nombraLaReferenciaComoAutor(JavaClass servicio, JavaClass referida) {
        String nombreDeRecurso = comoRecurso(referida.getSimpleName());
        for (JavaMethod metodo : servicio.getMethods()) {
            for (JavaClass parametro : metodo.getRawParameterTypes()) {
                if (!isOwnCode(parametro)) {
                    continue;
                }
                for (JavaField campo : parametro.getAllFields()) {
                    if (nombreDeRecurso.equals(campo.getName())) {
                        return false;
                    }
                }
            }
            if (nombresDeParametro(metodo).filter(n -> n.contains(nombreDeRecurso)).isPresent()) {
                return false;
            }
        }
        return true;
    }

    /** {@code EmployeeJpaEntity} → {@code employeeId}. */
    private static String comoRecurso(String nombreDeEntidad) {
        String nucleo = nombreDeEntidad.endsWith(SUFIJO_ENTIDAD_JPA)
                ? nombreDeEntidad.substring(0,
                        nombreDeEntidad.length() - SUFIJO_ENTIDAD_JPA.length())
                : nombreDeEntidad;
        return Character.toLowerCase(nucleo.charAt(0)) + nucleo.substring(1) + "Id";
    }

    private static boolean yaSabeAcotar(JavaClass servicio) {
        for (JavaMethod metodo : servicio.getMethods()) {
            for (JavaMethodCall llamada : metodo.getMethodCallsFromSelf()) {
                JavaClass puerto = llamada.getTargetOwner();
                if (!isOwnCode(puerto) || !puerto.getPackageName().contains(PORT_OUT_PACKAGE)) {
                    continue;
                }
                if (llamada.getTarget().getName().contains("Company")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@code true} si la llamada resuelve una referencia que va a quedar guardada
     * en la fila: un {@code find…} que devuelve un tipo del proyecto (el
     * {@code XxxRef} de la feature), envuelto o no en {@code Optional}.
     *
     * <p>
     * Deja fuera los predicados y las proyecciones del mismo puerto
     * —{@code isOpen()}, {@code lockForUpdate()}, {@code outstandingAmount()},
     * {@code findStoragePath()}—: no producen la referencia que se escribe, y
     * acotarlos es otra conversación.
     */
    private static boolean resuelveUnaReferencia(JavaMethod method) {
        if (!method.getName().startsWith("find")) {
            return false;
        }
        JavaType retorno = method.getReturnType();
        if (isOwnCode(retorno.toErasure())) {
            return true;
        }
        return retorno instanceof JavaParameterizedType parametrizado
                && parametrizado.getActualTypeArguments().stream()
                        .anyMatch(argumento -> isOwnCode(argumento.toErasure()));
    }

    /**
     * La entidad JPA que hay detrás de un puerto de salida: se busca en los
     * repositorios que inyectan sus implementaciones, y se acepta solo la que
     * coincide con el núcleo del nombre del puerto.
     */
    private static Optional<JavaClass> entidadReferidaPor(JavaClass puerto) {
        String esperada = nucleoDelPuerto(puerto.getSimpleName()) + SUFIJO_ENTIDAD_JPA;
        for (JavaClass adaptador : puerto.getAllSubclasses()) {
            if (!isOwnCode(adaptador)) {
                continue;
            }
            for (JavaField campo : adaptador.getAllFields()) {
                JavaClass tipo = campo.getRawType();
                if (!isOwnCode(tipo) || !tipo.isAssignableTo(JpaRepository.class)) {
                    continue;
                }
                Optional<JavaClass> entidad = entidadDe(tipo)
                        .filter(e -> esperada.equals(e.getSimpleName()));
                if (entidad.isPresent()) {
                    return entidad;
                }
            }
        }
        return Optional.empty();
    }

    /** {@code AnimalQueryPort} → {@code Animal}. El sufijo es ruido del naming. */
    private static String nucleoDelPuerto(String simpleName) {
        for (String sufijo : List.of("QueryPort", "ValidationPort", "Repository", "Port")) {
            if (simpleName.endsWith(sufijo)) {
                return simpleName.substring(0, simpleName.length() - sufijo.length());
            }
        }
        return simpleName;
    }

    private static boolean esDeLaMismaFeature(JavaClass clazz, JavaPackage feature) {
        return paqueteDeLaFeature(clazz).map(p -> p.getName().equals(feature.getName()))
                .orElse(false);
    }

    /**
     * Las variantes acotadas que el puerto declara para el mismo método. Son una
     * <em>familia</em> y no un nombre porque un puerto puede declarar más de una
     * forma de acotar la misma carga, y todas valen:
     *
     * <ul>
     * <li>La <b>sobrecarga homónima</b> que recibe {@code companyId}
     * ({@code reactivate(id)} / {@code reactivate(id, companyId)}).</li>
     * <li>La <b>del mismo criterio</b>: el nombre, quitado el sufijo
     * {@code AndCompanyId}, termina en la misma cláusula {@code By…} que el ancho.
     * Así {@code findById} empareja tanto con {@code findByIdAndCompanyId} como con
     * {@code findOwnedByIdAndCompanyId}, y {@code findByIdForUpdate} con
     * {@code findByIdForUpdateAndCompanyId}.</li>
     * </ul>
     *
     * <p>
     * <b>Por qué la familia y no el nombre exacto.</b> Los cuatro catálogos por
     * empresa —{@code surgerytype}, {@code vaccinationtype},
     * {@code laboratorytesttype}, {@code diagnosticimagingtype}— declaran
     * <em>dos</em> finders acotados con propósitos distintos, y su javadoc lo dice:
     * {@code findByIdAndCompanyId} es el de <b>lectura</b> (la fila propia o
     * cualquiera de las generales) y {@code findOwnedByIdAndCompanyId} el de
     * <b>escritura</b> (SOLO la propia, porque editar una general la cambiaría para
     * todos los tenants). Sus ocho {@code Update…}/{@code Delete…} usan el segundo,
     * que es el <em>estrictamente más fuerte</em>, y la condición los marcaba a los
     * ocho por no llamar al primero: exigía cambiar código correcto por código
     * menos seguro. La cláusula {@code By…} es la señal de que las dos cargan lo
     * mismo por el mismo criterio, con la empresa encima.
     *
     * <p>
     * La comparación exige además el <b>mismo tipo de retorno</b> y que la acotada
     * reciba exactamente un parámetro más, para que no empareje con un finder que
     * responde otra pregunta ({@code findByIdIncludingDisabledAndCompanyId} tiene
     * cláusula {@code ByIdIncludingDisabled} y no cuenta como hermana de
     * {@code findById}).
     */
    private static List<JavaMethod> hermanasAcotadasDelPuerto(JavaClass puerto, JavaMethod metodo) {
        List<JavaMethod> hermanas = new ArrayList<>();
        for (JavaMethod otro : puerto.getAllMethods()) {
            if (otro.equals(metodo) || !filtraPorEmpresa(otro)) {
                continue;
            }
            if (otro.getName().equals(metodo.getName())) {
                hermanas.add(otro);
                continue;
            }
            if (cargaLoMismoAcotando(otro, metodo)) {
                hermanas.add(otro);
            }
        }
        return hermanas;
    }

    private static Optional<JavaMethod> hermanaAcotadaDelPuerto(JavaClass puerto,
            JavaMethod metodo) {
        return hermanasAcotadasDelPuerto(puerto, metodo).stream().findFirst();
    }

    /**
     * {@code true} si {@code acotada} responde la misma pregunta que {@code ancha}
     * más la empresa: mismo tipo de retorno, un parámetro más, y la misma cláusula
     * {@code By…} una vez quitado el sufijo {@code AndCompanyId}.
     */
    private static boolean cargaLoMismoAcotando(JavaMethod acotada, JavaMethod ancha) {
        if (!acotada.getRawReturnType().equals(ancha.getRawReturnType())) {
            return false;
        }
        List<JavaClass> deLaAcotada = acotada.getRawParameterTypes();
        List<JavaClass> deLaAncha = ancha.getRawParameterTypes();
        if (deLaAcotada.size() != deLaAncha.size() + 1) {
            return false;
        }
        for (int i = 0; i < deLaAncha.size(); i++) {
            if (!deLaAcotada.get(i).equals(deLaAncha.get(i))) {
                return false;
            }
        }
        String clausula = clausulaBy(sinElSufijoAcotado(acotada.getName()));
        return !clausula.isEmpty() && clausula.equals(clausulaBy(ancha.getName()));
    }

    /**
     * El sufijo {@code AndCompanyId} es <b>opcional</b>, y esa es la diferencia
     * entre reconocer la hermana acotada y no reconocerla. El proyecto la escribe
     * de las dos formas: {@code findOwnedByIdAndCompanyId} la lleva en el nombre, y
     * {@code MedicamentQueryPort.findAvailableById(id, companyId)} la lleva solo en
     * la firma. Quien decide si acota es {@code filtraPorEmpresa}, que mira el
     * parámetro; el nombre solo sirve para comprobar que las dos cargan por el
     * mismo criterio.
     */
    private static String sinElSufijoAcotado(String nombre) {
        return nombre.endsWith(SUFIJO_ACOTADO)
                ? nombre.substring(0, nombre.length() - SUFIJO_ACOTADO.length())
                : nombre;
    }

    /** {@code findOwnedById} → {@code ById}; {@code save} → {@code ""}. */
    private static String clausulaBy(String nombre) {
        int desde = nombre.lastIndexOf("By");
        return desde < 0 ? "" : nombre.substring(desde);
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

    // ── #135: el @Valid que falta delante del cuerpo ─────────────────────────

    /** Paquete de las restricciones estandar de Bean Validation. */
    private static final String PAQUETE_DE_RESTRICCIONES = "jakarta.validation.constraints";

    /**
     * Exige {@code @Valid} en todo {@code @RequestBody} cuyo tipo declare alguna
     * restricción de Bean Validation.
     *
     * <p>
     * Sin {@code @Valid} delante, el binder de Spring <b>no dispara el
     * validador</b>: la restricción está escrita y no se evalúa nunca. No falla
     * nada al compilar, la anotación se lee perfecta en una revisión y el contrato
     * que genera springdoc sigue anunciando el {@code maxLength} — porque springdoc
     * lo deriva del {@code @Size}, con {@code @Valid} o sin él. Es decir: las tres
     * señales que un humano mira dicen que la validación existe.
     *
     * <p>
     * El caso que la motivó (#135) es
     * {@code CancelAppointmentRequest(@Size(max = 300) String reason)}, y enseña
     * bien por qué el daño no se ve: el dominio vuelve a medir la longitud en
     * {@code Appointment}, así que a la base no entra basura. Lo que se rompe es la
     * <b>forma del error</b> — en vez del error de campo que el front sabe pintar
     * junto al textarea sale una excepción de dominio, con otro {@code errorCode} y
     * otra forma, y quien cancela la cita lee un mensaje genérico que no le dice
     * qué corregir. Que era omisión y no decisión lo demuestra el endpoint de la
     * línea de arriba, {@code changeStatus}, que sí lo llevaba.
     *
     * <p>
     * <b>El predicado mira el tipo, nunca el nombre.</b> Un {@code XxxRequest} sin
     * una sola restricción no tiene nada que validar y queda fuera a propósito: de
     * los tres {@code @RequestBody} sin {@code @Valid} que había al escribirla, dos
     * son legítimos —el {@code String} crudo del webhook de la DIAN y el
     * {@code RefreshTokenRequest}, que dejó de exigir su campo por escrito— y la
     * regla no los toca. Por eso nace dura y en cero, sin {@code freeze(...)}.
     *
     * <p>
     * <b>Acepta {@code @Validated} además de {@code @Valid}</b> porque Spring
     * acepta las dos: {@code RequestResponseBodyMethodProcessor} delega en
     * {@code ValidationAnnotationUtils}, que reconoce cualquier anotación cuyo
     * nombre empiece por «Valid».
     *
     * <p>
     * <b>Y baja a los tipos anidados</b> —los campos del cuerpo y los argumentos
     * genéricos de sus colecciones, que es donde vive el tipo interesante de una
     * {@code List<LineaRequest>}—, porque un {@code @NotBlank} en una línea de
     * detalle tampoco se evalúa si el cuerpo que la transporta no lleva
     * {@code @Valid}. El recorrido se acota con {@link #MAX_DEPTH} y con el
     * conjunto de visitados: un DTO que se referencia a sí mismo no lo cuelga.
     */
    static ArchCondition<JavaMethod> validarElCuerpoQueDeclaraRestricciones() {
        return new ArchCondition<>("llevar @Valid en el @RequestBody que declara restricciones") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter parametro : method.getParameters()) {
                    if (!parametro.isAnnotatedWith(RequestBody.class)
                            || disparaLaValidacion(parametro)) {
                        continue;
                    }
                    primeraRestriccion(parametro.getRawType(), new HashSet<>(), 0).ifPresent(
                            restriccion -> events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName() + " recibe un @RequestBody sin @Valid que "
                                            + restriccion + ": el binder no dispara el validador,"
                                            + " asi que esa restriccion no se evalua nunca")));
                }
            }
        };
    }

    /** {@code true} si el parametro lleva la anotacion que dispara el validador. */
    private static boolean disparaLaValidacion(JavaParameter parametro) {
        return parametro.isAnnotatedWith(Valid.class) || parametro.isAnnotatedWith(Validated.class);
    }

    /**
     * La primera restricción que declara el tipo y dónde vive, o vacío si no
     * declara ninguna. Mira los campos y los accesores —en un {@code record}, el
     * compilador propaga la anotación del componente a los dos—, y después baja a
     * los tipos anidados del proyecto.
     */
    private static Optional<String> primeraRestriccion(JavaClass tipo, Set<String> visitados,
            int profundidad) {
        if (profundidad > MAX_DEPTH || !isOwnCode(tipo) || !visitados.add(tipo.getFullName())) {
            return Optional.empty();
        }
        for (JavaField campo : tipo.getAllFields()) {
            Optional<String> restriccion = nombreDeRestriccion(campo.getAnnotations());
            if (restriccion.isPresent()) {
                return Optional.of("declara " + restriccion.get() + " en " + tipo.getSimpleName()
                        + "." + campo.getName());
            }
        }
        for (JavaMethod accesor : tipo.getMethods()) {
            Optional<String> restriccion = nombreDeRestriccion(accesor.getAnnotations());
            if (restriccion.isPresent()) {
                return Optional.of("declara " + restriccion.get() + " en " + tipo.getSimpleName()
                        + "." + accesor.getName() + "()");
            }
        }
        for (JavaField campo : tipo.getAllFields()) {
            for (JavaClass anidado : tiposQueTransporta(campo)) {
                Optional<String> restriccion = primeraRestriccion(anidado, visitados,
                        profundidad + 1);
                if (restriccion.isPresent()) {
                    return restriccion;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> nombreDeRestriccion(
            Set<? extends JavaAnnotation<?>> anotaciones) {
        return anotaciones.stream().map(JavaAnnotation::getRawType)
                .filter(VetSoftwareConditions::esUnaRestriccion)
                .map(anotacion -> "@" + anotacion.getSimpleName()).findFirst();
    }

    /**
     * Restricción estándar o propia. Lo segundo hoy no existe en el repositorio,
     * pero la comprobación por la meta-anotación {@code @Constraint} es lo que
     * evita que la regla se quede corta el día que alguien escriba la primera.
     */
    private static boolean esUnaRestriccion(JavaClass anotacion) {
        return anotacion.getPackageName().equals(PAQUETE_DE_RESTRICCIONES)
                || anotacion.isAnnotatedWith(Constraint.class)
                || anotacion.isMetaAnnotatedWith(Constraint.class);
    }

    /**
     * Los tipos que un campo transporta: el suyo y los argumentos genéricos que
     * declare. Sin lo segundo, una {@code List<LineaRequest>} se quedaría en
     * {@code List} y la línea de detalle no se miraría jamás.
     */
    private static List<JavaClass> tiposQueTransporta(JavaField campo) {
        List<JavaClass> tipos = new ArrayList<>();
        tipos.add(campo.getRawType());
        if (campo.getType() instanceof JavaParameterizedType parametrizado) {
            parametrizado.getActualTypeArguments().stream().map(JavaType::toErasure)
                    .forEach(tipos::add);
        }
        return tipos;
    }

    // ── La empresa no viaja en el cuerpo de la peticion ────────────────

    /**
     * El nombre exacto del componente que nunca puede llegar escrito por el
     * cliente.
     */
    private static final String CAMPO_DE_EMPRESA = "companyId";

    /**
     * Su forma de accesor JavaBean, por si el cuerpo no es un {@code record}. En un
     * {@code record} el componente y su accesor se llaman igual, asi que el nombre
     * de arriba cubre los dos.
     */
    private static final String ACCESOR_DE_EMPRESA = "getCompanyId";

    /**
     * Las tres puertas por las que Spring enlaza un objeto escrito por el cliente a
     * un parametro. Hoy solo se usa {@code @RequestBody} en {@code src/main}; las
     * otras dos van aqui para que el primer endpoint multipart que entre nazca ya
     * mirado, no para tapar nada existente.
     */
    private static boolean esCuerpoDePeticion(JavaParameter parametro) {
        return parametro.isAnnotatedWith(RequestBody.class)
                || parametro.isAnnotatedWith(RequestPart.class)
                || parametro.isAnnotatedWith(ModelAttribute.class);
    }

    static ArchCondition<JavaMethod> noRecibirLaEmpresaEnElCuerpo() {
        return new ArchCondition<>("no aceptar un companyId en el cuerpo de la peticion") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter parametro : method.getParameters()) {
                    if (!esCuerpoDePeticion(parametro)) {
                        continue;
                    }
                    primerCampoDeEmpresa(parametro.getRawType(), new HashSet<>(), 0)
                            .ifPresent(donde -> events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName() + " recibe un cuerpo que declara " + donde
                                            + ": ese numero lo escribe el cliente en el JSON, asi"
                                            + " que @authz.isMyCompany(#command.companyId) lo"
                                            + " compara consigo mismo y da true siempre. Un"
                                            + " empleado de la empresa A crea la fila en la B y el"
                                            + " gate no lo ve. La empresa se toma del principal:"
                                            + " authz.currentCompanyId()")));
                }
            }
        };
    }

    /**
     * Donde declara el tipo un {@code companyId}, o vacio si no lo declara. Mira
     * los campos y los accesores del propio tipo y despues baja a los tipos
     * anidados y a los argumentos genericos de sus colecciones, igual que
     * {@link #primeraRestriccion}: un {@code companyId} escondido en una linea de
     * detalle es exactamente el mismo defecto, y ademas el que nadie revisa.
     *
     * <p>
     * <b>Compara por nombre exacto, no por prefijo</b>, y eso no es tiquismiquis:
     * un {@code startsWith("companyId")} tumbaria hoy mismo un uso legitimo.
     * {@code RegisterUserRequest.companyIdentifier} es el NIT con el que se da de
     * alta una empresa en el registro publico —no el id de un tenant existente— y
     * en ese punto no hay principal del que sacar nada, asi que tiene que viajar en
     * el cuerpo a la fuerza.
     */
    private static Optional<String> primerCampoDeEmpresa(JavaClass tipo, Set<String> visitados,
            int profundidad) {
        if (profundidad > MAX_DEPTH || !isOwnCode(tipo) || !visitados.add(tipo.getFullName())) {
            return Optional.empty();
        }
        for (JavaField campo : tipo.getAllFields()) {
            if (CAMPO_DE_EMPRESA.equals(campo.getName())) {
                return Optional.of(tipo.getSimpleName() + "." + campo.getName());
            }
        }
        for (JavaMethod accesor : tipo.getMethods()) {
            if (accesor.getRawParameterTypes().isEmpty()
                    && (CAMPO_DE_EMPRESA.equals(accesor.getName())
                            || ACCESOR_DE_EMPRESA.equals(accesor.getName()))) {
                return Optional.of(tipo.getSimpleName() + "." + accesor.getName() + "()");
            }
        }
        for (JavaField campo : tipo.getAllFields()) {
            for (JavaClass anidado : tiposQueTransporta(campo)) {
                Optional<String> donde = primerCampoDeEmpresa(anidado, visitados, profundidad + 1);
                if (donde.isPresent()) {
                    return donde;
                }
            }
        }
        return Optional.empty();
    }

    // ── BE-10: cada adaptador con su rodaja ──────────────────────────────────

    /** Sufijo de las rodajas de persistencia; failsafe solo recoge {@code *IT}. */
    private static final String SUFIJO_RODAJA_PERSISTENCIA = "IT";

    /**
     * Sufijo de las rodajas web; surefire solo recoge {@code *Test}/{@code *Tests}.
     */
    private static final String SUFIJO_RODAJA_WEB = "Test";

    /** Marca de {@code target/test-classes} en el URI de origen de una clase. */
    private static final String SALIDA_DE_TEST = "/test-classes/";

    /**
     * Deja fuera lo que no es código de producción. El {@code @AnalyzeClasses} de
     * {@link PiramideDeTestsTest} importa {@code src/test} a propósito —lo necesita
     * para <em>encontrar</em> las rodajas—, y eso mete en el universo cosas que
     * parecen adaptadores sin serlo.
     *
     * <p>
     * El caso real: {@code GlobalExceptionHandlerTest.BoomController}, un
     * {@code @RestController} anidado de juguete cuyas rutas lanzan cada excepción
     * que se quiere mapear. Sin este filtro la regla exigía un
     * {@code BoomControllerTest} —una rodaja para un fixture—, y esa violación
     * fantasma habría entrado al store congelada, contaminando el indicador desde
     * el primer día.
     *
     * <p>
     * Dos filtros, porque hacen falta los dos: el URI de origen descarta lo que
     * viene de {@code target/test-classes} (la misma señal que usa
     * {@code ImportOption.DoNotIncludeTests}), y {@code getEnclosingClass} descarta
     * las anidadas, que nunca son un adaptador ni un controller de producción.
     */
    static DescribedPredicate<JavaClass> sonCodigoDeProduccion() {
        return DescribedPredicate.describe("son codigo de produccion",
                VetSoftwareConditions::esCodigoDeProduccion);
    }

    private static boolean esCodigoDeProduccion(JavaClass javaClass) {
        return javaClass.getEnclosingClass().isEmpty() && !vieneDelArbolDeTest(javaClass);
    }

    /**
     * La mitad de {@link #sonCodigoDeProduccion()} que mira <em>de dónde salió el
     * {@code .class}</em>, sin el filtro de clases anidadas.
     *
     * <p>
     * Existe separada porque hay una regla —{@code DOBLE_DE_TEST_NO_ESCANEABLE}— a
     * la que el filtro de anidadas le sobra, y no en el sentido de que le dé igual:
     * las clases anidadas son <strong>el 100 %</strong> de lo que esa regla
     * persigue, así que negar {@code sonCodigoDeProduccion()} entero descartaría
     * exactamente los casos que hay que cazar. Extraída aquí para que las dos
     * reglas compartan la única constante que dice qué es
     * {@code target/test-classes}: dos copias de esa cadena es como una de las dos
     * se queda atrás.
     */
    static DescribedPredicate<JavaClass> vienenDelArbolDeTest() {
        return DescribedPredicate.describe("vienen del arbol de test",
                VetSoftwareConditions::vieneDelArbolDeTest);
    }

    /**
     * Sin origen conocido no se puede afirmar que sea de test: se trata como
     * producción, igual que hacía el chequeo del que se extrajo.
     */
    private static boolean vieneDelArbolDeTest(JavaClass javaClass) {
        return javaClass.getSource()
                .map(source -> source.getUri().toString().contains(SALIDA_DE_TEST)).orElse(false);
    }

    /**
     * Exige que todo adaptador {@code JpaXxxRepository} tenga en su mismo paquete
     * una rodaja de persistencia: una clase {@code *IT} cuyo nombre contenga el
     * núcleo del adaptador ({@code JpaStockLotRepository} →
     * {@code StockLotPersistenceIT}).
     *
     * <p>
     * <strong>Por qué el nombre y no solo el paquete.</strong> Bastaría con exigir
     * «algún {@code *IT} en el paquete», pero entonces una sola rodaja taparía los
     * dieciséis adaptadores de {@code animal.infrastructure.persistence} y el
     * indicador dejaría de contar lo que importa. Cruzar por el núcleo del nombre
     * hace que la unidad de medida sea el adaptador, que es la unidad de riesgo:
     * cada uno traduce su propio SQL y su propio {@code Sort}.
     *
     * <p>
     * <strong>Qué queda fuera, a propósito.</strong> Solo entran los adaptadores
     * que siguen el naming del CLAUDE.md ({@code Jpa} + entidad +
     * {@code Repository}). Los {@code JpaXxxQueryPort},
     * {@code JpaXxxValidationPort} y los adaptadores sueltos
     * ({@code NumberingAllocationAdapter}, {@code DianJobLeaseAdapter}) no se
     * exigen: son proyecciones de una consulta o de un lease, y su rodaja aporta
     * bastante menos que la del adaptador que escribe. Ampliar el alcance es
     * ampliar el predicado, no relajar esta condición.
     */
    static ArchCondition<JavaClass> tenerRodajaDePersistencia() {
        return new ArchCondition<>("tener una rodaja *IT en su mismo paquete") {
            @Override
            public void check(JavaClass adaptador, ConditionEvents events) {
                String nucleo = nucleoDelAdaptador(adaptador.getSimpleName());
                boolean cubierto = hermanasDePaquete(adaptador)
                        .anyMatch(nombre -> nombre.endsWith(SUFIJO_RODAJA_PERSISTENCIA)
                                && nombre.contains(nucleo));
                events.add(new SimpleConditionEvent(adaptador, cubierto,
                        adaptador.getSimpleName() + " no tiene rodaja de persistencia: falta "
                                + nucleo + "PersistenceIT (@DataJpaTest) en "
                                + adaptador.getPackageName()));
            }
        };
    }

    /**
     * Exige que todo {@code @RestController} tenga en su mismo paquete una rodaja
     * web: una clase {@code *Test} cuyo nombre empiece por el del controller
     * ({@code AnimalController} → {@code AnimalControllerTest}).
     *
     * <p>
     * <strong>Por qué el prefijo y no «algún {@code *Test} en el paquete».</strong>
     * Porque en {@code infrastructure/web} conviven rodajas y tests unitarios
     * corrientes: {@code RefreshTokenCookieTest}, {@code CashArqueoCsvTest} e
     * {@code InventoryCsvTest} son JUnit puro sobre un helper, no ejercitan ningún
     * endpoint, y un criterio por paquete los contaría como red que no existe.
     * Exigir el prefijo del controller elimina los tres falsos positivos sin
     * enumerar excepciones.
     *
     * <p>
     * <strong>El {@code GlobalExceptionHandler} no cuenta como controller</strong>:
     * es {@code @RestControllerAdvice}, una anotación distinta que no está
     * meta-anotada con {@code @RestController}, así que el predicado ni lo mira.
     */
    static ArchCondition<JavaClass> tenerRodajaWeb() {
        return new ArchCondition<>("tener una rodaja *Test en su mismo paquete") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                String esperado = controller.getSimpleName() + SUFIJO_RODAJA_WEB;
                boolean cubierto = hermanasDePaquete(controller)
                        .anyMatch(nombre -> nombre.endsWith(SUFIJO_RODAJA_WEB)
                                && nombre.startsWith(controller.getSimpleName()));
                events.add(new SimpleConditionEvent(controller, cubierto,
                        controller.getSimpleName() + " no tiene rodaja web: falta " + esperado
                                + " (@WebMvcTest) en " + controller.getPackageName()));
            }
        };
    }

    /**
     * {@code JpaStockLotRepository} → {@code StockLot}. El núcleo es lo que
     * comparten el adaptador y su rodaja; el prefijo {@code Jpa} y el sufijo
     * {@code Repository} son ruido del naming.
     */
    private static String nucleoDelAdaptador(String simpleName) {
        String nucleo = simpleName.substring("Jpa".length());
        return nucleo.substring(0, nucleo.length() - "Repository".length());
    }

    /**
     * Nombres simples de las clases de nivel superior del mismo paquete —de los dos
     * árboles, porque el {@code @AnalyzeClasses} de {@code PiramideDeTestsTest} sí
     * importa {@code src/test}—. Sin subpaquetes: un test de {@code web/request} no
     * es la rodaja del controller. Sin clases anidadas: las {@code @Nested} de una
     * rodaja no son rodajas.
     */
    private static Stream<String> hermanasDePaquete(JavaClass javaClass) {
        return javaClass.getPackage().getClasses().stream()
                .filter(hermana -> hermana.getEnclosingClass().isEmpty())
                .filter(hermana -> !hermana.equals(javaClass)).map(JavaClass::getSimpleName);
    }

    // ── BE-26: bloqueo optimista ─────────────────────────────────────────────

    /**
     * Códigos de exención de {@code @Version}. Nombran la razón
     * <em>estructural</em> por la que una entidad no necesita bloqueo optimista; el
     * motivo concreto va entrada por entrada en
     * {@code HexagonalArchitectureTest.ENTIDADES_EXENTAS_DE_VERSION}. Esa es toda
     * la gracia del mecanismo: el diff de un PR enseña a quién se le perdona y por
     * qué, en vez de dejar la ausencia como un olvido indistinguible de una
     * decisión.
     */
    enum CodigoDeExencion {
        /** Se inserta y no se vuelve a modificar. */
        E1_APPEND_ONLY,
        /** Relación N:M pura: insert + delete, sin campo propio mutable. */
        E2_TABLA_PUENTE,
        /** Token de un solo uso o de vida corta. */
        E3_TOKEN,
        /** Vista de solo lectura o entidad {@code @Immutable}. */
        E4_VISTA,
        /**
         * Dato de referencia sembrado, sin pantalla que dos operadores editen a la vez.
         */
        E5_SEMILLA,
        /**
         * La concurrencia la resuelve un mecanismo más fuerte, nombrado en el motivo.
         */
        E6_YA_PROTEGIDO
    }

    /** Una línea de la lista de exenciones: a quién, con qué código y por qué. */
    record ExencionDeVersion(String entidad, CodigoDeExencion codigo, String motivo) {
    }

    /** {@code version = ?} como condición, con {@code auth_version = ?} fuera. */
    private static final Pattern CONDICION_DE_VERSION = Pattern
            .compile("(?<![a-z0-9_])version\\s*=\\s*\\?");

    private static final String WHERE = " where ";

    /**
     * BE-26, primera mitad: toda {@code @Entity} decide por escrito si lleva
     * bloqueo optimista. O declara un campo {@code @Version}, o aparece en la lista
     * de exenciones con su código y su motivo. No hay tercera salida silenciosa.
     */
    static ArchCondition<JavaClass> declararBloqueoOptimistaOEstarExenta(
            List<ExencionDeVersion> exenciones) {
        Map<String, ExencionDeVersion> porEntidad = indexar(exenciones);
        return new ArchCondition<>(
                "declarar un campo @Version, o figurar en la lista de exenciones") {
            @Override
            public void check(JavaClass entidad, ConditionEvents events) {
                String nombre = entidad.getSimpleName();
                boolean decidido = tieneCampoVersion(entidad) || porEntidad.containsKey(nombre);
                events.add(new SimpleConditionEvent(entidad, decidido, nombre
                        + " no declara ningún campo @Version: o le das bloqueo optimista, o la"
                        + " añades a ENTIDADES_EXENTAS_DE_VERSION con su código"
                        + " (E1_APPEND_ONLY … E6_YA_PROTEGIDO) y el motivo escrito"));
            }
        };
    }

    /**
     * BE-26, segunda mitad: el fallo silencioso. Cuando una entidad lleva
     * {@code @Version}, Hibernate liga <b>dos</b> parámetros al SQL del
     * {@code @SQLDelete} —primero el {@code id}, después la {@code version}—, así
     * que un {@code WHERE id = ?} con un solo {@code ?} deja el borrado lógico roto
     * en ejecución. No lo ve ninguna revisión: la anotación se lee perfecta.
     *
     * <p>
     * <strong>Por qué mira el {@code WHERE} y no el SQL entero.</strong> El
     * {@code SET} de {@code employees} y {@code system_users} lleva
     * {@code auth_version = auth_version + 1} —invalidación de sesión, otra cosa—,
     * y una subcadena ingenua {@code "version"} las daría por buenas sin mirar
     * nada. El lookbehind del patrón es la segunda red: descarta
     * {@code auth_version = ?} aunque alguien lo escriba dentro del {@code WHERE}.
     * Por lo mismo la condición se fija en la columna y no en el nombre del filtro:
     * {@code unit_measure_catalog} borra por {@code code} en vez de por {@code id}
     * y cumpliría igual el día que se le ponga {@code @Version}.
     */
    static ArchCondition<JavaClass> ligarLaVersionEnElBorradoLogico() {
        return new ArchCondition<>("acotar por la columna version el WHERE de su @SQLDelete") {
            @Override
            public void check(JavaClass entidad, ConditionEvents events) {
                if (!tieneCampoVersion(entidad)) {
                    return;
                }
                String sql = entidad.getAnnotationOfType(SQLDelete.class).sql();
                events.add(new SimpleConditionEvent(entidad, condicionaPorVersion(sql),
                        entidad.getSimpleName() + " lleva @Version, así que Hibernate liga DOS"
                                + " parámetros a su @SQLDelete (id y luego version); este WHERE"
                                + " solo nombra uno y el borrado lógico queda roto en ejecución."
                                + " Añádele AND version = ? al final. SQL actual: " + sql));
            }
        };
    }

    /**
     * BE-26, la parte que impide que la lista mienta. Una lista de exenciones que
     * nadie limpia acaba perdonando a entidades que ya no existen y —peor— tapando
     * a entidades que sí se versionaron: el día que alguien le ponga
     * {@code @Version} a una exenta y no borre su línea, la lista seguirá afirmando
     * por escrito algo falso y nadie se enterará.
     *
     * <p>
     * El sujeto real de la regla es la lista, no cada entidad, así que el censo se
     * levanta en {@code init(…)} y las violaciones se emiten en {@code finish(…)},
     * una por entrada podrida: entrada duplicada, entidad borrada o entidad ya
     * versionada.
     */
    static ArchCondition<JavaClass> mantenerLaListaDeExencionesAlDia(
            List<ExencionDeVersion> exenciones) {
        return new ArchCondition<>(
                "corresponder a una entidad que existe y que sigue sin @Version") {
            private final Set<String> existentes = new HashSet<>();
            private final Set<String> versionadas = new HashSet<>();

            @Override
            public void init(Collection<JavaClass> entidades) {
                existentes.clear();
                versionadas.clear();
                for (JavaClass entidad : entidades) {
                    existentes.add(entidad.getSimpleName());
                    if (tieneCampoVersion(entidad)) {
                        versionadas.add(entidad.getSimpleName());
                    }
                }
            }

            @Override
            public void check(JavaClass entidad, ConditionEvents events) {
                // Nada por entidad: lo que se juzga es la lista, y para eso hace
                // falta el censo completo. Ver finish().
            }

            @Override
            public void finish(ConditionEvents events) {
                Set<String> vistas = new HashSet<>();
                for (ExencionDeVersion exencion : exenciones) {
                    String nombre = exencion.entidad();
                    if (!vistas.add(nombre)) {
                        events.add(SimpleConditionEvent.violated(exencion, nombre
                                + " aparece dos veces en ENTIDADES_EXENTAS_DE_VERSION: deja una"
                                + " sola entrada, con el motivo que de verdad aplica"));
                    } else if (!existentes.contains(nombre)) {
                        events.add(SimpleConditionEvent.violated(exencion, nombre
                                + " figura en ENTIDADES_EXENTAS_DE_VERSION y ya no es ninguna"
                                + " clase @Entity: bórralo de la lista"));
                    } else if (versionadas.contains(nombre)) {
                        events.add(SimpleConditionEvent.violated(exencion, nombre
                                + " ya declara @Version y sigue exento como " + exencion.codigo()
                                + ": sácalo de ENTIDADES_EXENTAS_DE_VERSION para que la lista"
                                + " siga diciendo la verdad"));
                    }
                }
            }
        };
    }

    /** Un {@code @Version} declarado en la propia entidad o heredado. */
    private static boolean tieneCampoVersion(JavaClass entidad) {
        return entidad.getAllFields().stream()
                .anyMatch(campo -> campo.isAnnotatedWith(Version.class));
    }

    /**
     * Busca la condición sobre la columna {@code version} dentro del {@code WHERE}.
     */
    private static boolean condicionaPorVersion(String sql) {
        String normalizado = sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        int where = normalizado.indexOf(WHERE);
        return where >= 0 && CONDICION_DE_VERSION
                .matcher(normalizado.substring(where + WHERE.length())).find();
    }

    /**
     * Índice por nombre simple; el duplicado no revienta aquí, lo denuncia la
     * regla.
     */
    private static Map<String, ExencionDeVersion> indexar(List<ExencionDeVersion> exenciones) {
        return exenciones.stream().collect(Collectors.toMap(ExencionDeVersion::entidad,
                exencion -> exencion, (primera, repetida) -> primera, LinkedHashMap::new));
    }

    // ── #53: la puerta de atrás del bloqueo optimista ─────────────────────────

    /**
     * Solo actualizaciones. Un {@code DELETE} se lleva la fila entera: no queda
     * versión que mover ni {@code save} que pueda pisar nada.
     */
    private static final Pattern SENTENCIA_QUE_ACTUALIZA = Pattern.compile("^update\\s",
            Pattern.CASE_INSENSITIVE);

    private static final String SET = " set ";

    /**
     * El objetivo de una asignación: {@code columna} o {@code alias.columna}. El
     * lookbehind implícito lo da el anclaje —el fragmento ya viene recortado por el
     * primer {@code =}—, así que {@code auth_version} cae por sí solo: su columna
     * es {@code auth_version}, no {@code version}.
     */
    private static final Pattern OBJETIVO_ASIGNADO = Pattern
            .compile("^(?:([a-z][a-z0-9_]*)\\.)?([a-z][a-z0-9_]*)$");

    /** La columna del bloqueo optimista. */
    private static final String COLUMNA_VERSION = "version";

    /**
     * {@code version} usada como <em>filtro</em>. Misma idea de lookbehind que
     * {@link #CONDICION_DE_VERSION} —{@code auth_version} no cuenta—, pero aquí
     * encontrarla es el defecto y no el arreglo: en un {@code UPDATE} de conjunto,
     * condicionar por la versión lo deja actualizando cero filas y el servicio lo
     * lee como «no existe».
     */
    private static final Pattern VERSION_EN_EL_FILTRO = Pattern
            .compile("(?<![a-z0-9_])(?:[a-z][a-z0-9_]*\\.)?version\\s*=");

    /**
     * Lo que puede seguir al nombre de una tabla en la cláusula de actualización
     * sin ser su alias. {@code as} no está: ese sí introduce alias y se salta.
     */
    private static final Set<String> NO_ES_UN_ALIAS = Set.of("set", "join", "inner", "left",
            "right", "full", "outer", "cross", "straight_join", "on", "using", ",");

    /**
     * Cierre de la incidencia #53: toda {@code @Query} de {@code UPDATE} sobre una
     * tabla cuya entidad lleva {@code @Version} tiene que mover {@code version} en
     * su {@code SET}.
     *
     * <p>
     * <b>El mapa tabla → ¿versionada? se levanta del propio censo de
     * {@code @Entity}</b>, en {@code init(…)}: cada entidad aporta dos claves —su
     * {@code @Table(name = …)} para el SQL nativo y su nombre simple para el JPQL—
     * apuntando al mismo booleano. Deliberadamente <b>no</b> hay lista literal de
     * tablas versionadas: BE-26 ya dejó escrito lo que le pasa a una lista que
     * nadie mantiene, y aquí sería peor todavía, porque bastaría añadir
     * {@code @Version} a una entidad para que su tabla dejara de estar vigilada sin
     * que nada lo dijera. Hoy la cuenta es 71 de 104; mañana la que sea.
     *
     * <p>
     * <b>Las dos formas de {@code UPDATE} se resuelven contra el mismo censo.</b>
     * El nativo nombra la tabla ({@code UPDATE medication_schedules s SET …}) y el
     * JPQL nombra la entidad ({@code UPDATE RefreshTokenJpaEntity r SET …}); son
     * tres los JPQL del repositorio y los tres caen sobre entidades exentas, pero
     * la regla no lo da por hecho: mira el nombre que venga y lo busca en las dos
     * claves. Por eso tampoco necesita leer {@code nativeQuery}.
     *
     * <p>
     * <b>Y mira el {@code SET}, no la sentencia entera.</b> Un {@code version} en
     * el {@code WHERE} no es el arreglo —sería un {@code UPDATE} condicional que
     * falla en silencio devolviendo cero filas—, y el {@code SET} de
     * {@code employees} y {@code system_users} lleva {@code auth_version =
     * auth_version + 1}, que es invalidación de sesión y no bloqueo optimista: una
     * subcadena ingenua los daría por buenos. La cláusula se trocea por comas a
     * profundidad de paréntesis cero, de cada trozo se toma lo que hay antes del
     * primer {@code =} y solo eso cuenta como columna escrita.
     *
     * <p>
     * <b>El alias decide a qué tabla se le escribe.</b>
     * {@code UPDATE role_permissions rp JOIN roles r ON … SET rp.enabled = true}
     * toca {@code role_permissions} —exenta, tabla puente— y no {@code roles}, que
     * sí va versionada pero aquí solo se lee. Sin resolver el alias, esas cuatro
     * consultas de {@code RolePermissionJpaRepository} serían cuatro falsos
     * positivos y la regla se habría muerto de ruido el primer día.
     *
     * <p>
     * Una tabla que no corresponda a ninguna {@code @Entity} no se juzga: sin
     * entidad no hay {@code @Version} que saltarse.
     */
    static ArchCondition<JavaClass> moverLaVersionEnElUpdateMasivo() {
        return new ArchCondition<>("mover version en el SET —y no filtrar por ella en el"
                + " WHERE— en toda @Query de UPDATE sobre una tabla versionada") {
            private final Map<String, Boolean> versionadaPorNombre = new LinkedHashMap<>();

            @Override
            public void init(Collection<JavaClass> repositorios) {
                versionadaPorNombre.clear();
                repositorios.stream().findFirst().flatMap(VetSoftwareConditions::paqueteRaiz)
                        .ifPresent(raiz -> raiz.getClassesInPackageTree().stream()
                                .filter(clase -> clase.isAnnotatedWith(Entity.class))
                                .forEach(entidad -> censar(entidad, versionadaPorNombre)));
            }

            @Override
            public void check(JavaClass repositorio, ConditionEvents events) {
                for (JavaMethod metodo : repositorio.getMethods()) {
                    Optional<String> sentencia = sentenciaQueActualiza(metodo);
                    if (sentencia.isEmpty()) {
                        continue;
                    }
                    List<String> hallazgos = hallazgosDelUpdate(sentencia.get(),
                            versionadaPorNombre);
                    events.add(new SimpleConditionEvent(metodo, hallazgos.isEmpty(),
                            repositorio.getSimpleName() + "." + metodo.getName() + "(): "
                                    + String.join("; ", hallazgos)
                                    + ". @Version solo protege el ciclo leer-modificar-guardar"
                                    + " de una entidad gestionada, y esta @Query va directa a la"
                                    + " base: el bump va en el SET (version = version + 1) y"
                                    + " nunca en el WHERE, que convertiría el UPDATE en"
                                    + " condicional y lo dejaría actualizando cero filas."
                                    + " SQL actual: " + unaLinea(sentencia.get())));
                }
            }
        };
    }

    /** El statement de una {@code @Query} de UPDATE, si el método declara una. */
    private static Optional<String> sentenciaQueActualiza(JavaMethod method) {
        return method.tryGetAnnotationOfType(Query.class).map(Query::value).map(String::strip)
                .filter(sql -> SENTENCIA_QUE_ACTUALIZA.matcher(sql).find());
    }

    /** Las dos claves con las que una entidad se deja encontrar desde el SQL. */
    private static void censar(JavaClass entidad, Map<String, Boolean> censo) {
        boolean versionada = tieneCampoVersion(entidad);
        censo.put(entidad.getSimpleName().toLowerCase(Locale.ROOT), versionada);
        entidad.tryGetAnnotationOfType(Table.class).map(Table::name)
                .filter(nombre -> !nombre.isBlank())
                .ifPresent(nombre -> censo.put(nombre.toLowerCase(Locale.ROOT), versionada));
    }

    /**
     * Lo que le falta —o le sobra— a una sentencia de {@code UPDATE}: las tablas
     * versionadas a las que escribe sin tocarles {@code version}, y el filtro por
     * {@code version} si lo hubiera. Lista vacía = la consulta cumple.
     */
    private static List<String> hallazgosDelUpdate(String sentencia, Map<String, Boolean> censo) {
        String normalizada = unaLinea(sentencia).toLowerCase(Locale.ROOT);
        int set = normalizada.indexOf(SET);
        if (set < 0) {
            return List.of();
        }
        Map<String, String> porAlias = tablasDeLaClausula(
                normalizada.substring("update".length(), set));
        String principal = porAlias.values().stream().findFirst().orElse("");
        String desdeSet = normalizada.substring(set + SET.length());
        int filtro = comienzoDelFiltro(desdeSet);

        Set<String> escritas = new LinkedHashSet<>();
        Set<String> conVersion = new HashSet<>();
        String clausulaSet = filtro < 0 ? desdeSet : desdeSet.substring(0, filtro);
        for (String objetivo : objetivosDelSet(clausulaSet)) {
            Matcher asignacion = OBJETIVO_ASIGNADO.matcher(objetivo);
            if (!asignacion.matches()) {
                continue;
            }
            String alias = asignacion.group(1);
            String tabla = alias == null ? principal : porAlias.getOrDefault(alias, principal);
            escritas.add(tabla);
            if (COLUMNA_VERSION.equals(asignacion.group(2))) {
                conVersion.add(tabla);
            }
        }

        List<String> hallazgos = new ArrayList<>(escritas.stream()
                .filter(tabla -> Boolean.TRUE.equals(censo.get(tabla)))
                .filter(tabla -> !conVersion.contains(tabla))
                .map(tabla -> "escribe en " + tabla + " (versionada) sin mover version en el SET")
                .toList());
        if (filtro >= 0 && VERSION_EN_EL_FILTRO.matcher(desdeSet.substring(filtro)).find()) {
            hallazgos.add("condiciona por version en el WHERE, que es el defecto contrario:"
                    + " un UPDATE de conjunto no lo lee nadie antes y actualizaría cero filas");
        }
        return hallazgos;
    }

    /**
     * Dónde acaba el {@code SET} y empieza el filtro: el primer {@code WHERE} a
     * profundidad de paréntesis cero, para que el de una subconsulta no corte antes
     * de tiempo. {@code -1} si la sentencia no filtra.
     */
    private static int comienzoDelFiltro(String desdeSet) {
        int profundidad = 0;
        for (int i = 0; i < desdeSet.length(); i++) {
            char caracter = desdeSet.charAt(i);
            if (caracter == '(') {
                profundidad++;
            } else if (caracter == ')') {
                profundidad--;
            } else if (profundidad == 0 && desdeSet.startsWith(WHERE, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Alias y nombres de tabla de la cláusula que va entre {@code UPDATE} y
     * {@code SET}, cada uno apuntando a su tabla. El primer valor es la tabla
     * principal, la que recibe las asignaciones sin prefijo.
     */
    private static Map<String, String> tablasDeLaClausula(String clausula) {
        Map<String, String> porAlias = new LinkedHashMap<>();
        String[] tokens = clausula.replace(",", " , ").strip().split("\\s+");
        boolean esperaTabla = true;
        for (int i = 0; i < tokens.length; i++) {
            if (!esperaTabla) {
                esperaTabla = "join".equals(tokens[i]) || ",".equals(tokens[i]);
                continue;
            }
            String tabla = tokens[i];
            porAlias.putIfAbsent(tabla, tabla);
            int siguiente = i + 1;
            if (siguiente < tokens.length && "as".equals(tokens[siguiente])) {
                siguiente++;
            }
            if (siguiente < tokens.length && !NO_ES_UN_ALIAS.contains(tokens[siguiente])) {
                porAlias.putIfAbsent(tokens[siguiente], tabla);
                i = siguiente;
            }
            esperaTabla = false;
        }
        return porAlias;
    }

    /**
     * Lo que hay a la izquierda del {@code =} en cada asignación del {@code SET}.
     * Trocea por comas a profundidad de paréntesis cero, para que ni una
     * subconsulta ni un {@code CASE WHEN x = 1} cuelen su comparación como si fuera
     * una columna escrita. El recorte del {@code WHERE} ya viene hecho por
     * {@link #comienzoDelFiltro(String)}.
     */
    private static List<String> objetivosDelSet(String clausulaSet) {
        List<String> fragmentos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int profundidad = 0;
        for (int i = 0; i < clausulaSet.length(); i++) {
            char caracter = clausulaSet.charAt(i);
            if (caracter == '(') {
                profundidad++;
            } else if (caracter == ')') {
                profundidad--;
            }
            if (profundidad == 0 && caracter == ',') {
                fragmentos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(caracter);
            }
        }
        fragmentos.add(actual.toString());
        return fragmentos.stream().map(fragmento -> {
            int igual = fragmento.indexOf('=');
            return igual < 0 ? "" : fragmento.substring(0, igual).strip();
        }).filter(objetivo -> !objetivo.isEmpty()).toList();
    }

    // ── #209: la authority que desentona en una feature de SYSTEM ────────────

    /** El disyunto que abre la puerta a un empleado con permiso sembrado. */
    private static final String AUTHORITY = "hasAuthority";

    /**
     * Cuántos hermanos hacen falta para que «la norma de la feature» sea una norma
     * y no una coincidencia. Con uno solo, el conjunto de dos gates iguales no dice
     * nada; a partir de dos, la excepción destaca.
     */
    private static final int HERMANOS_MINIMOS = 2;

    /**
     * Cierre de la incidencia #209: en una feature cuyos puertos están todos
     * cerrados a {@code ROLE_SYSTEM}, ninguno puede abrirse por
     * {@code hasAuthority} sin acotar la empresa.
     *
     * <p>
     * <b>El hallazgo.</b> Catorce {@code Reactivate…UseCase} de catálogos maestros
     * declaraban {@code hasRole('SYSTEM') or hasAuthority('X.update')} mientras
     * <em>todos</em> sus hermanos —crear, leer, listar, actualizar, borrar— eran
     * {@code hasRole('SYSTEM')} a secas. Bastaba sembrar esa authority en un rol de
     * empresa para reactivar filas de un catálogo global, que es dato compartido
     * por todos los tenants. No es una fuga de lectura: es escritura en el catálogo
     * que usan las demás empresas.
     *
     * <p>
     * <b>Por qué ninguna regla de BE-COV lo caza, y no por descuido.</b> Las cuatro
     * llevan una guarda antifalsos positivos —{@code laFeatureTieneDatosDeEmpresa}—
     * que excluye las features cuya entidad no alcanza {@code CompanyJpaEntity}.
     * Esa guarda es correcta y es lo que las mantiene sin ruido, pero un catálogo
     * maestro es exactamente lo que excluye. Este es el hueco complementario:
     * aquellas miran <em>a quién pertenece la fila</em>; esta mira <em>si el gate
     * desentona de sus hermanos</em>.
     *
     * <p>
     * <b>Las cuatro condiciones, y qué falso positivo paga cada una.</b> Un método
     * incumple solo si se cumplen todas:
     * <ol>
     * <li>su {@code @PreAuthorize} menciona {@code hasAuthority};</li>
     * <li>ni su firma recibe {@code companyId} ni su SpEL invoca
     * {@code @authz.isMyCompany}. Esta salva
     * {@code permission/ListPermissionsByCompanyUseCase}, que es precisamente el
     * «caso de uso hermano que sí recibe {@code companyId}» que prescribe la
     * sección de autorización del {@code CLAUDE.md}: sus cinco hermanos son
     * {@code hasRole('SYSTEM')} exacto y sin esta condición sería el primer falso
     * positivo;</li>
     * <li><b>todos</b> los demás puertos de la feature que llevan
     * {@code @PreAuthorize} son {@code hasRole('SYSTEM')} exacto, sin disyuntos.
     * Esta es la que decide: salva {@code company} —cuyos cinco hermanos declaran
     * {@code hasAuthority}, así que ahí la authority <em>es</em> la norma y
     * {@code ReactivateCompanyUseCase} no se alineó a propósito— y las ~24 features
     * clínicas donde pasa lo mismo. Un predicado ingenuo sin ella dispara en las 24
     * y la regla se muere de ruido el primer día;</li>
     * <li>hay al menos {@value #HERMANOS_MINIMOS} de esos hermanos, para que «la
     * norma de la feature» sea una norma y no una coincidencia de dos.</li>
     * </ol>
     *
     * <p>
     * <b>Los {@code @NoAuthorizationRequired} no cuentan como hermanos.</b> Un
     * puerto que declara por escrito que su autorización no es un JWT —el token de
     * un solo uso, la firma HMAC de un webhook— no dice nada sobre cuál es la norma
     * de la feature, y contarlo la falsearía en las dos direcciones.
     *
     * <p>
     * <b>Nace dura y en cero</b>, sobre el árbol ya alineado, que es el criterio
     * normal del repositorio para no congelar: la campaña de los catorce puertos se
     * cerró antes de escribirla. Hoy vigila <b>19</b> features —los catálogos
     * maestros, los {@code base_*} y los {@code system_*}— con entre 4 y 6 puertos
     * cada una.
     *
     * <p>
     * <b>Lo que no ve.</b> Una feature cuya norma ya está mezclada queda fuera por
     * construcción: {@code permission} tiene un hermano con {@code hasAuthority}
     * —el legítimo, el que recibe la empresa— y eso basta para que la condición (3)
     * la excluya entera. Es el precio de no tener falsos positivos, y es
     * consciente: la regla detecta al que <b>rompe</b> una norma unánime, no al que
     * está mal en una feature que ya era heterogénea. Y tampoco mira de quién es la
     * empresa que señala el {@code id} —ese es un problema distinto y peor, el de
     * la incidencia #208, y mezclarlos aquí volvería esta regla imposible de
     * mantener.
     */
    static ArchCondition<JavaMethod> noAbrirPorAuthorityLoQueLaFeatureCierraASystem() {
        return new ArchCondition<>("no abrirse por hasAuthority cuando todos los demas puertos"
                + " de la feature son hasRole('SYSTEM') exacto") {
            private final Map<String, List<JavaMethod>> gatesPorFeature = new LinkedHashMap<>();

            @Override
            public void init(Collection<JavaMethod> puertos) {
                gatesPorFeature.clear();
                for (JavaMethod puerto : puertos) {
                    if (puerto.tryGetAnnotationOfType(PreAuthorize.class).isEmpty()) {
                        continue;
                    }
                    paqueteDeLaFeature(puerto.getOwner()).ifPresent(feature -> gatesPorFeature
                            .computeIfAbsent(feature.getName(), clave -> new ArrayList<>())
                            .add(puerto));
                }
            }

            @Override
            public void check(JavaMethod puerto, ConditionEvents events) {
                Optional<PreAuthorize> gate = puerto.tryGetAnnotationOfType(PreAuthorize.class);
                if (gate.isEmpty()) {
                    return;
                }
                String expresion = gate.get().value();
                if (!expresion.contains(AUTHORITY)) {
                    return;
                }
                if (transportaCompanyId(puerto) || ISMYCOMPANY_REF.matcher(expresion).find()) {
                    return;
                }
                List<JavaMethod> hermanos = hermanosDeLaFeature(puerto, gatesPorFeature);
                if (hermanos.size() < HERMANOS_MINIMOS || !todosCerradosASystem(hermanos)) {
                    return;
                }
                events.add(new SimpleConditionEvent(puerto, false, puerto.getFullName()
                        + " se abre con " + expresion.strip() + " mientras sus " + hermanos.size()
                        + " puertos hermanos son hasRole('SYSTEM') exacto. Basta sembrar esa"
                        + " authority en un rol de empresa para alcanzar el endpoint, y la fila"
                        + " es de un catalogo global que comparten todos los tenants: no es una"
                        + " lectura ajena, es una escritura en el dato de los demas. Salidas:"
                        + " alinear el gate a hasRole('SYSTEM'), o acotar la empresa —recibir"
                        + " companyId y validarlo con @authz.isMyCompany— si de verdad este"
                        + " puerto tiene que servir a un empleado"));
            }
        };
    }

    /** Los demás puertos con gate de la misma feature; el candidato queda fuera. */
    private static List<JavaMethod> hermanosDeLaFeature(JavaMethod puerto,
            Map<String, List<JavaMethod>> gatesPorFeature) {
        return paqueteDeLaFeature(puerto.getOwner())
                .map(feature -> gatesPorFeature.getOrDefault(feature.getName(), List.of()).stream()
                        .filter(hermano -> !hermano.equals(puerto)).toList())
                .orElse(List.of());
    }

    private static boolean todosCerradosASystem(List<JavaMethod> hermanos) {
        return hermanos.stream().map(hermano -> hermano.getAnnotationOfType(PreAuthorize.class))
                .allMatch(gate -> soloAlcanzablePorSystem(gate.value()));
    }

    // ── #196: el literal booleano en la proyección ───────────────────────────

    /**
     * Las funciones de agregación. Lo que hay <em>dentro</em> de sus paréntesis no
     * es una columna de salida sino un argumento, y lo que la función proyecta es
     * un número: {@code COUNT(CASE WHEN … THEN 1 END)} y
     * {@code SUM(CASE WHEN … THEN x END)} son legítimos y están hoy en
     * {@code OpenAccountJpaRepository}. Sin esta exclusión la regla nacería con
     * falsos positivos y se moriría de ruido el primer día.
     */
    private static final Set<String> AGREGADOS = Set.of("count", "sum", "avg", "min", "max");

    /** Las dos palabras que abren y cierran una lista de proyección. */
    private static final String PALABRA_SELECT = "select";

    private static final String PALABRA_FROM = "from";

    private static final Set<String> LITERALES_BOOLEANOS = Set.of("true", "false");

    /**
     * Solo se juzga la sentencia que <b>devuelve filas al programa</b>. La coerción
     * que revienta ocurre al <em>extraer</em> el resultado, así que un literal
     * booleano que va a parar a una columna nunca la sufre.
     *
     * <p>
     * El caso real que lo obliga: {@code EmployeeBranchJpaRepository} hace
     * {@code INSERT INTO employee_branches (…, enabled)}
     * {@code SELECT e.id, b.id, CURRENT_TIMESTAMP, true FROM employees e …}. Ese
     * {@code true} está sintácticamente en una lista de proyección y es
     * perfectamente correcto —alimenta la columna {@code enabled} y Hibernate no lo
     * extrae jamás—. Sin este filtro la regla nacería con una violación legítima,
     * que es la forma en que muere una regla nueva. Por lo mismo quedan fuera las
     * subconsultas del {@code SET} de un {@code UPDATE}.
     */
    private static final Pattern SENTENCIA_QUE_CONSULTA = Pattern.compile("^select\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Cierre de la incidencia #196: ninguna {@code @Query} puede llevar un literal
     * booleano en su <b>lista de proyección</b>.
     *
     * <p>
     * <b>El defecto que la justifica tuvo la facturación electrónica caída al 100
     * %</b> (#185). {@code MembershipSubModuleJpaRepository} preguntaba si una
     * membresía tenía habilitado un submódulo con
     * {@code SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END}. Con Hibernate
     * 7 esa expresión se tipa como {@code Integer} al extraer el resultado, así que
     * la coerción del literal {@code Boolean} lanza
     * {@code Cannot coerce value 'true' [java.lang.Boolean] to Integer} — el 100 %
     * de las veces, no un porcentaje. Y esa consulta es la <b>primera</b> lectura a
     * base de datos de toda emisión, transmisión, reconciliación y webhook DIAN, de
     * modo que el módulo entero quedó caído sin que ninguna prueba lo viera: su
     * único uso en el árbol de test era un mock.
     *
     * <p>
     * <b>Por qué una regla y no solo la rodaja de integración.</b> Un
     * {@code @DataJpaTest} contra MySQL real caza <em>esa</em> consulta —y
     * {@code MembershipSubModulePersistenceIT} ya la ejecuta—, pero solo esa: el
     * defecto sobrevivió meses precisamente porque nadie había escrito la rodaja.
     * Esta regla no depende de que exista rodaja; mira todas las {@code @Query} que
     * haya.
     *
     * <p>
     * <b>Qué cuenta como proyección.</b> Lo que va entre un {@code SELECT} y su
     * {@code FROM} <em>a la misma profundidad de paréntesis</em>. Eso deja fuera,
     * sin enumerarlas, las tres formas legítimas que hoy existen en el repositorio:
     * el {@code ORDER BY CASE WHEN l.expireDate IS NULL THEN 1 ELSE 0 END} de
     * {@code StockLotJpaRepository}, que va detrás del {@code FROM} y ni siquiera
     * es columna de salida; los {@code COUNT(CASE WHEN …)} y
     * {@code SUM(CASE WHEN …)} de {@code OpenAccountJpaRepository}, que proyectan
     * números —ver {@link #AGREGADOS}—; y cualquier {@code WHERE enabled = false},
     * que es un filtro y no una proyección.
     *
     * <p>
     * <b>Y baja a las subconsultas.</b> Un
     * {@code (SELECT CASE WHEN … THEN true END …)} dentro de la lista de columnas
     * tiene exactamente el mismo problema de tipado, así que cada {@code SELECT}
     * lleva su propio estado de proyección en su propio nivel de paréntesis.
     *
     * <p>
     * <b>Los literales de texto se saltan enteros.</b> Un
     * {@code WHERE estado = 'true'} es una cadena, no el literal booleano del
     * lenguaje de consulta, y una búsqueda por subcadena lo daría por violación.
     *
     * <p>
     * <b>Lo que esta regla no ve</b>, por la misma limitación del modelo de
     * ArchUnit que documenta {@code UPDATE_MASIVO_MUEVE_LA_VERSION}: el SQL crudo
     * por {@code JdbcTemplate}, cuyo literal vive en el pool de constantes del
     * llamador y no en el valor de una anotación. Hoy ese hueco está vacío —el
     * único {@code JdbcTemplate} que consulta es {@code JdbcDianJobLeasePort}, que
     * actualiza, y {@code TokenCleanupRepository} solo borra—; si dejara de
     * estarlo, la herramienta sería un {@code RegexpMultiline} de Checkstyle y no
     * esta regla.
     */
    static ArchCondition<JavaMethod> proyectarSinLiteralBooleano() {
        return new ArchCondition<>("no proyectar literales booleanos: Hibernate 7 tipa la"
                + " expresion como Integer y la coercion del Boolean falla el 100%% de las veces") {
            @Override
            public void check(JavaMethod metodo, ConditionEvents events) {
                Optional<String> consulta = metodo.tryGetAnnotationOfType(Query.class)
                        .map(Query::value).map(String::strip)
                        .filter(sql -> SENTENCIA_QUE_CONSULTA.matcher(sql).find());
                if (consulta.isEmpty()) {
                    return;
                }
                List<String> literales = literalesEnLaProyeccion(consulta.get());
                events.add(new SimpleConditionEvent(metodo, literales.isEmpty(),
                        metodo.getOwner().getSimpleName() + "." + metodo.getName() + "():"
                                + " proyecta el literal booleano " + String.join(", ", literales)
                                + ". Con Hibernate 7 la expresion se tipa como Integer al extraer"
                                + " el resultado y la coercion del Boolean falla siempre, no a"
                                + " ratos: asi cayo la facturacion electronica entera en #185."
                                + " Salidas: una consulta derivada (existsBy..., countBy...) o"
                                + " proyectar el numero y comparar en Java." + " SQL actual: "
                                + unaLinea(consulta.get())));
            }
        };
    }

    /**
     * Los contenedores que Spring Data desenvuelve antes de proyectar. El tipo
     * proyectado de {@code List<FooView>} es {@code FooView}, no {@code List}.
     */
    private static final Set<String> CONTENEDORES_DE_PROYECCION = Set.of("java.util.List",
            "java.util.Optional", "java.util.Set", "java.util.Collection", "java.lang.Iterable",
            "java.util.stream.Stream", "org.springframework.data.domain.Page",
            "org.springframework.data.domain.Slice", "org.springframework.data.domain.Window");

    /**
     * Cierre de la incidencia #472, y la mitad que le faltaba a
     * {@code PROYECCION_SIN_LITERAL_BOOLEANO} (#196): ninguna
     * {@code @Query(nativeQuery = true)} puede proyectar sobre un tipo que exponga
     * una propiedad {@code Boolean} o {@code boolean}.
     *
     * <p>
     * <b>Por qué la regla de #196 no vio este defecto.</b> Aquella mira el <b>texto
     * del SQL</b> y prohíbe el literal ({@code THEN true}); esta mira el <b>tipo de
     * destino</b>. Son dos formas de escribir el mismo problema —una columna
     * booleana que nadie sabe convertir al extraer el resultado— y la primera solo
     * cazaba la que ya nos había mordido.
     * {@code ContractItemJpaRepository.findModuleLines} no tiene un solo literal en
     * su proyección: proyecta {@code sm.read_only_capable} y {@code ci.is_core},
     * dos columnas reales, con el alias correcto. Pasaba la regla vieja limpiamente
     * y aun así tumbaba el alta de empresa entera.
     *
     * <p>
     * <b>Por qué falla.</b> MySQL no tiene tipo booleano. El CLAUDE.md exige
     * {@code TINYINT} pelado —{@code TINYINT(1)} lo reporta el driver como
     * {@code BIT} y revienta el arranque con {@code ddl-auto: validate}—, así que
     * Connector/J entrega la columna como {@code java.lang.Byte}. Una entidad
     * gestionada no lo nota, porque Hibernate le aplica su
     * {@code preferred_boolean_jdbc_type: TINYINT}; una proyección de consulta
     * nativa <b>no pasa por ahí</b>: la resuelve el
     * {@code ProjectingMethodInterceptor} de Spring Data, cuyo
     * {@code ConversionService} no trae ningún converter {@code Byte -> Boolean}.
     * Al no poder convertir intenta tratar el getter como proyección anidada y
     * lanza {@code UnsupportedOperationException: Cannot project java.lang.Byte to
     * java.lang.Boolean}.
     *
     * <p>
     * <b>Por qué nadie lo vio durante meses.</b> El fallo no está en la consulta
     * sino en la primera fila que devuelve: con el catálogo comercial vacío la
     * consulta devolvía cero filas y el código de proyección <b>nunca llegaba a
     * ejecutarse</b>. La primera siembra con datos reales lo destapó a la primera.
     * Es justo el caso que una rodaja de persistencia no cubre si nadie escribió la
     * suya, y por eso hace falta una regla que mire todas las {@code @Query} que
     * existan, se ejecuten o no.
     *
     * <p>
     * <b>Nace dura y en cero</b>, como #196: la única violación era
     * {@code ContractModuleLineView} y quedó arreglada al cerrar #472.
     *
     * <p>
     * <b>Las entidades quedan fuera a propósito.</b> Una {@code @Query} nativa que
     * devuelve una {@code @Entity} la materializa Hibernate con sus tipos, no el
     * proyector de Spring Data, así que su {@code Boolean getEnabled()} es
     * legítimo. Incluirlas convertiría la regla en ruido sobre media docena de
     * repositorios que funcionan.
     *
     * <p>
     * La salida ante una violación no es tocar la regla ni cambiar la columna a
     * {@code TINYINT(1)} —lo prohíbe el CLAUDE.md— ni forzar un {@code CAST} en el
     * SQL —MySQL no puede devolver un booleano por mucho que se le pida—: es
     * proyectar el número y traducirlo en Java, como hacen
     * {@code JpaSubscriptionQueryPort.esCierto} y
     * {@code CompanyEntitlementJpaRepository}.
     */
    static ArchCondition<JavaMethod> proyectarSinBooleanoEnConsultaNativa() {
        return new ArchCondition<>("no proyectar booleanos en consultas nativas: MySQL entrega"
                + " TINYINT como Byte y Spring Data no sabe convertirlo") {
            @Override
            public void check(JavaMethod metodo, ConditionEvents events) {
                boolean nativa = metodo.tryGetAnnotationOfType(Query.class).map(Query::nativeQuery)
                        .orElse(Boolean.FALSE);
                if (!nativa) {
                    return;
                }
                JavaClass proyectado = tipoProyectado(metodo.getReturnType());
                List<String> booleanas = propiedadesBooleanas(proyectado);
                events.add(new SimpleConditionEvent(metodo, booleanas.isEmpty(),
                        metodo.getOwner().getSimpleName() + "." + metodo.getName() + "():"
                                + " la consulta nativa proyecta sobre " + proyectado.getSimpleName()
                                + ", que expone " + String.join(", ", booleanas)
                                + " como booleano. MySQL no tiene tipo booleano y el CLAUDE.md"
                                + " exige TINYINT pelado, asi que Connector/J entrega un Byte y"
                                + " el ProjectingMethodInterceptor de Spring Data revienta con"
                                + " 'Cannot project java.lang.Byte to java.lang.Boolean' en"
                                + " cuanto la consulta devuelve UNA fila: asi cayo el alta de"
                                + " empresa entera en #472. Salida: proyectar Byte/Integer y"
                                + " comparar contra cero en Java."));
            }
        };
    }

    /**
     * El tipo sobre el que Spring Data va a proyectar, desenvolviendo los
     * contenedores. {@code List<FooView>} proyecta sobre {@code FooView};
     * {@code Optional<List<FooView>>} también.
     */
    private static JavaClass tipoProyectado(JavaType tipo) {
        if (tipo instanceof JavaParameterizedType parametrizado
                && CONTENEDORES_DE_PROYECCION.contains(parametrizado.toErasure().getName())
                && !parametrizado.getActualTypeArguments().isEmpty()) {
            return tipoProyectado(parametrizado.getActualTypeArguments().get(0));
        }
        return tipo.toErasure();
    }

    /**
     * Las propiedades booleanas que el tipo proyectado expone. Lista vacía = el
     * tipo cumple.
     *
     * <p>
     * Solo cuentan los accesores sin argumentos: así el {@code equals(Object)} que
     * toda interfaz hereda de {@code Object} —que también devuelve {@code boolean}—
     * no convierte cada proyección del proyecto en una violación.
     */
    private static List<String> propiedadesBooleanas(JavaClass proyectado) {
        if (proyectado.isEquivalentTo(Boolean.class) || proyectado.isEquivalentTo(boolean.class)) {
            return List.of("el propio valor devuelto");
        }
        // Una @Entity la materializa Hibernate con sus tipos, no el proyector de
        // Spring Data: su Boolean es legitimo. Y lo que no es del proyecto no es una
        // proyeccion sino una columna suelta --un List<String> findCodes()--, donde
        // String trae isBlank(), isEmpty() e isLatin1() heredados de la JDK: ese fue
        // el primer falso positivo que dio esta regla al escribirla.
        if (proyectado.isAnnotatedWith("jakarta.persistence.Entity")
                || !proyectado.getName().startsWith(APP_PACKAGE + ".")) {
            return List.of();
        }
        return proyectado.getAllMethods().stream()
                .filter(metodo -> metodo.getRawParameterTypes().isEmpty())
                .filter(metodo -> metodo.getRawReturnType().isEquivalentTo(Boolean.class)
                        || metodo.getRawReturnType().isEquivalentTo(boolean.class))
                .map(JavaMethod::getName).sorted().toList();
    }

    /**
     * Los literales booleanos que una consulta pone en su lista de proyección.
     * Lista vacía = la consulta cumple.
     *
     * <p>
     * Un solo barrido de caracteres, porque las tres cosas que hay que distinguir
     * —la profundidad de paréntesis, si el paréntesis lo abrió un agregado y si el
     * {@code SELECT} de esta profundidad ya vio su {@code FROM}— son estado que una
     * expresión regular no puede llevar.
     *
     * <p>
     * Un paréntesis <b>hereda</b> el estado de proyección del nivel de fuera, y no
     * lo reinicia: un {@code NEW com.x.Dto(…, CASE WHEN … THEN true END)} o un
     * {@code CASE} entre paréntesis siguen siendo la misma lista de columnas, y su
     * literal se extrae igual. Lo que sí cambia el estado es una subconsulta, que
     * trae su propio {@code SELECT} y su propio {@code FROM} y los aplica a su
     * nivel.
     */
    private static List<String> literalesEnLaProyeccion(String consulta) {
        String sql = unaLinea(consulta).toLowerCase(Locale.ROOT);
        List<String> hallazgos = new ArrayList<>();
        // Un flag por nivel de parentesis: cada subconsulta proyecta por su cuenta.
        List<Boolean> proyectando = new ArrayList<>();
        proyectando.add(Boolean.FALSE);
        // Un flag por parentesis abierto: lo abrio una funcion de agregacion?
        Deque<Boolean> argumentoDeAgregado = new ArrayDeque<>();
        String palabraAnterior = "";
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                i = finDelLiteralDeTexto(sql, i);
                palabraAnterior = "";
                continue;
            }
            if (c == '(') {
                argumentoDeAgregado.push(AGREGADOS.contains(palabraAnterior));
                // Hereda el estado del nivel de fuera: un parentesis que NO abre
                // subconsulta —una llamada a funcion, un NEW Dto(...), un CASE
                // entre parentesis— sigue formando parte de la misma proyeccion.
                // Si lo que abre es una subconsulta, su propio SELECT/FROM lo
                // corrige acto seguido.
                proyectando.add(proyectando.get(proyectando.size() - 1));
                palabraAnterior = "";
                i++;
                continue;
            }
            if (c == ')') {
                if (!argumentoDeAgregado.isEmpty()) {
                    argumentoDeAgregado.pop();
                }
                if (proyectando.size() > 1) {
                    proyectando.remove(proyectando.size() - 1);
                }
                palabraAnterior = "";
                i++;
                continue;
            }
            if (!esCaracterDePalabra(c)) {
                palabraAnterior = "";
                i++;
                continue;
            }
            int fin = i;
            while (fin < sql.length() && esCaracterDePalabra(sql.charAt(fin))) {
                fin++;
            }
            String palabra = sql.substring(i, fin);
            int nivel = proyectando.size() - 1;
            if (PALABRA_SELECT.equals(palabra)) {
                proyectando.set(nivel, Boolean.TRUE);
            } else if (PALABRA_FROM.equals(palabra)) {
                proyectando.set(nivel, Boolean.FALSE);
            } else if (LITERALES_BOOLEANOS.contains(palabra)
                    && Boolean.TRUE.equals(proyectando.get(nivel))
                    && !argumentoDeAgregado.contains(Boolean.TRUE)) {
                hallazgos.add(palabra);
            }
            palabraAnterior = palabra;
            i = fin;
        }
        return hallazgos;
    }

    /**
     * Un identificador o palabra clave del lenguaje de consulta. El punto <b>no</b>
     * entra a propósito: así {@code l.trueValue} se parte en dos palabras y ninguna
     * de las dos es {@code true}.
     */
    private static boolean esCaracterDePalabra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '$';
    }

    /**
     * Índice justo detrás de la comilla que cierra un literal de texto, saltando
     * las comillas escapadas por duplicación. Sin esto, un {@code = 'true'} en el
     * {@code WHERE} contaría como literal booleano.
     */
    private static int finDelLiteralDeTexto(String sql, int comillaInicial) {
        int i = comillaInicial + 1;
        while (i < sql.length()) {
            if (sql.charAt(i) == '\'') {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return sql.length();
    }

    /** El paquete raíz de la aplicación, desde cualquier clase suya. */
    private static Optional<JavaPackage> paqueteRaiz(JavaClass clazz) {
        JavaPackage paquete = clazz.getPackage();
        while (!paquete.getName().equals(APP_PACKAGE)) {
            Optional<JavaPackage> padre = paquete.getParent();
            if (padre.isEmpty()) {
                return Optional.empty();
            }
            paquete = padre.get();
        }
        return Optional.of(paquete);
    }
}
