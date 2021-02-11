package pw.avvero.test;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.test.web.client.*;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

public class CustomRequestExpectationManager implements RequestExpectationManager {
    private final List<ClientHttpRequest> requests = new ArrayList<>();
    private final Map<ClientHttpRequest, Throwable> requestFailures = new LinkedHashMap<>();
    private final RequestExpectationGroup remainingExpectations = new RequestExpectationGroup();

    @Override
    public ResponseActions expectRequest(ExpectedCount count, RequestMatcher matcher) {
//        Assert.state(this.requests.isEmpty(), "Cannot add more expectations after actual requests are made");
        RequestExpectation expectation = new DefaultRequestExpectation(count, matcher);
        this.remainingExpectations.getExpectations().add(expectation);
        return expectation;
    }

    @Override
    public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
        RequestExpectation expectation;
        synchronized (this.requests) {
            try {
                // Try this first for backwards compatibility
                ClientHttpResponse response = validateRequestInternal(request);
                if (response != null) {
                    return response;
                } else {
                    expectation = matchRequest(request);
                }
            } catch (Throwable ex) {
                this.requestFailures.put(request, ex);
                throw ex;
            } finally {
                this.requests.add(request);
            }
        }
        return expectation.createResponse(request);
    }

    /**
     * Subclasses must implement the actual validation of the request
     * matching to declared expectations.
     *
     * @deprecated as of 5.0.3, subclasses should implement {@link #matchRequest(ClientHttpRequest)}
     * instead and return only the matched expectation, leaving the call to create the response
     * as a separate step (to be invoked by this class).
     */
    @Deprecated
    @Nullable
    protected ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
        return null;
    }

    @Override
    public void verify() {
        if (this.remainingExpectations.getExpectations().isEmpty()) {
            return;
        }
        int count = 0;
        for (RequestExpectation expectation : this.remainingExpectations.getExpectations()) {
            if (!expectation.isSatisfied()) {
                count++;
            }
        }
        if (count > 0) {
            String message = "Further request(s) expected leaving " + count + " unsatisfied expectation(s).\n";
            throw new AssertionError(message + getRequestDetails());
        }
        if (!this.requestFailures.isEmpty()) {
            throw new AssertionError("Some requests did not execute successfully.\n" +
                    this.requestFailures.entrySet().stream()
                            .map(entry -> "Failed request:\n" + entry.getKey() + "\n" + entry.getValue())
                            .collect(Collectors.joining("\n", "\n", "")));
        }
    }

    /**
     * Return details of executed requests.
     */
    protected String getRequestDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.requests.size()).append(" request(s) executed");
        if (!this.requests.isEmpty()) {
            sb.append(":\n");
            for (ClientHttpRequest request : this.requests) {
                sb.append(request.toString()).append("\n");
            }
        } else {
            sb.append(".\n");
        }
        return sb.toString();
    }

    @Override
    public void reset() {
        this.requests.clear();
        this.requestFailures.clear();
        this.remainingExpectations.reset();
    }

    public RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
        RequestExpectation expectation = this.remainingExpectations.findExpectation(request);
        if (expectation == null) {
            RequestExpectation notFoundExpectation = new DefaultRequestExpectation(never(), requestTo(request.getURI()));
            notFoundExpectation.andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));
            return notFoundExpectation;
        }
        this.remainingExpectations.update(expectation);
        return expectation;
    }
}
