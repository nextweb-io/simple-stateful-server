package io.nextweb.sss.internal;

import io.nextweb.sss.NextwebStateServiceConfiguration;
import de.mxro.server.contexts.GetPropertyCallback;
import de.mxro.server.contexts.LogCallback;
import de.mxro.server.contexts.SetPropertyCallback;
import de.mxro.server.contexts.StatefulContext;

public class DefaulStatefulService implements StatefulContext {

	final NextwebStateServiceConfiguration conf;

	@Override
	public void log(final String path, final String title,
			final String message, final LogCallback callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setProperty(final String path, final Object value,
			final SetPropertyCallback callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public void getProperty(final String path,
			final GetPropertyCallback callback) {
		// TODO Auto-generated method stub

	}

	public DefaulStatefulService(final NextwebStateServiceConfiguration conf) {
		super();
		this.conf = conf;
	}

}
