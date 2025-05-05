package com.metricstracker;

import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

public class MetricsManager
{
    private final static float MSEC_PER_SEC = 1000;
    private final static float MSEC_PER_MIN = 60 * MSEC_PER_SEC;
    private final String MasterKey;
    @Getter
    public Event lastEvent;
    private long startTime;
    private HashMap< String, Long > keyQuantities;

    public MetricsManager( Event event )
    {
        this.startTime = 0;
        this.keyQuantities = new HashMap<>();
        this.lastEvent = null;
        this.MasterKey = null;
        addDataPoint( event );
    }

    public MetricsManager( String masterKey, Event event )
    {
        this.startTime = Instant.now().toEpochMilli();
        this.MasterKey = masterKey;
        this.keyQuantities = new HashMap<>();
        this.keyQuantities.put( masterKey, (long) 0 );
        this.lastEvent = null;
        addDataPoint( masterKey, event );
    }

    public MetricsManager( String masterKey )
    {
        this.startTime = Instant.now().toEpochMilli();
        this.MasterKey = masterKey;
        this.keyQuantities = new HashMap<>();
        this.keyQuantities.put( masterKey, (long) 0 );
        this.lastEvent = null;
    }

    public void addDataPoint( String key, Event event )
    {
        if ( !key.equals( MasterKey ) )
        {
            return;
        }

        if ( this.startTime == 0 )
        {
            this.startTime = Instant.now().toEpochMilli();
        }
        this.lastEvent = event;
        long quantity = 0;
        if ( this.keyQuantities.containsKey( key ) )
        {
            quantity = this.keyQuantities.get( key );
        }

        quantity += event.getQuantity();
        this.keyQuantities.put( key, quantity);
    }

    public void addDataPoint( Event event )
    {
        if ( this.startTime == 0 )
        {
            this.startTime = Instant.now().toEpochMilli();
        }
        this.lastEvent = event;

        for ( String s : event.getInformation() )
        {
            long quantity = 0;

            if ( this.keyQuantities.containsKey( s ) )
            {
                quantity = this.keyQuantities.get( s );
            }

            quantity += event.getQuantity();
            this.keyQuantities.put( s, quantity );
        }
    }

    public float getRunTimeSeconds()
    {
        long now = Instant.now().toEpochMilli();
        return ( ( now - this.startTime ) / MSEC_PER_SEC );
    }

    public float getRunTimeMinutes()
    {
        long now = Instant.now().toEpochMilli();
        return ( ( now - this.startTime ) / MSEC_PER_MIN );
    }

    public float getQuantityPerHour( String key )
    {
        float qph = 0;
        float runTime = getRunTimeSeconds();

        if ( this.keyQuantities.containsKey( key ) )
        {
            if ( runTime <= 1 )
            {
                return this.keyQuantities.get( key );
            }

            qph = this.keyQuantities.get( key );
            qph /= getRunTimeSeconds();
            qph *= 3600;
        }
        return qph;
    }

    public boolean containsAnyKeyFrom( List< String > key )
    {
        for ( String s : key )
        {
            if ( this.keyQuantities.containsKey( s ) )
            {
                return true;
            }
        }

        return false;
    }

    public long getCumulativeQuantity( String key )
    {
        return this.keyQuantities.get( key );
    }

    public void reset()
    {
        this.keyQuantities.clear();
        this.startTime = 0;
    }
}
