package com.paperpilot.server.controller;

import com.paperpilot.server.entity.TutorialArticleEntity;
import com.paperpilot.server.repository.TutorialArticleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tutorials")
public class TutorialController {
    private final TutorialArticleRepository repository;

    public TutorialController(TutorialArticleRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TutorialArticleEntity> activeArticles() {
        return repository.findByActiveFlagTrueOrderBySortOrderAscUpdatedAtDesc();
    }

    @GetMapping("/{id}")
    public TutorialArticleEntity article(@PathVariable("id") Long id) {
        TutorialArticleEntity article = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教程不存在"));
        if (!article.isActiveFlag()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "教程已下架");
        }
        return article;
    }
}
