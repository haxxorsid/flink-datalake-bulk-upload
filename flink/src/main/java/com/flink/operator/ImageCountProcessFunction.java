package com.flink.operator;

import com.flink.app.ImageWithCount;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

// ProcessWindowFunction access tumbling window end time and assign it in the ImageWithCount object.
public class ImageCountProcessFunction
        extends ProcessWindowFunction<ImageWithCount, ImageWithCount, String, TimeWindow> {

    @Override
    public void process(String s, ProcessWindowFunction<ImageWithCount, ImageWithCount, String, TimeWindow>.Context context, Iterable<ImageWithCount> iterable, Collector<ImageWithCount> collector) throws Exception {
        ImageWithCount imgWithCount = iterable.iterator().next();
        imgWithCount.setTimeStamp(context.window().getEnd());
        collector.collect(imgWithCount);
    }
}
