package org.apache.ode.bpel.engine;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.dao.MessageDAO;
import org.apache.ode.bpel.dao.MessageExchangeDAO;
import org.apache.ode.bpel.iapi.BpelEngineException;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.MyRoleMessageExchange;
import org.apache.ode.bpel.iapi.ProcessConf.CLEANUP_CATEGORY;
import org.apache.ode.bpel.rapi.PartnerLinkModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

abstract class MyRoleMessageExchangeImpl extends MessageExchangeImpl implements MyRoleMessageExchange {

    private static final Log __log = LogFactory.getLog(MyRoleMessageExchangeImpl.class);

    protected final QName _callee;
    protected CorrelationStatus _cstatus;
    protected String _clientId;

    public MyRoleMessageExchangeImpl(ODEProcess process, String mexId, PartnerLinkModel oplink,
                                     Operation operation, QName callee) {
        super(process, null, mexId, oplink, oplink != null ? oplink.getMyRolePortType() : null, operation);
        _callee = callee;
    }

    public CorrelationStatus getCorrelationStatus() {
        return _cstatus;
    }
  
    @Override
    void load(MessageExchangeDAO dao) {
        super.load(dao);
        _cstatus = dao.getCorrelationStatus() == null ? null : CorrelationStatus.valueOf(dao.getCorrelationStatus());
        _clientId = dao.getPartnersKey();
    }

    @Override
    public void save(MessageExchangeDAO dao) {
        super.save(dao);
        dao.setCorrelationStatus(_cstatus == null ? null : _cstatus.toString());
        dao.setPartnersKey(_clientId);
        dao.setCallee(_callee);
        
        if (_changes.contains(Change.REQUEST)) {
            _changes.remove(Change.REQUEST);
            MessageDAO requestDao = dao.createMessage(_request.getType());
            requestDao.setData(_request.getMessage());   
            requestDao.setHeader(_request.getHeader());   
            dao.setRequest(requestDao);
        }
        
    }

    public FailureType getFailureType() {
        if (getStatus() != Status.ACK || getAckType() != AckType.FAILURE)
            throw new IllegalStateException("MessageExchange did not fail!");
        
        return _failureType;
    }
    
    public String getClientId() {
        return _clientId;
    }

    public Future<Status> invokeAsync() {
        throw new BpelEngineException("Unsupported InvocationStyle");
    }

    public Status invokeBlocking() throws BpelEngineException, TimeoutException {
        throw new BpelEngineException("Unsupported InvocationStyle");
    }

    public void invokeReliable() {
        throw new BpelEngineException("Unsupported InvocationStyle");

    }

    public Status invokeTransacted() throws BpelEngineException {
        throw new BpelEngineException("Unsupported InvocationStyle");
    }

    public void setRequest(final Message request) {
        _request = (MessageImpl) request;
        _changes.add(Change.REQUEST);
    }

    public QName getServiceName() {
        return _callee;
    }

    public String toString() {
        try {
            return "{MyRoleMex#" + _mexId + " [Client " + _clientId + "] calling " + _callee + "." + getOperationName() + "(...)}";
        } catch (Throwable t) {
            return "{MyRoleMex#???}";
        }
    }

    public void complete() {
        // TODO Auto-generated method stub

    }

    public void release(boolean instanceSucceeded) {
        if( _process.isCleanupCategoryEnabled(instanceSucceeded, CLEANUP_CATEGORY.MESSAGES) ) {
            if(__log.isDebugEnabled()) __log.debug("Releasing mex " + getMessageExchangeId());
            _process.releaseMessageExchange(getMessageExchangeId());
        }
    }
 
    protected MessageExchangeDAO doInvoke() {
        if (getStatus() != Status.NEW) throw new IllegalStateException("Invalid state: " + getStatus());
        request();
        
        MessageExchangeDAO dao = _process.createMessageExchange(getMessageExchangeId(), MessageExchangeDAO.DIR_PARTNER_INVOKES_MYROLE);
        save(dao);
        if (__log.isDebugEnabled()) __log.debug("invoke() EPR= " + _epr + " ==> " + _process);
        try {
            _process.invokeProcess(dao);
        } finally {
            if (dao.getStatus() == Status.ACK) {
                _failureType = dao.getFailureType();
                _fault = dao.getFault();
                _explanation  = dao.getFaultExplanation();
                ack(dao.getAckType());
            }
        }
        return dao;
    }


    /**
     * Return a deep clone of the given message
     * 
     * @param message
     * @return
     */
    protected Message cloneMessage(Message message) {
        Message clone = createMessage(message.getType());
        clone.setMessage((Element) message.getMessage().cloneNode(true));
        Map<String, Node> headerParts = message.getHeaderParts();
        for (String partName : headerParts.keySet()) {
            clone.setHeaderPart(partName, (Element) headerParts.get(partName).cloneNode(true)); 
        }
        Map<String, Node> parts = message.getHeaderParts();
        for (String partName : parts.keySet()) {
            clone.setHeaderPart(partName, (Element) parts.get(partName).cloneNode(true)); 
        }
        return clone;
    }
        
    protected abstract void onAsyncAck(MessageExchangeDAO mexdao);    

}
