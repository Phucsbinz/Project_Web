package com.utetra.service.impl;
import com.cloudinary.Cloudinary;
import com.utetra.dto.MediaUploadResult;
import com.utetra.service.MediaService;
import java.util.Map;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service @ConditionalOnProperty(name="cloudinary.enabled",havingValue="true")
public class CloudinaryMediaService implements MediaService {
    private final Cloudinary cloudinary;
    private final String folder;
    public CloudinaryMediaService(Cloudinary cloudinary,@Value("${cloudinary.folder}") String folder) { this.cloudinary=cloudinary; this.folder=folder; }
    @Override public MediaUploadResult uploadImage(MultipartFile file) {
        if (file==null || file.isEmpty() || file.getSize()>5*1024*1024L) throw new IllegalArgumentException("Image must be nonempty and at most 5 MB");
        if (!java.util.Set.of("image/jpeg","image/png").contains(file.getContentType()==null?"":file.getContentType()))
            throw new IllegalArgumentException("Only JPEG and PNG images are supported by the baseline");
        try {
            try (var stream=file.getInputStream();var input=ImageIO.createImageInputStream(stream)) {
                if (input==null) throw new IllegalArgumentException("Invalid image content");
                var readers=ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Invalid image content");
                var reader=readers.next();
                try {
                    String format=reader.getFormatName();
                    if (!("PNG".equalsIgnoreCase(format) && "image/png".equals(file.getContentType()))
                        && !("JPEG".equalsIgnoreCase(format) && "image/jpeg".equals(file.getContentType())))
                        throw new IllegalArgumentException("Image content does not match JPEG/PNG MIME type");
                    reader.setInput(input);
                    if (reader.getWidth(0)>6000 || reader.getHeight(0)>6000) throw new IllegalArgumentException("Image dimensions exceed limit");
                    if (reader.read(0)==null) throw new IllegalArgumentException("Invalid image content");
                }
                finally { reader.dispose(); }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Image cannot be decoded",exception);
        }
        try {
            Map<?,?> result=cloudinary.uploader().upload(file.getBytes(),Map.of("folder",folder,"resource_type","image"));
            Object url=result.get("secure_url"),id=result.get("public_id");
            if (url==null || id==null || !url.toString().startsWith("https://")) throw new IllegalStateException("Invalid Cloudinary response");
            return new MediaUploadResult(url.toString(),id.toString(),"image");
        } catch (IllegalArgumentException exception) { throw exception; }
        catch (Exception exception) { throw new IllegalStateException("Image upload failed",exception); }
    }
    /** Caller must obtain the public ID from a server-owned entity, never directly from a request. */
    @Override public void deleteOwnedAsset(String publicId) {
        if (publicId==null || !publicId.startsWith(folder+"/")) throw new IllegalArgumentException("Asset is outside the application folder");
        try {
            Map<?,?> result=cloudinary.uploader().destroy(publicId,Map.of("resource_type","image","invalidate",true));
            Object status=result==null?null:result.get("result");
            if (!"ok".equals(status) && !"not found".equals(status))
                throw new IllegalStateException("Cloudinary did not confirm image deletion");
        }
        catch (Exception exception) { throw new IllegalStateException("Image cleanup failed",exception); }
    }
}
