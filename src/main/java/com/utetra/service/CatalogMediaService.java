package com.utetra.service;

import com.utetra.repository.catalog.*;
import com.utetra.security.UtetraPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.atomic.AtomicReference;
import static com.utetra.service.CatalogGuard.*;

@Service
public class CatalogMediaService {
    private final MediaService media;
    private final CatalogGuard guard;
    private final StoreRepository stores;
    private final ProductRepository products;
    private final TransactionTemplate transaction;
    public CatalogMediaService(MediaService media,CatalogGuard guard,StoreRepository stores,ProductRepository products,PlatformTransactionManager manager) {
        this.media=media; this.guard=guard; this.stores=stores; this.products=products; this.transaction=new TransactionTemplate(manager);
    }
    public void upload(UtetraPrincipal p,String type,Long id,long suppliedVersion,MultipartFile file) {
        require(java.util.Set.of("product","store","banner").contains(type),"Loại ảnh không hợp lệ");
        transaction.executeWithoutResult(tx -> check(p,type,id));
        var uploaded=media.uploadImage(file); // No database locks held during the provider call.
        AtomicReference<String> previous=new AtomicReference<>();
        try {
            transaction.executeWithoutResult(tx -> {
                check(p,type,id);
                if("product".equals(type)) {
                    var entity=found(products.findById(id)); version(entity.getVersion(),suppliedVersion); previous.set(entity.getImagePublicId());
                    entity.setImageUrl(uploaded.secureUrl()); entity.setImagePublicId(uploaded.publicId()); products.flush();
                } else {
                    var entity=found(stores.findById(id)); version(entity.getVersion(),suppliedVersion);
                    if("banner".equals(type)) { previous.set(entity.getBannerPublicId()); entity.setBannerUrl(uploaded.secureUrl()); entity.setBannerPublicId(uploaded.publicId()); }
                    else { previous.set(entity.getImagePublicId()); entity.setImageUrl(uploaded.secureUrl()); entity.setImagePublicId(uploaded.publicId()); }
                    stores.flush();
                }
                guard.audit(p,"IMAGE",type.toUpperCase(java.util.Locale.ROOT),id);
            });
        } catch(RuntimeException ex) {
            try { media.deleteOwnedAsset(uploaded.publicId()); } catch(RuntimeException cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
        if(previous.get()!=null && !previous.get().equals(uploaded.publicId())) {
            try { media.deleteOwnedAsset(previous.get()); }
            catch(RuntimeException ex) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Catalog image committed; old asset cleanup requires retry",ex); }
        }
    }
    private void check(UtetraPrincipal p,String type,Long id) {
        guard.business(p,"product".equals(type)?found(products.findById(id)).getStoreId():id);
    }
}
