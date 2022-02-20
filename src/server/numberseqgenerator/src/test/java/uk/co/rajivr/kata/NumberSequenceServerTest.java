package uk.co.rajivr.kata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.grpc.testing.GrpcCleanupRule;
import uk.co.rajivr.numsequence.NumSeqRequest;
import uk.co.rajivr.numsequence.NumSeqResponse;
import uk.co.rajivr.numsequence.SimpleNumberServiceGrpc;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * Unit test for simple App.
 */
public class NumberSequenceServerTest 
{

	private int port = 5000;
	private ExecutorService executor = Executors.newFixedThreadPool(4);

	/**
	 * This rule manages automatic graceful shutdown for the registered servers and channels at the
	 * end of test.
	 */
	@Rule
	public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

	@Test
	public void simpleNumberServiceImpl_replyMessage() throws Exception
	{

		// Generate a unique in-process server name.
		String serverName = InProcessServerBuilder.generateName();

		// Create a server, add service, start, and register for automatic graceful shutdown.
		grpcCleanup.register(InProcessServerBuilder
				.forName(serverName).directExecutor().addService(new SimpleNumberServiceImpl()).build().start());

		// SimpleNumberServiceGrpc.SimpleNumberServiceFutureStub futureStub = SimpleNumberServiceGrpc.newFutureStub(
		//   grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build())
		// );
		// futureStub.getNumber(NumSeqRequest.newBuilder().setStartNumber(1).setIntervalMs(1000).build());


		SimpleNumberServiceGrpc.SimpleNumberServiceBlockingStub blockingStub =  SimpleNumberServiceGrpc.newBlockingStub(
				grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build())
				);

		NumSeqResponse response = blockingStub.getNumber(NumSeqRequest.newBuilder().setClientId("testclient").setStartNumber(12345).setIntervalMs(1000).build());

