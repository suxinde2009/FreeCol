/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.util.Utils;


/**
 * Helper container to remember a unit state prior to some change,
 * and fire off any consequent property changes.
 */
public class UnitWas implements Comparable<UnitWas> {

    private static final Logger logger = Logger.getLogger(UnitWas.class.getName());

    private Unit unit;
    private UnitType type;
    private Role role;
    private Location loc;
    private GoodsType work;
    private int workAmount;
    private Colony colony;
    private TypeCountMap<EquipmentType> equipment;


    /**
     * Record the state of a unit.
     *
     * @param unit The <code>Unit</code> to remember.
     */
    public UnitWas(Unit unit) {
        this.unit = unit;
        this.type = unit.getType();
        this.role = unit.getRole();
        this.loc = unit.getLocation();
        this.work = unit.getWorkType();
        this.workAmount = getAmount(loc, work);
        this.colony = unit.getColony();
        this.equipment = new TypeCountMap<EquipmentType>();
        this.equipment.add(unit.getEquipment());
        if (unit.getGoodsContainer() != null) {
            unit.getGoodsContainer().saveState();
        }
    }


    public Unit getUnit() {
        return unit;
    }

    public Location getLocation() {
        return loc;
    }

    public GoodsType getWorkType() {
        return work;
    }

    /**
     * Compares this UnitWas with another.
     *
     * Order by decreasing capacity of the location the unit is to be
     * moved to, so that if we traverse a sorted list of UnitWas we
     * minimize the chance of a unit being moved to a full location.
     * Unfortunately this also tends to move units that need equipment
     * first, leading to failures to rearm, so it is best to make two
     * passes anyway.  See revertAll() below.  However we can still
     * try our best by using the amount of equipment the unit needs as
     * a secondary criterion (favouring the least equipped).
     *
     * @param uw The <code>UnitWas</code> to compare to.
     * @return A comparison result.
     */
    public int compareTo(UnitWas uw) {
        int cmp = ((UnitLocation)uw.loc).getUnitCapacity()
            - ((UnitLocation)this.loc).getUnitCapacity();
        if (cmp != 0) return cmp;
        return this.equipment.keySet().size() - uw.equipment.keySet().size();
    }

    /**
     * Fire any property changes resulting from actions of a unit.
     */
    public void fireChanges() {
        UnitType newType = null;
        Role newRole = null;
        Location newLoc = null;
        GoodsType newWork = null;
        int newWorkAmount = 0;
        TypeCountMap<EquipmentType> newEquipment = null;
        if (!unit.isDisposed()) {
            newLoc = unit.getLocation();
            if (colony != null) {
                newType = unit.getType();
                newRole = unit.getRole();
                newWork = unit.getWorkType();
                newWorkAmount = (newWork == null) ? 0
                    : getAmount(newLoc, newWork);
                newEquipment = unit.getEquipment();
            }
        }

        FreeColGameObject oldFcgo = (FreeColGameObject)loc;
        FreeColGameObject newFcgo = (FreeColGameObject)newLoc;
        if (loc != newLoc) {
            oldFcgo.firePropertyChange(change(oldFcgo), unit, null);
            if (newLoc != null) {
                newFcgo.firePropertyChange(change(newFcgo), null, unit);
            }
        }
        if (colony != null) {
            if (type != newType && newType != null) {
                String pc = ColonyChangeEvent.UNIT_TYPE_CHANGE.toString();
                colony.firePropertyChange(pc, type, newType);
            } else if (role != newRole && newRole != null) {
                String pc = Tile.UNIT_CHANGE.toString();
                colony.firePropertyChange(pc, role.toString(),
                                          newRole.toString());
            }
            if (work != newWork) {
                if (work != null && oldFcgo != null && workAmount != 0) {
                    oldFcgo.firePropertyChange(work.getId(), workAmount, 0);
                }
                if (newWork != null && newFcgo != null && newWorkAmount != 0) {
                    newFcgo.firePropertyChange(newWork.getId(),
                                               0, newWorkAmount);
                }
            } else if (workAmount != newWorkAmount) {
                newFcgo.firePropertyChange(newWork.getId(),
                                           workAmount, newWorkAmount);
            }
        }
        if (newEquipment != null) {
            Set<EquipmentType> keys = new HashSet<EquipmentType>();
            keys.addAll(equipment.keySet());
            keys.addAll(newEquipment.keySet());
            for (EquipmentType e : keys) {
                int cOld = equipment.getCount(e);
                int cNew = newEquipment.getCount(e);
                if (cOld != cNew) {
                    unit.firePropertyChange(Unit.EQUIPMENT_CHANGE, cOld, cNew);
                }
            }
        }
        if (unit.getGoodsContainer() != null) {
            unit.getGoodsContainer().fireChanges();
        }
    }

