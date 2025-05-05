package com.metricstracker;

import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.client.game.NpcUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DamageHandler
{
    private int tickCounter = 0;
    private final int ticksToSelfDestruct = 100;
    private HashMap<Actor, Event> eventsToValidate = new HashMap<>();
    private List<Actor> actorsToTrackOneTick = new ArrayList<>();
    private List<Integer> TickCycle = new ArrayList<>();
    public boolean isMonsterKilledEvent( Hitsplat hitsplat,  Actor actor, NpcUtil npcUtil )
    {
        if ( hitsplat.isMine() )
        {
            if ( !(actor instanceof NPC) )
            {
                return false;
            }

            // Start tracking the mob after the player deals damage below 50% hp
            if ( actor.getHealthRatio() <= 0 || ( actor.getHealthRatio() <= actor.getHealthScale() / 2 )  )
            {
                return true;
            }

            if ( npcUtil.isDying((NPC) actor) )
            {
                return true;
            };
        }

        return false;
    }

    public void emitMonsterKilledEvent( Actor actor, EventConsumer consumer )
    {
        Event.eventType eventType = Event.eventType.MONSTERS_KILLED;
        List<String> info = new ArrayList<>();
        info.add( actor.getName() );
        Event event = new Event( eventType, info, 1 );
        tickCounter = 0;
        eventsToValidate.put(actor, event);
    }


    public void tick( EventConsumer consumer, NpcUtil npcUtil )
    {
        int sz = eventsToValidate.keySet().size() - 1;
        if ( sz >= 0 )
        {
            Actor actors[] = eventsToValidate.keySet().toArray(new Actor[0]);
            for ( int i = sz; i >= 0; --i )
            {
                Actor actor = actors[i];
                if ( actor == null
                ||   actor.isDead()
                ||   npcUtil.isDying((NPC) actor ) )
                {
                    consumer.addPendingEvent(eventsToValidate.get(actor));
                    eventsToValidate.remove( actor );
                }
            }
            // Delete lists after a minute of inactivity to avoid any memory leaks
            tickCounter++;
            if ( tickCounter == ticksToSelfDestruct )
            {
                eventsToValidate.clear();
                tickCounter = 0;
            }
        }
    }
}
