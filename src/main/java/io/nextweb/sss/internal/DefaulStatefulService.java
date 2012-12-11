package io.nextweb.sss.internal;

import io.nextweb.Link;
import io.nextweb.Session;
import io.nextweb.jre.Nextweb;
import io.nextweb.sss.NextwebStateServiceConfiguration;
import de.mxro.server.contexts.GetPropertyCallback;
import de.mxro.server.contexts.LogCallback;
import de.mxro.server.contexts.SetPropertyCallback;
import de.mxro.server.contexts.StatefulContext;

public class DefaulStatefulService implements StatefulContext {

	private final NextwebStateServiceConfiguration conf;
	private final Session session;
	private final Link root;

	@Override
	public void log(final String path, final String title,
			final String message, final LogCallback callback) {
		
		
		pl[p]
	}

	@Override
	public void setProperty(final String path, final Object value,
			final SetPropertyCallback callback) {

	}

	@Override
	public void getProperty(final String path,
			final GetPropertyCallback callback) {

	}

	public DefaulStatefulService(final NextwebStateServiceConfiguration conf) {
		super();
		this.conf = conf;
		this.session = Nextweb.createSession();
		this.root = this.session.node(conf.getRootNodeUri(),
				conf.getRootNodeSecret());

	}

	public NextwebStateServiceConfiguration getConfiguration() {
		return conf;
	}

}
