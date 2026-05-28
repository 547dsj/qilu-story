package com.qilu.story.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qilu.story.dto.DeepseekResponse;
import com.qilu.story.entity.Story;
import com.qilu.story.entity.StoryNode;
import com.qilu.story.exception.BusinessException;
import com.qilu.story.mapper.StoryMapper;
import com.qilu.story.mapper.StoryNodeMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 故事 Service
 */
@Slf4j
@Service
public class StoryService {

    @Autowired
    private StoryMapper storyMapper;

    @Autowired
    private StoryNodeMapper storyNodeMapper;

    @Autowired
    private DeepseekService deepseekService;

    /**
     * 创建故事（同步，立即返回ID）
     */
    @Transactional
    public Long createStorySync(String title, String opening, Long authorId) {
        // 保存故事主记录
        Story story = Story.builder()
                .title(title)
                .opening(opening)
                .authorId(authorId)
                .build();
        
        storyMapper.insert(story);
        return story.getId();
    }

    /**
     * 异步生成故事分支（后台生成，不阻塞）
     */
    @Async
    public void generateBranchesAsync(Long storyId, String opening) {
        log.info("开始异步生成故事分支: storyId={}", storyId);
        
        try {
            // 创建根节点
            StoryNode rootNode = StoryNode.builder()
                    .storyId(storyId)
                    .parentNodeId(null)
                    .content(opening)
                    .isEnding(0)
                    .build();
            storyNodeMapper.insert(rootNode);
            Long rootNodeId = rootNode.getId();

            // 递归生成分支（最大深度 7 层，约 1 万字）
            generateBranchRecursively(storyId, rootNodeId, opening, 1, 7);
            
            log.info("故事分支生成完成: storyId={}", storyId);
        } catch (Exception e) {
            log.error("异步生成故事分支失败: storyId={}", storyId, e);
        }
    }

    /**
     * 递归生成分支
     */
    private void generateBranchRecursively(Long storyId, Long parentNodeId, String content, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth) {
            log.info("达到最大深度 {}，停止生成分支", maxDepth);
            return;
        }
        