		assertEquals(12345, response.getNumber());
		assertEquals("testclient", response.getClientId());

	}



	@Test
	public void simpleNumberService_testAsyncClient() throws Exception
	{
		// Generate a unique in-process server name.
		String serverName = InProcessServerBuilder.generateName();

		// Create a server, add service, start, and register for automatic graceful shutdown.
		grpcCleanup.register(InProcessServerBuilder
				.forName(serverName).directExecutor().addService(new SimpleNumberServiceImpl()).build().start());


		SimpleNumberServiceGrpc.SimpleNumberServiceFutureStub stub = SimpleNumberServiceGrpc.newFutureStub(grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

		// send an async request and test result via a callback on return
		NumSeqRequest request = NumSeqRequest.newBuilder()
				.setClientId("client")
				.setStartNumber(123)
				.build();

		ListenableFuture<NumSeqResponse> future = stub.getNumber(request);

		Futures.addCallback(future, new FutureCallback<NumSeqResponse>() {
			@Override
			public void onSuccess(NumSeqResponse result) {

				System.out.println("Intercepted response for client: " + result.getClientId());
				assertTrue(true);
			}

			@Override
			public void onFailure(Throwable t) {
				t.printStackTrace();
				assertTrue(false);
			}
		}, executor);

		NumSeqResponse resp = future.get();

	}


	@Test
	public void simpleNumberService_testConcurrentClients() throws Exception
	{

		/**
		 * Asynchronous test client for in-process grpc server
		 * @author rajivr
		 *
		 */
		class TestClient{


			private String Id;
			private String serverName;
			private long startNumber = 1;
			private SimpleNumberServiceGrpc.SimpleNumberServiceFutureStub stub;

			public TestClient(String clientId, String grpcServerName, long startNumber) {
				this.Id = clientId;
				this.serverName = grpcServerName;
				this.stub =  SimpleNumberServiceGrpc.newFutureStub(grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
				this.startNumber = startNumber;

			}


			public ListenableFuture<NumSeqResponse> getNumber()
			{

				NumSeqRequest request = NumSeqRequest.newBuilder()
						.setClientId(Id)
						.setStartNumber(this.startNumber)
						.build();

				ListenableFuture<NumSeqResponse> future = stub.getNumber(request);

				return future;		
			}


			public String getId() {
				return Id;
			}

			public long getStartNumber() {
				return startNumber;
			}
		}

		// Generate a unique in-process server name.
		String serverName = InProcessServerBuilder.generateName();

		// Create a server, add service, start, and register for automatic graceful shutdown.
		grpcCleanup.register(InProcessServerBuilder
				.forName(serverName).directExecutor().addService(new SimpleNumberServiceImpl()).build().start());


		TestClient client1 = new TestClient("client1", serverName, 12345);
		TestClient client2 = new TestClient("client2", serverName, 12346);
		TestClient client3 = new TestClient("client3", serverName, 12347);

		final HashMap<String, TestClient> testClients = new HashMap<>();
		testClients.put(client1.getId(), client1);
		testClients.put(client2.getId(), client2);
		testClients.put(client3.getId(), client3);


		ArrayList<ListenableFuture<NumSeqResponse>>	asyncTasks = new ArrayList<>();

		asyncTasks.add(client1.getNumber());
		asyncTasks.add(client2.getNumber());
		asyncTasks.add(client3.getNumber());


		for (ListenableFuture<NumSeqResponse> f : asyncTasks) {

			Futures.addCallback(f, new FutureCallback<NumSeqResponse>() {
				@Override
				public void onSuccess(NumSeqResponse result) {

					System.out.println("Intercepted response for client: " + result.getClientId());

					TestClient client = testClients.get(result.getClientId());
					assertEquals(result.getNumber(), client.getStartNumber());
				}

				@Override
				public void onFailure(Throwable t) {
					t.printStackTrace();
					assertTrue(false);

				}
			}, executor);
		}



		for (ListenableFuture<NumSeqResponse> f : asyncTasks) {
			NumSeqResponse resp =  f.get();
		}
	}




	@Test
	public void simpleNumberService_testConcurrentClientsFanIn() throws Exception
	{

		/**
		 * Asynchronous test client for in-process grpc server
		 * @author rajivr
		 *
		 */
		class TestClient{


			private String Id;
			private String serverName;
			private long startNumber = 1;
			private SimpleNumberServiceGrpc.SimpleNumberServiceFutureStub stub;

			public TestClient(String clientId, String grpcServerName, long startNumber) {
				this.Id = clientId;
				this.serverName = grpcServerName;
				this.stub =  SimpleNumberServiceGrpc.newFutureStub(grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
				this.startNumber = startNumber;

			}

			public ListenableFuture<NumSeqResponse> getNumber()
			{

				NumSeqRequest request = NumSeqRequest.newBuilder()
						.setClientId(Id)
						.setStartNumber(this.startNumber)
						.build();

				ListenableFuture<NumSeqResponse> future = stub.getNumber(request);

				return future;		
			}


			public String getId() {
				return Id;
			}

			public long getStartNumber() {
				return startNumber;
			}
		}

		// Generate a unique in-process server name.
		String serverName = InProcessServerBuilder.generateName();

		// Create a server, add service, start, and register for automatic graceful shutdown.
		grpcCleanup.register(InProcessServerBuilder
				.forName(serverName).directExecutor().addService(new SimpleNumberServiceImpl()).build().start());


		TestClient client1 = new TestClient("client1", serverName, 12345);
		TestClient client2 = new TestClient("client2", serverName, 12346);
		TestClient client3 = new TestClient("client3", serverName, 12347);

		final HashMap<String, TestClient> testClients = new HashMap<>();
		testClients.put(client1.getId(), client1);
		testClients.put(client2.getId(), client2);
		testClients.put(client3.getId(), client3);



		ListenableFuture<List<NumSeqResponse>> tasks = Futures.allAsList(client1.getNumber(), client2.getNumber(), client3.getNumber());

		Futures.addCallback(tasks, new FutureCallback<List<NumSeqResponse>>() {
			@Override
			public void onSuccess(List<NumSeqResponse> results) {

				for (NumSeqResponse result : results) {

					assertNotNull(result);
					System.out.println("Intercepted response for client: " + result.getClientId());
					TestClient client = testClients.get(result.getClientId());
					assertEquals(result.getNumber(), client.getStartNumber());

				}
			}

			@Override
			public void onFailure(Throwable t) {
				t.printStackTrace();
				assertTrue(false);

			}
		}, executor);
	}






}



