package io.nextweb.sss;

import io.nextweb.sss.internal.DefaulStatefulService;
import de.mxro.server.contexts.StatefulContext;

public class NextwebSSS {

	public static StatefulContext createService(
			final NextwebStateServiceConfiguration conf) {
		return new DefaulStatefulService(conf);
	}
}
