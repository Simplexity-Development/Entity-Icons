package simplexity.entityicons;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@RestController
@RequestMapping("/api/v1/textures")
public class ImageController {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<Integer> validSizes = new HashSet<>(List.of(8, 16));

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
        return processImageRequest(filePath, filename, size, scale);
    }

    @GetMapping(value = "/{category}/{type}/{filename}/{size}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getImageWithType(
            @PathVariable String category,
            @PathVariable String filename,
            @PathVariable int size,
            @PathVariable String type,
            @RequestParam(required = false, defaultValue = "1") int scale) {
        // TODO: No longer valid.
        String filePath = "file:" + assetsBasePath + "/png_files/" + category + "/" + size + "x" + size + "/" + type + "/" + filename + ".png";
        return processImageRequest(filePath, filename, size, scale);
    }

    // TODO: New endpoint
    @GetMapping(value="/{mob}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> findFirst(
            HttpServletRequest request,
            @PathVariable String mob,
            // Arguments applying to all mobs.
            @RequestParam(required = false, defaultValue = "16") Integer size,
            @RequestParam(required = false, defaultValue = "1") Integer scale
    )
    {
        File jsonFile = new File(assetsBasePath + "/png_files/file_path.json");
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonFile);
        }
        catch (IOException e) {
            // TODO: Change error message?
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading JSON file");
        }
        if (!validSizes.contains(size)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid size, must be 8 or 16");
        ArrayNode mobsArrayNode = findMobs(root, mob, request.getParameterMap());
        JsonNode mobNode = selectMob(mobsArrayNode);
        String assetPath = getAssetPath(mobNode, size);

        return processImageRequest(assetPath, size, scale);
    }

    private String getAssetPath(JsonNode mobNode, Integer size) {
        if (mobNode.has("dev_note") && mobNode.has("response")) {
            HttpStatus status = HttpStatus.valueOf(mobNode.get("response").asInt());
            throw new ResponseStatusException(status, mobNode.get("dev_note").asText());
        }
        JsonNode texturesNode = mobNode.get("textures");
        if (texturesNode == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Schema violation, missing textures property");
        JsonNode textureValue = texturesNode.get(size.toString());
        if (textureValue == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Schema violation, missing texture value property");
        return textureValue.asText();
    }

    private JsonNode selectMob(ArrayNode mobsArrayNode) {
        for (JsonNode mobNode : mobsArrayNode) {
            if (mobNode.has("default") && mobNode.get("default").asBoolean()) return mobNode;
        }
        for (JsonNode mobNode : mobsArrayNode) {
            if (!mobNode.has("response")) return mobNode;
        }
        return mobsArrayNode.get(0);
    }

    private ArrayNode findMobs(JsonNode root, String mob, Map<String, String[]> parameterMap) {

        JsonNode mobNode = root.get(mob);
        if (mobNode == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mob");
        JsonNode typesNode = mobNode.get("types");
        if (typesNode == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Schema violation, missing types property");
        ArrayNode typesArray = (ArrayNode) typesNode;
        ArrayNode filteredArray = objectMapper.createArrayNode();

        for (String key : parameterMap.keySet()) {
            if (key.equals("size") || key.equals("scale")) continue;
            Set<String> values = new HashSet<>(List.of(parameterMap.get(key)));
            for (JsonNode jsonNode : typesArray) {
                JsonNode dataValue = jsonNode.get(key);
                if (dataValue != null && values.contains(dataValue.asText())) filteredArray.add(jsonNode);
            }
            typesArray = filteredArray;
            filteredArray = objectMapper.createArrayNode();
        }

        if (typesArray.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No mob with those properties were found");
        return typesArray;
    }

    private ResponseEntity<byte[]> processImageRequest(String filePath, String filename, int size, int scale) {

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

    private ResponseEntity<byte[]> processImageRequest(String filePath, int size, int scale) {

        filePath = "file:" + assetsBasePath + filePath;
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
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "image/png");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=%s", filePath));
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

}


