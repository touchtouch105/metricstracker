package com.metricstracker;

import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.NpcUtil;

import java.util.HashMap;

public class DamageHandler
{
    private int tickCounter = 0;
    private final int ticksToSelfDestruct = 100;
    private HashMap< Actor, MetricEvent > eventsToValidate = new HashMap<>();

    public void hitsplatApplied( HitsplatApplied hitsplatApplied, NpcUtil npcUtil, EventBus eventBus )
    {
        Actor actor = hitsplatApplied.getActor();
        Hitsplat hitsplat = hitsplatApplied.getHitsplat();

        if ( hitsplat.isMine()
        && ( actor instanceof NPC) )
        {
            emitDamageDoneEvent( actor, hitsplat, eventBus );
        }

        if ( isMonsterKilledEvent( hitsplat, actor, npcUtil ) )
        {
            emitMonsterKilledEvent( actor );
        }
    }

    public void emitAnimationChange( Actor actor, EventBus eventBus )
    {
        if ( !( actor instanceof NPC ) )
        {
            return;
        }

        if ( eventsToValidate.containsKey( actor ) )
        {
            NPC npc = ( NPC ) actor;
            if ( npc.getId() == NpcID.YAMA_VOIDFLARE )
            {
                int animation = npc.getAnimation();

                switch ( animation )
                {
                    case AnimationID.NPC_VOIDFLARE_EXPLODE:
                        eventsToValidate.remove( actor );
                        break;
                    case AnimationID.NPC_VOIDFLARE_DEATH:
                        eventBus.post( actor );
                        eventsToValidate.remove( actor );
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private boolean isMonsterKilledEvent( Hitsplat hitsplat, Actor actor, NpcUtil npcUtil )
    {
        if ( !( actor instanceof NPC ) )
        {
            return false;
        }
      
        NPC npc = ( NPC ) actor;

        if ( hitsplat.isMine()
        &&   hitsplat.getAmount() > 0 )
        {
            // Start tracking the mob after the player deals damage below 50% hp
            if ( actor.getHealthRatio() <= 0 || ( actor.getHealthRatio() <= actor.getHealthScale() / 2 )  )
            {
                return true;
            }

            if ( npcUtil.isDying( ( NPC ) actor) )
            {
                return true;
            }

            // Special cases that need to be added manually due to healthratio not updating
            switch ( npc.getId() )
            {
                case NpcID.YAMA_VOIDFLARE:
                    return true;
                default:
                    return false;
            }
        }

        return false;
    }

    private void emitMonsterKilledEvent( Actor actor )
    {
        MetricEvent metricEvent = new MetricEvent( MetricEvent.eventType.MONSTERS_KILLED, actor.getName(), 1 );
        tickCounter = 0;
        eventsToValidate.put( actor, metricEvent);
    }

    private void emitDamageDoneEvent( Actor actor, Hitsplat hitsplat, EventBus eventBus )
    {
        MetricEvent metricEvent = new MetricEvent( MetricEvent.eventType.DAMAGE_DEALT, actor.getName(), hitsplat.getAmount() );
        eventBus.post( metricEvent );
    }

    public void tick( NpcUtil npcUtil, EventBus eventBus )
    {
        int sz = eventsToValidate.keySet().size() - 1;
        if ( sz >= 0 )
        {
            Actor actors[] = eventsToValidate.keySet().toArray( new Actor[ 0 ] );
            for ( int i = sz; i >= 0; --i )
            {
                Actor actor = actors[ i ];

                if ( isActorDead( actor, npcUtil ) )
                {
                    eventBus.post( eventsToValidate.get( actor ) );
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

    private boolean isActorDead( Actor actor, NpcUtil npcUtil )
    {
        if ( actor == null
        ||   npcUtil.isDying( ( NPC ) actor )
        ||   damageHandlerCheckSpecialCases( ( NPC ) actor ) )
        {
            return true;
        }

        return false;
    }

    private boolean damageHandlerCheckSpecialCases( NPC npc )
    {
        int id = npc.getId();

        switch ( id )
        {
            case NpcID.YAMA:
            case NpcID.YAMA_JUDGE_OF_YAMA:
                return npc.getHealthRatio() == 0;
            case NpcID.NIGHTMARE_TOTEM_1_CHARGED:
            case NpcID.NIGHTMARE_TOTEM_2_CHARGED:
            case NpcID.NIGHTMARE_TOTEM_3_CHARGED:
            case NpcID.NIGHTMARE_TOTEM_4_CHARGED:
                return true;
            default:
                return false;
        }
    }
}
