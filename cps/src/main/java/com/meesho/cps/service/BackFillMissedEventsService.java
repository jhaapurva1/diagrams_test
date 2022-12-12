package com.meesho.cps.service;

import com.google.common.collect.Lists;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.presto.AdInteractionPrismEventPrestoData;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cpsclient.request.MissedEventsBackfillRequest;
import com.meesho.prism.beans.PrismSortOrder;
import com.meesho.prism.proxy.beans.EngineResponse;
import com.meesho.prism.sdk.FetchType;
import com.meesho.prism.sdk.PrismDW;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BackFillMissedEventsService {

    @Autowired
    private PrismService prismService;

    //String PRESTO_TABLE_NAME = "scrap.events_missing_on_13aug";
    String FIELD_TO_FILTER = "event_id";

    public static Set<String> allProcessedIds = new HashSet<>();
    public static Map<String, Object> processDetails =  new HashMap<>();
    public Map<String, Object> backFill(MissedEventsBackfillRequest missedEventsBackfillRequest){

        if(missedEventsBackfillRequest.getProcessSync()){
            return process(missedEventsBackfillRequest);
        }else {
            processAsync(missedEventsBackfillRequest);
        }

        return new HashMap<>();

    }

    private Map<String, Object> process(MissedEventsBackfillRequest missedEventsBackfillRequest){
        processDetails.put("Status", "INPROGRESS");
        String lastProcessedId = "";
        String eventId = "";
        try{
            Integer batchLimit = missedEventsBackfillRequest.getBatchSize();
            Integer processLimit = missedEventsBackfillRequest.getProcessBatchSize();

            if(batchLimit < processLimit){
                processDetails.put("Error", "batchLimit < processLimit");
                processDetails.put("Status", "FAILED");
                return processDetails;
            }

            List<AdInteractionPrismEventPrestoData> adInteractionPrismEvents = getFeedFromSource(missedEventsBackfillRequest.getLastProcessedEventId(), batchLimit, missedEventsBackfillRequest.getDumpId(), missedEventsBackfillRequest.getPrestoTableName());

            adInteractionPrismEvents = adInteractionPrismEvents.stream().filter(value -> !allProcessedIds.contains(value.getEventId())).collect(Collectors.toList());

            if(CollectionUtils.isEmpty(adInteractionPrismEvents)){
                log.info("{} : backFill Completed", missedEventsBackfillRequest.getLastProcessedEventId());
                processDetails.put("Status", "COMPLETED- no data");
                return processDetails;
            }

            List<List<AdInteractionPrismEventPrestoData> > partitionedList = Lists.partition(adInteractionPrismEvents, processLimit);

            int totalProcessed = 0;

            for(List<AdInteractionPrismEventPrestoData> events: partitionedList){

                List<AdInteractionPrismEvent> prismEvents = new ArrayList<>();

                for(AdInteractionPrismEventPrestoData event : events){

                    AdInteractionInvalidReason reason = Objects.nonNull(event.getReason()) ? AdInteractionInvalidReason.valueOf(event.getReason()) : null;

                    BigDecimal clickMultiplier = BigDecimal.valueOf(event.getClickMultiplier());
                    clickMultiplier = clickMultiplier.setScale(2, RoundingMode.HALF_UP);

                    if(Objects.isNull(event.getCpc()) || Objects.isNull(event.getOrigin())){
                        continue;
                    }

                    BigDecimal cpc = BigDecimal.valueOf(event.getCpc());
                    cpc = cpc.setScale(2, RoundingMode.HALF_UP);

                    AdInteractionPrismEvent prismEvent = AdInteractionPrismEvent
                            .builder()
                            .eventId(event.getEventId())
                            .cpc(cpc)
                            .clickMultiplier(clickMultiplier)
                            .reason(reason)
                            .status(AdInteractionStatus.valueOf(event.getStatus()))
                            .campaignId(event.getCampaignId())
                            .catalogId(event.getCatalogId())
                            .eventName(event.getEventName())
                            .eventTimestamp(event.getEventTimestamp())
                            .eventTimeIso(event.getEventTimeIso())
                            .currentTimestamp(event.getCurrentTimestamp())
                            .origin(event.getOrigin())
                            .screen(event.getScreen())
                            .userId(event.getUserId())
                            .interactionType(event.getInteractionType())
                            .build();
                    prismEvents.add(prismEvent);
                    eventId = prismEvent.getEventId();
                }

                prismService.publishEventEx(Constants.PrismEventNames.AD_INTERACTIONS, prismEvents);

                Set<String> processedIds = events.stream().map(value -> value.getEventId()).collect(Collectors.toSet());
                allProcessedIds.addAll(processedIds);
                lastProcessedId = events.get(events.size()-1).getEventId();
                log.info("{} : backFill lastProcessedId {}", missedEventsBackfillRequest.getLastProcessedEventId(), lastProcessedId);
                processDetails.put("lastProcessedEvent", lastProcessedId);
                totalProcessed+=events.size();
                processDetails.put("totalProcessed", totalProcessed);
                processDetails.put("OverallProcessed", allProcessedIds.size());
            }
            log.info("{} : Processing Completed : {}", missedEventsBackfillRequest.getLastProcessedEventId(), processDetails);
            processDetails.put("Status", "COMPLETED");
            return processDetails;
        }catch (Exception e){
            log.error("{} : ERROR Processing failed Last processed : {}", missedEventsBackfillRequest.getLastProcessedEventId(),lastProcessedId);
            log.error("{} : ERROR Processing failed details : {}", missedEventsBackfillRequest.getLastProcessedEventId(),processDetails);
            processDetails.put("Error", e.getMessage());
            processDetails.put("eventId", eventId);
            processDetails.put("Status", "FAILED");
            return processDetails;
        }
    }

    private void processAsync(MissedEventsBackfillRequest missedEventsBackfillRequest){
        CompletableFuture.runAsync(() -> {
            process(missedEventsBackfillRequest);
        });
    }

    public List<AdInteractionPrismEventPrestoData> getFeedFromSource(String lastProcessedEventId, int limit, String dumpId, String prestoTableName) {
        final LinkedHashMap<String, PrismSortOrder> sortOrderMap = new LinkedHashMap<>();
        sortOrderMap.put(FIELD_TO_FILTER, PrismSortOrder.ASCENDING);

        String filter = String.format(" %s > '%s' and event_name <> 'ad_click' and dump_id = '%s'", FIELD_TO_FILTER, lastProcessedEventId, dumpId);

        PrismDW prismDW = PrismDW.getInstance();
        EngineResponse prismEngineResponse =  prismDW.fetchOffset(prestoTableName,
                Collections.singletonList("*"), filter, null, null,
                sortOrderMap, limit, 0, AdInteractionPrismEventPrestoData.class, FetchType.JDBC);

        List<AdInteractionPrismEventPrestoData> prismEngineResponseList = new ArrayList<>();
        while (prismEngineResponse.hasNext()) {
            List<AdInteractionPrismEventPrestoData> responseList = prismEngineResponse.extractData();
            prismEngineResponseList.addAll(responseList);
            prismEngineResponse = prismEngineResponse.next();
        }
        log.info("{} : getFeedFromSource : fetched  records: {}", lastProcessedEventId, prismEngineResponseList.size());
        return prismEngineResponseList;
    }

}
