package com.utetra.identity;
import jakarta.validation.*;
import java.lang.annotation.*;
/** Length after strip(), in Unicode characters rather than UTF-16 code units. */
@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=CodePointLength.Check.class)
public @interface CodePointLength {
    String message() default "Độ dài nội dung không hợp lệ.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int min() default 0;
    int max();
    class Check implements ConstraintValidator<CodePointLength,String> {
        private int min,max;
        public void initialize(CodePointLength rule) { min=rule.min(); max=rule.max(); }
        public boolean isValid(String value,ConstraintValidatorContext context) {
            if(value==null) return true;
            String text=value.strip(); int length=text.codePointCount(0,text.length());
            return length>=min && length<=max;
        }
    }
}
