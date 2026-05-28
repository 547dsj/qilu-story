package com.qilu.story.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qilu.story.entity.Story;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故事 Mapper
 */
@Mapper
public interface StoryMapper extends BaseMapper<Story> {
}
