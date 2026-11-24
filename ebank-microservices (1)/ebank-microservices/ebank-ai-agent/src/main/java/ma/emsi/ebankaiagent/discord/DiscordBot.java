package ma.emsi.ebankaiagent.discord;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import ma.emsi.ebankaiagent.agents.EbankAiAgent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Client Discord du chatbot (JDA). Active avec discord.bot.enabled=true
 * Le bot repond : en message prive, ou dans un salon quand il est mentionne (@EBankBot ...).
 */
@Component
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
public class DiscordBot extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DiscordBot.class);
    private static final int MAX_LENGTH = 1900; // limite Discord : 2000

    private final EbankAiAgent agent;
    private final String token;
    private JDA jda;

    public DiscordBot(@Value("${discord.bot.token}") String token, EbankAiAgent agent) {
        this.token = token;
        this.agent = agent;
    }

    @PostConstruct
    public void start() {
        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();
        log.info("Bot Discord demarre");
    }

    @PreDestroy
    public void stop() {
        if (jda != null) jda.shutdown();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        boolean isPrivate = event.isFromType(ChannelType.PRIVATE);
        boolean mentioned = event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser());
        if (!isPrivate && !mentioned) return;

        String query = event.getMessage().getContentRaw()
                .replaceAll("<@!?\\d+>", "")
                .trim();
        if (query.isEmpty()) return;

        String conversationId = "discord-" + event.getChannel().getId();
        event.getChannel().sendTyping().queue();

        // L'appel au LLM est bloquant : on ne bloque pas le thread d'evenements de JDA
        CompletableFuture.supplyAsync(() -> agent.chat(conversationId, query))
                .exceptionally(e -> {
                    log.error("Erreur agent", e);
                    return "Desole, une erreur est survenue : " + e.getMessage();
                })
                .thenAccept(answer -> {
                    String text = (answer == null || answer.isBlank()) ? "(reponse vide)" : answer;
                    for (int i = 0; i < text.length(); i += MAX_LENGTH) {
                        event.getChannel().sendMessage(text.substring(i, Math.min(text.length(), i + MAX_LENGTH))).queue();
                    }
                });
    }
}
