package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.NotNull;

import java.security.SignatureException;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(BidFlow.class)
public class BidFlowResponder extends FlowLogic<SignedTransaction> {
    private final FlowSession otherPartySession;

    public BidFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
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



        return subFlow(new ReceiveFinalityFlow(otherPartySession));
    }
}