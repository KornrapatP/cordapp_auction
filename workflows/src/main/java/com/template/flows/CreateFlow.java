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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class CreateFlow extends FlowLogic<SignedTransaction> {
    private final Integer auctionValue;
    private final String auctionName;
    private final Instant auctiontimeWindow;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    /**
     * Constructor
     * @param auctionValue
     * @param auctionName
     * @param auctionTime
     */
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
    public SignedTransaction call() throws FlowException {
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Query vault for AuctionStates
        List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                .queryBy(AuctionState.class).getStates();
        // Put all AuctionStates with the same input name to an Array
        Object[] inputStateAndRef = auntionStateAndRefs.stream().filter(auctionStateAndRef -> {
            AuctionState auctionState = auctionStateAndRef.getState().getData();
            return auctionState.getName().equals(auctionName);
        }).toArray();
        // Check if there's a duplicate
        if (inputStateAndRef.length != 0) {
            throw new IllegalArgumentException("duplicate names");
        }

        // We create the transaction components.
        // Get all nodes without notary to be added to the AuctionState constructor as participants
        List<AbstractParty> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                .collect(Collectors.toList());
        parties.remove(notary);

        // Generate output state
        ArrayList<AbstractParty> participants = new ArrayList<AbstractParty>();
        participants.add(getOurIdentity());
        AuctionState outputState = new AuctionState(parties, this.auctionName, this.auctionValue, this.auctiontimeWindow, getOurIdentity(), getOurIdentity(), participants);

        // Create command object with a CommandData and PublicKeys of the signers
        Command command = new Command<>(new TemplateContract.Commands.Create(), getOurIdentity().getOwningKey());

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, TemplateContract.ID)
                .addCommand(command);

        // Verify transaction
        txBuilder.verify(getServiceHub());

        // Self signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Creating a session with the other party.
        List<FlowSession> bidderSessions = new ArrayList<>();

        // Create a copy of Parties and remove our identity
        List<AbstractParty> send = new ArrayList<AbstractParty>(parties);
        send.remove(getOurIdentity());

        // Add Sessions to List
        for(AbstractParty bidder: send) {
            System.out.println(bidder);
            bidderSessions.add(initiateFlow(bidder));
        }

        // We finalise the transaction and then send it to the counterparty.

        return subFlow(new FinalityFlow(signedTx, bidderSessions));
    }
}