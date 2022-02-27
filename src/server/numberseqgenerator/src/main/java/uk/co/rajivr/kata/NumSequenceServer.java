package uk.co.rajivr.kata;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;



/**
 * Server that operates either in stateless or stateful mode as directed by protocol
 */
public class NumSequenceServer 
{
    private static final Logger logger = Logger.getLogger(NumSequenceServer.class.getName());

    private int port = 5000;
    
    private final Server server;
    
    private static ExecutorService executor = Executors.newFixedThreadPool(5);
    
    
    
public NumSequenceServer(int port) {
    	
    // use the internal threadpool for listening to responses by default	
	this(ServerBuilder.forPort(port).executor(executor), port);		
	}
    
    
    public NumSequenceServer(ServerBuilder<?> serverBuilder, int port) {
    	
    	this.port = port;
    	// create a grpc server with our own thread pool to service requests
    	this.server = serverBuilder.addService(new StatelessNumberSeqGenService()).build();
	}
    
 

	/** Start serving requests. */
    public void start() throws IOException {
      server.start();
      logger.info("Server started, listening on " + port);
      
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          // Use stderr here since the logger may have been reset by its JVM shutdown hook.
          System.err.println("*** shutting down gRPC server since JVM is shutting down");
          try {
            NumSequenceServer.this.stop();
        	  
          } catch (InterruptedException e) {
            e.printStackTrace(System.err);
          }
          System.err.println("*** server shut down");
        }
      });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
      if (server != null) {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
      }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
      if (server != null) {
        server.awaitTermination();
      }
    }

    /**
     * Main method.  
     */
    public static void main(String[] args) throws Exception {
      NumSequenceServer server = new NumSequenceServer(5000);
      server.start();
      server.blockUntilShutdown();
    }
}

