package com.jrasp.api.authentication;

public class PayloadDto {

    private String username;
    private Long start;
    private Long end;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public PayloadDto(String username, Long start, Long end) {
        this.username = username;
        this.start = start;
        this.end = end;
    }
}
