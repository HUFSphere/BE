package com.hufsphere.linkboard.dto;

import java.util.List;

public record SuggestedQuestionsResponse(
        List<String> questions
) {}