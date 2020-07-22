package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TemplateContract;
import com.template.states.AuctionState;
import com.template.states.IOUState;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.contracts.Command;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.hibernate.mapping.Array;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class CreateFlow extends FlowLogic<Void> {
    private final Integer auctionValue;
    private final String auctionName;
    private final Instant auctiontimeWindow;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public CreateFlow(Integer auctionValue, String auctionName, String auctionTime) {
        this.auctionValue = auctionValue;
        this.auctionName = auctionName;
        this.auctiontimeWindow =  Instant.parse(auctionTime);
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create the transaction components.
        List<AbstractParty> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                .collect(Collectors.toList());
        parties.remove(notary);
        AuctionState outputState = new AuctionState(parties, this.auctionName, this.auctionValue, this.auctiontimeWindow, getOurIdentity());
        Command command = new Command<>(new TemplateContract.Commands.Create(), getOurIdentity().getOwningKey());

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, TemplateContract.ID)
                .addCommand(command);

        // Verify transaction
        txBuilder.verify(getServiceHub());

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
        // Creating a session with the other party.
        List<FlowSession> bidderSessions = new ArrayList<>();
        List<AbstractParty> send = new ArrayList<AbstractParty>(parties);
        send.remove(getOurIdentity());
        for(AbstractParty bidder: send) {
            System.out.println(bidder);
            bidderSessions.add(initiateFlow(bidder));
        }
        // We finalise the transaction and then send it to the counterparty.
        subFlow(new FinalityFlow(signedTx, bidderSessions));
        return null;
    }
}