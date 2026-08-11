package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "소스 연결 요청")
public class SourceConnectionCreateRequest {

    @Schema(description = "연결할 소스 타입", example = "github", allowableValues = {"github", "notion", "figma"})
    @NotBlank(message = "sourceType은 필수입니다")
    private String sourceType;

    @Schema(description = "소스 식별자 (레포 풀네임 등)", example = "pypa/sampleproject")
    @NotBlank(message = "sourceRef는 필수입니다")
    private String sourceRef;
}
