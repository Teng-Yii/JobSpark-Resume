package com.tengYii.jobspark.domain.cv.render.markdown;


import com.tengYii.jobspark.common.exception.ValidationException;
import com.tengYii.jobspark.model.bo.*;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 将领域模型 CvBO 映射为 FreeMarker 模板期望的 Map。
 * - 支持别名表（当 .ftl 字段命名与模型不一致时）
 * - 缺失字段报告（抛出 ValidationException）
 *
 * 设计约定：
 * - 模板主文件：templates/cv.md.ftl
 * - 模板字段（默认）：name, age, title, avatarUrl, summary, contact, socialLinks,
 *   educations, experiences, projects, skills, certificates, meta
 * - 所有列表字段为空时映射为空列表，避免 null 判断。
 */
public class TemplateFieldMapper {

    private final Map<String, String> aliases;

    @Builder
    public TemplateFieldMapper(Map<String, String> aliases) {
        this.aliases = Optional.ofNullable(aliases).orElseGet(HashMap::new);
    }

    /**
     * 将 CvBO 映射为模板数据 Map。
     * 若必填字段缺失，抛出 ValidationException。
     */
    public Map<String, Object> toTemplateData(CvBO cv) {
        if (cv == null) {
            throw ValidationException.illegal("CvBO 为空");
        }
        // 必填校验：姓名、联系方式至少一种、教育或经历至少一项
        validateRequired(cv);

        Map<String, Object> data = new LinkedHashMap<>();
        // 基本
        put(data, "name", cv.getName());
        put(data, "birthDate", cv.getBirthDate());
        put(data, "title", cv.getTitle());
        put(data, "avatarUrl", cv.getAvatarUrl());

        // 富文本摘要
        put(data, "summary", Optional.ofNullable(cv.getSummary()).orElse(""));

        // 联系方式
        ContactBO c = cv.getContact();
        Map<String, Object> contactMap = new LinkedHashMap<>();
        if (c != null) {
            contactMap.put("phone", nullToEmpty(c.getPhone()));
            contactMap.put("email", nullToEmpty(c.getEmail()));
            contactMap.put("wechat", nullToEmpty(c.getWechat()));
            contactMap.put("location", nullToEmpty(c.getLocation()));
        }
        put(data, "contact", contactMap);

        // 社交链接
        List<Map<String, Object>> social = new ArrayList<>();
        if (cv.getSocialLinks() != null) {
            for (LinkBO link : cv.getSocialLinks()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("label", nullToEmpty(link.getLabel()));
                m.put("url", nullToEmpty(link.getUrl()));
                social.add(m);
            }
        }
        put(data, "socialLinks", social);

        // 教育
        List<Map<String, Object>> edus = new ArrayList<>();
        if (cv.getEducations() != null) {
            for (EducationBO e : cv.getEducations()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("school", nullToEmpty(e.getSchool()));
                m.put("major", nullToEmpty(e.getMajor()));
                m.put("startDate", e.getStartDate());
                m.put("endDate", e.getEndDate());
                m.put("description", Optional.ofNullable(e.getDescription()).orElse(""));
                edus.add(m);
            }
        }
        put(data, "educations", edus);

        // 经历
        List<Map<String, Object>> exps = new ArrayList<>();
        if (cv.getExperiences() != null) {
            for (ExperienceBO e : cv.getExperiences()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("company", nullToEmpty(e.getCompany()));
                m.put("role", nullToEmpty(e.getRole()));
                m.put("startDate", e.getStartDate());
                m.put("endDate", e.getEndDate());
                m.put("highlights", e.getHighlights());
                exps.add(m);
            }
        }
        put(data, "experiences", exps);

        // 项目
        List<Map<String, Object>> projects = new ArrayList<>();
            for (ProjectBO p : cv.getProjects()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", nullToEmpty(p.getName()));
                m.put("role", nullToEmpty(p.getRole()));
                m.put("descriptionMarkdown", Optional.ofNullable(p.getDescriptionMarkdown()).orElse(""));
                List<String> hl = new ArrayList<>();
                if (p.getHighlights() != null) {
                    for (HighlightBO highlightBO : p.getHighlights()) {
                        hl.add(Optional.ofNullable(highlightBO).map(HighlightBO::getHighlightMarkdown).orElse(""));
                    }
                }
                m.put("highlights", hl);
                projects.add(m);
            }

        put(data, "projects", projects);

        // 技能
        List<Map<String, Object>> skills = new ArrayList<>();
        if (cv.getSkills() != null) {
            for (SkillBO s : cv.getSkills()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", nullToEmpty(s.getName()));
                m.put("level", nullToEmpty(s.getLevel()));
                skills.add(m);
            }
        }
        put(data, "skills", skills);

        // 证书
        List<Map<String, Object>> certs = new ArrayList<>();
        if (cv.getCertificates() != null) {
            for (CertificateBO ctf : cv.getCertificates()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", nullToEmpty(ctf.getName()));
                m.put("issuer", nullToEmpty(ctf.getIssuer()));
                m.put("date", ctf.getDate());
                certs.add(m);
            }
        }
        put(data, "certificates", certs);

        // 版式元数据
        FormatMetaBO meta = cv.getMeta();
        Map<String, Object> metaMap = new LinkedHashMap<>();
        if (meta != null) {
            metaMap.put("alignment", nullToEmpty(meta.getAlignment()));
            metaMap.put("lineSpacing", meta.getLineSpacing());
            metaMap.put("fontFamily", nullToEmpty(meta.getFontFamily()));
            metaMap.put("datePattern", nullToEmpty(meta.getDatePattern()));
            metaMap.put("hyperlinkStyle", nullToEmpty(meta.getHyperlinkStyle()));
            metaMap.put("showAvatar", meta.getShowAvatar());
            metaMap.put("showSocial", meta.getShowSocial());
            metaMap.put("twoColumnLayout", meta.getTwoColumnLayout());
            LocaleConfigBO lc = meta.getLocaleConfig();
            Map<String, Object> lcm = new LinkedHashMap<>();
            if (lc != null) {
                lcm.put("locale", nullToEmpty(lc.getLocale()));
                lcm.put("datePattern", nullToEmpty(lc.getDatePattern()));
            }
            metaMap.put("locale", lcm);
        }
        put(data, "meta", metaMap);

        // 应用别名映射：将别名键复制为模板期望的键
        applyAliases(data);

        return data;
    }

