package com.vetsoftware.app.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * {@code S3Config} no es un builder trivial: decide credenciales explicitas vs.
 * la cadena por defecto, y si forzar un endpoint (LocalStack/MinIO). Esa logica
 * condicional propia es lo que se prueba aqui, sin levantar contexto de Spring
 * ni tocar red: {@link S3Client#builder()} solo ensambla el cliente, no
 * resuelve credenciales ni abre conexiones.
 */
@DisplayName("S3Config — construccion condicional del cliente S3")
class S3ConfigTest {

    private final S3Config config = new S3Config();

    @Nested
    @DisplayName("selección de credenciales")
    class SeleccionDeCredenciales {

        @Test
        @DisplayName("con access key y secret key usa credenciales estáticas")
        void con_access_key_y_secret_key_usa_credenciales_estaticas() throws Exception {
            S3Properties properties = new S3Properties("mi-bucket", "us-east-1", null, "AKID",
                    "SECRET", false);

            AwsCredentialsProvider provider = invokeCredentialsProvider(properties);

            assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("AKID");
            assertThat(provider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET");
        }

        @Test
        @DisplayName("sin credenciales explícitas usa la cadena por defecto (env vars / rol IAM)")
        void sin_credenciales_explicitas_usa_la_cadena_por_defecto() throws Exception {
            S3Properties properties = new S3Properties("mi-bucket", "us-east-1", null, null, null,
                    false);

            AwsCredentialsProvider provider = invokeCredentialsProvider(properties);

            assertThat(provider).isInstanceOf(DefaultCredentialsProvider.class);
        }

        @Test
        @DisplayName("con solo una de las dos credenciales cae también a la cadena por defecto")
        void con_solo_una_credencial_cae_a_la_cadena_por_defecto() throws Exception {
            S3Properties properties = new S3Properties("mi-bucket", "us-east-1", null, "AKID", null,
                    false);

            assertThat(invokeCredentialsProvider(properties))
                    .isInstanceOf(DefaultCredentialsProvider.class);
        }

        private AwsCredentialsProvider invokeCredentialsProvider(S3Properties properties)
                throws Exception {
            Method credentialsProvider = S3Config.class.getDeclaredMethod("credentialsProvider",
                    S3Properties.class);
            credentialsProvider.setAccessible(true);
            return (AwsCredentialsProvider) credentialsProvider.invoke(config, properties);
        }
    }

    @Nested
    @DisplayName("bean s3Client")
    class BeanS3Client {

        @Test
        @DisplayName("sin endpoint configurado no fuerza un endpoint override")
        void sin_endpoint_configurado_no_fuerza_endpoint_override() {
            S3Properties properties = new S3Properties("mi-bucket", "us-east-1", null, "AKID",
                    "SECRET", false);

            try (S3Client client = config.s3Client(properties)) {
                assertThat(client.serviceClientConfiguration().region())
                        .isEqualTo(Region.of("us-east-1"));
                assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
            }
        }

        @Test
        @DisplayName("con endpoint configurado (LocalStack/MinIO) fuerza el endpoint override")
        void con_endpoint_configurado_fuerza_endpoint_override() {
            S3Properties properties = new S3Properties("mi-bucket", "us-east-1",
                    "http://localhost:4566", "AKID", "SECRET", true);

            try (S3Client client = config.s3Client(properties)) {
                assertThat(client.serviceClientConfiguration().endpointOverride())
                        .contains(URI.create("http://localhost:4566"));
            }
        }
    }
}
