/**
 *  Copyright (C) 2002-2013  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class MovementTest extends FreeColTestCase {

    private static final TileType hills
        = spec().getTileType("model.tile.hills");
    private static final TileType ocean
        = spec().getTileType("model.tile.ocean");
    private static final TileType plains
        = spec().getTileType("model.tile.plains");

    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");

    private static final EquipmentType horses
        = spec().getEquipmentType("model.equipment.horses");
    private static final EquipmentType indianHorses
        = spec().getEquipmentType("model.equipment.indian.horses");
    private static final EquipmentType indianMuskets
        = spec().getEquipmentType("model.equipment.indian.muskets");
    private static final EquipmentType muskets
        = spec().getEquipmentType("model.equipment.muskets");
    
    private static final TileImprovementType riverType
        = spec().getTileImprovementType("model.improvement.river");
    private static final TileImprovementType roadType
        = spec().getTileImprovementType("model.improvement.road");


    public void testMoveFromPlainsToPlains() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setExplored(dutch, true);
        tile2.setExplored(dutch, true);

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);

        int moveCost = plains.getBasicMoveCost();
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

        // Plowing should not change result
        assertTrue("No improvements", tile2.getTileImprovements().isEmpty());
        TileImprovement ti = new TileImprovement(game, tile2, spec().getTileImprovementType("model.improvement.plow"));
        ti.setTurnsToComplete(0);
        tile2.add(ti);
        assertTrue("Plowed", tile2.getCompletedTileImprovements().size() == 1);
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));
    }

    public void testMoveFromPlainsToHills() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile2.setType(hills);
        tile1.setExplored(dutch, true);
        tile2.setExplored(dutch, true);

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);

        int moveCost = hills.getBasicMoveCost();
        assertTrue(moveCost > colonist.getMovesLeft());
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

    }

    public void testMoveAlongRoad() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setExplored(dutch, true);
        tile2.setExplored(dutch, true);

        TileImprovement road1 = tile1.addRoad();
        assertTrue(road1.isRoad());
        assertFalse(road1.isComplete());
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        road1.updateRoadConnections(true);
        assertEquals(road1, tile1.getRoad());

        TileImprovement road2 = tile2.addRoad();
        road2.setTurnsToComplete(0);
        road2.updateRoadConnections(true);
        assertTrue(road2.isComplete());
        assertEquals(road2, tile2.getRoad());
        
        assertTrue(road1.isConnectedTo(tile1.getDirection(tile2)));
        assertTrue(road2.isConnectedTo(tile2.getDirection(tile1)));

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        int moveCost = 1;
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));
    }

    public void testMoveAlongRiver() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = tile1.getNeighbourOrNull(Map.Direction.NE);
        tile1.setExplored(dutch, true);
        tile2.setExplored(dutch, true);
        assertEquals(Map.Direction.NE, map.getDirection(tile1, tile2));
        assertEquals(Map.Direction.SW, map.getDirection(tile2, tile1));

        TileImprovement river1 = tile1.addRiver(1, "0101");
        assertTrue(river1.isRiver());
        assertTrue(river1.isComplete());
        assertTrue(tile1.hasRiver());

        TileImprovement river2 = tile2.addRiver(1, "0101");
        assertTrue(river2.isRiver());
        assertTrue(river2.isComplete());
        assertTrue(tile2.hasRiver());

        assertFalse(river1.isConnectedTo(Map.Direction.NE));
        assertTrue (river1.isConnectedTo(Map.Direction.SE));
        assertFalse(river1.isConnectedTo(Map.Direction.SW));
        assertTrue (river1.isConnectedTo(Map.Direction.NW));
        assertFalse(river2.isConnectedTo(Map.Direction.NE));
        assertTrue (river2.isConnectedTo(Map.Direction.SE));
        assertFalse(river2.isConnectedTo(Map.Direction.SW));
        assertTrue (river2.isConnectedTo(Map.Direction.NW));

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);

        // rivers start parallel, no cost reduction
        int moveCost = 3;
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

        // rivers are connected, cost reduction applies
        river1.updateRiverConnections("1000");
        river2.updateRiverConnections("0010");

        assertTrue (river1.isConnectedTo(Map.Direction.NE));
        assertFalse(river1.isConnectedTo(Map.Direction.SE));
        assertFalse(river1.isConnectedTo(Map.Direction.SW));
        assertFalse(river1.isConnectedTo(Map.Direction.NW));
        assertFalse(river2.isConnectedTo(Map.Direction.NE));
        assertFalse(river2.isConnectedTo(Map.Direction.SE));
        assertTrue (river2.isConnectedTo(Map.Direction.SW));
        assertFalse(river2.isConnectedTo(Map.Direction.NW));

        moveCost = 1;
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));
    }

    public void testScoutColony() {
        Game game = getGame();
        Map map = getTestMap(true);
        game.setMap(map);

        Player french = game.getPlayer("model.nation.french");
        Player dutch = game.getPlayer("model.nation.dutch");
        Player iroquois = game.getPlayer("model.nation.iroquois");

        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        Tile tile3 = map.getTile(6, 8);
        tile1.setExplored(french, true);
        tile2.setExplored(french, true);
        tile3.setExplored(french, true);
        tile1.setExplored(dutch, true);
        tile2.setExplored(dutch, true);
        tile3.setExplored(dutch, true);

        Colony colony = getStandardColony();

        assertEquals(tile1.getColony(), colony);

        Unit colonist = new ServerUnit(game, tile2, french, colonistType);
        assertEquals(Unit.MoveType.MOVE_NO_ACCESS_SETTLEMENT,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(horses, 1);
        assertEquals(Unit.MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(muskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT,
                     colonist.getMoveType(tile1));

        Unit brave = new ServerUnit(game, tile3, iroquois, braveType);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianHorses, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianMuskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
    }

    public void testScoutIndianSettlement() {
        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);

        Player french = game.getPlayer("model.nation.french");
        Player inca = game.getPlayer("model.nation.inca");
        Player iroquois = game.getPlayer("model.nation.iroquois");

        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        Tile tile3 = map.getTile(6, 8);
        tile1.setExplored(french, true);
        tile2.setExplored(french, true);
        tile3.setExplored(french, true);

        // Build settlement
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.player(inca).settlementTile(tile1).skillToTeach(null).build();

        Unit colonist = new ServerUnit(game, tile2, french, colonistType);
        assertEquals(Unit.MoveType.MOVE_NO_ACCESS_CONTACT,
                     colonist.getMoveType(tile1));
        Player.makeContact(french, inca);
        assertEquals(Unit.MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(horses, 1);
        assertEquals(Unit.MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(muskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT,
                     colonist.getMoveType(tile1));

        Unit brave = new ServerUnit(game, tile3, iroquois, braveType);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianHorses, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianMuskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
    }
}
