package solutions.shapeit.noirstay.management;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import solutions.shapeit.noirstay.common.ApiException;
import solutions.shapeit.noirstay.common.Database;
import solutions.shapeit.noirstay.identity.Identity;

/** Persistent local photograph adapter re-encodes images and never trusts supplied paths.
 * @author Daniel Jr Nkulu
 */
@RestController
public class MediaController {
    private final Path root;private final ManagementService management;private final Identity identity;private final Database db;
    public MediaController(@Value("${noirstay.media-root}") String root,ManagementService management,Identity identity,Database db) {
        this.root=Path.of(root).toAbsolutePath().normalize();this.management=management;this.identity=identity;this.db=db;
    }
    @Transactional
    @PostMapping(value="/api/v1/host/listings/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String,Object> upload(Authentication auth,@PathVariable UUID id,@RequestParam MultipartFile file) throws IOException {
        Map<String,Object> listing=management.own(identity.role(auth,"HOST"),id);
        if(file.isEmpty() || file.getSize()>5*1024*1024 || !List.of("image/jpeg","image/png").contains(file.getContentType()))
            throw new ApiException(400,"INVALID_IMAGE","Upload a JPEG or PNG up to 5 MB.");
        @SuppressWarnings("unchecked") List<String> images=new ArrayList<>((List<String>)listing.get("images"));
        if(images.size()>=12) throw ApiException.conflict("A listing supports up to twelve photographs.");
        BufferedImage decoded;
        try(ImageInputStream stream=ImageIO.createImageInputStream(file.getInputStream())) {
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext()) throw new ApiException(400,"INVALID_IMAGE","The image could not be decoded.");
            ImageReader reader=readers.next();
            try {
                String format=reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if(!List.of("jpeg","jpg","png").contains(format)) throw new ApiException(400,"INVALID_IMAGE","Only JPEG and PNG content is accepted.");
                reader.setInput(stream);long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
                if(pixels>24_000_000 || pixels<10000) throw new ApiException(400,"IMAGE_DIMENSIONS","Choose an image between 100 × 100 and 24 megapixels.");
                decoded=reader.read(0);
            } finally { reader.dispose(); }
        } catch(IOException e) {
            throw new ApiException(400,"INVALID_IMAGE","The photograph is damaged or cannot be decoded.");
        }
        Files.createDirectories(root);String name=UUID.randomUUID()+".png";Path target=root.resolve(name);
        ImageIO.write(decoded,"png",target.toFile());String url="/media/"+name;images.add(url);
        db.jdbc.update("UPDATE listing SET images=?::jsonb WHERE id=?",db.json(images),id);
        return Map.of("url",url,"images",images);
    }
    @GetMapping("/media/{name}") ResponseEntity<FileSystemResource> read(@PathVariable String name) {
        if(!name.matches("[a-f0-9-]{36}\\.png")) throw ApiException.missing();
        Path target=root.resolve(name).normalize();if(!target.startsWith(root) || !Files.isRegularFile(target)) throw ApiException.missing();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(new FileSystemResource(target));
    }
}
