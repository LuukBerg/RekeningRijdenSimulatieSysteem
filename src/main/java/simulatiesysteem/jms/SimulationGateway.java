/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem.jms;

import simulatiesysteem.jms.gateway.MessageSenderGateway;

/**
 *
 * @author u
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
