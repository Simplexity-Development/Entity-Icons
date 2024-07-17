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
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<byte[]> getImage(@PathVariable String category, @PathVariable String filename, @PathVariable int size) {
        String filePath = "file:" + assetsBasePath + "/png_files/" + category + "/" + size + "x" + size + "/" + filename + ".png";
        Resource resource = resourceLoader.getResource(filePath);
        if (resource.exists()) {
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] imageBytes = StreamUtils.copyToByteArray(inputStream);
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, "image/png");

                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}


