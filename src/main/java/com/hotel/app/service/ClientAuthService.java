package com.hotel.app.service;

import com.hotel.app.dto.request.ClientRegisterRequest;
import com.hotel.app.dto.request.VerifyOtpRequest;

public interface ClientAuthService {
    void registerClient(ClientRegisterRequest request);
    void verifyOtp(VerifyOtpRequest request);
    void forgotPassword(com.hotel.app.dto.request.ForgotPasswordRequest request);
    void resetPassword(com.hotel.app.dto.request.ResetPasswordRequest request);
}
