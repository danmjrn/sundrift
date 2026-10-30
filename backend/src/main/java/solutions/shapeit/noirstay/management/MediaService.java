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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import solutions.shapeit.noirstay.common.ApiException;
import solutions.shapeit.noirstay.common.Database;
import solutions.shapeit.noirstay.identity.UserAccount;

/** Validated local photograph storage and listing-reference transactions.
 * @author Daniel Jr Nkulu
 */
@Service
public class MediaService {
    private final Path root;private final ManagementService management;private final Database db;
    public MediaService(@Value("${noirstay.media-root}") String root,ManagementService management,Database db) {
        this.root=Path.of(root).toAbsolutePath().normalize();this.management=management;this.db=db;
    }
    @Transactional
    public Map<String,Object> upload(UserAccount user,UUID id,MultipartFile file) throws IOException {
        Map<String,Object> listing=management.own(user,id);
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
    @Transactional
    public Map<String,Object> remove(UserAccount user,UUID id,String url) {
        Map<String,Object> listing=management.own(user,id);
        @SuppressWarnings("unchecked") List<String> images=new ArrayList<>((List<String>)listing.get("images"));
        if(!images.remove(url)) throw ApiException.missing();
        String state=images.isEmpty()?"DRAFT":(String)listing.get("state");
        // Remove only this listing's reference. Shared fixtures and historical media remain on disk.
        db.jdbc.update("UPDATE listing SET images=?::jsonb,state=? WHERE id=?",db.json(images),state,id);
        return Map.of("images",images,"state",state);
    }
    public FileSystemResource read(String name) {
        if(!name.matches("[a-f0-9-]{36}\\.png")) throw ApiException.missing();
        Path target=root.resolve(name).normalize();if(!target.startsWith(root) || !Files.isRegularFile(target)) throw ApiException.missing();
        return new FileSystemResource(target);
    }
}
