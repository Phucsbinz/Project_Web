package com.utetra.identity;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
/** Called at the service boundary too, so internal callers cannot bypass HTTP validation. */
@Component
public class IdentityInputs {
    private final Validator validator;
    public IdentityInputs(Validator validator) { this.validator=validator; }
    public <T> T require(T value) {
        if(value==null || !validator.validate(value).isEmpty()) throw IdentityException.invalid("Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường bắt buộc và giới hạn độ dài.");
        return value;
    }
}
