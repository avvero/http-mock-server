package pw.avvero.test.mock

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import pw.avvero.test.Application
import pw.avvero.test.HttpMockServiceBuilder
import spock.lang.Specification

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@SpringBootTest
@ContextConfiguration(classes = [Application])
class MockRestServiceServerTests extends Specification {

    @Autowired
    RestTemplate restTemplate

    def "Expectation for endpoint is provided"() {
        setup:
        def httpMock = HttpMockServiceBuilder.build(restTemplate)
        when: "expectation is not provided"
        restTemplate.exchange("endpoint", GET, null, Map)
        then:
        thrown(HttpClientErrorException.NotFound)
        when: "expectation is provided"
        httpMock.expect(requestTo("/endpoint"))
                .andExpect(method(GET))
                .andRespond(withSuccess('{"key":"value"}', APPLICATION_JSON))
        and:
        def response = restTemplate.exchange("endpoint", GET, null, Map)
        then:
        notThrown(HttpClientErrorException.NotFound)
        and:
        response.body["key"] == "value"
    }

}
