package com.utetra.order.service;

import com.utetra.dto.MediaUploadResult;
import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.service.MediaService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Provider calls stay outside the SQL transaction; ownership is checked before upload. */
@Service
public class ReturnEvidenceService {
  private final OrderWorkflowService workflow;
  private final MediaService media;
  private final boolean enabled;

  public ReturnEvidenceService(
      OrderWorkflowService workflow,
      MediaService media,
      @Value("${cloudinary.enabled:false}") boolean enabled) {
    this.workflow = workflow;
    this.media = media;
    this.enabled = enabled;
  }

  public Map<String, Object> submit(Actor actor, long orderId, String reason, MultipartFile image) {
    reason = OrderSupport.required(reason, 2000);
    if (image == null || image.isEmpty()) return workflow.requestReturn(actor, orderId, reason);
    workflow.validateReturnEligibility(actor, orderId);
    if (!enabled)
      throw new OrderProblem(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Chưa bật dịch vụ ảnh. Bạn có thể gửi lý do trước; ảnh cần cấu hình Cloudinary chung.");
    MediaUploadResult uploaded;
    try {
      uploaded = media.uploadImage(image);
    } catch (IllegalArgumentException failure) {
      throw OrderProblem.invalid(
          "Ảnh không hợp lệ. Chọn ảnh JPEG/PNG đọc được, tối đa 5 MB và 6000 px mỗi chiều.");
    } catch (IllegalStateException failure) {
      throw new OrderProblem(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Không tải được ảnh chứng cứ. Hãy thử lại hoặc gửi lý do trước.");
    }
    try {
      if (uploaded == null
          || uploaded.publicId() == null
          || uploaded.publicId().isBlank()
          || uploaded.secureUrl() == null
          || !uploaded.secureUrl().startsWith("https://")
          || !"image".equals(uploaded.resourceType()))
        throw OrderProblem.invalid("Dịch vụ ảnh trả kết quả không hợp lệ.");
      return workflow.requestReturnWithEvidence(actor, orderId, reason, uploaded);
    } catch (RuntimeException failure) {
      if (uploaded == null || uploaded.publicId() == null) throw failure;
      try {
        media.deleteOwnedAsset(uploaded.publicId());
      } catch (RuntimeException cleanup) {
        failure.addSuppressed(cleanup);
        org.slf4j.LoggerFactory.getLogger(getClass())
            .error(
                "Return evidence cleanup failed; asset requires manual reconciliation: {}",
                uploaded.publicId());
      }
      throw failure;
    }
  }
}
