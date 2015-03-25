package com.tomitapio.missilemada2;

import java.util.Vector;

/* Date: 23.11.2013 */
public class Formation {
  Faction parentFaction = null;
  int curr_ships = 0;
  int whichformation;
  Vector ships_in_this_formation = null;
  Vector destinationXYZ = null;

  public Formation(Faction in_fac) {
    parentFaction = in_fac;
  }
  public void setWhichFormation(int i) {
    //verify inputs
    whichformation = i;
  }
  public void assignShip(Ship s) {
    ships_in_this_formation.add(s);
  }
  public void setDestination(Vector XYZ) {
    destinationXYZ = XYZ;
  }
  public Vector shipWantsFPosition(Ship undamagedship) {

    //if formation 1, AC in front, defe middle, scouts in rear.

    //if formation 2, scouts in front, defe middle, AC in rear.

    //if formation 3, defe in front, AC middle, scouts in rear.

    //if formation 4, everyone in wide front line.

    //if formation 5, everyone in tight cluster (AC first).

    //if formation 6, most percentage-damaged ships first.

    //if formation 7, most percentage-damaged ships last.



    return null;
  }
  public Vector shipWantsSaferPosition(Ship damagedship) {
    return null;
  }

}
