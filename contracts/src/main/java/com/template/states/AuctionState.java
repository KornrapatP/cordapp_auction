package com.template.states;
import com.template.contracts.TemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Replace TemplateState's definition with:
@BelongsToContract(TemplateContract.class)
public class AuctionState implements ContractState {
    private final List<AbstractParty> participants;
    private final String name;
    private final Integer value;
    private final Instant timeWindow;
    private final Party auctioneer;
    private final Party currentBidder;
    public AuctionState(List<AbstractParty> participants, String name, Integer value, Instant timeWindow, Party auctioneer, Party currentBidder) {
        this.participants = participants;
        this.name = name;
        this.value = value;
        this.timeWindow = timeWindow;
        this.auctioneer = auctioneer;
        this.currentBidder = currentBidder;
    }

    public Party getAuctioneer() {
        return auctioneer;
    }

    public Party getCurrentBidder() {
        return currentBidder;
    }

    public Integer getValue() {
        return value;
    }

    public Instant getTimeWindow() {
        return timeWindow;
    }

    public String getName() {
        return name;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}