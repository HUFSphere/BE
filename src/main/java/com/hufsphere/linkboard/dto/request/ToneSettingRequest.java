package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "톤 설정 저장 요청")
public class ToneSettingRequest {

    @Schema(
            description = "프리셋 키 목록. beginner/intermediate/expert 중 하나 이상 중복 선택 가능",
            example = "[\"beginner\", \"expert\"]"
    )
    @NotEmpty(message = "presetKeys는 최소 1개 이상이어야 합니다")
    private List<String> presetKeys;

    @Schema(description = "사용자 추가 텍스트 (최대 500자)", example = "특히 Spring 관련 결정은 더 자세히 설명해주세요")
    @Size(max = 500, message = "customText는 500자를 초과할 수 없습니다")
    private String customText;
}
