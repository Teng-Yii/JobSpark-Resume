package com.tengYii.jobspark.domain.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tengYii.jobspark.common.enums.DeleteFlagEnum;
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


    /**
     * 将CvBO转换并保存为PO对象
     *
     * @param cvBO 简历业务对象
     * @return 保存后的简历ID
     */
    public Long convertAndSaveCv(CvBO cvBO) {
        if (Objects.isNull(cvBO)) {
            throw new IllegalArgumentException("简历对象不能为空");
        }

        LocalDateTime nowTime = LocalDateTime.now();
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
     * 转换CvBO为CvPO
     *
     * @param cvBO    简历业务对象
     * @param nowTime 当前时间
     * @return 简历PO对象
     */
    private CvPO convertToCvPO(CvBO cvBO, LocalDateTime nowTime) {
        CvPO cvPO = new CvPO();
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
            String experienceId = UUID.randomUUID().toString();

            // 转换Experience
            CvExperiencePO experiencePO = convertToExperiencePO(experienceBO);
            experiencePO.setId(experienceId);
            experiencePO.setCvId(cvId);
            experiencePOList.add(experiencePO);

            // 转换并关联Highlights
            if (CollectionUtils.isNotEmpty(experienceBO.getHighlights())) {
                List<CvHighlightPO> highlightPOList = experienceBO.getHighlights()
                        .stream()
                        .map(highlightBO -> {
                            CvHighlightPO highlightPO = convertToHighlightPO(highlightBO);
                            highlightPO.setExperienceId(experienceId);
                            return highlightPO;
                        })
                        .collect(Collectors.toList());
                allHighlightPOList.addAll(highlightPOList);
            }
        }

        // 批量插入Experience
        experienceMapper.batchInsert(experiencePOList);

        // 批量插入所有Highlights
        if (CollectionUtils.isNotEmpty(allHighlightPOList)) {
            highlightMapper.batchInsert(allHighlightPOList);
        }
    }

    /**
     * 转换并保存项目经验列表
     *
     * @param cvId     简历ID
     * @param projects 项目经验BO列表
     */
    private void convertAndSaveProjects(Long cvId, List<ProjectBO> projects, LocalDateTime nowTime) {
        if (CollectionUtils.isNotEmpty(projects)) {
            List<CvProjectPO> projectPOs = projects.stream()
                    .filter(Objects::nonNull)
                    .map(projectBO -> {
                        CvProjectPO projectPO = new CvProjectPO();
                        BeanUtils.copyProperties(projectBO, projectPO);
                        projectPO.setCvId(cvId);
                        projectPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());

                        return projectPO;
                    })
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(projectPOs)) {
                projectRepository.saveBatch(projectPOs);
            }
        }
    }

    /**
     * 转换并保存技能列表
     *
     * @param cvId   简历ID
     * @param skills 技能BO列表
     */
    private void convertAndSaveSkills(Long cvId, List<SkillBO> skills, LocalDateTime nowTime) {
        if (CollectionUtils.isNotEmpty(skills)) {
            List<CvSkillPO> skillPOs = skills.stream()
                    .filter(Objects::nonNull)
                    .map(skillBO -> {
                        CvSkillPO skillPO = new CvSkillPO();
                        BeanUtils.copyProperties(skillBO, skillPO);
                        skillPO.setCvId(cvId);
                        skillPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());

                        return skillPO;
                    })
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(skillPOs)) {
                skillRepository.saveBatch(skillPOs);
            }
        }
    }

    /**
     * 转换并保存证书列表
     *
     * @param cvId         简历ID
     * @param certificates 证书BO列表
     */
    private void convertAndSaveCertificates(Long cvId, List<CertificateBO> certificates, LocalDateTime nowTime) {
        if (CollectionUtils.isNotEmpty(certificates)) {
            List<CvCertificatePO> certificatePOs = certificates.stream()
                    .filter(Objects::nonNull)
                    .map(certificateBO -> {
                        CvCertificatePO certificatePO = new CvCertificatePO();
                        BeanUtils.copyProperties(certificateBO, certificatePO);
                        certificatePO.setCvId(cvId);
                        certificatePO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());

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
            formatMetaPO.setCvId(cvId);

            // 如果包含LocaleConfigBO，需要单独处理
            if (Objects.nonNull(formatMetaBO.getLocaleConfig())) {
                LocaleConfigBO localeConfigBO = formatMetaBO.getLocaleConfig();

                CvLocaleConfigPO cvLocaleConfigPO = new CvLocaleConfigPO();
                BeanUtils.copyProperties(localeConfigBO, cvLocaleConfigPO);
                cvLocaleConfigPO.setDeleteFlag(DeleteFlagEnum.NOT_DELETED.getCode());

                localeConfigRepository.save(cvLocaleConfigPO);
            }

            formatMetaRepository.save(formatMetaPO);
        }
    }

}
