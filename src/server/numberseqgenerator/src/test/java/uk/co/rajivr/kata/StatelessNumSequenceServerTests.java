package uk.co.rajivr.kata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc;

public class StatelessNumSequenceServerTests {


	/**
	 * This rule manages automatic graceful shutdown for the registered servers and channels at the
	 * end of test.
	 */
	@Rule
	public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

	private NumSequenceServer server;
	private ManagedChannel inProcessChannel;


	@Before
	public void setUp() throws Exception {

		// Generate a unique in-process server name.
		String serverName = InProcessServerBuilder.generateName();

		// Use directExecutor for both InProcessServerBuilder and InProcessChannelBuilder can reduce the
		// usage timeouts and latches in test. But we still add timeout and latches where they would be
		// needed if no directExecutor were used, just for demo purpose.
		server = new NumSequenceServer(
				InProcessServerBuilder.forName(serverName).directExecutor(), 0);
		server.start();


		// Create a client channel and register for automatic graceful shutdown.
		inProcessChannel = grpcCleanup.register(
				InProcessChannelBuilder.forName(serverName).directExecutor().build());


	}

	@After
	public void tearDown() throws Exception {

		server.stop();

	}


	@Test
	public void testMessagesReceived()throws Exception{

		ArrayList<Long> expectedNumberList = new ArrayList<>();
		final ArrayList<Long> actualNumberList = new ArrayList<>();

		NumSeqRequest request = NumSeqRequest.newBuilder()
				.setClientId("testClient")
				.setNumTotalMessages(10)
				.setStartNumber(2)
				.setIntervalMs(100)
				.build();


		long num = 2;

		for(int i=0; i <10; i++)
		{
			num *=2;
			expectedNumberList.add(Long.valueOf(num));
		}

		final CountDownLatch latch = new CountDownLatch(1);


		StreamObserver<NumSeqResponse> responseObserver =
				new StreamObserver<NumSeqResponse>() {
			@Override
			public void onNext(NumSeqResponse resp) {
				actualNumberList.add(Long.valueOf(resp.getNumber()));
			}

			@Override
			public void onError(Throwable t) {
				fail();
			}

			@Override
			public void onCompleted() {
				latch.countDown();
			}
		};
		
		
		NumberSequenceGeneratorGrpc.NumberSequenceGeneratorStub stub = NumberSequenceGeneratorGrpc.newStub(inProcessChannel);
		
		// make the RPC call
		stub.startNumberSequence(request, responseObserver);
		
		assertTrue(latch.await(5, TimeUnit.SECONDS));
		
		
		// check if received numbers match with expected numbers
		assertEquals(expectedNumberList, actualNumberList);
	}
	

}
