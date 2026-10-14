package com.hotel.app.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @JsonAlias("otpCode")
    private String otp;

    public void setOtp(String otp) {
        this.otp = otp != null ? otp.trim() : null;
    }
}
