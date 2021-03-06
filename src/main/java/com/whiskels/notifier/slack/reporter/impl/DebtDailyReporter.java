package com.whiskels.notifier.slack.reporter.impl;

import com.whiskels.notifier.external.Supplier;
import com.whiskels.notifier.external.json.debt.Debt;
import com.whiskels.notifier.slack.reporter.SlackReporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.whiskels.notifier.common.datetime.DateTimeUtil.reportDate;
import static com.whiskels.notifier.common.util.FormatUtil.COLLECTOR_TWO_NEW_LINES;
import static com.whiskels.notifier.slack.reporter.builder.SlackPayloadBuilder.builder;

@Slf4j
@Component
@Profile("slack-common")
@ConditionalOnProperty("slack.customer.debt.webhook")
@ConditionalOnBean(value = Debt.class, parameterizedContainer = Supplier.class)
public class DebtDailyReporter extends SlackReporter<Debt> {
    @Value("${slack.customer.debt.header:Debt report on}")
    private String header;

    public DebtDailyReporter(@Value("${slack.customer.debt.webhook}") String webHook,
                             Supplier<Debt> provider,
                             ApplicationEventPublisher publisher) {
        super(webHook, publisher, provider);
    }

    @Scheduled(cron = "${slack.customer.debt.cron:0 0 13 * * MON-FRI}", zone = "${common.timezone}")
    public void report() {
        log.debug("Creating employee debt payload");
        publish(builder()
                .hook(webHook)
                .collector(COLLECTOR_TWO_NEW_LINES)
                .header(header + reportDate(provider.lastUpdate()))
                .notifyChannel()
                .block(provider.getData())
                .build());
    }
}
