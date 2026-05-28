package com.qilu.story.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建故事请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoryRequest {
    private String title;
    private String opening;
}
