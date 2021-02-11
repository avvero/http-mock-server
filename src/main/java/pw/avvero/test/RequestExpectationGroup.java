package pw.avvero.test;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.test.web.client.RequestExpectation;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Helper class to manage a group of remaining expectations.
 */
class RequestExpectationGroup {

    private final Set<RequestExpectation> expectations = new LinkedHashSet<>();

    public void addAllExpectations(Collection<RequestExpectation> expectations) {
        this.expectations.addAll(expectations);
    }

    public Set<RequestExpectation> getExpectations() {
        return this.expectations;
    }

    /**
     * Return a matching expectation, or {@code null} if none match.
     */
    @Nullable
    public RequestExpectation findExpectation(ClientHttpRequest request) throws IOException {
        for (RequestExpectation expectation : this.expectations) {
            try {
                expectation.match(request);
                return expectation;
            } catch (AssertionError error) {
                // We're looking to find a match or return null..
            }
        }
        return null;
    }

    /**
     * Invoke this for an expectation that has been matched.
     * <p>The count of the given expectation is incremented, then it is
     * either stored if remainingCount > 0 or removed otherwise.
     */
    public void update(RequestExpectation expectation) {
        expectation.incrementAndValidate();
        updateInternal(expectation);
    }

    private void updateInternal(RequestExpectation expectation) {
        if (expectation.hasRemainingCount()) {
            this.expectations.add(expectation);
        } else {
            this.expectations.remove(expectation);
        }
    }

    /**
     * Add expectations to this group.
     *
     * @deprecated as of 5.0.3, if favor of {@link #addAllExpectations}
     */
    @Deprecated
    public void updateAll(Collection<RequestExpectation> expectations) {
        expectations.forEach(this::updateInternal);
    }

    /**
     * Reset all expectations for this group.
     */
    public void reset() {
        this.expectations.clear();
    }
}
