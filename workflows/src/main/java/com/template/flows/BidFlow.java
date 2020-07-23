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

import java.security.PublicKey;
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

        // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionName
        // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
        // transaction.
        List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                .queryBy(AuctionState.class).getStates();

        StateAndRef<AuctionState> inputStateAndRef = auntionStateAndRefs.stream().filter(auctionStateAndRef -> {
            AuctionState auctionState = auctionStateAndRef.getState().getData();
            return auctionState.getName().equals(auctionName);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));

        AuctionState input = inputStateAndRef.getState().getData();

        // Create outputState
        AuctionState outputState = new AuctionState(input.getParticipants() , auctionName, auctionValue, input.getTimeWindow(), input.getAuctioneer(), getOurIdentity());
        // Put all signers PubicKey into a list
        List<PublicKey> signers = new ArrayList<PublicKey>();
        signers.add(getOurIdentity().getOwningKey());
        signers.add(input.getAuctioneer().getOwningKey());

        // Create Command from CommandData Bid and list of required signers
        Command command = new Command<>(new TemplateContract.Commands.Bid(), signers);

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState, TemplateContract.ID)
                .addCommand(command);

        // Verify transaction
        txBuilder.verify(getServiceHub());

        // Self Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Create a Session with the Auctioneer and initiate CollectSignaturesFlow
        FlowSession auctioneerSes = initiateFlow(input.getAuctioneer());
        auctioneerSes.send(true);
        signedTx = subFlow(new CollectSignaturesFlow(signedTx, Collections.singletonList(auctioneerSes)));

        // Initiate Sessions with all participants to Finalize flow
        List<FlowSession> allSessions = new ArrayList<FlowSession>();
        allSessions.add(auctioneerSes);

        for(AbstractParty party: outputState.getParticipants()){
            if(!party.equals(getOurIdentity())) {
                FlowSession session = initiateFlow(party);
                session.send(false);
                allSessions.add(session);
            }
        }

        allSessions.remove(notary);

        subFlow(new FinalityFlow(signedTx, allSessions));
        return null;
    }
}