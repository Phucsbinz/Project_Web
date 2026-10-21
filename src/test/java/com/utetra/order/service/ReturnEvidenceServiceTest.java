package com.utetra.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.utetra.dto.MediaUploadResult;
import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.service.MediaService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ReturnEvidenceServiceTest {
  final OrderWorkflowService workflow = mock(OrderWorkflowService.class);
  final MediaService media = mock(MediaService.class);
  final Actor buyer = new Actor(1, "ROLE_USER");
  final MockMultipartFile image =
      new MockMultipartFile("image", "test.png", "image/png", new byte[] {1});
  final MediaUploadResult asset =
      new MediaUploadResult("https://test.invalid/evidence.png", "utetra/test", "image");

  @Test
  void invalidReasonOrOwnerNeverUploadsAnAsset() {
    var service = new ReturnEvidenceService(workflow, media, true);
    assertThatThrownBy(() -> service.submit(buyer, 1, " ", image)).isInstanceOf(OrderProblem.class);
    doThrow(OrderProblem.missing()).when(workflow).validateReturnEligibility(buyer, 1);
    assertThatThrownBy(() -> service.submit(buyer, 1, "Quality problem", image))
        .isInstanceOf(OrderProblem.class);
    verifyNoInteractions(media);
  }

  @Test
  void disabledProviderStillAllowsReasonOnlyAndRefusesImage() {
    var service = new ReturnEvidenceService(workflow, media, false);
    when(workflow.requestReturn(buyer, 1, "Reason")).thenReturn(Map.of("id", 1));
    assertThat(service.submit(buyer, 1, "Reason", null)).containsEntry("id", 1);
    assertThatThrownBy(() -> service.submit(buyer, 1, "Reason", image))
        .isInstanceOf(OrderProblem.class);
    verifyNoInteractions(media);
  }

  @Test
  void successfulUploadPersistsOnlyTheServerOwnedAsset() {
    when(media.uploadImage(image)).thenReturn(asset);
    when(workflow.requestReturnWithEvidence(buyer, 1, "Reason", asset)).thenReturn(Map.of("id", 1));
    assertThat(new ReturnEvidenceService(workflow, media, true).submit(buyer, 1, "Reason", image))
        .containsEntry("id", 1);
    verify(media, never()).deleteOwnedAsset(anyString());
  }

  @Test
  void transactionFailureCleansTheUploadedAssetAndPreservesTheOriginalFailure() {
    when(media.uploadImage(image)).thenReturn(asset);
    var failure = OrderProblem.conflict("Window expired during upload");
    when(workflow.requestReturnWithEvidence(buyer, 1, "Reason", asset)).thenThrow(failure);
    assertThatThrownBy(
            () ->
                new ReturnEvidenceService(workflow, media, true).submit(buyer, 1, "Reason", image))
        .isSameAs(failure);
    verify(media).deleteOwnedAsset(asset.publicId());
  }

  @Test
  void providerFailureDoesNotCreateAReturnRequest() {
    when(media.uploadImage(image)).thenThrow(new IllegalStateException("Provider unavailable"));
    assertThatThrownBy(
            () ->
                new ReturnEvidenceService(workflow, media, true).submit(buyer, 1, "Reason", image))
        .isInstanceOf(OrderProblem.class);
    verify(workflow, never()).requestReturnWithEvidence(any(), anyLong(), anyString(), any());
  }

  @Test
  void invalidImageGivesValidationErrorWithoutCreatingReturn() {
    when(media.uploadImage(image)).thenThrow(new IllegalArgumentException("Bad MIME"));
    assertThatThrownBy(() -> new ReturnEvidenceService(workflow, media, true)
        .submit(buyer, 1, "Reason", image))
        .isInstanceOfSatisfying(OrderProblem.class, problem -> {
          assertThat(problem.status()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
          assertThat(problem.getMessage()).contains("JPEG/PNG", "5 MB");
        });
    verify(workflow, never()).requestReturnWithEvidence(any(), anyLong(), anyString(), any());
  }

  @Test
  void failedCleanupPreservesOriginalFailureForReconciliation() {
    when(media.uploadImage(image)).thenReturn(asset);
    var failure = OrderProblem.conflict("Window expired during upload");
    var cleanup = new IllegalStateException("Provider rejected deletion");
    when(workflow.requestReturnWithEvidence(buyer, 1, "Reason", asset)).thenThrow(failure);
    doThrow(cleanup).when(media).deleteOwnedAsset(asset.publicId());
    assertThatThrownBy(() -> new ReturnEvidenceService(workflow, media, true)
        .submit(buyer, 1, "Reason", image)).isSameAs(failure);
    assertThat(failure.getSuppressed()).containsExactly(cleanup);
  }
}
