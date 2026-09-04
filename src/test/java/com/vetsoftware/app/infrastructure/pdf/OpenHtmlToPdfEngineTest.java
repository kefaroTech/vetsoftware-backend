package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

class OpenHtmlToPdfEngineTest {

    private static final String BRAND_FONT_HTML = """
            <!DOCTYPE html>
            <html lang="es">
              <head>
                <meta charset="UTF-8"/>
                <style>
                  @page { size: A4; margin: 10mm; }
                  body { font-family: 'Inter'; font-weight: 400; }
                  .inter-semibold { font-family: 'Inter'; font-weight: 600; }
                  .inter-bold { font-family: 'Inter'; font-weight: 700; }
                  .poppins { font-family: 'Poppins'; font-weight: 400; }
                  .poppins-semibold { font-family: 'Poppins'; font-weight: 600; }
                  .poppins-bold { font-family: 'Poppins'; font-weight: 700; }
                  .mono { font-family: 'JetBrains Mono'; font-weight: 400; }
                </style>
              </head>
              <body>
                <p>Inter regular</p>
                <p class="inter-semibold">Inter semibold</p>
                <p class="inter-bold">Inter bold</p>
                <p class="poppins">Poppins regular</p>
                <p class="poppins-semibold">Poppins semibold</p>
                <p class="poppins-bold">Poppins bold</p>
                <p class="mono">JetBrains Mono</p>
              </body>
            </html>
            """;

