package com.whiskels.notifier.telegram.orchestrator;

import com.whiskels.notifier.external.Supplier;
import com.whiskels.notifier.external.json.employee.Employee;
import com.whiskels.notifier.telegram.SendMessagePublisherTest;
import com.whiskels.notifier.telegram.domain.Role;
import com.whiskels.notifier.telegram.domain.User;
import com.whiskels.notifier.telegram.handler.impl.EmployeeEventHandler;
import com.whiskels.notifier.telegram.security.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static com.whiskels.notifier.MockedClockConfiguration.EXPECTED_DATE;
import static com.whiskels.notifier.external.json.EmployeeTestData.employeeNullBirthday;
import static com.whiskels.notifier.external.json.EmployeeTestData.employeeWorking;
import static com.whiskels.notifier.telegram.UserTestData.USER_1;
import static com.whiskels.notifier.telegram.domain.Role.ADMIN;
import static com.whiskels.notifier.telegram.domain.Role.HR;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {SchedulableHandlerOrchestrator.class, AuthorizationService.class
})
@Import(SchedulableHandlerOrchestratorTest.SchedulableBeanConfig.class)
class SchedulableHandlerOrchestratorTest extends SendMessagePublisherTest {
    private static final String EXPECTED_SCHEDULE_MESSAGE = format("*Employee events on 22-01-2014*%n%n*Birthdays:*%nJason Bourne 01.01%n%n*Work anniversaries:*%nNobody%n%n");

    @Autowired
    SchedulableHandlerOrchestrator orchestrator;

    @Autowired
    EmployeeEventHandler employeeEventHandler;

    @Test
    void getSchedulableHandler() {
        assertEquals(employeeEventHandler, orchestrator.getSchedulableHandler(Set.of(HR)));
    }

    @Test
    void testOperateSchedule() {
        orchestrator.operate(new User(-1, -1, "Test HR", Set.of(HR), null));
        verifyPublishedMessage(-1, EXPECTED_SCHEDULE_MESSAGE);
    }

    @Test
    void getSchedulableHandler_withException() {
        Set<Role> unsupportedTestRoles = Set.of(ADMIN);
        assertThrows(UnsupportedOperationException.class, () -> orchestrator.getSchedulableHandler(unsupportedTestRoles));
    }

    @Test
    void testOperateSchedule_noAction() {
        orchestrator.operate(USER_1);
        verifyNoInteractions(publisher);
    }

    @TestConfiguration
    static class SchedulableBeanConfig {
        @Autowired
        AuthorizationService authorizationService;

        @Autowired
        ApplicationEventPublisher publisher;

        @Bean
        Supplier<Employee> employeeDataProvider() {
            Supplier<Employee> employeeSupplier = mock(Supplier.class);
            when(employeeSupplier.getData()).thenReturn(List.of(employeeWorking(), employeeNullBirthday()));
            when(employeeSupplier.lastUpdate()).thenReturn(EXPECTED_DATE);
            return employeeSupplier;
        }

        @Bean
        EmployeeEventHandler employeeEventHandler() {
            return new EmployeeEventHandler(employeeDataProvider());
        }
    }
}
