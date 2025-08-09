package com.metricstracker;

import java.util.HashMap;

public class MetricsSnapshot
{
    private static final HashMap< String, String > primaryQuantities = new HashMap<>();
    private static final HashMap< String, String > alternateQuantities = new HashMap<>();
    private static final HashMap< String, String > primaryRates = new HashMap<>();
    private static final HashMap< String, String > alternateRates = new HashMap<>();

    MetricsSnapshot()
    {

    }

    void updateSnapshot( String name, long primaryQuantity, float primaryRate, long altQuantity, float altRate )
    {
        final String quantString = "Killed:" + primaryQuantity;
        final String rateString = "KPH:" + primaryRate;
        final String altQuantString = "Damage:" + altQuantity;
        final String altRateString = "DPS:" + altRate;
        primaryQuantities.put( name, quantString );
        primaryRates.put( name, rateString );
        alternateQuantities.put( name, altQuantString );
        alternateRates.put( name, altRateString );
    }

    void updateSnapshot( String name, long quantity, float rate )
    {
        final String quantString = "Killed:" + quantity;
        final String rateString = "KPH:" + rate;
        primaryQuantities.put( name, quantString );
        primaryRates.put( name, rateString );
    }

    String getPrimaryQuantity( String name )
    {
        return primaryQuantities.get( name );
    }

    String getAlternateQuantity( String name )
    {
        return alternateQuantities.get( name );
    }

    String getPrimaryRate( String name )
    {
        return primaryRates.get( name );
    }

    String getAlternateRate( String name )
    {
        return alternateRates.get( name );
    }


}
