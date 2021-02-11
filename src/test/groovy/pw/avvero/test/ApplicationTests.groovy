package pw.avvero.test

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [Application])
class ApplicationTests extends Specification {

    def "Application could be launched"() {
        expect:
        1 == 1
    }

}