        try {
            log.info("生成第 {} 层分支，父节点ID: {}", currentDepth, parentNodeId);
            
            // 调用 AI 生成分支
            DeepseekResponse branchResponse = deepseekService.generateBranch(content);
            
            // 创建选项 A 节点
            StoryNode optionANode = StoryNode.builder()
                    .storyId(storyId)
                    .parentNodeId(parentNodeId)
                    .content(branchResponse.getOptionA().getContent())
                    .isEnding(branchResponse.getOptionA().getIsEnding() ? 1 : 0)
                    .build();
            storyNodeMapper.insert(optionANode);
            
            // 创建选项 B 节点
            StoryNode optionBNode = StoryNode.builder()
                    .storyId(storyId)
                    .parentNodeId(parentNodeId)
                    .content(branchResponse.getOptionB().getContent())
                    .isEnding(branchResponse.getOptionB().getIsEnding() ? 1 : 0)
                    .build();
            storyNodeMapper.insert(optionBNode);
            
            // 更新父节点的选项信息
            StoryNode parentNode = storyNodeMapper.selectById(parentNodeId);
            parentNode.setOptionALabel(branchResponse.getOptionA().getLabel());
            parentNode.setOptionBLabel(branchResponse.getOptionB().getLabel());
            parentNode.setOptionANextId(optionANode.getId());
            parentNode.setOptionBNextId(optionBNode.getId());
            storyNodeMapper.updateById(parentNode);
            
            log.info("第 {} 层分支创建成功，A节点ID: {}, B节点ID: {}", currentDepth, optionANode.getId(), optionBNode.getId());
            
            // 递归：为选项 A 生成下一层（如果不是结局）
            if (!branchResponse.getOptionA().getIsEnding()) {
                generateBranchRecursively(storyId, optionANode.getId(), 
                    branchResponse.getOptionA().getContent(), currentDepth + 1, maxDepth);
            }
            
            // 递归：为选项 B 生成下一层（如果不是结局）
            if (!branchResponse.getOptionB().getIsEnding()) {
                generateBranchRecursively(storyId, optionBNode.getId(), 
                    branchResponse.getOptionB().getContent(), currentDepth + 1, maxDepth);
            }
            
        } catch (Exception e) {
            log.error("生成第 {} 层分支失败: {}", currentDepth, e.getMessage());
        }
    }

    /**
     * 检查故事是否已生成完成
     */
    public boolean isStoryReady(Long storyId) {
        QueryWrapper<StoryNode> wrapper = new QueryWrapper<>();
        wrapper.eq("story_id", storyId);
        Long count = storyNodeMapper.selectCount(wrapper);
        return count > 1; // 有根节点以外的节点
    }

    /**
     * 获取故事根节点
     */
    public StoryNode getStoryRoot(Long storyId) {
        QueryWrapper<StoryNode> wrapper = new QueryWrapper<>();
        wrapper.eq("story_id", storyId)
               .isNull("parent_node_id");
        
        StoryNode node = storyNodeMapper.selectOne(wrapper);
        if (node == null) {
            throw new BusinessException("故事不存在");
        }
        
        return node;
    }

    /**
     * 获取故事节点
     */
    public StoryNode getStoryNode(Long nodeId) {
        StoryNode node = storyNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }
        return node;
    }

    /**
     * 获取下一个节点
     */
    public StoryNode getNextNode(Long nodeId, String choice) {
        StoryNode currentNode = getStoryNode(nodeId);
        
        Long nextNodeId = null;
        if ("A".equalsIgnoreCase(choice)) {
            nextNodeId = currentNode.getOptionANextId();
        } else if ("B".equalsIgnoreCase(choice)) {
            nextNodeId = currentNode.getOptionBNextId();
        } else {
            throw new BusinessException("无效的选择，请选择 A 或 B");
        }

        if (nextNodeId == null) {
            throw new BusinessException("该选项不存在或已是结局");
        }

        return getStoryNode(nextNodeId);
    }

    /**
     * 获取故事信息
     */
    public Story getStory(Long storyId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            throw new BusinessException("故事不存在");
        }
        return story;
    }
    
    /**
     * 获取用户的所有故事
     */
    public List<Story> getStoriesByAuthorId(Long authorId) {
        QueryWrapper<Story> wrapper = new QueryWrapper<>();
        wrapper.eq("author_id", authorId)
               .orderByDesc("create_time");
        return storyMapper.selectList(wrapper);
    }

    /**
     * 删除故事（需要验证权限）
     */
    public void deleteStory(Long storyId, Long userId) {
        // 先检查故事是否存在
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            throw new BusinessException("故事不存在");
        }
        // 验证是否为作者本人
        if (!story.getAuthorId().equals(userId)) {
            throw new BusinessException("无权删除此故事");
        }
        // 删除故事（由于设置了 ON DELETE CASCADE，关联的节点会自动删除）
        storyMapper.deleteById(storyId);
    }

        /**
     * 同步生成故事分支（会等待完成）
     */
    public void generateBranchesSync(Long storyId, String opening) {
        log.info("开始同步生成故事分支: storyId={}", storyId);
        try {
            // 创建根节点
            StoryNode rootNode = StoryNode.builder()
                    .storyId(storyId)
                    .parentNodeId(null)
                    .content(opening)
                    .isEnding(0)
                    .build();
            storyNodeMapper.insert(rootNode);
            
            // 递归生成分支（最大深度 6 层）
            generateBranchRecursively(storyId, rootNode.getId(), opening, 1, 6);
            
            log.info("故事分支生成完成: storyId={}", storyId);
        } catch (Exception e) {
            log.error("同步生成故事分支失败: storyId={}", storyId, e);
            throw new BusinessException("生成故事分支失败: " + e.getMessage());
        }
    }
}