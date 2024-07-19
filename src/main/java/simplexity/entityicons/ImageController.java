package simplexity.entityicons;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


@RestController
@RequestMapping("/api/v1/textures")
public class ImageController {

    private final ResourceLoader resourceLoader;

    @Value("${assets.base-path}")
    private String assetsBasePath;

    public ImageController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping(value = "/{category}/{filename}/{size}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getImage(
            @PathVariable String category,
            @PathVariable String filename,
            @PathVariable int size,
            @RequestParam(required = false, defaultValue = "1") int scale) {
        String filePath = "file:" + assetsBasePath + "/png_files/" + category + "/" + size + "x" + size + "/" + filename + ".png";
        Resource resource = resourceLoader.getResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        }
        byte[] imageBytes;
        try (InputStream inputStream = resource.getInputStream()) {
            imageBytes = StreamUtils.copyToByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).build();
        }
        String downloadName = filename + "_" + size + "x" + size;
        if (scale > 1) {
            BufferedImage rescaledImage = Rescaler.rescaleImage(filePath, scale, size);
            if (rescaledImage == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            ByteArrayOutputStream imageByteArrayOutputStream = new ByteArrayOutputStream();
            try {
                ImageIO.write(rescaledImage, "png", imageByteArrayOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
            }
            imageBytes = imageByteArrayOutputStream.toByteArray();
            downloadName = filename + "_" + (size * scale) + "x" + (size * scale);

        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "image/png");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=%s", downloadName));
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);


    }

}


