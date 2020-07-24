package com.template.contracts;

import com.template.states.AuctionState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.time.Instant;

// ************
// * Contract *
// ************
public class TemplateContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.template.contracts.TemplateContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        if(tx.getCommands().size() != 1){
            throw new IllegalArgumentException("One command Expected");
        }

        Command command = tx.getCommand(0);
        if (command.getValue() instanceof Commands.Create) {
            verifyCreate(tx);
        } else if (command.getValue() instanceof Commands.Bid) {
            verifyBid(tx);
        } else if (command.getValue() instanceof  Commands.Entry) {
            verifyEntry(tx);
        }
    }

    private void verifyEntry(LedgerTransaction tx) {
        if(tx.getInputStates().size() != 1) throw new IllegalArgumentException("One Input Expected");
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");
        Command command = tx.getCommand(0);
        AuctionState outputState = (AuctionState) tx.getOutput(0);
        AuctionState inputState = (AuctionState) tx.getInput(0);
        //check id/ name
        if (!outputState.getName().equals(inputState.getName())) {
            System.out.println(1);
            throw new IllegalArgumentException("The id/name don't match!");
        }
        if (!inputState.getAuctioneer().equals(outputState.getAuctioneer())) {
            System.out.println(2);
            throw new IllegalArgumentException("The auctioneer must be the same!");
        }
        if (!inputState.getValue().equals(outputState.getValue())) {
            System.out.println(3);
            System.out.println(inputState.getValue());
            System.out.println(outputState.getValue());
            throw new IllegalArgumentException("The new bid must not change");
        }
        if (!TimeWindow.untilOnly(inputState.getTimeWindow()).contains(Instant.now())) {
            System.out.println(4);
            throw new IllegalArgumentException("Auction is not Active");
        }
        if (!inputState.getTimeWindow().equals(outputState.getTimeWindow())) {
            System.out.println(5);
            throw new IllegalArgumentException("The time window must not change!");
        }
        if (!inputState.getParticipants().containsAll(outputState.getParticipants())) {
            System.out.println(6);
            throw new IllegalArgumentException("Participants were modified!");
        }
        if (!outputState.getParticipants().containsAll(inputState.getParticipants())) {
            System.out.println(7);
            throw new IllegalArgumentException("Participants were modified!");
        }
        if (!outputState.getAllowedBidders().contains(outputState.getCurrentBidder())) {
            System.out.println(8);
            throw new IllegalArgumentException("Bidder hasn't paid the entry fee");
        }
        if (!outputState.getAllowedBidders().containsAll(inputState.getAllowedBidders())) {
            System.out.println(9);
            throw new IllegalArgumentException("Participants were maliciously modified!");
        }
        if (outputState.getAllowedBidders().size() != inputState.getAllowedBidders().size() + 1) {
            System.out.println(10);
            throw new IllegalArgumentException("Participants were maliciously modified!");
        }
        if (!command.getSigners().contains(inputState.getAuctioneer().getOwningKey())) {
            System.out.println(11);
            throw new IllegalArgumentException("Auctioneer did not sign this transaction!");
        }
        for (AbstractParty party : outputState.getAllowedBidders()) {
            if (!(inputState.getAllowedBidders().contains(party) || command.getSigners().contains(party.getOwningKey()))) {
                System.out.println(12);
                throw new IllegalArgumentException("Newly added bidder did not sign this!");
            }
        }
        if (command.getSigners().size() != 2) {
            System.out.println(13);
            throw new IllegalArgumentException("There must be exactly 2 signers!");
        }
    }

    private void verifyCreate(LedgerTransaction tx) {
        if(tx.getInputStates().size() != 0) throw new IllegalArgumentException("Zero Input Expected");
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");
        Command command = tx.getCommand(0);
        AuctionState auctionState = (AuctionState) tx.getOutput(0);
        if (!auctionState.getAuctioneer().equals(auctionState.getCurrentBidder())) {
            throw new IllegalArgumentException("The auctioneer must initially be the highest bidder!");
        }
        if(!TimeWindow.untilOnly(auctionState.getTimeWindow()).contains(Instant.now())) {
            throw new IllegalArgumentException("Auction is not Active");
        }
        if (!command.getSigners().contains(auctionState.getAuctioneer().getOwningKey())) {
            throw new IllegalArgumentException("Auctioneer did not issue this auction");
        }
        if (command.getSigners().size() != 1) {
            throw new IllegalArgumentException("Only the auctioneer could sign this");
        }
        if (auctionState.getValue() <= 0) {
            throw new IllegalArgumentException("Invalid starting value");
        }
    }

    private void verifyBid(LedgerTransaction tx) {
        if(tx.getInputStates().size() != 1) throw new IllegalArgumentException("One Input Expected");
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");
        Command command = tx.getCommand(0);
        AuctionState outputState = (AuctionState) tx.getOutput(0);
        AuctionState inputState = (AuctionState) tx.getInput(0);
        //check id/ name
        if (!outputState.getName().equals(inputState.getName())) {
            throw new IllegalArgumentException("The id/name don't match!");
        }
        if (!inputState.getAuctioneer().equals(outputState.getAuctioneer())) {
            throw new IllegalArgumentException("The auctioneer must be the same!");
        }
        if (inputState.getValue()<=outputState.getValue()) {
            throw new IllegalArgumentException("The new bid must be lower!");
        }
        if (!TimeWindow.untilOnly(inputState.getTimeWindow()).contains(Instant.now())) {
            throw new IllegalArgumentException("Auction is not Active");
        }
        if (!inputState.getTimeWindow().equals(outputState.getTimeWindow())) {
            throw new IllegalArgumentException("The time window must not change!");
        }
        if (!inputState.getParticipants().containsAll(outputState.getParticipants())) {
            throw new IllegalArgumentException("Participants were modified!");
        }
        if (!outputState.getParticipants().containsAll(inputState.getParticipants())) {
            throw new IllegalArgumentException("Participants were modified!");
        }
        if (!outputState.getAllowedBidders().contains(outputState.getCurrentBidder())) {
            throw new IllegalArgumentException("Bidder hasn't paid the entry fee");
        }
        if (!inputState.getAllowedBidders().containsAll(outputState.getAllowedBidders())) {
            throw new IllegalArgumentException("Allowed Bidders were modified!");
        }
        if (!outputState.getAllowedBidders().containsAll(inputState.getAllowedBidders())) {
            throw new IllegalArgumentException("Allowed Bidders were modified!");
        }
        if (!command.getSigners().contains(inputState.getAuctioneer().getOwningKey())) {
            throw new IllegalArgumentException("Auctioneer did not sign this transaction!");
        }
        if (!command.getSigners().contains(outputState.getCurrentBidder().getOwningKey())) {
            throw new IllegalArgumentException("Bidder did not sign this transaction!");
        }
        if (command.getSigners().size() != 2) {
            throw new IllegalArgumentException("There must be exactly 2 signers!");
        }
        if (outputState.getValue() < 0) {
            throw new IllegalArgumentException("Invalid bid value");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Bid implements Commands {}
        class Entry implements Commands {}
    }
}