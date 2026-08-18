package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.client.dto.WorkspaceInviteResponse;
import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.RecentActivitiesResponse;
import com.hufsphere.linkboard.dto.SuggestedQuestionsResponse;
import com.hufsphere.linkboard.dto.request.WorkspaceCreateRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceJoinRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceMemberAddRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateNameRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceSettingResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceCreateResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceDetailResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceJoinResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceListResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceMemberAddResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceMemberResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceUpdateNameResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.WorkspaceMemberService;
import com.hufsphere.linkboard.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Workspace",
        description = "워크스페이스 설정 및 관리 API"
)
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMemberService workspaceMemberService;
    private final JwtProvider jwtProvider;

    @Operation(
            summary = "워크스페이스 생성",
            description = "워크스페이스를 생성하고 생성자를 leader로 등록합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "워크스페이스 생성 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceCreateResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORKSPACE_CREATED",
                                      "message": "워크스페이스가 생성되었습니다",
                                      "data": {
                                        "workspaceId": 1,
                                        "name": "HUFSphere",
                                        "ownerUserId": 1,
                                        "inviteCode": "XK79-2M9Q",
                                        "inviteCodeExpiresAt": "2026-08-25T10:00:00",
                                        "sources": [
                                          {
                                            "sourceId": 1,
                                            "sourceType": "github",
                                            "sourceRef": "HUFSphere/BE",
                                            "connStatus": "pending"
                                          }
                                        ],
                                        "createdAt": "2026-08-18T10:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이름 검증 실패 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "워크스페이스 이름은 1~50자여야 합니다",
                                      "path": "/api/v1/workspaces"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "로그인이 필요합니다",
                                      "path": "/api/v1/workspaces"
                                    }"""))),
    })
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceCreateResponse>>
    createWorkspace(
            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "name": "HUFSphere",
                              "githubRepo": "HUFSphere/BE",
                              "notionUrl": "",
                              "figmaUrl": ""
                            }"""))
            )
            @RequestBody
            WorkspaceCreateRequest request
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceCreateResponse response =
                workspaceService.createWorkspace(
                        loginUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "WORKSPACE_CREATED",
                                "워크스페이스가 생성되었습니다",
                                response
                        )
                );
    }

    @Operation(
            summary = "멤버 초대/추가",
            description = "팀장이 사용자를 워크스페이스 멤버로 추가합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "멤버 추가 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceMemberAddResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "MEMBER_ADDED",
                                      "message": "멤버가 추가되었습니다",
                                      "data": {
                                        "membershipId": 2,
                                        "workspaceId": 1,
                                        "userId": 2,
                                        "role": "member",
                                        "joinedAt": "2026-08-18T10:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "팀장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "멤버 초대는 팀장만 가능합니다",
                                      "path": "/api/v1/workspaces/1/members"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스 또는 사용자를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/members"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 워크스페이스에 속한 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 409,
                                      "error": "Conflict",
                                      "message": "이미 워크스페이스에 속한 사용자입니다",
                                      "path": "/api/v1/workspaces/1/members"
                                    }"""))),
    })
    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<WorkspaceMemberAddResponse>>
    addMember(
            @PathVariable Long workspaceId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "userId": 2,
                              "role": "member"
                            }"""))
            )
            @RequestBody
            WorkspaceMemberAddRequest request
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceMemberAddResponse response =
                workspaceMemberService.addMember(
                        workspaceId,
                        loginUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "MEMBER_ADDED",
                                "멤버가 추가되었습니다",
                                response
                        )
                );
    }

    @Operation(
            summary = "멤버 목록 조회",
            description = "워크스페이스의 멤버 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "멤버 목록 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "MEMBERS_OK",
                              "message": "멤버 목록 조회 성공",
                              "data": [
                                {
                                  "membershipId": 1,
                                  "userId": 1,
                                  "name": "박재영",
                                  "role": "leader",
                                  "joinedAt": "2026-08-12T10:00:00"
                                }
                              ]
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/members"
                                    }"""))),
    })
    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>>
    getMembers(
            @PathVariable Long workspaceId
    ) {
        List<WorkspaceMemberResponse> response =
                workspaceMemberService.getMembers(
                        workspaceId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "MEMBERS_OK",
                        "멤버 목록 조회 성공",
                        response
                )
        );
    }

    @Operation(
            summary = "멤버 내보내기",
            description = "팀장이 워크스페이스 멤버를 내보냅니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "멤버 내보내기 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "MEMBER_REMOVED",
                              "message": "멤버가 내보내졌습니다",
                              "data": null
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "팀장이 아니거나 팀장 자신을 내보내려 함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "멤버 내보내기는 팀장만 가능합니다",
                                      "path": "/api/v1/workspaces/1/members/2"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스 또는 사용자를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/members/2"
                                    }"""))),
    })
    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>>
    removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization
    ) {
        Long loginUserId =
                extractUserId(authorization);

        workspaceMemberService.removeMember(
                workspaceId,
                loginUserId,
                userId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "MEMBER_REMOVED",
                        "멤버가 내보내졌습니다",
                        null
                )
        );
    }

    @Operation(
            summary = "워크스페이스 나가기",
            description = "로그인한 사용자가 워크스페이스에서 나갑니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 나가기 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "WORKSPACE_LEFT",
                              "message": "워크스페이스에서 나갔습니다",
                              "data": null
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "팀장은 나갈 수 없거나(먼저 위임 필요) 이 워크스페이스의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "팀장은 워크스페이스를 나갈 수 없습니다. 먼저 팀장을 위임해주세요",
                                      "path": "/api/v1/workspaces/1/leave"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/leave"
                                    }"""))),
    })
    @DeleteMapping("/{workspaceId}/leave")
    public ResponseEntity<ApiResponse<Void>>
    leaveWorkspace(
            @PathVariable Long workspaceId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization
    ) {
        Long loginUserId =
                extractUserId(authorization);

        workspaceMemberService.leaveWorkspace(
                workspaceId,
                loginUserId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACE_LEFT",
                        "워크스페이스에서 나갔습니다",
                        null
                )
        );
    }

    @Operation(
            summary = "초대 코드로 참여",
            description = "초대 코드를 이용해 워크스페이스에 참여합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 참여 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceJoinResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORKSPACE_JOINED",
                                      "message": "워크스페이스에 참여했습니다",
                                      "data": {
                                        "workspaceId": 1,
                                        "name": "HUFSphere",
                                        "role": "member"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "초대 코드 형식이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "초대 코드 형식이 올바르지 않습니다",
                                      "path": "/api/v1/workspaces/join"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "유효하지 않거나 만료된 초대 코드",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "유효하지 않거나 만료된 초대 코드입니다",
                                      "path": "/api/v1/workspaces/join"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 워크스페이스에 속해 있음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 409,
                                      "error": "Conflict",
                                      "message": "이미 워크스페이스에 속해 있습니다",
                                      "path": "/api/v1/workspaces/join"
                                    }"""))),
    })
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<WorkspaceJoinResponse>>
    joinWorkspace(
            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "inviteCode": "XK79-2M9Q"
                            }"""))
            )
            @RequestBody
            WorkspaceJoinRequest request
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceJoinResponse response =
                workspaceMemberService.joinWorkspace(
                        loginUserId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACE_JOINED",
                        "워크스페이스에 참여했습니다",
                        response
                )
        );
    }

    @Operation(
            summary = "내 워크스페이스 목록",
            description = "로그인한 사용자가 속한 워크스페이스 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 목록 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "WORKSPACES_OK",
                              "message": "워크스페이스 목록 조회 성공",
                              "data": [
                                {
                                  "workspaceId": 1,
                                  "name": "HUFSphere",
                                  "myRole": "leader",
                                  "ownerUserId": 1,
                                  "sourceCount": 3,
                                  "createdAt": "2026-08-12T10:00:00"
                                }
                              ]
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "로그인이 필요합니다",
                                      "path": "/api/v1/workspaces"
                                    }"""))),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceListResponse>>>
    getMyWorkspaces(
            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization
    ) {
        Long loginUserId =
                extractUserId(authorization);

        List<WorkspaceListResponse> response =
                workspaceMemberService
                        .getMyWorkspaces(
                                loginUserId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACES_OK",
                        "워크스페이스 목록 조회 성공",
                        response
                )
        );
    }

    @Operation(
            summary = "워크스페이스 상세 조회",
            description = "로그인한 사용자가 속한 워크스페이스의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceDetailResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORKSPACE_OK",
                                      "message": "워크스페이스 조회 성공",
                                      "data": {
                                        "workspaceId": 1,
                                        "name": "HUFSphere",
                                        "ownerUserId": 1,
                                        "myRole": "leader",
                                        "memberCount": 3,
                                        "sourceCount": 2,
                                        "createdAt": "2026-08-12T10:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이 워크스페이스에 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "이 워크스페이스에 접근 권한이 없습니다",
                                      "path": "/api/v1/workspaces/1"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1"
                                    }"""))),
    })
    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>>
    getWorkspaceDetail(
            @PathVariable Long workspaceId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceDetailResponse response =
                workspaceMemberService
                        .getWorkspaceDetail(
                                workspaceId,
                                loginUserId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACE_OK",
                        "워크스페이스 조회 성공",
                        response
                )
        );
    }

    @Operation(
            summary = "워크스페이스 수정",
            description = "팀장이 워크스페이스 이름을 수정합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 수정 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceUpdateNameResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORKSPACE_UPDATED",
                                      "message": "워크스페이스가 수정되었습니다",
                                      "data": {
                                        "workspaceId": 1,
                                        "name": "HUFSphere BE팀"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이름 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "워크스페이스 이름은 1~50자여야 합니다",
                                      "path": "/api/v1/workspaces/1"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "팀장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "워크스페이스 수정은 팀장만 가능합니다",
                                      "path": "/api/v1/workspaces/1"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1"
                                    }"""))),
    })
    @PatchMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceUpdateNameResponse>>
    updateWorkspace(
            @PathVariable Long workspaceId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "name": "HUFSphere BE팀"
                            }"""))
            )
            @RequestBody
            WorkspaceUpdateNameRequest request
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceUpdateNameResponse response =
                workspaceMemberService.updateWorkspaceName(
                        workspaceId,
                        loginUserId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACE_UPDATED",
                        "워크스페이스가 수정되었습니다",
                        response
                )
        );
    }

    @Operation(
            summary = "워크스페이스 삭제",
            description = "팀장이 워크스페이스를 삭제합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 삭제 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "WORKSPACE_DELETED",
                              "message": "워크스페이스가 삭제되었습니다",
                              "data": null
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "팀장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "워크스페이스 삭제는 팀장만 가능합니다",
                                      "path": "/api/v1/workspaces/1"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1"
                                    }"""))),
    })
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<Void>>
    deleteWorkspace(
            @PathVariable Long workspaceId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization
    ) {
        Long loginUserId =
                extractUserId(authorization);

        workspaceMemberService.deleteWorkspace(
                workspaceId,
                loginUserId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACE_DELETED",
                        "워크스페이스가 삭제되었습니다",
                        null
                )
        );
    }

    @Operation(
            summary = "워크스페이스 설정 조회 (7.1)",
            description = "워크스페이스의 기본 정보 및 설정값을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 설정 조회 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceSettingResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORKSPACE_SETTING_OK",
                                      "message": "워크스페이스 설정 조회 성공",
                                      "data": {
                                        "id": 1,
                                        "name": "HUFSphere",
                                        "description": "HUFS 캡스톤 팀",
                                        "defaultLanguage": "ko",
                                        "updatedAt": "2026-08-18T10:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/workspaces/1/settings"
                                    }"""))),
    })
    @GetMapping("/{workspaceId}/settings")
    public ResponseEntity<?> getWorkspaceSettings(
            @PathVariable Long workspaceId
    ) {
        WorkspaceSettingResponse response =
                workspaceService.getWorkspaceSettings(
                        workspaceId
                );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "code", "WORKSPACE_SETTING_OK",
                        "message", "워크스페이스 설정 조회 성공",
                        "data", response
                )
        );
    }

    @Operation(
            summary = "워크스페이스 설정 수정 (7.2)",
            description = "워크스페이스의 이름, 설명, 기본 언어 등 설정을 수정합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "워크스페이스 설정 수정 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceSettingResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORKSPACE_UPDATED",
                                      "message": "워크스페이스 설정 수정 성공",
                                      "data": {
                                        "id": 1,
                                        "name": "HUFSphere",
                                        "description": "HUFS 캡스톤 팀 - BE/FE/AI",
                                        "defaultLanguage": "ko",
                                        "updatedAt": "2026-08-18T11:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/workspaces/1/settings"
                                    }"""))),
    })
    @PutMapping("/{workspaceId}/settings")
    public ResponseEntity<?> updateWorkspaceSettings(
            @PathVariable Long workspaceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "name": "HUFSphere",
                              "description": "HUFS 캡스톤 팀 - BE/FE/AI",
                              "defaultLanguage": "ko"
                            }"""))
            )
            @RequestBody WorkspaceUpdateRequest request
    ) {
        WorkspaceSettingResponse response =
                workspaceService
                        .updateWorkspaceSettings(
                                workspaceId,
                                request
                        );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "code", "WORKSPACE_UPDATED",
                        "message", "워크스페이스 설정 수정 성공",
                        "data", response
                )
        );
    }

    private Long extractUserId(
            String authorization
    ) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new InvalidCredentialsException(
                    "로그인이 필요합니다"
            );
        }

        String token =
                authorization.substring(7);

        try {
            return jwtProvider.getUserIdFromToken(
                    token
            );
        } catch (Exception ex) {
            throw new InvalidCredentialsException(
                    "로그인이 필요합니다"
            );
        }
    }

    @Operation(
            summary = "팀원 초대 코드 발급 및 재발급",
            description = "7일간 유효한 난수 초대 코드를 발급하거나 기존 코드를 재발급하여 갱신합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "초대 코드 발급 성공",
                    content = @Content(schema = @Schema(implementation = WorkspaceInviteResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "INVITATION_CODE_CREATED",
                                      "message": "초대 코드 발급 성공",
                                      "data": {
                                        "workspaceId": 1,
                                        "inviteCode": "XK79-2M9Q",
                                        "expiresAt": "2026-08-25T10:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/workspaces/1/invitations"
                                    }"""))),
    })
    @PostMapping("/{workspaceId}/invitations")
    public ResponseEntity<?> generateInviteCode(@PathVariable Long workspaceId) {
        WorkspaceInviteResponse response = workspaceService.generateOrRenewInviteCode(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "INVITATION_CODE_CREATED",
                "message", "초대 코드 발급 성공",
                "data", response
        ));
    }

    @Operation(summary = "대시보드 AI 추천 질문 조회 (5.6)", description = "워크스페이스의 작업들을 바탕으로 생성된 AI 추천 질문 3개를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 질문 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "SUGGESTED_QUESTIONS_OK",
                              "message": "추천 질문 조회 성공",
                              "data": {
                                "questions": [
                                  "이 프로젝트에서 인증 방식은 왜 JWT로 정해졌나요?",
                                  "최근에 가장 많이 논의된 기능은 무엇인가요?",
                                  "디자인 관련 결정 중 아직 개발에 반영 안 된 게 있나요?"
                                ]
                              }
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/workspaces/1/suggested-questions"
                                    }"""))),
    })
    @GetMapping("/{workspaceId}/suggested-questions")
    public ResponseEntity<?> getSuggestedQuestions(
            @PathVariable Long workspaceId,
            @Parameter(description = "응답 언어. 없으면 워크스페이스 기본 언어", example = "ko")
            @RequestParam(required = false) String lang
    ) {
        SuggestedQuestionsResponse response = workspaceService.getSuggestedQuestions(workspaceId, lang);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "SUGGESTED_QUESTIONS_OK",
                "message", "추천 질문 조회 성공",
                "data", response
        ));
    }

    @Operation(summary = "대시보드 최근 활동 조회 (5.7)", description = "가장 최근에 갱신된 작업 3개를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "최근 활동 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "RECENT_ACTIVITIES_OK",
                              "message": "최근 활동 조회 성공",
                              "data": {
                                "activities": [
                                  {
                                    "workItemId": 1,
                                    "sourceType": "GITHUB",
                                    "itemType": "PR",
                                    "title": "Add JWT auth",
                                    "status": "MERGED",
                                    "sourceUrl": "https://github.com/org/repo/pull/142",
                                    "sourceUpdatedAt": "2026-08-18T09:00:00"
                                  }
                                ]
                              }
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/workspaces/1/recent-activities"
                                    }"""))),
    })
    @GetMapping("/{workspaceId}/recent-activities")
    public ResponseEntity<?> getRecentActivities(@PathVariable Long workspaceId) {
        RecentActivitiesResponse response = workspaceService.getRecentActivities(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "RECENT_ACTIVITIES_OK",
                "message", "최근 활동 조회 성공",
                "data", response
        ));
    }
}