    // TODO: fix this non-OO nastiness
    private String change(FreeColGameObject fcgo) {
        return (fcgo instanceof Tile) ? Tile.UNIT_CHANGE
            : (fcgo instanceof Europe) ? Europe.UNIT_CHANGE
            : (fcgo instanceof ColonyTile) ? ColonyTile.UNIT_CHANGE
            : (fcgo instanceof Building) ? Building.UNIT_CHANGE
            : (fcgo instanceof Unit) ? Unit.CARGO_CHANGE
            : null;
    }

    // TODO: fix this non-OO nastiness
    private int getAmount(Location location, GoodsType goodsType) {
        if (goodsType != null && location instanceof WorkLocation) {
            ProductionInfo info = ((WorkLocation)location).getProductionInfo();
            for (AbstractGoods ag : info.getProduction()) {
                if (ag.getType() == goodsType) return ag.getAmount();
            }
            /*
            if (location instanceof Building) {
                Building building = (Building) location;
                ProductionInfo info = building.getProductionInfo();
                return (info == null || info.getProduction() == null
                    || info.getProduction().size() == 0) ? 0
                    : info.getProduction().get(0).getAmount();
            } else if (location instanceof ColonyTile) {
                return ((ColonyTile)location).getTotalProductionOf(goodsType);
            }
            */
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        Tile tile = colony.getTile();
        String eqStr = "/";
        for (EquipmentType e : equipment.keySet()) {
            eqStr += e.toString().substring(16, 17);
        }
        String locStr = (loc == null) ? ""
            : (loc instanceof Building)
            ? ((Building)loc).getType().getSuffix()
            : (loc instanceof ColonyTile)
            ? tile.getDirection(((ColonyTile)loc).getWorkTile()).toString()
            : (loc instanceof Tile)
            ? (loc.getId() + eqStr)
            : loc.getId();
        Location newLoc = unit.getLocation();
        String newEqStr = "/";
        for (EquipmentType e : unit.getEquipment().keySet()) {
            newEqStr += e.toString().substring(16, 17);
        }
        String newLocStr = (newLoc == null) ? ""
            : (newLoc instanceof Building)
            ? ((Building)newLoc).getType().getSuffix()
            : (newLoc instanceof ColonyTile)
            ? tile.getDirection(((ColonyTile)newLoc).getWorkTile()).toString()
            : (newLoc instanceof Tile)
            ? (newLoc.getId() + newEqStr)
            : newLoc.getId();
        GoodsType newWork = unit.getWorkType();
        int newWorkAmount = (newWork == null) ? 0 : getAmount(newLoc, newWork);
        return String.format("%-30s %-25s -> %-25s",
            unit.getId() + ":" + unit.getType().getSuffix(),
            locStr + ((work == null || workAmount <= 0) ? "" : "("
                + Integer.toString(workAmount) + " " + work.getSuffix() + ")"),
            newLocStr + ((newWork == null || newWorkAmount <= 0) ? "" : "("
                + Integer.toString(newWorkAmount) + " "
                + newWork.getSuffix() + ")")).trim();
    }
}
