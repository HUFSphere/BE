package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkedWorkItemDto {

    private int toIndex;
    private String toTitle;
    private String toSourceType;
    private String toUrl;
    private String linkReason;
    private double score;
}
