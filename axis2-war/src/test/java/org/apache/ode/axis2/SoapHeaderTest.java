package org.apache.ode.axis2;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.util.Utils;
import org.apache.ode.utils.DOMUtils;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * Tests a simple process that gets a message that includes a header, pass it on when invoking
 * a dummy service, gets the changed header back and returns it.
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SoapHeaderTest extends Axis2TestBase {
    
    public SoapHeaderTest(String name) {
        super(name);
    }


  @Test
    public void testSimplePassing() throws Exception {
        server.deployService("TestSoapHeader", "dummy-service.wsdl",
                new QName("http://axis2.ode.apache.org", "DummyService"), "DummyServiceSOAP11port_http", 
                new MessageReceiver() {
            public void receive(MessageContext messageCtx) throws AxisFault {
                System.out.println(messageCtx.getEnvelope());
                OMElement cidElmt = messageCtx.getEnvelope().getHeader().getFirstElement();
                // Also checking if the session is included in passing
                assertTrue(messageCtx.getEnvelope().toString().indexOf("session") > 0);
                
                assertEquals("ConversationId", cidElmt.getLocalName());
                assertEquals("ZZZXYZ", cidElmt.getText());

                MessageContext outMsgContext = Utils.createOutMessageContext(messageCtx);
                outMsgContext.getOperationContext().addMessageContext(outMsgContext);

                // Far too many lines of code...
                SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
                SOAPEnvelope env = factory.createSOAPEnvelope();
                SOAPHeader header = factory.createSOAPHeader(env);
                SOAPBody body = factory.createSOAPBody(env);
                outMsgContext.setEnvelope(env);
                OMElement respElmt = factory.createOMElement(new QName("http://axis2.ode.apache.org", "faultTestResponse"));
                body.addChild(respElmt);
                respElmt.setText("dummy");
                SOAPHeaderBlock headerBlock = factory.createSOAPHeaderBlock("ConversationId",
                        factory.createOMNamespace("http://my.company/super/protocol", "pns"), header);
                headerBlock.setText("ZZYV");
                AxisEngine.send(outMsgContext);
            }
        });

        if (!server.isDeployed("TestSoapHeader")) server.deployProcess("TestSoapHeader");

        String response = server.sendRequestFile("http://localhost:8888/processes/headerTest",
                "TestSoapHeader", "testRequest.soap");
 
        Element rootElemt = DOMUtils.stringToDOM(response);
        Element cidElemt = DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(rootElemt));
        assertEquals("ConversationId", cidElemt.getLocalName());
        assertEquals("ZZYV", cidElemt.getTextContent());

        server.undeployProcess("TestSoapHeader");
    }
    
}
