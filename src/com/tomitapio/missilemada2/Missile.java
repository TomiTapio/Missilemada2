package com.tomitapio.missilemada2;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.Random;
import java.util.Vector;

public class Missile extends MobileThing {
  double sensor_range; // skill: how far missile can see
  double damage_yield; //megajoules.
  double turning_rate; // radians per sec. related to bearingXY.
  double max_speed;
  double max_fuel; //50 000 blab-units.
  double curr_fuel; //when zero, can't turn any more; expire.

  double buildcost;
  double distance_flown = 0.0;
  Ship parentShip;
  Faction parentFaction;
  String mislDNA;

  //boolean have_destination = false;
  //Vector current_destinationXYZ;
  Ship current_target = null;

  public double curr_hull_hp; //Mjoules. For marking missile as dead(do not try shootdown)
  private double max_hull_hp; //Mjoules

  double bearingXY; //the current (wishful??)bearing, changed by turning_rate.
  double bearingXZ;
  double COLLISION_RANGE; //from main class.
  double TURN_MARGIN; //from main class.

  public Missile (String in_dna, Ship in_parentShip, double in_bearingXY, double in_bearingXZ, double initialSpeed, Ship in_target/*null is okay*/) {
    super();
    parentShip = in_parentShip;
    if (parentShip == null) {
      System.out.println("Missile constructor func: parentShip was null.");
      return;
    }
    parentFaction = parentShip.getFaction();
    if (parentFaction == com.tomitapio.missilemada2.Missilemada2.getPlayerFaction())
      this.setIsSeenByPlayer(true); //plr sees plr-faction missiles always. But this variable expires -- update on sensor use.
    else
      this.setIsSeenByPlayer(false);

    unique_id = MobileThing.getNextId();
    current_target = in_target;
    if (in_target == null){
      //System.out.println("misl created with no ship target, dest coords BLA. Launched by "+ in_parentShip.toStringShort());
    }
    
    xcoord = parentShip.getX();
    ycoord = parentShip.getY();
    zcoord = parentShip.getZ();

    COLLISION_RANGE = Missilemada2.getMissileCollisionRangeMin(); //should depend on time tick size. // elite missiles have better explosion radius?
    TURN_MARGIN = Missilemada2.getMissileTurnMargin();

    double[] arr = dnaToParams(in_dna);
    mislDNA = in_dna;
    max_speed = arr[0];
    sensor_range = arr[1];
    damage_yield = arr[2];
    max_fuel = arr[3];
    curr_fuel = max_fuel;
    turning_rate = arr[4];
    mass_kg = arr[5];
    buildcost = arr[6];

    pixelradius = 15;
    curr_hull_hp = 200.0;
    max_hull_hp = curr_hull_hp;

    bearingXY = in_bearingXY;
    bearingXZ = in_bearingXZ;
    //bearingXY = Missilemada2.calcBearingXY2D(this, in_target);
    //bearingXZ = Missilemada2.calcBearingXZ(this, in_target);
    //xxxxxxxxxxx or ship gives its speed vector.
    xspeed = initialSpeed * Math.cos(bearingXY);
    yspeed = initialSpeed * Math.sin(bearingXY);
    zspeed = initialSpeed * Math.sin(bearingXZ);//xxxxx under work

    //setTexture("starbase.png", 1.0);
    //System.out.println("new mis " + mislDNA + " buildcost "+(int)buildcost+" mass "+(int)mass_kg + " dam " + (int) (damage_yield/10000.0)
     //       + " Spd "+(int)(max_speed*1000) + " turn " +(int)(turning_rate*100) + " maxfuel "+(int)max_fuel);
  }
  public static double[] dnaToParams(String in_dna) {
    String dna;
    if (in_dna.length() < 5){ //require min length. CAT-> CATCATCATCATCAT. q -> qqqqq
      dna = in_dna+in_dna+in_dna+in_dna+in_dna+in_dna+in_dna+in_dna;
    } else {
      dna = in_dna;
    }
    Random r1 = new Random((long) dna.codePointAt(0));
    Random r2 = new Random((long) dna.codePointAt(1));
    Random r3 = new Random((long) dna.codePointAt(2));
    Random r4 = new Random((long) dna.codePointAt(3));
    Random r5 = new Random((long) dna.codePointAt(4));
    double for_init1 = r1.nextDouble(); //must init, otherwise super low variation!
    double for_init2 = r2.nextDouble();    double for_init3 = r3.nextDouble();    double for_init4 = r4.nextDouble();    double for_init5 = r5.nextDouble();

    double spdrand = r1.nextDouble();
    double spd =  Missilemada2.getMissileSpeedMin() + spdrand*(Missilemada2.getMissileSpeedMax() - Missilemada2.getMissileSpeedMin());

    double senrand = r2.nextDouble();
    double senrange = Missilemada2.getSensorRangeMinMissile() * (1.05 + senrand); //gameplay

    double maxfuel = 29000 * (r4.nextDouble() + 0.60); //nerfed 2014-10-14

    double trrand = r5.nextDouble();
    double turnrate = 0.00108/*was:0.012, 0.0004 is too little but fun. */ * (trrand + 0.26); //rads per second? or minute? I don't know.
    if (trrand < 0.07) turnrate = turnrate * 0.79; //nerf turning of bottom 7 %.
    if (trrand > 0.93) turnrate = turnrate * 1.55; //boost turning of top 7 %.

    double yield_variance = r3.nextDouble();
    double yield = Missilemada2.getAvgMislYield() * (yield_variance + 0.50);
    if (yield_variance < 0.13) { //then puny cheapo model, BUT PLENTIFUL. Hinders enemy defense-beam use.
      yield = 0.65 * yield;
      turnrate = 0.7*turnrate;
      maxfuel = 0.95*maxfuel;
      spd = 0.75*spd;
    }
    if (yield_variance > 0.92) { //then high-dam elite, pricey.
      yield = 1.25 * yield;
      turnrate = 1.1*turnrate;
      maxfuel = 1.26*maxfuel;
      spd = 1.1*spd;
    }

    double mass_kg = 40
            + 0.0000008 * ((1.7*yield)-29000)
            + 40*(1.0+((0.1*spd)*(0.1*spd)*(0.1*spd) -1.0))
            + 99 * turnrate
            + 0.00059 * maxfuel; //formula.
    //int for human-redable debug.
    //rebalanced 2014-06-24.
    int cost_from_mass    = new Double(5.3*mass_kg).intValue();
    int cost_from_speed   = new Double(41.0*( (0.99+spd)*(0.99+spd) -1.9)).intValue();
    int cost_from_yield   = new Double(0.057*(yield-(14500))).intValue();
    int cost_from_turn    = new Double(13300.0*((1.1+turnrate)*(1.1+turnrate))).intValue();
    int cost_from_maxfuel = new Double(0.45 * maxfuel).intValue();
    int cost_from_sensors = new Double(0.049*senrange).intValue();
    double buildcost = cost_from_mass + cost_from_speed + cost_from_yield + cost_from_turn + cost_from_maxfuel + cost_from_sensors;  //formula. mass is a penalty ofcoz
    buildcost = 0.00014 * buildcost; //global scaling
    double[] ret = new double[7];
    ret[0] = spd;
    ret[1] = senrange;
    ret[2] = yield;
    ret[3] = maxfuel;
    ret[4] = turnrate;
    ret[5] = mass_kg;
    ret[6] = buildcost;
//  for (int i = 0; i < 7; i++) {
//    System.out.println(" i"+i+":" + ret[i]);
//  }
    return ret;
  }
  public double getTurningRate() {
    return turning_rate;
  }
  public Faction getFaction() {
    return parentFaction;
  }
  public double getCost() {
    return buildcost;
  }
  public double getSensorRange() {
    return sensor_range;
  }
  public double getYield() {
    return damage_yield;
  }
  public void advance_time(double seconds) {
    if (Missilemada2.getWorldTimeIncrement() < 1) return;

    if (Missilemada2.gimmeRandDouble() < 0.04)
      setIsSeenByPlayer(false);

    Vector myvisibleships = Missilemada2.getShips_XYNearest(xcoord, ycoord, 1.15*sensor_range, 5/*max 5 nearest ships to see*/);
    //ignore target's stealth on keeping_target.
    if (current_target != null) {
      //check target is still there, not destroyed. within sensor radius. AND hasn't defected to us.
      if (myvisibleships.contains(current_target)
              && (current_target.getFaction() != parentFaction )
              && calcDistanceMTMT(this,current_target) < sensor_range) {
        //ok
        //xxdraw target marker - not needed.
        //System.out.println("Misl: Visib still contains target "+current_target.toString());
      } else {
        current_target = null;
        //Missilemada2.removeMissile(this); //dont delete misl on target lost. no fun.
      }
    }
    //look for enemy target within sensor radius
    //current_destinationXYZ = null; ///////////////parentShip.getParentFaction().getXYZ(); //xxx hax
    Ship candidate = null;
    int siz = myvisibleships.size();
    if (siz > 0) {
      for (int i = 0; i < siz; i++) {
        candidate = (Ship) myvisibleships.elementAt(i);
        if (candidate != null) {
        if (calcDistanceMTMT(this, candidate) < sensor_range * candidate.getStealthFactor()/*max 1.15 min 0.55*/ ) { //this added coz on RTree using lost Z dimension distance.

          if ( (parentFaction != candidate.getFaction()) && candidate.getHP() > 10.0) { //if not ally, and alive, set as target.
            if (current_target != null) { //if have target, check for closer target (GAMEPLAY)
              if (calcDistanceMTMT(this, candidate) < calcDistanceMTMT(this, current_target) /*&& Missilemada2.gimmeRandDouble() < 0.3 */) { //if ca is closer, AND RAND, switch to ca.
                current_target.undoGotTargeted();
                current_target = candidate;

                //draw target marker
                //if (isSeenByPlayer()) {
                //Missilemada2.addVfx2(current_target.getX(), current_target.getY(), current_target.getZ(), "GOT_TARGETED", 1500, 910.0, this, "scan.png", 1.0, "");

                current_target.gotTargeted();
                //current_destinationXYZ = current_target.getXYZ();
                //System.out.println("Misl "+getId()+" acquired CLOSER target "+candidate.toString());

              } else { //keep curr but check if it's dead.
                if (current_target.isDestroyed()) {
                  current_target = null;
                  reduceSpeed(0.988); //slow down to become more agile.
                } else {
                  //if very close to target, slow down. xxhack to stop spiral around target.
                  if (calcDistanceMTMT(this, current_target) < 0.8*Missilemada2.getArrivedDistance()) {
                    reduceSpeed(0.97);
                  }
                }
                //current_target.gotTargeted();
                //System.out.println("Misl "+getId()+" KEPT target "+current_target.getId());
              }
            } else { //no curr, acquire candidate.
              current_target = candidate;
              current_target.gotTargeted();
              if (isSeenByPlayer()) {
                Missilemada2.addVfxOnMT(xcoord + (current_target.getRadius() * (Missilemada2.gimmeRandDouble() - 0.5)), ycoord, zcoord,
                        "MISSILES_FIRST_TARGET", 100/*sec*/, 40/*siz*/, 0.09/*transp*/, null/*this*/, "scan.png", 1.0, "");
              }
            }
          } else { //else ally
            //System.out.println("Misl "+getId()+" saw ally "+candidate.toString());
          }
        }
      }
      }
    } else { //see no foe or friend ships, uhhhhh
      current_target = null;
      //did above before for loop: current_destinationXYZ = getXYZ();
    }

    if (curr_fuel > 0.0 && current_target != null) {
      boolean turned = false;
      //cap the turnrate, if have massive 90-1000 seconds in one frame. over-turning is bad.
      double timescaled_turnrate = (turning_rate * seconds);
      if (timescaled_turnrate > 3.14 / 2.0)
        timescaled_turnrate = 3.14 / 2.0;

      //change bearingXY more towards target
      double desired_bearingXY = Missilemada2.calcBearingXY(this, current_target);
      if (desired_bearingXY > bearingXY + Math.PI + TURN_MARGIN) {
        bearingXY = bearingXY - timescaled_turnrate; //TURN LEFT ACROSS ZERO
        turned = true;
      } else if (desired_bearingXY < bearingXY - Math.PI - TURN_MARGIN) {
        bearingXY = bearingXY + timescaled_turnrate; //TURN RIGHT ACROSS ZERO
        turned = true;
      } else if (desired_bearingXY > (bearingXY + TURN_MARGIN)) { //then TURN RIGHT normally
        bearingXY = bearingXY + timescaled_turnrate; //TURN RIGHT
        turned = true;
      } else if (desired_bearingXY < (bearingXY - TURN_MARGIN)) { //then turn left normally
        bearingXY = bearingXY - timescaled_turnrate; //TURN LEFT
        turned = true;
      } //else no need to turn.

      //xxx turn on the XZ plane..... similarly.....
      double desired_bearingXZ = Missilemada2.calcBearingXZ(this, current_target);
      if (desired_bearingXZ > bearingXZ + Math.PI + TURN_MARGIN) {
        bearingXZ = bearingXZ - timescaled_turnrate; //TURN LEFT ACROSS ZERO
        turned = true;
      } else if (desired_bearingXZ < bearingXZ - Math.PI - TURN_MARGIN) {
        bearingXZ = bearingXZ + timescaled_turnrate; //TURN RIGHT ACROSS ZERO
        turned = true;
      } else if (desired_bearingXZ > (bearingXZ + TURN_MARGIN)) { //then TURN RIGHT normally
        bearingXZ = bearingXZ + timescaled_turnrate; //TURN RIGHT
        turned = true;
      } else if (desired_bearingXZ < (bearingXZ - TURN_MARGIN)) { //then turn left normally
        bearingXZ = bearingXZ - timescaled_turnrate; //TURN LEFT
        turned = true;
      } //else no need to turn.


      if (turned) { //used some fuel because of turning thrusters.
        //possibly vanish from player faction's knowledge (= don't draw)
        if (Missilemada2.gimmeRandDouble() < 0.024) {
          setIsSeenByPlayer(false); //and can get seen again my player ships' sensor use.
        }

        //misl slows down on purpose, when turning.
        //BADDDD reduceSpeed(0.98);

        double usage = (1.0+0.09*max_speed+5.9*turning_rate)*(1.0+0.09*max_speed+5.9*turning_rate)
                + 0.05*(0.9 + mass_kg/880)*(0.9 + mass_kg/880); //bloody difficult to balance
        curr_fuel = curr_fuel - (0.0049 * usage * seconds); //gameplay tuning spot
        //System.out.println("Missile of cost "+buildcost+" burn fuel in turning tick:" + usage + " blab is "+(-0.36 + 1.05*max_speed+3.5*turning_rate));
      }
      //flip across bearingXY boundary
      if (bearingXY < 0.0) {
        bearingXY = 2*Math.PI + bearingXY; //2 o'clock
      } else if (bearingXY > 2*Math.PI) {
        bearingXY = bearingXY - 2*Math.PI; //4 o'clock
      }
      //xxx flip across xz
      if (bearingXZ < 0.0) {
        bearingXZ = 2*Math.PI + bearingXZ; //2 o'clock
      } else if (bearingXZ > 2*Math.PI) {
        bearingXZ = bearingXZ - 2*Math.PI; //4 o'clock
      }

    } else {
      //else keep current bearings due to no target. Volley'ed missiles often this.
    }

    //------ begin move calc ------
    double curr_accel = max_speed / (Missilemada2.SECONDS_TO_MAX_SPEED / 10.0); //20? sec to reach zero to max.
  /*  double des_x = 0.0;
    double des_y = 0.0;
    double des_z = 0.0;
    double bear_xy_from_desti = 0.0;
    double bear_xz_from_desti = 0.0;
    double bear_xy, bear_xz;*/

    //xxcannot: if (getSpeedCurrent() < max_speed) { // if not at max speed, can accelerate(change direction).

    xspeed = xspeed + (curr_accel * seconds) * Math.cos(bearingXY); //bear from turning x radians per second. bearingXY gets turned towards the desired bearing.
    yspeed = yspeed + (curr_accel * seconds) * Math.sin(bearingXY);
    zspeed = zspeed + (curr_accel * seconds) * Math.sin(bearingXZ);

    //cap their speed at max speed. Quite important for missile behavior and thus gameplay.
    reduceSpeedToThisCap(max_speed);

    xcoord = xcoord + (xspeed * seconds);
    ycoord = ycoord + (yspeed * seconds);
    zcoord = zcoord + (zspeed * seconds);
    prev_xcoord = xcoord;
    prev_ycoord = ycoord;
    prev_zcoord = zcoord;

    //quick-n-dirty aste collision check
//    Asteroid as = Missilemada2.getRandomAsteroid();
//    if (as.areCoordsInsideYou(this.getXYZ())) {
//      //then misl collides with aste.
//      Missilemada2.removeMissile(this);
//    }

    //if moved into current_explosion, get damaged, or get destroyed by the plasma forces.
    Vector curr_exp_xyz = Missilemada2.getCurrentExplosionLocation();
    if (curr_exp_xyz != null) {
      if (calcDistanceVecVec(curr_exp_xyz, this.getXYZ()) < Missilemada2.getCurrentExplosionRange()) {
        if (Missilemada2.gimmeRandDouble() < 0.05) {
          Missilemada2.removeMissile(this);
          System.out.println("*******Missile "+getId()+" removed coz travelled through misl explosion.");
          //if "return;" here then this one can't explode this turn and turned useless. instead, worse yield because premature boom.
          damage_yield = damage_yield * 0.5;
        } else {
          max_speed = max_speed * 0.8;
          sensor_range = sensor_range * 0.7;
          turning_rate = turning_rate * 0.7;
          //yield stays the same.

        }

      }
    } //else no active missile_explosion on the map in this time-tick.

    //xxx Missilemada2.setCurrentExplosion(this.getXYZ(), COLLISION_RANGE); //other missiles passing through explosion, may get damaged or destroyed!

    //end move calc
    double movement_in_timetick = getSpeedCurrent() * seconds; //km
    distance_flown = distance_flown + movement_in_timetick;

    if (distance_flown > Missilemada2.getMissileRangeMax()) {
      //System.out.println("Misl "+getId()+": flew for way too long distance. reducing fuel.");
      curr_fuel = 0.988*curr_fuel;
      //Missilemada2.removeMissile(this);
    }

    //fly straight, using tiny bit of fuel to power systems & navigational deflectors & sensor sweeps.
    curr_fuel = curr_fuel - 0.010*movement_in_timetick; //gameplay important.

    //check for collision with any ship, priority over asteroid hits.
    //xxx could try this INSIDE SENSOR SWEEP
    //STARBASE is now a subtype of ship, for simplicity.
    try {
      Vector coll_range_ships = Missilemada2.getShips_XYNearest(xcoord, ycoord, 1.3 * COLLISION_RANGE, 2);
      Ship tryship = null;
      siz = coll_range_ships.size();
      if (siz > 0) {
        for (int i = 0; i < siz; i++) {
          tryship = (Ship) coll_range_ships.elementAt(i);
          if (tryship != null) {
            if (this.parentFaction != tryship.getFaction() /*don't hit friend*/
                    && calcDistanceMTMT(this, tryship) < COLLISION_RANGE+2*tryship.getRadius() /*don't hit too-far one*/
                    /*&& !tryship.isDestroyed()*/ ) { //----if not ally, and within range, hit it.

              //System.out.println("Misl "+getId()+": hit enemy ship: "+tryship.toString() + " for "+damage_yield);
              tryship.getDamaged(damage_yield, "missile", parentShip, false);
              parentShip.damage_dealt = parentShip.damage_dealt + damage_yield;
              if (parentShip.getFaction() == Missilemada2.getPlayerFaction() && tryship.isSeenByPlayer()) {
                Missilemada2.changeWorldTimeIncrement(-1); //slow down world, because combat is happening.
              }
              Missilemada2.setCurrentExplosion(this.getXYZ(), 0.9*COLLISION_RANGE); //other missiles passing through explosion, may get damaged or destroyed!
              if (parentShip.getFaction() == Missilemada2.getPlayerFaction() || tryship.getFaction() == Missilemada2.getPlayerFaction()) {
                if (damage_yield > 0.5*Missilemada2.getAvgMislYield()) //if not bottom 25% of missiles, light source.
                  Missilemada2.setDynamicLight("MISSILE_EXP", 0.2f, Missilemada2.getWorldTime()+1250, this.getXYZ());
              }
              //vfx: MISSILE_HIT
              if (isSeenByPlayer()) {
                playMissileExploded(buildcost);
                double randx = 29000*(Missilemada2.gimmeRandDouble() -0.5);
                Missilemada2.addVfxOnMT(randx, 0.6 * randx, 0, "MISSILE_EXP", 1800/*sec*/, 1290.0 * (damage_yield / Missilemada2.getAvgMislYield())/*siz*/, 0.4/*transp*/, this, "orb_hotplasma.png", 1.0, "");
                if (Missilemada2.gimmeRandDouble() < 0.3) {
                  if (this.isSeenByPlayer()) {
                    Missilemada2.createDebrisFlatSprite("missile-hit-ship_debris.png", 5.5 * (0.50 + Missilemada2.gimmeRandDouble()), 1350.0 * (1.0 + Missilemada2.gimmeRandDouble()), 1350.0 * (1.0 + Missilemada2.gimmeRandDouble()), this, false, false);
                    Missilemada2.sendDebrisTowardsCamera("BATTLE", this);
                  }
                }
              }
              Missilemada2.removeMissile(this);
              //break; //no break means damage multiple ships that in range of yield.
            } else { //missile super close to ally OR WRECK. Misl does costly emergency dodge.
              //System.out.println("Misl "+getId()+": ignored friendly ship: "+currship.toString());
              //missile slowed down due to a friendly in the way. //EMERGENT BEHAVIOR? ughhh bad when 20 ship pile! misl run out of fuel so much.
/*
              if (tryship != this.parentShip) { // no slowdown due to ship that fired this.
                max_speed = 0.88 * max_speed;
                turning_rate = 0.9 * turning_rate;
                curr_fuel = 0.75 * curr_fuel;
                //tryship.youHinderedMissile();
              }
*/
            }
          }
        }
      }
    } catch (Exception e) {
      System.out.println("Missile: ship collision exception... " +  e.toString());
    }

    //check for collision with any asteroid, nahhh

    if (curr_fuel < 0.07 * max_fuel) { // at 10% switch to low-fuel mode and draw pretty trail
      if (Missilemada2.gimmeRandDouble() < 0.07) {
        if (isSeenByPlayer()) {
          //Missilemada2.addVfx2(getXYZ(), "MISL_LOWFUEL", 2400/*sec*/, 700.0/*siz*/, 0.6/*transp*/, "scan.png", 0.2, "");
        }
      }
      turning_rate = turning_rate * 0.998;
      max_speed = max_speed * 0.999;
      //curr_fuel = curr_fuel * 1.02; //xxx a hack
      if (max_speed < 0.3*Missilemada2.getMissileSpeedMin()) //if became too slow, remove from game. Do not become a mine.
        curr_fuel = -0.001;
    }

    if (curr_fuel < 0.01) {
      //System.out.println("Misl "+mislDNA+" (cost "+getCost()+") expired, distance flown: "+ distance_flown); //flown 422k - 635k
      //Missilemada2.addToVFXList(xcoord, ycoord, "MISSILE_EXPIRED", 2800, null);
      if (isSeenByPlayer())
        Missilemada2.createDebrisFlatSprite("missile_expired.png", getSpeedCurrent()*0.3, 950.0*(1.0+Missilemada2.gimmeRandDouble()), 950.0*(1.0+Missilemada2.gimmeRandDouble()), this, true, false);
      Missilemada2.removeMissile(this); //missile is now useless, as it can be dodged very easily.
      parentShip.reportSampleMislFuelRange(distance_flown);
    }

    //if extra much out of bounds, destroy. //xx not needed, fuel limits.
  }
  public double getDefenseBeamEvasion() {
    //xx want sort of 0.04 to 0.95
    double yi = damage_yield / Missilemada2.getAvgMislYield(); //high yield hinders evasion
    double sp = 0.9*max_speed / Missilemada2.getMissileSpeedMin();
    double tu = turning_rate / (0.012 * (1 + 0.35));
    double se = 0.5*sensor_range / Missilemada2.getSensorRangeMinMissile();
    double normalized0_1 = 0.50*(-yi + sp + tu + se);
    //System.out.println("mis eva "+ normalized0_1); // -0.3 to 1.1, okay.
    double foo = 0.61 * (0.04 + 0.91*normalized0_1);
    return foo;
  }
  private void playMissileExploded(double missile_cost) {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("ko-hackazat", 1, "") /*Vector of pitches*/, 25 /*core note*/, 32 /*slapbass*/, 94, 4.3F /*note duration*/);
  }
  public double getHullPerc() { //for logic
    return curr_hull_hp / max_hull_hp;
  }

  public void drawMissile(float scale1) {
    float height = 3100.0f;
    float width  = 3100.0f;
    if (textureMy != null) {
      //other obj draw have enabled these:      //GL11.glEnable(GL11.GL_TEXTURE_2D);
      textureMy.bind();
    } else {
      //texture disable was before missiles' for-loop.
    }
    GL11.glPushMatrix();
//debug zcoord and dist to target.
//          GL11.glPushMatrix();
//          //GL11.glPushAttrib(xxxxxxx);
//          String tx;
//          if (current_target != null)
//            tx = " z: "+(int)(zcoord/1000.0) + " di "+ (int)(calcDistanceMTMT(this, current_target) / 1000.0);
//          else
//            tx = " z: "+(int)(zcoord/1000.0);
//          Missilemada2.drawTextXYZ(Missilemada2.getFont60fornowwww(0), 190f,  (float)xcoord, (float)(ycoord), (float)zcoord,
//                  tx, org.newdawn.slick.Color.white);
//          GL11.glPopMatrix();

    //String tx = ""+(int)(100*bearingXY) + "xy "+ (int)(100*bearingXZ) +"xz";
    //Missilemada2.drawTextXYZ(Missilemada2.getFont60fornowwww(0), 130f,  (float)xcoord, (float)(ycoord), (float)zcoord,
    //        tx, org.newdawn.slick.Color.white);

    //draw faint red line to target.
    if (current_target != null) {
      GL11.glColor4f(0.9f, 0.2f, 0.2f, 0.22f);
      FlatSprite.drawFlatLineVecVec(this.getXYZ(), current_target.getXYZ(), 480.0*scale1);
    }
    GL11.glTranslated(xcoord, ycoord, zcoord);
    Color co = parentShip.getMissileColor();
    GL11.glColor4f(co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 0.9f);
    float yieldfac = (float)(1.2*getYield()/Missilemada2.getAvgMislYield());
    float speedfac = (float)(max_speed / Missilemada2.getMissileSpeedMax()); //speed is VISIBLE, no need to draw it.
    float turnfac = (float) (turning_rate / (0.056 * (1.0 + 0.5)));
    float sensfac = (float) sensor_range / (float)(1.2/*xx very important for gameplay!*/ * Missilemada2.getSensorRangeMinMissile() * (1.0 + 0.55));
    float fuelfac = (float) (max_fuel / 49000 * (1.0 + 0.62) );

    //draw sensors, before shape-deform-scale.
//    if (Missilemada2.gimmeRandDouble() < 0.02) {
//    Sphere s = new Sphere();
//    s.setDrawStyle(GLU.GLU_SILHOUETTE); //fill, line, silhouette, point
//    s.draw((float)sensor_range, 8, 8);
//    }

    //try rotate to match its heading! //xxfun?
    GL11.glRotatef((float)(bearingXY*(180.0/Math.PI)), 0f, 0f, 1.0f);

    float rota = (float) ((180.0/Math.PI) * (getBearingXYfromSpeed() - Math.PI / 2.0)); //okay at  (float) ((180.0/Math.PI) * (getBearingXYfromSpeed() - Math.PI / 2.0))
    GL11.glRotatef(rota, 0f, 0f, 1f); //rotate on z-axis so x and y change.
    GL11.glScalef(scale1*yieldfac, scale1*yieldfac, scale1);

    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex3f(0f, 0f, 0f);                       GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glVertex3f(0f, /*speedfac* */height, 0f);     GL11.glNormal3f(0.0f, 0.1f, 1.0f);
    GL11.glVertex3f(sensfac*width, turnfac*height, 0f);GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glVertex3f(fuelfac*width, 0f, 0f);            GL11.glNormal3f(0.0f, 0.1f, 1.0f);
    GL11.glEnd();

    GL11.glPopMatrix();
  }
  public static String enforceMissileDNA_maxfuel(String in_dna) {
    //4th char to "q". 012q4
    String ret = in_dna.substring(0,3) + "q" /*missile max fuel enforced*/ + in_dna.substring(4);
    return ret;
  }
}
