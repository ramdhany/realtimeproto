/**
 * 
 */
package uk.co.rajivr.kata;

/**
 * @author rajivr
 *
 */
public interface IDataStore {
	
	
	void listPush(String key, long value);
	
	long listPop(String key);

}
