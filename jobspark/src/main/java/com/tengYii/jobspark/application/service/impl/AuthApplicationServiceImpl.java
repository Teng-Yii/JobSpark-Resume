package com.tengYii.jobspark.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tengYii.jobspark.application.service.AuthApplicationService;
import com.tengYii.jobspark.common.exception.BusinessException;
import com.tengYii.jobspark.common.utils.login.JwtTokenUtil;
import com.tengYii.jobspark.dto.request.ForgetPasswordRequest;
import com.tengYii.jobspark.dto.request.RegisterRequest;
import com.tengYii.jobspark.dto.response.LoginResponse;
import com.tengYii.jobspark.infrastructure.repo.UserInfoRepository;
import com.tengYii.jobspark.model.po.UserInfoPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 认证应用服务实现类
 *
 * @author tengYii
 * @since 1.0.0
 */
@Slf4j
@Service
public class AuthApplicationServiceImpl implements AuthApplicationService {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserInfoRepository userInfoRepository;

    /**
     * 密码加密器
     */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * JWT访问令牌过期时间（秒）
     */
    @Value("${jwt.access-token-expiration:7200}")
    private Long accessTokenExpiration;

    /**
     * 验证用户身份，返回用户ID
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户ID，如果验证失败则返回null
     */
    @Override
    public Long authenticateUser(String username, String password) {
        // 参数校验
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return null;
        }

