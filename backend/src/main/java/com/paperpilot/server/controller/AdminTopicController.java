package com.paperpilot.server.controller;

import com.paperpilot.server.service.TopicResearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/topics")
public class AdminTopicController {
    private final TopicResearchService topicResearchService;

    public AdminTopicController(TopicResearchService topicResearchService) {
        this.topicResearchService = topicResearchService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return topicResearchService.adminList(keyword);
    }

    @PostMapping("/generate-hot")
    public List<Map<String, Object>> generateHot(@RequestBody(required = false) Map<String, Object> body) {
        return topicResearchService.generateOfficialHotTopics(body == null ? Map.of() : body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return topicResearchService.adminDelete(id);
    }
}
