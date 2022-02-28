package uk.co.rajivr.kata;


import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc.NumberSequenceGeneratorStub;


public class NumSequenceClient {


	private static final Logger logger = Logger.getLogger(NumSequenceClient.class.getName());

	private final NumberSequenceGeneratorStub stub;

	private int totalNumMessages = 0;

	private String Id;


	private long sum = 0;
	//	private final NumberSequenceGeneratorStub asyncStub;


	public NumSequenceClient(Channel channel, int messageCount, String clientId) {



		stub = NumberSequenceGeneratorGrpc.newStub(channel);
		totalNumMessages = messageCount;
		this.Id = clientId;
	}


	public void getNumberSequence()
	{
		final CountDownLatch latch = new CountDownLatch(1);
		
		logger.info("Client " + Id + ":");
		NumSeqRequest request = NumSeqRequest.newBuilder()
				.setClientId(Id)
				.setNumTotalMessages(totalNumMessages)
				.build();


		StreamObserver<NumSeqResponse> responseObserver =
				new StreamObserver<NumSeqResponse>() {
			@Override
			public void onNext(NumSeqResponse resp) {

				long recvNumber = resp.getNumber();
				logger.info("Number " + resp.getSeqNo() + " : " + recvNumber);

				sum+= recvNumber;
				
				
				if (resp.getSeqNo() >= totalNumMessages)
				{
					latch.countDown();
					
				}
					
			}

			@Override
			public void onError(Throwable t) {

			}

			@Override
			public void onCompleted() {
				latch.countDown();
			}
		};
		
		
		stub.startNumberSequence(request, responseObserver);
		
		try {
			latch.await();
//			latch.await(totalNumMessages + 10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		logger.info("client.getSequence() finished.");



	}


	public long getSum()
	{
		return this.sum;
	}



	public static void main(String[] args) throws InterruptedException {
		String target = "localhost:5000";
		if (args.length > 0) {
			if ("--help".equals(args[0])) {
				System.err.println("Usage: [target]");
				System.err.println("");
				System.err.println("  target  The server to connect to. Defaults to " + target);
				System.exit(1);
			}
			target = args[0];
		}


		ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();	 

		try {
			NumSequenceClient client = new NumSequenceClient(channel, 10, "client-" + UUID.randomUUID());
			client.getNumberSequence();


		} finally {
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}

	}

}