        // 根据用户名查询用户
        LambdaQueryWrapper<UserInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfoPO::getUsername, username);
        // 确保查询未删除的用户
        queryWrapper.eq(UserInfoPO::getDeleteFlag, false);
        UserInfoPO userInfoPO = userInfoRepository.getOne(queryWrapper);

        // 用户不存在
        if (Objects.isNull(userInfoPO)) {
            log.warn("用户登录失败：用户不存在，username={}", username);
            return null;
        }

        // 验证密码
        if (!passwordEncoder.matches(password, userInfoPO.getPassword())) {
            log.warn("用户登录失败：密码错误，username={}", username);
            return null;
        }

        // 检查用户状态
        if (Objects.nonNull(userInfoPO.getStatus()) && !userInfoPO.getStatus()) {
            log.warn("用户登录失败：用户已被禁用，username={}", username);
            // 这里为了接口定义（返回Long），只能返回null，或者抛出异常。
            // 鉴于接口签名是返回 Long，保持返回 null，由上层处理（或者上层捕获不到异常会认为是认证失败）
            return null;
        }

        // 登录成功，更新最近登录时间
        userInfoPO.setLastLoginTime(LocalDateTime.now());
        userInfoRepository.updateById(userInfoPO);

        return userInfoPO.getId();
    }

    /**
     * 用户登出
     * 将token加入黑名单，使其失效
     *
     * @param token JWT token
     */
    @Override
    public void logout(String token) {
        // 参数校验
        if (StringUtils.isEmpty(token)) {
            return;
        }

        try {
            // 将token加入黑名单
            // 设置黑名单保留时间为token的剩余有效期
            jwtTokenUtil.addTokenToBlacklist(token, accessTokenExpiration);
            log.info("用户登出成功，token已加入黑名单");
        } catch (Exception e) {
            // 如果token无效，忽略异常，登出操作应该是幂等的
            log.warn("用户登出异常：{}", e.getMessage());
        }
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息PO对象
     */
    @Override
    public UserInfoPO getUserInfo(Long userId) {
        // 参数校验
        if (Objects.isNull(userId)) {
            return null;
        }

        // 根据用户ID获取用户信息
        return userInfoRepository.getById(userId);
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求参数
     */
    @Override
    public void register(RegisterRequest registerRequest) {
        // 1. 基础参数校验
        if (Objects.isNull(registerRequest)) {
            throw BusinessException.paramError("注册请求参数不能为空");
        }
        if (StringUtils.isEmpty(registerRequest.getUsername())) {
            throw BusinessException.paramError("用户名不能为空");
        }
        if (StringUtils.isEmpty(registerRequest.getPassword())) {
            throw BusinessException.paramError("密码不能为空");
        }
        // 校验确认密码
        if (!StringUtils.equals(registerRequest.getPassword(), registerRequest.getConfirmPassword())) {
            throw BusinessException.paramError("两次输入的密码不一致");
        }

        // 2. 检查用户名是否已存在
        LambdaQueryWrapper<UserInfoPO> usernameQuery = new LambdaQueryWrapper<>();
        usernameQuery.eq(UserInfoPO::getUsername, registerRequest.getUsername());
        if (userInfoRepository.count(usernameQuery) > 0) {
            throw BusinessException.paramError("用户名已存在");
        }

        // 3. 检查邮箱是否已存在（如果邮箱不为空）
        if (StringUtils.isNotEmpty(registerRequest.getEmail())) {
            LambdaQueryWrapper<UserInfoPO> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(UserInfoPO::getEmail, registerRequest.getEmail());
            if (userInfoRepository.count(emailQuery) > 0) {
                throw BusinessException.paramError("邮箱已被注册");
            }
        }

        // 4. 构建用户信息对象
        UserInfoPO newUser = new UserInfoPO();
        newUser.setUsername(registerRequest.getUsername());
        // 密码加密存储
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setEmail(registerRequest.getEmail());
        // 设置默认状态
        newUser.setStatus(true);
        newUser.setDeleteFlag(false);
        newUser.setCreatedTime(LocalDateTime.now());
        newUser.setUpdatedTime(LocalDateTime.now());

        // 5. 保存到数据库
        boolean saved = userInfoRepository.save(newUser);
        if (!saved) {
            throw BusinessException.systemError("注册失败，数据库保存异常");
        }

        log.info("用户注册成功：username={}, id={}", newUser.getUsername(), newUser.getId());
    }

    /**
     * 忘记密码
     *
     * @param forgetPasswordRequest 忘记密码请求参数
     */
    @Override
    public void forgetPassword(ForgetPasswordRequest forgetPasswordRequest) {
        // 1. 参数校验
        if (Objects.isNull(forgetPasswordRequest)) {
            throw BusinessException.paramError("请求参数不能为空");
        }
        String username = forgetPasswordRequest.getUsername();
        String email = forgetPasswordRequest.getEmail();

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(email)) {
            throw BusinessException.paramError("用户名和邮箱不能为空");
        }

        // 2. 验证用户是否存在且邮箱匹配
        LambdaQueryWrapper<UserInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfoPO::getUsername, username);
        queryWrapper.eq(UserInfoPO::getEmail, email);
        queryWrapper.eq(UserInfoPO::getDeleteFlag, false);

        UserInfoPO userInfoPO = userInfoRepository.getOne(queryWrapper);

        if (Objects.isNull(userInfoPO)) {
            // 为了安全，通常不提示具体是用户名错还是邮箱错，或者直接提示"用户信息不匹配"
            log.error("忘记密码验证失败：用户名与邮箱不匹配，username={}, email={}", username, email);
            throw BusinessException.paramError("用户名或注册邮箱错误");
        }

        // 3. 模拟发送重置密码邮件逻辑
        // 实际场景：生成随机Token -> 存入Redis(key=reset_pwd_token:userId) -> 发送邮件包含链接
        // 这里仅模拟日志记录
        String resetToken = java.util.UUID.randomUUID().toString();
        log.info("【模拟邮件发送】用户 {} 申请重置密码，重置链接：https://jobspark.com/reset-password?token={}", username, resetToken);

        // 如果需要更新数据库状态（例如记录最后一次申请重置时间），可以在这里操作
        // 目前 UserInfoPO 没有相关字段，故略过
    }

    /**
     * 刷新token
     *
     * @param token 当前的JWT token
     * @return 新的token信息
     */
    @Override
    public LoginResponse refreshToken(String token) {
        // 参数校验
        if (StringUtils.isEmpty(token)) {
            return null;
        }

        Long userId = null;
        try {
            // 验证当前token是否有效
            Boolean isValid = jwtTokenUtil.validateToken(token);
            if (Objects.isNull(isValid) || !isValid) {
                return null;
            }

            // 从token中获取用户ID
            userId = jwtTokenUtil.getUserIdFromToken(token);
            if (Objects.isNull(userId)) {
                return null;
            }

            // 查询用户信息
            UserInfoPO userInfoPO = userInfoRepository.getById(userId);
            if (Objects.isNull(userInfoPO)) {
                return null;
            }

            // 将旧token加入黑名单
            jwtTokenUtil.addTokenToBlacklist(token, accessTokenExpiration);

            // 生成新token
            String username = userInfoPO.getUsername();
            String newToken = jwtTokenUtil.generateAccessToken(userId, username);

            // 构造并返回新的登录响应
            return new LoginResponse(newToken, accessTokenExpiration, userId, username);

        } catch (Exception e) {
            // 异常日志输出，包含userId和token
            log.error("刷新token失败，userId: {}, token: {}, 异常信息: {}", userId, token, e.getMessage());
            return null;
        }
    }
}