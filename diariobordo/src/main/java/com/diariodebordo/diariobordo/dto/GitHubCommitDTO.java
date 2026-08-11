package com.diariodebordo.diariobordo.dto;

import java.time.LocalDateTime;

public class GitHubCommitDTO {
    private String sha;
    private String message;
    private LocalDateTime date;
    private String author;
    private String repoName;
    private String url;

    public GitHubCommitDTO() {}

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}