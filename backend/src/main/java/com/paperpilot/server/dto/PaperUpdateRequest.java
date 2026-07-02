package com.paperpilot.server.dto;

public class PaperUpdateRequest {

 private String progress;
 private String note;
 private String readAt;
 private String paperUrl;
 private String authors;
 private String publishYear;
  private java.util.List<String> journalTags;

 public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReadAt() {
        return readAt;
    }

    public void setReadAt(String readAt) {
        this.readAt = readAt;
    }

    public String getPaperUrl() {
        return paperUrl;
    }

    public void setPaperUrl(String paperUrl) {
        this.paperUrl = paperUrl;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getPublishYear() {
        return publishYear;
    }

 public void setPublishYear(String publishYear) {
   this.publishYear = publishYear;
 }

  public java.util.List<String> getJournalTags() {
    return journalTags;
  }

  public void setJournalTags(java.util.List<String> journalTags) {
    this.journalTags = journalTags;
  }
}
