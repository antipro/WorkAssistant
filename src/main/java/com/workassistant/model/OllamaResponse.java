package com.workassistant.model;

/**
 * Response model for Ollama API
 */
public class OllamaResponse {
    private String model;
    private String response;
    private boolean done;
    private String error;

    public OllamaResponse() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
