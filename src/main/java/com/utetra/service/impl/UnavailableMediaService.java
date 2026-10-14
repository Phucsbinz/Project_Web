package com.utetra.service.impl;
import com.utetra.dto.MediaUploadResult;
import com.utetra.service.MediaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service @ConditionalOnProperty(name="cloudinary.enabled",havingValue="false",matchIfMissing=true)
public class UnavailableMediaService implements MediaService {
    public MediaUploadResult uploadImage(MultipartFile file) { throw new IllegalStateException("Configure and enable Cloudinary before uploading"); }
    public void deleteOwnedAsset(String publicId) { throw new IllegalStateException("Cloudinary is not enabled"); }
}
