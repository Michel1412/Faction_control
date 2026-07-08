import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates 16x16 pixel-art item textures for Faction Control (dev fallback only).
 * Official item PNGs live in {@code src/main/resources/assets/faction_control/textures/item/}.
 * Run: {@code ./gradlew generatePlaceholderTextures}
 */
public final class TexturePlaceholderGenerator {
    private static final int SIZE = 16;
    private static final Path OUTPUT_DIR = Paths.get(
            "src", "main", "resources", "assets", "faction_control", "textures", "item"
    );

    private TexturePlaceholderGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        writePixelArt("faction_upgrade_item.png", FACTION_UPGRADE_PALETTE, FACTION_UPGRADE_ART);
        writePixelArt("raid_controller_item.png", RAID_CONTROLLER_PALETTE, RAID_CONTROLLER_ART);

        System.out.println("Generated pixel-art textures in " + OUTPUT_DIR.toAbsolutePath());
    }

    /** Industrial green PCB / magnetic card with upward arrow. */
    private static final Map<Character, Color> FACTION_UPGRADE_PALETTE = palette(
            '.', 0x00000000,
            'B', 0xFF0B3D2E,
            'G', 0xFF1B5E3B,
            'g', 0xFF2E8B57,
            'C', 0xFF3D9970,
            'c', 0xFF52B788,
            'L', 0xFF74C69D,
            'W', 0xFFE8FFF0,
            'Y', 0xFFC9A227,
            'y', 0xFFF4D35E
    );

    private static final String[] FACTION_UPGRADE_ART = {
            "................",
            "..BBBBBBBBBB....",
            ".BGGGGGGGGGGB...",
            ".BgCcCcCcCcBg...",
            ".BG.cCLLLCc.GB..",
            ".BG..CLWLC.GB...",
            ".BG..CLLLC.GB...",
            ".BG...CCC..GB...",
            ".BG...CCC..GB...",
            ".BGcCCCCCCCcGB..",
            ".BYYGGGGGGYYB...",
            ".BGGGGGGGGGGB...",
            "..BBBBBBBBBB....",
            "................",
            "................",
            "................"
    };

    /** Military pager / brick phone with alert buttons and red screen pixels. */
    private static final Map<Character, Color> RAID_CONTROLLER_PALETTE = palette(
            '.', 0x00000000,
            'D', 0xFF1E1E1E,
            'd', 0xFF3A3A3A,
            'S', 0xFF121212,
            'R', 0xFFE63946,
            'r', 0xFF9B2226,
            'Y', 0xFFF4C430,
            'y', 0xFFE8B923,
            'A', 0xFFD62828,
            'a', 0xFF6B1010,
            'H', 0xFF5C5C5C
    );

    private static final String[] RAID_CONTROLLER_ART = {
            "................",
            "..DDDDDDDDDD....",
            "..DSSSSSSSSD....",
            "..DSRrRrRrSD....",
            "..DSRrRrRrSD....",
            "..DSSSSSSSSD....",
            "..DddddddddD....",
            "..DdYyAADdD.....",
            "..DdYyAADdD.....",
            "..DHHHHHHHHD....",
            "..DddddddddD....",
            "..DDDDDDDDDD....",
            "................",
            "................",
            "................",
            "................"
    };

    private static Map<Character, Color> palette(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Palette entries must be key/value pairs.");
        }
        Map<Character, Color> palette = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            palette.put((Character) entries[i], argb((Integer) entries[i + 1]));
        }
        return palette;
    }

    private static Color argb(int argb) {
        return new Color(argb, true);
    }

    private static void writePixelArt(String fileName, Map<Character, Color> palette, String[] rows) throws IOException {
        if (rows.length != SIZE) {
            throw new IllegalArgumentException(fileName + " must be exactly " + SIZE + " rows.");
        }

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            String row = rows[y];
            if (row.length() != SIZE) {
                throw new IllegalArgumentException(fileName + " row " + y + " must be " + SIZE + " characters.");
            }
            for (int x = 0; x < SIZE; x++) {
                char key = row.charAt(x);
                Color color = palette.get(key);
                if (color == null) {
                    throw new IllegalArgumentException(fileName + " uses unknown palette key '" + key + "' at " + x + "," + y);
                }
                image.setRGB(x, y, color.getRGB());
            }
        }

        Path output = OUTPUT_DIR.resolve(fileName);
        ImageIO.write(image, "PNG", output.toFile());
        System.out.println("  -> " + output);
    }
}