    private void validateRequired(CvBO cv) {
        if (StringUtils.isBlank(cv.getName())) {
            throw ValidationException.missing("name");
        }
        ContactBO contactBO = cv.getContact();
        boolean hasContact = contactBO != null && (StringUtils.isNotBlank(contactBO.getPhone()) || StringUtils.isNotBlank(contactBO.getEmail()) || StringUtils.isNotBlank(contactBO.getWechat()));
        if (!hasContact) {
            throw ValidationException.missing("contact.phone/email/wechat 之一");
        }

        if (CollectionUtils.isEmpty(cv.getEducations())){
            throw ValidationException.missing("教育经历 至少一项");
        }
    }

    private void put(Map<String, Object> data, String key, Object value) {
        data.put(resolveKey(key), value == null ? "" : value);
    }

    private String resolveKey(String key) {
        // 如果别名表指定了模板期望键，则返回别名映射到的键；否则原样
        return aliases.getOrDefault(key, key);
    }

    private void applyAliases(Map<String, Object> data) {
        // 若别名表有某键（如模板用 fullname），但模型中无该键，则尝试复制对应原键（如 name）值
        aliases.forEach((original, alias) -> {
            if (!data.containsKey(alias) && data.containsKey(original)) {
                data.put(alias, data.get(original));
            }
        });
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}