package com.tomitapio.missilemada2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;
import java.awt.*;
import java.util.Comparator;
import java.util.Vector;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;

public class Asteroid extends MobileThing implements Comparable<Asteroid> { /* creation Date: 30.12.2013 */
  Vector my_mining_site;
  double hull_hp; //current, Mjoules
  double max_hull_hp; //Mjoules
  double radius; // km
  float x_stretch;
  float initial_rotation_x, initial_rotation_z;
  Color aste_color;
  String aste_type; //NORMAL, maybe NEVERSPLIT

  FlatSprite fake;

  double resource_fuel = 0.0;
  double resource_metal1 = 0.0;
  double resource_metal2 = 0.0;
  double resource_metal3 = 0.0;
  int ra6,ra7;

  private static String randomAsteType() {



    return "NORMAL";
  }

  public Asteroid (double size_km, Vector xyz) {
    super();
    unique_id = MobileThing.getNextId();
    //xxx verify inputs

    mass_kg = size_km * 400500500500.0; //1.5km to 115 cube km. mass 4.3 * 115 is 500 000 000 000?
    radius = size_km;
    pixelradius = (int)(size_km *7.0); //for visual debris
    recalc();

    xcoord = ((Double)xyz.get(0)).doubleValue();
    ycoord = ((Double)xyz.get(1)).doubleValue();
    zcoord = ((Double)xyz.get(2)).doubleValue();
    my_mining_site = this.getXYZ(); //initial.
    aste_type = "NORMAL"; //xxx
    xspeed = com.tomitapio.missilemada2.Missilemada2.getAsteDriftSpeed() * (com.tomitapio.missilemada2.Missilemada2.gimmeRandDouble() -0.5);
    yspeed = com.tomitapio.missilemada2.Missilemada2.getAsteDriftSpeed() * (com.tomitapio.missilemada2.Missilemada2.gimmeRandDouble() -0.5);
    zspeed = com.tomitapio.missilemada2.Missilemada2.getAsteDriftSpeed() * (com.tomitapio.missilemada2.Missilemada2.gimmeRandDouble() -0.5);

    //no DNA, just random chance in creation.
    //rand resources. Color is revealed to player when aste is scouted.
    int ranred = com.tomitapio.missilemada2.Missilemada2.gimmeRandInt(20);
    int ranot = com.tomitapio.missilemada2.Missilemada2.gimmeRandInt(20);

    double ran = com.tomitapio.missilemada2.Missilemada2.gimmeRandDouble();
    double tex_scale_rand = 0.8 + 8.0* com.tomitapio.missilemada2.Missilemada2.gimmeRandDouble();
      if (ran > 0.0 && ran < 0.65) { //65% nothing.
        //no resources.
        aste_color = new Color(30+ranred,30+ranot,30+ranot);
        setTexture("mohave_marble_mywrap.png", 1.0+(tex_scale_rand/8.0));
      }
      if (ran > 0.65 && ran < 0.73) { //8% aste have fuel components
        resource_fuel = 0.01 * (mass_kg / 1000.0); //tons
        aste_color = new Color(89,85,20);
        setTexture("sliced_gneiss_light.png", tex_scale_rand);
      }
      if (ran > 0.73 && ran < 0.80) { //7% aste have m1
        resource_metal1 = 0.01 * (mass_kg / 1000.0); //tons. the common hullbuilding metal.
        aste_color = new Color(50,50,90);
        setTexture("azul256.png", tex_scale_rand);
      }
      if (ran > 0.80 && ran < 0.86) { //6% aste have m2
        resource_metal2 = 0.005 * (mass_kg / 1000.0); //tons.
        aste_color = new Color(60,115,70);
        setTexture("oro_esotico_marble.png", tex_scale_rand);
      }
      if (ran > 0.86 && ran < 0.92) { //6% aste have m3+m1
        resource_metal3 = 0.007 * (mass_kg / 1000.0); //tons.
        resource_metal1 = 0.005 * (mass_kg / 1000.0); //tons.
        aste_color = new Color(90,30,110);
        setTexture("crema_valencia_marble.png", tex_scale_rand);
      }
      if (ran > 0.92 && ran < 1.0001) { //8% aste have m3+m2
        resource_metal3 = 0.009 * (mass_kg / 1000.0); //tons.
        resource_metal2 = 0.003 * (mass_kg / 1000.0); //tons.
        aste_color = new Color(120,110,80);
        setTexture("golden_spider_marble.png", tex_scale_rand);
      }

      //rand elongation
      x_stretch = 0.70f + (float)(1.5*Missilemada2.gimmeRandDouble());
      //rand rotation
      initial_rotation_x = (float) (Missilemada2.gimmeRandInt(179) * 2.0*(Missilemada2.gimmeRandDouble() -0.5));
      initial_rotation_z = (float) (Missilemada2.gimmeRandInt(179) * 2.0*(Missilemada2.gimmeRandDouble() -0.5));
      //rand poly count of sphere
      ra6 = 5+Missilemada2.gimmeRandInt(4);
      ra7 = 6+Missilemada2.gimmeRandInt(5);

    fake = new FlatSprite((float)(20.0*radius), (float)(20.0*radius), xcoord, ycoord, zcoord + 2000.0, "fake_asteroid.png", 1.0, 1.0f/*transp*/);

      int ca = 40 + (int) (Missilemada2.gimmeRandDouble() * 70.0);
      if (aste_type.equals("NORMAL"))
        aste_color = new Color(ca+9, ca, ca); //gray-reddish

      //sound stuff:
      setMIDIInstrument(116/*taiko drum*/);
      int cor = 120-pixelradius;
      if (cor < 20)
        cor = 25;
      setCoreNote(cor);
    }
  public double getHP() {
    return hull_hp;
  }
  public double getRadius() {
    return radius;
  }
  public Vector getMiningXYZ(){
    return getXYZ();
/*  possibly very bad:
    //rand chance to change spot.
    if (Missilemada2.gimmeRandDouble() < 0.002) {
      my_mining_site = new Vector(3,3);
      my_mining_site.add(0, new Double(xcoord + (Missilemada2.gimmeRandDouble() - 0.5)*13.6*radius));
      my_mining_site.add(1, new Double(ycoord + (Missilemada2.gimmeRandDouble() - 0.5)*13.6*radius));
      my_mining_site.add(2, new Double(zcoord + (Missilemada2.gimmeRandDouble() - 0.5)*10.5*radius));
    }
    return my_mining_site;
*/
  }
  public int compareTo(Asteroid n) { //in case wanted to z-depth sort for drawing order. unneeded now that use smaller distances.
    Integer cd  = new Integer((int)Math.round(2500.0*zcoord));
    Integer cdn = new Integer((int)Math.round(2500.0*n.getZ()));
    return cdn.compareTo(cd);
  }
  public boolean isResourceless() {
    return (resource_fuel < 0.1 && resource_metal1 < 0.1 && resource_metal2 < 0.1 && resource_metal3 < 0.1);
  }
  public double getResourcesTotalAmt() {
    return resource_fuel + resource_metal1 + resource_metal2 + resource_metal3;
  }
  public boolean hasResource(String res) {
    if (res.equals("FUEL") && resource_fuel > 1.1)
      return true;
    if (res.equals("METAL1") && resource_metal1 > 1.1)
      return true;
    if (res.equals("METAL2") && resource_metal2 > 1.1)
      return true;
    if (res.equals("METAL3") && resource_metal3 > 1.1)
      return true;
    return false;
  }
  public void recalc() { //after mass changed due to split.
    hull_hp = (5 * 26000000) * (mass_kg / (100 * 58000000) ); //calc from mass, want much over ships' 26000000 MJ ??
    max_hull_hp = hull_hp;
    //recalc new radius km.


    //DONT radius = 0.0008 * Math.cbrt(mass_kg); //world units, for collisions
    //System.out.println("Asteroid recalc: mass_kg: "+mass_kg+" kg, radius: " + radius + " pixel_radius: "+pixelradius);
  }
  public boolean areCoordsInsideYou(Vector xyz) {
    double dist = calcDistanceVecVec(xyz, this.getXYZ());
    if (dist < this.radius) {
      return true;
    } else
      return false;
  }
  public void advance_time (double seconds) {
    if (Missilemada2.getWorldTimeIncrement() < 1) return; //zero means pause

    //if too close to a base, fucks up the mining logic. So... move asteroid. Should only effect start hours of game.
    if (Missilemada2.gimmeRandDouble() < 0.004*seconds) {
      if (Missilemada2.isAsteTooCloseToABase(this) && Missilemada2.getWorldTime() < 50000) {
        ycoord = ycoord + 0.94*Missilemada2.getCombatDistMin_Generic();
        xcoord = xcoord + (0.5 - Missilemada2.gimmeRandDouble()) * Missilemada2.getCombatDistMin_Generic();
      }
    }


    xcoord = xcoord + (xspeed * (seconds));
    ycoord = ycoord + (yspeed * (seconds));
    zcoord = zcoord + (zspeed * (seconds));
    prev_xcoord = xcoord;
    prev_ycoord = ycoord;
    prev_zcoord = zcoord;

    //update location of our flatsprite.
    fake.setX(xcoord);
    fake.setY(ycoord);
    fake.setZ(zcoord);

    //random chance to reset the "is in combat zone" bool.
    if (Missilemada2.gimmeRandDouble() < 0.0001*seconds) {
      setIsNearBattle(false);
    }
    //asteroids flake debris sprites by themselves! makes world more alive.
    if (Missilemada2.gimmeRandDouble() < (0.000006 * seconds)) {
      Missilemada2.createDebrisFlatSprite("mining_debris.png", 1.7*(0.10+Missilemada2.gimmeRandDouble()), 1350.0*(1.0+Missilemada2.gimmeRandDouble()), 900.0, this, false, false);
    }
  }
  public double/*tons gotten*/ mining(String res, double in_seconds, Ship mining_ship) {

    double seconds = 0.4 * in_seconds; //adjust mining rate.

    if (res.equals("FUEL") && resource_fuel > 1.1) {
      resource_fuel = resource_fuel - 0.015*seconds;
      return 0.015*seconds;
    }
    if (res.equals("METAL1") && resource_metal1 > 1.1) {
      resource_metal1 = resource_metal1 - 0.006*seconds;
      return 0.006*seconds;
    }
    if (res.equals("METAL2") && resource_metal2 > 1.1) {
      resource_metal2 = resource_metal2 - 0.006*seconds;
      return 0.006*seconds;
    }
    if (res.equals("METAL3") && resource_metal3 > 1.1) {
      resource_metal3 = resource_metal3 - 0.003*seconds;
      return 0.003*seconds;
    }
    return 0.0;
  }
  public boolean hasWantedResource(String res) { //YYYY takes 5.7% of CPU.
    if (res.equals("FUEL") && resource_fuel > 10.1) {
      return true;
    }
    if (res.equals("METAL1") && resource_metal1 > 10.1) {
      return true;
    }
    if (res.equals("METAL2") && resource_metal2 > 10.1) {
      return true;
    }
    if (res.equals("METAL3") && resource_metal3 > 10.1) {
      return true;
    }
    return false;
  }
  public void drawAsteroid(float scale1) {
    GL11.glEnable(GL_DEPTH_TEST);
    Sphere s = new Sphere();
    GL11.glPushMatrix();

    if (Missilemada2.isAsteroidKnownToPlayerFaction(this)) {
      //special func for player_displaying.
      //use revealed-to-player colors&textures
      //use textures
      if (textureMy != null) {
        //GL11.glEnable(GL11.GL_TEXTURE_2D);
        //GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.
        textureMy.bind(); //SLOW says profiler!     ///GL11.glBindTexture(GL11.GL_TEXTURE_2D, 1);
        s.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."
      }

      //s.setNormals(GLU.GLU_SMOOTH); //none, flat, smooth.
      s.setNormals(GLU.GLU_SMOOTH); //COULD BE SLOW



      s.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point

      float red = aste_color.getRed() / 255.0f;
      float green = aste_color.getGreen() / 255.0f;
      float blue = aste_color.getBlue() / 255.0f;
      GL11.glColor4f(red, green, blue, 1.0f);

      GL11.glTranslatef((float)xcoord, (float)ycoord, (float)zcoord);
      //all the asteroid rotatef took 15.5% of cpu time!!
      GL11.glRotatef(initial_rotation_z, 0.0f, 0.0f, 1.0f);
      GL11.glRotatef(initial_rotation_x, 0.0f, 0.0f, 1.0f);
      double rot = Math.signum(initial_rotation_x) * 0.0010* Missilemada2.getWorldTime()%360.0; //xxxx initial rand rota speed.
      double rot2 =Math.signum(initial_rotation_z) * 0.0005 * Missilemada2.getWorldTime()%360.0;
      GL11.glRotatef((float)rot, 0.0f, 0.0f, 1.0f);
      GL11.glRotatef((float)rot2, 0.0f, 1.0f, 0.0f);
      GL11.glScalef(x_stretch, 1.0f, 1.0f);
      s.draw(scale1*(float)radius*10.0f, ra7,ra6);

    } else { //color indicates unscouted asteroid.
      //no texture for unknown ones. big speedup!?!?
      //GL11.glDisable(GL11.GL_TEXTURE_2D);
      s.setTextureFlag(true);
      textureUnknAste.bind();
      s.setNormals(GLU.GLU_FLAT); //COULD BE SLOW
      s.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point

      //use whatever, for speedup.
      //Missilemada2.setOpenGLMaterial("UNSCOUTED_ASTEROID");
      GL11.glColor4f(0.1f, 0.2f, 0.4f, 1.0f); //change alpha for ghostly orbs.

      GL11.glTranslatef((float)xcoord, (float)ycoord, (float)zcoord);
      GL11.glScalef(x_stretch, 1.0f, 1.0f);
      GL11.glRotatef(60f, 0.0f, 0.0f, 1.0f);
      GL11.glRotatef(initial_rotation_x, 1.0f, 0.0f, 0.0f);
      GL11.glRotatef(initial_rotation_z, 0.0f, 0.0f, 1.0f);

      s.draw(scale1*(float)radius*10.0f, 5,5); //CRUDE COZ FAR

    }
    GL11.glPopMatrix();

  }

  public void drawYourFakeSprite(float aste_visualscaling) {
    fake.drawFlatSprite(1.7f, (float)(this.getSpeedCurrent()*18000.0) /*rota*/, true);
  }

  static class AsteroidComparator implements Comparator<Asteroid> {
    @Override
    public int compare(Asteroid a, Asteroid b) {
      return a.compareTo(b);
    }
  }
}
