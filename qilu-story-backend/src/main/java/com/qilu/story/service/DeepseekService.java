package com.qilu.story.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qilu.story.dto.DeepseekResponse;
import com.qilu.story.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * DeepSeek API Service
 */
@Slf4j
@Service
public class DeepseekService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.max-tokens}")
    private Integer maxTokens;

    @Value("${deepseek.temperature}")
    private Double temperature;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 生成故事分支（带重试）
     */
    public DeepseekResponse generateBranch(String previousContent) {
        return generateBranchWithRetry(previousContent, 3);
    }

    /**
     * 带重试的生成方法
     */
    private DeepseekResponse generateBranchWithRetry(String previousContent, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return doGenerateBranch(previousContent);
            } catch (Exception e) {
                log.warn("调用 DeepSeek 失败，第 {} 次重试: {}", attempt, e.getMessage());
                if (attempt == maxRetries) {
                    throw new BusinessException("生成故事分支失败: " + e.getMessage());
                }
                try {
                    Thread.sleep(2000); // 等待2秒后重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new BusinessException("生成故事分支失败: 重试次数已用完");
    }

    /**
     * 实际调用 API 的方法
     */
    private DeepseekResponse doGenerateBranch(String previousContent) {
        // 构建请求
        String prompt = buildPrompt(previousContent);
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Object[]{message});

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

        // 调用 API
        String response = restTemplate.postForObject(apiUrl, entity, String.class);
        log.info("DeepSeek API 响应: {}", response);

        // 解析响应
        JSONObject jsonResponse = JSON.parseObject(response);
        if (jsonResponse == null || jsonResponse.getJSONArray("choices") == null) {
            throw new BusinessException("DeepSeek API 返回格式错误");
        }

        JSONObject choice = jsonResponse.getJSONArray("choices").getJSONObject(0);
        String content = choice.getJSONObject("message").getString("content");

        // 从响应中提取 JSON
        return parseResponse(content);
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String previousContent) {
        return "你是一个专业的互动小说作家。请根据以下故事内容，生成两个后续分支选项。\n\n" +
                "当前剧情：\n" + previousContent + "\n\n" +
                "【严格要求】\n" +
                "1. 只输出纯 JSON，不要有任何其他文字、注释、解释\n" +
                "2. JSON 格式必须如下，不要修改字段名：\n" +
                "{\n" +
                "  \"optionA\": {\n" +
                "    \"label\": \"选项A文字\",\n" +
                "    \"content\": \"选项A的剧情内容，150-200字\",\n" +
                "    \"isEnding\": false\n" +
                "  },\n" +
                "  \"optionB\": {\n" +
                "    \"label\": \"选项B文字\",\n" +
                "    \"content\": \"选项B的剧情内容，150-200字\",\n" +
                "    \"isEnding\": false\n" +
                "  }\n" +
                "}\n\n" +
                "3. isEnding 可以是 true 或 false\n" +
                "4. label 2-6个字，content 150-200字\n" +
                "5. 不要输出任何其他内容，只输出上面的 JSON";
    }

    /**
     * 解析 API 响应（增强容错）
     */
    private DeepseekResponse parseResponse(String content) {
        try {
            log.info("原始响应内容: {}", content);
            
            // 方法1：直接解析
            try {
                DeepseekResponse response = JSON.parseObject(content, DeepseekResponse.class);
                if (response != null && response.getOptionA() != null && response.getOptionB() != null) {
                    return validateAndFillDefaults(response);
                }
            } catch (Exception e1) {
                log.debug("直接解析失败，尝试提取JSON: {}", e1.getMessage());
            }
            
            // 方法2：提取 JSON 部分（去掉 markdown 标记）
            String cleanContent = content;
            cleanContent = cleanContent.replaceAll("```json\\s*", "");
            cleanContent = cleanContent.replaceAll("```\\s*", "");
            
            // 找到第一个 { 和最后一个 }
            int startIndex = cleanContent.indexOf("{");
            int endIndex = cleanContent.lastIndexOf("}");
            
            if (startIndex == -1 || endIndex == -1) {
                throw new BusinessException("响应中找不到 JSON 格式数据");
            }
            
            String jsonStr = cleanContent.substring(startIndex, endIndex + 1);
            log.info("提取的 JSON: {}", jsonStr);
            
            DeepseekResponse response = JSON.parseObject(jsonStr, DeepseekResponse.class);
            return validateAndFillDefaults(response);
            
        } catch (Exception e) {
            log.error("解析 DeepSeek 响应失败: {}", content, e);
            throw new BusinessException("解析 API 响应失败: " + e.getMessage());
        }
    }

    /**
     * 验证并填充默认值
     */
    private DeepseekResponse validateAndFillDefaults(DeepseekResponse response) {
        if (response.getOptionA() == null) {
            throw new BusinessException("JSON 格式不正确，缺少 optionA");
        }
        if (response.getOptionB() == null) {
            throw new BusinessException("JSON 格式不正确，缺少 optionB");
        }
        
        // 填充默认值
        if (response.getOptionA().getLabel() == null || response.getOptionA().getLabel().trim().isEmpty()) {
            response.getOptionA().setLabel("继续前进");
        }
        if (response.getOptionB().getLabel() == null || response.getOptionB().getLabel().trim().isEmpty()) {
            response.getOptionB().setLabel("另寻他路");
        }
        if (response.getOptionA().getContent() == null || response.getOptionA().getContent().trim().isEmpty()) {
            response.getOptionA().setContent("你做出了选择，继续前行...");
        }
        if (response.getOptionB().getContent() == null || response.getOptionB().getContent().trim().isEmpty()) {
            response.getOptionB().setContent("你做出了选择，继续前行...");
        }
        if (response.getOptionA().getIsEnding() == null) {
            response.getOptionA().setIsEnding(false);
        }
        if (response.getOptionB().getIsEnding() == null) {
            response.getOptionB().setIsEnding(false);
        }
        
        return response;
    }
}