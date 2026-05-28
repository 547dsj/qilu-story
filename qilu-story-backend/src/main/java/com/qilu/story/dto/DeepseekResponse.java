package com.qilu.story.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DeepSeek API 响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeepseekResponse {
    private OptionInfo optionA;
    private OptionInfo optionB;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionInfo {
        private String label;
        private String content;
        private Boolean isEnding;
        
        // getter 带默认值
        public String getLabel() {
            return (label == null || label.isEmpty()) ? "继续" : label;
        }
        
        public String getContent() {
            return (content == null || content.isEmpty()) ? "故事继续..." : content;
        }
        
        public Boolean getIsEnding() {
            return isEnding == null ? false : isEnding;
        }
    }
}