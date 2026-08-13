package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.dto.request.LoginRequest;
import com.hufsphere.linkboard.dto.request.SignupRequest;
import com.hufsphere.linkboard.dto.response.LoginResponse;
import com.hufsphere.linkboard.dto.response.SignupResponse;
import com.hufsphere.linkboard.exception.DuplicateUsernameException;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("이미 사용 중인 아이디입니다");
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
                .orElseThrow(() -> new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다");
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getUsername());

        return LoginResponse.of(user, accessToken, refreshToken);
    }
}
