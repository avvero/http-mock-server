package pw.avvero.test;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class HttpMockServiceBuilder {

    public static MockRestServiceServer build(RestTemplate restTemplate) {
        return MockRestServiceServer.bindTo(restTemplate)
                .ignoreExpectOrder(true)
                .build(new CustomRequestExpectationManager());
    }

}
