package uk.co.rajivr.kata;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



/**
 * Class representing state data for each client connection
 * 
 */
public class ClientState {


	private String clientId;
	private int  lastIndexSent;

	// We should avoid  holding all the generated mumbers in memory
	// for now, we can hold all values in this queue 
	// later we can hold number values in a datastore like Redis
	BlockingDeque<Integer> genNumberQueue;	
	
	
	private int checksum;


	private IDataStore dataStore;



	public ClientState(String clientid, IDataStore ds) {

		this.clientId = clientid;
		this.dataStore = ds;
		this.genNumberQueue =  new LinkedBlockingDeque<>();
		this.checksum = 0;


	}


	public ClientState(String clientid, int lastNumberIndex, IDataStore ds) {

		this.clientId = clientid;
		this.lastIndexSent = lastNumberIndex;
		this.dataStore = ds;

	}
	
	
	/**
	 * Get the total number 
	 * @return
	 */
	public int getTotalNumberCount()
	{
		return genNumberQueue.size();
	}
	
	
	public void addGeneratedNumber(Integer number)
	{
		
		genNumberQueue.addLast(number);
		
		
	}
	
	
	public Integer getGeneratedNumber() throws InterruptedException
	{
		Integer num = genNumberQueue.takeFirst();
		
		// update checksum
		
		updateCheckSum(num.intValue());
		
		return  num;
	}
	
	


	/**
	 * 
	 * @param clientId
	 * @return a new ClientState instance
	 */
	public static ClientState readClientStateFromDS(String clientId, IDataStore ds)
	{

		// read state from datastore 
		
		// for now, create an empty ClientState object

		return new ClientState(clientId, ds);

	}


	boolean loadtNumberRangeFromDS(int startIdx, int endIdx)
	{


		return true;
	}


	public boolean writeStateToDS()
	{

		return true;

	}


	public int getChecksum() {
		return checksum;
	}


	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}
	
	
	void updateCheckSum(int number)
	{
		int nob = 0;
		int number_complement = 0;
		nob = (int)(Math.floor(Math.log(number) / Math.log(2))) + 1;
		
		number_complement = ((1 << nob) - 1) ^ number;
		
		checksum += number_complement;
	}


	public String getClientId() {

		return clientId;
	}



}
