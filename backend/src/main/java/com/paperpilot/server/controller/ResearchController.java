package com.paperpilot.server.controller;

import com.paperpilot.server.dto.PaperUpdateRequest;
import com.paperpilot.server.dto.SearchRequest;
import com.paperpilot.server.service.ResearchDataService;
import com.paperpilot.server.vo.DashboardSummaryVO;
import com.paperpilot.server.vo.LibraryPaperVO;
import com.paperpilot.server.vo.SearchPaperVO;
import com.paperpilot.server.vo.SearchSessionVO;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class ResearchController {

    private final ResearchDataService researchDataService;

    public ResearchController(ResearchDataService researchDataService) {
        this.researchDataService = researchDataService;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryVO dashboardSummary() {
        return researchDataService.getDashboardSummary();
    }

    @GetMapping("/library/papers")
    public List<LibraryPaperVO> libraryPapers(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "tag", required = false) String tag
    ) {
        return researchDataService.listLibraryPapers(keyword, tag);
    }

    @GetMapping("/library/papers/{workspaceId}")
    public ResponseEntity<LibraryPaperVO> libraryPaper(@PathVariable("workspaceId") String workspaceId) {
        try {
            return ResponseEntity.ok(researchDataService.getLibraryPaper(workspaceId));
        } catch (NoSuchElementException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/library/papers/{workspaceId}")
    public LibraryPaperVO updateLibraryPaper(
        @PathVariable("workspaceId") String workspaceId,
        @RequestBody PaperUpdateRequest request
    ) {
        return researchDataService.updateLibraryPaper(workspaceId, request);
    }

    @DeleteMapping("/library/papers/{workspaceId}")
    public void deleteLibraryPaper(@PathVariable("workspaceId") String workspaceId) {
        researchDataService.deleteLibraryPaper(workspaceId);
    }

    @PostMapping("/library/papers/{workspaceId}/repair")
    public LibraryPaperVO repairLibraryPaper(@PathVariable("workspaceId") String workspaceId) {
        return researchDataService.repairLibraryPaper(workspaceId);
    }

    @PostMapping("/library/papers/repair")
    public List<LibraryPaperVO> repairLibraryPapers() {
        return researchDataService.repairLibraryPapers();
    }

    @PostMapping("/search/session")
    public SearchSessionVO logSearch(@RequestBody SearchRequest request) {
        return researchDataService.logSearch(request);
    }

    @GetMapping("/search/papers")
    public List<SearchPaperVO> searchPapers(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "author", required = false) String author
    ) {
        return researchDataService.searchPapers(keyword, author);
    }
}
