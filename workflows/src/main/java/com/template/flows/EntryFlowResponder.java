package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(EntryFlow.class)
public class EntryFlowResponder extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    public EntryFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        boolean flag = otherPartySession.receive(Boolean.class).unwrap(it -> it);
        // Flag to decide when CollectSignaturesFlow is called for this counterparty. SignTransactionFlow is
        // executed only if CollectSignaturesFlow is called from the initiator.
        if(flag) {
            subFlow(new SignTransactionFlow(otherPartySession) {

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                }
            });
        }
        subFlow(new ReceiveFinalityFlow(otherPartySession));


        return null;
    }
}