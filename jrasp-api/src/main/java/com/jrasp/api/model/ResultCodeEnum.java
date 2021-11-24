package com.jrasp.api.model;

public enum ResultCodeEnum {
    
    /**
     * 正常
     **/
    SUCCESS(200, "处理成功"),

    /**
     * 客户端错误
     */
    CLIENT_ERROR(400, "请求参数异常"),


    /**
     * 鉴权失败
     */
    AUTH_ERROR(401, "鉴权失败"),

    /**
     * 资源未找到
     */
    NOT_FOUND(404,"未找到"),

    /**
     * 服务端错误
     */
    SERVER_ERROR(500, "服务器内部错误");


    private int code;
    
    private String msg;
    
    ResultCodeEnum(int code, String codeMsg) {
        this.code = code;
        this.msg = codeMsg;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getCodeMsg() {
        return msg;
    }
}
