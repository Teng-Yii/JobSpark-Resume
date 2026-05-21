package com.tengYii.jobspark.common.constants;

/**
 * 简历RAG服务常量
 */
public class ResumeRagConstants {

    private ResumeRagConstants() {
    }

    // ==================== 分块类型 ====================
    public static final String CHUNK_TYPE_OVERVIEW = "overview";
    public static final String CHUNK_TYPE_SUMMARY = "summary";
    public static final String CHUNK_TYPE_SKILLS = "skills";
    public static final String CHUNK_TYPE_EXPERIENCE = "experience";
    public static final String CHUNK_TYPE_PROJECT = "project";
    public static final String CHUNK_TYPE_EDUCATION = "education";

    // ==================== 元数据Key ====================
    public static final String META_CHUNK_TYPE = "chunk_type";
    public static final String META_RESUME_ID = "resume_id";
    public static final String META_CV_TYPE = "cv_type";
    public static final String META_CHUNK_INDEX = "chunk_index";
    public static final String META_INDUSTRIES = "industries";
    public static final String META_COMPANIES = "companies";
    public static final String META_SKILL_NAMES = "skill_names";
    public static final String META_ROLES = "roles";

    // ==================== 缓存配置 ====================
    public static final String CACHE_KEY_HYDE = "rag:hyde:";
    public static final String CACHE_KEY_MQ = "rag:mq:";
    public static final long CACHE_TTL_HOUR = 1L;

    // ==================== 检索参数 ====================
    public static final double VECTOR_MIN_SCORE_COARSE = 0.60;
    public static final double VECTOR_MIN_SCORE_FINE = 0.55;
    public static final int RRF_K = 60;
    public static final int COARSE_RECALL_LIMIT = 20;
    public static final int FINE_RECALL_MULTIPLIER = 5;
    public static final int MULTI_QUERY_COUNT = 3;
}
