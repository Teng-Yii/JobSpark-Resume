package com.tengYii.jobspark.domain.agent;

//import com.tengYii.jobspark.domain.agent.tool.ExcelTool;
import dev.langchain4j.agentic.declarative.ToolProviderSupplier;
import dev.langchain4j.agentic.declarative.ToolsSupplier;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.time.Duration;
import java.util.List;

public interface ExcelAgent {

    @SystemMessage("你是一个 Excel 助手，可以帮助用户创建和操作 Excel 文件。你可以使用提供的工具来完成任务。")
    String chat(@UserMessage String userMessage);


//    @ToolsSupplier
//    static Object[] tools() {
//        return new Object[]{new ExcelTool()};
//    }

    @ToolProviderSupplier
    static McpToolProvider toolProvider() {
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("http://localhost:8017/mcp")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                // 允许通过名称过滤mcp工具
                .filterToolNames("get_issue", "get_issue_comments", "create_chart")
                .build();

        return toolProvider;
    }
}
