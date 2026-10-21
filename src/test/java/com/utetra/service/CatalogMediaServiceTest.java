package com.utetra.service;

import com.utetra.dto.MediaUploadResult;
import com.utetra.entity.catalog.Store;
import com.utetra.repository.catalog.*;
import com.utetra.security.UtetraPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogMediaServiceTest {
    @Mock MediaService media;
    @Mock CatalogGuard guard;
    @Mock StoreRepository stores;
    @Mock ProductRepository products;
    @Mock PlatformTransactionManager manager;
    CatalogMediaService service;
    final UtetraPrincipal principal=new UtetraPrincipal(1L,1L,"ROLE_VENDOR",Instant.now().plusSeconds(3600));
    final MockMultipartFile file=new MockMultipartFile("file","test.png","image/png",new byte[]{1});
    @BeforeEach void setup() { service=new CatalogMediaService(media,guard,stores,products,manager); when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus()); }
    @Test void providerFailureDoesNotChangeDatabase() {
        when(media.uploadImage(file)).thenThrow(new IllegalStateException("disabled"));
        assertThatThrownBy(() -> service.upload(principal,"store",1L,0,file)).isInstanceOf(IllegalStateException.class);
        verify(stores,never()).flush(); verify(media,never()).deleteOwnedAsset(any());
    }
    @Test void committedImageDeletesOldAssetAfterCommit() {
        var entity=new Store(); entity.setImagePublicId("utetra/old"); when(stores.findById(1L)).thenReturn(Optional.of(entity));
        when(media.uploadImage(file)).thenReturn(new MediaUploadResult("https://cdn.example.test/new","utetra/new","image"));
        service.upload(principal,"store",1L,0,file);
        var order=inOrder(manager,media,stores); order.verify(manager).commit(any()); order.verify(media).uploadImage(file); order.verify(stores).flush(); order.verify(manager).commit(any()); order.verify(media).deleteOwnedAsset("utetra/old");
        assertThat(entity.getImagePublicId()).isEqualTo("utetra/new");
    }
    @Test void failedTransactionCleansNewUploadAndKeepsOldAsset() {
        var entity=new Store(); entity.setImagePublicId("utetra/old"); when(stores.findById(1L)).thenReturn(Optional.of(entity));
        when(media.uploadImage(file)).thenReturn(new MediaUploadResult("https://cdn.example.test/new","utetra/new","image"));
        doThrow(new org.springframework.dao.OptimisticLockingFailureException("conflict")).when(stores).flush();
        assertThatThrownBy(() -> service.upload(principal,"store",1L,0,file)).isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        verify(media).deleteOwnedAsset("utetra/new"); verify(media,never()).deleteOwnedAsset("utetra/old"); verify(manager).rollback(any());
    }
}
