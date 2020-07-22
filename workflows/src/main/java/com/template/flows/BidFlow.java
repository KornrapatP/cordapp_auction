package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TemplateContract;
import com.template.states.AuctionState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.contracts.Command;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.intellij.lang.annotations.Flow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class BidFlow extends FlowLogic<Void> {
    private final Integer auctionValue;
    private final String auctionName;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public BidFlow(Integer auctionValue, String auctionName) {
        this.auctionValue = auctionValue;
        this.auctionName = auctionName;
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

        // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionId
        // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
        // transaction.
        List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                .queryBy(AuctionState.class).getStates();

        StateAndRef<AuctionState> inputStateAndRef = auntionStateAndRefs.stream().filter(auctionStateAndRef -> {
            AuctionState auctionState = auctionStateAndRef.getState().getData();
            return auctionState.getName().equals(auctionName);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));

        AuctionState input = inputStateAndRef.getState().getData();

        AuctionState outputState = new AuctionState(input.getParticipants() , auctionName, auctionValue, input.getTimeWindow(), input.getAuctioneer(), getOurIdentity());
        Command command = new Command<>(new TemplateContract.Commands.Bid(), getOurIdentity().getOwningKey());

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState, TemplateContract.ID)
                .addCommand(command);

        // Verify transaction
        txBuilder.verify(getServiceHub());

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);


        // Creating a session with the other party.

        System.out.println("1");
        // We finalise the transaction and then send it to the counterparty.
        signedTx = subFlow(new CollectSignaturesFlow(signedTx, Collections.singletonList(initiateFlow(input.getAuctioneer()))));
        List<FlowSession> allSessions = new ArrayList<FlowSession>();
        for (AbstractParty party: outputState.getParticipants()){
            if(!party.equals(getOurIdentity())) {
                FlowSession session = initiateFlow(party);
                allSessions.add(session);
            }
        }

        allSessions.remove(notary);
        System.out.println("2");
        subFlow(new FinalityFlow(signedTx, allSessions));
        return null;
    }
}