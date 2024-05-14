package gg.auroramc.auroralib.api.user.storage;

import com.google.common.util.concurrent.AtomicDouble;

public class LatencyMeasure {
    private final AtomicDouble count = new AtomicDouble(0.0D);
    private final AtomicDouble total = new AtomicDouble(0.0D);

    public void addLatency(double latency) {
        this.count.addAndGet(1.0D);
        this.total.addAndGet(latency);
    }

    public double getAverageLatency() {
        return this.total.get() / Math.max(1, this.count.get());
    }

    public String prettyPrint() {
        return String.format("%.2f ms", this.getAverageLatency() / 1000000.0D);
    }
}
