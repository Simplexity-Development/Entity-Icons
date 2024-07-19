package simplexity.entityicons;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Rescaler {

    public static BufferedImage rescaleImage(String path, int multiplier, int originalSize) {
        BufferedImage originalImage;
        try {
            path = path.substring(5);
            File file = new File(path);
            originalImage = ImageIO.read(file);
        } catch (IOException e) {
            return null;
        }
        int size = multiplier * originalSize;
        BufferedImage resizedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphicsImage = resizedImage.createGraphics();
        graphicsImage.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphicsImage.drawImage(originalImage, 0,0, size, size,null);
        graphicsImage.dispose();
        return resizedImage;
    }
}
