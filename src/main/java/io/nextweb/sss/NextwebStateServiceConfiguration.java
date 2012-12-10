package io.nextweb.sss;

public interface NextwebStateServiceConfiguration {

	/**
	 * Address of the node under which properties and logs will be stored.
	 * 
	 * @return
	 */
	public String getRootNodeUri();

	public String getRootNodeSecret();

}
