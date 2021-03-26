package com.whiskels.notifier.telegram.handler.impl;

import com.whiskels.notifier.telegram.annotations.BotCommand;
import com.whiskels.notifier.telegram.domain.User;
import com.whiskels.notifier.telegram.handler.AbstractUserHandler;
import lombok.extern.slf4j.Slf4j;

import static com.whiskels.notifier.common.ParsingUtil.extractArguments;
import static com.whiskels.notifier.telegram.builder.MessageBuilder.create;
import static com.whiskels.notifier.telegram.domain.Role.ADMIN;

/**
 * Allows bot admin to change user name by sending bot a chat command
 * Syntax: /ADMIN_NAME userId name
 * <p>
 * Available to: Admin
 */
@Slf4j
@BotCommand(command = "/ADMIN_NAME", requiredRoles = {ADMIN})
public class AdminUpdateNameHandler extends AbstractUserHandler {
    @Override
    protected void handle(User admin, String message) {
        log.debug("Preparing /ADMIN_NAME");
        final String arguments = extractArguments(message);
        final int userId = Integer.parseInt(arguments.substring(0, arguments.indexOf(" ")));

        final User toUpdate = userService.get(userId).orElse(null);

        if (toUpdate != null) {
            toUpdate.setName(extractArguments(arguments));
            userService.update(toUpdate);

            publish(create(admin)
                    .line("Updated user: %s", toUpdate.toString())
                    .build());
        } else {
            publish(create(admin)
                    .line("Couldn't find user: %d", userId)
                    .build());
        }
    }
}
