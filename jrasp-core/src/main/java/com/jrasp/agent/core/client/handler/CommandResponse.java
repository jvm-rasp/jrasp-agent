package com.jrasp.agent.core.client.handler;

import com.jrasp.agent.core.client.packet.PacketType;

public class CommandResponse {

    private static final String FOTMAT = "code=%s;message=%s;type=%s";

    private int code; // 200 success、400 client error、500 server error

    private String message;

    private PacketType type;

    public static CommandResponse ok(String msg, PacketType type) {
        return new CommandResponse(200, msg, type);
    }

    public static CommandResponse clientError(String msg, PacketType type) {
        return new CommandResponse(400, msg, type);
    }

    public static CommandResponse serverError(String msg, PacketType type) {
        return new CommandResponse(500, msg, type);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public CommandResponse() {
    }

    public CommandResponse(int code, String message, PacketType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format(FOTMAT, code, message, type);
    }
}