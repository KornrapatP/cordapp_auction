package com.template;

import com.google.common.collect.ImmutableList;
import com.template.flows.BidFlow;
import com.template.flows.CreateFlow;
import com.template.states.AuctionState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.ZoneOffset;

import  static org.junit.Assert.*;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode c = network.createNode();

    public FlowTests() {
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void dummyTest() throws Exception{
        CreateFlow flow = new CreateFlow(100, "test", Instant.now().plusSeconds(20).toString());
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        AuctionState output = signedTransaction.getTx().outputsOfType(AuctionState.class).get(0);

        assertEquals("test", output.getName());
        assertEquals(100, (int) output.getValue());
    }

}
