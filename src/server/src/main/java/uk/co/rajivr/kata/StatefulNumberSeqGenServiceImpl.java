/**
 * 
 */
package uk.co.rajivr.kata;


import java.util.logging.Logger;

import org.apache.commons.math3.random.RandomDataGenerator;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.NumSeqResponse.Builder;


/**
 * @author rajivr
 *
 */
public class StatefulNumberSeqGenServiceImpl{

	private static final Logger logger = Logger.getLogger(StatefulNumberSeqGenServiceImpl.class.getName());


	// Random number generator uses the Well19937c algorithm
	private RandomDataGenerator prng;

	private ClientState clientData;


	private IDataStore ds;


	private boolean cancelNumberGeneration;

	private Thread numberGenThread;

	/**
	 * 
	 */
	public StatefulNumberSeqGenServiceImpl() {

		ds = null;
		cancelNumberGeneration=  false;

	}



	private void startNumberGeneration(int count)
	{
		logger.info("Stateful server: Generating number sequence for client " + clientData.getClientId() + " ...");
		this.prng = new RandomDataGenerator();

		Runnable numberGenerationTask = () -> {

			for (int i = 0; (i < count && !cancelNumberGeneration) ; i++) {
				clientData.addGeneratedNumber(Integer.valueOf(prng.nextInt(1, 0xffff)));
			}

			logger.info("Stateful server: Finished generating number sequence for client " + clientData.getClientId());
		};

		numberGenThread = new Thread(numberGenerationTask);
		numberGenThread.start();

	}

	public void startNumberSequence(NumSeqRequest request, StreamObserver<NumSeqResponse> responseObserver) {

		// load client's state from datastore
		clientData = ClientState.readClientStateFromDS(request.getClientId(), ds);


		// get total count of numbers to produce
		int count = request.getNumTotalMessages();

		// if we have less numbers in store, generate some more		
		if (clientData.getTotalNumberCount() < count)
		{
			startNumberGeneration(count);
		}


		int sleepMs = (request.getIntervalMs() > 0)? request.getIntervalMs() : 1000; 


		int i = 1;
		int number = 0;

		while(!Context.current().isCancelled() && i < count){

			// Get a number from generated sequence and update checksum before sending
			try {
				number = clientData.getGeneratedNumber().intValue();
				
				logger.info("Read value: " + number);


				logger.info("Stateful server. number to client: " + number);


				Builder respBuilder = NumSeqResponse.newBuilder()
						.setClientId(request.getClientId())
						.setSeqNo(i++)
						.setNumber(number);
				if (i == count)
				{
					respBuilder.setChecksum(clientData.getChecksum());
				}

				responseObserver.onNext(respBuilder.build());
				i++;


				// logger.info("Sleeping for " + sleepMs + "ms");
				Thread.sleep(sleepMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		logger.info("Finished handling request from client: " + request.getClientId());
		responseObserver.onCompleted();		
	}

}
