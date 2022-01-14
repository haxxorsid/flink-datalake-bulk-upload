package com.flink.operator;


import com.flink.app.Image;
import com.flink.app.ImageWithCount;
import org.apache.flink.api.common.functions.AggregateFunction;

public class CountingAggregator implements AggregateFunction<Image, ImageWithCount, ImageWithCount> {

    @Override
    public ImageWithCount createAccumulator() {
        return new ImageWithCount();
    }

    @Override
    public ImageWithCount add(final Image value, final ImageWithCount accumulator) {
        accumulator.setImage(value);
        accumulator.setCount(accumulator.getCount() + 1);
        return accumulator;
    }

    @Override
    public ImageWithCount getResult(final ImageWithCount accumulator) {
        return accumulator;
    }

    @Override
    public ImageWithCount merge(final ImageWithCount a, final ImageWithCount b) {
        a.setCount(b.getCount());
        return a;
    }
}