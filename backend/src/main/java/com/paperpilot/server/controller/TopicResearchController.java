package com.paperpilot.server.controller;

import com.paperpilot.server.service.TopicResearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicResearchController {
    private final TopicResearchService topicResearchService;

    public TopicResearchController(TopicResearchService topicResearchService) {
        this.topicResearchService = topicResearchService;
    }

    @GetMapping
    public List<Map<String, Object>> list(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "discipline", required = false) String discipline,
        @RequestParam(value = "stage", required = false) String stage,
        @RequestParam(value = "goal", required = false) String goal,
        @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
        @RequestParam(value = "savedOnly", required = false, defaultValue = "false") boolean savedOnly
    ) {
        return topicResearchService.list(keyword, discipline, stage, goal, sort, savedOnly);
    }

    @PostMapping("/generate")
    public List<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        return topicResearchService.generate(body);
    }

    @PostMapping("/{id}/save")
    public Map<String, Object> save(@PathVariable String id) {
        return topicResearchService.toggleSave(id);
    }

    @PostMapping("/{id}/interested")
    public Map<String, Object> interested(@PathVariable String id) {
        return topicResearchService.markInterested(id);
    }

    @PostMapping("/{id}/import-library")
    public Map<String, Object> importLibrary(@PathVariable String id) {
        return topicResearchService.importToLibrary(id);
    }

    @PostMapping("/{id}/papers/import")
    public Map<String, Object> importPaper(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return topicResearchService.importPaperToLibrary(id, body);
    }

    @PostMapping("/{id}/outline")
    public Map<String, Object> outline(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        String target = body == null ? "review" : String.valueOf(body.getOrDefault("target", "review"));
        return topicResearchService.exportOutline(id, target);
    }
}
