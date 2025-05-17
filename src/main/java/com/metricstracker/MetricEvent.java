package com.metricstracker;

import lombok.Getter;

public class MetricEvent
{
    enum eventType
    {
        MASTER,
        NONE,
        MONSTERS_KILLED,
        DAMAGE_DEALT
    }

    // Type of the data point being created
    @Getter
    public eventType type;
    @Getter
    public String name;
    @Getter
    public int quantity;

    public MetricEvent(eventType type, String name, int quantity )
    {
        this.type = type;
        this.name = name;
        this.quantity = quantity;
    }

    public MetricEvent(eventType type )
    {
        this.type = type;
        this.name = null;
        this.quantity = 0;
    }
}
