<#-- 简历 Markdown 模板：FreeMarker (.ftl) -> Markdown
     渲染后将交由 CommonMark 解析为 HTML，再用于 PDF/Docx。
     变量说明：
     - 基本：name, age, title, avatarUrl
     - 摘要：summary (string)
     - 联系方式：contact.{phone,email,wechat,location}
     - 社交：socialLinks[] -> {label,url}
     - 教育：educations[] -> {school,major,startDate,endDate,description}
     - 经历：experiences[] -> {company,role,startDate,endDate,highlights[]}
     - 项目：projects[] -> {name,role,description,highlights[]}
     - 技能：skills[] -> {name,level}
     - 证书：certificates[] -> {name,issuer,date}
     - 元数据：meta.* （格式控制，主要由 CSS 在 HTML 阶段处理）
     - 额外渲染开关：headingOffset, compactList, includeHeaderBlock
-->

<#function h n>
    <#-- 标题级别偏移：h(1) -> #, h(2) -> ## -->
    <#assign level = (n + headingOffset)!n>
    <#assign hashes = "" />
    <#list 1..level as i>
        <#assign hashes = hashes + "#" />
    </#list>
    <#return hashes>
</#function>

<#function fmtDate d>
    <#-- 日期格式示例：如果为空，返回“至今/在读”等 -->
    <#if d?has_content>
        ${d?string((meta.datePattern)!"yyyy.MM")}
    <#else>
        至今
    </#if>
</#function>

<#function bulletList items>
    <#-- 将字符串列表渲染为 Markdown 列表 -->
    <#if items?has_content>
        <#list items as it>
- ${it}
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
${h(3)} ${edu.school} · ${edu.major}（${fmtDate(edu.startDate)} - ${fmtDate(edu.endDate)}）
<#if edu.description?has_content>
${edu.description}
</#if>

</#list>
</#if>

<#-- 工作/实习经历 -->
<#if experiences?has_content>
${h(2)} 实习/工作经历

<#list experiences as exp>
${h(3)} ${exp.company} · ${exp.role}（${fmtDate(exp.startDate)} - ${fmtDate(exp.endDate)}）
<#if exp.highlights?has_content>
<#if compactList!true>
<#list exp.highlights as hl>
- ${hl}
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
${h(3)} ${p.name} · ${p.role}
<#if p.description?has_content>
${p.description}
</#if>
<#if p.highlights?has_content>
<#list p.highlights as hl>
- ${hl}
</#list>
</#if>

</#list>
</#if>

<#-- 技能/亮点 -->
<#if skills?has_content>
${h(2)} 技能与亮点

<#list skills as s>
- ${s.name}<#if s.level?has_content>（${s.level}）</#if>
</#list>
</#if>

<#-- 证书/获奖 -->
<#if certificates?has_content>
${h(2)} 证书与获奖

<#list certificates as c>
- ${c.name} · ${c.issuer}<#if c.date?has_content>（${fmtDate(c.date)}）</#if>
</#list>
</#if>