package com.udesc.scriptumapp.model;

import java.util.List;

public class Document {
    public Long id;
    public String title;
    public List<String> content;
    public String status;
    public Double reliability;

    public Document(String title, List<String> content, String status, Double reliability) {
        this.title = title;
        this.content = content;
        this.status = status;
        this.reliability = reliability;
    }


    public String getSummary() {
        if (content != null && !content.isEmpty()) {
            return content.get(0);
        }
        return "Sem conteúdo";
    }

    public String getStatusDetail() {
        if (reliability != null) {
            return "Confiabilidade: " + reliability + "%";
        }
        return "";
    }

    public String getJoinedContent() {
        if (content == null) return "";
        return String.join("\n\n", content);
    }
}