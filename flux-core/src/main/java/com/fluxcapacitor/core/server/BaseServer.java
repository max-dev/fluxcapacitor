package com.fluxcapacitor.core.server;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluxcapacitor.core.AppConfiguration;
import com.fluxcapacitor.core.netty.NettyHandlerContainer;
import com.fluxcapacitor.core.netty.NettyServer;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.blitz4j.LoggingConfiguration;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;

/**
 * @author cfregly
 */
public class BaseServer implements Closeable {
	private static final Logger logger = LoggerFactory
			.getLogger(BaseServer.class);

	public NettyServer server;

	protected final AppConfiguration config;

	@Inject
	public BaseServer(AppConfiguration config) {
		this.config = config;
	}

	public void start() {
		LoggingConfiguration.getInstance().configure();

		PackagesResourceConfig rcf = new PackagesResourceConfig(
				config.getString("jersey.resources.package",
						"not-found-in-configuration"));

		server = NettyServer
				.builder()
				.host("0.0.0.0")
				// listen on any interface
				.port(config.getInt("netty.http.port", Integer.MIN_VALUE))
				.addHandler(
						"jerseyHandler",
						ContainerFactory.createContainer(
								NettyHandlerContainer.class, rcf))
				.numBossThreads(NettyServer.cpus)
				.numWorkerThreads(NettyServer.cpus * 4).build();

		// Register with Eurekas
		MyDataCenterInstanceConfig instanceConfig = new MyDataCenterInstanceConfig();

		ApplicationInfoManager.getInstance().initComponent(instanceConfig);
		ApplicationInfoManager.getInstance().setInstanceStatus(
				InstanceStatus.UP);

		// Note: If running locally on MacOS, you'll need to make sure your
		// /etc/hosts file contains the following entry:
		// 127.0.0.1 <hostname>.local
		// or else clients will likely not be able to connect.
		DiscoveryManager.getInstance().initComponent(instanceConfig,
				new DefaultEurekaClientConfig());
	}

	@Override
	public void close() {
		Closeables.closeQuietly(server);
		LoggingConfiguration.getInstance().stop();
	}
}
