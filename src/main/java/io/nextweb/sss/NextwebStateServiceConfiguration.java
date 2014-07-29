package io.nextweb.sss;

import de.mxro.factories.Configuration;

public interface NextwebStateServiceConfiguration extends Configuration {

	/**
	 * Address of the node under which properties and logs will be stored.
	 * 
	 * @return
	 */
	public String getRootNodeUri();

	public String getRootNodeSecret();

	/**
	 * What is the maximum number of log messages to be kept for a path?
	 * 
	 * @return
	 */
	public int maxMessagesPerNode();

}
