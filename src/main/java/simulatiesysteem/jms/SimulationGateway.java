package simulatiesysteem.jms;

import simulatiesysteem.jms.gateway.MessageSenderGateway;

/**
 *
 * @author Rabie Bkhiti
 */
public class SimulationGateway {

    private MessageSenderGateway sender;

    public SimulationGateway() {
        sender = new MessageSenderGateway("StepChannel");

    }

    private void initSender() {
        sender();
    }

    private void sender() {
    }

}
