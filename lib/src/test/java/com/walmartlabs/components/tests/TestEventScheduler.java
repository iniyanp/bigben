package com.walmartlabs.components.tests;

import com.google.common.util.concurrent.ListenableFuture;
import com.walmart.gmp.ingestion.platform.framework.data.core.DataManager;
import com.walmartlabs.components.scheduler.core.EventProcessor;
import com.walmartlabs.components.scheduler.core.EventReceiver;
import com.walmartlabs.components.scheduler.model.Bucket;
import com.walmartlabs.components.scheduler.model.Event;
import com.walmartlabs.components.scheduler.model.EventDO.EventKey;
import com.walmartlabs.components.scheduler.services.BulkEventGeneration;
import com.walmartlabs.components.scheduler.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.walmart.gmp.ingestion.platform.framework.core.Props.PROPS;
import static com.walmartlabs.components.scheduler.utils.TimeUtils.bucketize;
import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.ofInstant;

/**
 * Created by smalik3 on 3/9/16
 */
@ContextConfiguration(locations = {"/test-scheduler.xml"/*, "/cluster.xml"*/})
public class TestEventScheduler extends AbstractTestNGSpringContextTests implements EventProcessor<Event> {

    static {
        System.setProperty("dm.entity.packages.scan", "com.walmartlabs.components.scheduler.model");
        System.setProperty("com.walmart.platform.config.runOnEnv", "prod");
        System.setProperty("event.shard.size", "10");
        System.setProperty("hazelcast.slow.operation.detector.stacktrace.logging.enabled", "true");
    }

    @Autowired
    private DataManager<EventKey, Event> dataManager;

    @Autowired
    private DataManager<Long, Bucket> bucketDM;

    @Autowired
    private EventReceiver eventReceiver;

    @Autowired
    private EventService eventService;

    @Override
    public ListenableFuture<Event> process(Event event) {
        System.out.println("processing event: " + event);
        events.remove(event.id().toString());
        if (events.size() == 0) {
            System.out.println("test done");
        }
        return immediateFuture(event);
    }

    private static final Map<String, Boolean> events = new ConcurrentHashMap<>();

    @Test(enabled = false)
    public void testEventScheduler() throws Exception {
        final Integer scanInterval = PROPS.getInteger("event.schedule.scan.interval.minutes", 1);
        final ZonedDateTime now = now();
        final int delay = 1;
        final long from = bucketize(now.plusMinutes(delay).toInstant().toEpochMilli(), scanInterval);
        final String t1 = ofInstant(ofEpochMilli(from), UTC).toString();
        System.out.println(eventService.generateEvents(new BulkEventGeneration(t1, 1, 100, "0")));
        new CountDownLatch(1).await();
    }
}
