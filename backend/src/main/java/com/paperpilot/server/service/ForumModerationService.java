package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.ModelConfigEntity;
import com.paperpilot.server.repository.ModelConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ForumModerationService {

    private final AiChatService aiChatService;
    private final ModelConfigRepository modelConfigRepository;
    private final ObjectMapper objectMapper;

    public ForumModerationService(
        AiChatService aiChatService,
        ModelConfigRepository modelConfigRepository,
        ObjectMapper objectMapper
    ) {
        this.aiChatService = aiChatService;
        this.modelConfigRepository = modelConfigRepository;
        this.objectMapper = objectMapper;
    }

    public ModerationResult review(Map<String, Object> body) {
        String title = text(body.get("title"));
        String content = text(body.get("content"));
        if (!StringUtils.hasText(title)) {
            return ModerationResult.reject("标题不能为空。", "basic-validation");
        }
        if (!StringUtils.hasText(content) || content.length() <= 5) {
            return ModerationResult.reject("内容不能为空，且需要大于 5 个字。", "basic-validation");
        }

        if (!hasConfiguredModel()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "管理员尚未启用可用的全站 AI 路由，请联系管理员后再发布帖子。");
        }

        try {
            AiChatService.ChatResult response = aiChatService.chatJson(
                "你是科研社区内容审核员。只输出JSON，字段为 approved(boolean)、reason(string)、riskLevel(low/medium/high)。审核标准：内容必须与科研学习相关；不得伪造学术事实、暴力色情、违法、攻击他人、代写交易、付款引流；正常的数据集求助、论文期刊推荐、科研优惠、比赛组队和方法讨论应通过。",
                objectMapper.writeValueAsString(Map.of(
                    "postType", text(body.get("postType")),
                    "direction", text(body.get("direction")),
                    "title", title,
                    "content", content,
                    "tags", body.getOrDefault("tags", List.of())
                ))
            );
            JsonNode result = objectMapper.readTree(response.content());
            boolean approved = result.path("approved").asBoolean(false);
            String reason = result.path("reason").asText(approved ? "AI 审核通过" : "内容未通过 AI 审核");
            return new ModerationResult(approved, reason, response.modelName());
        } catch (Exception exception) {
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI 审核调用失败，请联系管理员检查全站模型路由或稍后重试。");
        }
    }

    private boolean hasConfiguredModel() {
        ModelConfigEntity config = modelConfigRepository
            .findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(ModelConfigService.SCENE_GENERAL)
            .orElse(null);
        if (config == null || !StringUtils.hasText(config.getBaseUrl()) || !StringUtils.hasText(config.getModelName())) {
            return false;
        }
        return StringUtils.hasText(config.getApiKey())
            || config.getBaseUrl().contains("127.0.0.1")
            || config.getBaseUrl().contains("localhost");
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public record ModerationResult(boolean approved, String reason, String reviewer) {
        static ModerationResult approve(String reason, String reviewer) {
            return new ModerationResult(true, reason, reviewer);
        }

        static ModerationResult reject(String reason, String reviewer) {
            return new ModerationResult(false, reason, reviewer);
        }
    }
}
