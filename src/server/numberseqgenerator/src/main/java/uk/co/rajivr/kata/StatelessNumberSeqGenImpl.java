package uk.co.rajivr.kata;



import java.util.Random;
import java.util.logging.Logger;


import io.grpc.stub.StreamObserver;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc;

public class StatelessNumberSeqGenImpl extends NumberSequenceGeneratorGrpc.NumberSequenceGeneratorImplBase {


	private static final Logger logger = Logger.getLogger(StatelessNumberSeqGenImpl.class.getName());

	@Override
	public void startNumberSequence(NumSeqRequest request, StreamObserver<NumSeqResponse> responseObserver) {

		logger.info("Handling request from client: " +request.getClientId());

		long startNumber = (request.getStartNumber() > 0)  ? request.getStartNumber() : StatelessNumberSeqGenImpl.getRandomNumberInRange(0, 255);
		
		int count = request.getNumTotalMessages();
		int sleepMs = (request.getIntervalMs() > 0)? request.getIntervalMs() : 1000; 
		
		for (int i = 0; i <count; i++) {
			
			NumSeqResponse resp = NumSeqResponse.newBuilder()
									.setClientId(request.getClientId())
									.setSeqNo(i+1)
									.setNumber(startNumber)
									.build();
			responseObserver.onNext(resp);
			startNumber *= 2;
			
			try {
				Thread.sleep(sleepMs);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		logger.info("Finished handling request from client: " +request.getClientId());
		
		
	}
	
	
	private static int getRandomNumberInRange(int min, int max) {
		
		Random r = new Random();
		return r.ints(min, (max + 1)).limit(1).findFirst().getAsInt();
		
	}

}
