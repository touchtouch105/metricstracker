package com.metricstracker;

import lombok.Getter;

import java.util.List;

public class Event
{
    enum eventType
    {
        NONE,
        ITEM_DROPS,
        XP_DROPS,
        LVLS_GAINED,
        MONSTERS_KILLED,
        DAMAGE_DEALT,
        DAMAGE_TAKEN,
        RESOURCES_GATHERED,
        CHAT_MESSAGES
    }

    // Type of the data point being created
    @Getter
    public eventType Type;
    @Getter
    public List<String> Information;
    @Getter
    public int Quantity;

    public Event( eventType type, List<String> info, int quantity )
    {
        this.Type = type;
        this.Information = info;
        this.Quantity = quantity;
    }
}
