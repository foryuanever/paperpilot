package com.paperpilot.server.vo;

import java.util.List;

public class DashboardSummaryVO {

    private List<DashboardStatVO> stats;
    private List<RecentPaperVO> recentPapers;
    private List<FolderSummaryVO> libraryFolders;

    public DashboardSummaryVO(
        List<DashboardStatVO> stats,
        List<RecentPaperVO> recentPapers,
        List<FolderSummaryVO> libraryFolders
    ) {
        this.stats = stats;
        this.recentPapers = recentPapers;
        this.libraryFolders = libraryFolders;
    }

    public List<DashboardStatVO> getStats() {
        return stats;
    }

    public List<RecentPaperVO> getRecentPapers() {
        return recentPapers;
    }

    public List<FolderSummaryVO> getLibraryFolders() {
        return libraryFolders;
    }
}
