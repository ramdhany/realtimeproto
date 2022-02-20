package uk.co.rajivr.kata;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc.NumberSequenceGeneratorBlockingStub;


public class NumSequenceClient {


	private static final Logger logger = Logger.getLogger(NumSequenceClient.class.getName());

	private final NumberSequenceGeneratorBlockingStub blockingStub;

	private int totalNumMessages = 0;

	private String Id;

	private long lastReceivedNumber = 0;
	private long sum = 0;
	//	private final NumberSequenceGeneratorStub asyncStub;


	public NumSequenceClient(Channel channel, int messageCount, String clientId) {
		blockingStub = NumberSequenceGeneratorGrpc.newBlockingStub(channel);
		totalNumMessages = messageCount;
		this.Id = clientId;
	}


	public void getNumberSequence()
	{
		logger.info("Client " + Id + ":");
		NumSeqRequest request = NumSeqRequest.newBuilder()
				.setClientId(Id)
				.setNumTotalMessages(totalNumMessages)
				.build();

		Iterator<NumSeqResponse> numberSequence;
		
		 try {

		numberSequence = blockingStub.startNumberSequence(request);

		while( numberSequence.hasNext())
		{

			NumSeqResponse resp = numberSequence.next();

			long recvNumber = resp.getNumber();
			logger.info("Number " + resp.getSeqNo() + " : " + recvNumber);

			sum+= recvNumber;
			lastReceivedNumber = recvNumber;
		}
		
		 } catch (StatusRuntimeException e) {
		      logger.warning("RPC failed: {0}" + e.getStatus());		    
		    }

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
			 NumSequenceClient client = new NumSequenceClient(channel, 50, "client-" + UUID.randomUUID());
			 client.getNumberSequence();
			 
			
		} finally {
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}

	}

}
