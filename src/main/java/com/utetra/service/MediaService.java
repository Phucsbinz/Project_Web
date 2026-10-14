package com.utetra.service;
import com.utetra.dto.MediaUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaUploadResult uploadImage(MultipartFile file);
    void deleteOwnedAsset(String publicId);
}
