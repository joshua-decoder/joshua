package joshua.discriminative.semiring_parsingv2.pmodule;

import java.util.Collection;

import joshua.discriminative.semiring_parsingv2.SignedValue;


/*like a hashmap, where the key is the ID
 * */

//the type of ID is always integer
//V: type of value
public interface SparseArray<V,M> {
	
	public Collection<Integer> getIds();
	
	public Collection<SignedValue> getValues();

	/**return null if the id does not exist
	 **/
	public V getValueAt(int id);
	
	//public void addInToArray(int id, V val);
	
	public M duplicate();
	
	public void add(M b);
	
}
