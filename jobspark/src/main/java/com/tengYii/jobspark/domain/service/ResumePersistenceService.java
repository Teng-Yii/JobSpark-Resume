package com.tengYii.jobspark.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tengYii.jobspark.common.enums.CvHighLightTypeEnum;
import com.tengYii.jobspark.common.enums.DeleteFlagEnum;
import com.tengYii.jobspark.common.utils.SnowflakeUtil;
import com.tengYii.jobspark.infrastructure.repo.*;
import com.tengYii.jobspark.model.bo.*;
import com.tengYii.jobspark.model.po.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ResumePersistenceService {

    @Autowired
    private CvRepository cvRepository;

    @Autowired
    private CvContactRepository contactRepository;

    @Autowired
    private CvSocialLinkRepository linkRepository;

    @Autowired
    private CvEducationRepository educationRepository;

    @Autowired
    private CvExperienceRepository experienceRepository;

    @Autowired
    private CvProjectRepository projectRepository;

    @Autowired
    private CvSkillRepository skillRepository;

    @Autowired
    private CvCertificateRepository certificateRepository;

    @Autowired
    private CvFormatMetaRepository formatMetaRepository;

    @Autowired
    private CvLocaleConfigRepository localeConfigRepository;

    @Autowired
    private CvHighlightRepository highlightRepository;


    /**
     * 将CvBO转换并保存为PO对象
     *
     * @param cvBO    简历业务对象
     * @param nowTime 当前时间
     * @return 保存后的简历ID
     */
    public Long convertAndSaveCv(CvBO cvBO, LocalDateTime nowTime) {
        if (Objects.isNull(cvBO)) {
            throw new IllegalArgumentException("简历对象不能为空");
        }

        // 1. 转换并保存主简历信息
        CvPO cvPO = convertToCvPO(cvBO, nowTime);
        cvRepository.save(cvPO);
        Long cvId = cvPO.getId();

        // 2. 转换并保存联系方式
        convertAndSaveContact(cvId, cvBO.getContact(), nowTime);

        // 3. 转换并保存社交链接
        convertAndSaveSocialLinks(cvId, cvBO.getSocialLinks(), nowTime);

        // 4. 转换并保存教育经历
        convertAndSaveEducations(cvId, cvBO.getEducations(), nowTime);

        // 5. 转换并保存工作经历
        convertAndSaveExperiences(cvId, cvBO.getExperiences(), nowTime);

        // 6. 转换并保存项目经验
        convertAndSaveProjects(cvId, cvBO.getProjects(), nowTime);

        // 7. 转换并保存技能信息
        convertAndSaveSkills(cvId, cvBO.getSkills(), nowTime);

        // 8. 转换并保存证书信息
        convertAndSaveCertificates(cvId, cvBO.getCertificates(), nowTime);

        // 9. 转换并保存格式元数据
        convertAndSaveFormatMeta(cvId, cvBO.getMeta(), nowTime);

        return cvId;
    }

    /**
     * 根据简历po对象查询并转换为BO对象
     *
     * @param cvPO 简历po对象
     * @return 简历业务对象
     */
    public CvBO convertToCvBO(CvPO cvPO) {

        // 1. 填充主cvBO对象
        Long cvId = cvPO.getId();
        CvBO cvBO = new CvBO();
        BeanUtils.copyProperties(cvPO, cvBO);

        // 2. 查询并转换联系方式
        cvBO.setContact(convertToContactBO(cvId));

        // 3. 查询并转换社交链接
        cvBO.setSocialLinks(convertToSocialLinkBOList(cvId));

        // 4. 查询并转换教育经历
        cvBO.setEducations(convertToEducationBOList(cvId));

        // 5. 查询并转换工作经历
        cvBO.setExperiences(convertToExperienceBOList(cvId));

        // 6. 查询并转换项目经验
        cvBO.setProjects(convertToProjectBOList(cvId));

        // 7. 查询并转换技能信息
        cvBO.setSkills(convertToSkillBOList(cvId));

        // 8. 查询并转换证书信息
        cvBO.setCertificates(convertToCertificateBOList(cvId));

        // 9. 查询并转换格式元数据
        cvBO.setMeta(convertToFormatMetaBO(cvId));

        return cvBO;
    }

    /**
     * 转换联系方式PO为BO
     *
     * @param cvId 简历ID
     * @return 联系方式BO
     */
    private ContactBO convertToContactBO(Long cvId) {
        CvContactPO contactPO = contactRepository.getOne(
                new LambdaQueryWrapper<CvContactPO>()
                        .eq(CvContactPO::getCvId, cvId)
                        .eq(CvContactPO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
        );

        if (Objects.isNull(contactPO)) {
            return null;
        }

        ContactBO contactBO = new ContactBO();
        BeanUtils.copyProperties(contactPO, contactBO);
        return contactBO;
    }

    /**
     * 转换社交链接PO列表为BO列表
     *
     * @param cvId 简历ID
     * @return 社交链接BO列表
     */
    private List<SocialLinkBO> convertToSocialLinkBOList(Long cvId) {
        List<CvSocialLinkPO> linkPOList = linkRepository.list(
                new LambdaQueryWrapper<CvSocialLinkPO>()
                        .eq(CvSocialLinkPO::getCvId, cvId)
                        .eq(CvSocialLinkPO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
        );

        if (CollectionUtils.isEmpty(linkPOList)) {
            return new ArrayList<>();
        }

        return linkPOList.stream()
                .map(linkPO -> {
                    SocialLinkBO linkBO = new SocialLinkBO();
                    BeanUtils.copyProperties(linkPO, linkBO);
                    return linkBO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换教育经历PO列表为BO列表
     *
     * @param cvId 简历ID
     * @return 教育经历BO列表
     */
    private List<EducationBO> convertToEducationBOList(Long cvId) {
        List<CvEducationPO> educationPOList = educationRepository.list(
                new LambdaQueryWrapper<CvEducationPO>()
                        .eq(CvEducationPO::getCvId, cvId)
                        .eq(CvEducationPO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
        );

        if (CollectionUtils.isEmpty(educationPOList)) {
            return new ArrayList<>();
        }

        return educationPOList.stream()
                .map(educationPO -> {
                    EducationBO educationBO = new EducationBO();
                    BeanUtils.copyProperties(educationPO, educationBO);
                    return educationBO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换工作经历PO列表为BO列表
     *
     * @param cvId 简历ID
     * @return 工作经历BO列表
     */
    private List<ExperienceBO> convertToExperienceBOList(Long cvId) {
        List<CvExperiencePO> experiencePOList = experienceRepository.list(
                new LambdaQueryWrapper<CvExperiencePO>()
                        .eq(CvExperiencePO::getCvId, cvId)
                        .eq(CvExperiencePO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
        );

        if (CollectionUtils.isEmpty(experiencePOList)) {
            return new ArrayList<>();
        }

        return experiencePOList.stream()
                .map(experiencePO -> {
                    ExperienceBO experienceBO = new ExperienceBO();
                    BeanUtils.copyProperties(experiencePO, experienceBO);

                    // 查询并设置工作亮点
                    List<HighlightBO> highlights = convertToHighlightBOList(
                            experiencePO.getId(),
                            CvHighLightTypeEnum.EXPERIENCE.getType()
                    );
                    experienceBO.setHighlights(highlights);

                    return experienceBO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换项目经验PO列表为BO列表
     *
     * @param cvId 简历ID
     * @return 项目经验BO列表
     */
    private List<ProjectBO> convertToProjectBOList(Long cvId) {
        List<CvProjectPO> projectPOList = projectRepository.list(
                new LambdaQueryWrapper<CvProjectPO>()
                        .eq(CvProjectPO::getCvId, cvId)
                        .eq(CvProjectPO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
        );

        if (CollectionUtils.isEmpty(projectPOList)) {
            return new ArrayList<>();
        }

        return projectPOList.stream()
                .map(projectPO -> {
                    ProjectBO projectBO = new ProjectBO();
                    BeanUtils.copyProperties(projectPO, projectBO);

                    // 查询并设置项目亮点
                    List<HighlightBO> highlights = convertToHighlightBOList(
                            projectPO.getId(),
                            CvHighLightTypeEnum.PROJECT.getType()
                    );
                    projectBO.setHighlights(highlights);

                    return projectBO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换技能PO列表为BO列表
     *
     * @param cvId 简历ID
     * @return 技能BO列表
     */
    private List<SkillBO> convertToSkillBOList(Long cvId) {
        List<CvSkillPO> skillPOList = skillRepository.list(
                new LambdaQueryWrapper<CvSkillPO>()
                        .eq(CvSkillPO::getCvId, cvId)
                        .eq(CvSkillPO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
        );

        if (CollectionUtils.isEmpty(skillPOList)) {
            return new ArrayList<>();
        }

        return skillPOList.stream()
                .map(skillPO -> {
                    SkillBO skillBO = new SkillBO();
                    BeanUtils.copyProperties(skillPO, skillBO);

                    // 查询并设置技能亮点
                    List<HighlightBO> highlights = convertToHighlightBOList(
                            skillPO.getId(),
                            CvHighLightTypeEnum.SKILL.getType()
                    );
                    skillBO.setHighlights(highlights);

                    return skillBO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换证书PO列表为BO列表
     *
     * @param cvId 简历ID
     * @return 证书BO列表
     */
    private List<CertificateBO> convertToCertificateBOList(Long cvId) {
        List<CvCertificatePO> certificatePOList = certificateRepository.list(
                new LambdaQueryWrapper<CvCertificatePO>()
                        .eq(CvCertificatePO::getCvId, cvId)
                        .eq(CvCertificatePO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
        );

        if (CollectionUtils.isEmpty(certificatePOList)) {
            return new ArrayList<>();
        }

        return certificatePOList.stream()
                .map(certificatePO -> {
                    CertificateBO certificateBO = new CertificateBO();
                    BeanUtils.copyProperties(certificatePO, certificateBO);
                    return certificateBO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换格式元数据PO为BO
     *
     * @param cvId 简历ID
     * @return 格式元数据BO
     */
    private FormatMetaBO convertToFormatMetaBO(Long cvId) {
        CvFormatMetaPO formatMetaPO = formatMetaRepository.getOne(
                new LambdaQueryWrapper<CvFormatMetaPO>()
                        .eq(CvFormatMetaPO::getCvId, cvId)
        );

        if (Objects.isNull(formatMetaPO)) {
            return null;
        }

        FormatMetaBO formatMetaBO = new FormatMetaBO();
        BeanUtils.copyProperties(formatMetaPO, formatMetaBO);

        // 查询并设置本地化配置
        if (Objects.nonNull(formatMetaPO.getId())) {
            CvLocaleConfigPO localeConfigPO = localeConfigRepository.getByFormatMetaId(formatMetaPO.getId());
            if (Objects.nonNull(localeConfigPO)) {
                LocaleConfigBO localeConfigBO = new LocaleConfigBO();
                BeanUtils.copyProperties(localeConfigPO, localeConfigBO);
                formatMetaBO.setLocaleConfig(localeConfigBO);
            }
        }

        return formatMetaBO;
    }

    /**
     * 转换亮点PO列表为BO列表
     *
     * @param relatedId 关联ID
     * @param type      亮点类型
     * @return 亮点BO列表
     */
    private List<HighlightBO> convertToHighlightBOList(Long relatedId, Integer type) {
        List<CvHighlightPO> highlightPOList = highlightRepository.list(
                new LambdaQueryWrapper<CvHighlightPO>()
                        .eq(CvHighlightPO::getRelatedId, relatedId)
                        .eq(CvHighlightPO::getType, type)
                        .eq(CvHighlightPO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
                        .orderByAsc(CvHighlightPO::getSortOrder)
        );

        if (CollectionUtils.isEmpty(highlightPOList)) {
            return new ArrayList<>();
        }

        return highlightPOList.stream()
                .map(highlightPO -> {
                    HighlightBO highlightBO = new HighlightBO();
                    BeanUtils.copyProperties(highlightPO, highlightBO);
                    return highlightBO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换CvBO为CvPO
     *
     * @param cvBO    简历业务对象
     * @param nowTime 当前时间
     * @return 简历PO对象
     */
    private CvPO convertToCvPO(CvBO cvBO, LocalDateTime nowTime) {
        CvPO cvPO = new CvPO();
        cvPO.setId(SnowflakeUtil.snowflakeId());
        BeanUtils.copyProperties(cvBO, cvPO);

        // 设置时间戳
        cvPO.setCreatedTime(nowTime);
        cvPO.setUpdatedTime(nowTime);
        cvPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());

        return cvPO;
    }

    /**
     * 转换并保存联系方式
     *
     * @param cvId      简历ID
     * @param contactBO 联系方式BO
     */
    private void convertAndSaveContact(Long cvId, ContactBO contactBO, LocalDateTime nowTime) {
        if (Objects.nonNull(contactBO)) {
            CvContactPO contactPO = new CvContactPO();
            contactPO.setId(SnowflakeUtil.snowflakeId());
            BeanUtils.copyProperties(contactBO, contactPO);

            contactPO.setCvId(cvId);
            contactPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
            contactPO.setCreatedTime(nowTime);
            contactPO.setUpdatedTime(nowTime);
            contactRepository.save(contactPO);
        }
    }

    /**
     * 转换并保存社交链接列表
     *
     * @param cvId        简历ID
     * @param socialLinks 社交链接BO列表
     */
    private void convertAndSaveSocialLinks(Long cvId, List<SocialLinkBO> socialLinks, LocalDateTime nowTime) {
        if (CollectionUtils.isNotEmpty(socialLinks)) {
            List<CvSocialLinkPO> linkPOList = socialLinks.stream()
                    .filter(Objects::nonNull)
                    .map(linkBO -> {
                        CvSocialLinkPO linkPO = new CvSocialLinkPO();
                        linkPO.setId(SnowflakeUtil.snowflakeId());
                        BeanUtils.copyProperties(linkBO, linkPO);

                        linkPO.setCvId(cvId);
                        linkPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
                        linkPO.setCreatedTime(nowTime);
                        linkPO.setUpdatedTime(nowTime);

                        return linkPO;
                    })
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(linkPOList)) {
                linkRepository.saveBatch(linkPOList);
            }
        }
    }

    /**
     * 转换并保存教育经历列表
     *
     * @param cvId       简历ID
     * @param educations 教育经历BO列表
     */
    private void convertAndSaveEducations(Long cvId, List<EducationBO> educations, LocalDateTime nowTime) {
        if (CollectionUtils.isNotEmpty(educations)) {
            List<CvEducationPO> educationPOs = educations.stream()
                    .filter(Objects::nonNull)
                    .map(educationBO -> {
                        CvEducationPO educationPO = new CvEducationPO();
                        educationPO.setId(SnowflakeUtil.snowflakeId());
                        BeanUtils.copyProperties(educationBO, educationPO);
                        educationPO.setCvId(cvId);
                        educationPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
                        educationPO.setCreatedTime(nowTime);
                        educationPO.setUpdatedTime(nowTime);

                        return educationPO;
                    })
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(educationPOs)) {
                educationRepository.saveBatch(educationPOs);
            }
        }
    }

    /**
     * 转换并保存工作经历列表
     *
     * @param cvId             简历ID
     * @param experienceBOList 工作经历BO列表
     */
    private void convertAndSaveExperiences(Long cvId, List<ExperienceBO> experienceBOList, LocalDateTime nowTime) {
        if (CollectionUtils.isEmpty(experienceBOList)) {
            return;
        }

        // 准备批量插入的数据
        List<CvExperiencePO> experiencePOList = new ArrayList<>();
        List<CvHighlightPO> allHighlightPOList = new ArrayList<>();

        // 遍历处理每个Experience
        for (ExperienceBO experienceBO : experienceBOList) {
            // 预生成Experience主键
            long experienceId = SnowflakeUtil.snowflakeId();

            // 转换Experience
            CvExperiencePO experiencePO = new CvExperiencePO();
            BeanUtils.copyProperties(experienceBO, experiencePO);
            experiencePO.setId(experienceId);
            experiencePO.setCvId(cvId);

            experiencePO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
            experiencePO.setCreatedTime(nowTime);
            experiencePO.setUpdatedTime(nowTime);
            experiencePOList.add(experiencePO);

            // 转换并关联Highlights
            if (CollectionUtils.isNotEmpty(experienceBO.getHighlights())) {
                List<CvHighlightPO> highlightPOList = experienceBO.getHighlights()
                        .stream()
                        .map(highlightBO -> {
                            CvHighlightPO highlightPO = new CvHighlightPO();

                            highlightPO.setHighlight(highlightBO.getHighlight());
                            highlightPO.setType(CvHighLightTypeEnum.EXPERIENCE.getType());
                            highlightPO.setRelatedId(experienceId);
                            highlightPO.setSortOrder(highlightBO.getSortOrder());
                            highlightPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
                            highlightPO.setCreatedTime(nowTime);
                            highlightPO.setUpdatedTime(nowTime);
                            return highlightPO;
                        })
                        .toList();
                allHighlightPOList.addAll(highlightPOList);
            }
        }

        // 批量插入Experience
        experienceRepository.saveBatch(experiencePOList);

        // 批量插入所有Highlights
        if (CollectionUtils.isNotEmpty(allHighlightPOList)) {
            highlightRepository.saveBatch(allHighlightPOList);
        }
    }

    /**
     * 转换并保存项目经验列表
     *
     * @param cvId          简历ID
     * @param projectBOList 项目经验BO列表
     * @param nowTime       当前时间
     */
    private void convertAndSaveProjects(Long cvId, List<ProjectBO> projectBOList, LocalDateTime nowTime) {
        if (CollectionUtils.isEmpty(projectBOList)) {
            return;
        }

        // 准备批量插入的数据
        List<CvProjectPO> projectPOList = new ArrayList<>();
        List<CvHighlightPO> allHighlightPOList = new ArrayList<>();

        // 遍历处理每个Project
        for (ProjectBO projectBO : projectBOList) {
            // 预生成Project主键
            long projectId = SnowflakeUtil.snowflakeId();

            // 转换Project
            CvProjectPO projectPO = new CvProjectPO();
            BeanUtils.copyProperties(projectBO, projectPO);
            projectPO.setId(projectId);
            projectPO.setCvId(cvId);

            projectPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
            projectPO.setCreatedTime(nowTime);
            projectPO.setUpdatedTime(nowTime);
            projectPOList.add(projectPO);

            // 转换并关联Highlights
            if (CollectionUtils.isNotEmpty(projectBO.getHighlights())) {
                List<CvHighlightPO> highlightPOList = projectBO.getHighlights()
                        .stream()
                        .map(highlightBO -> {
                            CvHighlightPO highlightPO = new CvHighlightPO();

                            highlightPO.setHighlight(highlightBO.getHighlight());
                            highlightPO.setType(CvHighLightTypeEnum.PROJECT.getType());
                            highlightPO.setRelatedId(projectId);
                            highlightPO.setSortOrder(highlightBO.getSortOrder());
                            highlightPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
                            highlightPO.setCreatedTime(nowTime);
                            highlightPO.setUpdatedTime(nowTime);
                            return highlightPO;
                        })
                        .toList();
                allHighlightPOList.addAll(highlightPOList);
            }
        }

        // 批量插入Project
        projectRepository.saveBatch(projectPOList);

        // 批量插入所有Highlights
        if (CollectionUtils.isNotEmpty(allHighlightPOList)) {
            highlightRepository.saveBatch(allHighlightPOList);
        }
    }

    /**
     * 转换并保存技能列表
     *
     * @param cvId        简历ID
     * @param skillBOList 技能BO列表
     * @param nowTime     当前时间
     */
    private void convertAndSaveSkills(Long cvId, List<SkillBO> skillBOList, LocalDateTime nowTime) {
        if (CollectionUtils.isEmpty(skillBOList)) {
            return;
        }

        // 准备批量插入的数据
        List<CvSkillPO> skillPOList = new ArrayList<>();
        List<CvHighlightPO> allHighlightPOList = new ArrayList<>();

        // 遍历处理每个Skill
        for (SkillBO skillBO : skillBOList) {
            // 预生成Skill主键
            long skillId = SnowflakeUtil.snowflakeId();

            // 转换Skill
            CvSkillPO skillPO = new CvSkillPO();
            BeanUtils.copyProperties(skillBO, skillPO);
            skillPO.setId(skillId);
            skillPO.setCvId(cvId);

            skillPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
            skillPO.setCreatedTime(nowTime);
            skillPO.setUpdatedTime(nowTime);
            skillPOList.add(skillPO);

            // 转换并关联Highlights
            if (CollectionUtils.isNotEmpty(skillBO.getHighlights())) {
                List<CvHighlightPO> highlightPOList = skillBO.getHighlights()
                        .stream()
                        .map(highlightBO -> {
                            CvHighlightPO highlightPO = new CvHighlightPO();

                            highlightPO.setHighlight(highlightBO.getHighlight());
                            highlightPO.setType(CvHighLightTypeEnum.SKILL.getType());
                            highlightPO.setRelatedId(skillId);
                            highlightPO.setSortOrder(highlightBO.getSortOrder());
                            highlightPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
                            highlightPO.setCreatedTime(nowTime);
                            highlightPO.setUpdatedTime(nowTime);
                            return highlightPO;
                        })
                        .toList();
                allHighlightPOList.addAll(highlightPOList);
            }
        }

        // 批量插入Skill
        skillRepository.saveBatch(skillPOList);

        // 批量插入所有Highlights
        if (CollectionUtils.isNotEmpty(allHighlightPOList)) {
            highlightRepository.saveBatch(allHighlightPOList);
        }
    }

    /**
     * 转换并保存证书列表
     *
     * @param cvId              简历ID
     * @param certificateBOList 证书BO列表
     */
    private void convertAndSaveCertificates(Long cvId, List<CertificateBO> certificateBOList, LocalDateTime nowTime) {
        if (CollectionUtils.isNotEmpty(certificateBOList)) {
            List<CvCertificatePO> certificatePOs = certificateBOList.stream()
                    .filter(Objects::nonNull)
                    .map(certificateBO -> {
                        CvCertificatePO certificatePO = new CvCertificatePO();
                        BeanUtils.copyProperties(certificateBO, certificatePO);
                        certificatePO.setCvId(cvId);
                        certificatePO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
                        certificatePO.setCreatedTime(nowTime);
                        certificatePO.setUpdatedTime(nowTime);

                        return certificatePO;
                    })
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(certificatePOs)) {
                certificateRepository.saveBatch(certificatePOs);
            }
        }
    }

    /**
     * 转换并保存格式元数据
     *
     * @param cvId         简历ID
     * @param formatMetaBO 格式元数据BO
     */
    private void convertAndSaveFormatMeta(Long cvId, FormatMetaBO formatMetaBO, LocalDateTime nowTime) {
        if (Objects.nonNull(formatMetaBO)) {
            CvFormatMetaPO formatMetaPO = new CvFormatMetaPO();
            BeanUtils.copyProperties(formatMetaBO, formatMetaPO);

            // 预生成FormatMeta主键ID
            long formatMetaId = SnowflakeUtil.snowflakeId();
            formatMetaPO.setId(formatMetaId);
            formatMetaPO.setCvId(cvId);

            formatMetaPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
            formatMetaPO.setCreatedTime(nowTime);
            formatMetaPO.setUpdatedTime(nowTime);

            // 如果包含LocaleConfigBO，需要单独处理
            if (Objects.nonNull(formatMetaBO.getLocaleConfig())) {
                LocaleConfigBO localeConfigBO = formatMetaBO.getLocaleConfig();

                CvLocaleConfigPO cvLocaleConfigPO = new CvLocaleConfigPO();
                BeanUtils.copyProperties(localeConfigBO, cvLocaleConfigPO);
                cvLocaleConfigPO.setFormatMetaId(formatMetaId);
                cvLocaleConfigPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());
                cvLocaleConfigPO.setCreatedTime(nowTime);
                cvLocaleConfigPO.setUpdatedTime(nowTime);

                localeConfigRepository.save(cvLocaleConfigPO);
            }

            formatMetaRepository.save(formatMetaPO);
        }
    }

}
