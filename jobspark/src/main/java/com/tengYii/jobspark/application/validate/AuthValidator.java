package com.tengYii.jobspark.application.validate;

import com.google.common.base.Joiner;
import com.tengYii.jobspark.common.constants.ParseConstant;
import com.tengYii.jobspark.dto.request.ForgetPasswordRequest;
import com.tengYii.jobspark.dto.request.LoginRequest;
import com.tengYii.jobspark.dto.request.RegisterRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 登录权限校验器
 */
public class AuthValidator {

    /**
     * 校验登录请求
     *
     * @param loginRequest 登录请求参数
     * @return 错误信息字符串，无错误返回空
     */
    public static String validateLogin(LoginRequest loginRequest) {
        List<String> errorMessages = new ArrayList<>();

        // 参数验证
        if (Objects.isNull(loginRequest)) {
            errorMessages.add("登录请求不能为空");
            // 快速失败，后续无需校验
            return Joiner.on(ParseConstant.COMMA).join(errorMessages);
        }

        if (StringUtils.isEmpty(loginRequest.getUsername())) {
            errorMessages.add("用户名不能为空");
        } else if (loginRequest.getUsername().length() < 3 || loginRequest.getUsername().length() > 20) {
            errorMessages.add("用户名长度必须在3-20个字符之间");
        }

        if (StringUtils.isEmpty(loginRequest.getPassword())) {
            errorMessages.add("密码不能为空");
        } else if (loginRequest.getPassword().length() < 6) {
            errorMessages.add("密码长度不能小于6位");
        }

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }

    /**
     * 校验注册请求
     *
     * @param registerRequest 注册请求参数
     * @return 错误信息字符串，无错误返回空
     */
    public static String validateRegister(RegisterRequest registerRequest) {
        List<String> errorMessages = new ArrayList<>();

        // 参数验证
        if (Objects.isNull(registerRequest)) {
            errorMessages.add("注册请求不能为空");
            return Joiner.on(ParseConstant.COMMA).join(errorMessages);
        }

        if (StringUtils.isEmpty(registerRequest.getUsername())) {
            errorMessages.add("用户名不能为空");
        } else if (registerRequest.getUsername().length() < 3 || registerRequest.getUsername().length() > 20) {
            errorMessages.add("用户名长度必须在3-20个字符之间");
        }

        if (StringUtils.isEmpty(registerRequest.getPassword())) {
            errorMessages.add("密码不能为空");
        } else if (registerRequest.getPassword().length() < 6) {
            errorMessages.add("密码长度不能小于6位");
        }

        if (StringUtils.isNotEmpty(registerRequest.getPassword())
                && StringUtils.isNotEmpty(registerRequest.getConfirmPassword())
                && !StringUtils.equals(registerRequest.getPassword(), registerRequest.getConfirmPassword())) {
            errorMessages.add("两次输入的密码不一致");
        }

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }

    /**
     * 校验忘记密码请求
     *
     * @param forgetPasswordRequest 忘记密码请求参数
     * @return 错误信息字符串，无错误返回空
     */
    public static String validateForgetPassword(ForgetPasswordRequest forgetPasswordRequest) {
        List<String> errorMessages = new ArrayList<>();

        // 参数验证
        if (Objects.isNull(forgetPasswordRequest)) {
            errorMessages.add("忘记密码请求不能为空");
            return Joiner.on(ParseConstant.COMMA).join(errorMessages);
        }

        if (StringUtils.isEmpty(forgetPasswordRequest.getUsername()) && StringUtils.isEmpty(forgetPasswordRequest.getEmail())) {
            errorMessages.add("用户名或邮箱不能为空");
        }

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }
}