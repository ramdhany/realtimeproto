/**
 * 
 */
package uk.co.rajivr.kata;

import io.grpc.stub.StreamObserver;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.SimpleNumberServiceGrpc.SimpleNumberServiceImplBase;

/**
 * @author rajivr
 *
 */
public class SimpleNumberServiceImpl extends SimpleNumberServiceImplBase {
	
	
	@Override
	public void getNumber(NumSeqRequest request, StreamObserver<NumSeqResponse> responseObserver) {
		
		NumSeqResponse response = NumSeqResponse.newBuilder()
				.setClientId(request.getClientId())
				.setNumber(request.getStartNumber())
				.setSeqNo(1)
				.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();		
		
	}
	
	
	
	

}
