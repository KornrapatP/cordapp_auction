package com.template.states;
import com.template.contracts.TemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.time.Instant;
import java.util.List;

/** Class representing the state of an auction */
@BelongsToContract(TemplateContract.class)
public class AuctionState implements ContractState {
    /** List of everyone allowed to participate */
    private final List<AbstractParty> participants;

    /** Name of the auction used as identifier */
    private final String name;

    /** Bid value */
    private final Integer value;

    /** Time the auction closes */
    private final Instant timeWindow;

    /** Auctioneer */
    private final Party auctioneer;

    /** Current Bidder */
    private final Party currentBidder;

    /**
     * Constructor
     * @param participants
     * @param name
     * @param value
     * @param timeWindow
     * @param auctioneer
     * @param currentBidder
     */
    public AuctionState(List<AbstractParty> participants, String name, Integer value, Instant timeWindow, Party auctioneer, Party currentBidder) {
        this.participants = participants;
        this.name = name;
        this.value = value;
        this.timeWindow = timeWindow;
        this.auctioneer = auctioneer;
        this.currentBidder = currentBidder;
    }

    /** Auctioneer getter */
    public Party getAuctioneer() {
        return auctioneer;
    }

    /** CurrentBidder getter */
    public Party getCurrentBidder() {
        return currentBidder;
    }

    /** Value getter */
    public Integer getValue() {
        return value;
    }

    /** TimeWindow getter */
    public Instant getTimeWindow() {
        return timeWindow;
    }

    /** Name getter */
    public String getName() {
        return name;
    }

    /** Participants getter */
    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}