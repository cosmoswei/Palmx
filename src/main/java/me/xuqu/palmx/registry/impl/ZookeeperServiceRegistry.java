package me.xuqu.palmx.registry.impl;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.registry.AbstractServiceRegistry;
import me.xuqu.palmx.registry.RegistryDTO;
import me.xuqu.palmx.util.CuratorUtils;

import java.util.List;

@Slf4j
public class ZookeeperServiceRegistry extends AbstractServiceRegistry {

    @Override
    protected void doRegister(String serviceName, String serviceAddress) {
        CuratorUtils.createEphemeralNode(serviceName, serviceAddress);
        log.info("Register a service[{}, {}] to zookeeper", serviceName, serviceAddress);
    }

    @Override
    public void unregister(String serviceName, PalmxSocketAddress inetSocketAddress) {

    }

    @Override
    protected List<RegistryDTO> doLookup(String serviceName) {
        List<RegistryDTO> childrenNodes = CuratorUtils.getChildrenNodes(serviceName);
        log.debug("Get services[name = {}] from zookeeper, {}", serviceName, childrenNodes);
        return childrenNodes;
    }
}
