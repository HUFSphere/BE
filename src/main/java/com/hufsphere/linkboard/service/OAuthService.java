package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.GoogleOAuthClient;
import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.dto.request.OAuthLoginRequest;
import com.hufsphere.linkboard.dto.response.GoogleTokenResponse;
import com.hufsphere.linkboard.dto.response.GoogleUserInfo;
import com.hufsphere.linkboard.dto.response.OAuthLoginResponse;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final AppUserRepository appUserRepository;
    private final GoogleOAuthClient googleOAuthClient;
    private final JwtProvider jwtProvider;

    @Transactional
    public OAuthLoginResponse loginOrSignup(OAuthLoginRequest request) {

        if (!request.oauthProvider().equalsIgnoreCase("google")) {
            throw new IllegalArgumentException("지원하지 않는 소셜 제공자입니다.");
        }

        GoogleTokenResponse tokenResponse =
                googleOAuthClient.exchangeCode(request.oauthCode());

        GoogleUserInfo userInfo =
                googleOAuthClient.getUserInfo(tokenResponse.accessToken());

        String nativeLang =
                request.nativeLang() == null || request.nativeLang().isBlank()
                        ? "ko"
                        : request.nativeLang();

        AppUser existingUser = appUserRepository
                .findByOauthProviderAndOauthSubject(
                        "google",
                        userInfo.sub()
                )
                .orElse(null);

        boolean isNewUser = existingUser == null;

        AppUser user;

        if (isNewUser) {
            user = appUserRepository.save(
                    AppUser.builder()
                            .name(userInfo.name())
                            .oauthProvider("google")
                            .oauthSubject(userInfo.sub())
                            .nativeLang(nativeLang)
                            .build()
            );
        } else {
            user = existingUser;
        }

        String accessToken =
                jwtProvider.generateAccessToken(user.getId(), user.getName());

        String refreshToken =
                jwtProvider.generateRefreshToken(user.getId(), user.getName());

        return new OAuthLoginResponse(
                user.getId(),
                user.getName(),
                user.getOauthProvider(),
                user.getNativeLang(),
                accessToken,
                refreshToken,
                isNewUser
        );
    }
}