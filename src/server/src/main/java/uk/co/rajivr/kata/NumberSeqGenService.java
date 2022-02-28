package uk.co.rajivr.kata;

import java.util.logging.Logger;

import io.grpc.stub.StreamObserver;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.NumberSequenceGeneratorGrpc;


/**
 * 
 * Number sequence generator service. 
 * startNumberSequence() will forward request to a server implementation based on mode specified by client: STATEFUL/STATELESS
 *
 */
public class NumberSeqGenService extends NumberSequenceGeneratorGrpc.NumberSequenceGeneratorImplBase {


	private static final Logger logger = Logger.getLogger(NumberSeqGenService.class.getName());

	/* Server mode implementations */
	private StatelessNumberSeqGenServiceImpl statelessServiceImpl; // stateless server impl.
	private StatefulNumberSeqGenServiceImpl statefulServiceImpl; // stateful server impl.

	
	
	
	public NumberSeqGenService() {

		statelessServiceImpl = new StatelessNumberSeqGenServiceImpl();
		statefulServiceImpl= new StatefulNumberSeqGenServiceImpl();
		
	}
	
	
	

	@Override
	public void startNumberSequence(NumSeqRequest request, StreamObserver<NumSeqResponse> responseObserver) {
		
		
		
		logger.info("Server mode value" + request.getServiceModeValue());
		
		
		
		switch (request.getServiceMode()) {
		case STATEFUL_SERVER:

			logger.info("Handling client request in stateful server mode ...");
			statefulServiceImpl.startNumberSequence(request, responseObserver);
			
			break;
		case STATELESS_SERVER:

			logger.info("Handling client request in stateless server mode ...");
			statelessServiceImpl.startNumberSequence(request, responseObserver);
			
			break;

		default:
			break;
		}
		
	}
	
	
	

}
