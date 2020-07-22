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
        if(tx.getInputStates().size() != 0) throw new IllegalArgumentException("One Input Expected");
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");
        Command command = tx.getCommand(0);
        AuctionState auctionState = (AuctionState) tx.getOutput(0);
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

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Action implements Commands {}
        class Create implements Commands {}
        class Bid implements Commands {}
    }
}