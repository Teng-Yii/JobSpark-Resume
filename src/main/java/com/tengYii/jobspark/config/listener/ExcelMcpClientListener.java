package com.tengYii.jobspark.config.listener;

import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.protocol.McpCallToolRequest;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ExcelAgent MCP客户端监听器
 * <p>
 * 用于监听ExcelAgent的MCP调用情况，记录工具调用、资源获取和Prompt获取的详细日志
 * </p>
 */
public class ExcelMcpClientListener implements McpClientListener {

    /**
     * 日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelMcpClientListener.class);

    /**
     * MCP工具执行前回调
     *
     * @param context MCP调用上下文
     */
    @Override
    public void beforeExecuteTool(McpCallContext context) {
        if (Objects.isNull(context)) {
            LOGGER.warn("MCP工具执行前 - 上下文为空");
            return;
        }

        String toolName = extractToolName(context);
        Object toolArguments = extractToolArguments(context);

        LOGGER.info("MCP工具执行开始 - 工具名称: {}, 请求参数: {}", toolName, toolArguments);
    }

    /**
     * MCP工具执行后回调
     *
     * @param context   MCP调用上下文
     * @param result    工具执行结果
     * @param rawResult 原始结果
     */
    @Override
    public void afterExecuteTool(McpCallContext context, ToolExecutionResult result, Map<String, Object> rawResult) {
        if (Objects.isNull(context)) {
            LOGGER.warn("MCP工具执行后 - 上下文为空");
            return;
        }

        String toolName = extractToolName(context);
        Object toolArguments = extractToolArguments(context);

        Map resultMap = (HashMap) rawResult.get("result");
        LOGGER.info("MCP工具执行完成 - 工具名称: {}, 请求参数: {}, 结果: {}",
                toolName,
                toolArguments,
                resultMap.get("content"));
    }

    /**
     * MCP工具执行错误回调
     *
     * @param context MCP调用上下文
     * @param error   错误信息
     */
    @Override
    public void onExecuteToolError(McpCallContext context, Throwable error) {
        if (Objects.isNull(context)) {
            LOGGER.error("MCP工具执行错误 - 上下文为空", error);
            return;
        }

        String toolName = extractToolName(context);
        Object toolArguments = extractToolArguments(context);

        LOGGER.error("MCP工具执行错误 - 工具名称: {}, 请求参数: {}, 错误信息: {}",
                toolName,
                toolArguments,
                Objects.nonNull(error) ? error.getMessage() : "未知错误",
                error);
    }

    /**
     * MCP资源获取前回调
     *
     * @param context MCP调用上下文
     */
    @Override
    public void beforeResourceGet(McpCallContext context) {
        if (Objects.isNull(context)) {
            LOGGER.warn("MCP资源获取前 - 上下文为空");
            return;
        }
    }

    /**
     * MCP资源获取后回调
     *
     * @param context   MCP调用上下文
     * @param result    资源读取结果
     * @param rawResult 原始结果
     */
    @Override
    public void afterResourceGet(McpCallContext context, McpReadResourceResult result, Map<String, Object> rawResult) {
        if (Objects.isNull(context)) {
            LOGGER.warn("MCP资源获取后 - 上下文为空");
            return;
        }

        Object toolArguments = extractToolArguments(context);

        LOGGER.info("MCP资源获取完成 - 请求参数: {}, 结果状态: {}, 原始结果大小: {}",
                toolArguments,
                Objects.nonNull(result) ? "成功" : "空",
                MapUtils.isNotEmpty(rawResult) ? rawResult.size() : 0);
    }

    /**
     * MCP资源获取错误回调
     *
     * @param context MCP调用上下文
     * @param error   错误信息
     */
    @Override
    public void onResourceGetError(McpCallContext context, Throwable error) {
        if (Objects.isNull(context)) {
            LOGGER.error("MCP资源获取错误 - 上下文为空", error);
            return;
        }

        Object toolArguments = extractToolArguments(context);

        LOGGER.error("MCP资源获取错误 - 请求参数: {}, 错误信息: {}",
                toolArguments,
                Objects.nonNull(error) ? error.getMessage() : "未知错误",
                error);
    }

    /**
     * MCP Prompt获取前回调
     *
     * @param context MCP调用上下文
     */
    @Override
    public void beforePromptGet(McpCallContext context) {
    }

    /**
     * MCP Prompt获取后回调
     *
     * @param context   MCP调用上下文
     * @param result    Prompt获取结果
     * @param rawResult 原始结果
     */
    @Override
    public void afterPromptGet(McpCallContext context, McpGetPromptResult result, Map<String, Object> rawResult) {
    }

    /**
     * MCP Prompt获取错误回调
     *
     * @param context MCP调用上下文
     * @param error   错误信息
     */
    @Override
    public void onPromptGetError(McpCallContext context, Throwable error) {
    }

    /**
     * 从上下文中提取工具名称
     *
     * @param context MCP调用上下文
     * @return 工具名称，如果无法获取则返回"未知"
     */
    private String extractToolName(McpCallContext context) {
        if (Objects.isNull(context) || Objects.isNull(context.message())) {
            return "未知";
        }

        Map<String, Object> params = ((McpCallToolRequest) context.message()).getParams();
        return MapUtils.isNotEmpty(params)
                ? (String) params.get("name")
                : "未知";
    }

    /**
     * 从上下文中提取请求参数
     *
     * @param context MCP调用上下文
     * @return 请求参数，如果无法获取则返回"未知"
     */
    private Object extractToolArguments(McpCallContext context) {
        if (Objects.isNull(context) || Objects.isNull(context.message())) {
            return "未知";
        }

        Map<String, Object> params = ((McpCallToolRequest) context.message()).getParams();
        return MapUtils.isNotEmpty(params)
                ? params.get("arguments")
                : "未知";
    }
}
