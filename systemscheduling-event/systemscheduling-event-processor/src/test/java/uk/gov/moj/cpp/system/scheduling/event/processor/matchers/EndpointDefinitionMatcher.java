package uk.gov.moj.cpp.system.scheduling.event.processor.matchers;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.clients.core.EndpointDefinition;
import uk.gov.justice.services.clients.core.QueryParam;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;


public class EndpointDefinitionMatcher extends TypeSafeDiagnosingMatcher<EndpointDefinition> {

    private final EndpointDefinition endpointDefinition;

    public static EndpointDefinitionMatcher matchEndpointDefinition(final EndpointDefinition endpointDefinition) {
        return new EndpointDefinitionMatcher(endpointDefinition);
    }

    private EndpointDefinitionMatcher(final EndpointDefinition endpointDefinition) {
        this.endpointDefinition = endpointDefinition;
    }

    @Override
    protected boolean matchesSafely(final EndpointDefinition endpointDefinition, final Description description) {
        describe(endpointDefinition, description);
        return Objects.equals(endpointDefinition.getBaseUri(), this.endpointDefinition.getBaseUri()) &&
                Objects.equals(endpointDefinition.getPath(), this.endpointDefinition.getPath()) &&
                Objects.equals(endpointDefinition.getPathParams(), this.endpointDefinition.getPathParams()) &&
                Objects.equals(endpointDefinition.getResponseMediaType(), this.endpointDefinition.getResponseMediaType()) &&
                equals(endpointDefinition.getQueryParams(), this.endpointDefinition.getQueryParams());

    }


    private boolean equals(final Collection<QueryParam> queryParams1, final Collection<QueryParam> queryParams2) {
        final Comparator<QueryParam> queryParamComparator = comparing(QueryParam::getName).thenComparing(QueryParam::getType).thenComparing(QueryParam::isRequired);
        final List<QueryParam> l1 = queryParams1.stream().sorted(queryParamComparator).collect(toList());
        final List<QueryParam> l2 = queryParams2.stream().sorted(queryParamComparator).collect(toList());

        if (l1.size() != l2.size()) {
            return false;
        }
        return IntStream.range(0, l1.size()).allMatch(i -> queryParamComparator.compare(l1.get(i), l2.get(i)) == 0);
    }

    @Override
    public void describeTo(final Description description) {
        describe(endpointDefinition, description);
    }

    private void describe(final EndpointDefinition endpointDefinition, final Description description) {
        description
                .appendText("\nEndpointDefinition:")
                .appendText("\n\tbaseUri=").appendValue(endpointDefinition.getBaseUri())
                .appendText("\n\tpath=").appendValue(endpointDefinition.getPath())
                .appendText("\n\tpathParams=").appendValue(endpointDefinition.getPathParams())
                .appendText("\n\tqueryParams=").appendValue(endpointDefinition.getQueryParams())
                .appendText("\n\tresponseMediaType=").appendValue(endpointDefinition.getResponseMediaType());
    }

}
