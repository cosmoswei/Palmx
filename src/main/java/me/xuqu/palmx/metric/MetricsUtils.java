package me.xuqu.palmx.metric;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.metric.pojo.Arith;
import me.xuqu.palmx.metric.pojo.JvmMetrics;
import me.xuqu.palmx.metric.pojo.NetworkMetrics;
import me.xuqu.palmx.metric.pojo.SystemMetrics;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MetricsUtils {

    private static final SystemInfo si = new SystemInfo();
    private static final HardwareAbstractionLayer hardware = si.getHardware();
    private static final CentralProcessor processor = hardware.getProcessor();

    private static MeterRegistry registry;

    public MetricsUtils(MeterRegistry registry) {
        MetricsUtils.registry = registry;
    }

    public double getCpuLoad() {
        double[] loadAverage = processor.getSystemLoadAverage(1);
        return loadAverage[0];
    }

    public double getCpuUsage() {
        /*
         * 在计算机系统中，CPU 时间可以被分为几类，通常包括：
         * User Time: CPU 花费在用户空间程序上的时间。
         * System Time: CPU 花费在内核空间（操作系统）的时间。
         * Idle Time: CPU 空闲的时间，表示没有任何任务运行时的时间。
         * Nice Time: CPU 花费在降低优先级的任务上的时间（在 Linux 系统上常见）。
         * I/O Wait Time: CPU 等待 I/O 操作完成的时间。
         * IRQ Time: CPU 花费在处理硬件中断上的时间。
         * SoftIRQ Time: CPU 花费在处理软件中断上的时间。
         * Steal Time：是虚拟化环境中特有的一个概念。它表示的是在虚拟机环境中，由于虚拟机监控程序（Hypervisor）将 CPU 资源分配给其他虚拟机，从而导致当前虚拟机未能获得 CPU 时间的那部分时间。
         */
        long[] startTicks = processor.getSystemCpuLoadTicks();
        long[] endTicks = processor.getSystemCpuLoadTicks();
        long user = endTicks[CentralProcessor.TickType.USER.getIndex()] - startTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = endTicks[CentralProcessor.TickType.NICE.getIndex()] - startTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = endTicks[CentralProcessor.TickType.SYSTEM.getIndex()] - startTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = endTicks[CentralProcessor.TickType.IDLE.getIndex()] - startTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long ioWait = endTicks[CentralProcessor.TickType.IOWAIT.getIndex()] - startTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = endTicks[CentralProcessor.TickType.IRQ.getIndex()] - startTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softIrq = endTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - startTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = endTicks[CentralProcessor.TickType.STEAL.getIndex()] - startTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + ioWait + irq + softIrq + steal;
        if (totalCpu == 0) {
            return 0;
        }
        long usedCpu = user + sys;
        return Arith.round(Arith.mul(Arith.div(usedCpu, totalCpu, 2), 100), 2);
    }

    /**
     * 内存负载的获取
     * 虽然内存负载不是一个直接可测量的指标，但你可以通过以下方式间接了解内存的负载情况：
     * <p>
     * 分页活动：监控交换区（swap）的使用情况。
     * <p>
     * 当物理内存耗尽时，操作系统会使用交换区，将一部分内存数据交换到硬盘上。频繁的分页活动意味着内存负载较高。
     * 内存碎片化：操作系统的内存管理机制。
     * <p>
     * 内存碎片化可能导致系统在需要连续的内存块时，无法充分利用内存，导致性能下降。
     * 内存延迟：内存访问的延迟时间。
     * <p>
     * 如果内存访问延迟较高，可能意味着内存负载较高。
     * 这些指标通常需要更深入的系统工具或监控系统来获取和分析。
     */
    public double getMemorySwapSize() {
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        return (double) memory.getVirtualMemory().getSwapTotal() / 1024 / 1024;
    }

    public double getMemorySwapUsage() {
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long swapTotal = memory.getVirtualMemory().getSwapTotal();
        long swapUsed = memory.getVirtualMemory().getSwapUsed();
        return (double) swapUsed / swapTotal * 100;
    }

    public double getMemoryUsage() {
        GlobalMemory memory = hardware.getMemory();
        // 总内存
        long totalMemory = memory.getTotal();
        // 可用内存
        long availableMemory = memory.getAvailable();
        // 使用的内存
        long usedMemory = totalMemory - availableMemory;
        // 内存使用率
        return (double) usedMemory / totalMemory * 100;
    }

    // todo
    public double getDiskLoad() {
        return 1;
    }

    // todo
    public double getDiskIOLoad() {
        return 1;
    }

    public SystemMetrics getSystemMetrics() {
        SystemMetrics systemMetrics = new SystemMetrics();
        systemMetrics.setCpuLoad(getCpuLoad());
        systemMetrics.setCpuUsage(getCpuUsage());
        systemMetrics.setMemoryLoad(getMemoryUsage());
        systemMetrics.setMemoryUsage(getMemoryUsage());
        systemMetrics.setDiskLoad(getDiskLoad());
        systemMetrics.setDiskIOLoad(getDiskIOLoad());
        return systemMetrics;
    }

    // todo
    public NetworkMetrics getNetworkMetrics() {
        return null;
    }

    public double getCpuUsage2() {
        return registry.get("system.cpu.usage").gauge().value();
    }

    public double getJvmMemoryUsed() {
        return registry.get("jvm.memory.used")
                .tag("area", "heap")
                .tag("id", "PS Eden Space")
                .gauge().value();

    }

    public JvmMetrics getJvmMetrics() {
        JvmMetrics jvmMetrics = new JvmMetrics();
        Timer gcPauseTimer;
        try {
            // 查询 GC 暂停时间的 Timer
            gcPauseTimer = registry.get("jvm.gc.pause")
                    .tag("action", "end of minor GC")
                    .tag("cause", "System.gc()")
                    .timer();
            // 获取暂停时间的统计值
            double meanPauseTime = gcPauseTimer.mean(TimeUnit.SECONDS); // 平均暂停时间 (秒)
            double maxPauseTime = gcPauseTimer.max(TimeUnit.SECONDS); // 最大暂停时间 (秒)
            // 输出暂停时间的统计值
            double throughput = (1 / (1 + meanPauseTime)) * 100;
            jvmMetrics.setJvmGcThroughput(throughput);
            jvmMetrics.setJvmGcTime(maxPauseTime);
            jvmMetrics.setJvmStwTime(0.0D);
        } catch (Exception e) {
            log.info("no gc event");
        }

        // thread
        double liveThreads = registry.get("jvm.threads.live").gauge().value();
        jvmMetrics.setLiveThreads(liveThreads);
        double peakThreads = registry.get("jvm.threads.peak").gauge().value();
        jvmMetrics.setPeakThreads(peakThreads);
        double blockedThreads = registry.get("jvm.threads.states").tag("state", "blocked").gauge().value();
        jvmMetrics.setBlockedThreads(blockedThreads);
        double runnableThreads = registry.get("jvm.threads.states").tag("state", "runnable").gauge().value();
        jvmMetrics.setRunnableThreads(runnableThreads);
        double newThreads = registry.get("jvm.threads.states").tag("state", "new").gauge().value();
        jvmMetrics.setNewThreads(newThreads);
        double timedWaitingThreads = registry.get("jvm.threads.states").tag("state", "timed-waiting").gauge().value();
        jvmMetrics.setTimedWaitingThreads(timedWaitingThreads);
        double terminatedThreads = registry.get("jvm.threads.states").tag("state", "terminated").gauge().value();
        jvmMetrics.setTerminatedThreads(terminatedThreads);
        double waitingThreads = registry.get("jvm.threads.states").tag("state", "waiting").gauge().value();
        jvmMetrics.setWaitingThreads(waitingThreads);
        // 堆内存
        jvmMetrics.setJvmMemoryPoolUsage(getHeadUsedRate(registry));
        // GC
        jvmMetrics.setJvmHeapMemoryAllocationRate(getAllocationRate(registry));
        jvmMetrics.setJvmHeapMemoryPromotedRate(getPromotedRate(registry));
        return jvmMetrics;

    }

    private double getAllocationRate(MeterRegistry registry) {
        double memoryAllocated = registry.get("jvm.gc.memory.allocated").counter().count();
        return memoryAllocated / 1024 / 1024;
    }

    private double getPromotedRate(MeterRegistry registry) {
        double memoryPromoted = registry.get("jvm.gc.memory.promoted").counter().count();
        return memoryPromoted / 1024 / 1024;
    }

    private static double getHeadUsedRate(MeterRegistry registry) {
        // 获取JVM内存使用指标
        double edenSpaceUsed = registry.get("jvm.memory.used")
                .tags("area", "heap", "id", "PS Eden Space")
                .gauge()
                .value();

        double edenSpaceMax = registry.get("jvm.memory.max")
                .tags("area", "heap", "id", "PS Eden Space")
                .gauge()
                .value();

        double survivorSpaceUsed = registry.get("jvm.memory.used")
                .tags("area", "heap", "id", "PS Survivor Space")
                .gauge()
                .value();

        double survivorSpaceMax = registry.get("jvm.memory.max")
                .tags("area", "heap", "id", "PS Survivor Space")
                .gauge()
                .value();


        double oldGenUsed = registry.get("jvm.memory.used")
                .tags("area", "heap", "id", "PS Old Gen")
                .gauge()
                .value();

        double oldGenMax = registry.get("jvm.memory.max")
                .tags("area", "heap", "id", "PS Old Gen")
                .gauge()
                .value();
        log.info("Eden Space Used: {} MB", edenSpaceUsed / 1014 / 1024);
        log.info("Eden Space Max: {} MB", edenSpaceMax / 1014 / 1024);
        log.info("Survivor Space Used: {} MB", survivorSpaceUsed / 1014 / 1024);
        log.info("Survivor Space Max: {} MB", survivorSpaceMax / 1014 / 1024);
        log.info("Old Gen Used: {} MB", oldGenUsed / 1014 / 1024);
        log.info("Old Gen Max: {} MB", oldGenMax / 1014 / 1024);
        return (edenSpaceUsed + survivorSpaceUsed + oldGenUsed) / (edenSpaceMax + survivorSpaceMax + oldGenMax) * 100;
    }
}
