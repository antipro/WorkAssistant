package com.workassistant.model;

/**
 * Request model for Ollama chat API
 */
public class OllamaRequest {
    private String model;
    private String prompt;
    private boolean stream;
    private boolean thinking;

    public OllamaRequest() {
    }

    public OllamaRequest(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
        this.stream = false;
        this.thinking = false;
    }

    public OllamaRequest(String model, String prompt, boolean stream) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
        this.thinking = false;
    }

    public OllamaRequest(String model, String prompt, boolean stream, boolean thinking) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
        this.thinking = thinking;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}
