package uk.gov.moj.cpp.system.scheduling.command.api.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.moj.cpp.accesscontrol.test.utils.matcher.OutcomeMatcher.outcome;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.accesscontrol.test.utils.matcher.OutcomeMatcher;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class AccessControlTest extends BaseDroolsAccessControlTest {

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("systemscheduling.command.schedule-job", List.of("System Users"), outcome().successful()),
                Arguments.of("systemscheduling.command.schedule-job", List.of("System Users", "Legal Adviser"), outcome().successful()),
                Arguments.of("systemscheduling.command.schedule-job", List.of("Other group"), outcome().failure()),
                Arguments.of("systemscheduling.command.cancel-job", List.of("System Users"), outcome().successful()),
                Arguments.of("systemscheduling.command.cancel-job", List.of("System Users", "Legal Adviser"), outcome().successful()),
                Arguments.of("systemscheduling.command.cancel-job", List.of("Other group"), outcome().failure()),
                Arguments.of("systemscheduling.command.not-defined", List.of("System Users"), outcome().failure())
        );
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public AccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @MethodSource("data")
    @ParameterizedTest
    public void shouldVerifyAccessRight(final String actionName, final List<String> groups, final OutcomeMatcher outcome) {
        final Action action = createActionFor(actionName);

        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(eq(action), ArgumentMatchers.<String>any()))
                .thenAnswer(invocationOnMock -> groups.contains(invocationOnMock.getArgument(1, String.class)));

        final ExecutionResults results = executeRulesWith(action);
        assertThat(results, CoreMatchers.is(outcome));
    }
}
