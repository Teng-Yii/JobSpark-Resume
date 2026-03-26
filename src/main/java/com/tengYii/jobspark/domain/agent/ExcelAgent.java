package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.config.listener.ExcelMcpClientListener;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatMemoryProviderSupplier;
import dev.langchain4j.agentic.declarative.ToolProviderSupplier;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.*;
import dev.langchain4j.service.tool.ToolProvider;

import java.time.Duration;
import java.util.List;

/**
 * Excel智能处理Agent
 * <p>
 * 基于MCP工具提供Excel文件的读取、分析、处理和可视化能力
 * </p>
 */
public interface ExcelAgent {

    /**
     * Excel智能对话处理
     *
     * @param filePath    Excel文件路径
     * @param userMessage 用户指令消息
     * @return 处理结果
     */
    @Agent("专业Excel智能处理助手，具备Excel文件读取、数据分析、图表生成和数据处理能力。" +
            "支持多种Excel操作场景，包括数据提取、格式转换、统计分析、可视化图表生成等。")
    @SystemMessage("""
            你是一个专业的Excel数据处理专家，具备以下核心能力：
            
            ### 核心功能
            1. **数据读取与解析**
               - 支持读取Excel文件的各类数据格式
               - 智能识别表格结构、表头和数据类型
               - 处理合并单元格、公式等复杂格式
            
            2. **数据分析**
               - 数据统计与汇总分析
               - 数据质量检查与异常识别
               - 数据趋势分析与洞察提取
            
            3. **数据处理**
               - 数据清洗与格式转换
               - 数据筛选、排序与分组
               - 数据合并与拆分操作
            
            4. **可视化能力**
               - 支持生成多种类型的图表，图表生成在其他sheet中创建
               - 数据可视化建议与实现
            
            ### 工具使用策略
            - **MCP工具优先**: 当需要读取Excel文件内容或分析文件或操作数据时，优先使用MCP工具
            - **智能建议**: 根据用户需求提供数据处理建议
            - **结果输出**: 以清晰、结构化的方式返回处理结果
            
            ### 响应规范
            - 准确理解用户意图，选择合适的处理方式
            - 提供详细的处理过程和结果说明
            - 如遇问题，给出明确的错误原因和解决建议
            """)
    @UserMessage("""
            请处理以下Excel相关请求：
            
            文件路径：{{filePath}}
            
            用户指令：
            {{userMessage}}
            
            请根据上述要求执行相应操作，并返回处理结果。
            """)
    Result<String> chat(@MemoryId String memoryId, @V("filePath") String filePath, @V("userMessage") String userMessage);

    @ChatMemoryProviderSupplier
    static ChatMemory chatMemory(Object memoryId) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .build();
    }

    @ToolProviderSupplier
    static ToolProvider toolProvider() {
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("http://localhost:8017/mcp")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        // 创建MCP客户端监听器，用于监听MCP调用情况
        dev.langchain4j.mcp.client.McpClientListener listener = new ExcelMcpClientListener();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .listener(listener)
                .build();

        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                // 允许通过名称过滤mcp工具
//                .filterToolNames("get_issue", "get_issue_comments", "create_chart")
                .build();

        return toolProvider;
    }
}


