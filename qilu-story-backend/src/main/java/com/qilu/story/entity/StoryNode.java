package com.qilu.story.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故事节点实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("story_node")
public class StoryNode {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long storyId;
    
    private Long parentNodeId;
    
    private String content;
    
    private String optionALabel;
    
    private String optionBLabel;
    
    private Long optionANextId;
    
    private Long optionBNextId;
    
    private Integer isEnding;
    
    private LocalDateTime createTime;
}
