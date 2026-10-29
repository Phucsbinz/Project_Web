package com.utetra.identity;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
public final class IdentityForms {
    private IdentityForms() {}
    public record Register(@NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,50}") String username,
        @NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=72) String password,
        @NotBlank @CodePointLength(min=1,max=100) String fullName) {}
    public record CreateAccount(@NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,50}") String username,
        @NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=72) String password,
        @NotBlank @CodePointLength(min=1,max=100) String fullName,
        @NotNull @Pattern(regexp="ROLE_USER|ROLE_VENDOR|ROLE_MANAGER|ROLE_SHIPPER") String role) {}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record SendOtp(@NotBlank @Email @Size(max=254) String email,
        @Pattern(regexp="REGISTER|PASSWORD_RESET") @NotNull String purpose) {}
    public record Verify(@NotBlank @Email @Size(max=254) String email,@Pattern(regexp="[0-9]{6}") @NotNull String otp,
        @Pattern(regexp="REGISTER|PASSWORD_RESET") @NotNull String purpose) {}
    public record Reset(@NotBlank @Size(max=200) String grant,@NotBlank @Size(min=8,max=72) String password) {}
    public record Login(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=72) String password) {}
    public record Profile(@NotBlank @CodePointLength(min=1,max=100) String fullName,@PhoneNumber String phone,@NotNull @PositiveOrZero @JsonDeserialize(using=StrictNumbers.LongValue.class) Long version) {}
    public record Password(@NotBlank @Size(max=72) String currentPassword,@NotBlank @Size(min=8,max=72) String password) {}
    public record Address(@NotBlank @CodePointLength(min=1,max=100) String recipientName,
        @NotBlank @PhoneNumber String phone,
        @NotBlank @CodePointLength(min=1,max=500) String addressDetail,@JsonDeserialize(using=StrictNumbers.BooleanValue.class) boolean isDefault) {}
    public record Review(@Positive long productId,@Positive long orderItemId,@Min(1) @Max(5) int stars,
        @CodePointLength(max=2000) String content) {}
    public record Comment(@Positive long productId,@Positive long orderItemId,@NotBlank @CodePointLength(min=50,max=2000) String content) {}
    public record Manage(@NotNull @JsonDeserialize(using=StrictNumbers.BooleanValue.class) Boolean active,@Pattern(regexp="ROLE_USER|ROLE_VENDOR|ROLE_MANAGER|ROLE_ADMIN|ROLE_SHIPPER") String role,@NotNull @PositiveOrZero @JsonDeserialize(using=StrictNumbers.LongValue.class) Long version) {}
}
