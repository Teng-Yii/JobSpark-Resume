package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.application.service.AuthApplicationService;
import com.tengYii.jobspark.application.validate.AuthValidator;
import com.tengYii.jobspark.common.utils.login.JwtTokenUtil;
import com.tengYii.jobspark.common.utils.login.UserContext;
import com.tengYii.jobspark.dto.request.ForgetPasswordRequest;
import com.tengYii.jobspark.dto.request.LoginRequest;
import com.tengYii.jobspark.dto.request.RegisterRequest;
import com.tengYii.jobspark.dto.response.SecureLoginResponse;
import com.tengYii.jobspark.dto.response.UserInfoResponse;
import com.tengYii.jobspark.common.exception.ValidationException;
import com.tengYii.jobspark.model.po.UserInfoPO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

/**
 * 登录认证controller
 *
 * @author tengYii
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private AuthApplicationService authApplicationService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * JWT访问令牌过期时间（秒）
     */
    @Value("${jwt.access-token-expiration:7200}")
    private Long accessTokenExpiration;

    /**
     * Bearer Token 前缀
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 用户登录接口
     *
     * @param loginRequest 登录请求对象，包含用户名和密码
     * @return 安全的登录响应，仅包含JWT token和过期时间
     */
    @PostMapping("/login")
    public ResponseEntity<SecureLoginResponse> login(@RequestBody LoginRequest loginRequest) {

        // 校验请求参数合法性
        String validationResult = AuthValidator.validateLogin(loginRequest);
        if (StringUtils.isNotBlank(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        // 用户认证
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        Long userId = authApplicationService.authenticateUser(username, password);
        if (Objects.isNull(userId)) {
            throw new ValidationException(String.valueOf(HttpStatus.UNAUTHORIZED.value()), "用户名或密码错误");
        }

        // 生成JWT token
        String accessToken = jwtTokenUtil.generateAccessToken(userId, username);

        // 构建安全的登录响应
        // 基于安全考虑，仅返回token和过期时间，不返回敏感的用户信息
        SecureLoginResponse secureLoginResponse = new SecureLoginResponse(accessToken, accessTokenExpiration);
        return ResponseEntity.ok(secureLoginResponse);
    }

    /**
     * 用户注册接口
     *
     * @param registerRequest 注册请求对象
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest registerRequest) {

        // 校验请求参数合法性
        String validationResult = AuthValidator.validateRegister(registerRequest);
        if (StringUtils.isNotEmpty(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        // 执行注册逻辑
        authApplicationService.register(registerRequest);

        return ResponseEntity.ok().build();
    }

    /**
     * 发送忘记密码验证码接口
     *
     * @param forgetPasswordRequest 忘记密码请求对象
     * @return 处理结果
     */
    @PostMapping("/sendForgetPasswordCode")
    public ResponseEntity<Void> sendForgetPasswordCode(@RequestBody ForgetPasswordRequest forgetPasswordRequest) {

        // 校验请求参数合法性
        String errorMsg = AuthValidator.validateSendForgetPasswordCode(forgetPasswordRequest);
        if (StringUtils.isNotEmpty(errorMsg)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), errorMsg);
        }

        // 执行发送验证码逻辑
        authApplicationService.sendForgetPasswordCode(forgetPasswordRequest);

        return ResponseEntity.ok().build();
    }

    /**
     * 忘记密码接口（重置密码）
     *
     * @param forgetPasswordRequest 忘记密码请求对象
     * @return 处理结果
     */
    @PostMapping("/forgetPassword")
    public ResponseEntity<Void> forgetPassword(@RequestBody ForgetPasswordRequest forgetPasswordRequest) {

        // 校验请求参数合法性
        String validationResult = AuthValidator.validateForgetPassword(forgetPasswordRequest);
        if (StringUtils.isNotEmpty(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        // 执行忘记密码逻辑
        authApplicationService.forgetPassword(forgetPasswordRequest);
        return ResponseEntity.ok().build();
    }

    /**
     * 用户登出接口
     *
     * @param request HTTP请求对象，用于获取Authorization头中的token
     * @return 登出响应
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {

        // 获取Authorization头中的token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !StringUtils.startsWith(authHeader, BEARER_PREFIX)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "无效的token格式");
        }

        try {
            // 提取实际的token（去掉"Bearer "前缀）
            String token = StringUtils.substringAfter(authHeader, BEARER_PREFIX);

            // 判空校验（防止某些极端情况截取为空串）
            if (StringUtils.isEmpty(token)) {
                throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "无效的token格式");
            }

            // 执行登出操作
            authApplicationService.logout(token);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new ValidationException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "登出失败");
        }
    }

    /**
     * 验证token有效性接口
     * <p>
     * 仅验证token的有效性，不返回用户信息，遵循单一职责原则
     *
     * @return 验证结果，成功返回200状态码
     */
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken() {

        // 从ThreadLocal获取当前用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (Objects.isNull(currentUserId)) {
            throw new ValidationException(String.valueOf(HttpStatus.UNAUTHORIZED.value()), "用户未登录");
        }

        // token有效，返回成功状态
        return ResponseEntity.ok().build();
    }

    /**
     * 获取当前用户信息接口
     * <p>
     * 将用户信息获取与token验证分离，
     * 提供专门的接口用于获取用户详细信息
     *
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUserInfo() {

        // 从ThreadLocal获取当前用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (Objects.isNull(currentUserId)) {
            throw new ValidationException(String.valueOf(HttpStatus.UNAUTHORIZED.value()), "用户未登录");
        }

        // 获取用户信息
        UserInfoPO userInfo = authApplicationService.getUserInfo(currentUserId);
        if (Objects.isNull(userInfo)) {
            throw new ValidationException(String.valueOf(HttpStatus.NOT_FOUND.value()), "用户信息不存在");
        }

        UserInfoResponse userInfoResponse = new UserInfoResponse();
        BeanUtils.copyProperties(userInfo, userInfoResponse);
        return ResponseEntity.ok(userInfoResponse);
    }
}