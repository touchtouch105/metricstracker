package com.metricstracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventConsumer
{
    private MetricsTrackerPanel panel;
    public EventConsumer(MetricsTrackerPanel panel)
    {
        this.panel = panel;
    }

    HashMap<Event.eventType, List<Event>> pendingEventsNeedsValidated = new HashMap<>();
    HashMap<Event.eventType, Integer> pendingEventsTicksLeft = new HashMap<>();
    HashMap<Event.eventType, Boolean> pendingEventValidation = new HashMap<>();
    private List<Event> pendingEvents = new ArrayList<>();

    public void consumePendingEvents()
    {
        int sz = pendingEvents.size();
        for ( int i = sz - 1; i >= 0; --i )
        {
            Event event = pendingEvents.get( i );
            panel.addEvent( event );
            pendingEvents.remove( i );
        }
        invalidatePendingEvents();
    }
    public void addPendingEvent( Event event )
    {
        pendingEvents.add( event );
    }

    public void addPendingEvent( Event event, boolean needsValidated, int ticksToValidate )
    {
        if ( needsValidated )
        {
            if ( pendingEventValidation.containsKey( event.getType() ) )
            {
                if ( pendingEventValidation.get( event.getType() ) )
                {
                    pendingEvents.add(event);
                    pendingEventValidation.put( event.getType(), false );
                    pendingEventsTicksLeft.put( event.getType(), ticksToValidate );
                    return;
                }
            }

            List<Event> eventList = null;
            if ( pendingEventsNeedsValidated.containsKey( event.getType() ) )
            {
                eventList = pendingEventsNeedsValidated.get( event.getType() );
            }

            if ( eventList == null )
            {
                eventList = new ArrayList<>();
            }

            eventList.add( event );
            pendingEventsNeedsValidated.put( event.getType(), eventList );
            pendingEventsTicksLeft.put( event.getType(), ticksToValidate );
        }
        else
        {
            pendingEvents.add(event);
        }
    }
    public void validatePendingEvents( Event.eventType eventType )
    {
        if ( pendingEventsNeedsValidated.containsKey(eventType) )
        {
            pendingEvents.addAll( pendingEventsNeedsValidated.get(eventType) );
            pendingEventsNeedsValidated.put(eventType, new ArrayList<>());
        }
        else
        {
            pendingEventValidation.put( eventType, true );
        }
    }
    private void invalidatePendingEvents()
    {
        for ( Event.eventType eventType : pendingEventsNeedsValidated.keySet() )
        {
            if ( pendingEventsTicksLeft.containsKey( eventType ) )
            {
                int ticksLeft = pendingEventsTicksLeft.get(eventType) - 1;
                if ( ticksLeft > 0 )
                {
                    pendingEventsTicksLeft.put( eventType, ticksLeft );
                }
                else
                {
                    pendingEventsTicksLeft.remove(eventType);
                    pendingEventsNeedsValidated.remove(eventType);
                }
            }
            else
            {
                pendingEventsNeedsValidated.remove( eventType );
            }
        }

        if ( pendingEventValidation.size() > 0 )
        {
            pendingEventValidation.clear();
        }
    }
}
