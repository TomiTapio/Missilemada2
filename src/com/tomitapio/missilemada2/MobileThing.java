package com.tomitapio.missilemada2;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.Vector;

public class MobileThing
{
  TextureMy textureMy;
  static TextureMy textureUnknAste;
  //int texture_width;
  //int texture_height;

  //location: in Missilemada2, kilometers. Original plan was playfield = Saturn's rings.
  double xcoord;
  double ycoord;
  double zcoord;
  Double xcoordD;
  Double ycoordD;
  Double zcoordD;

  Vector my_xyz;
  double prev_xcoord;
  double prev_ycoord;
  double prev_zcoord;

  //speed: km per sec.
  double xspeed; //positive is right
  double yspeed; //positive is up
  double zspeed; //positive z is towards camera.

  double mass_kg; //thing can lose fuel, hull plating etc.
  double max_mass_kg; //full of fuel, no cargo

  MobileThing tractorer = null; //who is using a tractorbeam on this. (self is a ship or aste or missile) //xx in theory, missiles tractoring asteroids.

  int pixelradius = 3;
  long unique_id = 0;
  static long next_uid = 0;

  boolean is_near_combat = false; //handy thing to remember
  boolean spotted_by_player = false; //for drawing, so some things player does not see.

  int corenote = 80; // MIDI note 0..127
  int MIDI_instrument = 1; //default acoustic piano

