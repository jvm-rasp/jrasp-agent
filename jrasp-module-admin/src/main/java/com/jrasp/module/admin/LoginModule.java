package com.jrasp.module.admin;

import com.jrasp.api.ConfigInfo;
import com.jrasp.api.Information;
import com.jrasp.api.Module;
import com.jrasp.api.Resource;
import com.jrasp.api.annotation.Command;
import com.jrasp.api.authentication.JwtTokenService;
import com.jrasp.api.authentication.PayloadDto;
import com.jrasp.api.json.JSONObject;
import com.jrasp.api.log.Log;
import com.jrasp.api.model.RestResult;
import com.jrasp.api.model.RestResultUtils;
import com.jrasp.api.model.ResultCodeEnum;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import java.io.PrintWriter;
import java.util.Map;

@MetaInfServices(Module.class)
@Information(id = "user", version = "0.0.1", author = "jrasp")
// 不要修改这里的id，因为core中已经把/user/login、update 加入了鉴权白名单了
public class LoginModule implements Module {

    @Resource
    private Log logger;

    @Resource
    private JSONObject jsonObject;

    @Resource
    private ConfigInfo configInfo;

    @Resource
    private JwtTokenService jwtTokenService;

    // 登陆获取授权token
    // 不要修改这里的名称，因为core中已经把/user/login加入了鉴权白名单了
    @Command(value = "login", method = Command.Method.POST)
    public void login(Map<String, String> parameterMap, final PrintWriter writer) {
        String username = parameterMap.get("username");
        String password = parameterMap.get("password");
        RestResult<String> restResult = null;
        if (StringUtils.equals(username, configInfo.getUsername()) &&
                StringUtils.equals(password, configInfo.getPassword())) {
            PayloadDto payloadDto = jwtTokenService.getDefaultPayloadDto(username);
            String token = "";
            try {
                token = jwtTokenService.generateToken(jsonObject.toJSONString(payloadDto));
            } catch (Exception e) {
                // todo null
                logger.error("generateToken error", e);
            }
            restResult = RestResultUtils.success("login success", token);
        } else {
            restResult = RestResultUtils.failed(ResultCodeEnum.CLIENT_ERROR, "用户名或者密码错误");
        }
        writer.println(jsonObject.toFormatJSONString(restResult));
        writer.flush();
    }

    // 密码修改
    @Command(value = "update", method = Command.Method.POST)
    public void update(Map<String, String> parameterMap, final PrintWriter writer) {
        String username = parameterMap.get("username");
        String password = parameterMap.get("password");
        String newPassword = parameterMap.get("newPassword");
        RestResult<String> restResult = null;
        if (StringUtils.equals(username, configInfo.getUsername()) && StringUtils.equals(password, configInfo.getPassword())) {
            configInfo.setPassword(newPassword);
            restResult = RestResultUtils.success("update password success");
        } else {
            restResult = RestResultUtils.failed(ResultCodeEnum.CLIENT_ERROR, "用户名或者密码错误");
        }
        writer.println(jsonObject.toFormatJSONString(restResult));
        writer.flush();
    }
}
