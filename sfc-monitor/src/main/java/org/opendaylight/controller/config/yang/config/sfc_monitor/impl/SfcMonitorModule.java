package org.opendaylight.controller.config.yang.config.sfc_monitor.impl;


import org.opendaylight.monitor.provider.provider.MonitorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcMonitorModule extends org.opendaylight.controller.config.yang.config.sfc_monitor.impl.AbstractSfcMonitorModule {
    private static final Logger LOG = LoggerFactory.getLogger(MonitorProvider.class);

    public SfcMonitorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SfcMonitorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sfc_monitor.impl.SfcMonitorModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("SFC Monitor initializing");

//        NotificationProviderService notificationService = getNotificationServiceDependency();

        final MonitorProvider monitorProvider = new MonitorProvider(getDataBrokerDependency(), getNotificationServiceDependency());

        java.lang.AutoCloseable ret = new java.lang.AutoCloseable() {
            @Override
            public void close() throws Exception {
                monitorProvider.close();
            }
        };

        LOG.info("SFC Monitor initialized: (instance {})", ret);

        return ret;
    }

}
