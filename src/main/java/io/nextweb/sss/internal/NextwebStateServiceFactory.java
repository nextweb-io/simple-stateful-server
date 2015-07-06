package io.nextweb.sss.internal;

import delight.concurrency.jre.JreConcurrency;
import delight.factories.Configuration;
import delight.factories.Dependencies;
import delight.factories.Factory;

import io.nextweb.sss.NextwebStateServiceConfiguration;

public class NextwebStateServiceFactory implements Factory<DefaulStatefulService, NextwebStateServiceConfiguration, Dependencies> {

	@Override
	public boolean canInstantiate(Configuration conf) {
		return conf instanceof NextwebStateServiceConfiguration;
	}

	@Override
	public DefaulStatefulService create(NextwebStateServiceConfiguration conf,
			Dependencies dependencies) {
		return new DefaulStatefulService(conf, new JreConcurrency());
	}

}
