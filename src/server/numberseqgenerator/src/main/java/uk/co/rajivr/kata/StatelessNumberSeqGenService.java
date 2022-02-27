package uk.co.rajivr.kata;



import java.util.Random;
import java.util.logging.Logger;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc;

public class StatelessNumberSeqGenService extends NumberSequenceGeneratorGrpc.NumberSequenceGeneratorImplBase {


	private static final Logger logger = Logger.getLogger(StatelessNumberSeqGenService.class.getName());

	@Override
	public void startNumberSequence(NumSeqRequest request, StreamObserver<NumSeqResponse> responseObserver) {

		long startNumber = (request.getStartNumber() > 0)  ? request.getStartNumber() : StatelessNumberSeqGenService.getRandomNumberInRange(0, 255);
		logger.info("Handling request from client: " +request.getClientId() + " startNumber: " + startNumber);

		int sleepMs = (request.getIntervalMs() > 0)? request.getIntervalMs() : 1000; 
		
		int i=1;
		
		while(!Context.current().isCancelled()){
			
			NumSeqResponse resp = NumSeqResponse.newBuilder()
									.setClientId(request.getClientId())
									.setSeqNo(i++)
									.setNumber(startNumber)
									.build();
			responseObserver.onNext(resp);
			startNumber *= 2;
			
			try {
				// logger.info("Sleeping for " + sleepMs + "ms");
				Thread.sleep(sleepMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
		logger.info("Finished handling request from client: " +request.getClientId());
		responseObserver.onCompleted();		
		
	}
	
	
	private static int getRandomNumberInRange(int min, int max) {
		
		Random r = new Random();
		return r.ints(min, (max + 1)).limit(1).findFirst().getAsInt();
		
	}

}
