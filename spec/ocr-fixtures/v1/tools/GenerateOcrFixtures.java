import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;

/** Generates the project-owned OCR corpus. Run from the repository root with JDK 17. */
public final class GenerateOcrFixtures {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 512;
    private static final Path ROOT = Path.of("spec", "ocr-fixtures", "v1");
    private static final Path IMAGES = ROOT.resolve("images");
    private static final List<String> VARIANTS = List.of(
        "clean", "rotated", "low_contrast", "small", "curved", "blurred"
    );
    private static final List<FixtureCase> CASES = List.of(
        new FixtureCase("latin_e27", "E27 220-240 V", "{\"base\":\"E27\",\"voltage\":\"220-240\"}"),
        new FixtureCase("cyrillic_e27", "Е27 220-240 В", "{\"base\":\"E27\",\"voltage\":\"220-240\"}"),
        new FixtureCase("latin_e14", "E14 230 V 6 W", "{\"base\":\"E14\",\"voltage\":\"230\",\"power\":\"6\"}"),
        new FixtureCase("cyrillic_e14", "Е14 230 В 6 Вт", "{\"base\":\"E14\",\"voltage\":\"230\",\"power\":\"6\"}"),
        new FixtureCase("latin_b22d", "B22d 230 V", "{\"base\":\"B22d\",\"voltage\":\"230\"}"),
        new FixtureCase("cyrillic_b22d", "В22д 230 В", "{\"base\":\"B22d\",\"voltage\":\"230\"}"),
        new FixtureCase("latin_gu10", "GU10 220-240 V", "{\"base\":\"GU10\",\"voltage\":\"220-240\"}"),
        new FixtureCase("cyrillic_gu10", "ГУ10 220-240 В", "{\"base\":\"GU10\",\"voltage\":\"220-240\"}"),
        new FixtureCase("latin_g9", "G9 230 V 4.5 W", "{\"base\":\"G9\",\"voltage\":\"230\",\"power\":\"4.5\"}"),
        new FixtureCase("cyrillic_g9", "Г9 230 В 4,5 Вт", "{\"base\":\"G9\",\"voltage\":\"230\",\"power\":\"4.5\"}"),
        new FixtureCase("latin_r7s", "R7s 230 V 8 W", "{\"base\":\"R7s\",\"voltage\":\"230\",\"power\":\"8\"}"),
        new FixtureCase("cyrillic_r7s", "Р7с 230 В 8 Вт", "{\"base\":\"R7s\",\"voltage\":\"230\",\"power\":\"8\"}"),
        new FixtureCase("latin_units", "50 Hz 806 lm 2700 K", "{\"frequency\":\"50\",\"lumens\":\"806\",\"kelvin\":\"2700\"}"),
        new FixtureCase("cyrillic_units", "50 Гц 806 лм 2700 К", "{\"frequency\":\"50\",\"lumens\":\"806\",\"kelvin\":\"2700\"}"),
        new FixtureCase("confusable_negative", "GУ1О 220", "{}"),
        new FixtureCase("no_text", "", "{}")
    );

    public static void main(String[] args) throws Exception {
        Files.createDirectories(IMAGES);
        List<String> entries = new ArrayList<>();
        for (FixtureCase fixture : CASES) {
            for (String variant : VARIANTS) {
                String fileName = fixture.id() + "__" + variant + ".png";
                Path output = IMAGES.resolve(fileName);
                ImageIO.write(render(fixture.text(), variant), "png", output.toFile());
                entries.add("    {\"file\":\"images/" + fileName + "\",\"case\":\""
                    + fixture.id() + "\",\"variant\":\"" + variant + "\",\"rawText\":\""
                    + jsonEscape(fixture.text()) + "\",\"expected\":" + fixture.expectedJson()
                    + ",\"sha256\":\"" + sha256(output) + "\"}");
            }
        }
        String manifest = "{\n  \"schemaVersion\":1,\n  \"license\":\"project-owned-synthetic\",\n"
            + "  \"fixtureCount\":" + entries.size() + ",\n  \"fixtures\":[\n"
            + String.join(",\n", entries) + "\n  ]\n}\n";
        Files.writeString(ROOT.resolve("manifest.json"), manifest, StandardCharsets.UTF_8);
    }

    private static BufferedImage render(String text, String variant) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color background = variant.equals("low_contrast") ? new Color(205, 205, 198) : new Color(244, 241, 225);
        Color foreground = variant.equals("low_contrast") ? new Color(150, 150, 145) : new Color(35, 38, 42);
        graphics.setColor(background);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setColor(foreground);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, variant.equals("small") ? 28 : 64));

        if (!text.isEmpty()) {
            if (variant.equals("rotated")) {
                AffineTransform original = graphics.getTransform();
                graphics.rotate(Math.toRadians(-9), WIDTH / 2.0, HEIGHT / 2.0);
                graphics.drawString(text, 75, 285);
                graphics.setTransform(original);
            } else if (variant.equals("curved")) {
                int x = 45;
                for (int i = 0; i < text.length(); i++) {
                    String glyph = text.substring(i, i + 1);
                    int y = 260 + (int) Math.round(Math.sin(i * 0.55) * 34);
                    graphics.drawString(glyph, x, y);
                    x += graphics.getFontMetrics().stringWidth(glyph);
                }
            } else {
                graphics.drawString(text, variant.equals("small") ? 210 : 55, 275);
            }
        } else {
            graphics.setStroke(new java.awt.BasicStroke(8));
            graphics.drawOval(280, 145, 180, 180);
            graphics.drawRect(570, 170, 150, 130);
        }
        graphics.dispose();

        if (!variant.equals("blurred")) return image;
        float[] kernel = new float[25];
        java.util.Arrays.fill(kernel, 1f / kernel.length);
        return new ConvolveOp(new Kernel(5, 5, kernel), ConvolveOp.EDGE_NO_OP, null).filter(image, null);
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record FixtureCase(String id, String text, String expectedJson) {}
}
