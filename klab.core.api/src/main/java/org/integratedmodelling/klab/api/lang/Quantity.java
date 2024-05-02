package org.integratedmodelling.klab.api.lang;

import org.integratedmodelling.klab.api.collections.Literal;

import java.io.Serializable;

/**
 * Just a number with units.
 * 
 * @author ferdinando.villa
 *
 */
public interface Quantity extends Literal {
	
	/**
	 * May be an integer or a double.
	 * 
	 * @return
	 */
	Number getValue();

	/**
	 * Unvalidated unit as a string.
	 * 
	 * @return
	 */
	String getUnit();
	
	/**
	 * 
	 * @return
	 */
	String getCurrency();
	
	static Quantity parse(String specification) {
	    // TODO
	    return null;
	}
}
