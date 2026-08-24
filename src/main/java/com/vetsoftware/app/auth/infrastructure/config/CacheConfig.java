package com.vetsoftware.app.auth.infrastructure.config;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

/**
 * Configuracion del cache Redis.
 *
 * <p>
 * <b>Por que hay un serializador POR CACHE y no uno solo</b> (incidencia #464).
 * {@code GenericJacksonJsonRedisSerializer} construido con
 * {@code builder().build()} escribe JSON <i>sin</i> informacion de tipo: un
 * {@code Set<String>} sale a Redis como {@code ["a","b"]} y, al releerlo,
 * Jackson no tiene con que reconstruir el {@code Set} y devuelve un
 * {@code ArrayList}. El proxy de {@code @Cacheable} hace el {@code checkcast}
 * al tipo declarado por el metodo y revienta con {@code ClassCastException}. El
 * sintoma es de manual y aun asi despista: la PRIMERA peticion de cada usuario
 * funciona -fallo de cache, se calcula y se guarda- y la SEGUNDA responde 500
 * desde dentro del {@code AuthFilter}, antes de llegar a ningun controlador.
 *
 * <p>
 * <b>Por que no se arregla activando el tipado por defecto.</b> Es la via corta
 * - {@code enableUnsafeDefaultTyping()} o {@code enableDefaultTyping(ptv)}
 * escriben el {@code @class} en el JSON- y tiene dos problemas. Uno de
 * seguridad: el tipado por defecto convierte cualquier escritura en Redis en
 * instanciacion de clases arbitrarias, que es la familia de gadgets de
 * deserializacion, y el cache de <b>permisos</b> es el peor sitio posible para
 * abrir esa puerta. Y otro de correccion: el {@code @class} fija la clase
 * concreta, asi que {@link java.util.Set#of()} -que
 * {@code JpaPermissionResolver} devuelve cuando el empleado no tiene roles- se
 * serializaria como {@code java.util.ImmutableCollections$SetN}, que Jackson no
 * sabe instanciar al volver. El arreglo curaria el caso general y dejaria roto
 * justo el caso borde.
 *
 * <p>
 * <b>Por que tampoco se cambia el tipo de retorno a {@code List}.</b> Seria la
 * otra via corta y reintroduce duplicados donde menos gracia hacen:
 * {@code JpaPermissionResolver} agrega los permisos de VARIOS roles y el mismo
 * codigo puede venir por dos, asi que {@code Collectors.toSet()} no es
 * cosmetica sino la deduplicacion. Ademas el {@code Set} es el tipo del puerto
 * -{@code EffectivePermissionResolver.resolveFor(Long, Set)} lo recibe como
 * argumento- y de {@code AuthContext}, de modo que el cambio se propagaria por
 * {@code application/port/out} entero para acomodar un detalle de
 * infraestructura.
 *
 * <p>
 * Lo que queda es declarar el tipo exacto de cada cache: el JSON sigue siendo
 * el mismo array compacto, no hay polimorfismo que validar y Jackson
 * reconstruye un {@code Set} porque se lo hemos dicho. El precio es que un
 * {@code @Cacheable} nuevo cuyo nombre no este en {@link #TIPOS_POR_CACHE} cae
 * en el {@link #defaultCacheConfig()} generico y repite el defecto — por eso
 * {@code CacheConfigTest} recorre {@code src/main} y rompe el build si aparece
 * un nombre sin tipo declarado.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** TTL de los caches de autorizacion. */
    private static final Duration TTL = Duration.ofMinutes(5);

    /**
     * Nombre de cache -> tipo exacto que declara el metodo {@code @Cacheable}. Es
     * la lista que {@code CacheConfigTest} contrasta contra el codigo: si no
     * cuadran, el build se pone rojo.
     */
    public static final Map<String, JavaType> TIPOS_POR_CACHE = Map.of("system-user-permissions",
            setOf(String.class), "employee-permissions", setOf(String.class), "employee-branch-ids",
            setOf(Long.class));

    private static JavaType setOf(Class<?> elemento) {
        return TypeFactory.createDefaultInstance().constructCollectionType(Set.class, elemento);
    }

    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources(ObservationRegistry observationRegistry) {
        return DefaultClientResources.builder()
                .tracing(new MicrometerTracing(observationRegistry, "Redis")).build();
    }

    /**
     * Configuracion por defecto: solo la usan los caches que NO estan en
     * {@link #TIPOS_POR_CACHE}. Hoy no hay ninguno.
     */
    @Bean
    public RedisCacheConfiguration defaultCacheConfig() {
        return base().serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(GenericJacksonJsonRedisSerializer.builder().build()));
    }

    /**
     * Registra un serializador tipado por cada cache de {@link #TIPOS_POR_CACHE}.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer typedCacheConfigurations() {
        return builder -> TIPOS_POR_CACHE
                .forEach((nombre, tipo) -> builder.withCacheConfiguration(nombre,
                        base().serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(new JacksonJsonRedisSerializer<>(tipo)))));
    }

    private static RedisCacheConfiguration base() {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(TTL)
                .disableCachingNullValues();
    }
}
