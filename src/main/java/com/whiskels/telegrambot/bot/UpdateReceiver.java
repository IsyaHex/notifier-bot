package com.whiskels.telegrambot.bot;

import com.whiskels.telegrambot.bot.handler.AbstractBaseHandler;
import com.whiskels.telegrambot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.whiskels.telegrambot.util.TelegramUtil.extractCommand;

/**
 * Main class used to handle incoming Updates.
 * Chooses suitable inheritor of AbstractBaseHandler to handle the input
 */
@Component
@Slf4j
public class UpdateReceiver {
    private final List<AbstractBaseHandler> handlers;
    private final UserService userService;

    public UpdateReceiver(List<AbstractBaseHandler> handlers, UserService userService) {
        this.handlers = handlers;
        this.userService = userService;
    }

    /**
     * Analyzes received update and chooses correct handler if possible
     *
     * @param update
     * @return list of SendMessages to execute
     */
    public List<PartialBotApiMethod<? extends Serializable>> handle(Update update) {
        try {
            int userId = 0;
            String text = null;

            if (isMessageWithText(update)) {
                final Message message = update.getMessage();
                userId = message.getFrom().getId();
                text = message.getText();
            } else if (update.hasCallbackQuery()) {
                final CallbackQuery callbackQuery = update.getCallbackQuery();
                userId = callbackQuery.getFrom().getId();
                text = callbackQuery.getData();
            }

            if (text != null && userId != 0) {
                return getHandler(text).authenticateAndHandle(userService.get(userId), text);
            }

            throw new UnsupportedOperationException("Operation not supported");
        } catch (Exception e) {
            log.debug("Exception: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Selects handler which can handle received command
     *
     * @param text content of received message
     * @return handler suitable for command
     */
    private AbstractBaseHandler getHandler(String text) {
        return handlers.stream()
                .filter(h -> h.getClass()
                        .isAnnotationPresent(BotCommand.class))
                .filter(h -> Stream.of(h.getClass()
                        .getAnnotation(BotCommand.class)
                        .command())
                        .anyMatch(c -> c.equalsIgnoreCase(extractCommand(text))))
                .findAny()
                .orElseThrow(UnsupportedOperationException::new);
    }

    private boolean isMessageWithText(Update update) {
        return !update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText();
    }
}