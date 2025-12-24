package com.tengYii.jobspark.application.validate;

import com.google.common.base.Joiner;
import com.tengYii.jobspark.common.constants.ParseConstant;
import com.tengYii.jobspark.dto.request.LoginRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 登录权限校验器
 */
public class AuthValidator {


    public static String validateLogin(LoginRequest loginRequest) {
        List<String> errorMessages = new ArrayList<>();

        // 参数验证
        if (Objects.isNull(loginRequest)) {
            errorMessages.add("登录请求不能为空");
        }

        if (StringUtils.isEmpty(loginRequest.getUsername())) {
            errorMessages.add("用户名不能为空");
        }

        if (StringUtils.isEmpty(loginRequest.getPassword())) {
            errorMessages.add("密码不能为空");
        }

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }
}