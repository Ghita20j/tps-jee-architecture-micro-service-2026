package ma.emsi.ebankaiagent.telegram;

import ma.emsi.ebankaiagent.agents.EbankAiAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/** Client Telegram du chatbot (long polling). Active avec telegram.bot.enabled=true */
@Component
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private static final int MAX_LENGTH = 4000; // limite Telegram : 4096

    private final EbankAiAgent agent;
    private final String username;

    public TelegramBot(@Value("${telegram.bot.token}") String token,
                       @Value("${telegram.bot.username}") String username,
                       EbankAiAgent agent) {
        super(token);
        this.username = username;
        this.agent = agent;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText();

        if (text.equals("/start")) {
            send(chatId, "Bonjour ! Je suis l'assistant EBank. Posez-moi une question sur vos clients ou vos comptes.");
            return;
        }
        try {
            execute(new SendChatAction(chatId, ActionType.TYPING.toString()));
            String answer = agent.chat("telegram-" + chatId, text);
            send(chatId, answer);
        } catch (Exception e) {
            log.error("Erreur agent", e);
            send(chatId, "Desole, une erreur est survenue : " + e.getMessage());
        }
    }

    private void send(String chatId, String text) {
        if (text == null || text.isBlank()) text = "(reponse vide)";
        for (int i = 0; i < text.length(); i += MAX_LENGTH) {
            String part = text.substring(i, Math.min(text.length(), i + MAX_LENGTH));
            try {
                execute(new SendMessage(chatId, part));
            } catch (TelegramApiException e) {
                log.error("Envoi Telegram impossible", e);
            }
        }
    }
}
