package tech.smartboot.mqtt.plugin.openapi;

import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

/**
 * Prometheus Histogram 指标。
 *
 * <p>线程安全，适用于统计请求耗时、消息处理耗时等指标。</p>
 */
public class HistogramMetric {

    private final String name;

    private final String help;

    private final double[] buckets;

    private final LongAdder[] bucketCounters;

    private final LongAdder count = new LongAdder();

    private final DoubleAdder sum = new DoubleAdder();

    public HistogramMetric(String name, String help, double[] buckets) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Histogram name must not be empty");
        }
        if (buckets == null || buckets.length == 0) {
            throw new IllegalArgumentException("Histogram buckets must not be empty");
        }

        this.name = name;
        this.help = help == null ? "" : help;
        this.buckets = Arrays.copyOf(buckets, buckets.length);

        for (int i = 0; i < this.buckets.length; i++) {
            if (Double.isNaN(this.buckets[i])
                    || Double.isInfinite(this.buckets[i])
                    || this.buckets[i] <= 0) {
                throw new IllegalArgumentException("Histogram bucket must be a positive finite number");
            }

            if (i > 0 && this.buckets[i] <= this.buckets[i - 1]) {
                throw new IllegalArgumentException("Histogram buckets must be sorted in ascending order");
            }
        }

        this.bucketCounters = new LongAdder[this.buckets.length];

        for (int i = 0; i < this.bucketCounters.length; i++) {
            this.bucketCounters[i] = new LongAdder();
        }
    }

    /**
     * 记录一次观测值。
     *
     * @param value 观测值，通常为秒
     */
    public void observe(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            return;
        }

        count.increment();
        sum.add(value);

        for (int i = 0; i < buckets.length; i++) {
            if (value <= buckets[i]) {
                bucketCounters[i].increment();
            }
        }
    }

    /**
     * 追加 Prometheus exposition format 内容。
     *
     * @param builder 输出缓冲区
     */
    public void appendPrometheus(StringBuilder builder) {
        builder.append("# HELP ")
                .append(name)
                .append(' ')
                .append(help)
                .append('\n');

        builder.append("# TYPE ")
                .append(name)
                .append(" histogram\n");

        for (int i = 0; i < buckets.length; i++) {
            builder.append(name)
                    .append("_bucket{le=\"")
                    .append(formatNumber(buckets[i]))
                    .append("\"} ")
                    .append(bucketCounters[i].sum())
                    .append('\n');
        }

        builder.append(name)
                .append("_bucket{le=\"+Inf\"} ")
                .append(count.sum())
                .append('\n');

        builder.append(name)
                .append("_count ")
                .append(count.sum())
                .append('\n');

        builder.append(name)
                .append("_sum ")
                .append(sum.sum())
                .append('\n');
    }

    private String formatNumber(double value) {
        if (value == Double.POSITIVE_INFINITY) {
            return "+Inf";
        }

        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }

        return Double.toString(value);
    }

    /**
     * 简单的线程安全 Double 累加器。
     */
    private static class DoubleAdder {

        private double value;

        public synchronized void add(double delta) {
            value += delta;
        }

        public synchronized double sum() {
            return value;
        }
    }
}