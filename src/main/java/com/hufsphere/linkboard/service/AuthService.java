package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.dto.request.LoginRequest;
import com.hufsphere.linkboard.dto.request.RefreshTokenRequest;
import com.hufsphere.linkboard.dto.request.SignupRequest;
import com.hufsphere.linkboard.dto.request.UpdateMyInfoRequest;
import com.hufsphere.linkboard.dto.response.LoginResponse;
import com.hufsphere.linkboard.dto.response.MyInfoResponse;
import com.hufsphere.linkboard.dto.response.SignupResponse;
import com.hufsphere.linkboard.dto.response.TokenRefreshResponse;
import com.hufsphere.linkboard.dto.response.UpdateMyInfoResponse;
import com.hufsphere.linkboard.exception.DuplicateUsernameException;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<String> SUPPORTED_NATIVE_LANGS = Set.of(
            "en", "ko", "de", "ja", "zh", "es", "ms", "it", "fr", "ar", "ru"
    );

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(
                    "이미 사용 중인 아이디입니다"
            );
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nativeLang(request.getNativeLang())
                .build();

        AppUser saved = appUserRepository.save(user);

        return SignupResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "아이디 또는 비밀번호가 올바르지 않습니다"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException(
                    "아이디 또는 비밀번호가 올바르지 않습니다"
            );
        }

        String accessToken =
                jwtProvider.generateAccessToken(
                        user.getId(),
                        user.getUsername()
                );

        String refreshToken =
                jwtProvider.generateRefreshToken(
                        user.getId(),
                        user.getUsername()
                );

        return LoginResponse.of(
                user,
                accessToken,
                refreshToken
        );
    }

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다"
                        )
                );

        return MyInfoResponse.from(user);
    }

    @Transactional
    public UpdateMyInfoResponse updateMyInfo(
            Long userId,
            UpdateMyInfoRequest request
    ) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다"
                        )
                );

        if (request.nativeLang() != null
                && !request.nativeLang().isBlank()
                && !isSupportedLanguage(request.nativeLang())) {

            throw new IllegalArgumentException(
                    "nativeLang은 en, ko, de, ja, zh, es, ms, it, fr, ar, ru 중 하나여야 합니다"
            );
        }

        user.updateProfile(
                request.name(),
                request.nativeLang()
        );

        return UpdateMyInfoResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TokenRefreshResponse refreshToken(
            RefreshTokenRequest request
    ) {
        try {
            Long userId =
                    jwtProvider.getUserIdFromToken(
                            request.refreshToken()
                    );

            AppUser user = appUserRepository.findById(userId)
                    .orElseThrow(() ->
                            new InvalidCredentialsException(
                                    "리프레시 토큰이 유효하지 않습니다. 다시 로그인해주세요"
                            )
                    );

            String accessToken =
                    jwtProvider.generateAccessToken(
                            user.getId(),
                            user.getUsername()
                    );

            return new TokenRefreshResponse(accessToken);

        } catch (Exception ex) {
            throw new InvalidCredentialsException(
                    "리프레시 토큰이 유효하지 않습니다. 다시 로그인해주세요"
            );
        }
    }

    private boolean isSupportedLanguage(String nativeLang) {
        return SUPPORTED_NATIVE_LANGS.contains(nativeLang);
    }
}