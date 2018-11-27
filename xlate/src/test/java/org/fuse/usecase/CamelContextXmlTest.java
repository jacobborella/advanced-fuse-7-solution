package org.fuse.usecase;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Before;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelContextXmlTest extends CamelSpringTestSupport {
    private static String PERSON_REQ_OK =
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
         "<p:Person xmlns:p=\"http://www.app.customer.com\"" +
         "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
         "  xsi:schemaLocation=\"http://www.app.customer.com PatientDemographics.xsd \">" +
         "  <p:age>30</p:age>" +
         "  <p:legalname>" +
         "    <p:given>First</p:given>" +
         "    <p:family>Last</p:family>" +
         "  </p:legalname>" +
         "  <p:fathername>Dad</p:fathername>" +
         "  <p:mothername>Mom</p:mothername>" +
         "  <p:gender xsi:type=\"p:Code\">" +
         "    <p:code>Male</p:code>" +
         "  </p:gender>" +
         "</p:Person>";
         private static String PERSON_REQ_INVALID_XML =
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
         "<p:Person xmlns:p=\"http://www.app.customer.com\"" +
         "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
         "  xsi:schemaLocation=\"http://www.app.customer.com PatientDemographics.xsd \">" +
         "  <p:age>30</p:age>" +
         "  <p:legalname>" +
         "    <p:given>First</p:given>" +
         "    <p:family>Last</p:family>" +
         "    <p:foo>notok</p:foo>" +
         "  </p:legalname>" +
         "  <p:fathername>Dad</p:fathername>" +
         "  <p:mothername>Mom</p:mothername>" +
         "  <p:gender xsi:type=\"p:Code\">" +
         "    <p:code>Male</p:code>" +
         "  </p:gender>" +
         "</p:Person>";
         private String NOT_XML = "foo";

	@Produce(uri = "direct:input")
	protected ProducerTemplate inputEndpoint;

	@EndpointInject(uri = "mock:file:todo/out")
    private MockEndpoint mockAccountQueue;
	@EndpointInject(uri = "mock:file:todo/dlq")
    private MockEndpoint mockAccountQueue2;

    static {
        System.setProperty("spring.profiles.active", "test");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
			
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:input");
                mockEndpointsAndSkip("file:*");
            }
        });
    }

    
    @Test
    public void testInvalidXML() throws Exception {
        Exchange ex = new DefaultExchange(context);
        ex.getIn().setBody(PERSON_REQ_OK);
        mockAccountQueue.setExpectedCount(1);
        mockAccountQueue2.setExpectedCount(0);
        inputEndpoint.send(ex);
        mockAccountQueue.assertIsSatisfied();
        mockAccountQueue2.assertIsSatisfied();
    }

	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring/camel-context.xml");
		return ctx; 
	}
}