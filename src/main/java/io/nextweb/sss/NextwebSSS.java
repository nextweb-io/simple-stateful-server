package io.nextweb.sss;

import io.nextweb.sss.internal.DefaulStatefulService;

import java.io.PrintWriter;
import java.io.StringWriter;

import one.utils.concurrent.Concurrency;

import org.junit.Assert;

import de.mxro.server.contexts.StatefulContext;

public class NextwebSSS {

	public static StatefulContext createService(final Concurrency con,
			final NextwebStateServiceConfiguration conf) {
		return new DefaulStatefulService(conf, con);
	}

	// <!-- one.download
	// https://u1.linnk.it/qc8sbw/usr/apps/textsync/docs/java-stacktrace-to-string
	// -->
	/**
	 * <p>
	 * Printing the stack trace of an exception to to String.
	 * </p>
	 * <p>
	 * As proposed by Brian Agnew on <a
	 * href="http://stackoverflow.com/a/1149712/270662">stackoverflow</a>
	 * 
	 * @param t
	 * @return
	 */
	public static String stacktraceToString(final Throwable t) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);

		t.printStackTrace(pw);

		return sw.toString();
	}

	public static void main(final String[] args) {

		deepStackTrace(1000);
	}

	public static void deepStackTrace(final int goDown) {
		if (goDown == 0) {
			for (int i = 0; i <= 999; i++) {
				try {
					Assert.fail();
				} catch (final Throwable t) {
					System.out.println(stacktraceToString(t).length());
				}
			}
			return;
		}
		deepStackTrace(goDown - 1);

	}

}
