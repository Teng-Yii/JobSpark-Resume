package com.tengYii.jobspark.bug;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.observability.*;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复现 langchain4j-agentic 1.12.2-beta22 ThreadLocal NPE bug 的最小测试用例
 * <p>
 * Bug 描述：
 * 当 Agent 作为顶层入口被直接调用（非子 Agent 模式）时，
 * AgentInvocationHandler.invoke() 不会调用 LangChain4jManaged.setCurrent() 设置 ThreadLocal，
 * 导致 AgentBuilder 注册的工具执行回调中 LangChain4jManaged.current() 返回 null，触发 NPE。
 * <p>
 * 触发条件：
 * 1. Agent 构建时配置了 listener
 * 3. 从业务代码直接调用 Agent 方法（非 Planner 编排调用）
 * <p>
 */
@Slf4j
class LangChain4jThreadLocalNpeTest {


    // ==================== 定义简单 Listener ====================

    static class SimpleAgentListener implements AgentListener {

        private final String name;
        private boolean beforeToolCalled = false;
        private boolean afterToolCalled = false;
        private Throwable capturedException = null;
        private AgenticScope capturedScope = null;

        public SimpleAgentListener(String name) {
            this.name = name;
        }

        @Override
        public void beforeAgentInvocation(AgentRequest agentRequest) {
            log.info("[{}] beforeAgentInvocation: {}", name, agentRequest.agentName());
        }

        @Override
        public void afterAgentInvocation(AgentResponse agentResponse) {
            log.info("[{}] afterAgentInvocation: {}", name, agentResponse.agentName());
        }

        @Override
        public void onAgentInvocationError(AgentInvocationError error) {
            log.error("[{}] onAgentInvocationError: {}", name, error.error().getMessage());
            this.capturedException = error.error();
        }

        @Override
        public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
            log.info("[{}] beforeAgentToolExecution: tool={}", name,
                    beforeAgentToolExecution.toolExecution().request().name());
            this.beforeToolCalled = true;
            this.capturedScope = beforeAgentToolExecution.agenticScope();
        }

        @Override
        public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
            log.info("[{}] afterAgentToolExecution: tool={}", name,
                    afterAgentToolExecution.toolExecution().request().name());
            this.afterToolCalled = true;
            this.capturedScope = afterAgentToolExecution.agenticScope();
        }

        @Override
        public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        }

        @Override
        public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        }

        @Override
        public boolean inheritedBySubagents() {
            return false;
        }

        public boolean wasBeforeToolCalled() { return beforeToolCalled; }
        public boolean wasAfterToolCalled() { return afterToolCalled; }
        public Throwable getCapturedException() { return capturedException; }
        public AgenticScope getCapturedScope() { return capturedScope; }
    }

    // ==================== 定义简单 Tool ====================

    public static class SimpleTool {
        @Tool("一个简单的测试工具，返回输入的大写形式")
        public String toUpperCase(String text) {
            log.info("Tool被执行: toUpperCase({})", text);
            return text != null ? text.toUpperCase() : "NULL";
        }
    }

    // ==================== Mock ChatModel（模拟 LLM 返回工具调用） ====================

    static class MockToolCallingChatModel implements ChatModel {
        private final String toolNameToCall;
        private int callCount = 0;

        public MockToolCallingChatModel(String toolNameToCall) {
            this.toolNameToCall = toolNameToCall;
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            callCount++;

            // 第一次调用：返回工具调用请求
            if (callCount == 1) {
                AiMessage aiMessage = AiMessage.builder()
                        .toolExecutionRequests(List.of(
                                ToolExecutionRequest.builder()
                                        .name(toolNameToCall)
                                        .arguments("{\"text\": \"hello\"}")
                                        .build()
                        ))
                        .build();

                return ChatResponse.builder()
                        .aiMessage(aiMessage)
                        .build();
            }

            // 第二次调用：返回最终结果
            AiMessage finalMessage = AiMessage.builder()
                    .text("工具执行完成，结果是：HELLO")
                    .build();
            return ChatResponse.builder()
                    .aiMessage(finalMessage)
                    .build();
        }
    }

    @Test
    public void testListener(){
        MockToolCallingChatModel mockChatModel = new MockToolCallingChatModel("toUpperCase");

        SimpleAgentListener listener = new SimpleAgentListener("TestListener");

        // 配置 listener
        SimpleAgent agent = AgenticServices
                .agentBuilder(SimpleAgent.class)
                .chatModel(mockChatModel)
//                .tools(new SimpleTool())
                 .listener(listener)
                .build();

        String helllo = agent.execute("helllo");
        System.out.println(helllo);
    }
}
