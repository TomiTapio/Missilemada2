package com.tomitapio.missilemada2;
import java.util.Vector;

public class StarBase extends Ship {
  public StarBase(Vector XYZ, Faction in_fac) {
    super("STARBASE", in_fac, XYZ, "i_am_a_starbase", "starbase.png", false/*needs to check for crew availability*/);
    xcoord = ((Double) XYZ.get(0)).doubleValue();
    ycoord = ((Double) XYZ.get(1)).doubleValue();
    zcoord = ((Double) XYZ.get(2)).doubleValue();
    xspeed = 0.0;
    yspeed = 0.0;
    zspeed = 0.0;

    mass_kg = 0.8 * 400500500500.0; //1.5km to 115 cube km. mass 4.3 * 115 is 500 000 000 000?. 400 Mtons.

    parentFaction = in_fac;
    name = "Suisei no " + com.tomitapio.missilemada2.Missilemada2.getRandomJapaneseName(); //comet of

    radius = 410;
    pixelradius = 410; //xxxx

    unique_id = MobileThing.getNextId();
    //xxxverify inputs

    double engine_status = 1.0; //0.0 - 1.0 (100%)
    double lifesupport_status = 1.0;
    double sensors_status = 1.0;
    double shield_status = 1.0;

    double missilesystem_status = 1.0;
    double beamsystem_status = 1.0; //atk and defe beam
    double miningsystem_status = 1.0; //even ones that don't have mining gear??

    setTexture("starbase.png", 1.9);
  }
  public Vector getDockingXYZ() {
    double randy = 40.0 * radius * com.tomitapio.missilemada2.Missilemada2.gimmeRandDouble();
    Vector ret = new Vector(3,3);
    ret.add(0, new Double(xcoord+38.0*radius));
    ret.add(1, new Double(ycoord-20.0*radius + randy));
    ret.add(2, new Double(zcoord + 90));
    return ret;
  }
  public Vector getTextXYZ() {
    Vector ret = new Vector(3,3);
    ret.add(0, new Double(xcoord-78.0*radius));
    ret.add(1, new Double(ycoord-120.0*radius));
    ret.add(2, new Double(zcoord + 900));
    return ret;

  }
}