package com.hufsphere.linkboard.exception;

import com.hufsphere.linkboard.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Spring 6.1+에서는 정적 리소스가 없을 때 조용히 404를 내려주는 대신
    // NoResourceFoundException을 던진다. 이 예외를 따로 잡지 않으면 아래의
    // Exception.class catch-all에 걸려 존재하지 않는 모든 경로(예: "/")가
    // 500으로 응답하게 된다.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "요청하신 경로를 찾을 수 없습니다",
                request
        );
    }

    // 요청 본문이 비어있거나, JSON 형식이 아니거나, 잘못된 인코딩(예: UTF-8이 아닌 바이트)이면
    // 이 예외가 던져진다. 별도로 잡지 않으면 Exception.class catch-all에 걸려 클라이언트 잘못인데도
    // 400이 아닌 500으로 응답하게 된다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "요청 본문을 읽을 수 없습니다. 형식과 인코딩(UTF-8)을 확인해주세요",
                request
        );
    }

    @ExceptionHandler(WorkspaceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceNotFound(
            WorkspaceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidSourceTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSourceType(
            InvalidSourceTypeException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidSyncPayloadException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSyncPayload(
            InvalidSyncPayloadException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(NotionNotConnectedException.class)
    public ResponseEntity<ErrorResponse> handleNotionNotConnected(
            NotionNotConnectedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(FigmaNotConnectedException.class)
    public ResponseEntity<ErrorResponse> handleFigmaNotConnected(
            FigmaNotConnectedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(GithubNotConnectedException.class)
    public ResponseEntity<ErrorResponse> handleGithubNotConnected(
            GithubNotConnectedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSourceNotFound(
            SourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SyncAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleSyncAlreadyRunning(
            SyncAlreadyRunningException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SourceFetchFailedException.class)
    public ResponseEntity<ErrorResponse> handleSourceFetchFailed(
            SourceFetchFailedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(NotionOAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handleNotionOAuthFailed(
            NotionOAuthFailedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(FigmaOAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handleFigmaOAuthFailed(
            FigmaOAuthFailedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(GithubOAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handleGithubOAuthFailed(
            GithubOAuthFailedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(OAuthAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleOAuthAuthentication(
            OAuthAuthenticationException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MemberInviteForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleMemberInviteForbidden(
            MemberInviteForbiddenException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(WorkspaceOrUserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceOrUserNotFound(
            WorkspaceOrUserNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(AlreadyWorkspaceMemberException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyWorkspaceMember(
            AlreadyWorkspaceMemberException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(WorkspaceAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceAccessDenied(
            WorkspaceAccessDeniedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(WorkspaceDetailNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceDetailNotFound(
            WorkspaceDetailNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse body = ErrorResponse.of(
                status,
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
    @ExceptionHandler(WorkspaceUpdateForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceUpdateForbidden(
            WorkspaceUpdateForbiddenException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }
    @ExceptionHandler(WorkspaceDeleteForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceDeleteForbidden(
            WorkspaceDeleteForbiddenException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MemberRemoveForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleMemberRemoveForbidden(
            MemberRemoveForbiddenException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(WorkspaceLeaveForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceLeaveForbidden(
            WorkspaceLeaveForbiddenException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InviteCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInviteCodeNotFound(
            InviteCodeNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SourceDeleteForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleSourceDeleteForbidden(
            SourceDeleteForbiddenException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(TeamNormNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTeamNormNotFound(
            TeamNormNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }
}