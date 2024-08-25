package me.xuqu.palmx.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.qos.QoSHandler;
import me.xuqu.palmx.registry.RegistryDTO;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static me.xuqu.palmx.serialize.Serialization.serialize;

/**
 * 用于操作 Zookeeper 的工具类
 */
@Slf4j
@UtilityClass
public class CuratorUtils {

    /**
     * Zookeeper Client
     */
    private CuratorFramework curatorFramework;

    /**
     * 判断节点是否存在
     *
     * @param serviceName    服务名
     * @param ServiceAddress 服务的地址
     * @return 是否存在布尔值
     */
    public boolean existsNode(String serviceName, String ServiceAddress) {
        try {
            return getClient().checkExists().forPath(buildNodePath(serviceName, ServiceAddress)) != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据服务名和地址创建一个结点
     *
     * @param serviceName    服务名
     * @param serviceAddress 服务的地址
     * @param createMode     创建结点的模式
     */
    public void createNode(String serviceName, String serviceAddress, CreateMode createMode) {
        // 节点的绝对路径，比如 /palmx/me.xuqu.service.FooService/192.168.13.13:8080
        String nodePath = buildNodePath(serviceName, serviceAddress);
        try {
            // 若当前要创建的节点已经存在了则直接返回
            if (existsNode(serviceName, serviceAddress)) {
                log.warn("Node exists for {}", nodePath);
                return;
            }

            // 构建注册节点对象
            int serializationType = PalmxConfig.getSerializationType().ordinal();
            RegistryDTO registryDTO = new RegistryDTO();
            registryDTO.setProtocol("http3");
            registryDTO.setHost(serviceAddress);
            registryDTO.setPort(8080);
            registryDTO.setServiceName(serviceName);
            // 获取本地的数据指标对象
            int localQoSLevel = QoSHandler.getLocalQoSLevel();
            registryDTO.setQoSLevel(localQoSLevel);
            registryDTO.setTmRegistry(new Date());
            registryDTO.setTmRefresh(new Date());
            byte[] registryDTOByte = serialize((byte) serializationType, registryDTO);
            // 创建节点
            getClient().create()
                    .creatingParentsIfNeeded()
                    .withMode(createMode)
                    .forPath(nodePath, registryDTOByte);
            log.debug("Create node[{}], path = {} success", createMode, nodePath);
        } catch (Exception e) {
            log.error("Create node[{}], path = {} failed {}", createMode, nodePath, e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateZNode(String path, byte[] dataBytes) throws Exception {
        if (getClient().checkExists().forPath(path) != null) {
            getClient().setData().forPath(path, dataBytes);
        } else {
            getClient().create().creatingParentsIfNeeded().forPath(path, dataBytes);
        }
        System.out.println("Updated node " + path + " with data: " + Arrays.toString(dataBytes));
    }

    /**
     * 创建临时的节点
     */
    public void createEphemeralNode(String serviceName, String ServiceAddress) {
        createNode(serviceName, ServiceAddress, CreateMode.EPHEMERAL);
    }

    /**
     * 永久性的创建节点
     */
    public void createPersistentNode(String serviceName, String ServiceAddress) {
        createNode(serviceName, ServiceAddress, CreateMode.PERSISTENT);
    }

    /**
     * 根据服务名和地址删除节点
     */
    public void deleteNode(String serviceName, String ServiceAddress) {
        try {
            String nodePath = buildNodePath(serviceName, ServiceAddress);
            getClient().delete().forPath(nodePath);
            log.info("Delete node, path = {}", nodePath);
        } catch (Exception e) {
            log.warn("{}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据服务名获取相关节点
     *
     * @param serviceName 服务名
     * @return 节点列表
     */
    public List<String> getChildrenNodes(String serviceName) {
        try {
            String nodePath = buildNodePath(serviceName);
            List<String> services = getClient().getChildren().forPath(nodePath);
            log.debug("Get children nodes from zookeeper, path = {}, result = {}", nodePath, services);
            return services;
        } catch (Exception e) {
            if (e.getClass() == KeeperException.NoNodeException.class) {
                return Collections.emptyList();
            }
            log.error("Get children nodes failed, {}", e.getMessage());
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    /**
     * 关闭当前在使用的连接
     */
    public void close() {
        if (curatorFramework != null && curatorFramework.getState() == CuratorFrameworkState.STARTED) {
            curatorFramework.close();
        }
        log.info("Zookeeper connection has been close");
    }

    /**
     * 获取 Curator Framework 对象
     *
     * @return 单例对象（不严谨）
     */
    public static CuratorFramework getClient() {

        if (curatorFramework == null || curatorFramework.getState() == CuratorFrameworkState.STOPPED) {
            // 最多尝试三次
            RetryNTimes retryNTimes = new RetryNTimes(3, 3000);
            curatorFramework = CuratorFrameworkFactory.newClient(PalmxConfig.getZookeeperAddress(), retryNTimes);
            curatorFramework.start();

            log.info("Starting Curator Framework");

            try {
                if (!curatorFramework.blockUntilConnected(30, TimeUnit.SECONDS)) {
                    log.error("Zookeeper[{}, Timeout] has connect failed", PalmxConfig.getZookeeperAddress());
                    throw new RuntimeException("Timeout waiting to connect ZooKeeper");
                }
                log.info("Zookeeper[{}] has connected, {}", PalmxConfig.getZookeeperAddress(), curatorFramework.getState());
                Runtime.getRuntime().addShutdownHook(new Thread(CuratorUtils::close));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return curatorFramework;
    }

    public String buildNodePath(String serviceName) {
        String zookeeperRootNode = PalmxConfig.getZookeeperRootNode();
        String path = String.format("%s/%s", zookeeperRootNode, serviceName);
        if (path.startsWith("/")) {
            return path;
        }

        // 把前缀 / 加上
        return "/" + path;
    }

    /**
     * 通过服务名和地址生成节点路径
     *
     * @param serviceName    服务名，如 me.xuqu.service.FooService
     * @param serviceAddress 服务地址，如 192.168.16.16:8080
     * @return 绝对路径
     */
    public String buildNodePath(String serviceName, String serviceAddress) {
        String zookeeperRootNode = PalmxConfig.getZookeeperRootNode();
        String path = String.format("%s/%s/%s", zookeeperRootNode, serviceName, serviceAddress);
        if (path.startsWith("/")) {
            return path;
        }
        // 把前缀 / 加上
        return "/" + path;
    }
}
