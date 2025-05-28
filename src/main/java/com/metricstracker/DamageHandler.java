package com.metricstracker;

import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
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
        MetricEvent metricEvent = new MetricEvent( MetricEvent.eventType.MONSTERS_KILLED, getSquishedKey( actor.getName() ), 1 );

        tickCounter = 0;
        eventsToValidate.put( actor, metricEvent);
    }

    private void emitDamageDoneEvent( Actor actor, Hitsplat hitsplat, EventBus eventBus )
    {
        MetricEvent metricEvent = new MetricEvent( MetricEvent.eventType.DAMAGE_DEALT, getSquishedKey( actor.getName() ), hitsplat.getAmount() );
        eventBus.post( metricEvent );
    }

    public void tick( NpcUtil npcUtil, EventBus eventBus, LocalPoint playerLocation )
    {
        boolean bPosted = false;
        int sz = eventsToValidate.keySet().size() - 1;
        if ( sz >= 0 )
        {
            Actor actors[] = eventsToValidate.keySet().toArray( new Actor[ 0 ] );
            for ( int i = sz; i >= 0; --i )
            {
                Actor actor = actors[ i ];

                if ( isActorDead( actor, npcUtil, playerLocation ) )
                {
                    // Moons bosses have multiple forms, and will count as individual kills unless crunched into 1 kill
                    String name = eventsToValidate.get( actor ).name;
                    if ( name.equals( "Eclipse Moon" )
                    ||   name.equals( "Blue Moon" )
                    ||   name.equals( "Blood Moon" ) )
                    {
                        if ( !bPosted )
                        {
                            eventBus.post( eventsToValidate.get( actor ) );
                            bPosted = true;
                        }
                    }
                    else
                    {
                        eventBus.post( eventsToValidate.get( actor ) );
                    }

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

    private boolean isActorDead( Actor actor, NpcUtil npcUtil, LocalPoint playerLocation )
    {
        if ( actor == null
        ||   npcUtil.isDying( ( NPC ) actor )
        ||   damageHandlerCheckSpecialCases( ( NPC ) actor, playerLocation ) )
        {
            return true;
        }

        return false;
    }

    private boolean damageHandlerCheckSpecialCases( NPC npc, LocalPoint playerLocation )
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
            // Special case to check for moon of peril dying
            case -1:
                return ( ( playerLocation.getX() == 6208 && playerLocation.getY() == 6976 )
                      || ( playerLocation.getX() == 7104 && playerLocation.getY() == 6976 )
                      || ( playerLocation.getX() == 6208 && playerLocation.getY() == 6720 ) );
            default:
                return false;
        }
    }

    String getSquishedKey( String key )
    {
        switch ( key )
        {
            case "<col=00ffff>Cracked ice</col>":
            case "<col=00ffff>Frozen weapons</col>":
                return "Frozen weapons";
            case "Blue Moon":
            case "Enraged Blue Moon":
                return "Blue Moon";
            case "Eclipse Moon":
            case "Enraged Eclipse Moon":
                return "Eclipse Moon";
            case "Blood Moon":
            case "Enraged Blood Moon":
                return "Blood Moon";
        }

        return key;
    }
}
