package io.nextweb.sss.internal;

import delight.async.AsyncCommon;
import delight.async.Operation;
import delight.async.callbacks.ValueCallback;
import delight.async.flow.CallbackLatch;
import delight.concurrency.Concurrency;
import delight.functional.Closure;
import delight.functional.Success;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.appjangle.api.Client;
import com.appjangle.api.Link;
import com.appjangle.api.LinkList;
import com.appjangle.api.LinkListQuery;
import com.appjangle.api.ListQuery;
import com.appjangle.api.Node;
import com.appjangle.api.NodeList;
import com.appjangle.api.Query;
import com.appjangle.api.engine.fn.IntegerResult;
import com.appjangle.api.jre.ClientsGeneralJre;

import de.mxro.server.contexts.GetPropertyCallback;
import de.mxro.server.contexts.LogCallback;
import de.mxro.server.contexts.SetPropertyCallback;
import de.mxro.server.contexts.StatefulContext;
import de.mxro.service.callbacks.ShutdownCallback;
import io.nextweb.promise.NextwebPromise;
import io.nextweb.promise.exceptions.ExceptionListener;
import io.nextweb.promise.exceptions.ExceptionResult;
import io.nextweb.promise.exceptions.ImpossibleListener;
import io.nextweb.promise.exceptions.ImpossibleResult;
import io.nextweb.promise.exceptions.UndefinedListener;
import io.nextweb.promise.exceptions.UndefinedResult;
import io.nextweb.sss.NextwebStateServiceConfiguration;

public class DefaulStatefulService implements StatefulContext {

    private final NextwebStateServiceConfiguration conf;
    private final Client session;
    private final Link root;
    private final Concurrency con;

    private final Set<String> scheduledToDelete;

    private void logInternal(final int depth, final String path, final String title, final String message,
            final LogCallback callback) {
        // System.out.println("logging " + path + " " + title);
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
                // System.out.println("Messages node retrieved. " + msgs.uri());
                final Query appendSafe;
                if (depth == 0) {
                    appendSafe = messagesNode.appendSafe(title).appendSafe(message);
                } else {
                    appendSafe = messagesNode.appendSafe(title, "./" + depth + Math.abs(this.hashCode())).appendSafe(
                            message);
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
                        System.out.println("IMPOSSILBE " + ir.message());
                        if (depth < 20

                                /* && ir.cause().equals("nodewithaddressalreadydefined") */) {
                            logInternal(depth + 1, path, title, message, callback);
                            return;
                        }
                        callback.onFailure(new Exception("Impossible to append log message: " + ir.message()));
                    }
                });

                // System.out.println("get append " + message);
                appendSafe.get(new Closure<Node>() {

                    @Override
                    public void apply(final Node o) {
                        checkForOverflow(callback, msgs);
                    }

                });

                session.commit().get(new Closure<Success>() {

                    @Override
                    public void apply(final Success o) {

                    }
                });
            }
        });

    }

    @Override
    public void log(final String path, final String title, final String message, final LogCallback callback) {
        // System.out.println("logging; " + path + " " + title);
        logInternal(0, path, title, message, callback);
    }

    private void checkForOverflow(final LogCallback callback, final Node msgs) {
        // System.out.println("check for overflow. " + msgs.uri());
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

                // System.out.println("message: " + msgs.uri());
                // System.out.println("messages: " + messageLinks.size());

                if (messageLinks.size() < conf.maxMessagesPerNode()) {
                    callback.onLogged();
                    return;
                }

                final List<String> firstLinks = messageLinks.uris().subList(0,
                        Math.max(10, messageLinks.size() - conf.maxMessagesPerNode()));

                final CallbackLatch latch = new CallbackLatch(firstLinks.size()) {

                    @Override
                    public void onFailed(final Throwable t) {
                        callback.onFailure(t);
                    }

                    @Override
                    public void onCompleted() {

                        final IntegerResult clearVersions = msgs.clearVersions(50);

                        clearVersions.catchExceptions(new ExceptionListener() {

                            @Override
                            public void onFailure(final ExceptionResult r) {
                                callback.onFailure(r.exception());
                            }
                        });

                        clearVersions.get(new Closure<Integer>() {

                            @Override
                            public void apply(final Integer o) {
                                callback.onLogged();
                            }
                        });

                    }
                };

                for (final String link : firstLinks) {

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

                            final List<Operation<?>> res = new ArrayList<Operation<?>>(nodeList.size() + 2);

                            for (final Node n : nodeList) {
                                res.add(child.removeSafe(n));
                            }

                            res.add(msgs.removeSafe(child));

                            res.add(msgs.clearVersions(conf.maxMessagesPerNode()));

                            AsyncCommon.parallel(res, new ValueCallback<List<Object>>() {

                                @Override
                                public void onFailure(final Throwable t) {
                                    scheduledToDelete.remove(link);
                                    latch.registerFail(t);
                                }

                                @Override
                                public void onSuccess(final List<Object> value) {

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
    public void setProperty(final String path, final Object value, final SetPropertyCallback callback) {
        final Query setValueSafe = root.select("./" + path, value).setValueSafe(value);

        setValueSafe.catchExceptions(new ExceptionListener() {

            @Override
            public void onFailure(final ExceptionResult r) {
                callback.onFailure(r.exception());
            }
        });

        setValueSafe.get(new Closure<Node>() {

            @Override
            public void apply(final Node o) {
                final IntegerResult clearVersions = setValueSafe.clearVersions(20);
                clearVersions.catchExceptions(new ExceptionListener() {

                    @Override
                    public void onFailure(final ExceptionResult r) {
                        callback.onFailure(r.exception());
                    }
                });

                clearVersions.get(new Closure<Integer>() {

                    @Override
                    public void apply(final Integer o) {
                        callback.onPropertySet();
                    }
                });

            }
        });
    }

    @Override
    public void getProperty(final String path, final GetPropertyCallback callback) {
        getProperty(path, null, callback);
    }

    @Override
    public void getProperty(final String path, final Object defaultValue, final GetPropertyCallback callback) {
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

    public DefaulStatefulService(final NextwebStateServiceConfiguration conf, final Concurrency con) {
        super();
        this.conf = conf;
        this.session = ClientsGeneralJre.createClient();
        this.root = this.session.node(conf.getRootNodeUri(), conf.getRootNodeSecret());
        this.con = con;
        this.scheduledToDelete = this.con.newCollection().newThreadSafeSet(String.class);

    }

    public NextwebStateServiceConfiguration getConfiguration() {
        return conf;
    }

    @Override
    public void shutdown(final ShutdownCallback callback) {
        final NextwebPromise<Success> closeRequest = this.session.close();

        closeRequest.catchExceptions(new ExceptionListener() {

            @Override
            public void onFailure(final ExceptionResult r) {
                callback.onFailure(r.exception());
            }
        });

        closeRequest.get(new Closure<Success>() {

            @Override
            public void apply(final Success o) {

                callback.onSuccess();
            }
        });

    }

}
