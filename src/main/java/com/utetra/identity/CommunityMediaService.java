package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import com.cloudinary.Cloudinary;
import com.utetra.dto.MediaUploadResult;
import com.utetra.service.MediaService;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CommunityMediaService {
    private final IdentityRepository db; private final MediaService images; private final ObjectProvider<Cloudinary> cloud;
    private final String folder;
    private final MediaValidation validation;
    public CommunityMediaService(IdentityRepository db,MediaService images,ObjectProvider<Cloudinary> cloud,@Value("${cloudinary.folder}") String folder,MediaValidation validation) {
        this.db=db; this.images=images; this.cloud=cloud; this.folder=folder;
        this.validation=validation;
    }
    public List<Map<String,Object>> list(String kind,long id) {
        checkKind(kind); return db.list("select secure_url,resource_type from "+kind+"_media where "+kind+"_id=? order by display_order,id",id);
    }
    public void attach(String kind,long id,List<MultipartFile> files) {
        checkKind(kind); if(files==null) return;
        if(files.stream().anyMatch(f -> f==null || f.isEmpty() && f.getOriginalFilename()!=null && !f.getOriginalFilename().isBlank())) throw IdentityException.invalid("Tệp đính kèm không được rỗng.");
        var actual=files.stream().filter(f -> !f.isEmpty()).toList();
        if(actual.size()>5) throw IdentityException.invalid("Tối đa 5 tệp đính kèm.");
        actual.forEach(this::validate);
        int index=0;
        for(var file:actual) {
            var uploaded=upload(file);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) cleanup(uploaded.publicId(),uploaded.resourceType()); }
            });
            db.update("insert into "+kind+"_media("+kind+"_id,secure_url,public_id,resource_type,display_order) values(?,?,?,?,?)",id,uploaded.secureUrl(),uploaded.publicId(),uploaded.resourceType(),index++);
        }
    }
    public void removeAll(String kind,long id) {
        checkKind(kind); var old=db.list("select public_id,resource_type from "+kind+"_media where "+kind+"_id=?",id);
        db.update("delete from "+kind+"_media where "+kind+"_id=?",id);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { old.forEach(m -> cleanup(string(m,"public_id"),string(m,"resource_type"))); }
        });
    }
    public void validate(MultipartFile file) {
        if(file!=null && "video/mp4".equals(file.getContentType())) validation.video(file);
        else validation.image(file);
    }
    private MediaUploadResult upload(MultipartFile file) {
        if(!"video/mp4".equals(file.getContentType())) return images.uploadImage(file);
        if(cloud.getIfAvailable()==null) throw new IdentityException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Dịch vụ media chưa sẵn sàng.");
        String uploadedId=null;
        try {
            // Cloudinary's video decoder must accept the media; magic bytes alone are insufficient.
            var result=cloud.getObject().uploader().upload(file.getBytes(),Map.of("folder",folder,"resource_type","video","allowed_formats",List.of("mp4")));
            String url=Objects.toString(result.get("secure_url"),""),id=Objects.toString(result.get("public_id"),"");
            uploadedId=id;
            Object duration=result.get("duration"),video=result.get("video");
            if(!url.startsWith("https://") || !id.startsWith(folder+"/") || !(duration instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()<=0 || !(video instanceof Map<?,?> track) || !track.containsKey("codec")) throw new IllegalStateException("Invalid video response");
            return new MediaUploadResult(url,id,"video");
        } catch(Exception ex) {
            if(uploadedId!=null && uploadedId.startsWith(folder+"/")) cleanup(uploadedId,"video");
            throw new IdentityException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Không thể tải video lên hoặc tệp không có luồng hình ảnh hợp lệ.");
        }
    }
    private void cleanup(String id,String type) {
        try {
            if(!id.startsWith(folder+"/")) throw new IllegalArgumentException("Invalid owned asset");
            if(type.equals("image")) images.deleteOwnedAsset(id);
            else cloud.getObject().uploader().destroy(id,Map.of("resource_type","video"));
        } catch(Exception ex) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Community media cleanup requires retry (provider unavailable)"); }
    }
    private void checkKind(String kind) { if(!Set.of("review","comment").contains(kind)) throw new IllegalArgumentException("Invalid media kind"); }
}
