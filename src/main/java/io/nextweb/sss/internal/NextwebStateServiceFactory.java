package io.nextweb.sss.internal;

import io.nextweb.sss.NextwebStateServiceConfiguration;
import de.mxro.factories.Configuration;
import de.mxro.factories.Dependencies;
import de.mxro.factories.Factory;

public class NextwebStateServiceFactory implements Factory<DefaulStatefulService, NextwebStateServiceConfiguration, Dependencies> {

	@Override
	public boolean canInstantiate(Configuration conf) {
		return conf instanceof NextwebStateServiceConfiguration;
	}

	@Override
	public DefaulStatefulService create(NextwebStateServiceConfiguration conf,
			Dependencies dependencies) {
		return new ;
	}

}
