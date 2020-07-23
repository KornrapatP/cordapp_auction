package com.template.contracts;

import com.template.states.AuctionState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TimeWindow;
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
            System.out.println(1);
            throw new IllegalArgumentException("The id/name don't match!");
        }
        if (!inputState.getAuctioneer().equals(outputState.getAuctioneer())) {
            System.out.println(2);
            throw new IllegalArgumentException("The auctioneer must be the same!");
        }
        if (inputState.getValue()<=outputState.getValue()) {
            System.out.println(3);
            throw new IllegalArgumentException("The new bid must be lower!");
        }
        if (!TimeWindow.untilOnly(inputState.getTimeWindow()).contains(Instant.now())) {
            System.out.println(4);
            throw new IllegalArgumentException("Auction is not Active");
        }
        if (!inputState.getTimeWindow().equals(outputState.getTimeWindow())) {
            System.out.println(5);
            throw new IllegalArgumentException("The time window must not change!");
        }
        if (!command.getSigners().contains(inputState.getAuctioneer().getOwningKey())) {
            System.out.println(6);
            throw new IllegalArgumentException("Auctioneer did not sign this transaction!");
        }
        if (!command.getSigners().contains(outputState.getCurrentBidder().getOwningKey())) {
            System.out.println(7);
            throw new IllegalArgumentException("Bidder did not sign this transaction!");
        }
        if (command.getSigners().size() != 2) {
            System.out.println(8);
            throw new IllegalArgumentException("There must be exactly 2 signers!");
        }
        if (outputState.getValue() < 0) {
            System.out.println(9);
            throw new IllegalArgumentException("Invalid bid value");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Bid implements Commands {}
    }
}