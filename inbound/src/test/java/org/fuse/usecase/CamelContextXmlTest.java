package org.fuse.usecase;


import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Before;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;
import javax.jms.JMSException;
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
    private static String PERSON_REQ_OK_SPEC_CHAR =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<p:Person xmlns:p=\"http://www.app.customer.com\"" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            "  xsi:schemaLocation=\"http://www.app.customer.com PatientDemographics.xsd \">" +
            "  <p:age>30</p:age>" +
            "  <p:legalname>" +
            "    <p:given>åæøö</p:given>" +
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

	@EndpointInject(uri = "mock:amqp:queue:q.empi.deim.in")
    private MockEndpoint mockAccountQueue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
			
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:input");
                mockEndpointsAndSkip("amqp:*");
            }
        });
    }

    /**
     * This is mostly here to show different styles of
     * testing. I would prefer to also verify the response
     * as done in testPersonRequestOk2().
     * @throws Exception
     */
    @Test
	public void testPersonRequestOk() throws Exception {
    	mockAccountQueue.setExpectedCount(1);
    	
    	inputEndpoint.sendBody(PERSON_REQ_OK);
    	
        mockAccountQueue.assertIsSatisfied();
    }

    /**
     * Verify that an ok message is verified and sent
     * to the internal queue. Also verify response and
     * http codes.
     * @throws Exception
     */
    @Test
    public void testPersonRequestOk2() throws Exception {
        Exchange ex = new DefaultExchange(context);
        ex.getIn().setBody(PERSON_REQ_OK);
        mockAccountQueue.setExpectedCount(1);
        inputEndpoint.send(ex);
        assertStringContains(ex.getIn().getBody().toString(), "<Comment>DONE</Comment>");
        assertEquals("200", ex.getIn().getHeader("CamelHttpResponseCode"));
        mockAccountQueue.assertIsSatisfied();
    }

    /**
     * As testPersonRequestOk2, but with special characters.
     * @throws Exception
     */
    @Test
    public void testPersonRequestOkSpecialCharacters() throws Exception {
        Exchange ex = new DefaultExchange(context);
        ex.getIn().setBody(PERSON_REQ_OK_SPEC_CHAR);
        mockAccountQueue.setExpectedCount(1);
        inputEndpoint.send(ex);
        assertStringContains(ex.getIn().getBody().toString(), "<Comment>DONE</Comment>");
        assertEquals("200", ex.getIn().getHeader("CamelHttpResponseCode"));
        mockAccountQueue.assertIsSatisfied();
    }

    /**
     * Simulate that amq is down and that a proper response
     * is sent to the client.
     * 
     * @throws Exception
     */
    @Test
	public void testAMQDown() throws Exception {
    	mockAccountQueue.whenAnyExchangeReceived(new Processor(){
    		//no matter which message received throw a JMSException
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new JMSException("No connection to queue.");
            }
        });
        Exchange ex = new DefaultExchange(context);
        ex.getIn().setBody(PERSON_REQ_OK);
    	
        inputEndpoint.send(ex);
        assertStringContains(ex.getIn().getBody().toString(), "<Comment>Delivery of message failed.</Comment>");
        assertEquals("500", ex.getIn().getHeader("CamelHttpResponseCode"));
    }
    
    /**
     * Verify that xml validation is done by passing
     * a request, which contains illegal content compared
     * to the xsd. Verify the response.
     * 
     * Furthermore ensure that no data is written to the
     * internal queue. 
     * 
     * @throws Exception
     */
    @Test
    public void testInvalidXML() throws Exception {
        Exchange ex = new DefaultExchange(context);
        ex.getIn().setBody(PERSON_REQ_INVALID_XML);
        mockAccountQueue.setExpectedCount(0);
        inputEndpoint.send(ex);
        assertStringContains(ex.getIn().getBody().toString(), "<Comment>Delivery failed because xml format was invalid.</Comment>");
        assertEquals("400", ex.getIn().getHeader("CamelHttpResponseCode"));
        mockAccountQueue.assertIsSatisfied();
    }

    /**
     * Verify that pssing content, which isn't xml is
     * handled by the xml validator. Verify the response
     * 
     * Furthermore ensure that no data is written to the
     * internal queue.
     * 
     * @throws Exception
     */
    @Test
    public void testNotXML() throws Exception {
        Exchange ex = new DefaultExchange(context);
        ex.getIn().setBody(NOT_XML);
        mockAccountQueue.setExpectedCount(0);
        inputEndpoint.send(ex);
        assertStringContains(ex.getIn().getBody().toString(), "<Comment>Delivery failed because xml format was invalid.</Comment>");
        assertEquals("400", ex.getIn().getHeader("CamelHttpResponseCode"));
        mockAccountQueue.assertIsSatisfied();
    }

	@Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring/camel-context.xml");
		return ctx; 
	}
	
	

}
