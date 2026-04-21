package com.tengYii.jobspark.bug;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.observability.AfterAgentToolExecution;
import dev.langchain4j.agentic.observability.BeforeAgentToolExecution;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 复现 Bug: beforeAgentInvocation 方法未被调用，但 beforeAgentToolExecution/afterAgentToolExecution 被调用
 *
 * <p>
 * <b>Bug 描述：</b><br>
 * 当使用 AgenticServices.agentBuilder() 创建的 Agent 被直接调用时（非 Planner 编排模式），<br>
 * beforeAgentInvocation() 和 afterAgentInvocation() 回调方法不会被触发，<br>
 * 但 beforeAgentToolExecution() 和 afterAgentToolExecution() 回调方法会被正常触发。
 *
 * <p>
 * <b>Log 和 Stack trace：</b><br>
 * 无异常抛出，但 Listener 中的以下方法未被调用：
 * - beforeAgentInvocation(AgentRequest)
 * - afterAgentInvocation(AgentResponse)
 *
 * <p>
 * <b>复现步骤：</b>
 * 1. 使用 AgenticServices.agentBuilder() 创建一个带有 @Agent 注解方法的 Agent 接口
 * 2. 为该 Agent 配置一个实现了 AgentListener 的监听器
 * 3. 直接调用 Agent 方法（非通过 Planner 编排）
 * 4. 观察监听器的回调方法调用情况
 *
 * <p>
 * <b>预期行为：</b><br>
 * 当 Agent 方法被调用时，以下回调应该按照以下顺序被触发：
 * 1. beforeAgentInvocation(AgentRequest)
 * 2. [Agent 内部执行，LLM 调用 Tool]
 * 3. beforeAgentToolExecution(BeforeAgentToolExecution)
 * 4. afterAgentToolExecution(AfterAgentToolExecution)
 * 5. afterAgentInvocation(AgentResponse)
 *
 * <p>
 * <b>实际行为：</b><br>
 * 只触发了第 3、4 步，第 1、5 步未被触发。
 *
 * <p>
 * <b>LangChain4j 版本：</b> 1.14.0-beta24-SNAPSHOT<br>
 * <b>LLM(s) used:</b> Mock (模拟)<br>
 * <b>Java version:</b> 17+<br>
 * <b>Spring Boot version:</b> N/A
 *
 * <p>
 * <b>附加说明：</b><br>
 * 此问题存在于 AgentInvocationHandler 中，该处理器在处理 Agent 方法调用时，<br>
 * 没有像 PlannerBasedInvocationHandler 那样调用 beforeAgentInvocation/afterAgentInvocation 回调。
 */
public class AgentListenerNotCalledBugTest {

    /**
     * 测试用 Tool
     */
    public static class TestTool {
        @Tool("将文本转换为大写")
        public String toUpperCase(String text) {
            return text != null ? text.toUpperCase() : "NULL";
        }
    }

    /**
     * 自定义 AgentListener，用于验证各个回调方法是否被调用
     */
    static class TestAgentListener implements AgentListener {

        private final AtomicBoolean beforeInvocationCalled = new AtomicBoolean(false);
        private final AtomicBoolean afterInvocationCalled = new AtomicBoolean(false);
        private final AtomicBoolean beforeToolExecutionCalled = new AtomicBoolean(false);
        private final AtomicBoolean afterToolExecutionCalled = new AtomicBoolean(false);

        @Override
        public void beforeAgentInvocation(AgentRequest agentRequest) {
            beforeInvocationCalled.set(true);
            System.out.println("[TestListener] beforeAgentInvocation called: agent=" + agentRequest.agentName());
        }

        @Override
        public void afterAgentInvocation(AgentResponse agentResponse) {
            afterInvocationCalled.set(true);
            System.out.println("[TestListener] afterAgentInvocation called: agent=" + agentResponse.agentName());
        }

        @Override
        public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
            beforeToolExecutionCalled.set(true);
            System.out.println("[TestListener] beforeAgentToolExecution called: tool="
                    + beforeAgentToolExecution.toolExecution().request().name());
        }

        @Override
        public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
            afterToolExecutionCalled.set(true);
            System.out.println("[TestListener] afterAgentToolExecution called: tool="
                    + afterAgentToolExecution.toolExecution().request().name());
        }

        public AtomicBoolean getBeforeInvocationCalled() {
            return beforeInvocationCalled;
        }

        public AtomicBoolean getAfterInvocationCalled() {
            return afterInvocationCalled;
        }

        public AtomicBoolean getBeforeToolExecutionCalled() {
            return beforeToolExecutionCalled;
        }

        public AtomicBoolean getAfterToolExecutionCalled() {
            return afterToolExecutionCalled;
        }
    }

    /**
     * Mock ChatModel，模拟 LLM 返回工具调用
     */
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

    /**
     * 测试方法：复现 beforeAgentInvocation 未被调用的问题
     */
    @Test
    public void testBeforeAgentInvocationNotCalled() {
        // Given: 创建一个会触发工具调用的 Agent
        MockToolCallingChatModel mockChatModel = new MockToolCallingChatModel("toUpperCase");
        TestAgentListener listener = new TestAgentListener();

        SimpleAgent agent = AgenticServices
                .agentBuilder(SimpleAgent.class)
                .chatModel(mockChatModel)
                .tools(new TestTool())
                .listener(listener)
                .build();

        // When: 调用 Agent 方法
        String result = agent.execute("hello");

        // Then: 验证 Tool 执行回调被调用了
        assertThat(listener.getBeforeToolExecutionCalled().get())
                .as("beforeAgentToolExecution should be called")
                .isTrue();
        assertThat(listener.getAfterToolExecutionCalled().get())
                .as("afterAgentToolExecution should be called")
                .isTrue();

        // But: 验证 Agent 调用回调没有被调用（这是 Bug！）
        assertThat(listener.getBeforeInvocationCalled().get())
                .as("beforeAgentInvocation should be called (BUG: this is NOT called!)")
                .isTrue();
        assertThat(listener.getAfterInvocationCalled().get())
                .as("afterAgentInvocation should be called (BUG: this is NOT called!)")
                .isTrue();

        System.out.println("Result: " + result);
    }
}
