package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.domain.agent.CvOptimizationAgent;
import com.tengYii.jobspark.domain.model.*;
import com.tengYii.jobspark.infrastructure.file.FileStorageService;
import com.tengYii.jobspark.model.Cv;
import com.tengYii.jobspark.utils.ChatModelProvider;
import com.tengYii.jobspark.utils.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    private final FileStorageService fileStorageService;
    private final ChatModel chatModel = ChatModelProvider.createChatModel();

    public static void main(String[] args)  throws IOException {
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        ChatModel chatModel = ChatModelProvider.createChatModel();
        CvOptimizationAgent cvOptimizationAgent = AgenticServices.createAgenticSystem(CvOptimizationAgent.class, chatModel);
        String optimizeCv = cvOptimizationAgent.optimizeCv(masterCv, jobDescription);
//        Cv optimizeCv = cvOptimizationAgent.optimizeCv(masterCv, jobDescription);

        System.out.println(optimizeCv);
    }

    public Resume analyzeResume(MultipartFile file, String jobTitle, String industry) {
        try {
            // 存储文件并获取文件ID
            String fileId = fileStorageService.storeResumeFile(file);
            
            // 读取文件内容
            String fileContent = fileStorageService.getFileContent(fileId);
            
            // 创建简历对象
            Resume resume = new Resume(fileId, file.getOriginalFilename(), fileContent);
            
            // 使用AI解析简历内容
            ResumeAnalysisResult analysisResult = parseResumeWithAI(fileContent, jobTitle, industry);
            
            // 标记简历为已分析状态
            resume.markAsAnalyzed(
                analysisResult.getParsedContent(),
                analysisResult.getBasicInfo(),
                analysisResult.getEducationExperiences(),
                analysisResult.getWorkExperiences(),
                analysisResult.getSkills()
            );
            
            log.info("简历解析完成: {}", fileId);
            return resume;
            
        } catch (Exception e) {
            log.error("简历解析失败", e);
            throw new RuntimeException("简历解析失败: " + e.getMessage(), e);
        }
    }

    private ResumeAnalysisResult parseResumeWithAI(String resumeContent, String jobTitle, String industry) {
        try {
            String prompt = buildResumeAnalysisPrompt(resumeContent, jobTitle, industry);
            String aiResponse = chatModel.chat(prompt);
            
            // 解析AI响应并构建结果对象
            return parseAIResponse(aiResponse);
        } catch (Exception e) {
            log.error("AI简历解析失败", e);
            throw new RuntimeException("AI简历解析失败: " + e.getMessage(), e);
        }
    }

    private String buildResumeAnalysisPrompt(String resumeContent, String jobTitle, String industry) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请解析以下简历内容，提取关键信息：\n\n");
        prompt.append("简历内容：\n");
        prompt.append(resumeContent);
        prompt.append("\n\n");
        
        if (jobTitle != null && !jobTitle.isEmpty()) {
            prompt.append("目标职位：").append(jobTitle).append("\n");
        }
        if (industry != null && !industry.isEmpty()) {
            prompt.append("行业：").append(industry).append("\n");
        }
        
        prompt.append("\n请按以下格式返回解析结果：\n");
        prompt.append("基本信息：姓名、邮箱、电话、地点、个人简介、目标职位、工作年限\n");
        prompt.append("教育经历：学校、学位、专业、时间、描述\n");
        prompt.append("工作经历：公司、职位、时间、描述、成就\n");
        prompt.append("技能：技能名称、熟练程度、类别、描述\n");
        
        return prompt.toString();
    }

    private ResumeAnalysisResult parseAIResponse(String aiResponse) {
        // 这里简化处理，实际应该使用更复杂的解析逻辑
        // 可以使用JSON格式让AI返回结构化数据
        
        // 模拟解析结果
        ResumeBasicInfo basicInfo = new ResumeBasicInfo(
            "张三", "zhangsan@email.com", "13800138000", "北京", 
            "资深Java开发工程师", "Java开发工程师", "5年", "阿里巴巴"
        );
        
        List<EducationExperience> educationList = new ArrayList<>();
        educationList.add(new EducationExperience(
            "清华大学", "本科", "计算机科学与技术", "2015-2019", 
            "主修计算机科学，获得学士学位"
        ));
        
        List<WorkExperience> workList = new ArrayList<>();
        workList.add(new WorkExperience(
            "阿里巴巴", "Java开发工程师", "2019-2022", 
            "负责核心业务系统开发", "获得优秀员工奖"
        ));
        
        List<Skill> skills = new ArrayList<>();
        skills.add(new Skill("Java", "精通", "编程语言", "5年Java开发经验"));
        skills.add(new Skill("Spring Boot", "熟练", "框架", "熟悉Spring全家桶"));
        
        return new ResumeAnalysisResult(aiResponse, basicInfo, educationList, workList, skills);
    }

    public Object getResumeAnalysis(String resumeId) {
        // 获取简历分析结果
        // 这里可以返回解析后的结构化数据
        return "简历分析结果";
    }

    // 内部类用于封装解析结果
    @RequiredArgsConstructor
    @Slf4j
    private static class ResumeAnalysisResult {
        private final String parsedContent;
        private final ResumeBasicInfo basicInfo;
        private final List<EducationExperience> educationExperiences;
        private final List<WorkExperience> workExperiences;
        private final List<Skill> skills;

        public String getParsedContent() { return parsedContent; }
        public ResumeBasicInfo getBasicInfo() { return basicInfo; }
        public List<EducationExperience> getEducationExperiences() { return educationExperiences; }
        public List<WorkExperience> getWorkExperiences() { return workExperiences; }
        public List<Skill> getSkills() { return skills; }
    }
}