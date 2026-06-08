import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

public class TextureGenerator {
    public static void main(String[] args) throws IOException {
        String basePath = "src/main/resources/assets/alieninvasion/textures/entity/";
        new File(basePath).mkdirs();

        // Alien Grunt: Greenish-Grey (64x32)
        generateTexture(basePath + "alien_grunt.png", 64, 32, new Color(100, 120, 100));

        // Alien Brute: Dark Grey/Red (128x128)
        generateTexture(basePath + "alien_brute.png", 128, 128, new Color(60, 50, 50));

        // Telekinetic Alien: Dark Purple/Cyan (64x64)
        generateTexture(basePath + "telekinetic_alien.png", 64, 64, new Color(40, 0, 60));

        // UFO: Silver/Green (64x64)
        generateTexture(basePath + "ufo.png", 64, 64, new Color(180, 180, 190));

        System.out.println("Textures generated successfully.");
    }

    private static void generateTexture(String path, int width, int height, Color baseColor) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int noise = random.nextInt(60) - 30;
                int r = clamp(baseColor.getRed() + noise);
                int g = clamp(baseColor.getGreen() + noise);
                int b = clamp(baseColor.getBlue() + noise);

                // Add some "features"
                if (random.nextInt(100) > 95) {
                    r = clamp(r - 40);
                    g = clamp(g - 40);
                    b = clamp(b - 40);
                }

                int col = (255 << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, col);
            }
        }

        ImageIO.write(image, "png", new File(path));
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}
