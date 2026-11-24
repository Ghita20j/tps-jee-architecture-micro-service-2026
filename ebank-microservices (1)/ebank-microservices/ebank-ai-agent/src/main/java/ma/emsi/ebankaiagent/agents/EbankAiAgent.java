package ma.emsi.ebankaiagent.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

/**
 * Agent IA base sur Spring AI.
 * - Les tools viennent des micro-services via MCP (ToolCallbackProvider injecte par le client MCP).
 * - La memoire est isolee par CONVERSATION_ID (Spring Boot 4 / Spring AI recent).
 */
@Service
public class EbankAiAgent {

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant virtuel de la banque EBank.
            Tu aides les utilisateurs a consulter les clients et les comptes bancaires,
            et a effectuer des operations (versement, retrait, virement, creation de compte).
            Regles :
            - Utilise TOUJOURS les outils disponibles pour obtenir des donnees reelles ; n'invente jamais de chiffres.
            - Si une information manque (ex : identifiant de compte, montant), demande-la.
            - Avant une operation qui modifie un solde, rappelle le compte et le montant concernes.
            - Reponds dans la langue de l'utilisateur (francais, darija, arabe ou anglais), de facon claire et concise.
            - Presente les listes sous forme de puces lisibles.
            """;

    private final ChatClient chatClient;

    public EbankAiAgent(ChatClient.Builder chatClient, ToolCallbackProvider tools, ChatMemory chatMemory) {
        this.chatClient = chatClient
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** Version de l'enonce (conversation unique "C1"). */
    public String chat(String query) {
        return chat("C1", query);
    }

    /** Une conversation par utilisateur (chat Telegram, salon Discord, client REST...). */
    public String chat(String conversationId, String query) {
        return chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
