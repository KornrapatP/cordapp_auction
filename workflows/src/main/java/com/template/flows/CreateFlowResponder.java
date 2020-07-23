package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(CreateFlow.class)
public class CreateFlowResponder extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    public CreateFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Finalize the Flow and save new state
        subFlow(new ReceiveFinalityFlow(otherPartySession));

        return null;
    }
}