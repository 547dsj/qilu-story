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
 * 故事主表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("story")
public class Story {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String opening;
    
    private Long authorId;
    
    private LocalDateTime createTime;
}
