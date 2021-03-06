package com.whiskels.notifier.telegram.handler.impl;

import com.whiskels.notifier.telegram.annotation.BotCommand;
import com.whiskels.notifier.telegram.annotation.Schedulable;
import com.whiskels.notifier.telegram.builder.MessageBuilder;
import com.whiskels.notifier.telegram.domain.Schedule;
import com.whiskels.notifier.telegram.domain.User;
import com.whiskels.notifier.telegram.handler.AbstractScheduleHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.util.List;
import java.util.stream.Collectors;

import static com.whiskels.notifier.telegram.Command.*;
import static com.whiskels.notifier.telegram.builder.MessageBuilder.builder;
import static com.whiskels.notifier.telegram.domain.Role.*;
import static com.whiskels.notifier.telegram.util.ParsingUtil.extractArguments;
import static com.whiskels.notifier.telegram.util.ParsingUtil.getTime;

/**
 * Adds schedule to the user and shows current schedule times
 * <p>
 * Available to: HR, Manager, Head, Admin
 */
@Slf4j
@BotCommand(command = SCHEDULE, requiredRoles = {HR, MANAGER, HEAD, ADMIN})
@ConditionalOnBean(annotation = Schedulable.class)
@RequiredArgsConstructor
public class ScheduleAddHandler extends AbstractScheduleHandler {
    @Value("${telegram.bot.schedule.empty:Empty}")
    private String emptySchedule;

    @Override
    protected void handle(User user, String message) {
        if (!message.contains(" ")) {
            inlineKeyboardMessage(user);
            return;
        }


        MessageBuilder builder = builder(user);
        try {
            List<Integer> time = getTime(extractArguments(message));
            final int hours = time.get(0);
            final int minutes = time.get(1);

            log.debug("Adding schedule {}:{} to {}", hours, minutes, user.getChatId());

            scheduleService.addSchedule(new Schedule(hours, minutes, null), user.id());
            builder.line("Scheduled status messages to")
                    .line("be sent daily at *%02d:%02d*", hours, minutes);
        } catch (Exception e) {
            builder.line("You've entered invalid time")
                    .line("Please try again");
            log.debug("Incorrect schedule time {}", message);
        }

        publish(builder.build());
    }

    /**
     * Returns predefined list of options and information on empty command
     */
    private void inlineKeyboardMessage(User user) {
        String currentSchedule = emptySchedule;

        final List<Schedule> schedule = scheduleService.getSchedule(user.getChatId());
        if (schedule != null && !schedule.isEmpty()) {
            currentSchedule = schedule.stream()
                    .map(e -> String.format("%02d:%02d", e.getHour(), e.getMinutes()))
                    .collect(Collectors.joining(", "));
        }

        publish(builder(user)
                .line("*Your current schedule:*")
                .line(currentSchedule)
                .line()
                .line("Choose from available options or add preferred time to [/schedule](/schedule) command:")
                .row()
                .buttonWithArguments("9:00", SCHEDULE)
                .buttonWithArguments("12:00", SCHEDULE)
                .buttonWithArguments("15:00", SCHEDULE)
                .row()
                .button("Clear schedule", SCHEDULE_CLEAR)
                .button("Help", SCHEDULE_HELP)
                .build());
    }
}
