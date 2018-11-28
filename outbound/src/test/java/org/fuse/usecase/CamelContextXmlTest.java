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
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelContextXmlTest extends CamelSpringTestSupport {
	static {
		System.setProperty("spring.profiles.active", "test");
	}

	@Produce(uri = "direct:input")
	protected ProducerTemplate inputEndpoint;

	@EndpointInject(uri = "mock:file:dlq")
    private MockEndpoint deadLetterQueue;

	@EndpointInject(uri = "mock:file:localhost")
    private MockEndpoint httpEndpoint;

	@Before
    public void setUp() throws Exception {
        super.setUp();
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
			
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:input");
            }
        });
        context.getRouteDefinitions().get(1).adviceWith(context, new AdviceWithRouteBuilder() {
			
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("file:*");
            }
        });
    }

	/**
	 * Verify that the message is written to the deadletter
	 * queue if the http call fail.
	 * @throws Exception
	 */
    @Test
	public void testHttpTimeout() throws Exception {
    	deadLetterQueue.setExpectedCount(1);
    	
    	httpEndpoint.whenAnyExchangeReceived(new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
				throw new java.net.ConnectException("timeout!");
				
			}
		});
    	
    	inputEndpoint.sendBody("somecontent");
    	
    	deadLetterQueue.assertIsSatisfied();
    }
    
    /**
     * Verify that http handling is ok is the http call
     * only fail once.
	 * @throws Exception
	 */
    @Test
	public void testHttpTimeout1() throws Exception {
    	deadLetterQueue.setExpectedCount(0);
        Exchange ex = new DefaultExchange(context);
        ex.getIn().setBody("somedata");
    	
        //fail the first http call
    	httpEndpoint.whenExchangeReceived(0, new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
					throw new java.net.ConnectException("timeout!");
			}
		});
    	
    	//ping a reply on the second http call
    	httpEndpoint.whenExchangeReceived(1, new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
			    String s = exchange.getIn().getBody().toString();
			    exchange.getIn().setBody(s+"_http");
			}
		});

        inputEndpoint.send(ex);
        
        assertEquals("somedata_http", ex.getIn().getBody().toString());
    	
    	deadLetterQueue.assertIsSatisfied();
    }

    @Override
	protected ClassPathXmlApplicationContext createApplicationContext() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring/camel-context.xml");
		return ctx; 
	}
	
	

}
