package io.nextweb.sss.internal;

import io.nextweb.Link;
import io.nextweb.Node;
import io.nextweb.Query;
import io.nextweb.Session;
import io.nextweb.fn.Closure;
import io.nextweb.fn.ExceptionListener;
import io.nextweb.fn.ExceptionResult;
import io.nextweb.fn.Result;
import io.nextweb.fn.Success;
import io.nextweb.jre.Nextweb;
import io.nextweb.operations.exceptions.ImpossibleListener;
import io.nextweb.operations.exceptions.ImpossibleResult;
import io.nextweb.operations.exceptions.UndefinedListener;
import io.nextweb.operations.exceptions.UndefinedResult;
import io.nextweb.sss.NextwebStateServiceConfiguration;
import de.mxro.server.ShutdownCallback;
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

		final Query messagesNode = root.select("./" + path, "messages");

		final Query appendSafe = messagesNode.appendSafe(title).appendSafe(
				message);

		appendSafe.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		appendSafe.catchImpossible(new ImpossibleListener() {

			@Override
			public void onImpossible(final ImpossibleResult ir) {
				if (ir.cause().equals("nodewithaddressalreadydefined")) {
					log(path, title, message, callback);
					return;
				}
			}
		});

		appendSafe.get(new Closure<Node>() {

			@Override
			public void apply(final Node o) {
				callback.onLogged();
			}
		});
	}

	@Override
	public void setProperty(final String path, final Object value,
			final SetPropertyCallback callback) {
		final Query setValueSafe = root.select("./" + path, value)
				.setValueSafe(value);

		setValueSafe.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		setValueSafe.get(new Closure<Node>() {

			@Override
			public void apply(final Node o) {
				callback.onPropertySet();
			}
		});
	}

	@Override
	public void getProperty(final String path,
			final GetPropertyCallback callback) {
		getProperty(path, null, callback);
	}

	@Override
	public void getProperty(final String path, final Object defaultValue,
			final GetPropertyCallback callback) {
		final Query select;
		if (defaultValue == null) {
			select = root.select("./" + path);
		} else {
			select = root.select("./" + path, defaultValue);
		}

		select.catchUndefined(new UndefinedListener() {

			@Override
			public void onUndefined(final UndefinedResult r) {
				callback.onPropertyDoesNotExist();
			}
		});

		select.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		select.get(new Closure<Node>() {

			@Override
			public void apply(final Node o) {
				callback.onPropertyRetrieved(o.value());
			}
		});
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

	@Override
	public void shutdown(final ShutdownCallback callback) {
		final Result<Success> closeRequest = this.session.close();

		closeRequest.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		System.out.println("request close");
		closeRequest.get(new Closure<Success>() {

			@Override
			public void apply(final Success o) {
				System.out.println("close done.");
				callback.onShutdownComplete();
			}
		});

	}

}
