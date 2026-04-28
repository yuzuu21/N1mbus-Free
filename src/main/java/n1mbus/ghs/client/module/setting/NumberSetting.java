package n1mbus.ghs.client.module.setting;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;

    public NumberSetting(String name, double defaultValue, double min, double max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }

    @Override
    public void setValue(Double value) {
        super.setValue(Math.max(min, Math.min(max, value)));
    }
}
