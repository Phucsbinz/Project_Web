package com.utetra.identity;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Validate actual bytes before any provider call, including when an adapter is mocked in tests. */
@Component
public class MediaValidation {
    public static final long IMAGE_LIMIT=5*1024*1024L,VIDEO_LIMIT=20*1024*1024L;
    public void image(MultipartFile file) {
        size(file,IMAGE_LIMIT);
        String mime=Objects.toString(file.getContentType(),"");
        if(!Set.of("image/png","image/jpeg").contains(mime)) throw IdentityException.invalid("Ảnh phải có định dạng JPEG hoặc PNG.");
        try(var stream=file.getInputStream();var input=ImageIO.createImageInputStream(stream)) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw IdentityException.invalid("Nội dung ảnh không hợp lệ.");
            var reader=readers.next();
            try {
                reader.setInput(input);
                String actual=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!(mime.equals("image/png") && actual.equals("png") || mime.equals("image/jpeg") && Set.of("jpeg","jpg").contains(actual)))
                    throw IdentityException.invalid("Loại tệp khai báo không khớp nội dung ảnh.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || width>6000 || height>6000) throw IdentityException.invalid("Kích thước ảnh tối đa 6000×6000 pixel.");
                if(reader.read(0)==null) throw IdentityException.invalid("Không thể giải mã ảnh.");
            } finally { reader.dispose(); }
        } catch(IOException ex) { throw IdentityException.invalid("Ảnh bị hỏng hoặc không đọc được."); }
    }
    public void video(MultipartFile file) {
        size(file,VIDEO_LIMIT);
        if(!"video/mp4".equals(file.getContentType())) throw IdentityException.invalid("Video phải có định dạng MP4.");
        try(var stream=file.getInputStream()) {
            byte[] bytes=stream.readNBytes(32);
            if(bytes.length<16 || !"ftyp".equals(new String(bytes,4,4,StandardCharsets.US_ASCII))) throw IdentityException.invalid("Nội dung MP4 không hợp lệ.");
            long boxSize=Integer.toUnsignedLong(ByteBuffer.wrap(bytes,0,4).getInt());
            String brand=new String(bytes,8,4,StandardCharsets.US_ASCII);
            if(boxSize<16 || boxSize>file.getSize() || !Set.of("isom","iso2","iso3","iso4","iso5","iso6","avc1","mp41","mp42","M4V ","MSNV","dash").contains(brand))
                throw IdentityException.invalid("Không nhận container audio/QuickTime hoặc MP4 bị hỏng.");
        } catch(IOException ex) { throw IdentityException.invalid("Không đọc được video."); }
    }
    private void size(MultipartFile file,long limit) {
        if(file==null || file.isEmpty()) throw IdentityException.invalid("Vui lòng chọn tệp không rỗng.");
        if(file.getSize()>limit) throw new IdentityException(HttpStatus.PAYLOAD_TOO_LARGE,"Ảnh tối đa 5MB; video tối đa 20MB.");
    }
}
