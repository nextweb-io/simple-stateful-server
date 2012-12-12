package io.nextweb.sss.internal;

import io.nextweb.Link;
import io.nextweb.LinkList;
import io.nextweb.LinkListQuery;
import io.nextweb.ListQuery;
import io.nextweb.Node;
import io.nextweb.NodeList;
import io.nextweb.Query;
import io.nextweb.Session;
import io.nextweb.fn.BasicResult;
import io.nextweb.fn.Closure;
import io.nextweb.fn.ExceptionListener;
import io.nextweb.fn.ExceptionResult;
import io.nextweb.fn.Result;
import io.nextweb.fn.Success;
import io.nextweb.fn.SuccessFail;
import io.nextweb.jre.Nextweb;
import io.nextweb.operations.exceptions.ImpossibleListener;
import io.nextweb.operations.exceptions.ImpossibleResult;
import io.nextweb.operations.exceptions.UndefinedListener;
import io.nextweb.operations.exceptions.UndefinedResult;
import io.nextweb.sss.NextwebStateServiceConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import one.async.joiner.CallbackLatch;
import one.utils.concurrent.Concurrency;
import de.mxro.server.ShutdownCallback;
import de.mxro.server.contexts.GetPropertyCallback;
import de.mxro.server.contexts.LogCallback;
import de.mxro.server.contexts.SetPropertyCallback;
import de.mxro.server.contexts.StatefulContext;

public class DefaulStatefulService implements StatefulContext {

	private final NextwebStateServiceConfiguration conf;
	private final Session session;
	private final Link root;
	private final Concurrency con;

	private final Set<String> scheduledToDelete;

	private void logInternal(final int depth, final String path,
			final String title, final String message, final LogCallback callback) {

		final Query messagesNode = root.select("./" + path, "messages");

		messagesNode.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		messagesNode.get(new Closure<Node>() {

			@Override
			public void apply(final Node msgs) {
				final Query appendSafe;
				if (depth == 0) {
					appendSafe = messagesNode.appendSafe(title).appendSafe(
							message);
				} else {
					appendSafe = messagesNode.appendSafe(title, "./" + depth
							+ Math.abs(this.hashCode()));
				}

				appendSafe.catchExceptions(new ExceptionListener() {

					@Override
					public void onFailure(final ExceptionResult r) {
						callback.onFailure(r.exception());
					}
				});

				appendSafe.catchImpossible(new ImpossibleListener() {

					@Override
					public void onImpossible(final ImpossibleResult ir) {
						if (depth < 10
								&& ir.cause().equals(
										"nodewithaddressalreadydefined")) {
							logInternal(depth + 1, path, title, message,
									callback);
							return;
						}
						callback.onFailure(new Exception(
								"Impossible to append log message: "
										+ ir.message()));
					}
				});

				appendSafe.get(new Closure<Node>() {

					@Override
					public void apply(final Node o) {

						checkForOverflow(callback, msgs);

					}

				});
			}
		});

	}

	@Override
	public void log(final String path, final String title,
			final String message, final LogCallback callback) {
		logInternal(0, path, title, message, callback);
	}

	private void checkForOverflow(final LogCallback callback, final Node msgs) {
		final LinkListQuery selectAllLinks = msgs.selectAllLinks();

		selectAllLinks.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		selectAllLinks.get(new Closure<LinkList>() {

			@Override
			public void apply(final LinkList messageLinks) {

				if (messageLinks.size() < conf.maxMessagesPerNode()) {
					callback.onLogged();
					return;
				}

				final List<String> firstTenLinks = messageLinks.uris().subList(
						0, 10);

				final CallbackLatch latch = new CallbackLatch(firstTenLinks
						.size()) {

					@Override
					public void onFailed(final Throwable t) {
						callback.onFailure(t);
					}

					@Override
					public void onCompleted() {
						callback.onLogged();
					}
				};

				for (final String link : firstTenLinks) {

					synchronized (scheduledToDelete) {
						if (scheduledToDelete.contains(link)) {
							latch.registerSuccess();
							continue;
						}
						scheduledToDelete.add(link);
					}

					final Link child = session.node(link);

					final ListQuery getQuery = child.selectAll();

					getQuery.catchExceptions(new ExceptionListener() {

						@Override
						public void onFailure(final ExceptionResult r) {
							scheduledToDelete.remove(link);
							latch.registerFail(r.exception());
						}
					});

					getQuery.get(new Closure<NodeList>() {

						@Override
						public void apply(final NodeList nodeList) {

							final List<BasicResult<?>> res = new ArrayList<BasicResult<?>>(
									nodeList.size() + 2);

							for (final Node n : nodeList) {
								res.add(child.removeSafe(n));
							}

							res.add(msgs.removeSafe(child));

							res.add(msgs.clearVersions(conf
									.maxMessagesPerNode()));

							final Result<SuccessFail> getAll = session.getAll(
									true,
									res.toArray(new BasicResult[res.size()]));

							getAll.catchExceptions(new ExceptionListener() {

								@Override
								public void onFailure(final ExceptionResult r) {
									scheduledToDelete.remove(link);
									latch.registerFail(r.exception());
								}
							});

							getAll.get(new Closure<SuccessFail>() {

								@Override
								public void apply(final SuccessFail o) {
									if (o.isFail()) {
										scheduledToDelete.remove(link);
										latch.registerFail(o.getException());
										return;
									}

									scheduledToDelete.remove(link);
									latch.registerSuccess();
								}
							});

						}
					});

				}

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

	public DefaulStatefulService(final NextwebStateServiceConfiguration conf,
			final Concurrency con) {
		super();
		this.conf = conf;
		this.session = Nextweb.createSession();
		this.root = this.session.node(conf.getRootNodeUri(),
				conf.getRootNodeSecret());
		this.con = con;
		this.scheduledToDelete = this.con.newCollection().newThreadSafeSet(
				String.class);

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

		closeRequest.get(new Closure<Success>() {

			@Override
			public void apply(final Success o) {

				callback.onShutdownComplete();
			}
		});

	}

}
