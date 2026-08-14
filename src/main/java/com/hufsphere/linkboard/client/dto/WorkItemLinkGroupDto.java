package com.hufsphere.linkboard.client.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkItemLinkGroupDto {

    private int fromIndex;
    private String fromTitle;
    private List<LinkedWorkItemDto> linkedItems;
}
