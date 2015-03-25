package com.tomitapio.missilemada2;

/**
 * Date: 23.8.2013
 */
public class HazardZone {

  double xcoord;
  double ycoord;
  double zcoord;
  double radius;
  double strength;
  String hazardType;
  MobileThing attachedTo; //so can have ships and asteroids with gravity well/fountain, and unique challenge-level-added missiles

  public HazardZone (double x, double y, double z, String in_type, double r, double str, MobileThing m) {
    xcoord = x;
    ycoord = y;
    zcoord = z;
    hazardType = new String(in_type);
    radius = r;
    strength = str; //no mass
    attachedTo = m;
  }

  public String getType() {
    return hazardType;
  }
  public double getX() {
    return xcoord;
  }
  public double getY() {
    return ycoord;
  }
  public double getZ() {
    return zcoord;
  }

  public double getRadius() {
    return radius;
  }
  public double getStrength() {
    return strength;
  }
  public boolean areCoordsInside(double in_x, double in_y, double in_z) {
    double dist = Math.sqrt( (in_x - xcoord) * (in_x - xcoord) + (in_y - ycoord) * (in_y - ycoord) );



    if (dist < radius) {
      return true;
    } else {
      return false;
    }
  }
  public boolean atCenterOfHZ(MobileThing m) {



    return false;
  }
  public void drawHazardZone() {





  }

}
