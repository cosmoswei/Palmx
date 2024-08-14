package me.xuqu.palmx.metric.pojo;

import lombok.Data;

@Data
public class JvmMetrics {

    // JVM GC 吞吐
    private double jvmGcThroughput;
    // JVM GC 时间
    private double jvmGcTime;
    // todo JVM STW 时间
    private double jvmStwTime;
    // JVM 内存使用情况
    private double jvmMemoryPoolUsage;
    // JVM堆内存分配速率
    private double jvmHeapMemoryAllocationRate;
    // JVM堆内存晋升速率
    private double jvmHeapMemoryPromotedRate;
    //
    private double liveThreads;

    private double peakThreads;

    private double blockedThreads;

    private double runnableThreads;

    private double newThreads;

    private double timedWaitingThreads;

    private double terminatedThreads;

    private double waitingThreads;

    /**
     * GC相关数据：
     * jvm.gc.memory.allocated{}: JVM每秒分配的内存量。
     * jvm.gc.memory.promoted{}: JVM每秒从年轻代提升到老年代的内存量。
     * jvm.gc.live.data.size{}: 当前JVM中的活跃数据大小，即垃圾回收后存活对象的大小。
     * jvm.gc.max.data.size{}: JVM中老年代可以使用的最大内存量。
     * 缓冲区相关数据：
     * jvm.buffer.count{id=mapped}: 当前映射缓冲区的数量。
     * jvm.buffer.count{id=direct}: 当前直接缓冲区的数量。
     * jvm.buffer.memory.used{id=direct}: 已使用的直接缓冲区内存。
     * jvm.buffer.memory.used{id=mapped}: 已使用的映射缓冲区内存。
     * jvm.buffer.total.capacity{id=mapped}: 映射缓冲区的总容量。
     * jvm.buffer.total.capacity{id=direct}: 直接缓冲区的总容量。
     * 内存使用相关数据：
     * jvm.memory.committed{area=heap,id=PS Old Gen}: 已提交的老年代堆内存大小。
     * jvm.memory.committed{area=heap,id=PS Eden Space}: 已提交的Eden区堆内存大小。
     * jvm.memory.max{area=heap,id=PS Old Gen}: 老年代堆内存的最大可用大小。
     * jvm.memory.used{area=heap,id=PS Eden Space}: Eden区堆内存的已使用大小。
     * jvm.memory.used{area=nonheap,id=Metaspace}: 非堆内存（元数据空间）的已使用大小。
     */

}
