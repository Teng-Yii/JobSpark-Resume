package com.tengYii.jobspark.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

public class McpConnectTest {

    @Test
    public void testMcpConnect(){
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("http://localhost:8017/mcp")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        List<ToolSpecification> tools = mcpClient.listTools();
        assert !tools.isEmpty();
    }
}
