package com.qilu.story.controller;

import com.qilu.story.dto.CreateStoryRequest;
import com.qilu.story.entity.Story;
import com.qilu.story.entity.StoryNode;
import com.qilu.story.service.StoryService;
import com.qilu.story.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * 故事 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/stories")
public class StoryController {

    @Autowired
    private StoryService storyService;

    /**
     * 创建故事（异步生成分支，立即返回ID）
     */
    @PostMapping
public ApiResponse<Long> createStory(@RequestBody CreateStoryRequest request, HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    log.info("创建故事: title={}, authorId={}", request.getTitle(), userId);
    
    // 同步创建空白故事
    Long storyId = storyService.createStorySync(request.getTitle(), request.getOpening(), userId);
    
    // 同步生成分支（会等待完成）
    storyService.generateBranchesSync(storyId, request.getOpening());
    
    return ApiResponse.success("故事创建成功", storyId);
}

    /**
     * 获取故事根节点
     */
    @GetMapping("/{storyId}/start")
    public ApiResponse<StoryNode> getStoryStart(@PathVariable Long storyId) {
        log.info("获取故事根节点: storyId={}", storyId);
        StoryNode rootNode = storyService.getStoryRoot(storyId);
        return ApiResponse.success("获取成功", rootNode);
    }

    /**
     * 获取下一个节点
     */
    @GetMapping("/{storyId}/nodes/{nodeId}/next")
    public ApiResponse<StoryNode> getNextNode(@PathVariable Long storyId, 
                                              @PathVariable Long nodeId,
                                              @RequestParam String choice) {
        log.info("获取下一个节点: storyId={}, nodeId={}, choice={}", storyId, nodeId, choice);
        StoryNode nextNode = storyService.getNextNode(nodeId, choice);
        return ApiResponse.success("获取成功", nextNode);
    }

    /**
     * 获取指定节点
     */
    @GetMapping("/nodes/{nodeId}")
    public ApiResponse<StoryNode> getNode(@PathVariable Long nodeId) {
        log.info("获取故事节点: nodeId={}", nodeId);
        StoryNode node = storyService.getStoryNode(nodeId);
        return ApiResponse.success("获取成功", node);
    }

    /**
     * 获取当前用户的所有故事
     */
    @GetMapping("/my-stories")
    public ApiResponse<List<Story>> getMyStories(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<Story> stories = storyService.getStoriesByAuthorId(userId);
        return ApiResponse.success("获取成功", stories);
    }

    /**
     * 删除故事
     */
    @DeleteMapping("/{storyId}")
    public ApiResponse<Void> deleteStory(@PathVariable Long storyId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        storyService.deleteStory(storyId, userId);
        return ApiResponse.success("删除成功", null);
    }

        /**
     * 检查故事是否已生成完成
     */
    @GetMapping("/{storyId}/ready")
    public ApiResponse<Boolean> isStoryReady(@PathVariable Long storyId) {
        log.info("检查故事生成状态: storyId={}", storyId);
        boolean ready = storyService.isStoryReady(storyId);
        return ApiResponse.success("获取成功", ready);
    }
}