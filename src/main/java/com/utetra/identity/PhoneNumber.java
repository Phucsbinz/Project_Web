package com.utetra.identity;
import jakarta.validation.*;
import java.lang.annotation.*;
@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=PhoneNumber.Check.class)
public @interface PhoneNumber {
    String message() default "Số điện thoại cần 8–15 chữ số, có thể dùng +, dấu cách, ngoặc hoặc dấu gạch ngang.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    class Check implements ConstraintValidator<PhoneNumber,String> {
        public boolean isValid(String value,ConstraintValidatorContext context) { return IdentityValidation.validPhone(value); }
    }
}
