package com.tengYii.jobspark.common.utils.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 邮件发送辅助类
 *
 * @author tengYii
 * @since 1.0.0
 */
@Slf4j
@Component
public class EmailHelper {

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送简单HTML邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容（支持HTML）
     * @return 是否发送成功
     */
    public boolean sendHtmlMail(String to, String subject, String content) {
        if (Objects.isNull(javaMailSender)) {
            log.error("邮件发送失败：JavaMailSender未注入，请检查配置");
            return false;
        }

        if (StringUtils.isEmpty(to) || StringUtils.isEmpty(subject) || StringUtils.isEmpty(content)) {
            log.error("邮件发送失败：参数不完整，to={}, subject={}", to, subject);
            return false;
        }

        if (StringUtils.isEmpty(from)) {
            log.error("邮件发送失败：发件人未配置");
            return false;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            javaMailSender.send(message);
            log.info("邮件发送成功：to={}, subject={}", to, subject);
            return true;
        } catch (MessagingException e) {
            log.error("邮件发送异常：to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return false;
        }
    }
}