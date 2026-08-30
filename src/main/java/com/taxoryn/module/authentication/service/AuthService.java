package com.taxoryn.module.authentication.service;

import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.ForgotPasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LoginResponse;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.user.dto.UserDto;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse registerOrganization(RegisterOrganizationRequest request);

    UserDto registerUserByAdmin(RegisterUserByAdminRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    UserDto getMe();

    void logout(String accessToken, LogoutRequest request);

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request, String clientIp);

    void resetPassword(ResetPasswordRequest request, String clientIp);
}