    @Test
    void rendersValidPdfWithAccentedTextAndPagedCss() throws Exception {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofMegabytes(1), DataSize.ofMegabytes(2));
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                  <head>
                    <meta charset="UTF-8"/>
                    <style>
                      @page { size: A4; margin: 15mm; }
                      body { font-family: sans-serif; }
                      table { width: 100%; border-collapse: collapse; -fs-table-paginate: paginate; }
                      th, td { border: 1px solid #444; padding: 4px; }
                    </style>
                  </head>
                  <body>
                    <h1>Fórmula médica veterinaria</h1>
                    <table><thead><tr><th>Descripción</th></tr></thead>
                    <tbody><tr><td>Vacunación y diagnóstico</td></tr></tbody></table>
                    <img alt="QR" width="1" height="1"
                         src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="/>
                  </body>
                </html>
                """;

        byte[] pdf = engine.render(html);

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isOne();
        }
    }

    @Test
    void rejectsBlankAndOversizedInput() {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofBytes(16), DataSize.ofMegabytes(1));

        assertThatThrownBy(() -> engine.render(" ")).isInstanceOf(PdfRenderException.class)
                .hasMessageContaining("vacío");
        assertThatThrownBy(() -> engine.render("<html>documento demasiado grande</html>"))
                .isInstanceOf(PdfRenderException.class).hasMessageContaining("excede el límite");
    }

    @Test
    void stopsRenderingWhenOutputExceedsConfiguredLimit() {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofKilobytes(4), DataSize.ofBytes(128));

        assertThatThrownBy(() -> engine.render("<html><body>PDF</body></html>"))
                .isInstanceOf(PdfRenderException.class);
    }

    @Test
    @DisplayName("un HTML cuyo tamaño en bytes excede el límite se rechaza aunque su longitud en "
            + "caracteres no lo haga (caracteres multibyte UTF-8)")
    void rejectsHtmlWhoseByteSizeExceedsTheLimitEvenWhenItsCharacterCountDoesNot() {
        // 'ó' ocupa 2 bytes en UTF-8: 7 caracteres pero 14 bytes, por encima del límite
        // de 10 bytes aunque por debajo si solo se contaran caracteres.
        OpenHtmlToPdfEngine engine = engine(DataSize.ofBytes(10), DataSize.ofMegabytes(1));

        assertThatThrownBy(() -> engine.render("óóóóóóó")).isInstanceOf(PdfRenderException.class)
                .hasMessageContaining("excede el límite");
    }

    @Test
    @DisplayName("agota el tiempo de espera cuando no hay cupo de render disponible")
    void reportsTimeoutWhenNoRenderSlotIsAvailable() throws Exception {
        OpenHtmlToPdfEngine engine = new OpenHtmlToPdfEngine(new PdfProperties(1,
                Duration.ofMillis(1), DataSize.ofMegabytes(1), DataSize.ofMegabytes(1)));
        acquireTheSoleRenderSlot(engine);

        assertThatThrownBy(() -> engine.render("<html><body>PDF</body></html>"))
                .isInstanceOf(PdfRenderException.class)
                .hasMessageContaining("Tiempo de espera agotado");
    }

    @Test
    @DisplayName("una interrupción mientras espera el cupo de render se envuelve como PdfRenderException")
    void wrapsAnInterruptionWhileWaitingForARenderSlotAsPdfRenderException() {
        OpenHtmlToPdfEngine engine = engine(DataSize.ofMegabytes(1), DataSize.ofMegabytes(1));
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> engine.render("<html><body>PDF</body></html>"))
                .isInstanceOf(PdfRenderException.class).hasMessageContaining("interrumpida");

        // La producción restaura el flag de interrupción al capturar
        // InterruptedException;
        // se limpia aquí para no filtrar estado a otros tests del mismo hilo.
        Thread.interrupted();
    }

    @Nested
    @DisplayName("carga de las fuentes de marca")
    class CargaDeFuentesDeMarca {

        @ParameterizedTest
        @ValueSource(strings = {"/fonts/Inter-Regular.ttf", "/fonts/Inter-SemiBold.ttf",
                "/fonts/Inter-Bold.ttf", "/fonts/Poppins-Regular.ttf",
                "/fonts/Poppins-SemiBold.ttf", "/fonts/Poppins-Bold.ttf",
                "/fonts/JetBrainsMono-Regular.ttf"})
        @DisplayName("una fuente ausente impide construir el motor, en vez de degradar a Helvetica")
        void una_fuente_ausente_impide_construir_el_motor(String resource) throws Exception {
            Constructor<?> constructor = engineConstructorWithout(resource);

            assertThatThrownBy(() -> constructor.newInstance(properties()))
                    .isInstanceOf(InvocationTargetException.class).cause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Falta la fuente embebida")
                    .hasMessageContaining(resource);
        }

        @Test
        @DisplayName("con las siete presentes el motor arranca")
        void con_las_siete_presentes_el_motor_arranca() throws Exception {
            Constructor<?> constructor = engineConstructorWithout("/fonts/no-oculta-ninguna.ttf");

            assertThat(constructor.newInstance(properties())).isInstanceOf(HtmlToPdfEngine.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("registro de las fuentes de marca en el renderizador")
    class RegistroDeFuentesDeMarca {

        @Mock
        private PdfRendererBuilder builder;

        @Test
        @DisplayName("las siete se registran con su familia, su peso y con subsetting activado")
        void las_siete_se_registran_con_su_familia_y_su_peso() throws Exception {
            registerBrandFonts(engine(DataSize.ofMegabytes(1), DataSize.ofMegabytes(1)), builder);

            verify(builder).useFont(anyFontSupplier(), eq("Inter"), eq(400), eq(FontStyle.NORMAL),
                    eq(true));
            verify(builder).useFont(anyFontSupplier(), eq("Inter"), eq(600), eq(FontStyle.NORMAL),
                    eq(true));
            verify(builder).useFont(anyFontSupplier(), eq("Inter"), eq(700), eq(FontStyle.NORMAL),
                    eq(true));
            verify(builder).useFont(anyFontSupplier(), eq("Poppins"), eq(400), eq(FontStyle.NORMAL),
                    eq(true));
            verify(builder).useFont(anyFontSupplier(), eq("Poppins"), eq(600), eq(FontStyle.NORMAL),
                    eq(true));
            verify(builder).useFont(anyFontSupplier(), eq("Poppins"), eq(700), eq(FontStyle.NORMAL),
                    eq(true));
            verify(builder).useFont(anyFontSupplier(), eq("JetBrains Mono"), eq(400),
                    eq(FontStyle.NORMAL), eq(true));
            verifyNoMoreInteractions(builder);
        }
    }

    @Nested
    @DisplayName("fuentes embebidas en el PDF resultante")
    class FuentesEmbebidasEnElPdf {

        @Test
        @DisplayName("las tres familias de marca viajan dentro del PDF")
        void las_tres_familias_de_marca_viajan_dentro_del_pdf() throws Exception {
            byte[] pdf = engine(DataSize.ofMegabytes(1), DataSize.ofMegabytes(8))
                    .render(BRAND_FONT_HTML);

            assertThat(fontsOf(pdf)).extracting(RenderedFont::name)
                    .anyMatch(name -> name.contains("Inter"))
                    .anyMatch(name -> name.contains("Poppins"))
                    .anyMatch(name -> name.contains("JetBrains"));
        }

        @Test
        @DisplayName("ninguna familia se sustituye por la Helvetica no embebida del visor")
        void ninguna_familia_se_sustituye_por_helvetica() throws Exception {
            byte[] pdf = engine(DataSize.ofMegabytes(1), DataSize.ofMegabytes(8))
                    .render(BRAND_FONT_HTML);

            assertThat(fontsOf(pdf)).isNotEmpty()
                    .allMatch(RenderedFont::embedded, "viaja embebida en el PDF")
                    .extracting(RenderedFont::name).noneMatch(name -> name.contains("Helvetica"));
        }
    }

    private record RenderedFont(String name, boolean embedded) {
    }

    private static List<RenderedFont> fontsOf(byte[] pdf) throws IOException {
        List<RenderedFont> fonts = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                for (COSName name : resources.getFontNames()) {
                    PDFont font = resources.getFont(name);
                    fonts.add(new RenderedFont(font.getName(), font.isEmbedded()));
                }
            }
        }
        return fonts;
    }

    /**
     * {@code PdfRendererBuilder} sobrecarga {@code useFont} con un
     * {@code PDFontSupplier}, así que un {@code any()} sin tipo deja la llamada
     * ambigua y no compila.
     */
    private static FSSupplier<InputStream> anyFontSupplier() {
        return any();
    }

    private static void registerBrandFonts(OpenHtmlToPdfEngine engine, PdfRendererBuilder builder)
            throws Exception {
        Method method = OpenHtmlToPdfEngine.class.getDeclaredMethod("registerBrandFonts",
                PdfRendererBuilder.class);
        method.setAccessible(true);
        method.invoke(engine, builder);
    }

    private static Constructor<?> engineConstructorWithout(String resource) throws Exception {
        return new FontHidingClassLoader(resource).loadClass(OpenHtmlToPdfEngine.class.getName())
                .getConstructor(PdfProperties.class);
    }

    private static void acquireTheSoleRenderSlot(OpenHtmlToPdfEngine engine) throws Exception {
        Field field = OpenHtmlToPdfEngine.class.getDeclaredField("renderSlots");
        field.setAccessible(true);
        ((Semaphore) field.get(engine)).acquire();
    }

    private static PdfProperties properties() {
        return new PdfProperties(1, Duration.ofSeconds(1), DataSize.ofMegabytes(1),
                DataSize.ofMegabytes(1));
    }

    private static OpenHtmlToPdfEngine engine(DataSize maxHtmlSize, DataSize maxPdfSize) {
        return new OpenHtmlToPdfEngine(
                new PdfProperties(1, Duration.ofSeconds(1), maxHtmlSize, maxPdfSize));
    }

    /**
     * Vuelve a definir el motor en su propio espacio de nombres con uno de los
     * {@code .ttf} invisible, de modo que la ausencia se simule sin tocar el
     * classpath del resto de la suite. El motor y sus dos clases anidadas se
     * definen aquí juntos a propósito: repartir el nido entre este cargador y su
     * padre haría fallar la comprobación de nestmates con un
     * {@code IllegalAccessError}.
     */
    private static final class FontHidingClassLoader extends ClassLoader {

        private static final String ENGINE = OpenHtmlToPdfEngine.class.getName();

        private final String hiddenResource;

        private FontHidingClassLoader(String hiddenResource) {
            super(OpenHtmlToPdfEngineTest.class.getClassLoader());
            this.hiddenResource = hiddenResource.substring(1);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return hiddenResource.equals(name) ? null : super.getResourceAsStream(name);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return name.equals(ENGINE) || name.startsWith(ENGINE + "$")
                    ? defineLocally(name)
                    : super.loadClass(name, resolve);
        }

        private Class<?> defineLocally(String name) throws ClassNotFoundException {
            Class<?> alreadyDefined = findLoadedClass(name);
            return alreadyDefined != null ? alreadyDefined : defineFromParent(name);
        }

        /**
         * El {@code ProtectionDomain} de producción no es decorativo: el agente de
         * JaCoCo trae {@code inclnolocationclasses=false}, así que una clase definida
         * desde un {@code byte[]} —sin {@code CodeSource}— no se instrumenta y su
         * ejecución no se registra. Sin él, el fail-fast que estos tests recorren
         * seguiría saliendo como línea sin cubrir en el informe de cobertura.
         */
        private Class<?> defineFromParent(String name) throws ClassNotFoundException {
            try (InputStream bytecode = getParent()
                    .getResourceAsStream(name.replace('.', '/') + ".class")) {
                byte[] bytes = bytecode.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length,
                        OpenHtmlToPdfEngine.class.getProtectionDomain());
            } catch (IOException exception) {
                throw new ClassNotFoundException(name, exception);
            }
        }
    }
}