  public MobileThing() {
    my_xyz = new Vector(4,1);
    my_xyz.add(0, new Double(0.0));
    my_xyz.add(1, new Double(0.0));
    my_xyz.add(2, new Double(0.0));
  }
  public boolean isNearBattle() {
    return is_near_combat;
  }
  public void setIsNearBattle(boolean foo) {
    is_near_combat = foo;
  }
  public boolean isSeenByPlayer() {
    return spotted_by_player;
  }
  public void setIsSeenByPlayer(boolean foo) {
    spotted_by_player = foo;
  }
  public void setOpenGLColorFromAWTColor(Color co, float alpha) {
    GL11.glColor4f(co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, alpha);
  }
  public String getStringMT() {
    return this.getClass().getName() + " id "+unique_id + " speed "+ getSpeedCurrent() + " XYdir "+ getBearingXYfromSpeed();
  }
  public boolean isNearCamera_2d(Vector cam_xyz, double dist) {
    //only look at x and y.
    if (calcDistance(xcoord, ycoord, 0.0, ((Double) cam_xyz.get(0)).doubleValue(), ((Double) cam_xyz.get(1)).doubleValue(), 0.0) < dist)
      return true;
    else
      return false;
  }
  public boolean isNearCamera_in3d(Vector cam_xyz, double dist_3d) {
    if (calcDistance(xcoord, ycoord, zcoord, ((Double) cam_xyz.get(0)), ((Double) cam_xyz.get(1)), ((Double) cam_xyz.get(2))) < dist_3d)
      return true;
    else
      return false;
  }
  public void setTexture(String filename, double scaling) {
    try {
      textureUnknAste = Missilemada2.getTextureLoader().getTexture("gneiss.png"); //xxxxhack
      textureMy = Missilemada2.getTextureLoader().getTexture(filename);
      //texture_width = (int) Math.round (scaling * textureMy.getImageWidth());
      //texture_height = (int) Math.round (scaling * textureMy.getImageHeight());
    } catch (IOException e) {
      System.out.println("MobileThing: Unable to load texture file: "+filename);
      e.printStackTrace();
    }
  }
  public void setTextureScaling(double scaling) {
    //NOT WORKING coz sphere has own tex coord inside it eh?

    //int neww = (int) Math.round(scaling * getTextureWidth());
    //int newh = (int) Math.round(scaling * getTextureHeight());
    //textureMy.setWidth(neww);
    //textureMy.setHeight(newh);
  }
  public int getTextureWidth() {
    return textureMy.getImageWidth();
  }
  public int getTextureHeight() {
    return textureMy.getImageHeight();
  }
  public double getKineticEnergy() {
    return 0.5 * (mass_kg) * (1000.0* getSpeedCurrent()) * (1000.0* getSpeedCurrent()); //km/s to m/s. return joules.
  }
  public double getMomentum() {
    return mass_kg * (1000.0* getSpeedCurrent());
  }
  public double getSpeedCurrent() { //in km/sec
    return Math.sqrt(xspeed*xspeed + yspeed*yspeed + zspeed*zspeed);
  }
  public Vector getXYZPredicted(int seconds) { //handy for escorting miners; don't aim at the spot they were at when move-decision was made.
    //extrapolate from thing's current_speed.
    Vector ret = new Vector(4,3);
    ret.add(0, new Double(xcoord + (xspeed * (seconds))));
    ret.add(1, new Double(ycoord + (yspeed * (seconds))));
    ret.add(2, new Double(zcoord + (zspeed * (seconds))));
    return ret;
  }
  public double getBearingXYfromSpeed() {
    //semi-verified
    if (Math.abs(xspeed) < 2.0E-30 && Math.abs(yspeed) < 2.0E-30 )
      return Math.atan2(xspeed, yspeed);
    //double dir_rad = Math.atan2(yspeed, xspeed); //out comes -pi to +pi
    double dir_rad = 0.0;
    try {
      dir_rad = ATAN2Lookup.atan2((float)xspeed, (float)yspeed); //out comes -pi to +pi
//    //System.out.println("dir_rad = Math.atan2(yspeed, xspeed) = " + dir_rad);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return dir_rad;
  }
  public double getBearingXZfromSpeed() {
    //xxx unverified
    if (Math.abs(xspeed) < 2.0E-30 && Math.abs(zspeed) < 2.0E-30 )
      return Math.atan2(xspeed, yspeed);


    double dir_rad = 0.0;
    try {
    dir_rad = ATAN2Lookup.atan2((float)xspeed, (float)zspeed); //out comes -pi to +pi
    //System.out.println("dir_rad = Math.atan2(yspeed, xspeed) = " + dir_rad);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return dir_rad;
  }
  public void reduceSpeed(double scaling) {
    if (scaling > 1.000001 || scaling < 0.001)
      throw new NullPointerException("ugh");
    xspeed = xspeed * scaling;
    yspeed = yspeed * scaling;
    zspeed = zspeed * scaling;
  }
  public void reduceSpeedToThisCap(double spd) {
    int loopbreak = 200;
    while (getSpeedCurrent() > spd) {
      loopbreak--;
      if (loopbreak < 1)
        break;
      xspeed = xspeed * 0.9992;
      yspeed = yspeed * 0.9992;
      zspeed = zspeed * 0.9992;
    }
  }
  public void reduceSpeedToThisKECap(double ke) {
    int loopbreak = 200;
    while (getKineticEnergy() > ke ) {
      loopbreak--;
      if (loopbreak < 1)
        break;
      xspeed = xspeed * 0.9992;
      yspeed = yspeed * 0.9992;
      zspeed = zspeed * 0.9992;
    }
  }
  public double getMass() {
    return mass_kg;
  }
  public int getPixelRadius() {
    return pixelradius; //a hack, for attached Vfxs
  }
  public long getId() {
    return unique_id;
  }
  public static long getNextId() {
    next_uid++;
    return next_uid;
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

  public void setX(double a) {
    xcoord = a;
  }
  public void setY(double a) {
    ycoord = a;
  }
  public void setZ(double a) {
    zcoord = a;
  }
  public void setXSpeed(double a) {
    xspeed = a;
  }
  public void setYSpeed(double a) {
    yspeed = a;
  }
  public void setZSpeed(double a) {
    zspeed = a;
  }
  public double getXSpeed() {
    return xspeed;
  }
  public double getYSpeed() {
    return yspeed;
  }
  public double getZSpeed() {
    return zspeed;
  }
  public void setCoreNote(int a) {
    corenote = a;
  }
  public int getCoreNote() {
    return corenote;
  }
  public void setMIDIInstrument(int a) {
    MIDI_instrument = a;
  }
  public int getMIDIInstrument() {
    return MIDI_instrument;
  }
  public void playNote(int offset, int volume, double dur_ticks) {
    Missilemada2.soundNotesPileAdd(new StampedNote(Missilemada2.getWorldTime() /*now*/, MIDI_instrument, corenote + offset, volume, dur_ticks, false));
  }
  public void playNoteDelayed(int offset, int volume, double dur_ticks, int waitWorldSeconds) { //please, offset -10 to 10, volume 10..127
    Missilemada2.soundNotesPileAdd(new StampedNote((Missilemada2.getWorldTime() + waitWorldSeconds) /*now plus delay*/,
          MIDI_instrument, corenote + offset, volume, dur_ticks, false));
  }
  public void setSpeed(double x, double y, double z) {
    xspeed = x;
    yspeed = y;
    zspeed = z;
  }
  public void setSpeed_mag_dirXY(double mag, double dir /*0..2PI*/) {
    xspeed = mag * Math.cos(dir);
    yspeed = mag * Math.sin(dir);
    zspeed = 0.0;
  }
  public double getDirXY() { //for setSpeed_KE_dirXY()
    return Missilemada2.calcBearingXY2D(xcoord, ycoord, xcoord + xspeed, ycoord + yspeed);
  }
  public void setSpeed_KE_dirXY(double kinetic_energy, double dir /*0..2PI*/) {
    //speed is in sort-of-pixels per second. Let's say 1000px is two meters.
    // 0.5 * mass_kg * (getSpeedCurrent()/500.0) * (getSpeedCurrent()/500.0);


    double spe = 500.0 * Math.sqrt((2*kinetic_energy)/mass_kg); // two meters is 1000 distance.
    xspeed = spe * Math.cos(dir);
    yspeed = spe * Math.sin(dir);
    zspeed = 0.0;
  }
  public Vector getXYZ_OLD() { // xxx might be slow? //YYYY takes 9.3% of CPU coz object creation... //could try reusing OBJ's XYZ_vector, not ADDING
    Vector ret = new Vector(4,3);
    ret.add(0, new Double(xcoord));
    ret.add(1, new Double(ycoord));
    ret.add(2, new Double(zcoord));
    return ret;
  }
  public Vector getXYZ() { //new 2014-07-10: reusing OBJ's XYZ_vector, not ADDING. Maybe 0.4% of CPU?
    my_xyz.setElementAt(new Double(xcoord), 0); //just a pointer change, AND new Double...
    my_xyz.setElementAt(new Double(ycoord), 1);
    my_xyz.setElementAt(new Double(zcoord), 2);
    return my_xyz;
  }
  public static Vector createXYZ(double a, double b, double c) {
    Vector ret = new Vector(4,3);
    ret.add(0, new Double(a));
    ret.add(1, new Double(b));
    ret.add(2, new Double(c));
    return ret;
  }
  public static Vector changeXYZ(Vector xyz, double a, double b, double c) { //YYY 2.5% of CPU time. I guess can't be helped. UNLESS... always hold a dummy vector in memory for this op.
    Vector ret = new Vector(4,3);
    ret.add(0, new Double( (Double)(xyz.get(0)) + a) );
    ret.add(1, new Double( (Double)(xyz.get(1)) + b) );
    ret.add(2, new Double( (Double)(xyz.get(2)) + c) );
    return ret;
  }
  public static Vector calcRelativeStanceXYZ(Vector self_xyz, Vector other_xyz, double maxdist_from_other, double mindist_from_other,
                                             double personality_LR /*0..1 with 0.5=no deviation */, double personality_nearfar /*0..1 with 1 at mindist-from-other.*/) {
    if (mindist_from_other > maxdist_from_other)
      return null;
    if (self_xyz == null || other_xyz == null)
      return null;
    if (personality_LR < 0.0 || personality_LR > 1.0)
      return null;
    if (personality_nearfar < 0.0 || personality_nearfar > 1.0)
      return null;




    double startdist = calcDistanceVecVec(self_xyz, other_xyz);
    double chosen_dist_from_self = 0.5*startdist;//xxxtemp
    //double chosen_dist_from_self = (startdist - maxdist_from_other) +  personality_nearfar * (startdist - mindist_from_other/*can be negative*/);
    if (chosen_dist_from_self > 1.3*startdist) {
      System.out.println("stance error: chose dist from self that is too large. startdist="+(startdist/Missilemada2.getBaseDistance())
              + " maxdiOth="+(maxdist_from_other/Missilemada2.getBaseDistance())+ " mindi="+(mindist_from_other/Missilemada2.getBaseDistance()));
    }
    //double chosen_dist_from_other = startdist - chosen_dist_from_self;

    //double maxdeviation_rads_from_straight = 0.5 * (110.0/180.0) * Math.PI;
    //double chosendeviation_rads = (personality_LR - 0.5) * maxdeviation_rads_from_straight



    double sidepossible = 0.1*chosen_dist_from_self; //this causes triangle shape
    //double sidepossible = startdist; //this causes rectangle to choose from
    double LR_devi = (personality_LR - 0.5) * sidepossible;
    double devi_bearing = (personality_LR - 0.5) * (3.14 / 2.5);

    //advance from self_xyz along the two bearings, to find out desired xyz.
    double bear_XY = devi_bearing + Missilemada2.calcBearingXY2D((Double) (self_xyz.get(0)), (Double) (self_xyz.get(1)),
          (Double) (other_xyz.get(0)), (Double) (other_xyz.get(1)));
    //com.tomitapio.missilemada2.ATAN2Lookup.atan2((float)LR_devi, (float)chosen_dist_from_self);
    double bear_XZ = bear_XY;
    Vector ret = new Vector(4,3);
    ret.add(0, new Double((Double)(self_xyz.get(0)) + (chosen_dist_from_self * Math.cos(bear_XY))));
    ret.add(1, new Double((Double)(self_xyz.get(1)) + (chosen_dist_from_self * Math.sin(bear_XY))));
    ret.add(2, new Double((Double)(self_xyz.get(2)) + (0.1*chosen_dist_from_self * Math.sin(bear_XZ)))); //note low desire of Z-axis change.
    //debugVFX__Text(ret, "calcstance");
    return ret;
  }
  public static Vector changeXYZTowards(Vector xyz_orig, Vector xyz_towards, double distancefragment/*km, not 20%*/) {
    //xxxx untested if output is reasonable, complex shit.

    if (xyz_orig == null) {
      System.out.println("changeXYZTowards called with null orig");
      return null;
      //return Missilemada2.getRandomLocation_using_asteroidlist();
    }
    if (xyz_towards == null) {
      System.out.println("changeXYZTowards called with null towards");
      return null;
      //nope: return xyz_orig;
      //xyz_towards = Missilemada2.getRandomLocation_using_asteroidlist();
    }
    Vector ret = new Vector(3,3);
    double fulldist = calcDistanceVecVec(xyz_orig, xyz_towards);
    if (distancefragment > fulldist) {
      //System.out.println("changeXYZTowards: asked fragment greater than fulldist");
      return xyz_towards;
    }

    double x1 = new Double((Double) xyz_orig.get(0));
    double y1 = new Double((Double) xyz_orig.get(1));
    double z1 = new Double((Double) xyz_orig.get(2));
    double x2 = new Double((Double) xyz_towards.get(0));
    double y2 = new Double((Double) xyz_towards.get(1));
    double z2 = new Double((Double) xyz_towards.get(2));

    double a;
      a = (x2 - x1)  * (distancefragment/fulldist);

    double b;
      b = (y2 - y1)  * (distancefragment/fulldist);

    double c;
      c = (z2 - z1)  * (distancefragment/fulldist);

    ret.add(0, new Double((Double)(xyz_orig.get(0)) + a));
    ret.add(1, new Double((Double)(xyz_orig.get(1)) + b));
    ret.add(2, new Double((Double)(xyz_orig.get(2)) + c));
    return ret;
  }

  public static double calcDistanceMTMT(MobileThing a, MobileThing b) {
    if (a == null || b == null) {
      System.out.println("calcDistanceMTMT(), one MT was null.");
      Exception e = new NullPointerException();
      e.printStackTrace();
      Missilemada2.pauseGame();

      return Missilemada2.world_x_max;
    }
    return Math.sqrt( (a.getX() - b.getX())*(a.getX() - b.getX())
            +  (a.getY() - b.getY())*(a.getY() - b.getY())
            + (a.getZ() - b.getZ())*(a.getZ() - b.getZ()) );
  }
  public static double calcDistance(double a, double b, double c, double x, double y, double z) {
    return Math.sqrt( (a - x)*(a - x)
            + (b - y)*(b - y)
            + (c - z)*(c - z) );
  }
  public static double calcDistanceVecVec(Vector xyz1, Vector xyz2) {
    if (xyz1 == null || xyz2 == null) {
      System.out.println("err, vecvec");
      throw new NullPointerException();
      //return Math.pow(55,30); //Missilemada2.getWorld
    }
    return Math.sqrt((((Double) xyz1.get(0)).doubleValue() - ((Double) xyz2.get(0)).doubleValue())*(((Double) xyz1.get(0)).doubleValue() - ((Double) xyz2.get(0)).doubleValue())
                   + (((Double) xyz1.get(1)).doubleValue() - ((Double) xyz2.get(1)).doubleValue())*(((Double) xyz1.get(1)).doubleValue() - ((Double) xyz2.get(1)).doubleValue())
                   + (((Double) xyz1.get(2)).doubleValue() - ((Double) xyz2.get(2)).doubleValue())*(((Double) xyz1.get(2)).doubleValue() - ((Double) xyz2.get(2)).doubleValue()));
  }
  public boolean areCoordsNearYou(Vector xyz) {
    if (calcDistanceVecVec(xyz, this.getXYZ()) < 2.0*Missilemada2.getArrivedDistance()) {
      return true;
    } else {
      return false;
    }
  }
  public void debugVFXText(Vector xyz, String tx) {
    Missilemada2.addVfx2(xyz, "TEXT", 7000, 130.0, 0.6/*transp*/, "", 1.0, tx);
  }
  public static String xyzToIntString(Vector xyz) {
    return "" + ((Double) xyz.get(0)).intValue() +"__"+ ((Double) xyz.get(1)).intValue() +"__"+ ((Double) xyz.get(2)).intValue();
  }
  public static String xyzToString(Vector xyz) {
    return "" + ((Double) xyz.get(0)).floatValue() +"__"+ ((Double) xyz.get(1)).floatValue() +"__"+ ((Double) xyz.get(2)).floatValue();
  }

  public void setTractorer(MobileThing aa) {
    tractorer = aa;
  }
  public boolean hasTractorer() {
    return (tractorer != null);
  }

}