package ma.emsi.ebankaiagent.web;

import ma.emsi.ebankaiagent.agents.EbankAiAgent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin("*")
public class AgentRestController {

    public record ChatRequest(String conversationId, String message) {}
    public record ChatResponse(String conversationId, String answer) {}

    private final EbankAiAgent agent;
    private final ToolCallbackProvider tools;

    public AgentRestController(EbankAiAgent agent, ToolCallbackProvider tools) {
        this.agent = agent;
        this.tools = tools;
    }

    /** GET http://localhost:8070/chat?query=Donne moi la liste des clients */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestParam String query,
                       @RequestParam(defaultValue = "C1") String conversationId) {
        return agent.chat(conversationId, query);
    }

    /** POST http://localhost:8070/api/chat  {"conversationId":"u1","message":"..."} */
    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String id = request.conversationId() == null ? "C1" : request.conversationId();
        return new ChatResponse(id, agent.chat(id, request.message()));
    }

    /** Liste des tools MCP decouverts : GET http://localhost:8070/tools */
    @GetMapping("/tools")
    public List<String> tools() {
        return Arrays.stream(tools.getToolCallbacks())
                .map(t -> t.getToolDefinition().name() + " : " + t.getToolDefinition().description())
                .toList();
    }
}
