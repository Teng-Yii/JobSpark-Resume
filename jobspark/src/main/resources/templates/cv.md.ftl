<#function h n>
    <#assign level = (n + headingOffset)!n>
    <#assign hashes = "" />
    <#list 1..level as i>
        <#assign hashes = hashes + "#" />
    </#list>
    <#return hashes>
</#function>

<#function fmtDate d="">
    <#if d?has_content>
        <#assign pattern = meta.datePattern!'yyyy.MM'>
        <#if d?is_date_like>
            <#return d?string(pattern)>
        <#else>
            <#return d?string>
        </#if>
    <#else>
        <#return "至今">
    </#if>
</#function>

<#function bulletList items>
    <#if items?has_content>
        <#list items as it>
- <#if it?is_hash> ${it.highlight} <#else> ${it} </#if>
        </#list>
    </#if>
</#function>

<#-- 顶部个人信息块 -->
<#if includeHeaderBlock!true>
${h(1)} ${name}<#if title?has_content> · ${title}</#if>
<#if age?has_content>年龄：${age}</#if>

联系方式：<#if contact.phone?has_content>📱 ${contact.phone}</#if><#if contact.email?has_content>  ✉️ ${contact.email}</#if><#if contact.wechat?has_content>  🟩 ${contact.wechat}</#if><#if contact.location?has_content>  📍 ${contact.location}</#if>

<#if socialLinks?has_content>
社交链接：
<#list socialLinks as s>
- [${s.label}](${s.url})
</#list>
</#if>

</#if>

<#-- 个人摘要 -->
<#if summary?has_content>
${h(2)} 个人摘要

${summary}
</#if>

<#-- 教育经历 -->
<#if educations?has_content>
${h(2)} 教育经历

<#list educations as edu>
${h(3)} ${edu.school} · ${edu.major}<#if edu.degree?has_content> · ${edu.degree}</#if>（${fmtDate(edu.startDate!)} - ${fmtDate(edu.endDate!)}）
<#if edu.description?has_content>
${edu.description}
</#if>

</#list>
</#if>

<#-- 工作/实习经历 -->
<#if experiences?has_content>
${h(2)} 实习/工作经历

<#list experiences as exp>
${h(3)} ${exp.company} · ${exp.role}（${fmtDate(exp.startDate!)} - ${fmtDate(exp.endDate!)}）
<#if exp.description?has_content>
${exp.description}
</#if>
<#if exp.highlights?has_content>
<#if compactList!true>
<#list exp.highlights as hl>
- <#if hl?is_hash> ${hl.highlight} <#else> ${hl} </#if>
</#list>
<#else>
${bulletList(exp.highlights)}
</#if>
</#if>

</#list>
</#if>

<#-- 项目经验 -->
<#if projects?has_content>
${h(2)} 项目经验

<#list projects as p>
${h(3)} ${p.name} · ${p.role}（${fmtDate(p.startDate!)} - ${fmtDate(p.endDate!)}）
<#if p.description?has_content>
${p.description}
</#if>
<#if p.highlights?has_content>
<#list p.highlights as hl>
- <#if hl?is_hash> ${hl.highlight} <#else> ${hl} </#if>
</#list>
</#if>

</#list>
</#if>

<#-- 技能/亮点 -->
<#if skills?has_content>
${h(2)} 技能与亮点

<#list skills as s>
- **${s.name}**<#if s.level?has_content>（${s.level}）</#if><#if s.highlights?has_content>：<#list s.highlights as hl><#if hl?is_hash> ${hl.highlight} <#else> ${hl} </#if><#if hl_has_next>；</#if></#list></#if>
</#list>
</#if>

<#-- 证书/获奖 -->
<#if certificates?has_content>
${h(2)} 证书与获奖

<#list certificates as c>
- ${c.name} · ${c.issuer}<#if c.date?has_content>（${fmtDate(c.date!)}）</#if>
</#list>
</#if>