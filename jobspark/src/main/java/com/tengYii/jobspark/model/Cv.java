package com.tengYii.jobspark.model;

import dev.langchain4j.model.output.structured.Description;

public class Cv {
    @Description("候选人的技能，以逗号分隔")
    private String skills;

    @Description("候选人的专业经验")
    private String professionalExperience;

    @Description("候选人的研究")
    private String studies;

    @Override
    public String toString() {
        return "CV:\n" +
                "skills = \"" + skills + "\"\n" +
                "professionalExperience = \"" + professionalExperience + "\"\n" +
                "studies = \"" + studies + "\"\n";
    }
}
