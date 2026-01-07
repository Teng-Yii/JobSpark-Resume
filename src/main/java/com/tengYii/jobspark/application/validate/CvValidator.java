package com.tengYii.jobspark.application.validate;

import com.tengYii.jobspark.common.exception.ValidationException;
import com.tengYii.jobspark.model.bo.ContactBO;
import com.tengYii.jobspark.model.bo.CvBO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CvBO 领域校验器：
 * - 必填：姓名
 * - 联系方式：phone/email/wechat 至少一种
 * - 教育或经历：至少一项
 * - 列表字段为空时允许运行，但会给出告警日志
 * <p>
 * 注意：模板字段映射一致性由 TemplateFieldMapper 负责，这里不做模板键校验。
 */
public class CvValidator {

    private static final Logger log = LoggerFactory.getLogger(CvValidator.class);

    public void validateOrThrow(CvBO cv) {
        if (cv == null) {
            throw ValidationException.illegal("CvBO 为空");
        }
        // 姓名
        if (StringUtils.isBlank(cv.getName())) {
            throw ValidationException.missing("name");
        }

        // 联系方式至少一种
        ContactBO c = cv.getContact();
        boolean hasContact = c != null && (notBlank(c.getPhone()) || notBlank(c.getEmail()) || notBlank(c.getWechat()));
        if (!hasContact) {
            throw ValidationException.missing("contact.phone/email/wechat 之一");
        }

        // 教育/经历至少一项
        boolean hasEdu = cv.getEducations() != null && !cv.getEducations().isEmpty();
        if (!hasEdu) {
            throw ValidationException.missing("教育经历至少一项");
        }

        // 软校验/提示
        if (cv.getSkills() == null || cv.getSkills().isEmpty()) {
            log.warn("校验提示：skills 为空，将继续渲染");
        }
        if (cv.getSummary() == null || StringUtils.isBlank(cv.getSummary())) {
            log.warn("校验提示：summary 为空，将继续渲染");
        }
        if (cv.getProjects() == null || cv.getProjects().isEmpty()) {
            log.warn("校验提示：projects 为空，将继续渲染");
        }

        // 元数据存在但字段为空的提示
        if (cv.getMeta() != null && cv.getMeta().getLocaleConfig() != null) {
            if (StringUtils.isBlank((cv.getMeta().getLocaleConfig().getLocale()))) {
                log.warn("校验提示：meta.localeConfig.locale 为空");
            }
        }

        log.info("CvBO 校验通过：name={}, contact={}, edu={}, exp={}",
                cv.getName(),
                maskContact(c),
                sizeOf(cv.getEducations()),
                sizeOf(cv.getExperiences()));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static int sizeOf(java.util.Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    private static String maskContact(ContactBO c) {
        if (c == null) return "N/A";
        String phone = c.getPhone();
        if (notBlank(phone) && phone.length() >= 7) {
            // 简单脱敏：保留前3后4
            phone = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        }
        String email = c.getEmail();
        if (notBlank(email)) {
            int at = email.indexOf('@');
            if (at > 1) {
                email = email.charAt(0) + "****" + email.substring(at);
            } else {
                email = "****";
            }
        }
        return "{phone=" + (phone == null ? "" : phone) + ", email=" + (email == null ? "" : email) + "}";
    }
}