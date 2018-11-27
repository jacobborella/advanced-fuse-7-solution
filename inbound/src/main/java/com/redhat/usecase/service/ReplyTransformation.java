package com.redhat.usecase.service;

import java.io.StringWriter;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.camel.Exchange;

import com.customer.app.response.ESBResponse;

public class ReplyTransformation {
    public String generateAnswer(Exchange exchange) throws Exception {


        String uuid = (String) exchange.getExchangeId();
        boolean published = "DONE".equals(exchange.getIn().getBody());
        ESBResponse esbResponse = new ESBResponse();
        esbResponse.setBusinessKey(uuid);
        esbResponse.setPublished(published);
        if(published) {
            esbResponse.setComment("DONE");
        } else {
            esbResponse.setComment((String) exchange.getIn().getBody());
        }

        //generate String
        JAXBContext context = JAXBContext.newInstance(ESBResponse.class);
        Marshaller m = context.createMarshaller();

        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // To format XML

        StringWriter sw = new StringWriter();
        m.marshal(esbResponse, sw);
        return sw.toString();    
        }
}