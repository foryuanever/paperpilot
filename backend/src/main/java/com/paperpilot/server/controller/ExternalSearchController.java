package com.paperpilot.server.controller;

import com.paperpilot.server.dto.SearchRequest;
import com.paperpilot.server.service.ExternalSearchService;
import com.paperpilot.server.service.SearchFormulaService;
import com.paperpilot.server.vo.SearchPaperVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/external")
public class ExternalSearchController {

    private final ExternalSearchService externalSearchService;
    private final SearchFormulaService searchFormulaService;

    public ExternalSearchController(ExternalSearchService externalSearchService, SearchFormulaService searchFormulaService) {
        this.externalSearchService = externalSearchService;
        this.searchFormulaService = searchFormulaService;
    }

    /**
     * Search external academic sources via Crossref (and optionally Unpaywall for PDF URLs).
     *
     * @param query search keyword (title, DOI, author, etc.)
     * @return list of matching papers with basic metadata
     */
    @GetMapping("/search")
    public com.paperpilot.server.vo.SearchResultVO searchExternal(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "source", defaultValue = "crossref") String source,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return externalSearchService.searchByQuery(query, source, page, pageSize);
    }

    @PostMapping("/search-formula")
    public Map<String, Object> generateSearchFormula(@RequestBody Map<String, Object> body) {
        return searchFormulaService.generate(String.valueOf(body.getOrDefault("idea", "")));
    }
}
