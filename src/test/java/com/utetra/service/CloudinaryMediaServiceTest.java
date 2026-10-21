package com.utetra.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cloudinary.Cloudinary;
import com.utetra.service.impl.CloudinaryMediaService;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CloudinaryMediaServiceTest {
  private final Cloudinary cloudinary = mock(Cloudinary.class, RETURNS_DEEP_STUBS);
  private final CloudinaryMediaService media = new CloudinaryMediaService(cloudinary, "utetra");

  private byte[] image(String format, int width) throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(width, 2, BufferedImage.TYPE_INT_RGB), format, output);
    return output.toByteArray();
  }

  @Test
  void validPngAndJpegReachProvider() throws Exception {
    when(cloudinary.uploader().upload(any(byte[].class), anyMap()))
        .thenReturn(Map.of("public_id", "utetra/evidence", "secure_url", "https://example.invalid/image"));
    for (String format : new String[] {"png", "jpeg"}) {
      var result = media.uploadImage(new MockMultipartFile("image", "image." + format,
          "image/" + format, image(format, 2)));
      assertThat(result.publicId()).isEqualTo("utetra/evidence");
    }
  }

  @Test
  void disguisedGifAndMismatchedPngNeverReachProvider() throws Exception {
    assertThatThrownBy(() -> media.uploadImage(new MockMultipartFile("image", "fake.png",
        "image/png", image("gif", 2)))).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> media.uploadImage(new MockMultipartFile("image", "fake.jpg",
        "image/jpeg", image("png", 2)))).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(cloudinary);
  }

  @Test
  void corruptedImageIsRejectedBeforeAnyProviderCall() throws Exception {
    byte[] png = image("png", 2);
    assertThatThrownBy(() -> media.uploadImage(new MockMultipartFile("image", "broken.png",
        "image/png", Arrays.copyOf(png, png.length / 2))))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(cloudinary);
  }

  @Test
  void oversizedImageIsRejectedBeforeDecodeOrUpload() {
    assertThatThrownBy(() -> media.uploadImage(new MockMultipartFile("image", "large.png",
        "image/png", new byte[5 * 1024 * 1024 + 1])))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(cloudinary);
  }

  @Test
  void excessiveDimensionsAreRejectedBeforeUpload() throws Exception {
    assertThatThrownBy(() -> media.uploadImage(new MockMultipartFile("image", "wide.png",
        "image/png", image("png", 6001)))).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(cloudinary);
  }

  @Test
  void deletionAcceptsConfirmedRemovalAndAlreadyMissingAsset() throws Exception {
    when(cloudinary.uploader().destroy(eq("utetra/evidence"), anyMap()))
        .thenReturn(Map.of("result", "ok"), Map.of("result", "not found"));
    media.deleteOwnedAsset("utetra/evidence");
    media.deleteOwnedAsset("utetra/evidence");
  }

  @Test
  void providerRejectionIsReportedInsteadOfSilentlyLosingCleanup() throws Exception {
    when(cloudinary.uploader().destroy(eq("utetra/evidence"), anyMap()))
        .thenReturn(Map.of("result", "error"));
    assertThatThrownBy(() -> media.deleteOwnedAsset("utetra/evidence"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void missingProviderResponseIsAlsoAFailure() throws Exception {
    when(cloudinary.uploader().destroy(eq("utetra/evidence"), anyMap())).thenReturn(null);
    assertThatThrownBy(() -> media.deleteOwnedAsset("utetra/evidence"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void cleanupCannotDeleteAnotherApplicationsAsset() {
    assertThatThrownBy(() -> media.deleteOwnedAsset("another-app/evidence"))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(cloudinary);
  }
}
