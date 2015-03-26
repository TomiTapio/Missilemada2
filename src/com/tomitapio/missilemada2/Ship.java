package com.tomitapio.missilemada2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Disk;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;
import org.newdawn.slick.Color;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import static org.lwjgl.opengl.GL11.GL_AMBIENT_AND_DIFFUSE;
import static org.lwjgl.opengl.GL11.GL_EMISSION;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;

public class Ship extends MobileThing implements Comparable<Ship> {
  String type;
  String ship_sprite_filename = "";

  String name;
  int curr_crew_count;
  int max_crew_count;
  Faction parentFaction; //null means dead, derelict ship.
  double radius;
  int tracerticks = 0;
  long timestamp_next_allowed_accel = 0; //delay for some operations like refit and unload
  long beam_available_timestamp = 0;

  String shipDNA;
  double buildcost;

  String misl_DNA;
  double misl_cost = 0.0;  //set on successful missile buying.
  double misl_speed = 0.0; //set on successful missile buying.
  double misl_sensorrange = Missilemada2.getMissileRangeMax(); //optimistic first estimate.
  double misl_range_measured = Missilemada2.getMissileRangeMax(); //then misl sends reports that change this, hopefully.
  int misl_fuelreportcount = 0;

  //double firing_bearing = 0.0; //same as facing I guess. But we have no facing, only bearing from current speed or from current destination.

  int see_asteroid_count = 0;
  Asteroid closestAsteroid; //good to know
  Asteroid destinationAsteroid; //only for miners
  Vector seenAsteroids_thistimetick; //this timetick seen ones, not other times

  double cargo_capacity, cargo_carried; //tons, I guess.
  boolean cargo_full = false;
  boolean did_mining = false;

  int see_enemy_count = 0;
  boolean am_under_fire = false;
  boolean dodge_mode = false;
  Ship ene_closest = null;
  Ship ene_least_dangerous = null;
  Ship ene_most_dangerous = null;
  Ship ene_most_damaged = null;
  boolean might_stay_in_formation_OBSO = false;
  double my_dist_from_frontline = 0.0;
  private double LR_stance = 0.5;

  private double danger_meter = 0.0;

  Missile tryShootDownMissile = null;
  Missile stored_tryShootDownMissile = null;

  int see_friend_count = 0;
  double see_total_enemy_battlestr = 0.0;
  double see_total_friend_battlestr = 0.0;
  Ship buddy_mil = null; //follow buddy somewhat, maybe try to assist or copy target
  Ship buddy_civilian = null; //follow buddy somewhat? maybe try to assist, at least maintain radio contact.
  Ship buddy_derelict = null; //for tractor-beam dragging
  boolean tractormode = false;

  int see_enemy_mislcount = 0;
  int see_enemy_mislcount_close = 0;
  int see_friend_mislcount = 0;
  int num_missiles_targeting_me;

  boolean have_target;
  boolean target_too_far_for_misl_avg;
  Ship current_target;
  Ship prev_timetick_target;

  boolean have_destination = false;
  boolean forceddestination = false;
  String destination_desc = "";
  Vector current_destinationXYZ;
  //vector of XYZ vectors -- places we have visited recently, so don't visit twice. if SCOUT and currdest x-near recent, req new dest.
  Vector visited_destinations_XYZs_fooooooo;

  boolean shield_flash = false;
  double max_battle_str = 0.0; //remember max so can calc current_battlestr_percentage

  double curr_kinectic_energy;
  double max_accel;
  double max_speed; //xx poss move var to MT
  double curr_hull_hp; //current, Mjoules
  double max_hull_hp; //Mjoules
  double max_shields; //Mjoules
  double curr_shields; //Mjoules
  double shield_regen_per_min; //Mjoules per second
  double sensor_range; // in world coords. super important. skill: how far ship can see buddies/enemies/missiles.
  double stealth; //resist enemy sensors. 0.0(assault cruiser, so large) to 1.0(sensor satellite, tiny and non-moving)

  double buildcredits_gen_per_minute; //for missile fire rate. Nanotech.
  double curr_buildcredits;
  double max_buildcredits;
  double defense_beam_accuracy; //0..1, skill of antimissile defense beam targeting.
  double defense_beam_rechargebasethou = 1000.0; //for merit-upgrading of recharge.
  double personality_aggro; //0..1

  double caused_destruction = 0.0; //in buildcost
  double spent_on_misl = 0.0; //in buildcost
  double damage_sustained = 0.0; //Mjoules (??on hull??)
  double damage_dealt = 0.0; //sort of killcount, Mjoules

  double mislcred_shot_down_xxxxtodo = 0.0;

  double timeAlive = 0;
  double score = 0; //dealt+sustained+shotdown, time alive.
  double merit_badges_curr = 0.0; //for scouting and killing and ship_convert_hacking and mining. One or two merit badges buys ship an upgrade I guess.
  double merit_badges_lifetime = 0.0;

  int beam_atk_draw_counter_frames = 0;
  int beam_def_draw_counter_frames = 0;

  //ship systems have repair percentages. Sensors to zero, engine to zero(kaboom), life support to zero(crew dies)... misl system, shie system, defe system, mining system
  double engine_status = 1.0; //0.0 - 1.0 (100%)
  double lifesupport_status = 1.0;
  double sensors_status = 1.0;
  double shield_status = 1.0;

  double missilesystem_status = 1.0;
  double beamsystem_status = 1.0; //atk and defe beam
  double miningsystem_status = 1.0; //even ones that don't have mining gear??

  private HashMap<String, Double> cargoMapStrToDouble;
  double cargo_delivered_lifetime = 0.0;
  int carried_sensats = 0;
  int carried_beamdrones = 0;
  int carried_missiledrones = 0;

  public Ship(String intype, Faction inf, Vector startXYZ, String in_dna, String texturefilename, boolean needs_crew) {
    super();
    //xxxverify inputs
    type = intype;
    xcoord = ((Double) startXYZ.get(0)).doubleValue();
    ycoord = ((Double) startXYZ.get(1)).doubleValue();
    zcoord = ((Double) startXYZ.get(2)).doubleValue();
    xspeed = 0.0;
    yspeed = 0.0;
    zspeed = 0.0;
    cargoMapStrToDouble = new HashMap<String, Double>();
    cargo_carried = 0.00001; //tons

    //xxfuture: can randomize ship graphics on creation.
    if (type.equals("MINER")) {
      ship_sprite_filename = "miner2.png";      }
    if (type.equals("TINYMINER")) {
      ship_sprite_filename = "tinyminer.png";    }
    if (type.equals("SCOUT")) {
      ship_sprite_filename = "scout_v3.png";    }
    if (type.equals("MISSILEDRONE")) {
      ship_sprite_filename = "missiledrone.png";    }
    if (type.equals("BEAMDRONE")) {
      ship_sprite_filename = "beamdrone.png";    }
    if (type.equals("SENSAT")) {
      ship_sprite_filename = "sensat.png";      }
    if (type.equals("DEFENDER")) {
      ship_sprite_filename = "defender.png";    }
    if (type.equals("AC")) {
      ship_sprite_filename = "assault_cruiser_mk1_transp.png";
      if (Missilemada2.gimmeRandDouble() < 0.5)
        ship_sprite_filename = "assault_cruiser_2.png";
      if (Missilemada2.gimmeRandDouble() < 0.5)
        ship_sprite_filename = "assault_cruiser_3.png";
    }

    //facing??  , separate from speed bearingXY.

    parentFaction = inf;
    unique_id = MobileThing.getNextId();
    LR_stance = Missilemada2.gimmeRandDouble(); //0..1, left or right preference in stances.

    //parse dna into vars
    String dna;
    if (in_dna.length() < 10){ //require minlength: CAT-> CATCATCATCATCAT.
      dna = in_dna+in_dna+in_dna+in_dna+in_dna+in_dna+in_dna+in_dna+in_dna+in_dna;
    } else {
      dna = in_dna;
    }
    if (intype.equals("STARBASE"))
      in_dna = "aaaaaaaaaaaaSTARBASESTANDARD"; //gameplay issue: qqqqqqqqq too powerful base perhaps.

    Random r1 = new Random((long) dna.codePointAt(0)); //hull
    Random r2 = new Random((long) dna.codePointAt(1)); //max shields
    Random r3 = new Random((long) dna.codePointAt(2)); //shield recharge speed
    Random r4 = new Random((long) dna.codePointAt(3)); //speed
    Random r5 = new Random((long) dna.codePointAt(4)); //sensor range, (determines how smart?)
    Random r6 = new Random((long) dna.codePointAt(5)); //buildcredits per minute
    //Random r7 = new Random((long) dna.codePointAt(6)); //prob used to be maxbc
    Random r8 = new Random((long) dna.codePointAt(7)); //defense beam accuracy (can be none)
    Random r9 = new Random((long) dna.codePointAt(8)); //stealth
    double for_init = r1.nextDouble(); //must init, otherwise super low variation!
    for_init = r2.nextDouble();    for_init = r3.nextDouble();    for_init = r4.nextDouble();    for_init = r5.nextDouble();    for_init = r6.nextDouble();
    for_init = r8.nextDouble();  for_init = r9.nextDouble();
    shipDNA = in_dna;
    misl_DNA = Missilemada2.randomDNAFromStr(in_dna);
        Missile tempMisl = new Missile(misl_DNA, this, 0.0, 0.0, 0.0, null);
        misl_cost = tempMisl.getCost();
        misl_sensorrange = tempMisl.getSensorRange();

    //things and then branch on ship type.
    double hull_factor = r1.nextDouble();
    if (hull_factor < 0.07)
      hull_factor = hull_factor * 0.55;  //bottom 6%
    if (hull_factor > 0.92)
      hull_factor = hull_factor * 1.2; //top 6%

    double senrange_core = Missilemada2.getSensorRangeMinShip(); //GAMEPLAY
    double speedrand = r4.nextDouble();
    double miner_speed = (8.0/25.0) * getAvgScoutSpeed() * (speedrand + 0.5);

    double maxsh_rand = r2.nextDouble();
    double maxshield_factor = 0.32 * (1.2*maxsh_rand + 0.07); //GAMEPLAY
    double shregen_rand = r3.nextDouble();
    //double shieldregenpermin_factor = 0.22 * (r3.nextDouble() + 0.07); //GAMEPLAY
    double shieldregenpermin_factor = 0.32 * (1.38*shregen_rand + 0.06); //GAMEPLAY

    //missile build credits nanotech
    double bcps_rand = r6.nextDouble();
    double bcps_factor = (bcps_rand + 0.22);
    double bcps_scaling = 0.000078;   //GAMEPLAY
    double bc_storage_scaling = 17.0; //GAMEPLAY

    if (type.equals("MINER")) { // spd "8", sets the speed standard for other types.
      curr_crew_count = 4;
      max_speed = miner_speed; //km per sec
      cargo_capacity = 200.0 * (1.0 + 0.4*hull_factor); //tons
      sensor_range = senrange_core * 1.44 * (r5.nextDouble() + 0.36);
      stealth = 0.15 * r9.nextDouble(); //minimal stealth capability, low shields low engines.
      curr_hull_hp = (60.0/25.0) * getAvgScoutHullHP() * (0.12+hull_factor); //in MJ. NN teraJ avg...
      defense_beam_accuracy = 0.0; //defense beam to nullify missiles. miner has only attack beam.
      personality_aggro = 0.0;

      max_shields = 7500500 * (maxshield_factor + 0.05); //in MJ. Even very puny shield helps versus mining debris.
      curr_shields = max_shields / 14.5;
      shield_regen_per_min = 2000 * shieldregenpermin_factor; //in MJ / sec. also powers the beam weapons.

      if (speedrand > 0.9) { //chance of elite speed.
        max_speed = 1.2*max_speed;
        stealth = 0.0; //extra glowy engines
        //punier shields, coz prefer speed.
        max_shields = 0.4 * max_shields;
        shield_regen_per_min = 0.3 * shield_regen_per_min;
      }

      //missile build credits nanotech - OLD:miner has none. NEW: 0.8 of scout.
      buildcredits_gen_per_minute = bcps_scaling * 230 * (bcps_factor - 0.22); //cheap miner shouldn't have missiles.
      if (buildcredits_gen_per_minute < 0.0) {
        buildcredits_gen_per_minute = 0.0;
        max_buildcredits = 0.0;
      } else {
        max_buildcredits = 0.25 * bc_storage_scaling * 4.25 * 60.0 * (buildcredits_gen_per_minute + 0.5); //how many hours' worth of missiles piled up?
      }
      curr_buildcredits = max_buildcredits / 5.0;
      carried_sensats = 1; //to warn us of foes near asteroid we have visited...
    }
    if (type.equals("TINYMINER")) { //spd "13" verycheap, vulnerable
      curr_crew_count = 0;
      max_speed = (12.5/8.0) * miner_speed; //km per sec
      //cargo_capacity = 80.0; //tons (80 tons, 20 cubic meters)
      cargo_capacity = 8.0 * (1.0 + 7.0*hull_factor); //tons
      sensor_range = senrange_core * 0.35 * (r5.nextDouble() + 0.30); //? needed for mining distance to be successful
      stealth = 0.0; //no stealth capability
      curr_hull_hp = 0.55*getAvgScoutHullHP() * (0.29+hull_factor); //in MJ.
      defense_beam_accuracy = 0.0; //defense beam to nullify missiles
      personality_aggro = 0.0;

      max_shields = 0.0;
      curr_shields = 0.0;
      shield_regen_per_min = 0.0; //in MJ / sec. also powers the beam weapons.

      buildcredits_gen_per_minute = 0.0;
      curr_buildcredits = 0.0;
      max_buildcredits = 0.0;
      //note: system statuses are 1.0 even when don't have the beam or misl or shield system.
    }
    if (type.equals("DEFENDER")) { //spd "14", defense beam specialist, especially for escorting miners.
      curr_crew_count = 5;
      max_speed = (14.0/8.0) * miner_speed; //km per sec
      cargo_capacity = 0.0; //tons
      sensor_range = senrange_core * 1.53 * (r5.nextDouble() + 0.45);
      stealth = 0.15 * r9.nextDouble(); //some stealth capability
      curr_hull_hp = (90.0/25.0) * getAvgScoutHullHP() * (0.43+hull_factor); //in MJ. NN teraJ avg...
      defense_beam_accuracy = 1.0 + 0.93 * r8.nextDouble(); //defense beam to nullify missiles, defender's specialty
      personality_aggro = 0.7;

      max_shields = 75500500 * (maxshield_factor + 0.35); //in MJ
      curr_shields = max_shields / 14.5;
      shield_regen_per_min = 96500 * shieldregenpermin_factor; //in MJ / sec. also powers the beam weapons.

      buildcredits_gen_per_minute = bcps_scaling * 2950 * (0.40 + bcps_factor);
      if (bcps_factor > 0.85)
        buildcredits_gen_per_minute = 1.3 * buildcredits_gen_per_minute;
      max_buildcredits = bc_storage_scaling * 4.35 * 60.0 * buildcredits_gen_per_minute; //how many hours' worth of missiles piled up?
      curr_buildcredits = max_buildcredits / 3.0;
      carried_sensats = 1;
    }
    if (type.equals("SCOUT")) { //spd "20"
      curr_crew_count = 2;
      max_speed = (19.0/8.0) * miner_speed; //km per sec
      //cargo_capacity = 32.0; //tons, 1/50th of miner. Like Star Trek runabout.
      cargo_capacity = 35.0 * ( -0.6 + 1.2*hull_factor); //tons
      if (hull_factor > 0.97)
        cargo_capacity = 1.5*cargo_capacity;
      if (cargo_capacity < 0.0)
        cargo_capacity = 0.0;
      sensor_range = senrange_core * 1.51 * (r5.nextDouble() + 0.47);
      stealth = 0.95 * r9.nextDouble(); //jolly good stealth capability, but expensive
      curr_hull_hp = getAvgScoutHullHP() * (0.35 + 1.3*hull_factor); //in MJ. NN teraJ avg...
      defense_beam_accuracy = 0.39 + 0.83 * r8.nextDouble(); //defense beam to nullify missiles
      personality_aggro = 0.20;

      max_shields = 25500500 * (maxshield_factor + 0.10); //in MJ
      curr_shields = max_shields / 14.5;
      shield_regen_per_min = 19500 * shieldregenpermin_factor; //in MJ / sec. also powers the beam weapons.

      if (speedrand < 0.15) { //chance of turtle speed -> more shields.
        stealth = 1.2 * stealth;
        max_shields = 1.15 * max_shields;
        shield_regen_per_min = 1.15 * shield_regen_per_min;
      }
      buildcredits_gen_per_minute = bcps_scaling * 470 * (2.5*bcps_factor + 0.15);
      max_buildcredits = bc_storage_scaling * 4.65 * 60.0 * buildcredits_gen_per_minute; //how many hours' worth of missiles piled up?
      curr_buildcredits = max_buildcredits; //may have crap misl-production; start as full.
      carried_sensats = 1;
    }
    if (type.equals("MISSILEDRONE")) { //spd "x", simple drone, no shields no beam. GOOD SENSORS
      curr_crew_count = 0;

      max_speed = (18.0/8.0) * miner_speed; //little slower than scout. must travel lots to resupply.
      cargo_capacity = 0.0; //tons
      /////sensor_range = senrange_core * 1.15 * (r5.nextDouble() + 0.21);
      sensor_range = senrange_core * 1.58 * (r5.nextDouble() + 0.47); //same as scout, coz otherwise useless...?
      stealth = 0.0;    //0.27 * r9.nextDouble(); //minimal stealth capability, coz no shields and slow. Low emissions.
      curr_hull_hp = 0.82 * getAvgScoutHullHP() * (0.48+hull_factor); //in MJ. NN teraJ avg... //fraction of scout hull.
      defense_beam_accuracy = 0.0; //defense beam to nullify missiles
      personality_aggro = 0.35;

      max_shields = 0.0;
      curr_shields = 0.0;
      shield_regen_per_min = 0.0; //in MJ / sec. also powers the beam weapons.

      buildcredits_gen_per_minute = 0.0; //0.0 of scout, should resupply at base.
      max_buildcredits = bc_storage_scaling * 9.95 * 60.0 * (bcps_scaling * 700.0); //how many hours' worth of missiles piled up?
      curr_buildcredits = max_buildcredits; //base loaded it up.

      misl_DNA = Missile.enforceMissileDNA_maxfuel(misl_DNA);
    }
    if (type.equals("BEAMDRONE")) { //spd "15", because supposed to escort others more than the missiledrone. No missiles.
      curr_crew_count = 0;
      max_speed = (15.0/8.0) * miner_speed; //km per sec
      cargo_capacity = 0.0; //tons
      sensor_range = senrange_core * 1.0 * (r5.nextDouble() + 0.21); //worse than scout or missile drone, is defensive escorter.
      stealth = 0.0; //zero stealth, cheap and/or strong shields
      curr_hull_hp = 0.44 * getAvgScoutHullHP() * (0.6+hull_factor); //in MJ. NN teraJ avg... //half of scout hull.
      defense_beam_accuracy = 0.44 + 0.9 * r8.nextDouble(); //defense beam to nullify missiles
      personality_aggro = 0.25;

      max_shields = 16500500 * (maxshield_factor + 0.45); //in MJ
      curr_shields = max_shields / 4.5;

      shield_regen_per_min = 14500 * shieldregenpermin_factor; //in MJ / sec. also powers the beam weapons. strong.
      if (shregen_rand > 0.86) {
        shield_regen_per_min = 1.4 * shield_regen_per_min;
        max_shields = 1.2 * max_shields;
      }
      if (speedrand > 0.82) { //chance of elite speed.
        max_speed = 1.27*max_speed;
      }
      buildcredits_gen_per_minute = 0.0;
      curr_buildcredits = 0.0;
      max_buildcredits = 0.0;
    }
    if (type.equals("SENSAT")) { //spd 0, deploy-from-(scout,AC,defe) sensor satellite. sort of missiledrone without moving or missiles.
      curr_crew_count = 0;
      max_speed = 0.0; //km per sec
      cargo_capacity = 0.0; //tons
      sensor_range = senrange_core * 1.25 * (0.5 + 0.21);
      stealth = 1.0 * 1.0; //max stealth capability, tiny and low-power.
      curr_hull_hp = 0.08 * getAvgScoutHullHP(); //in MJ.
      defense_beam_accuracy = 0.0; //defense beam to nullify missiles
      personality_aggro = 0.0;

      max_shields = 0.0;
      curr_shields = 0.0;
      shield_regen_per_min = 0.0; //in MJ / sec. also powers the beam weapons.

      buildcredits_gen_per_minute = 0.0; //0.0 of scout, should resupply at base.
      max_buildcredits = 0.0;
      curr_buildcredits = 0.0;
    }
    if (type.equals("AC")) { //spd "12" kinda slow
      curr_crew_count = 17; //therefore good chance of SYSTEM repairs. (not hull hp repairs)
      max_speed = (13.0/8.0) * miner_speed; //km per sec
      cargo_capacity = 0.0; //tons
      sensor_range = senrange_core * 1.58 * (0.96 + 0.47); //NOTE, standardised ELITE senrange. For gameplay.
      stealth = 0.0; //no stealth capability
      curr_hull_hp = (450.0/25.0) * getAvgScoutHullHP() * (0.6+hull_factor); //in MJ. NN teraJ avg...
      defense_beam_accuracy = 1.5 + r8.nextDouble(); //defense beam to nullify missiles
      personality_aggro = 1.0;

      max_shields = 320500500 * (maxshield_factor + 0.31); //in MJ
      curr_shields = max_shields / 10.0;
      shield_regen_per_min = 460500 * shieldregenpermin_factor; //in MJ / sec. also powers the beam weapons.

      buildcredits_gen_per_minute = bcps_scaling * 11050 * (0.75 + bcps_factor); //four defenders worth?
      if (bcps_factor > 0.90)
        buildcredits_gen_per_minute = 1.4 * buildcredits_gen_per_minute;
      max_buildcredits = bc_storage_scaling * 5.15 * 52.0 * buildcredits_gen_per_minute; //how many hours' worth of missiles piled up?
      curr_buildcredits = max_buildcredits / 13.0;

      //deployable drones, to help defend and to take some of enemy fire.
      carried_sensats = 3;
      carried_beamdrones = 2;
      carried_missiledrones = 1;
    }
    if (type.equals("STARBASE")) {  //speed almost zero
      curr_crew_count = 18; //therefore good chance of SYSTEM repairs. (some hull hp repairs because are a base.)
      max_speed = Missilemada2.getBaseSpeed(); //km per sec, from scenario config
      System.out.println("DEBUG: base speed is "+(max_speed/miner_speed)+" of rand miner speed.");
      cargo_capacity = 0.0; //tons
      sensor_range = senrange_core * 1.33 * (0.90 + 0.90); //NOTE, standardised senrange for all faction bases. For gameplay.
      stealth = 0.0; //no stealth capability, and bases must be equal
      curr_hull_hp = (400.0/25.0) * getAvgScoutHullHP() * (0.5+hull_factor); //in MJ. NN teraJ avg...
      defense_beam_accuracy = 1.5 + r8.nextDouble(); //defense beam to nullify missiles
      personality_aggro = 0.85;

      max_shields = 210500500 * (maxshield_factor + 0.13); //in MJ //nerf from 24 to 21
      curr_shields = max_shields / 12.5;
      shield_regen_per_min = 190500 * shieldregenpermin_factor; //in MJ / sec. also powers the beam weapons. //nerf from 28 to 18

      buildcredits_gen_per_minute = bcps_scaling * 7250 * (bcps_factor + 0.39); //base has xxdefender firepower, but more storage for missiles.
      max_buildcredits = bc_storage_scaling * 5.0 * 60.0 * buildcredits_gen_per_minute; //how many hours' worth of missiles piled up?
      curr_buildcredits = max_buildcredits / 4.0; //note, starts with decent pile.

      //alternate gameplay: base has no missiles, can die to a pile of scouts waiting outside beamrange.
          //buildcredits_gen_per_minute = 0.0;
          //curr_buildcredits = 0.0;
          //max_buildcredits = 0.0;
      //NO deployable drones in a mobile base.

      misl_DNA = Missile.enforceMissileDNA_maxfuel(misl_DNA);
    }

    //normalize defense accuracy to 0..1
    defense_beam_accuracy = defense_beam_accuracy / 2.5/*AC's max defe*/;

    max_crew_count = curr_crew_count;
    max_hull_hp = curr_hull_hp;
    max_accel = max_speed / Missilemada2.SECONDS_TO_MAX_SPEED;
    //if speed is 1 km/sec. accel is 0.003 km/s2. N accel units in 100seconds.

    //mass from type and systems
    mass_kg = 1.13 * (
              8500 /* 8.5 tons minimal chassis. */
            + 5.9 * curr_hull_hp
            + 20 * (0.2+(sensor_range/Missilemada2.getArrivedDistance()))
            + 28.0 * (max_shields/5)
            + 130.5 * shield_regen_per_min
            + 2600.0 * (2.0+max_speed)
            + 35.0 * max_buildcredits
            + 3930 * buildcredits_gen_per_minute //ramscoop or something to get materials for missile-nanotech
            + 7350 * defense_beam_accuracy
            + 1200*cargo_capacity); //formula
    mass_kg = mass_kg / 1000.0; //global scaling. want 75 ton avg scout.
    max_mass_kg = mass_kg; //can lose hull plating in battle. So later can restore to orig mass.

    //separate int_vars so can human-read in debug. not 5.54624246234642E9
    //rebalanced 2014-06-23. rebalanced 2014-07-09.
    int cost_from_mass    = new Double(0.55*mass_kg).intValue();
    int cost_from_crewarea= new Double(10500.0 * max_crew_count).intValue(); //crew don't have price yet -- this is lifesupport for miner 4ppl. //crew's repair tools cost too.
    int cost_from_minertech=new Double(3100.0 * cargo_capacity).intValue(); //because MINER must be expensive.
    int cost_from_HP      = new Double(0.00270 * curr_hull_hp).intValue();
    int cost_from_spd     = new Double(4300.0 * (max_speed) * (Math.sqrt(mass_kg) / 1000.0)).intValue(); //was super puny 15000 cost on AC
    int cost_from_maxbc   = new Double(400500.0 * (max_buildcredits/3200.0)).intValue(); /*for missiledrone's powerfulness */
    int cost_from_bcpm    = new Double(75500500.0*(buildcredits_gen_per_minute/24.0)).intValue();
    int cost_from_maxshi  = new Double(0.0079 * max_shields).intValue();
    int cost_from_shgen   = new Double(120.0 * shield_regen_per_min).intValue(); //YYYYxxxx was VERY dominant in str/cost !
    int cost_from_sensors = new Double(0.42 * (sensor_range)).intValue(); //senra/Missilemada2.getArrivedDistance() was bad?
    int cost_from_stealth = new Double(124500.0 * stealth).intValue();
    int cost_from_defeAcc = new Double(380500.0 * defense_beam_accuracy).intValue();
    buildcost = 3.9/*was4.4*/ * (
                      cost_from_mass
                    + cost_from_crewarea
                    + cost_from_minertech
                    + cost_from_HP
                    + cost_from_spd
                    + cost_from_maxbc
                    + cost_from_bcpm
                    + cost_from_maxshi
                    + cost_from_shgen
                    + cost_from_sensors
                    + cost_from_stealth
                    + cost_from_defeAcc );
    buildcost = buildcost / 1000.0; //three zeroes away.
    if (type.equals("SCOUT")) {
      System.out.print("");
    }

    if (buildcost < 0.001)
      throw new NullPointerException("foi");

    //end DNA

    if (isDrone())
      name = "DX"+unique_id;
    else
      name = Missilemada2.getRandomVulcanName();

    did_mining = false;
    am_under_fire = false;
    num_missiles_targeting_me = 0;
    have_target = false;
    current_target = null;
    current_destinationXYZ = null; //if , parentFaction.getXYZ(); then bad, circular if base is a ship, now.
    visited_destinations_XYZs_fooooooo = new Vector(50,50);

    corenote = 80; // MIDI note 0..127
    MIDI_instrument = 1; //default acoustic piano

    if (texturefilename.length() > 4)
      setTexture(texturefilename, 1.0);
    tracerticks = Missilemada2.gimmeRandInt(6);
    radius = 32+Math.sqrt(2250 * (max_hull_hp / getAvgScoutHullHP()));
    pixelradius = 55; //xx
    max_battle_str = getBattleStr();

    if (false) { //for debug
      System.out.print("possible ship: " + type + " " + in_dna + " "
              + (int) (buildcost / 1.0) + " cost "
              + (int) (mass_kg / 1000.0) + " tons "
              + (int) (curr_hull_hp / 1000.0) + " kHP "
              + (int) (max_speed * 10.0) + " speed "
              + (int) (stealth * 100.0) + " stealth "
              + (int) (buildcredits_gen_per_minute * 5000.0) + " BCpm "
              + (int) (max_buildcredits * 10.0) + " maxBC "
              + (int) (max_shields / 1000.0) + " kMaxSh "
              + (int) (shield_regen_per_min / 1000.0) + " ShGen "
              + (int) (sensor_range) + " kmSens "
              + (int) (defense_beam_accuracy * 10000.0) + " defeAcc ");
      System.out.print("str/cost " + (int) (max_battle_str / buildcost) + " ");
      System.out.println("str/mass " + (int) (max_battle_str / mass_kg) + " ");
    }

    //if faction not have enough crew... HANDLE in add-ship-to-world.
  }
  public Vector getResourcesRequired() {
    if (buildcost < 0.001)
      throw new NullPointerException("foi");

    Vector ret = new Vector(4,1);
    //xxbranch on type?
    //gameplay tuning, because asteroid rarity:
    ret.add(new Double(1.0*buildcost / 33.0)); //fuel
    ret.add(new Double(1.0*buildcost / 28.0)); //m1
    ret.add(new Double(1.0*buildcost / 45.0)); //m2
    ret.add(new Double(1.0*buildcost / 66.0)); //m3
    //System.out.println(toString()+ " cost "+getCost() +" needs total resources: "+ret.toString());
    return ret;
  }
  public double getRadius() {
    return radius;
  }
  public double getCost() {
    return buildcost;
  }
  public double getMaxShields() {
    return max_shields;
  }
  public double getCargoCapa() {
    return cargo_capacity;
  }
  public double getSystemsStatusAvg() { //1.0 is fully repaired!
    if (isDestroyed())
      return 0.0;
    double ret = (lifesupport_status + engine_status + shield_status + beamsystem_status + missilesystem_status + sensors_status + miningsystem_status) / 7.0;
    if (ret < (6.5/7.0)) { //if some systems damage, a vfx icon.
      if (Missilemada2.gimmeRandDouble() < 0.02) {
        if (isInPlayerFaction() || Missilemada2.getPlayerFaction().isShipScouted(this)) {
          Missilemada2.addVfxOnMT(getX(), getY() - 10.0 * radius, getZ() + 4.0 * radius, "SOME_SYSTEMS_DAMAGE", 1400, 480.0, 0.5/*transp*/, null, "systems_damage_some.png", 1.0, "");
        }
      }
    }
    if (ret < (3.5/7.0)) { //if HEAVY systems damage, a vfx icon.
      if (Missilemada2.gimmeRandDouble() < 0.07) {
        if (isInPlayerFaction() || Missilemada2.getPlayerFaction().isShipScouted(this)) {
          Missilemada2.addVfxOnMT(getX(), getY() - 10.0 * radius, getZ() + 4.0 * radius, "HEAVY_SYSTEMS_DAMAGE", 1400, 680.0, 0.5/*transp*/, null, "systems_damage_heavy.png", 1.0, "");
        }
      }
    }
    return ret;
  }
  public boolean isCoordsInsideYou(Vector XYZ) { //easy on a sphere.
    double in_x = (Double) XYZ.get(0);
    double in_y = (Double) XYZ.get(1);
    double in_z = (Double) XYZ.get(2);
    double dist =  Math.sqrt( (in_x - xcoord) * (in_x - xcoord) +  (in_y - ycoord) * (in_y - ycoord) + (in_z - zcoord) * (in_z - zcoord) );
    if (dist < this.radius) {
      return true;
    } else
      return false;
  }
  public boolean isInCombat() {
    if (current_target != null || am_under_fire)
      return true;
    else
      return false;
  }
  private boolean isMissileComingForMe(Missile m) {
    //for shootdown. if it is already past me, target some other missile...

    //?? predictTargetsPosition(m, jkhdfjkgh)

            return true;
  }
  public void setDes(Vector xyz, String debugwhy) {
    //verify valid in-playarea coords
    if (xyz == null) {
      System.out.println("setDes: a "+type+" wants null desti: "+debugwhy);
      return;
    }
    if (!Missilemada2.areCoordsWithinPlayarea(xyz)) {
      //Missilemada2.addToHUDMsgList("setDes: a "+type+"wants outside of play area");
      System.out.println("setDes: a "+type+"wants outside of play area: "+debugwhy);
      return;
    }

    if (xyz == null) {
      //throw new NullPointerException();
      debugVFXText(this.getXYZ(), "NULL_"+debugwhy);
      have_destination = false;
    } else {
      if (forceddestination) {
        //keep forced!
        debugVFXText(xyz, "try setDes:" + debugwhy + ":but keep forceddes");
      } else {
        current_destinationXYZ = xyz;
        have_destination = true;
        destination_desc = debugwhy;
        //forced is already false.
        debugVFXText(xyz, debugwhy);
      }
    }
  }
  public void forceDestination(Vector xyz, String debugwhy) {
    destination_desc = debugwhy;
    //verify valid in-playarea coords
    if (isStarbase() && xyz == null) {
      Missilemada2.addToHUDMsgList("forceDes: a stooopid starbase wants null forceDes");
      return; //xx hotfix
    }
    if (!Missilemada2.areCoordsWithinPlayarea(xyz)) {
      Missilemada2.addToHUDMsgList("forceDes: a "+type+"wants outside of play area");
    }

    //if major damage, refuse forced.
    if (getBattleStrPerc() < 0.75 /*&& Missilemada2.gimmeRandDouble() < 0.15*/) {
      //if desti is base, obey anyway.
      if (calcDistanceVecVec(xyz, parentFaction.getXYZ()) < 1.3*Missilemada2.getArrivedDistance()) {
        //desti is base
      } else {
        //if (isInPlayerFaction())
        //  Missilemada2.addToHUDMsgList("Our " + this.toStringShort() + " too damaged, refused a force_desti("+debugwhy+").");
        return;
      }
    }

    //Missilemada2.addVfxOnMT(0,0,0, "TEXT", 15900, 200.0, 0.6/*transp*/, this, "", 1.0, unique_id+" desti order rec");
    //Missilemada2.addVfxOnMT((Double) xyz.get(0),(Double) xyz.get(1),(Double) xyz.get(2), "TEXT", 15900, 200.0, 0.6/*transp*/, null, "", 1.0, unique_id+" desti order rec");
    //if (isInPlayerFaction())
    //  System.out.println(this.toStringShort() + " got forced desti: "+debugwhy+": "+ (Double) xyz.get(0) +"--"+ (Double) xyz.get(1) +"--"+ (Double) xyz.get(2));

    if (xyz == null) {
      //throw new NullPointerException();
      debugVFXText(this.getXYZ(), "NULL_FORCE!"+debugwhy);
      have_destination = false;
      forceddestination = false;
    } else {
      current_destinationXYZ = xyz;
      have_destination = true;
      forceddestination = true;
      debugVFXText(xyz, "FORCE!"+debugwhy);
    }
  }
  public void clearDestination() {
    current_destinationXYZ = null;
    have_destination = false;
    forceddestination = false;
    buddy_civilian = null;
    buddy_mil = null;
    //buddy_derelict = null; //?
    destinationAsteroid = null;
    // xxx  visited_destinations_XYZs_fooooooo.add(currentlocationvec)
  }
  public boolean equals(Object o) { //for score list
    if (!(o instanceof Ship))
      return false;
    Ship n = (Ship) o;
    return unique_id == n.unique_id;
  }
  public int hashCode() {
    Double cdDouble = new Double(caused_destruction);
    return (int) (64*shipDNA.hashCode() + cdDouble.hashCode() + unique_id);
  }
  public String toString() {
    if (parentFaction == null) {
      return "Derelict "+ type+" ship "+getId()+" ("+shipDNA + ") (cost "+Integer.toString((int) (buildcost / 1.0))+")";
    } else {
      return "F"+ parentFaction.getFactionId()+" "+type+" ship "+getId()+"("+getName()+", "+shipDNA + ") cost "
              +Integer.toString((int)(buildcost/1.0))+", strmax "+(int)max_battle_str+", str/cost "+(int)(max_battle_str/buildcost)
              +" mislrange "+(int)misl_range_measured+", "+getHowManyMislWhenFull()+" missiles.";
    }
  }
  public String toStringLeaderboard() { //don't say which faction, show battlestr, str perc, shield perc
    StringBuffer s = new StringBuffer(150);
    //xx possibly crop name to 14 char if longer.
    s.append("STR " + (int) (max_battle_str / 1000000) + " " + type + " " + getName());

    if (see_enemy_count > 0)
      s.append("_" + see_enemy_count);
    else
      s.append("_-");
    s.append(" dangr " + (int) (getDangerMeter() * 10.0));
    if (cargo_capacity > 4.0)
      s.append(" C " + (int) Math.round(cargo_carried));

    s.append("_shie " + (int) (getShieldPerc() * 100.0)+"% ");
    s.append("hull " + (int) (getHullPerc() * 100.0)+"% ");

    //s.append(+ " cost " +Integer.toString((int)(buildcost/1.0)));
    if (max_crew_count > 0)
      s.append(",crew " + getCrewCount());
    if (hasMissileCapability()) {
      s.append(",mrang" + (int) (misl_range_measured / 100000));
      s.append(",max" + getHowManyMislWhenFull() + "misl");
    }
    s.append(","+ destination_desc);

    String ret2 = s.toString();
    ret2 = ret2.replace("STARBASE", "BASE");
    ret2 = ret2.replace("TINYMINER", "TINYM");
    return ret2;
    //" str/cost "+(int)(max_battle_str/buildcost)
  }
  public String toStringShort() {
    if (parentFaction == null) {
      return "Derelict "+ type+" ship "+getId();
    } else {
      return "F"+ parentFaction.getFactionId()+" "+type+" "+getId(); //+"(cost "+Integer.toString((int)(buildcost/1.0))+")";
    }
  }
  public String toStrTypeNName() { //for HUD.
    if (parentFaction == null) {
      return "Derelict "+ type+" ship " + getId();
    } else {
      return ""+type+" "+getName();
    }
  }
  public int compareTo(Ship n) {
    Integer cd = new Integer((int)Math.round(100.0*getMaxBattleStr()));
    Integer cdn = new Integer((int)Math.round(100.0*n.getMaxBattleStr()));
    return cd.compareTo(cdn);
  }
  public long getTargetId() {
    if (current_target != null)
      return current_target.getId();
    else
      return -1;
  }
  public Faction getParentFaction() {
    return parentFaction;
  }
  public double getHullPerc() { //for logic
    return curr_hull_hp / max_hull_hp;
  }
  public double getShieldPerc() { //for logic
    if (max_shields < 1.0)
      return 1.0; //if don't own shields, we at 100%.
    return curr_shields / max_shields;
  }
  public double getBuildCredPerc() { //for logic
    if (max_buildcredits < 0.1)
      return 0.0;
    return curr_buildcredits / max_buildcredits;
  }
  private String getName() {
    return name;
  }
  public double gimmeBearingVariance(double br, double amt) {
    return br + amt * (Missilemada2.gimmeRandDouble() - 0.5);
  }
  public boolean isAtFactionBase() {
    if (parentFaction == null)
      return false;
    if (calcDistanceVecVec(this.getXYZ(), parentFaction.getXYZ()) < 0.98 * Missilemada2.getArrivedDistance()) {
      return true;
    } else {
      return false;
    }
  }
  public Ship getTarget() { //for: ask buddy's target.
    return current_target;
  }
  public void addDealt_hulldam(double a) { //Qapla'!
    caused_destruction = caused_destruction + a;
    addMerit(0.04 * (a / Missilemada2.getAvgMislYield()));
  }
  public void addDealt_shielddam(double a) { //less merit for this.
    //caused_destruction = caused_destruction + a;
    addMerit(0.015 * (a / Missilemada2.getAvgMislYield()));
  }
  public long get_caused_destruction() {
    return (long) caused_destruction;
  }
  public void addMerit(double a) {
    //if (isInPlayerFaction())
    //  Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Our " + toStringShort() + " earned "+a+" merit badges.");

    merit_badges_lifetime = merit_badges_lifetime + a;
    merit_badges_curr = merit_badges_curr + a;
    if (isSeenByPlayer() && merit_badges_lifetime > 2.0) { //xxx want "if this adding put over _A_ 2.0 threshold
      Missilemada2.addVfxOnMT(0, 4000, 800, "MERIT_UP", 4400, 980.0, 0.9/*transp*/, this, "qm_green.png", 1.0, "");
    }
  }
  public double getMeritCurr() {
    return merit_badges_curr;
  }
  public double getMeritLifetime() {
    return merit_badges_lifetime;
  }
  public double getDangerMeter() {
    return danger_meter;
  }
  public double getScore() {
    score = (  (0.00001* timeAlive + (caused_destruction/(spent_on_misl+0.1))/1000 + damage_sustained/2000000 + mislcred_shot_down_xxxxtodo / 30000)   / (buildcost/100000000)  );
    //if (score > 19 && Missilemada2.gimmeRandDouble() < 0.05)
    //  System.out.println("pretty good ship, "+getShipDNA() + " score now "+score);
    return score;
  }
  public double getHP() {
    return curr_hull_hp;
  }
  public void setFaction (Faction f) {
    parentFaction = f;
  }
  public Faction getFaction() {
    return parentFaction;
  }
  public void gotTargeted() {
    num_missiles_targeting_me++;
  }
  public void undoGotTargeted() {
    num_missiles_targeting_me--;
  }
  private void gotShieldsToFull() {
    setIsNearBattle(false);
    if (isInPlayerFaction() && type.equals("AC")) {
      Missilemada2.addToHUDMsgList("Our Cruiser " + this.toStringShort() + " got shields back to FULL.");
    }

/*
    int a = getMIDIInstrument();
    setMIDIInstrument(71*/
/*clarinet*//*
);
    playNote(10, 90, 8);
    setMIDIInstrument(a);
*/
  }

  public String getDNA() {
    return shipDNA;
  }
  public String getType() {
    return type;
  }
  public void addSpending(double a) {
    spent_on_misl = spent_on_misl + a;
  }
  public void setDead_oncreation() {
    setIsSeenByPlayer(false);
    curr_hull_hp = 0.000;
    curr_shields = 0.0;
    curr_crew_count = 0;

    engine_status = 0.0;
    shield_status = 0.0;
    beamsystem_status = 0.0;
    lifesupport_status = 0.0;
    miningsystem_status = 0.0;
    missilesystem_status = 0.0;
  }
  public boolean isDestroyed() {
    if (curr_hull_hp < 1.0)
      return true;
    else
      return false;
  }
  public void getDamaged(double in_joules, String damagetype, Ship scoring_ship, boolean bypass_shields) { // a single missile hit this.
    if (isDestroyed()) //if someone is hurting a destroyed ship, don't.
      return;

//    if (scoring_ship != null)
//      System.out.println("Ship "+toString()+" gets "+(int)in_joules + " "+damagetype+ " damage from ---- "+scoring_ship.toString());
//    else
//      System.out.println("Ship "+toString()+" gets "+(int)in_joules + " "+damagetype+ " damage from unkn");

    shield_flash = true; //blinkenshields
    am_under_fire = true;
    damage_sustained = damage_sustained + in_joules;

    if (scoring_ship != null) {
      parentFaction.shiftFrontLine(getXYZ(), 0.8, this);
      setIsNearBattle(true);
    } //else shipwide fire damage source, don't shift flag.

    //if missile explosion, lose some speed ("stability compensators" or something)
    if (damagetype.equals("missile explosion")) {
      reduceSpeed(0.99);
    }

    //if shields collapse or aren't there, or special damagetype(engine fire), take full input to hull
    if (curr_shields < in_joules || bypass_shields) {
      curr_shields = 0.0;
      curr_hull_hp = curr_hull_hp - in_joules;
      double hulldamageperc_of_hit = in_joules / max_hull_hp;
      mass_kg = mass_kg * 0.998; //lost some plating.

      if (scoring_ship != null) {
        scoring_ship.addDealt_hulldam(in_joules);
        //if player scored on enemy base, HUDmsg.
        if (isStarbase()
                && parentFaction != Missilemada2.getPlayerFaction()
                && scoring_ship.isInPlayerFaction() ) {
          Missilemada2.addToHUDMsgList("Our " + scoring_ship.toStrTypeNName() + " damaged enemy base! ("+(in_joules/1000000.0)+" MJ)");
        }
      }

      //if fire damage, curb number of debris. else don't curb.
      //no debris trail if player doesn't see the ship!
      if (damagetype.equals("shipwide fire") || damagetype.equals("engine fire") ) {
        if (this.isSeenByPlayer() && Missilemada2.gimmeRandDouble() < 0.07)
          Missilemada2.createDebrisFlatSprite("hull_bits.png", 0.28*(0.10+Missilemada2.gimmeRandDouble()), 750.0*(1.0+Missilemada2.gimmeRandDouble()), 750.0*(1.0+Missilemada2.gimmeRandDouble()), this, false);
      } else { //regular damage.
        if (this.isSeenByPlayer())
          Missilemada2.createDebrisFlatSprite("hull_bits.png", 0.28*(0.10+Missilemada2.gimmeRandDouble()), 750.0*(1.0+Missilemada2.gimmeRandDouble()), 750.0*(1.0+Missilemada2.gimmeRandDouble()), this, false);

        //too spammy: Missilemada2.putMelodyIntoQue(Missilemada2.DNAintoMelody(getDNA(), 3, "") /*Vector of pitches*/, corenote+2 /*core note*/, parentFaction.getMIDIInstrument() /*instrument*/, 1.4F /*note duration*/);
        int wub = (int)(100*hulldamageperc_of_hit);
        if (isInPlayerFaction() && !isAtFactionBase()) {
          playGotHullDamage();
          if (getHullPerc() < 0.755 && getHullPerc() > 0.70 && wub > 0) {
            Missilemada2.addToHUDMsgList("Our " + this.toStrTypeNName() + " reports major hull damage! Last hit took "+wub+"% off hull!");
          }
          if (getHullPerc() < 0.255 && getHullPerc() > 0.20 && wub > 0) {
            Missilemada2.addToHUDMsgList("Our " + this.toStrTypeNName() + " reports critical hull damage! Last hit took "+wub+"% off hull!");
          }
        }
      }

      //int drawSize = (int) Math.round( 3.3 * 0.77*Math.sqrt(buildcost/40000000 + mass_kg/8000000) * (700/850));
      //int rando =(int) Math.round( (22 * Missilemada2.gimmeRandDouble()) - 11);

      //get damage to systems! Even if you manage to recover full shields, you're not back to good condition.
      //if combat damage, it may damage systems, if engine fire / swipwide fire damage, DO NOT DAMAGE SYSTEMS EVERY TIMETICK.
      if (!bypass_shields) {
        if (Missilemada2.gimmeRandDouble() < 0.04 && curr_crew_count > 0) {
          curr_crew_count = curr_crew_count - 1;
          parentFaction.lostCrewmen(1);
          //System.out.println("Ship "+unique_id+" lost a crewman to incoming damage.");
          if (isInPlayerFaction()) {
            Missilemada2.addVfxOnMT(getX(), getY(), getZ(), "CREWMANDIED", 29000, 3380.0, 1.0/*transp*/, null, "lost_one_crew.png", 1.0, "");
            Missilemada2.addToHUDMsgList("Our " + this.toStringShort() + " lost a crewman during battle.");
          }
        }

        if (Missilemada2.gimmeRandDouble() < 0.07) { //must not happen too often. because MANY missiles.
          engine_status = engine_status - 0.08;
          if (engine_status < 0.03) {
            getDestroyed(scoring_ship, damagetype);
          }
        }
            //bonus chance of engine_broken! only from beam & misl.
            if (! (damagetype.equals("shipwide fire") || damagetype.equals("engine fire") ) ) {
              if (Missilemada2.gimmeRandDouble() < 0.009) { //must not happen too often. because MANY missiles.
                engine_status = 0.06;
                //vfx of busted engine
                if (isInPlayerFaction() || Missilemada2.getPlayerFaction().isShipScouted(this)) {
                  Missilemada2.addVfxOnMT(0, 0, 200, "SOME_SYSTEMS_DAMAGE", 9400, 980.0, 0.9/*transp*/, this, "systems_damage_some.png", 1.0, "");
                }
              }
            }

        if (Missilemada2.gimmeRandDouble() < 0.06) {
          lifesupport_status = lifesupport_status - 0.07;
          if (lifesupport_status < 0.02)
            lifesupport_status = 0.0;
        }
        if (Missilemada2.gimmeRandDouble() < 0.08) {
          sensors_status = sensors_status - 0.3;
          if (sensors_status < 0.02)
            sensors_status = 0.0;
        }
        if (Missilemada2.gimmeRandDouble() < 0.05) {
          beamsystem_status = beamsystem_status - 0.2;
          if (beamsystem_status < 0.02)
            beamsystem_status = 0.0;
        }
        if (Missilemada2.gimmeRandDouble() < 0.05) {
          missilesystem_status = missilesystem_status - 0.2;
          if (missilesystem_status < 0.02)
            missilesystem_status = 0.0;
        }
        if (Missilemada2.gimmeRandDouble() < 0.05) {
          shield_status = shield_status - 0.15;
          if (shield_status < 0.02)
            shield_status = 0.0;
        }
        if (Missilemada2.gimmeRandDouble() < 0.05) {
          miningsystem_status = miningsystem_status - 0.4; //more fragile
          if (miningsystem_status < 0.02)
            miningsystem_status = 0.0;
        }
      }
    } else {
      //----shields take damage, systems take TINY amount damage (so cannot survive forever on "my shields are so OP")
      curr_shields = curr_shields - in_joules;
      reduceSpeed(0.997);

      if (this.isSeenByPlayer())
        Missilemada2.createDebrisFlatSprite("shield_spark.png", 0.12*(0.10+Missilemada2.gimmeRandDouble()),
                1150.0*(1.0+Missilemada2.gimmeRandDouble()), 1150.0*(1.0+Missilemada2.gimmeRandDouble()), this, false/*false=rand bearing*/);

      shield_status = shield_status - 0.002;
      if (shield_status < 0.08)
        shield_status = 0.0;

      beamsystem_status = beamsystem_status - 0.002;
      if (beamsystem_status < 0.02)
        beamsystem_status = 0.0;

      lifesupport_status = lifesupport_status - 0.002;
      if (lifesupport_status < 0.02)
        lifesupport_status = 0.0;

//      engine_status = engine_status - 0.001;
//      if (engine_status < 0.01) {
//        getDestroyed(scoring_ship, damagetype);
//      }

      sensors_status = sensors_status - 0.002;
      if (sensors_status < 0.05)
        sensors_status = 0.0;

      if (scoring_ship != null)
        scoring_ship.addDealt_shielddam(in_joules);
    }

    if (curr_hull_hp < 10000.0) {
      curr_hull_hp = 0.0;
      //NO SHIP EXPLOSIONS.
          // double explosion_range = 5.2*Missilemada2.getMissileCollisionRange();
          // remove all missiles within explosion radius.
          // damage all ships within explosion radius.
      this.getDestroyed(scoring_ship, damagetype);
    } else {
      //didn't die this tick.
          if (isInPlayerFaction()) {
            Missilemada2.changeWorldTimeIncrement(-2); //slow down world, because combat is happening.
            if (type.equals("AC") && see_enemy_mislcount > 3) { //notify only if cruiser in trouble.
              if (getHullPerc() < 0.20) {
                Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Our " + toStringShort() + " is massively damaged.");
              } else if (getHullPerc() < 0.65) {
                Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Our " + toStringShort() + " took heavy damage.");
              }
            }
          }
    }
  }
  public void clearTarget() {
    current_target = null; //because it got destroyed.
    have_target = false;
  }
  public double getSensorRange() {
    return sensor_range;
  }
  public int getBuildTimeDelay() {
    //return (int) (4.5 * buildcost);
    return (int) (0.5 * buildcost);
  }
  public void getDestroyed(Ship scoring_ship, String damagetype) {
    try {
      //reset things coz dead.
      current_destinationXYZ = null;
      am_under_fire = false;
      buddy_civilian = null;
      buddy_derelict = null;
      buddy_mil = null;
      current_target = null;
      /* max_speed = 0.0;*/
      curr_hull_hp = 0.0; curr_shields = 0.0; engine_status = 0.0;

      //keep track of KIA(killed in action) count.
      if (curr_crew_count > 0 && parentFaction != null)
        parentFaction.lostCrewmen(curr_crew_count);
      curr_crew_count = 0;

      //faction focuses efforts this location, coz lost a ship.
      if (parentFaction != null) {
        parentFaction.shiftFrontLine(this.getXYZ(), 5.0, this);
        //withdraw frontline a little, towards base, coz mucho battle.
        parentFaction.shiftFrontLine(parentFaction.getXYZ(), 7.5, this);
      }

      if (scoring_ship != null) {
        //scoring ship gets merit badges.
        if (this.type.equals("SCOUT"))
          scoring_ship.addMerit(0.45);
        if (this.type.equals("TINYMINER"))
          scoring_ship.addMerit(0.18);
        if (this.type.equals("MINER"))
          scoring_ship.addMerit(0.7);
        if (this.type.equals("BEAMDRONE"))
          scoring_ship.addMerit(0.3);
        if (this.type.equals("MISSILEDRONE"))
          scoring_ship.addMerit(0.3);
        if (this.type.equals("SENSAT"))
          scoring_ship.addMerit(0.1);
        if (this.type.equals("DEFENDER"))
          scoring_ship.addMerit(1.0);
        if (this.type.equals("AC"))
          scoring_ship.addMerit(2.4);

        Faction scoring_fac = scoring_ship.getFaction();
        if (scoring_fac != null)
          scoring_fac.addScoreShipKill(this.buildcost, this);
        if (scoring_ship.getTargetId() == getId())
          scoring_ship.clearTarget(); //BUT scoring_ship might have gotten a new target many seconds ago.

        if (scoring_fac == Missilemada2.getPlayerFaction()) {
          if (type.equals("AC")) {
            Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "We destroyed an enemy cruiser! Well done, "+scoring_ship.toStrTypeNName()+"!", 2);
            Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("yay AC down", 5, "") /*Vector of pitches*/, 62 /*core note*/, 17 /*perc organ*/, 100, 2.9F /*note duration*/);
          }
          if (type.equals("MINER")) {
            Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "We destroyed an enemy mining ship! Well done, "+scoring_ship.toStrTypeNName()+"!", 2);
            Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("yay MINER down", 5, "") /*Vector of pitches*/, 62 /*core note*/, 17 /*perc organ*/, 100, 2.9F /*note duration*/);
          }





        }
    } else {
      System.out.println("--part2 SHIP DESTROYED by unkn ship or a fire, "+damagetype + ".");
    }

      //explosion visible TO PLAYER at ANY RANGE. So big.
      Missilemada2.addVfx2(getXYZ(), "SHIP_EXPLODED", (int) (5.7 * 60 * 60), 31.0 * radius, 0.85/*transp*/, "boom4.png", 1.0, "");
      Missilemada2.setDynamicLight("SHIP_EXP", 0.5f, Missilemada2.getWorldTime()+5000, this.getXYZ());
      Missilemada2.setCurrentExplosion(this.getXYZ(), 3.5 * Missilemada2.getMissileCollisionRangeMin()); //missiles passing through explosion, may get damaged or destroyed!

      //if player saw the explosion, place N debris bits, depending on buildcost.
      int bits_count = (int)(2.5*buildcost / 1000.0);
      if (this.isSeenByPlayer()) {
        for (int a = 0; a < bits_count; a++) {
          Missilemada2.createDebrisFlatSprite("hull_bits.png", 4.1*(0.10+Missilemada2.gimmeRandDouble()) /*spd*/,
                  950.0*(1.0+Missilemada2.gimmeRandDouble()), 950.0*(1.0+Missilemada2.gimmeRandDouble()), this, false/*false==randomdir*/);
        }
      }

      //sound effect if dead is plr, or dealing one is plr.
      if (type.equals("SENSAT") || type.equals("TINYMINER") ) { //if dead one SENSAT or TINYMINER, no sound.
        //no sound
      } else {
        if (isInPlayerFaction()) { //plr lost a ship
          Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("4kawwboom", 2, "") /*Vector of pitches*/, 45 /*core note*/, 55 /*orch-hit*/, 120, 12.9F /*note duration*/);
        } else if (scoring_ship != null) {
          if (scoring_ship.isInPlayerFaction()) //plr scored a ship kill
            Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("4kawwboom", 2, "") /*Vector of pitches*/, 56 /*core note*/, 55 /*orch-hit*/, 110, 10.0F /*note duration*/);
        }
      }

      if (parentFaction != null)
        parentFaction.shipCountDown(this, damagetype); //deduct one ship from faction's counter.
      if (parentFaction == Missilemada2.getPlayerFaction()) {
        //vfx of lost ship name to battlefield.
        Missilemada2.addVfxOnMT(xcoord, ycoord, zcoord, "NAMEOFLOSTSHIP", 60000/*ms*/, 3380.0, 1.0/*transp*/, null, "", 1.0, "lost "+getName());
      }
      System.out.println("------SHIP DESTROYED by "+damagetype + ". It was " +toString());


      if (type.equals("STARBASE")) { //if base, remove from shiplist and baselist.
        if (isInPlayerFaction()) {
          //plr lost.
          Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("4kawwboom", 14, "") /*Vector of pitches*/, 52 /*core note*/, 55 /*orch-hit*/, 120, 10.9F /*note duration*/);
        } else {
          //plr got a major victory.
          Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("krakaboom", 9, "") /*Vector of pitches*/, 52 /*core note*/, 55 /*orch-hit*/, 110, 9.0F /*note duration*/);
        }
        Missilemada2.removeStarBaseFromPlay((StarBase) this);
      }

      Missilemada2.removeShipFromActives(this);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
  private Vector predictTargetsPosition(MobileThing t, int howManyTicks) {
    Vector ret = new Vector(3,3);



    double tb = t.getBearingXYfromSpeed(); //xx t.getBearing();




    double tgt_movement_in_timetick = (0.000001 + ( (double) Missilemada2.getWorldTimeIncrement() / 1000 ) * t.getSpeedCurrent());
    //bearingXY rollover check?

    Double pred_x = new Double( t.getX() + (howManyTicks * tgt_movement_in_timetick * Math.cos(tb)) );
    Double pred_y = new Double( t.getY() + (howManyTicks * tgt_movement_in_timetick * Math.sin(tb)) );
    Double pred_z = new Double( t.getZ()); //xxx
    ret.add(0, pred_x);
    ret.add(1, pred_y);
    ret.add(2, pred_z);
    return ret;
  }

  private boolean executeMining(double seconds) {
    //if at base AND at minable aste, don't mine. Too hazardous and buggy.
    if (this.isAtFactionBase())
      return false;

    if (isScout()) {
      destinationAsteroid = closestAsteroid;
    }

    //if near DESIRED asteroid, do mining.
    if (destinationAsteroid != null && isNearAsteroid(destinationAsteroid)) {
      //System.out.println(Missilemada2.getWorldTime() +  ": "+type+" ship "+getId()+" tries to mine "+closestAsteroid.toString());
      if (calcDistanceMTMT(destinationAsteroid, this) < Missilemada2.getMiningDistance()) {
        double rand_for_visu = Missilemada2.getCurrVisualScaling() * 30*Missilemada2.gimmeRandDouble();
        double startcargo = cargo_carried;
        int dig_unit_count = 0;
        if (type.equals("MINER")) //make miner faster_mining than drone or scout.
          dig_unit_count = 4;
        else
          dig_unit_count = 1;
        for (int i = 0; i < dig_unit_count; i++) {
          if (destinationAsteroid.hasResource("METAL3"))
            this.addCargo("METAL3", destinationAsteroid.mining("METAL3", seconds, this));
          if (destinationAsteroid.hasResource("FUEL"))
            this.addCargo("FUEL", destinationAsteroid.mining("FUEL", seconds, this));
          if (destinationAsteroid.hasResource("METAL1"))
            this.addCargo("METAL1", destinationAsteroid.mining("METAL1", seconds, this));
          if (destinationAsteroid.hasResource("METAL2"))
            this.addCargo("METAL2", destinationAsteroid.mining("METAL2", seconds, this));
        }
        if (cargo_full
                && isInPlayerFaction()
                && type.equals("MINER")) {
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + toStrTypeNName() + " completed mining, carry "+(int)cargo_carried+" tons.", 2);
        }
        if (isScout() && isInPlayerFaction()   &&   cargo_carried > startcargo && startcargo < 0.1)
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + toStrTypeNName() + " did some much-needed mining.", 2);
        if (isInPlayerFaction()) {
          if (Missilemada2.gimmeRandDouble() < 0.0008 * seconds) {
            Missilemada2.createDebrisFlatSprite("mining_debris.png", 1.1*(0.10+Missilemada2.gimmeRandDouble()) /*spd*/,
                    750.0*(1.0+Missilemada2.gimmeRandDouble()), 750.0*(1.0+Missilemada2.gimmeRandDouble()), this, false);
          }
        }

        // X % chance of mining accident, equipment breaks.
        if (Missilemada2.gimmeRandDouble() < 0.003) {
          reduceSpeed(0.95);
          miningsystem_status = miningsystem_status - 0.15; //busted servos
          engine_status = engine_status - 0.05; //structural damage
          sensors_status = sensors_status - 0.05; //rocks in antenna dish
          playMiningAccident();
          timestamp_next_allowed_accel = Missilemada2.getWorldTime() + 4 * (int)(cargo_carried); //seconds
          //System.out.println(Missilemada2.getWorldTime() +  ": "+type+" ship "+getId()+" broke mining equipment by accident.");
          if (isInPlayerFaction()) {
            Missilemada2.addVfxOnMT(rand_for_visu, 4000, 50.0, "MINING_ACCIDENT", 13000, 1510.0, 0.8/*transp*/, this, "mining_accident.png", 1.0, "");
          }
        }

        if (cargo_carried > startcargo && startcargo < 0.1) { //if did first mining of this haul, play sound.
          timestamp_next_allowed_accel = Missilemada2.getWorldTime() + (int)(6.0*cargo_capacity); //seconds
          //mining happens -vfx:
          if (isInPlayerFaction()) {
            Missilemada2.addVfxOnMT(0.0, 3000.0, 0.0, "MINING", 20000, 2200.0, 0.4/*transp*/, this/*coords relative to*/, "mining_started.png", 1.0, "");
          }
          if (type.equals("SCOUT") || type.equals("TINYMINER")) {
            playMiningStartSmall();
          }
          if (type.equals("MINER"))
            playMiningStartBig();
        }
        //if cargo full, gonna leave this aste, so rescan it for faction-scoutreports.
        if (cargo_full && closestAsteroid != null) {
          parentFaction.addScoutReportAste(this, new ScoutReport(Missilemada2.getWorldTime(), closestAsteroid), closestAsteroid);
        }
        return true;
      } else {
        //have closest and it's too far.
        //System.out.println("but it's too far.");
      }
    } else {
      //no asteroids in sensor range.
    }
    return false;
  }
  private boolean isNearAsteroid(Asteroid des) { //was bad when closest did not match desti.
    if (des != null) {
      if (calcDistanceVecVec(des.getXYZ(), this.getXYZ()) < 1.4*Missilemada2.getMiningDistance()) {
        return true;
      }
    }
    return false;
  }
  private void addCargo(String reso, double amt_tons) {
    //addmerit no longer here, but on offload.

    //if (cargo_carried < 1.0)
    //  System.out.println(Missilemada2.getWorldTime() +  ": "+type+" ship "+getId()+" got haul's first "+reso+" cargo: "+amt_tons);
    //System.out.println(Missilemada2.getWorldTime() +  ": "+type+" ship "+getId()+" got some "+reso+" cargo: "+amt_tons);

    //HORRIBLY WRONG: cargoMapStrToDouble.put(reso, amt_tons/*resets stored*/);
    double prev = 0.0;
    if (cargoMapStrToDouble.get(reso) != null)
      prev = cargoMapStrToDouble.get(reso);
    cargoMapStrToDouble.put(reso, prev+amt_tons);

    cargo_carried = cargo_carried + amt_tons;
    if (cargo_carried > cargo_capacity) {
      cargo_full = true;
      forceDestination(parentFaction.getXYZ_starbase_safer_side(), "cargo full, must to base.");
      if (isInPlayerFaction())
        addVfxOfCargoCarried();
      //keep desti_aste

      //delay for pack mining gear...
      timestamp_next_allowed_accel = Missilemada2.getWorldTime() + (long)(60.0 * (int)cargo_capacity); //seconds
    } else {
      //xxxxx maybe not this line? timestamp_next_allowed_accel = Missilemada2.getWorldTime() + (long)(1.0 * amt_tons); //seconds
    }
  }
  private void tryGiveCargoToBase() { //instantaneous, then delay to the ship.
    if (calcDistanceVecVec(parentFaction.getXYZ_starbase_safer_side(), getXYZ()) < Missilemada2.getDockingDistance() && cargo_carried > 0.01) {
      if (isInPlayerFaction()) {
        Missilemada2.addVfxOnMT(xcoord, ycoord, zcoord + 1250.0, "CARGOTRANSFER", 10000, 900.0, 0.9/*transp*/, null, "cargo_transfer.png", 0.8, "");
        //System.out.println("player's " + type +" ship "+unique_id+" gave "+cargo_carried+" tons cargo to base "+base.getName());
      }
      parentFaction.receiveCargo("FUEL", this.cargoMapStrToDouble.get("FUEL"));
      parentFaction.receiveCargo("METAL1", this.cargoMapStrToDouble.get("METAL1"));
      parentFaction.receiveCargo("METAL2", this.cargoMapStrToDouble.get("METAL2"));
      parentFaction.receiveCargo("METAL3", this.cargoMapStrToDouble.get("METAL3"));

      //parentFaction.tryShipProduction(); //rand chance, plus when receive cargo.

      //delay on offload; small for scouts, large for miners.
      timestamp_next_allowed_accel = Missilemada2.getWorldTime() + (long)(20.5*cargo_carried); //seconds
      reduceSpeed(0.05); //magic slowdown with base's emitters...


      //Missilemada2.drawTextXYZ(Missilemada2.getFont60fornowwww(1), 200f, (float)xcoord, (float)ycoord, (float)zcoord, "OFFLOADED "+cargo_carried, Color.green);

      if (type.equals("SCOUT") || type.equals("TINYMINER"))
        playCargoOffloadSmall();
      if (type.equals("MINER"))
        playCargoOffloadBig();

      //get merit
      addMerit(0.0018*cargo_carried);
      //empty the cargo bays
      cargo_carried = 0.0;
      cargo_full = false;
      cargoMapStrToDouble = new HashMap<String, Double>();
      clearDestination(); //just in case

      parentFaction.resource_fuel = parentFaction.resource_fuel - 0.03*(mass_kg/1000.0); //resupply ship with fuel. //xxxbuggy when base atop minable asteroid.

      cargo_delivered_lifetime = cargo_delivered_lifetime + cargo_carried;
      //xxxxx merit badge every 700 tons? (ten hauls on TINYMINER)

    } //else too far or no cargo.
  }
  private boolean isAtDestination() {
    if (current_destinationXYZ == null)
      return true;
    return (calcDistanceVecVec(getXYZ(), current_destinationXYZ) < Missilemada2.getArrivedDistance());
  }
  private boolean isNearDestination() {
    if (current_destinationXYZ == null)
      return true;
    return (calcDistanceVecVec(getXYZ(), current_destinationXYZ) < Missilemada2.getNearlyArrivedDistance());
  }
  private void executeMove(double seconds, boolean driftonly_coz_mining) { //includes arrival detection.
    if (isDestroyed())
      return;

    if (type.equals("SENSAT")) {
      //PLAYERFAC: rand chance of scanning-vfx, to SHOW that sensat is there.
      if (isInPlayerFaction() && Missilemada2.gimmeRandDouble() < 0.00009*seconds) {
        Missilemada2.addVfxOnMT(getX(), getY(), getZ(), "PINGGG", 28000, 4850.0, 0.55/*transp*/, null, "scan_cyan2.png", 1.0, "");
      }
      return; //no move for any faction SENSAT!
    }

    //calc accel; mass changes with cargo and hull_plating_gone.
    double curr_max_accel = 0.0; //engine_status * max_accel; //damaged engine hinders ship.
    double curr_max_speed = engine_status * max_speed;
    //double curr_ke = getKineticEnergy();
    double currmass = mass_kg + cargo_carried; //yay, cargo sluggishes you
    double max_accel22 = max_speed / Missilemada2.SECONDS_TO_MAX_SPEED;
    //branch on type, for different thruster powers!  scouts are the agilest? miner and ac clumsiest.
    double secmax = Missilemada2.SECONDS_TO_MAX_SPEED;
    if (type.equals("MINER"))
      secmax = 2.3 * secmax;
    if (type.equals("TINYMINER"))
      secmax = 1.4 * secmax;
    if (type.equals("SCOUT"))
      secmax = 0.79 * secmax; //nimblest
    if (type.equals("MISSILEDRONE"))
      secmax = 1.2 * secmax;
    if (type.equals("BEAMDRONE"))
      secmax = 0.85 * secmax; //nimble, based on scout chassis.
    if (type.equals("DEFENDER"))
      secmax = 1.0 * secmax; //baseline engine/thrusters
    if (type.equals("AC"))
      secmax = 1.6 * secmax;
    if (type.equals("STARBASE"))
      secmax = 1.1 * secmax; //xxtuning
    if (type.equals("SENSAT"))
      secmax = 9.9 * secmax; //but they don't move.

    double thrusterforce = max_mass_kg * engine_status * (max_speed / secmax); // F = ma
    curr_max_accel = thrusterforce / currmass; // F = ma

    //if near destination (about to arrive), want less accel, same decel.
    double maxdecelperc = (curr_max_accel/max_speed) * (seconds/60.0);// 0.01? must relative to wti.
    //end calc

    //damaged engine damages ship. (power grid)
    if (engine_status < 0.25 && !isDestroyed()) {
      getDamaged(0.06 * (seconds/180.0) * Missilemada2.getAvgMislYield(), "engine fire", null, true/*bypass shields*/);
      if (Missilemada2.gimmeRandDouble() < 0.07)
        engine_status = engine_status + 0.01; //auto-stabilizing tech, so don't burn forever if incompetent crew.
    }
    if (getBattleStrPerc() < 0.30 && !isDestroyed()) //if very systems-damaged, are on fire, take more damage.
      getDamaged(0.08 * (seconds/180.0) * Missilemada2.getAvgMislYield(), "shipwide fire", null, true/*bypass shields*/);

    if (isDestroyed()) { //must be AFTER fire-damage code.
      advance_time_derelict_drift(seconds);
      return;
    }

    double des_x = 0.0;
    double des_y = 0.0;
    double des_z = 0.0;
    double bear_to_desti_xy = 0.0;
    double bear_to_desti_xz = 0.0;

//xxBADDDD at base: if too close to buddy, DON'T ACCEL MUCH.
//    if (buddy_mil != null) {
//      if (calcDistanceMTMT(this, buddy_mil) < getShipTooCloseDistance(this, buddy_mil)) {
//        curr_max_accel = 0.3*curr_max_accel;
//        if (isInPlayerFaction())
//          Missilemada2.addVfx2(getXYZ(), "TEXT", 15000, 190.0, 0.6/*transp*/, "", 1.0, "milbud too close");
//      }
//    }
//    if (buddy_civilian != null) {
//      if (calcDistanceMTMT(this, buddy_civilian) < getShipTooCloseDistance(this, buddy_civilian)) {
//        curr_max_accel = 0.3*curr_max_accel;
//        if (isInPlayerFaction())
//          Missilemada2.addVfx2(getXYZ(), "TEXT", 15000, 190.0, 0.6/*transp*/, "", 1.0, "civbud too close");
//      }
//    }

    if (current_destinationXYZ != null) {
      //okay, proceed.
    } else { // do move, but null desti, mehhhhhh
      //YYYY ohh, it was bearing 0.0 THAT CAUSED "FAR RIGHT, FAR RIGHT"...
      setDes(parentFaction.getXYZ_starbase_safer_side(), "to base because have no desti in exemove");
      have_destination = true;
      if (isInPlayerFaction())
        Missilemada2.addVfx2(getXYZ(), "TEXT", 5000, 190.0, 0.2/*transp*/, "", 1.0, "nodesti_inexemove_gotobase");
    }
    des_x = (Double)current_destinationXYZ.get(0);
    des_y = (Double)current_destinationXYZ.get(1);
    des_z = (Double)current_destinationXYZ.get(2);
    bear_to_desti_xy = Missilemada2.calcBearingXY(xcoord, ycoord, des_x, des_y); //cpu-intensive
    bear_to_desti_xz = Missilemada2.calcBearingXY(xcoord, zcoord, des_x, des_z); //cpu-intensive

    //if still have a home base... and are near it, try to offload cargo / replenish misl buildcredits.
      if (parentFaction.getStarbase() != null) {
        if (calcDistanceVecVec(this.getXYZ(), parentFaction.getStarbase().getXYZ()) < Missilemada2.getDockingDistance()) {
          if (cargo_carried > 0.2) {
            tryGiveCargoToBase();
            have_destination = false; //xxxx
            forceddestination = false;
          }
          if (type.equals("MISSILEDRONE") || isScout()) { //if these, pick up more missiles from base.
            if (curr_buildcredits < 0.9*max_buildcredits) {
              curr_buildcredits = max_buildcredits;
              parentFaction.removeNresourcesForMislRefill(max_buildcredits);
            }

          }
        }
      }

//    facing = calcXYBearingFromDesti();
//    bearing_from_speed = calcBearingFromSpeed();
//    double move_distance = getMoveDistFromSpeed();

    //accel in the direction of wanted destination.

    //sometimes decelerate!!! if gonna overshoot desti.
    boolean slowdown = false;
    if ( current_destinationXYZ != null
            && isNearDestination()
            && !isInCombat() ) {
      //tiny rand chance of declare_arrived coz might just loop around desti spot loooong time...
      //if (Missilemada2.gimmeRandDouble() < 0.0001) {
      //  clearDestination();
      //}

      if (getSpeedCurrent() > 0.1 * max_speed) { //dont use slowdown if we are already slow -- prevents start_moving!
        //curr_max_accel = 0.5*curr_max_accel; //less accel coz near arrival
        //reduceSpeed(1-maxdecelperc); //xx important?
        slowdown = true;
      }
    }
    //----if ship arrived----, DO STUFF AND ask for next destination.
    if ( current_destinationXYZ != null
         /*&& have_destination*/
         && isAtDestination()  ) {

      //curr_max_accel = 0.2*curr_max_accel; //less accel coz near arrival

      if (forceddestination) {
        forceddestination = false; //order (or panic) completed, out of forceddestination mode.
        have_destination = false;
        if (isMiner()) {
          //if miner forced-arrived at base(or other FORCED desti), then stay there for a while, don't RUN STRAIGHT OUT TO DANGERS
          setTimeDelay(4600);
        } else {
          //mil ship forced-arrived.
          setTimeDelay(900);
        }
      } else {
        //normal situation, arrived (without forced).

        //if carry sensats and this area has few friendlies(probably coz we scouted), deploy. No sens in deploying fragile SENSAT when enemy missiles near.
        if (carried_sensats > 0 && see_friend_count < 3 && see_enemy_mislcount == 0 && see_friend_mislcount == 0) {
          tryDeploySenSat();
        }
        have_destination = false;
        if (type.equals("SCOUT")) //xxxxxxx
          parentFaction.addScoutingDist(0.009 * this.sensor_range);
      }

      if (!isOnDelayTimer()) { //if arrived and are not on timer, vfx of arrival.
        Missilemada2.addVfxOnMT(0, 0, 0, "ARRIVED", 20000, 850.0, 0.4/*transp*/, this, "32texture_arrived.png", 1.0, "");
      }

      //if miner at destination (asteroid or base or scouting)
      if (isMiner()) {
        //reduceSpeed(1-maxdecelperc);
        slowdown = true;
        //??keep desti and do mining??
        if (cargo_full) {
          forceDestination(parentFaction.getXYZ_starbase_safer_side(), "miner cargo full force to base.");
        } else {
          if (destinationAsteroid != null) {
            //xxxxx forceDestination(destinationAsteroid.getXYZ(), "xxtry: force stay at DESTI-aste until full.");
          } else {
            if (closestAsteroid != null) {
              //umm what

              //DONT. forceDestination(closestAsteroid.getXYZ(), "xxtry: force stay at CLOSEST-aste until full.");
              //dont. clearDestination();

            } else {
              //duhh I se no asteroids
              //dont. clearDestination();
            }
          }
        }
      } else if (!isStarbase() && !isSenSat() && !isMiner()) { //scout/mil ship arrived at desti
        clearDestination();
        decideBehaviourAndDesti();

        if (isInCombat()) {
          slowdown = false; //zoom past at high speed
        } else {
          slowdown = true;
        }

        //we scout/mil arrived, somewhere. if not at base, and hurt, and no foes, go to base.
        if (!isAtFactionBase() && getBattleStrPerc() < 0.8 && see_enemy_count == 0 && see_enemy_mislcount == 0) {
          clearDestination();
          decideBehaviourAndDesti();
        }
        //we arrived. we are semi-okay, no desti, ask for new desti.
//      if (!have_destination) {
//        //current_destionXYZ = parentFaction.shipRequestsDestination(this, "arrived. we are okay, or not at base, no desti, ask for new desti.");
//        //have_destination = true;
//        current_destinationXYZ = null; //decide in other funcs than MOVE.
//        have_destination = false;
//
//      }
//
//      if (current_destinationXYZ != null) {
//        have_destination = true;
//        //System.out.println("worldtime "+Missilemada2.getWorldTime() + " Ship " + unique_id + " got a destination "+current_destinationXYZ.toString());
//      }
      }
      //we arrived, at base. if we  at base and hurt, stay there.
      if (isAtFactionBase()
              && !isStarbase()
              && (engine_status < 0.9 || shield_status < 0.9 || sensors_status < 0.9 || miningsystem_status < 0.9) ) {
        setDes(parentFaction.getXYZ_starbase_safer_side(), "systems damage, stay at base");
        //timestamp_next_allowed_accel = Missilemada2.getWorldTime() + 1600; //seconds
        have_destination = true;
        slowdown = true;
      }
    } //----end ship_arrived----

    //if in battle, prefer high speed over decel-for-arrival.
    if (isInCombat())
      slowdown = false;

    if (driftonly_coz_mining) {
      //no accel, therefore no turning. Mining or hullrepairs turn.
      //reduceSpeed(1 - (0.8 * maxdecelperc));
      reduceSpeed(0.96);
      //curr_max_accel = 0.0;
    } else { //normal move with accel.
      if (isStarbase())
        slowdown = false;
      if (slowdown) {
        double blorg = 1.0-maxdecelperc;
        if (blorg < 1.0 && blorg > 0.3)
          reduceSpeed(blorg);
        curr_max_accel = 0.90*curr_max_accel; //less accel coz near arrival
        //must accel, for direction change!
      }
/*
      if (dodge_mode) { //xxxunverified   try to alter course for better close_missile evading.
        bear_to_desti_xy = bear_to_desti_xy + 1.5;
        bear_to_desti_xz = bear_to_desti_xz + 1.5;
      }
*/
      //if (type.equals("MINER") && Missilemada2.gimmeRandDouble() < 0.003) {
      //  debugVFXText(getXYZ(), "beXY="+(int)(100*bear_to_desti_xy));
      //}

      //the acceleration:
      xspeed = xspeed + (curr_max_accel * seconds) * Math.cos(bear_to_desti_xy);
      yspeed = yspeed + (curr_max_accel * seconds) * Math.sin(bear_to_desti_xy);
      zspeed = zspeed + (curr_max_accel * seconds) * Math.sin(bear_to_desti_xz); //z is like y but in other dim, so SIN().
      //maxspeed is a ceiling.
      //cap the speed so max_speed has meaning. (ships must deflect debris, otherwise die of high travel speed.)
      reduceSpeedToThisCap(curr_max_speed); //new 2013-12-22. Prev was one-time reduction, not capping.
      curr_kinectic_energy = getKineticEnergy();
//      if (isStarbase() && isInPlayerFaction() && Missilemada2.gimmeRandDouble() < 0.01) {
//        System.out.println("DEBUG: plr base: KE="+curr_kinectic_energy+", spd="+getSpeedCurrent()+" km/s, bearXY="+getBearingXYfromSpeed());
//      }
    }
    //the actual move:
    xcoord = xcoord + (xspeed * (seconds));
    ycoord = ycoord + (yspeed * (seconds));
    zcoord = zcoord + (zspeed * (seconds));
    prev_xcoord = xcoord;
    prev_ycoord = ycoord;
    prev_zcoord = zcoord;

    //optional: draw tracer-ticks, exhaust puffs.
    //player only? nope, all factions' ships BUT vfx needs to be in plr range
    if (tracerticks == 90 && this.isSeenByPlayer()) {
      Missilemada2.addVfxOnMT(xcoord, ycoord, zcoord + 250.0, "TRACER", 3000, 900.0, 0.3/*transp*/, null, "32texturecyanx.png", 1.0, "");
      tracerticks = 0;
    } else {
      tracerticks++;
    }
  }
  private void executeTractor(double seconds) { //do after executemove.
    //tractor mode:
    if (tractormode && buddy_derelict != null) {
      //if dere withing my tractor range, X.
      //derelict speed change: towards base.
      // TOWARDS is HARD to calc!
      if (calcDistanceMTMT(this, buddy_derelict) < this.getTractorRange() && curr_crew_count > 0) {
        this.reduceSpeed(0.985); //xxhack
        buddy_derelict.setFaction(parentFaction); //xx optional?
        buddy_derelict.changeDerelictsSpeedTowardsVec(0.5 * engine_status * max_accel/*spd change*/, parentFaction.getXYZ(), seconds); //give it a nudge
        //actual move, xyz change, is in advance_time_derelict_drift().

        if (isInPlayerFaction() && buddy_derelict != null) {
          //Missilemada2.changeWorldTimeIncrement(-1); //slow down world, because xxtractor debug
          Missilemada2.addVfx2(buddy_derelict.getXYZ(), "TRACTORHAPPENING", 270/*sec*/, 1350.0/*siz*/, 0.6/*transp*/, "purple_swirl.png", 1.0, "");
          Missilemada2.addVfx2(this.getXYZ(), "TRACTORHAPPENING", 270/*sec*/, 1000.0/*siz*/, 0.4/*transp*/, "purple_swirl.png", 1.0, "");
        }
      }

      //if a derelict near my base, AND is being tractored, base interact with it:
      if (calcDistanceVecVec(buddy_derelict.getXYZ(), parentFaction.getXYZ()) < 0.9 * Missilemada2.getArrivedDistance()) {
        parentFaction.convertDerelictIntoResources(buddy_derelict);
        buddy_derelict = null;
        tractormode = false;
        clearDestination();
      }

    }
  }
  public static double getShipTooCloseDistance(Ship a, Ship buddy) {
    double spd = (a.getSpeedCurrent() * 60);
    double spd_bud = (buddy.getSpeedCurrent() * 60);
    double ret = 2.0*a.getRadius() + 2.0*buddy.getRadius() + 0.4*spd + 0.4*spd_bud; //may still be crap visually.
    //System.out.println("tooclosedist = "+ret);
    return ret;
  }
  public void advance_time (double seconds) {
    if (Missilemada2.getWorldTimeIncrement() < 1)
      return; //zero means pause

    if (Missilemada2.gimmeRandDouble() < 0.03)
      setIsSeenByPlayer(false);

    if (isDestroyed()) //if derelict, use separate advance_time func.
      return;

    if (curr_crew_count < 0) {
      System.out.println("Error: negative crew ("+curr_crew_count+"). "+toString());
    }

    timeAlive = timeAlive + seconds;
    beam_atk_draw_counter_frames--;
    beam_def_draw_counter_frames--;
    if (beam_def_draw_counter_frames < 1)
      stored_tryShootDownMissile = null; // reset linedraw destination.


    if (isOnDelayTimer()) {
      //System.out.println("worldtime "+Missilemada2.getWorldTime() + " Ship " + unique_id + " waits because took action earlier." );
      reduceSpeed(0.99);
      executeMove(seconds, true/*driftonly*/);
      return; //keep this return. no shield/buildcredgain while on delaytimer!
    }


    double movement_in_timetick = (0.000001 + ( (double) Missilemada2.getWorldTimeIncrement() / 1000 ) * max_speed); //default 10ms = 100fps
    //System.out.println(movement_in_timetick + "tick " + Missilemada2.getWorldTimeIncrement() +"wti, " +max_speed);

    //OLD: use bonuses carried, thanks to all the nanotech.
    //OLD: pick up bonuses

//    Vector myvisibleships = Missilemada2.getShipsWithin(xcoord, ycoord, zcoord, sensor_range); //not heavy, few ships.
//    Vector myclosemissiles = Missilemada2.getMissilesWithin(getXYZ(), sensor_range / 2.5); //xxxxnote //heavy operation
//    double distance_to_buddy = 90000.5;
//    double distance_to_target = 90000.5;


    //count visible enemies & friends, CHOOSE TARGET, figure out if too close to buddies.
    see_friend_count = 0;
    see_enemy_count = 0;
    see_friend_mislcount = 0;
    see_enemy_mislcount = 0;
    see_enemy_mislcount_close = 0;
    useSensors();

    if (isInPlayerFaction()) {
      setIsSeenByPlayer(true); //always see plr faction ships.
      if (see_enemy_mislcount > 4) {
        Missilemada2.changeWorldTimeIncrement(-1); //slow down world, because combat.
      }
    }
    if (see_enemy_count < 1) { //if enemies too far, or sensors broken. BUT could get target from buddy.
      //old have_target = false;
      //old current_target = null;
    } else {
      //if (current_target != null)
      //  System.out.println("Ship " + toString() + " sees "+see_enemy_count+" foes, curr tg range is "+calcDistanceMTMT(this, current_target));
    }

    if (current_target != null) {
      if (current_target.isDestroyed()) { //a hack to not shoot at dead ships.
        have_target = false;
        current_target = null;
      }
    }

    //generate more buildcreds.
    curr_buildcredits = curr_buildcredits + (buildcredits_gen_per_minute * (seconds / 60.0));
    if (curr_buildcredits > max_buildcredits) {
      curr_buildcredits = max_buildcredits;
      //System.out.println("Ship " + toString() + " reached full max misl buildcredits " + max_buildcredits);

      //xxxxnope - fire blindly(or at curr target) IF have two scout reports AND full hold.
      //if (parentFaction.countNumUniqueEnemiesInSRe() > 1 && Missilemada2.gimmeRandDouble() < 0.2)
      //  requestFireMissile();
    }

    //regen shields
    double shie_old = curr_shields;
    curr_shields = curr_shields + shield_regen_per_min * (seconds / 60.0);
       //47PERC OF CPU!!!!   System.out.println("REGENSHIE: "+((shield_regen_per_min * (seconds / 60.0))/(max_shields))+" ratio of max shie in this timetick. "+toString());
    if (curr_shields > max_shields) {
      curr_shields = max_shields;
      if (shie_old < 0.999 * max_shields) {
        this.gotShieldsToFull();
        shield_flash = false;
      }
      //am_under_fire = false;
      //num_missiles_targeting_me = 0; //laugh at foes
    }
    if (Missilemada2.gimmeRandDouble() < 0.03)
      shield_flash = false; //stop shield-got-hit effect.

    //formula: change behaviour-----------------------------------------------------
    did_mining = false;
    decideBehaviourAndDesti(); //does nothing if STARBASE or SENSAT
    //System.out.println("mode "+behavior_mode+ "; ship "+getId()+" sees "+see_enemy_count+" enemies and "+see_friend_count+" friends. EMisl:"+see_enemy_mislcount+" FMisl:"+see_friend_mislcount+" move*1000=" +1000* movement_in_timetick);

    //if type AND has asteroid nearby, try mining closestAsteroid.
    if (   (type.equals("MINER") || type.equals("SCOUT") || type.equals("TINYMINER") )
            && closestAsteroid != null
            && miningsystem_status > 0.65) {
      if (!cargo_full
              && calcDistanceMTMT(closestAsteroid, this) < Missilemada2.getMiningDistance()) {
        if (!closestAsteroid.isResourceless())
          did_mining = executeMining(seconds);
      } else {
        //cargo full or closestAsteroid too far.
        //xxx
        //useless spammy: Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" can't mine, cargo full("+(int)cargo_carried+") or aste too far "+(int)calcDistanceMTMT(closestAsteroid, this));
      }
    }

    //if a regular turn, attacks subroutine and move and tractor. Else mining turn, no atk or move or tractor.
    if (isSenSat()) //no atk/move/drift for SENSAT. Tiny speedup.
      return;

    if (!did_mining) {
      executeAttacks(seconds);

      //OLD: no move for base.   if (!type.equals("STARBASE")) {
      executeMove(seconds, false/*driftonly*/); //based on destination decision. //no collisions except with missiles.
      executeTractor(seconds);

      if (getSystemsStatusAvg() < 0.991) //992 is limit for one 5-percent thing.
        executeCrewRepairs(seconds); //never hull hp repairs by crew.
      if (curr_crew_count < max_crew_count && isAtFactionBase() && !isStarbase() && !isDrone())
        tryGetReplacementCrew();

      if (isAtFactionBase() && !isStarbase()) {
        boolean upgradesuccess = tryUpgradeWithMerits(seconds); //not for drones or starbase.
        if (upgradesuccess && isInPlayerFaction())
          playUpgradeinstalled();
      }
      checkIfCanHullRepair(seconds);

    } else { //did mining. move driftingly.
      if (!type.equals("STARBASE")) { //dunno if necessary.
        executeMove(seconds, true/*driftonly*/);
      }
    }
  }
  private void checkIfCanHullRepair(double seconds) {
    //if at base or AC, tryRepairShipHull()
    if (getHullPerc() < 0.96) { //without this condition, healthy ships REPAIR FOREVER...
      //if not on timer, get repaired, OTHERWISE EVERY TIME TICK NEW REPAIR, UNDER COMBAT.

      if (!isOnDelayTimer()) {
        if (!isStarbase()) {
          tryRepairShipHull(seconds); //includes: if (isNearBase() || isNearAC())

          //no time delay here, might not have been near a repair facilty.
          ///xxxxx execute hull repa AFTER wait time, for combat.... hull repa timer ticks down to zero?
        } else { //am a starbase, self-repair.

          //xxxxx starbase heals itself somewhat, NOT TO FULL
          double increaser = 1.0002 * (seconds / 200);
          if (increaser < 1.00007) {
            increaser = 1.00007;
          }
          curr_hull_hp = increaser * curr_hull_hp;
          if (curr_hull_hp > 0.90*max_hull_hp)
            curr_hull_hp = 0.90*max_hull_hp;
        }

      } else {
        //can't repair AGAIN, just unloaded, or got built, or got repaired.

      }
    } else {
      //no need for repairs.
    }
  }
  private boolean tryGetReplacementCrew() {
    //are at base and missing at least 1 crew.
    while (curr_crew_count < max_crew_count) {
      int newint = parentFaction.gimmeCrewmanFromBase(this);
      if (newint == 1)
        curr_crew_count = curr_crew_count + newint;
      else
        break;
    }
    if (isInPlayerFaction() && curr_crew_count == max_crew_count)
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" has a full crew again after visiting base.");
    return (curr_crew_count == max_crew_count);
  }
  private boolean isOnDelayTimer() {
    return (timestamp_next_allowed_accel > Missilemada2.getWorldTime());
  }
  public long gettimestamp_next_allowed_accel() {
    return timestamp_next_allowed_accel;
  }
  public boolean /*got an upgrade?*/ tryUpgradeWithMerits(double seconds) { //starbase should not call this.
    double meritcost_of_upgrade = 2.0;
    if (!isAtFactionBase()) { //must be at base to upgrade.
      return false;
    }
    if (isDrone()) { //drones no get upgrades, very expendable.
      return false;
    }
    if (merit_badges_curr < meritcost_of_upgrade) {
      return false;
    } else {
      //apply N upgrades, cost 2 per.
      while (merit_badges_curr > meritcost_of_upgrade) {
        merit_badges_curr = merit_badges_curr - meritcost_of_upgrade;

        if (isScout()) { //rand: speed or sensor upgrade, whichever happened to be lying around at base.
          if (Missilemada2.gimmeRandDouble() < 0.5) {
            max_speed = max_speed * 1.02;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" earned an engine upgrade. Installed now at base.");
          } else {
            sensor_range = sensor_range * 1.02;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" earned a sensor upgrade. Installed now at base.");
          }
        }
        if (type.equals("MINER")) { //rand: speed or cargo upgrade
          if (Missilemada2.gimmeRandDouble() < 0.5) {
            max_speed = max_speed * 1.04;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" earned an engine booster. Installed now at base.");
          } else {
            cargo_capacity = cargo_capacity + 20.0;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" earned a 20-ton cargo module. Installed now at base.");
          }
        }
        if (type.equals("DEFENDER")) { //rand: shields or defbeam upgrade
          if (Missilemada2.gimmeRandDouble() < 0.5) {
            shield_regen_per_min = shield_regen_per_min * 1.04;
            max_shields = max_shields * 1.03;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" earned a shield generator upgrade. Installed now.");
          } else {
            defense_beam_accuracy = defense_beam_accuracy * 1.07;
            sensor_range = sensor_range * 1.015;
            defense_beam_rechargebasethou = defense_beam_rechargebasethou * 0.97;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" upgraded beam targeting AI. Installed now at base.");
          }
        }
        if (type.equals("AC")) { //rand: shields or defbeam or nanobuild upgrade
          double rand = Missilemada2.gimmeRandDouble();
          if (rand < 0.33) {
            shield_regen_per_min = shield_regen_per_min * 1.045;
            max_shields = max_shields * 1.028;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" earned a shield generator upgrade. Installed now.");
          } else if (rand > 0.33 && rand < 0.66) {
            defense_beam_accuracy = defense_beam_accuracy * 1.06;
            sensor_range = sensor_range * 1.010;
            defense_beam_rechargebasethou = defense_beam_rechargebasethou * 0.97;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" upgraded beam targeting AI. Installed now at base.");
          } else if (rand > 0.66) {
            buildcredits_gen_per_minute = buildcredits_gen_per_minute * 1.04;
            max_buildcredits = max_buildcredits * 1.05;
            if (isInPlayerFaction())
              Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName()+" got a missile constructor upgrade, +4%. Installed now at base.");
          }
        }
      } //end while
      setTimeDelay(1.7 * 3600); //upgrade takes time. 1.5h
     return true;
    }
  }
  public void tryRepairShipHull(double seconds) { //starbase should not call this.
    if (isDestroyed())
      return;
    if (parentFaction.getStarbase() == null)
      return;

    //if in battle, NO REPAIR FOR YOU until things settle down (for the delicate repair drones(not drawn or tracked))
    if (am_under_fire || see_enemy_mislcount > 1 || see_enemy_count > 0)
      return;

    //if near base
    if (isAtFactionBase() && parentFaction.getStarbase().getHullPerc() > 0.3) {
      //System.out.println("Ship "+unique_id+" hullrepairs at base.");
      if (isInPlayerFaction())
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName() + " hull repairs, at base.",3);

      setIsNearBattle(false);
      reduceSpeed(0.50);
      getMoreDroneCargoFromBase();
      curr_hull_hp = curr_hull_hp + (0.08*max_hull_hp); //xxx 10% repair in an hour?
      //gain some xp, engineers learn from repairing.
      parentFaction.addXPToCommander(this, "HULLREPAIR");

      if (curr_hull_hp > max_hull_hp) {
        curr_hull_hp = 1.03 * max_hull_hp; //YY had might 0.03 bug.
        if (isInPlayerFaction()
            /*&& getHullPerc() > 0.98*/) { //if plr and got to full HP
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStrTypeNName() + " hull repairs COMPLETED, at base.",3);
        }
      }
      curr_shields = 0.0; //purged capacitors, vulnerable during repairs!
      mass_kg = max_mass_kg; //can lose plating in battle. Now restored.
      if (getHullPerc() > 0.98) {
        mass_kg = 1.05 * max_mass_kg;
      }

      if (getBattleStrPerc() < 0.30) { //if shipwide fire... and hullrepair...
        lifesupport_status = lifesupport_status + 0.05;
        engine_status = engine_status + 0.05;
        shield_status = shield_status + 0.05;
      }

      //do systems repairs? yes, in its own func.

      //use metal1(hull) resources from faction
      parentFaction.resource_metal1 = parentFaction.resource_metal1 - 0.0020*(mass_kg/1000.0);    ///- 0.1*(buildcost / 28.0);
      parentFaction.resource_metal2 = parentFaction.resource_metal2 - 0.0005*(mass_kg/1000.0);
      parentFaction.resource_fuel = parentFaction.resource_fuel     - 0.005*(mass_kg/1000.0); //resupply ship with fuel. (ships don't count how much fuel they have or use.)
      //is fine if base(faction) goes to negative resource -- they tore it from base's hull.

      //MISSILE STOCK RESUPPLIED FROM BASE'S ARSENAL
      double change = max_buildcredits - curr_buildcredits;
      curr_buildcredits = curr_buildcredits + change;
      parentFaction.getStarbase().changeBuildCreditsPlusMinus(-change);

      timestamp_next_allowed_accel = Missilemada2.getWorldTime() + 3600; //seconds
      if (isInPlayerFaction()) {
        Missilemada2.addVfxOnMT(getX(), getY(), getZ(), "HULLREPAIRS", 19000, 1380.0, 1.0/*transp*/, null, "hullrepair2.png", 1.0, "");
      }
    }

    //if near an AC, assault cruiser.
    Ship an_ac = Missilemada2.getNearestFriendlyAC(this);
    if (an_ac != null && an_ac != this) { //AC can't repair at_self!
      if (calcDistanceMTMT(this, an_ac) < Missilemada2.getMiningDistance()
          && an_ac.getHullPerc() > 0.4) {
        //System.out.println("Ship "+unique_id+" hullrepairs at relatively-intact AC.");

        setIsNearBattle(false);
        curr_hull_hp = curr_hull_hp + (0.1*max_hull_hp); //worse repair speed than at base. xxxxxxxxxxxxx
        if (isInPlayerFaction()
                && getHullPerc() > 0.98) { //if plr and got to full HP
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + type +" "+unique_id + " hull repaired at Assault Cruiser "+an_ac.getId()+".",3);
        }

        curr_shields = 0.0; //vulnerable during repairs!
        mass_kg = 1.1 * max_mass_kg; //can lose plating in battle. Now restored.
        reduceSpeed(0.30);
        //add crew? not at AC.

        //use metal1(hull) resources from faction (kept in AC's hold)
        parentFaction.resource_metal1 = parentFaction.resource_metal1 - 0.1*(buildcost / 28.0);

        timestamp_next_allowed_accel = Missilemada2.getWorldTime() + 4*3600; //seconds
        if (isInPlayerFaction()) {
          Missilemada2.addVfxOnMT(getX(), getY(), getZ(), "HULLREPAIRS", 19000, 1380.0, 1.0/*transp*/, null, "hull_repair.png", 1.0, "");
        }
      }
    }
  }
  public void changeBuildCreditsPlusMinus(double v) {
    curr_buildcredits = curr_buildcredits + v;
  }
  private void executeCrewRepairs(double seconds) {
    if (isDrone()) {
      if (isAtFactionBase()) {
        //ok you can get repairs, are at base.
      } else {
        //no crew, no repairs in the field.
        return;
      }
    }

    //system repairs: 1% chance of 5% sysrepair per crewman, per (5m tick)
    // (first life supp, then engines, then sensors, then shie, weap, hull, mining) (cargo bay damage is nothing, easy to re-pick-up fallen ore)
    //onmouseover xxx, ship numbers info panel. cargo(resources), crew count.

    //if life support is poorly, chance of losing a crewman.
    if (lifesupport_status < 0.45 || beamsystem_status < 0.35 ) {
      if (Missilemada2.gimmeRandDouble() < 0.0006 * seconds) {
        if (curr_crew_count > 0 && !isAtFactionBase()) {
          curr_crew_count = curr_crew_count - 1; //was: at base, get new crewman, crewman dies of lifesupport :-)
          //System.out.println("Ship "+unique_id+" lost a crewman to life support systems failure or power grid.");
          if (isInPlayerFaction()) {
            Missilemada2.addVfxOnMT(getX(), getY(), getZ(), "CREWMANDIED", 9000, 380.0, 1.0/*transp*/, null, "lost_one_crew.png", 1.0, "");
            Missilemada2.addToHUDMsgList("Our " + this.toStrTypeNName() + " lost a crewman to oxygen failure or power grid.",0);
            playCrewmanDied();
          }
          parentFaction.lostCrewmen(1);
        }
        if (isAtFactionBase()) {
          lifesupport_status =+ 0.03;
          beamsystem_status =+ 0.03;

        }
      }
    }
    //insert repair crew if at base.
    if (isAtFactionBase() && parentFaction.isBaseAlive() && !type.equals("STARBASE")) {
      curr_crew_count = curr_crew_count + 8;
      //System.out.println("Ship "+unique_id+" got base's repair crew onboard. currcrew="+curr_crew_count);
    }
    for (int i = 1; i <= curr_crew_count; i++) { //for each crewman, try a repair.
      if (Missilemada2.gimmeRandDouble() < (0.000012 * seconds)) { //0.x% of repair success PER N SECONDS PER CREWMAN. then see what got repaired.

        //NOT setTimeDelay(1200); //regular operations delayed due to successful repair.

        if (isInPlayerFaction()) {
          //nope, too spammy: Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("crewrepair yay", 6, "") /*Vector of pitches*/, 52 /*core note*/, 46 /*harp*/, 35, 1.9F /*note duration*/);
          Missilemada2.addVfxOnMT(0, 150, 50, "CREWREPAIR", 17000, 1100.0, 0.7/*transp*/, this, "crewrepairs2.png", 1.0, "");
        }
        curr_buildcredits = 0.94 * curr_buildcredits; //use some of the missile nanotech on the repairs.
        //in priority order try:
        if (lifesupport_status < 0.95) {
          lifesupport_status = lifesupport_status + 0.05; //for better CREW surviving
        } else if (engine_status < 0.95) {
          engine_status = engine_status + 0.05; //for better fleeing
        } else if (sensors_status < 0.95) {
          sensors_status = sensors_status + 0.05; //for better fleeing and fighting
        } else if (shield_status < 0.95) {
          shield_status = shield_status + 0.05; //for better surviving and fighting
        } else if (beamsystem_status < 0.95) {
          beamsystem_status = beamsystem_status + 0.05; //for better defense and fighting, also powered by shield generators.
        } else if (missilesystem_status < 0.95) {
          missilesystem_status = missilesystem_status + 0.05; //for better fighting
          misl_DNA = Missilemada2.randomDNAFromStr(Missilemada2.randomDNAFromStr(misl_DNA)); //missile system repairs altered what missiles we build!!
        } else if (miningsystem_status < 0.95) {
          miningsystem_status = miningsystem_status + 0.05;
        }
      }
    }
    //remove repair crew if at base.
    if (isAtFactionBase()  && parentFaction.isBaseAlive()  && !type.equals("STARBASE")) {
      curr_crew_count = curr_crew_count - 8;
      //System.out.println("Ship "+unique_id+" removed base's repair crew.  currcrew="+curr_crew_count);
    }
  }
  private void useSensors() {
    am_under_fire = false;
    double curr_sensor_range = sensors_status * sensor_range; //xxx times current hazardzone multiplier...notyet
    my_dist_from_frontline = calcDistanceVecVec(getXYZ(), parentFaction.getFrontlineXYZ());

    if (current_target != null) {
      //if too far, forget target UNLESS it is buddy's target.
      if (calcDistanceMTMT(this, current_target) > getSensorRange()) {
        if (isShipMyBuddysTarget(current_target)) {
          //keep target
        } else {
          //forget target
          current_target = null;
        }
      }

    }
    if (current_target != null) {
      //if target is destroyed, forget it.
      if (current_target.isDestroyed()) {
        current_target = null;
      }
    }

    //reset seen asteroids and see anew.
    //SPEEDUP: not every time-tick.
  if (Missilemada2.gimmeRandDouble() < 0.13) {
    see_asteroid_count = 0;
    if (type.equals("TINYMINER")) //a hack for the nearly-blind mining drone.
      seenAsteroids_thistimetick = Missilemada2.getAsteroidsWithin(this.getX(), this.getY(), this.getZ(), 3.0*curr_sensor_range); //heavy op
    else
      seenAsteroids_thistimetick = Missilemada2.getAsteroidsWithin(this.getX(), this.getY(), this.getZ(), curr_sensor_range); //heavy op

    for (int i = 0; i < seenAsteroids_thistimetick.size(); i++) {
      Asteroid as = (Asteroid) seenAsteroids_thistimetick.elementAt(i);
      if (as != null) {
        see_asteroid_count++;

        //FlatSprite.drawFlatLineVecVec(this.getXYZ(), as.getXYZ(), 30.5*this.getRadius());



        //if sensor-user is in combat, then objects nearby are tagged as being in combat zone!
        if (am_under_fire) {
          if (calcDistanceMTMT(as, this) < 0.3*sensor_range)
            as.setIsNearBattle(true);
        } else {
          //am in peace mode, tag this seen asteroid as "safe".
          if (calcDistanceMTMT(as, this) < 0.4*sensor_range)
            as.setIsNearBattle(false);
        }

        if ((calcDistanceMTMT(as, this) < Missilemada2.getAsteroidScanningRange()) && !Missilemada2.isAsteroidKnownToFaction(as, parentFaction)) { //if close enough to scan, and unscanned, send report.

          if (curr_crew_count > 0) { //drones can't report a scan, they are cheap.
            parentFaction.addScoutReportAste(this, new ScoutReport(Missilemada2.getWorldTime(), as), as);

            //delay for thorough scanning...
            timestamp_next_allowed_accel = Missilemada2.getWorldTime() + (long)(3000); //seconds
            reduceSpeed(0.90);

            //huge Vfx.
            if (isInPlayerFaction()) {
              //old: Missilemada2.addVfxOnMT((getX() + as.getX()) / 2.0, (getY() + as.getY()) / 2.0, 2.0 * as.getZ(), "SCOUTEDASTEROID", 13000, 1850.0, 0.75/*transp*/, null, "scan_cyan2.png", 1.0, "");
              Missilemada2.addVfx2(as.getXYZ(), "SCOUTEDASTEROID", 13000, 1850.0, 0.75/*transp*/, "scan_cyan2.png", 1.0, "" );
              //if (!as.isResourceless())
              //  Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStringShort()+" scanned usable asteroid "+as.getId() + " Merit get!");
            }
            //System.out.println(type+" ship "+unique_id+" _scouted_ asteroid " +s.toString() + " when senrange="+sensor_range + " scanrange="+Missilemada2.getAsteroidScanningRange());
          }
        }
        //if (!Missilemada2.isAsteroidKnownToPlayer(s))
        //  System.out.println("Ship "+unique_id+" _saw_ unscouted asteroid " +s.toString() + " at dist "+calcDistanceMTMT(s, this)+" when senrange="+sensor_range);


        if (closestAsteroid != null) {
          if (calcDistanceMTMT(as, this) < calcDistanceMTMT(closestAsteroid, this)  ) {
            closestAsteroid = as;
          }
        } else {
          closestAsteroid = as; //first in sensor range.
        }

        //all ships can send scouting candidate spots. But tinyminer can't scan asteroids, being CHEAP.
        if (Missilemada2.gimmeRandDouble() < 0.04) { //try to save cpu time with rarer isAsteroidScouted() calls.
          if (!parentFaction.isAsteroidScouted(as)){
            parentFaction.addScoutingCandidateSpot(as.getMiningXYZ()); //was a heavy op, so often!  //MobileThing.changeXYZ(as.getXYZ(), 1.6*as.getRadius(),0,0)
          }
        }
      }
    }
  }//end asteroids seeing

    //----detect dead ships, possibly choose and claim one for tractoring.
    if (buddy_derelict != null) {
      buddy_derelict.setTractorer(null); //un-claim derelict, in case we too far.
    }
    buddy_derelict = null; //reset in case we moved too far from it. I or other can reacquire it.
    Vector seen_dead_ships = Missilemada2.getDeadShipsWithin(this.getX(), this.getY(), this.getZ(), curr_sensor_range); //xx heavy op?
    for (int i = 0; i < seen_dead_ships.size(); i++) {
      Ship seen_dead_ship = (Ship) seen_dead_ships.elementAt(i);
      if (isInPlayerFaction()) {
        if (seen_dead_ship.isSeenByPlayer()) {
          //yeah we already know that one.
        } else {
          seen_dead_ship.setIsSeenByPlayer(true); //first purpose of deadshipslist
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + " We have spotted an OLD DERELICT "+seen_dead_ship.getType()+" (str "+seen_dead_ship.getBattleStrIntDisp()+")." ,3);
        }
      }
      if (canTractorThisDerelict(seen_dead_ship) //if I is capable
              && !seen_dead_ship.hasTractorer() //if not claimed
              && calcDistanceMTMT(this, seen_dead_ship) < getRangeForSetDerebuddy() /* if close*/) {
        if (seen_dead_ship.isSenSat() || seen_dead_ship.isStarbase() ) {
          //don't.
        } else { //tractorable type.
          buddy_derelict = seen_dead_ship; //yay //second purpose of deadshipslist
          buddy_derelict.setTractorer(this); //claim (or re-claim) this for my tractoring.
          break; //xx I guess necessary
        }
      } else {
        //cannot tractor, for various reasons.
        // nothing?
      }
    }

    //possibly reset buddies, if they are waaay too far. OR DEAD.
    if (buddy_mil != null) {
      if (calcDistanceMTMT(this, buddy_mil) > 1.3*curr_sensor_range || buddy_mil.isDestroyed()) {
        if (isDrone()) {
          //drone shall not forget.
        } else {
          buddy_mil = null; //too far, forget (lose track).
        }
      }
    }
    if (buddy_civilian != null) {
      if (calcDistanceMTMT(this, buddy_civilian) > 1.3*curr_sensor_range  || buddy_civilian.isDestroyed())
        buddy_civilian = null; //too far, forget (lose track).
    }

    //SPEEDUP: using RTree spatial indexing.  //SPEEDUP: not on every time-tick.???
    if (see_total_enemy_battlestr > 1.1*see_total_friend_battlestr || Missilemada2.gimmeRandDouble() < 0.20/*0.10*/) {

    Vector seenships = Missilemada2.getShips_XYNearest(this.getX(), this.getY(), 1.15*curr_sensor_range, 40/*max see 30 ships, CPU-COSTLY.*/);
    Ship prev_buddy_mil = buddy_mil;
    see_total_enemy_battlestr = 0.0;
    see_total_friend_battlestr = 0.0;
    see_friend_count = 0;
    see_enemy_count = 0;

    for (int i = 0; i < seenships.size(); i++) {
      Ship seen_ship = (Ship) seenships.elementAt(i);
      if (seen_ship != null) {
      if (calcDistanceMTMT(this, seen_ship) < curr_sensor_range * seen_ship.getStealthFactor()/*max 1.15 min 0.55*/ ) {
        //this added coz on RTree using lost Z dimension distance.
        //stealth reduces effective detection range.

        if (seen_ship.getFaction() == parentFaction) {
          //friend

          if (this.isInPlayerFaction()) //if see-er is in player faction
            seen_ship.setIsSeenByPlayer(true);

          if (seen_ship.getId() != this.getId()) { //self does not count.
            see_friend_count++;
            see_total_friend_battlestr = see_total_friend_battlestr + seen_ship.getBattleStr();

            //buddy_civilian deciding xxx
            if (seen_ship.getType().equals("MINER"))
              buddy_civilian = seen_ship;

            //buddy_mil deciding. One can not follow a drone's lead! A matter of honor.
            //base is allowed to have buddies. No-one can set base as buddy.
            if (prev_buddy_mil == null) { //if dont have buddy, then whichever visible is buddy.
              if (seen_ship.getType().equals("DEFENDER") || seen_ship.getType().equals("AC")) //not drones nor base nor scout. (scouts too fast to follow.)
                buddy_mil = seen_ship;
            } else { //choose better milbuddy, stronger one.
                  if (seen_ship.getBattleStr() > buddy_mil.getBattleStr() && !seen_ship.isStarbase()) {
                    buddy_mil = seen_ship;
                  }

//              if (seen_ship.isInCombat()) { //if friend is in combat, be its buddy.
//
//                  if (seen_ship.getBattleStrPerc() < 0.95) {
//                    //BAD: be damaged one's buddy rather than intact one's.
//                    prev_buddy_mil = buddy_mil;
//                    buddy_mil = seen_ship;
//                  } else {
//                    //meh, you're intact, I'll be buddy to someone else.
//                    //keep prev buddy.
//                  }
//              }
            }
          } //else self, ignore.
          //possible 0.01 chance of friendly radio babble/chatter.
          if (Missilemada2.gimmeRandDouble() < 0.0003 && isInPlayerFaction()) {
            Missilemada2.playRadioChatter(1/*which chatter*/, 70/*int*/, 65/*vol*/, 12/*pitch offset*/);
          }
        } else { //see an enemy
          see_enemy_count++;
          if (this.isInPlayerFaction())
            seen_ship.setIsSeenByPlayer(true); //but use scoutreports of faction to decide when to draw enemy ship.

          this.setIsNearBattle(true);
          seen_ship.setIsNearBattle(true);

          see_total_enemy_battlestr = see_total_enemy_battlestr + seen_ship.getBattleStr();

          //xxxxx too often?
          parentFaction.addScoutReportEnemyShip(this, new ScoutReport(Missilemada2.getWorldTime(), seen_ship));

          //if miner, change heading coz saw foe. //remember TINYMINER has poooor sensor range.
          if (type.equals("TINYMINER")
              || (type.equals("MINER") && getShieldPerc() < 0.9 ) ) { //full-shields miner ignores first sign of trouble.
            if (parentFaction.getStarbase() != null) { //if we still have a base
              //flee, if it is 50% stronger than me.
              if (seen_ship.getBattleStr() > 1.5*getBattleStr()) {
                setDes(parentFaction.getXYZ_starbase_safer_side(), "miner to base coz saw foe");
                //not forced.
              }
            }
          } else {
            if (Missilemada2.gimmeRandDouble() < 0.01) {
              parentFaction.addScoutingCandidateSpot(seen_ship.getXYZ()); //was a heavy op, so often!
            }
          }
          //shift frontline towards the ship (this) that saw enemy!! and towards the foe.
          if (Missilemada2.gimmeRandDouble() < 0.07) { //many usesensors calls per frame.
            parentFaction.shiftFrontLine(this.getXYZ(), 0.20, this);
            parentFaction.shiftFrontLine(seen_ship.getXYZ(), 0.10, this);
          }
          //System.out.println("Ship "+unique_id+" saw enemy ship " +s.toString() + " when senrange="+sensor_range);

          //UPDATE "enemies I have seen, aka possible targets to shoot at"
          if (!seen_ship.isDestroyed()) {
            if (current_target != null) {
              //update closer one.
              if (calcDistanceMTMT(seen_ship, this) < calcDistanceMTMT(ene_closest, this)  ) {
                ene_closest = seen_ship;
              }
              //update more damaged one.
              if (ene_most_damaged != null) {
                if (seen_ship.getHullPerc() < ene_most_damaged.getHullPerc()) {
                  ene_most_damaged = seen_ship;
                }
              }
              //update more dangerous one.
              if (ene_most_dangerous != null) {
                if (seen_ship.getBattleStr() > ene_most_dangerous.getBattleStr()) {
                  ene_most_dangerous = seen_ship;
                }
              }
              //update less dangerous one.
                if (ene_least_dangerous != null) {
                  if (seen_ship.getBattleStr() < ene_least_dangerous.getBattleStr()) {
                    ene_least_dangerous = seen_ship;
                  }
                }
            } else { // curr target is null, now first sighting, initial current_target.
              //nope: am_under_fire = true;
              current_target = seen_ship;
              have_target = true;
              ene_closest = seen_ship;
              ene_least_dangerous = seen_ship;
              ene_most_dangerous = seen_ship;
              ene_most_damaged = seen_ship;
              //put second vfx.
              if (isInPlayerFaction()
                      || this.isSeenByPlayer() ) {
                Missilemada2.addVfx2(current_target.getXYZ(), "TARGETLOCK", 1200/*sec*/, 410.0/*siz*/, 0.4/*transp*/, "antiscan.png", 1.0, "");
              }
            }
          }

          //DECIDE TARGET, closest or weakest or strongest or most damaged. (atk beam always to closest)
          if (current_target != null) { //if have seen at least ONE, can make a decision. else,
            if (type.equals("SCOUT")) { //scout harasses the lesser ones
              current_target = ene_least_dangerous;
              have_target = true;
            }
            if (type.equals("AC")) { //assault cruiser takes the big challenge
              current_target = ene_most_dangerous;
              have_target = true;
            }
            if (type.equals("STARBASE")) { //base takes the big challenge
              current_target = ene_most_dangerous;
              have_target = true;
            }
            if (type.equals("DEFENDER")) {
              current_target = ene_most_damaged;
              have_target = true;
            }
            //drones a bit stupider, always choose closest enemy.
            if ( type.equals("MISSILEDRONE") || type.equals("BEAMDRONE")) {
              current_target = ene_closest;
              have_target = true;
            }
            //MINER
            if (type.equals("MINER")) { //miner focuses on the lesser ones, has no chance vs a defender.
              current_target = ene_least_dangerous;
              have_target = true;
            }
            if (isInPlayerFaction() && prev_timetick_target != current_target) {
              Missilemada2.addVfx2(current_target.getXYZ(), "TARGETLOCK", 1300/*sec*/, 1110.0/*siz*/, 0.5/*transp*/, "scan.png", 1.0, "");
              //possible radio chatter about enemy, if player faction.
              if (Missilemada2.gimmeRandDouble() < 0.0005 && isInPlayerFaction()) {
                Missilemada2.playRadioChatter(2/*which chatter*/, 70/*instr*/, 95/*vol*/, 13/*pitch offset*/);
              }
            }
            prev_timetick_target = current_target;
            if (current_target.isDestroyed()) {
              current_target = null;
              prev_timetick_target = null;
              have_target = false;
            }
          }
        }
      }
    }
    }
    }
    if (isInPlayerFaction()) {
      if (see_enemy_count == 0) {
        if (Missilemada2.gimmeRandInt(100) < 5)
          Missilemada2.changeWorldTimeIncrement(1); //speed up world, GRADUALLY, because appears peaceful to this PLAYERFACTION ship.
      }
      if (see_enemy_count > 2) {
        Missilemada2.changeWorldTimeIncrement(-1); //slow down world, because combat is happening.
      }
    }

    //count enemy and friend missiles, for decisionmaking. was cpu-heavy at 150 ships times 400 missiles. Now RTree.
    //OLD: Vector seen_mis = Missilemada2.getMissilesWithin(getXYZ(), curr_sensor_range); //costly op
    Vector seen_mis = Missilemada2.getMissiles_XYNearest_ones(getX(), getY(),
             1.02 * curr_sensor_range, 60/*how many to return, note:can be lots, NOT THAT CPU-COSTLY */); //RTree version for speedup when 150+ missiles.
    see_enemy_mislcount = 0;
    see_enemy_mislcount_close = 0;
    Missile enemis = null;
    for (int i = 0; i < seen_mis.size(); i++) {
      Missile mi = (Missile) seen_mis.elementAt(i);
      if (mi != null) {
        //missiles may be too far on the z-axis, to be seen by the sensors. handled:
        if (calcDistanceMTMT(this, mi) < curr_sensor_range) { //if actually within 3d-distance sensor range
          if (this.isInPlayerFaction())
            mi.setIsSeenByPlayer(true);
          if (mi.parentFaction == this.parentFaction) {
            //friends'
            see_friend_mislcount++;
          } else {
            //foes'
            see_enemy_mislcount++;
            enemis = mi;

            if (Missilemada2.gimmeRandDouble() < 0.01)
              parentFaction.shiftFrontLine(mi.getXYZ(), 0.04, this); //oh there is enemy combat activity near me? move faction's frontline a tiny bit.


            if (this.isInPlayerFaction())
              mi.setIsSeenByPlayer(true);
            if (am_under_fire) { //if sensor-user is in combat, then objects nearby are tagged as being in combat zone! (detectable by, haha, radiation traces on missile surface)
              mi.setIsNearBattle(true);
            }

            if (calcDistanceMTMT(this, mi) < 0.3*sensor_range && mi.curr_hull_hp > 0.01) {
              see_enemy_mislcount_close++;
              am_under_fire = true;
            }
            if (calcDistanceMTMT(this, mi) < getDefenseBeamRange() && mi.curr_hull_hp > 0.01)
              tryShootDownMissile = mi;



            ///////if (s.getTarget)

          }
        }
      } //else m is null, bad.
    } //end foreach seen missile

    //if see lone ene-missile AND the frontline is close to base, shift frontline.
    double distance_betw_base_and_FL = calcDistanceVecVec(parentFaction.getXYZ(), parentFaction.getFrontlineXYZ());
    if (see_enemy_count == 0
            && see_enemy_mislcount == 1
            && enemis != null
            && enemis.curr_hull_hp > 0.01
            && distance_betw_base_and_FL < 0.18 * Missilemada2.getBaseDistance()) {
      parentFaction.shiftFrontLine(enemis.getXYZ(), 0.002, this); //oh there is enemy combat activity near me? move faction's frontline a tiny bit.
    }

    //possibly reset am_under_fire.
    if (see_enemy_count == 0 && see_enemy_mislcount == 0 && tryShootDownMissile == null && sensors_status > 0.7) { //if see no hostiles, and eyes are working, calm down.
      if (buddy_mil != null) {
        if (buddy_mil.hasTarget()) {
          //buddy in combat, we keep am_under_fire
        } else {
          am_under_fire = false;
        }
      } else { //no buddy, can reset
        am_under_fire = false;
      }
    }

    //xxxxxxx enemy bases
  }
  private boolean isShipMyBuddysTarget(Ship ta) {
    if (buddy_civilian != null) {
      if (buddy_civilian.getTarget() == ta) {
        return true;
      }
    }
    if (buddy_mil != null) {
      if (buddy_mil.getTarget() == ta) {
        return true;
      }
    }
    return false;
  }
  public double getStealthFactor() {
    //return 1.15(hurt, glowing, am large) to 0.55(great stealth). This multiplies foe sensor range.
    //double stealth; //resist enemy sensors. 0.0(assault cruiser) to 1.0(sensor satellite)
    double ret = 1.0; //default
    ret = ret - 0.45 * stealth;

    if (getHullPerc() < 0.4 || engine_status < 0.7 || shield_status < 0.7)
      ret = ret * 1.15; //more visible coz banged up.

    return ret;
  }
  public boolean isDrone() {
    if (max_crew_count == 0)
      return true;
    else
      return false;
  }
  public boolean isMiner() { //non-civ means mil+scout
    if (type.equals("MINER") || type.equals("TINYMINER")) //not:  type.equals("SENSAT")
      return true;
    else
      return false;
  }
  public boolean isMil() {
    if (type.equals("DEFENDER") || type.equals("AC")  || type.equals("MISSILEDRONE") || type.equals("BEAMDRONE"))
      return true;
    else
      return false;
  }
  public boolean isScout() {
    if (type.equals("SCOUT"))
      return true;
    else
      return false;
  }
  public boolean isSenSat() {
    if (type.equals("SENSAT"))
      return true;
    else
      return false;
  }
  public boolean isStarbase() {
    if (type.equals("STARBASE"))
      return true;
    else
      return false;
  }
  private void executeAttacks(double seconds) {
    //xx seconds is irrelevant? because beam is on timer and missiles are limited resource. //put missiles on timer? nope.

    if (isOnDelayTimer()) { //if is being built, or recovering from SOMETHING, shall not launch attacks.
      return;
    }

    if (type.equals("SENSAT") || type.equals("TINYMINER") )
      return;

    //try capture enemy ship, if it is near and enough damaged. Hacking.
    if (current_target != null) {
      if (calcDistanceMTMT(this, current_target) < 1.4*Missilemada2.getArrivedDistance()) { //close
        if (current_target.getShieldPerc() < 20.0 && current_target.getHullPerc() < 0.6 && current_target.getSystemsStatusAvg() < 0.8) { //damaged
          //xxxxx add vfx of hack attempt!

          if (current_target.curr_crew_count < 2 /*single person not enough to fight off a hack, busy captaining */
          || (Missilemada2.gimmeRandDouble() < 0.05 * (this.curr_crew_count - (current_target.lifesupport_status * (current_target.curr_crew_count - 1))))) { //rand chance vs hacking ship's crew count
            current_target.surrender_or_got_captured(this /*surrender-to*/, true/*hack*/);

            if (isInPlayerFaction()) {
              Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("hackazat", 10, "") /*Vector of pitches*/, 62 /*core note*/, 30 /*dist guit*/, 100, 3.3F /*note duration*/);
            }
          }
        }
      }
    }

    if (beamsystem_status > 0.4 && shield_regen_per_min > 1.0 && curr_shields > 0.10*max_shields) { //shieldless can't use def/atk beams! no generator power then.

      if (beam_available_timestamp < Missilemada2.getWorldTime()) {

        if (sensors_status > 0.5 && beamsystem_status > 0.4) {
          //----try missile shootdown with defense beam //can do this EVEN IF SEE NO ENEMIES (far, or sensors broken)
          if (see_enemy_mislcount > 0 && defense_beam_accuracy > 0.01) {
            //Vector list = Missilemada2.getMissilesWithin(this.getXYZ(), getDefenseBeamRange()); //costly op
            if (tryShootDownMissile != null) {
              Missile targetmis = tryShootDownMissile;
              if (calcDistanceMTMT(targetmis, this) < getDefenseBeamRange() && targetmis.curr_hull_hp > 0.01) {
                //then attempt shootdown.
                //parentFaction.shiftFrontLine(this.getXYZ(), 0.2, this);

                //used power from the shield batteries
                //xxgameplay: dont use power, shield loss would be from succ/unsucc misl...   curr_shields = curr_shields - (0.015*max_shields);
                beam_def_draw_counter_frames = (int) Math.round(0.63 * Missilemada2.getFPS()); //frames
                stored_tryShootDownMissile = tryShootDownMissile; //store it, coz try_sd WILL vary rapidly, while def beam recharges.
                beam_available_timestamp = Missilemada2.getWorldTime() + getDefenseBeamRechargeSeconds(); //used my shootdown try
                if (isInPlayerFaction()) {
                  Missilemada2.changeWorldTimeIncrement(-1); //slow down world, because combat
                  playDefBeamFiringNote();
                }
                //System.out.print("debug: shootdown try: def_acc "+defense_beam_accuracy+", mis eva "+targetmis.getDefenseBeamEvasion() + " ");
                if ( (defense_beam_accuracy > targetmis.getDefenseBeamEvasion() && Missilemada2.gimmeRandDouble() < 0.6)
                    || /*OR lucky shot*/ (Missilemada2.gimmeRandDouble() > 0.96)  ) { //might get multiple attempts due to slow missile???
                    targetmis.curr_hull_hp = 0.0; //so other ships WON'T target this dead missile!
                    Missilemada2.removeMissile(targetmis);
                    mislcred_shot_down_xxxxtodo = mislcred_shot_down_xxxxtodo + targetmis.getCost();
                    addMerit(0.001 * targetmis.getCost());

                    //if can tractor anything, get mis fragments to self.
                    if (canTractorThisDerelict(this))
                      curr_buildcredits = curr_buildcredits + (0.35*targetmis.getCost()); //salvage bits of enemy missile.

                    //System.out.println("misl "+targetmis.getId()+" shootdown success. Maxbeamrange: "+ (int)getDefenseBeamRange() + " -- "+toString());
                    //vfx, sfx
                    if (isInPlayerFaction() /*|| targetmis.getFaction() == Missilemada2.getPlayerFaction() */ || targetmis.isSeenByPlayer()) {
                      Missilemada2.addVfx2(targetmis.getXYZ(), "MISL EXPLO", 1400/*sec*/,
                              1310.0 * (targetmis.getYield() / Missilemada2.getAvgMislYield())/*siz*/, 0.6/*transp*/, "missile_shotdown.png", 1.0, "");
                      Missilemada2.createDebrisFlatSprite("missile-hit-defebeam_debris.png", targetmis.getSpeedCurrent(), 850.0*(1.0+Missilemada2.gimmeRandDouble()), 800, this, false);
                      Missilemada2.createDebrisFlatSprite("missile-hit-defebeam_debris.png", targetmis.getSpeedCurrent(), 650.0*(1.0+Missilemada2.gimmeRandDouble()), 600, this, false);
                      Missilemada2.createDebrisFlatSprite("missile-hit-defebeam_debris.png", targetmis.getSpeedCurrent(), 950.0*(1.0+Missilemada2.gimmeRandDouble()), 1100, this, false);
                    }
                } else {
                  //System.out.println("misl "+targetmis.getId()+" shootdown failure.");
                  //vfx of defe beam missing the missile. xx none.
                }
              }
            }
          }

          //----try beam attack/atk beam ON CLOSEST ENEMY(not curr target).
          boolean want_to_use_atkbeam = true;
          if (danger_meter > 1.2) {
            want_to_use_atkbeam = false;
          }
          if (ene_closest == null && current_target != null) { //miner hack.
            ene_closest = current_target;
          }
          if (ene_closest != null) {
            //System.out.println(type + " " + getId() + " tries beam attack on ship "+current_target.getId()+" at range "+ getAttackBeamRange() + " which might be too far");
            if (calcDistanceMTMT(ene_closest, this) < getAttackBeamRange()
                    && !ene_closest.isDestroyed()
                    && getShieldPerc() > 0.25
                    && want_to_use_atkbeam) {
              //used power from the shield batteries
              //curr_shields = curr_shields - (0.03*max_shields);
              curr_shields = curr_shields - (3.0 * getAtkBeamStr());

              ene_closest.getDamaged(getAtkBeamStr(), "attack beam", this, false);
              damage_dealt = damage_dealt + getAtkBeamStr();
              parentFaction.shiftFrontLine(this.getXYZ(), 0.2, this);
              //System.out.println("Ship " + unique_id + " used attack beam, dealt " + (int) getAtkBeamStr() + " damage at "+(int) getAttackBeamRange()+" range.");
              if (isInPlayerFaction()) {
                Missilemada2.createDebrisFlatSprite("attackbeam_ship_debris.png", 5.5*(0.50+Missilemada2.gimmeRandDouble()),
                        150.0*(1.0+Missilemada2.gimmeRandDouble()), 150.0*(1.0+Missilemada2.gimmeRandDouble()), this, true/*debris bearing from ship*/);
                playAtkBeamFiringNote();
              }
              if (isInPlayerFaction()) {
                Missilemada2.changeWorldTimeIncrement(-3); //slow down world, because combat with atkbeams.
              }
              beam_available_timestamp = Missilemada2.getWorldTime() + getAttackBeamRechargeSeconds();
              beam_atk_draw_counter_frames = (int) Math.round(0.82 * Missilemada2.getFPS()); //frames
            } else { //target out of reach, try beamatk enemy base.
              //xxxxxxxxxxxxxx

            }
          } //else no closest enemy.
        } else {
          //xxxx System.out.println("Ship "+ unique_id + " cannot use beam system, sensors are too damaged.");

        }

      } else {
        //beam is recharging
        //System.out.println("Ship "+ unique_id + " cannot use beam system, recharging.");

      }
    } else {
      //System.out.println("Ship "+ unique_id + " cannot use beam system, BS too damaged or doesn't have a shield generator...");
    }

    //----missile logic
    if (missilesystem_status > 0.3
        && max_buildcredits > 0.2/*some dont have missiles*/
        && curr_buildcredits > 0.02*max_buildcredits) {
      if (current_target != null) { //shoot at my target. may have gotten target from buddy, outside senrange.
        if (!current_target.isDestroyed()) {

          //if (curr_buildcredits > 0.95 * max_buildcredits) //xxhack to fix odd bug where they refuse to fight.
          //  requestFireMissile(current_target);

          //some ships might have long range misl, some short range.
          double dist_tg = calcDistanceMTMT(this, current_target);
          double threatlevel = 1.2; //default: puny enemy, fire fewer missiles.
          if (current_target.getBattleStr() > 0.4*this.getBattleStr()) //if bigger than 40% of me, serious fight
            threatlevel = 2.5;
          if (current_target.getBattleStr() > this.getBattleStr()) //if they stronger than me, fire_missile much more often.
            threatlevel = 4.5;


          if (isStarbase() || type.equals("AC")) { //logic fix for bases & AC, which have MASSIVE battlestr (400).
            threatlevel = 17.5;
            if (Missilemada2.gimmeRandDouble() < 0.1) {
              requestFireMissile(null, current_target); //xx a hack, double firing.
            }
            //xxsmall chance of randomizing the missile designs. The base crew have time on their hands.
            if (isStarbase() && Missilemada2.gimmeRandDouble() < 0.025) {
              misl_DNA = Missilemada2.getRandomDNA();
              misl_DNA = Missile.enforceMissileDNA_maxfuel(misl_DNA);
            }
          }

          if (spent_on_misl < 200.0) //if never have fired, shoot at foe regardless of MISSILES' RANGE. To get better range estimate.
            requestFireMissile(null/*xyz*/, current_target);



          if (dist_tg < getMyMissileDistKnown()
           && dist_tg > 1.02 * Missilemada2.getMissileCollisionRangeMin() ) { //don't fire a megavolley at a puny pls.
            //if I/we have already fired a bunch(at a puny), wait. OR foe is strong, fire.
            if (see_friend_mislcount < 25) { //few in flight, shoot more often
              if (Missilemada2.gimmeRandDouble() < (0.070*threatlevel))
                requestFireMissile(null/*xyz*/, current_target);
            } else { //many in flight, wait and see (don't waste missiles if enemy gon' die)
              //System.out.println(type + " "+unique_id+" waits to fire coz saw "+see_friend_mislcount+" ally missiles.");
              if (Missilemada2.gimmeRandDouble() < (0.025*threatlevel))
                requestFireMissile(null/*xyz*/, current_target);
            }

          } else { //too far or too near.
            //fire anyways if we have taken significant damage.
            if (this.getHullPerc() < 0.55) {
              //System.out.println(type + " "+unique_id+" _tries_ fire all misl coz has damaged hull. Deterrent.");
              requestFireMissile(null/*xyz*/, current_target);
            }

          }
        }
      } else if (buddy_mil != null) { //no currtarget, try the coords buddy gave for your missiles.
        if (buddy_mil.hasTarget()) {
          Ship budtar = buddy_mil.getTarget();
          if (calcDistanceMTMT(this, budtar) < 1.1*getMyMissileDistKnown() && !budtar.isDestroyed()) { //if buddy's target is super far, DON'T SPRAY AND PRAY. **gameplay issue
            if (curr_buildcredits > 0.5*max_buildcredits
                    && see_friend_mislcount < 15
                    && Missilemada2.gimmeRandDouble() < 0.3) {
              //System.out.println(type + " "+unique_id+" _tries_ to launch missile at buddy's target.");
              //debugVFXText(budtar.getXYZ(), "misl to buddy's target");
              requestFireMissile(null/*xyz*/, budtar);
            }
          } //else out of missile reach.
        } //else buddy has no target.
      } //else no milbuddy.
    } //end missile logic
  }
  private boolean hasTarget() {
    if (current_target != null) {
      if (current_target.isDestroyed()) {
        return false;
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
  public int/*how many missiles were launched*/ requestFireMissile(Vector tar_xyz, /* OR */ Ship targ) { //coord overrides Ship target.
    Missile m;
    //if got coords as input, ignore Ship param. For volley feature.
    if (tar_xyz != null) {
      if (!Missilemada2.areCoordsWithinPlayarea(tar_xyz))
        return 0;
      Asteroid tmp = new Asteroid(10.0, tar_xyz); //tmp object coz helper funcs deal only with MobileThings
      double beaXY = Missilemada2.calcBearingXY(this, tmp);
      double beaXZ = Missilemada2.calcBearingXZ(this, tmp);
      m = new Missile(misl_DNA, this, beaXY, beaXZ, 0.0, null); //0.0 was 0.7*getSpeedCurrent() /*ship's speed is misl starting spd*/
    } else { //Ship as input param, older code
      double beaXY = Missilemada2.calcBearingXY(this, targ);
      double beaXZ = Missilemada2.calcBearingXZ(this, targ);
      double dist_to_tg = calcDistanceMTMT(this, targ);

      //farther the target, the more spread we want.
      double vari_max = (0.022*3.14) / ((getMyMissileDistKnown()/4.0) / dist_to_tg); //gets smaller when dist diminishes.
      if (vari_max > 0.49) {
        vari_max = 0.49;
      }
      beaXY = gimmeBearingVariance(beaXY, vari_max);
      beaXZ = gimmeBearingVariance(beaXZ, vari_max);
      m = new Missile(misl_DNA, this, beaXY, beaXZ, 0.0, targ); //0.0 was 0.7*getSpeedCurrent() /*ship's speed is misl starting spd*/
    }
    misl_cost = m.getCost();
    if (curr_buildcredits > misl_cost) { //if can afford, fire.
      //misl get component speed from ship, along misl's new heading?
      m.setSpeed(xspeed, yspeed, zspeed); //initial speed and heading: SAME AS SHIP.
      m.reduceSpeed(0.85);

      misl_sensorrange = m.getSensorRange();
      misl_speed = m.max_speed;
      Missilemada2.addToMissileList(m);
      //System.out.println(""+ type+" "+ unique_id + " fired a missile of cost "+m.getCost()+" , evasion "+m.getDefenseBeamEvasion());
      curr_buildcredits = curr_buildcredits - misl_cost;
      this.addSpending(misl_cost);
      parentFaction.addMissileSpending(misl_cost);

      if (isInPlayerFaction()) {
        //if (Missilemada2.gimmeRandDouble() < 0.13)
        playMissileFiringNote(misl_cost);
        //Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStringShort() + " fired a missile of cost " + (int)m.getCost()+"" ,0);
      }
      if (isInPlayerFaction() && Missilemada2.gimmeRandDouble() < 0.4) {
        Missilemada2.changeWorldTimeIncrement(-1); //slow down world, because combat is happening.
      }
      return 1; //one missile was fired.
    } else { //can't afford, not enough buildcredits on board at the moment.
      double misl_per_hour = (buildcredits_gen_per_minute * 60.0) / misl_cost;
      if (curr_buildcredits > 0.85*max_buildcredits && Missilemada2.gimmeRandDouble() < 0.09) {
        System.out.println(type+" "+ unique_id + " is full bc but cannot afford misl cost "+m.getCost()
                +"cr. misl/hour is "+misl_per_hour+", bc/h is "+buildcredits_gen_per_minute*60+", storage="+max_buildcredits/m.getCost()+" missiles.");
      }
      return 0;
    }
  }
  private void decideBehaviourAndDesti() {
    if (type.equals("SENSAT")) //sensor satellite has no moves or attacks or decisions.
      return;

    if (type.equals("STARBASE")) { //OLD: base has no moves or decisions.
      //xxxxxx
      if (parentFaction.getDistanceBetwBaseAndFrontline() > 4000.0) {
        forceDestination(parentFaction.getFrontlineXYZ(), "BASE TO FL ALWAYS");
      } else {
        forceDestination(parentFaction.getFrontlineXYZ(), "BASE TO FL,close");
        //forceDestination(parentFaction.getScoutingCandidateSpot(), "BASE SCOUTSxxxxx");
      }
      return;
    }

    //----if crew dead, autopilot heads home.
    if (curr_crew_count < 1 && max_crew_count > 0) {
      forceDestination(parentFaction.getXYZ_starbase_safer_side(), "deadCrewAuto2base");
    }

    danger_meter = 0.0; //reset, so "not in battle" has zero dangermeter.

    if (forceddestination) {
      //if strict orders, or panic, then no new (peace or battle) destination choosing until arrive. the point of forceddestination.
      return;
    }

    //----EVERYONE: detect if we are in combat, maybe flee or advance or hold position or goto buddy.
    checkIfBuddiesDead();
    boolean buddy_in_battle = areMyBuddiesInCombat();
    if (see_enemy_count > 0 || see_enemy_mislcount > 2) { //got interrupted by enemy sighting, gotta decide new destination.
      if (!am_under_fire) { //on first bool flip, vfx.
        have_destination = false;
        am_under_fire = true;
        if (isInPlayerFaction()) {
          Missilemada2.addVfxOnMT(xcoord, ycoord + 9 * getRadius(), zcoord, "COMBATMODE", 19000, 1600.0, 0.6/*transp*/, null/*attach vfx to ship*/, "battlestart.png", 1.0, "");
        }
      } else {
        //ship was already in combat from earlier.
        //am_under_fire = true;
      }
      //CHANGED, tractormode = false; //xxgameplay: don't tractor while in battle.
      decideBattleMoveDestination();
      return;
    }
    if (buddy_in_battle) { //spammy on battlestart.png
      //if dist to mil bud is x and milbud in battle, do x
            //      if (buddy_mil != null) {
            //        if (calcDistanceMTMT(this, buddy_mil) < 0.9*sensor_range) {
            //          //xxxxxx ?
            //        }
            //        if (buddy_mil.isInCombat() && calcDistanceMTMT(this, buddy_mil) < 1.15 * sensors_status * sensor_range) {
            //          //try proper decision based on things our sensors see.
            //          if (isInPlayerFaction()) {
            //            Missilemada2.addVfxOnMT(xcoord, ycoord + 9 * getRadius(), zcoord, "COMBATMODE", 19000, 1600.0, 0.6/*transp*/, null/*attach vfx to ship*/, "milbuddy_causes_battlestart.png", 1.0, "");
            //          }
            //          decideBattleMoveDestination(); //xxxx ___blah, this "battleness" chains from buddy to buddy down the chain...___
            //          return;
            //        }
            //      }
      //if dist to civ bud(MINER) is x and civbud in battle, do x(rush to its aid)
      if (buddy_civilian != null) {
        if (calcDistanceMTMT(this, buddy_civilian) < 0.9*sensor_range) {
          if (buddy_civilian.getType().equals("MINER")) {
            if ((type.equals("SCOUT") || type.equals("DEFENDER") || type.equals("BEAMDRONE"))
                && !forceddestination){
              forceDestination(buddy_civilian.getXYZ(), "peacetime scout/def/bd to minerbuddy.");
              //am_under_fire = true;
              current_target = buddy_civilian.getTarget();
            }
          }
        }
      }
    }

    //----section begin: if didn't decideBattleMoveDestination()
      //not detect immediate danger, just 1-2 stray enemy missiles.

    //----if tractor mode, set destination: near the derelict.
    if (tractormode) {
      if (buddy_derelict != null) {
        //if too far from dere, give up on tractoring it.
        double dist_to_dere = calcDistanceMTMT(this, buddy_derelict);
        if (dist_to_dere > getRangeForSetDerebuddy()) {
          buddy_derelict.setTractorer(null); //un-claim it, in case we too far.
          buddy_derelict = null;
          clearDestination();
        } else { //ok continue tractoring.
          //_IF_ we can catch up to the dere, follow it.
          if (max_speed > 1.2*buddy_derelict.getSpeedCurrent()) {
            if (!isInCombat()) {

              if (dist_to_dere > 0.7*Missilemada2.getArrivedDistance())
                setDes(changeXYZ(buddy_derelict.getXYZ(), 0.0, 10.0, 10.0), "follow derelict");
              else
                setDes(changeXYZTowards(buddy_derelict.getXYZ(), parentFaction.getXYZ_starbase_safer_side(), 0.7*Missilemada2.getArrivedDistance()), "follow derelict");

              have_destination = true;
              return;
            } else { //else in combat, don't go to dere.
              if (see_enemy_mislcount < 4) { //if only few, go to dere.
                setDes(changeXYZ(buddy_derelict.getXYZ(), 0.0, 10.0, 10.0), "follow derelict B");
              }
            }
          } else {
            buddy_derelict.setTractorer(null); //un-claim it, in case we too far.
            buddy_derelict = null;
            clearDestination();
          }
        }
      } else {
        System.out.println("odd, "+ this.toStringShort() + " is in tractormode but no bud_dere. Canceling mode.");
        tractormode = false;
      }
    }
    //----if no battle, and derelict ship nearby, tractor it towards BASE.
    //in USESENSORS:   Vector nearbyDeadShipsList = Missilemada2.getShips_XYNearest(xcoord, ycoord, 1.05 * this.getTractorRange(), 4);        //go thru list, decide on a buddy_derelict.
    if (buddy_derelict != null && !tractormode) {
      if (canTractorThisDerelict(buddy_derelict)) {
        tractormode = true;
        //if (isInPlayerFaction())
        //  Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStringShort()+" begins tractoring a derelict "+buddy_derelict.getType() + ". Dere spd="+buddy_derelict.getSpeedCurrent());
      } else {
        //too heavy for us.
        if (isInPlayerFaction())
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + this.toStringShort() + " can't tractor a derelict " + buddy_derelict.getType() + ", too large.",0);
      }
    } //else nothing.

    //----scouts' behaviour, including aste-scan and aste-mine
    if (type.equals("SCOUT")) {
      String scoutmode = parentFaction.getMode("SCOUT");
      if (scoutmode.equals("BASE") /*&& !forceddestination */ && !isAtFactionBase()) {
        forceDestination(parentFaction.getXYZ_starbase_safer_side(), "scout stay home");
        return;
      }
      if ((scoutmode.equals("FLAG") || scoutmode.equals("FLAGLEFT") || scoutmode.equals("FLAGRIGHT")) && !forceddestination) {
        //yup, force frontline, player commanded so. Big trouble can cancel the force_desti however.
        if (isTooDamagedToObeyFrontline()) {
          forceDestination(parentFaction.getXYZ_starbase_safer_side(), "scout to base for repairs instead of frontline, hurt");
        } else {
          if (scoutmode.equals("FLAG"))
            forceDestination(parentFaction.getFrontlineXYZ_vary("CENTER"), "scout frontline, hp ok");
          if (scoutmode.equals("FLAGLEFT"))
            forceDestination(parentFaction.getFrontlineXYZ_vary("LEFT"), "scout frontlineLE, hp ok");
          if (scoutmode.equals("FLAGRIGHT"))
            forceDestination(parentFaction.getFrontlineXYZ_vary("RIGHT"), "scout frontlineRI, hp ok");
          //parentFaction.addScoutingDist(0.008 * this.sensor_range);
        }
        return;
      }
      if (!have_destination) {
        if (scoutmode.equals("MINERS")) { //mode: scouts protect miners; scouts do not scout or aste-mine.

          //xxx parentFaction.addScoutingDist(-0.0011 * this.sensor_range);

          //if already have a MINER buddy, don't change it
          if (buddy_civilian != null) {
            if (buddy_civilian.getType().equals("MINER")) {
              //dont change buddy
            } else { //wrong type buddy (TINYMINER), not worth guarding, get a manned MINER buddy.
              buddy_civilian = getMinerToProtect(); //can return null
            }
          } else {
            buddy_civilian = getMinerToProtect(); //can return null
          }
          if (buddy_civilian != null) {
            double dist_to_civbuddy = calcDistanceMTMT(this, buddy_civilian);
            if (!forceddestination //if found/had a miner type ship, set desti to it (precise spot, not just_near) (once combat starts, prob get closer to foes.)
                && dist_to_civbuddy > Missilemada2.getNearlyArrivedDistance() ) { //do no force desti if we are near escortee!
              Vector min_prot_desti = changeXYZTowards(buddy_civilian.getXYZPredicted(22000), parentFaction.getFrontlineXYZ(), 0.07*sensor_range); //err on side of hostile frontline.
              forceDestination(min_prot_desti, "scout escort miner");
              if (isInPlayerFaction() ) {
                //spammyyy  Missilemada2.addVfx2(current_destinationXYZ, "ESCORT", 30000, 2000.0, 0.3/*transp*/, "escorting_buddy.png", 1.0, "");
              }
              return;
            } else {
              //keep previously forced desti.
            }
          } else {
            //err
            setDes(parentFaction.getXYZ_starbase_safer_side(), "to base coz no miner to escort");
          }
          return; //end scoutmode==miner.
        }
        if ((scoutmode.equals("NEAR") || scoutmode.equals("FAR") ) && !forceddestination) { //regular, pre-modes logic. scout and possibly mine.
          if (cargo_full) { //rare case
            setDes(parentFaction.getStarbase().getDockingXYZ(), "to base, scout cargofull");

            return; //hope this dont break anything
          } else { //room in cargo hold, default scouting. FAR/NEAR effect is in Faction in this case.
            //parentFaction.addScoutingDist(0.008 * this.sensor_range);

            if (see_asteroid_count > 0 && closestAsteroid != null) { //ensures have closestAsteroid
              if (!Missilemada2.isAsteroidKnownToFaction(closestAsteroid, parentFaction)) { //if closest aste is unscanned, go to it.
                setDes(changeXYZ(closestAsteroid.getXYZ(), 0.8 * Missilemada2.getAsteroidScanningRange(), 0, 0), "sc to closeAs unscanned"); //X plus scanning range.

                return;
              } else { //closest is known, try some other within_my_sensors asteroid.

                //if closest has resource that faction lacks, MINE IT.
                String lack = parentFaction.getLackingResource();
                if (closestAsteroid.hasResource(lack)) {
                  forceDestination(closestAsteroid.getXYZ(), "sc to mine lacking");
                  return;
                }

                for (int i = 0; i < seenAsteroids_thistimetick.size(); i++) {
                  Asteroid as = (Asteroid) seenAsteroids_thistimetick.get(i);
                  if (Missilemada2.isAsteroidKnownToFaction(closestAsteroid, parentFaction)) {
                    //don't go there
                  } else {
                    //go to the unkn one that is in my sensors.
                    forceDestination(changeXYZ(as.getXYZ(), 0.20 * Missilemada2.getAsteroidScanningRange(), 0, 0), "sc to seen unscanned"); //X plus scanning range.
                    break;
                  }
                }
                if ((!have_destination) && (!forceddestination)) {
                  setDes(parentFaction.shipRequestsDestination(this, "sc asks, try non-closest aste. coz no desti"), "sc asks coz no desti");
                  if (current_destinationXYZ == null) {
                    //err
                    setDes(parentFaction.getXYZ_starbase_safer_side(), "sc asking failed, thus to base.");
                  } else {
                    have_destination = true;
                    //xxxx why scout still verysoon asks for another desti?
                    return;
                  }
                }
              }
            } else {
              //do not see any asteroids, semi-rare case?
              tryDeploySenSat();

              setDes(parentFaction.shipRequestsDestination(this, "I scout, dont see any aste, gimme a desti."), "sc99");
              if (current_destinationXYZ == null) {
                setDes(parentFaction.getXYZ_starbase_safer_side(), "sc99 ask fail; to base");
              } else {
                have_destination = true;
                return;
              }
            }
          }
/*
          //WHERE THIS BLOCK GO? if scout empty bays, and have taken some shield hits, go fetch more from starbase.
          if (type.equals("SCOUT")
                  && !have_destination
                  && curr_buildcredits < 0.2 * max_buildcredits
                  && getShieldPerc() < 0.9) {
            current_destinationXYZ = parentFaction.getXYZ();
            have_destination = true;
            forceddestination = true;
            debugVFXText(current_destinationXYZ, "sc2fetch_"+unique_id);
          }
*/

        }
      } else {
        //have a desti. keep it, unless battle starts.
      }
    } //end scout

    //----miners, when not in combat, usually head to base or chooseKnownAsteroidToMine().
    if ((type.equals("MINER") || type.equals("TINYMINER"))) {
      String minermode = parentFaction.getMode("MINER");
      if (minermode.equals("BASE")) {
        forceDestination(parentFaction.getXYZ_starbase_safer_side(), "miner stay home");
        return;
      } else { //else: old logic, active mining.
        if (cargo_full) {
          forceDestination(parentFaction.getXYZ_starbase_safer_side(), "miner to base for unloading");
          //would be illogical to do other than unload now. Except flee. But this func is peacetime vis-a-vis this ship.

          return;
        } else { //need to find some mining, OR we are at desti aste already.
          if (true  /*!have_destination*/
                  /*|| destinationAsteroid == null*/
                  ) { //xxxxxnewtry
            destinationAsteroid = null;
            Asteroid as = parentFaction.chooseKnownAsteroidToMine("", this);
            if (as != null) {
              setDes(as.getMiningXYZ(), "miner to wantedRes As");
              destinationAsteroid = as;

              return;
            } else { //no known good asteroids
              Asteroid asANY = parentFaction.chooseKnownAsteroidToMine("ANY", this);
              if (asANY != null) {
                setDes(asANY.getMiningXYZ(), "miner to ANY As");
                destinationAsteroid = asANY;
                //debugVFXText(current_destinationXYZ, "mi2ANYAste");
                return;
              } else {
                //we are pretty fucked, know no minable asteroid.
                if (closestAsteroid != null) {
                  if (closestAsteroid.isResourceless()) {

                  } else {
                    //xxxnew: goto closest since faction failed to choose for us.
                    setDes(closestAsteroid.getXYZ(), "miner to closest As, coz Fac didnt");
                    destinationAsteroid = closestAsteroid;
                  }
                }

                if (isInPlayerFaction()) {
                  //if (Missilemada2.gimmeRandDouble() < 0.02)
                  //  Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + type +" "+unique_id + ": faction not know where to mine.");
                }
                if (type.equals("MINER") && !have_destination) { //manned miner scouts
                  //current_destinationXYZ = POSSIBLY FAR FAR FAR RIGHT parentFaction.shipRequestsDestination(this, "MINER no has aste desti from faction, does scouting?");
                  setDes(Missilemada2.getRandomLocationNear(parentFaction.getXYZ_starbase_safer_side(), 0.1 * parentFaction.getScouting_distance_avg(), 0.1), "miner scouts near base");
                  destinationAsteroid = null;
                  if (isInPlayerFaction()) {
                    if (Missilemada2.gimmeRandDouble() < 0.2)
                      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() +" poor MINER "+unique_id + " has to resort to scouting :-( ",0);
                  }
                  return;
                }
                if (type.equals("TINYMINER") && !have_destination) { //unmanned miner scouts (or pouts?)
                  setDes(parentFaction.getXYZ_starbase_safer_side(), "tinyminer pouts");
                  // = parentFaction.shipRequestsDestination(this, "TINYMINER no has aste desti from faction, does X");
                  destinationAsteroid = null;
                  return;
                }
              }
            }
          } else {
            //have desti, keep it (which is base or wanted asteroid.)
          }
        }
      } //end GO-mode
    } //end miner

    //----MIL-ships, when not in combat:
    if ((type.equals("DEFENDER") || type.equals("AC") || type.equals("MISSILEDRONE") || type.equals("BEAMDRONE") )) {
      String milmode = parentFaction.getMode("MIL");

      //if missiledrone empty missile bays, go fetch more from starbase.
      if (type.equals("MISSILEDRONE") && !hasEnoughBC_misldrone() && !forceddestination) {
        Missilemada2.addToHUDMsgList(this.toStrTypeNName() + ": capacity "+getHowManyMislWhenFull()+" misl, need to fetch more from base.",0);
        forceDestination(parentFaction.getXYZ(), "misdrone fetch");
        debugVFXText(current_destinationXYZ, "misdr2fetch");
        return;
      }

      if (!have_destination) {
        if (milmode.equals("BASE") /*&& !forceddestination */ && !isAtFactionBase()) {
          forceDestination(parentFaction.getXYZ_starbase_safer_side(), "mil ordered to base");
          return;
        }
        if ((milmode.equals("FLAG") || milmode.equals("FLAGLEFT") || milmode.equals("FLAGRIGHT")) && !forceddestination) {
          if (isDrone()) { //drones can't go to FL alone / explore alone.
            if (buddy_mil == null) {
              //stay at base
              setDes(parentFaction.getXYZ_starbase_safer_side(), "mil drone stay at base coz no bud");
            } else {
              //follow milbuddy.
              setDes(predictTargetsPosition(buddy_mil, 6/*ticks*/), "mil drone follow milbud");
            }
            return;
          } else {
            //manned mil-ships
            //yup, force frontline, player commanded so. Big trouble can cancel the force_desti however.
            if (isTooDamagedToObeyFrontline()) {
              forceDestination(parentFaction.getXYZ_starbase_safer_side(), "mil to base for repairs instead of frontline, hurt");
            } else {
              if (see_friend_count >= 5) {
                //maybe... if (getBattleStr() > buddy_mil) then i take point? But buddy is stronger ship.
                if (milmode.equals("FLAG") )
                  forceDestination(parentFaction.getFrontlineXYZ_vary("CENTER"), "mil frontline3");
                if (milmode.equals("FLAGLEFT") )
                  forceDestination(parentFaction.getFrontlineXYZ_vary("LEFT"), "mil frontline3 LE");
                if (milmode.equals("FLAGRIGHT") )
                  forceDestination(parentFaction.getFrontlineXYZ_vary("RIGHT"), "mil frontline3 RI");
              } else { //don't wanna go alone-ish
                Vector spot = null;
                if (milmode.equals("FLAG") )
                  spot = parentFaction.getFrontlineXYZ_vary("CENTER");
                if (milmode.equals("FLAGLEFT") )
                  spot = parentFaction.getFrontlineXYZ_vary("LEFT");
                if (milmode.equals("FLAGRIGHT") )
                  spot = parentFaction.getFrontlineXYZ_vary("RIGHT");

                    if (see_friend_count <= 1) {
                      //refuse alone frontline. go warily.
                        Vector wary_des = MobileThing.changeXYZTowards(spot, parentFaction.getXYZ_starbase_safer_side(), 0.15*getSensorRange());
                      setDes(wary_des, "mil frontline alone");
                    } else { //2-4 friends near, forced
                      Vector wary_des2 = MobileThing.changeXYZTowards(spot, parentFaction.getXYZ_starbase_safer_side(), 0.08*getSensorRange());
                      forceDestination(wary_des2, "mil frontline2");
                    }
              }
            }
            return;
          }
        }
        if (milmode.equals("MINERS")) { //mode: mil escort miners
          //if already have a MINER buddy, don't change it
          if (buddy_civilian != null) {
            if (buddy_civilian.getType().equals("MINER")) {
              //dont change buddy
            } else { //wrong type buddy (TINYMINER), not worth guarding, get a manned MINER buddy.
              buddy_civilian = getMinerToProtect(); //can return null
            }
          } else {
            buddy_civilian = getMinerToProtect(); //can return null
          }
          if (buddy_civilian != null) {
            double dist_to_civbuddy = calcDistanceMTMT(this, buddy_civilian);

            if (!forceddestination //if found/had a miner type ship, set desti to it (precise spot, not just_near) (once combat starts, prob approach foes.)
                    && dist_to_civbuddy > Missilemada2.getNearlyArrivedDistance() ) { //do no force desti if we are near escortee!

              Vector min_prot_desti = changeXYZTowards(buddy_civilian.getXYZPredicted(39000), parentFaction.getFrontlineXYZ(), 0.09*sensor_range); //err on side of hostile frontline.
              forceDestination(min_prot_desti, "mil escort miner");

              if (isInPlayerFaction()) {
                //spammyyy  Missilemada2.addVfx2(current_destinationXYZ, "ESCORT", 35000, 4000.0, 0.3/*transp*/, "escorting_buddy.png", 1.0, "");
              }
            }
          } else {
            //no miner to protect, be at base.
            setDes(parentFaction.getXYZ_starbase_safer_side(), "mil: no miner to escort; to base.");
          }
        }

        if (milmode.equals("NEAR") || milmode.equals("FAR")) { //the old logic
          //drones can't explore, only BASE, MINERS.   For them NEAR,FAR,FLAG is "stick to milbuddy".
          if (isDrone()) {
            if (buddy_mil == null) {
              //stay at base
              setDes(Missilemada2.getRandomLocationNear(parentFaction.getXYZ(), 0.03 * Missilemada2.getBaseDistance(), 0.2), "drone stay,nobud");
              have_destination = true;
            } else {
              //follow milbuddy.
              setDes(Missilemada2.getRandomLocationNear(buddy_mil.getXYZ(), 0.03 * Missilemada2.getBaseDistance(), 0.2), "drone to bud");
              have_destination = true;
            }
            return;
          } else { //DEF & AC
            //OLD: setDes(parentFaction.shipRequestsDestination(this, "mil-ship wants peacetime scouting destination"), "mil ask peacetime");
            Vector spot = parentFaction.getScoutingSpotMIL(milmode/*near or far*/, this/*for senrange*/);
            if (spot == null) {
              //err
              System.out.println("MIL-ship "+ this.toStringShort() + " got peacetime null getScoutingSpotMIL()");
              have_destination = false;
            } else {
              setDes(spot, "DEF or AC got peacetime scouting desti");
              //tryDeploySenSat();
              //not: return;
            }
          }

        }
      } else {
        //have desti, keep it.
      }
    }

    //------------------------------------------------------------------------------------------------------------------
    if (did_mining) {
      //continue mining after did first high-speed mining pass.
      //keep destination.
      if (closestAsteroid != null) {
        setDes(closestAsteroid.getXYZ(), "since mined, to closest As.");
      }
      reduceSpeed(0.99);
    } else { //non-mining non-combat turn. ask for destination if not have one.
      if (see_enemy_count < 1) {
        if (!have_destination) {
          //xxxxxxx    System.out.println("Ship " + toStringShort() + " last chance to ask-ask ");
          if (isStarbase()) {
            setDes(getXYZ(), "bleh hotfix for base destination");
          }
          setDes(parentFaction.shipRequestsDestination(this, "last chance to ask, I didn't have desti."), "flarg98");
          if (current_destinationXYZ == null) {
            //err
            have_destination = false;
            if (isInPlayerFaction()) {
              Missilemada2.addVfx2(getXYZ(), "NODESTIFROMFAC", 4500, 1000.0, 0.2/*transp*/, "qm_red.png", 1.0, "");
              System.out.println("debug: Ship "+ toStringShort()+ " got nodesti, err state"); //was starbase.

            }

          } else {
            have_destination = true;
            //System.out.println("Ship "+ unique_id + " got desti, "+calcDistanceVecVec(current_destinationXYZ, parentFaction.getXYZ())+" away from base");
            if (isInPlayerFaction()) {
              //Missilemada2.addVfx2(xcoord+radius, ycoord+radius, zcoord + 550.0, "GOTDESTIFROMFAC", 900, 110.0, null, "32texture_gotdest.png", 1.0);
              Missilemada2.addVfx2(current_destinationXYZ, "DESTINATION", 4500, 1000.0, 0.2/*transp*/, "destination.png", 1.0, "");
            }
          }
        } else {
          // else keep desti.
        }
      } else {
        //did the combat logic already earlier.
      }
    }
  }
  private int getHowManyMislWhenFull() {
    //assume always know misl_cost.
    return (int) Math.floor(max_buildcredits / misl_cost);
  }
  private boolean hasEnoughBC_misldrone() {
    //old missiledrone: curr_buildcredits < 0.3/*hard to guess threshold*/ * max_buildcredits
    if (misl_cost > 10.0) {
      if (curr_buildcredits > 3*misl_cost) {
        return true;
      } else {
        return false;
      }
    } else {
      //unkn mislcost
      return true;
    }
  }
  private void decideBattleMoveDestination() {//flee or fight? set destination. // Always use full power of weapons if foe in range.(in other function)
    dodge_mode = false;

    //if scout or defender, and in heavy battle, request a volley.
    if ( (isScout() || getType().equals("DEFENDER"))
            && see_enemy_mislcount > 8) {
      if (Missilemada2.gimmeRandDouble() < 0.006
              && parentFaction != Missilemada2.getPlayerFaction()) { //AI only.
        parentFaction.volleyTowardsCoords( changeXYZTowards(parentFaction.getFrontlineXYZ_vary("CENTER"), this.getXYZ(), 0.04 * Missilemada2.getBaseDistance()) );
      }
    }


    //maybe surrender-defect, if overwhelming foe str and we are damaged. Then enemy missiles will ummmmm ignore us.
    if (getHullPerc() < 0.33
            && see_total_enemy_battlestr > 1.5*see_total_friend_battlestr
            && curr_crew_count > 0
            && !isStarbase()
            && Missilemada2.gimmeRandInt(100) < 25) { //must have crew alive for surrender chance.
      //surrender! but still might die of engine fire. Or from previous-parentfaction attacks.
      //also, battlestr of factions changes as a result of this defecting, so, more defecting may chain.
      this.surrender_or_got_captured(ene_closest /*surrender-to*/, false/*not hacked*/);
    }

    //calc fuzzy logic danger.
    danger_meter = 0.3*see_enemy_count + 0.11*see_enemy_mislcount - 0.11*see_friend_count;
    //increasings:
    if (see_enemy_count > (see_friend_count+2))
      danger_meter = danger_meter + 0.6;
    if (see_total_enemy_battlestr > (0.8*see_total_friend_battlestr)) //xx usually don't see foes, if I am puny
      danger_meter = danger_meter + 0.6;

    if (see_enemy_mislcount > (0.7*see_friend_mislcount))
      danger_meter = danger_meter + 0.6;
    if (see_enemy_mislcount > (1.25*see_friend_mislcount))
      danger_meter = danger_meter + 1.0;
    if (see_enemy_mislcount > (1.2*see_friend_mislcount))
      danger_meter = danger_meter + 1.6;
    if (see_enemy_mislcount_close*(0.8*Missilemada2.getAvgMislYield()) > 0.2*curr_hull_hp) //if fatal amt of missiles near, argle.
      danger_meter = danger_meter + 2.8;
    if (see_enemy_mislcount*(Missilemada2.getAvgMislYield()) > 0.9*max_shields)
      danger_meter = danger_meter + 0.9;

    if (see_friend_count < 1 || buddy_mil == null) //if am stupidly alone.
      danger_meter = danger_meter + 1.7;

    if (getShieldPerc() < 0.6)
      danger_meter = danger_meter + 1.0;
    if (getHullPerc() < 0.7)
      danger_meter = danger_meter + 1.0;
    if (engine_status < 0.7)
      danger_meter = danger_meter + 0.7;
    if (getSystemsStatusAvg() < 0.86)
      danger_meter = danger_meter + 1.2;
    if (getSystemsStatusAvg() < 0.66)
      danger_meter = danger_meter + 1.2;

    if (parentFaction.getStarbase() == null) //if base gone, be more timid and flighty
      danger_meter = danger_meter + 1.5;

    //then the danger level reducings

    if (buddy_mil != null) {
      if (buddy_mil.getBattleStr() > 1.3*this.getBattleStr())
        danger_meter = danger_meter - 0.5; //strong buddy boosts confidence.
    }
    if (ene_closest != null) {
      if (2.0 * personality_aggro * this.getBattleStr() > 1.1 * ene_closest.getBattleStr())
        danger_meter = danger_meter - 0.7;
    }
    if (type.equals("DEFENDER"))
      danger_meter = danger_meter - 0.7;
    if (type.equals("AC"))
      danger_meter = danger_meter - 2.2; //pishposh
    if (see_friend_count > see_enemy_count)
      danger_meter = danger_meter - 0.2;
    if (see_total_enemy_battlestr < see_total_friend_battlestr)
      danger_meter = danger_meter - 0.4;
    if (getShieldPerc() > 0.9)
      danger_meter = danger_meter - 0.5;
    if (2.0 * personality_aggro * see_total_friend_battlestr > 1.2 * see_total_enemy_battlestr)
      danger_meter = danger_meter - 0.5;

    //if (see_enemy_count > 0)
    //xxxxxx  System.out.println(type+" "+unique_id+" sees "+see_enemy_count+" foes, dang*100 = " + (int)(100.0*danger_meter));



    //-----new logic from faction commands...
    //remember, we are in combat! //BASE MINERS NEAR FAR FLAG. miners: BASE, GO.
    if (isScout()) {
      String scoutmode = parentFaction.getMode("SCOUT");
      if (scoutmode.equals("BASE") /*&& !forceddestination */ && !isAtFactionBase()) {
        forceDestination(parentFaction.getXYZ_starbase_safer_side(), "scout stay home");
        return;
      }
      if (scoutmode.equals("MINERS")) {
        //scouts protect miners
        checkIfBuddiesDead();

        //if already have a MINER buddy, don't change it
        if (buddy_civilian != null) {
          if (buddy_civilian.getType().equals("MINER")) {
            //dont change buddy
          } else {
            buddy_civilian = getMinerToProtect(); //can return null
          }
        } else {
          buddy_civilian = getMinerToProtect(); //can return null
        }
        if (buddy_civilian != null) { //if found/had a miner type ship, set desti to it (precise spot, not just_near)
          Vector min_prot_desti = changeXYZTowards(buddy_civilian.getXYZPredicted(17000), parentFaction.getFrontlineXYZ(), 0.04 * Missilemada2.getBaseDistance()); //err on side of hostile frontline.
          forceDestination(min_prot_desti, "scout protect miner");
          return;
        }
      }
      if (scoutmode.equals("NEAR") && !forceddestination) {
        //do scout near base AND ARE IN COMBAT. (pls not so close to frontline)
        //NEAR is the less-courage mode.

        if (current_destinationXYZ != null) { // if have desti, shift it nearer
          // if in combat and far from base, umm go closer to base.
          if ( getDestinationsDistanceFromBase() > 0.4*parentFaction.getScouting_distance_avg()) {
            //changes every time-tick until CURRDESTI close enough.
            setDes(changeXYZTowards(current_destinationXYZ, parentFaction.getXYZ(), 6500.0), "scout shift closer t base");
          }
          if (getDestinationsDistanceFromFrontline() < 0.5* getMyMissileDistKnown()) { //if close to frontline, do shift desti closer to base.
            //sub-case where frontline and base are very near
            if (parentFaction.getDistanceBetwBaseAndFrontline() < 0.15*Missilemada2.getBaseDistance()) {
              //THE SAME "closer to base".
              setDes(changeXYZTowards(current_destinationXYZ, parentFaction.getXYZ(), 6500.0), "scout shift closer tobase");
            } else { //base and frontline are quite different spots.
              //NEAR means away from frontline
              //changes every time-tick until CURRDESTI close enough or frontline has shifted
              setDes(changeXYZTowards(current_destinationXYZ, parentFaction.getXYZ(), 6500.0), "scout shift closer tobase97");
            }
          }
        } else { //no curr desti, x
          setDes(parentFaction.getScoutingCandidateSpot(), "sc2candispot");
        }
      }
      if (scoutmode.equals("FAR") && !forceddestination) {
        //do scout far from base AND ARE IN COMBAT. the old logic.
        if (see_enemy_mislcount > 10) { //optional: one misl must be close to me.
          setDes(parentFaction.getXYZ_starbase_safer_side(), "scout fall back to base on FARscout and foe 10 misl seen");
          return;
        }

        if (current_destinationXYZ != null) { // if have desti, shift it nearer
          // if in combat and far from base, umm go closer to base.
          if ( getDestinationsDistanceFromBase() < 0.4*parentFaction.getScouting_distance_avg()) {
            //changes every time-tick until CURRDESTI is FAR enough.
            setDes(changeXYZTowards(current_destinationXYZ, parentFaction.getScoutingCandidateSpot(), 6500.0), "scout shift from cur to candi");//xxxxxxx?verifylogic
          }
        } else { //no curr desti, x
          setDes(parentFaction.getScoutingCandidateSpot(), "scout FAR: to candi"); //xx might get null, is okay
        }
      }
      if ((scoutmode.equals("FLAG") || scoutmode.equals("FLAGLEFT") || scoutmode.equals("FLAGRIGHT")) && !forceddestination) {
        //yup, force frontline, player commanded so. Big trouble can cancel the force_desti however.
        if (scoutmode.equals("FLAG") )
          forceDestination(parentFaction.getFrontlineXYZ_vary("CENTER"), "sco frontline");
        if (scoutmode.equals("FLAGLEFT") )
          forceDestination(parentFaction.getFrontlineXYZ_vary("LEFT"), "sco frontline LE");
        if (scoutmode.equals("FLAGRIGHT") )
          forceDestination(parentFaction.getFrontlineXYZ_vary("RIGHT"), "sco frontline RI");
        return;
      }
    }

    if (isMil()) {
      String milmode = parentFaction.getMode("MIL");
      if (milmode.equals("BASE") /*&& !forceddestination */ && !isAtFactionBase()) {
        forceDestination(parentFaction.getXYZ_starbase_safer_side(), "mil stay home");
        return;
      }
      if (milmode.equals("MINERS")) {
        //mil protect miners

        //if already have a MINER buddy, don't change it
        if (buddy_civilian != null) {
          if (buddy_civilian.getType().equals("MINER")) {
            //dont change buddy
          } else {
            buddy_civilian = getMinerToProtect(); //can return null
          }
        } else {
          buddy_civilian = getMinerToProtect(); //can return null
        }
        if (buddy_civilian != null) { //if found/had a miner type ship, set desti to it (precise spot, not just_near)
          Vector min_prot_desti = changeXYZTowards(buddy_civilian.getXYZPredicted(17000), parentFaction.getFrontlineXYZ(), 0.09*sensor_range); //err on side of hostile frontline.
          forceDestination(min_prot_desti, "mil protect miner");
          return;
        }
      }

   /*   if (mo.equals("NEAR") && !forceddestination) {
        //do scout near base AND ARE IN COMBAT. (pls not so close to frontline)
        //NEAR is the less-courage mode.

        // if in combat and far from base, umm go closer to base.
        if ( getDestinationsDistanceFromBase() > 0.4*parentFaction.getScouting_distance_avg()) {
          //changes every time-tick until CURRDESTI close enough.
          scurrent_destinationXYZ = changeXYZTowards(current_destinationXYZ, parentFaction.getXYZ(), 6500.0);
          have_destination = true;
        }
        if (getDestinationsDistanceFromFrontline() < 0.5* getMyMissileDistKnown()) { //if close to frontline, do shift desti closer to base.
          //sub-case where frontline and base are very near
          if (parentFaction.getDistanceBetwBaseAndFrontline() < 0.15*Missilemada2.getBaseDistance()) {
            //THE SAME "closer to base".
            scurrent_destinationXYZ = changeXYZTowards(current_destinationXYZ, parentFaction.getXYZ(), 6500.0);
            have_destination = true;
          } else { //base and frontline are quite different spots.
            //NEAR means away from frontline
            //changes every time-tick until CURRDESTI close enough or frontline has shifted
            scurrent_destinationXYZ = changeXYZTowards(current_destinationXYZ, parentFaction.getXYZ(), 6500.0);
            have_destination = true;
          }
        }
      }
      if (mo.equals("FAR") && !forceddestination) {
        //do scout far from base AND ARE IN COMBAT. the old logic.


        // if in combat and far from base, umm go closer to base.
        if ( getDestinationsDistanceFromBase() < 0.4*parentFaction.getScouting_distance_avg()) {
          //changes every time-tick until CURRDESTI is FAR enough.
          scurrent_destinationXYZ = changeXYZTowards(current_destinationXYZ, parentFaction.getScoutingCandidateSpot(), 6500.0);
          have_destination = true;
        }
      }
*/
      if (milmode.equals("NEAR") && !forceddestination) {
        //do patrol near base AND ARE IN COMBAT. (pls not so close to frontline)
        // if far from base and combat, umm go closer to base.
        if ( getDistanceFromBase() > 0.4*parentFaction.getScouting_distance_avg()) {
          //parentFaction.shipRequestsDestination()
          //xxxxxxxxx


        }
        if (getDistanceFromFrontline() < getMyMissileDistKnown()) { //if close to frontline, do X.
          //xxxxxxxxx

          //xxx sub-case where FL and base are very near

        }

      }
      if (milmode.equals("FAR") && !forceddestination) {
        //do scout far from base AND ARE IN COMBAT. the old logic.
        if (current_destinationXYZ != null) { // if have desti, shift it nearer




        } else {



        }
      }
      if ((milmode.equals("FLAG") || milmode.equals("FLAGLEFT") || milmode.equals("FLAGRIGHT")) && !forceddestination) {
        //yup, force frontline, player commanded so. Big trouble can cancel the force_desti however.
            Vector spot = null;
            if (milmode.equals("FLAG") )
              spot = parentFaction.getFrontlineXYZ_vary("CENTER");
            if (milmode.equals("FLAGLEFT") )
              spot = parentFaction.getFrontlineXYZ_vary("LEFT");
            if (milmode.equals("FLAGRIGHT") )
              spot = parentFaction.getFrontlineXYZ_vary("RIGHT");
        if (haveShieldSystem()) {
          if (getShieldPerc() > 0.5 && getMissileStorePerc() > 0.15) {
            forceDestination(spot, "combat: mil stay at very frontline");
            return;
          } else { //fall back coz low shields
            Vector spotsafer = MobileThing.changeXYZTowards(spot, parentFaction.getXYZ(), 0.2*sensor_range);
            forceDestination(spotsafer, "combat: mil stay at rear of frontline coz need to recharge");
            return;
          }
        } else {
          //no shield capability, am cheapo or missiledrone
          if (getHullPerc() > 0.6) {
            Vector spotsafer = MobileThing.changeXYZTowards(spot, parentFaction.getXYZ(), 0.1*sensor_range);
            forceDestination(spotsafer, "combat: mil stay at rear of frontline coz owns no shields");
            return;
          } else {
            //low HP, go repair
            forceDestination(parentFaction.getXYZ_starbase_safer_side(), "low hull, owns no shields, go repair from frontline");
            return;
          }
        }
      }
    }
    if (isMiner()) {
      //xx we might be here coz buddy in battle, foes veeery far.
      String mo = parentFaction.getMode("MINER");
      if (mo.equals("BASE") && !forceddestination && !isAtFactionBase()) {
        forceDestination(parentFaction.getXYZ_starbase_safer_side(), "miners in base mode");
        return;
      }
      //xxx might go slightly away from frontlineflag-n-base combo?
      //else the normal miner-in-combat activity, old logic, miners decide for themselves when to flee.
//      if (danger_meter > 0.8) {
//        parentFaction.forgetAsteroidUNUSED(destinationAsteroid); //xx or closest? //xxxx causes MUCH MERIT GET.
//        clearDestination();
//      }
    }

    //----destination choices. if forced order from faction(or panic, or other), don't try new destination.
    if (forceddestination) {
      // keep desti
    } else { //regular combatdecisionmaking.
      //default: prefer to be in a mil-ship formation.
      if (isMiner()) {
        might_stay_in_formation_OBSO = false;
      } else { //mil type
        might_stay_in_formation_OBSO = true;
      }
      if (getShieldPerc() < 0.5
              || getHullPerc() < 0.8
              || see_enemy_count > (see_friend_count+2)
              || see_total_enemy_battlestr > (1.6*see_total_friend_battlestr)
              || see_enemy_mislcount > (1.2*see_friend_mislcount)
              ) {
        might_stay_in_formation_OBSO = false;
      }

      //xxxxxx move block elsewhere?
      //if have target and it's a starbase, reset destination... to next of it.
      if (current_target != null) {
        if (current_target.getType().equals("STARBASE") /*&& this.isNearDestination()*/ ) {
          setDes(current_target.getBattleDestiXYZ(), "target is a base, getBattleDesti");
        }
      }


      //if overwhelming missiles incoming, try to dodge(hopeless unless you're very fast).
      if (see_enemy_mislcount_close*(0.8*Missilemada2.getAvgMislYield()) > 0.35*curr_hull_hp) {
        setDes(decideEvadeLocation(tryShootDownMissile, ene_closest), "evadelocation"); //xxx
        dodge_mode = true;
        if (current_destinationXYZ != null) {
          have_destination = true;
        } else { //no two things to calc evade from, ie. __foes outside sensor range__.
          if (buddy_mil != null) {
            //current_destinationXYZ = buddy_mil.getXYZ();
            //have_destination = true;
          } else { //no buddy
            if (ene_closest == null) {

            } else {
              setDes(parentFaction.getSafeLocation(this, ene_closest/*if null, goes to base*/), "getsafelocation vis-a-vis closest foe");
            }
          }
        }
      }

      //if hurt, go home to repair. NOT FORCED!
      if (engine_status < 0.6
              || shield_status < 0.6
              || missilesystem_status < 0.6
              || lifesupport_status < 0.4
              || sensors_status < 0.8
              || getHullPerc() < 0.8) { //was: || see_enemy_mislcount > 1.2*see_friend_mislcount
        if (Missilemada2.gimmeRandDouble() < 0.01) { //don't spam the "ow ow ow"
          if (isInPlayerFaction()) {
            Missilemada2.addVfxOnMT(getX(), getY() - 10.0 * radius, getZ() + 4.0 * radius, "GONEREPAIRING", 9000, 880.0, 0.5/*transp*/, null, "going_to_repair.png", 1.0, "");
            //xx spammyMissilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + " "+getType()+" "+getId()+", cost "+(int)getCost()+", (str "+getBattleStrIntDisp()+") heading to repair at base.");
          }
        }
        //to base or random spot
        if (parentFaction.getStarbase() != null)
          setDes(parentFaction.getXYZ_starbase_safer_side(), "hurt, to base");
        else
          setDes(Missilemada2.getRandomAsteroid().getMiningXYZ(), "hurt, base dead, to random aste");

      }
      //if panic, forced go home, "rotate crew to less scared crew over there"[no code for such].
      if (getShieldPerc() < 0.4 && Missilemada2.gimmeRandDouble() < 0.015 && see_enemy_mislcount > (see_friend_mislcount+3)) {
        if (parentFaction.getStarbase() != null)
          forceDestination(parentFaction.getXYZ_starbase_safer_side(), "veryhurt, panic; to base"); //note: not docking spot, but safer closer spot (underneath/behind).
        else
          forceDestination(Missilemada2.getRandomAsteroid().getMiningXYZ(), "hurt, base dead, to random aste");

        if (Missilemada2.gimmeRandDouble() < 0.04) {
          if (isInPlayerFaction()) {
            Missilemada2.addVfxOnMT(-20.0 * radius, -10.0 * radius, 4.0 * radius, "PANIC", 1000, 880.0, 1.0/*transp*/, this, "sweatdrop.png", 1.0, "");
          }
        }
      }

      //----decide distance to foe, or flee overwhelming force.
      //----danger -4.9 is very confident, -2.5 is, +1 is one foe?, 1.08 is lots

        if (danger_meter < -3.0) { //very safe
          if (isInPlayerFaction()) {
            Missilemada2.addVfxOnMT(-20.0 * radius, -10.0 * radius, 4.0 * radius, "CONFIDENT", 1000, 880.0, 1.0/*transp*/, this, "ship_confidence_symbol.png", 1.0, "");
            //xxxxxx no idea what was to be changed here
          }


          //go closer
          //scurrent_destinationXYZ = current_target.getBattleDestiXYZ();
          if (current_target != null && !isMiner()) {
            setDes(getBattleDestiForAtkBeam(current_target), "danger:v.safe; to atk beam range.");
          } else { //in battle, am very safe, have no target. go to milbuddy.
            if (buddy_mil != null && !isMiner()) {
              setDes(getBattleDestiForAtkBeam(buddy_mil), "danger:v.safe; to buddytarget, atk beam range.");
            }
          }
        }
        if (danger_meter > -3.0 && danger_meter < -1.0) { //semi-safe
          //go closer
          if (current_target != null && !isMiner()) {
            setDes(current_target.getBattleDestiFAR_XYZ(current_target), "danger:semisafe; battledestifar()");
          } //else: in battle, am  safe, have no target. stay put.
        }

      if (danger_meter > -1.0 && danger_meter < 0.4) { //medium. 0.0 when far far from action.
        //stay put or go to buddy.
        if (buddy_mil != null && !isMiner() && am_under_fire && see_enemy_count > 0) {
          setDes(buddy_mil.getXYZ(), "danger:medium and see N foes; to buddy.");
        } else {
          //stay, THEREFORE NO DESTI CHANGE.
                //setDes(getXYZ(), "danger:medium; stay here"); //YYY fuck, this probably caused "fuck off to far far right, coz battlestart from buddy."
                //setDes(parentFaction.shipRequestsDestination(this, "medium danger, req"), "med_danger, faction_req");
                //setDes(parentFaction.getXYZ_starbase_safer_side(), "danger: medi; fall back to base");
        }
      }
      double offset = 2.2*Missilemada2.getMissileCollisionRangeMin();

      if (danger_meter > 0.4 && danger_meter < 1.9) { //danger
        //flee
        if (isSeenByPlayer() && Missilemada2.gimmeRandDouble() < 0.04) {
          Missilemada2.addVfxOnMT(offset, 10.0 * radius, 4.0 * radius, "DANGER1", 3000, 880.0, 0.6/*transp*/, this, "sweatdrop.png", 1.0, "");
        }
        forceddestination = false; //clear possible frontline-command
        setDes(parentFaction.getXYZ_starbase_safer_side(), "danger:d; fall back to base"); //parentFaction.getSafeLocation(this, /* from enemy*/ ene_closest);
        //not forceddestination, reinforcements may arrive any minute.
      }
          if (danger_meter > 0.8) {
            if (type.equals("AC") && getShieldPerc() < 0.38) { //ok the shit hit the fan, deploy AC's drones. (beamdrones help shoot down missiles)
              tryDeployCombatDrones();
            }
          }
      if (danger_meter > 1.9) { //high danger, go to base.
        //doubleflee
        if (isSeenByPlayer() && Missilemada2.gimmeRandDouble() < 0.04) {
          Missilemada2.addVfxOnMT(-2.2 * offset, 10.0 * radius, 4.0 * radius, "DANGER2", 3000, 980.0, 0.6/*transp*/, this, "sweatdrop.png", 1.0, "");
          Missilemada2.addVfxOnMT(-offset, 10.0 * radius, 4.0 * radius, "DANGER2", 3000, 980.0, 0.6/*transp*/, this, "sweatdrop.png", 1.0, "");
        }
        setDes(parentFaction.getXYZ_starbase_safer_side(), "danger:hi; to base");
      }

      //debug "dang number: very spammy.
      //if (Missilemada2.gimmeRandDouble() < 0.02)
      //  Missilemada2.addVfx2(getXYZ(), "TEXT", 35900, 200.0, 0.6/*transp*/, "", 1.0, (int)(10*danger_meter)+" dang");

      if (danger_meter > 4.0) { //extreme (4-8) danger, force halfway to base.
        double dist_to_base = calcDistanceVecVec(getXYZ(), parentFaction.getXYZ_starbase_safer_side());
        forceDestination(changeXYZTowards(getXYZ(), parentFaction.getXYZ_starbase_safer_side(), 0.55*dist_to_base), "danger:extr; force halfway to base");
        return; //only on max.danger case, not sooner.
      }
    // formation logic may override earlier logic, because are so healthy.
      double maxdi = this.getMyMissileDistKnown();
      double mindi = 0.5*this.getDefenseBeamRange() /*close enough to defend buddy*/;
      if (maxdi < 2.0*Missilemada2.getCombatDistMin_Generic())
        maxdi = 2.0*Missilemada2.getCombatDistMin_Generic();
      if (mindi < Missilemada2.getSensorRangeMinMissile())
        mindi = Missilemada2.getSensorRangeMinMissile();

//JUST BAD SPIRALING, FOLLOW FAR EAST...
//    if (might_stay_in_formation_OBSO && buddy_mil != null) {
//      if (buddy_mil.getBattleStr() > 1.001*this.getBattleStr()) { //follow a stronger one, to prevent cycle of following.
//
//      scurrent_destinationXYZ = MobileThing.calcRelativeStanceXYZ(this.getXYZ(), buddy_mil.getXYZ(),
//              maxdi, mindi,
//              this.getStanceLR(), 0.40/*not near buddy*/);
//      have_destination = true;
//      if (false /*Missilemada2.gimmeRandDouble() < 0.05*/) {
//        debugVFXText(getXYZ(), "me"+unique_id);
//        debugVFXText(buddy_mil.getXYZ(), "bu"+unique_id);
//        debugVFXText(current_destinationXYZ, "B_S"+unique_id);
//      }
//      //System.out.println("Using formation logic, buddy is "+buddy_mil.getId()+", I am "+toString());
//      }
//    }


    } //end not-forced mode aka regular decision mode.
  }
  private void surrender_or_got_captured(Ship surr_to, boolean hacked) {
    if (isDestroyed())
      return;
    if (surr_to.isDestroyed())
      return;

    //maybe: if has defected once, can't defect again. BUTTTTT they can change the crew at base, so it's not the ships's fault.
    try {
      System.out.println("SURRENDERED OR HACK-CAPTURED: " + toString()); //hell of a way to lose your best DEFENDER...
      parentFaction.addScoutReportEnemyShip(this, new ScoutReport(Missilemada2.getWorldTime(), this)); //keep turncoat ship visible for a while
      if (isInPlayerFaction()) {
        Missilemada2.addVfxOnMT(0, 0, 0, "SHIPBUILT", 20000, 6500.0, 0.8/*transp*/, this, "yellowcross.png", 1.0, "");
      }

      Faction surrender_to_fac = null;
      if (surr_to != null)
        surrender_to_fac = surr_to.getFaction(); //xxxx might be wrong faction... but let us allow them to make that error in panic???
      else
        surrender_to_fac = Missilemada2.getRandomFaction();//buggy?null

      String reason = "surrendered";
      if (hacked)
        reason = "hacked";
      parentFaction.shipCountDown(this, reason); //faction A lost a ship.
      parentFaction = surrender_to_fac; //change allegiance
      parentFaction.shipCountUp(this, reason);//faction B gained a ship. xxxdid they gain crew? no.
      parentFaction.addXPToCommander(this, "SURRENDERED"/*includes hacked*/);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  private Vector getBattleDestiForAtkBeam(Ship ene) {
    Vector eneloc = ene.getXYZ(); //default: stand and fight.
    //double dist_to_ene = calcDistanceMTMT(this, ene);
    return changeXYZTowards(eneloc, this.getXYZ(), 0.8 * getAttackBeamRange());
  }
  private Vector getBattleDestiXYZ() {
    double dist = 0.2*Missilemada2.getCombatDistMin(this);
    double randsign = Math.signum((Missilemada2.gimmeRandDouble() - 0.5));
    return changeXYZ(getXYZ(), randsign * dist, randsign * dist, randsign * dist);
  }
  private Vector getBattleDestiFAR_XYZ(Ship ene) {
    if (parentFaction == null)
      return null;

    Vector v = getXYZ(); //default: stand and fight.
    double dist_to_ene = calcDistanceMTMT(this, ene);

    //if ene too far for combat
    if (dist_to_ene > Missilemada2.getCombatDistMax(this)) {
      //xxxx closer desti
      v = ene.getBattleDestiXYZ();

    } else if (dist_to_ene < Missilemada2.getCombatDistMin(this)
            || dist_to_ene < 0.6*Missilemada2.getCombatDistMax(this)) { //if too close, go homeward
      v = ene.getBattleDestiXYZ();
      //shift towards home base
      v = changeXYZTowards(v, parentFaction.getXYZ(), 15.5 * Missilemada2.getCombatDistMin(this));

    } else { //not too far, not too close, have okay range ATM
      // new spot near self.
      //v = Missilemada2.getRandomLocationNear(this.getXYZ(), sensor_range / 0.3, 0.2);
      if (see_enemy_mislcount > see_friend_mislcount) {
        //move coz so many foe missiles
        v = changeXYZTowards(v, parentFaction.getXYZ(), 20.5 * Missilemada2.getCombatDistMin(this));
      } else {
        //stand and fight
        v = getXYZ();
      }
    }
    return v;
  }
  public double getMaxBattleStr() {
    return max_battle_str;
  }
  public double getBattleStr() { //varies as get systems damage and hull damage
    double part1 = (beamsystem_status) * 350.0 * getAtkBeamStr()
            + (0.3+missilesystem_status) * 24500.0 * (buildcredits_gen_per_minute)
            + (0.3+missilesystem_status) * 18500.0 * (max_buildcredits /*201 ON MISDRONE I GUESS*/)
            + shield_status * (44.0 * (0.1+shield_regen_per_min)   +   (0.1+max_shields)/150.0)
            + beamsystem_status * defense_beam_accuracy/*0..1*/ * 12500500.0
            + 300.0 * (curr_hull_hp / 1000500.0); //TOO DOMINANT?!?!? 20 mil on avg scout.
    double part2 = sensors_status * 3500500.0 * ((sensor_range / Missilemada2.getSensorRangeMinShip()) - 1.0);
    double part3 = engine_status * 9500500.0 * (max_speed / (0.5*getAvgScoutSpeed()) - 1.0);
    if (part2 < 0.0)
      part2 = 0.0;
    if (part3 < 0.0)
      part3 = 0.0;
    return 0.40 /*adjust for player reading*/ * (part1 + part2 + part3);
  }
  public int getBattleStrIntDisp() { //text for player's understanding.
    return (int) (getBattleStr() / 1000000.0);
  }
  public double getBattleStrPerc() { //0.0 - 1.0, possibly over 1.01
    return getBattleStr() / max_battle_str;
  }
  public void reportSampleMislFuelRange(double distance_fuel_expired) {
    misl_range_measured = (misl_range_measured * misl_fuelreportcount + distance_fuel_expired) / (misl_fuelreportcount +1);
    misl_fuelreportcount++;
    //System.out.println("Ship " +getId() + " misl_fuel_avg got update, radius avg is " +misl_range_measured);
  }
  public boolean isNearBase() {
    return (calcDistanceVecVec(this.getXYZ(), parentFaction.getXYZ()) < 3.8*Missilemada2.getMiningDistance());
  }
  public static double getAvgScoutHullHP() {
    return 80500500 * (0.25);
  }
  public static double getAvgScoutSpeed() {
    //scout min speed would be then 21.0. Max at 150%.
    return 21.0 * (1.0 + 0.5*(0.5/*the rand*/)); //1.851 km/sec shuttle from real world, but ditch that.
  }
  public static double getMaxScoutSpeed() {
    return 21.0 * (1.0 + 0.5*(1.0/*the rand*/));
  }
  public void setTimeDelay(double seconds) {
    timestamp_next_allowed_accel = Missilemada2.getWorldTime() + (int)seconds;
  }
  public Ship getBuddyMil() {
    return buddy_mil;
  }
  public Ship getBuddyCiv() {
    return buddy_civilian;
  }
  public java.awt.Color getMissileColor() {
    if (parentFaction == null) //if dead ship, ummmmmm change its missile color, okay.
      return new java.awt.Color(160,160,160);
    else
      return parentFaction.getMissileColor();
  }
  private double getDistanceFromBase() {
    return calcDistanceVecVec(getXYZ(), parentFaction.getXYZ());
  }
  private double getDistanceFromFrontline() {
    return calcDistanceVecVec(getXYZ(), parentFaction.getFrontlineXYZ());
  }
  private double getDestinationsDistanceFromBase() {
    if (current_destinationXYZ == null)
      return calcDistanceVecVec(getXYZ(), parentFaction.getXYZ()); //no desti, return self.dist from base.
    return calcDistanceVecVec(getDestination(), parentFaction.getXYZ());
  }
  private double getDestinationsDistanceFromFrontline() {
    if (current_destinationXYZ == null)
      return calcDistanceVecVec(getXYZ(), parentFaction.getFrontlineXYZ()); //no desti, return self.dist from frontline.
    return calcDistanceVecVec(getDestination(), parentFaction.getFrontlineXYZ());
  }
  private Vector getDestination() {
    return current_destinationXYZ;
  }
  public double getMyMissileDistKnown() {
    return misl_range_measured;
  }
  public double getMyMissileSenRange() {
    return misl_sensorrange;
  }
  public double getAttackBeamRange() {
    if (shield_regen_per_min < 1.0 || beamsystem_status < 0.5)
      return 0.0;

    double ret = 0.40 * sensor_range * (sensors_status * beamsystem_status);
    if (type.equals("BEAMDRONE")
        || isStarbase()) {
      ret = 1.42 * ret; //specialized
    }
    if (type.equals("AC")) {
      ret = 1.15 * ret;
    }
    return ret;
  }
  public double getDefenseBeamRange() {
    //beamdrone is better ranged too.
    return 0.35 * getAttackBeamRange(); //need to be closer, for need much more accuracy vs small fast missile.
  }
  public double getAtkBeamStr() {
    return 45.0 * shield_regen_per_min/*90 000 * 0.22*/ * beamsystem_status; //xxxx needs work. relative to avg missile or avg scout hp?
    //return 0.4 * Missile.getAvgMislYield() * beamsystem_status;
  }
  private int getAttackBeamRechargeSeconds(){
    double a = 700;
    if (type.equals("SCOUT"))
      a = 900;
    if (type.equals("DEFENDER"))
      a = 700;
    if (type.equals("BEAMDRONE") || type.equals("MINER"))
      a = 450;
    if (type.equals("AC") || type.equals("STARBASE") )
      a = 200; //possibly it has multiple emitters...
    return (int) Math.round(2.2 * a * (1.0/beamsystem_status));
  }
  private int getDefenseBeamRechargeSeconds() { //xxGAMEPLAY tuning spot
    //N sec, then can try another shootdown attempt. Missile goes 2*18 km in 18secs?
    double secs = 1000.0; //default
    if (type.equals("MINER"))
      secs = 0.900 * defense_beam_rechargebasethou;
    if (type.equals("SCOUT"))
      secs = 0.480 * defense_beam_rechargebasethou;
    if (type.equals("BEAMDRONE"))
      secs = 0.370 * defense_beam_rechargebasethou;
    if (type.equals("DEFENDER"))
      secs = 0.330 * defense_beam_rechargebasethou;
    if (type.equals("AC") || type.equals("STARBASE"))
      secs = 0.220 * defense_beam_rechargebasethou; //worth four scouts, defensively, eh?
    return (int) Math.round(2.7 * secs * (1.0/beamsystem_status)); //GAMEPLAY
  }
  public boolean hasBeamSystem() {
    if (shield_regen_per_min > 10.0) {
      if (isScout() || isStarbase() || type.equals("AC") || type.equals("DEFENDER") || type.equals("MINER") || type.equals("BEAMDRONE") ) {
        return true;
      }
    }
    return false;
  }
  private boolean hasMissileCapability() {
    if (max_buildcredits > 2 * (misl_cost+1.0))
      return true;
    else
      return false;
  }
  private int getMislLauncherRechargeSeconds() {
    //xxmaybe? branch on ship type. misldrone many tubes, scout one, defender two, base LOTS.
    return (int) Math.round(40 * (1.0/missilesystem_status));
  }
  private void playCrewmanDied() {
    if (isInPlayerFaction())
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("alas bongo", 10, "") /*Vector of pitches*/, 58 /*core note*/, 113 /*xxx*/, 90, 1.2F /*note duration*/);
  }
  private void playUpgradeinstalled() {
    if (isInPlayerFaction())
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("woot upgraydd", 4, "") /*Vector of pitches*/, 64 /*core note*/, 113 /*agogo*/, 100, 2.9F /*note duration*/);
    //click-clock, good.
  }
  private void playMiningStartSmall() {
    if (isInPlayerFaction() && !parentFaction.isAnyOfOurShipsInBattle())
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("scoutmin", 4, "") /*Vector of pitches*/, 31 /*core note*/, 67 /*instrument*/, 40, 0.5F /*note duration*/);
  }
  private void playMiningStartBig() {
    if (isInPlayerFaction() && !parentFaction.isAnyOfOurShipsInBattle())
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("scoutmin", 4, "") /*Vector of pitches*/, 22 /*core note*/, 100 /*instrument*/, 88, 1.8F /*note duration*/);
  }
  private void playCargoOffloadSmall() {
    //if (isInPlayerFaction() && !parentFaction.isAnyOfOurShipsInBattle())
    //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("scoutaofflo", 6, "") /*Vector of pitches*/, 34 /*core note*/, 67 /*instrument*/, 40, 0.5F /*note duration*/);
  }
  private void playCargoOffloadBig() {
    if (isInPlayerFaction()
            /*&& !parentFaction.isAnyOfOurShipsInBattle()*/ ) {
      //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("big miner off", 7, "") /*Vector of pitches*/, 22 /*core note*/, 100 /*instrument*/, 90, 1.2F /*note duration*/);
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("scrap get", 3, "") /*Vector of pitches*/, 34 /*core note*/, 113 /*agogo*/, 120, 1.9F /*note duration*/);
    }
  }
  private void playMiningAccident() {
    if (isInPlayerFaction()
            && !parentFaction.isAnyOfOurShipsInBattle()) {
      //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("accide-boom", 3, "") /*Vector of pitches*/, 92 /*core note*/, 125 /*helicopter*/, 70, 1.2F /*note duration*/);
      //old  Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("acci", 2, "") /*Vector of pitches*/, 45 /*core note*/, 72 /*piccolo*/, 60, 1.0F /*note duration*/);
    }
  }
  private void playMissileFiringNote(double missile_cost) {
    int missile_cost_int = (int) missile_cost; //was xx..xx seen
    int thenote = 42 - (int)(10*missile_cost/16.0); //want -10 at most.
    Missilemada2.putMelodyNotes(Missilemada2.stringNumberlistToMelodyVector(""+thenote) /*Vector of pitches*/, 0 /*core note*/, 32 /*slapbass*/, 110, 6.3F /*note duration*/);
  }
  private void playAtkBeamFiringNote() {
    //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("hackazat", 1, "") /*Vector of pitches*/, 52 /*core note*/, 31 /*guitar harmonics*/, 40, 4.3F /*note duration*/);
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("hackazat", 1, "") /*Vector of pitches*/, 77 /*core note*/, 110 /*fiddle*/, 80, 1.3F /*note duration*/);
  }
  private void playDefBeamFiringNote() {
    //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("hackazat", 1, "") /*Vector of pitches*/, 82 /*core note*/, 110 /*fiddle*/, 60, 1.3F /*note duration*/);
    //note31 instr 15 dulcimer.
    Missilemada2.putMelodyNotes(Missilemada2.stringNumberlistToMelodyVector("31") /*Vector of pitches*/, 0 /*core note*/, 15 /*dulcimer */, 60, 2.3F /*note duration*/);

  }
  private void playGotHullDamage() {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("hullplating2", 2, "") /*Vector of pitches*/, 42 /*core note*/, 126 /*applause*/, 120, 9.9f /*note duration*/);
  }
  private void playDeployedSENSAT() {
    //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("virpla", 3, "") /*Vector of pitches*/, 68 /*core note*/, 126 /*applause*/, 50, 0.9F /*note duration*/);
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("sensat deploy", 3, "") /*Vector of pitches*/, 72 /*core note*/, 30 /*dist guit*/, 75, 1.4f /*note duration*/);
  }
  private void playDeployedCarriedDrones() {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("drones deploy", 7, "") /*Vector of pitches*/, 65 /*core note*/, 30 /*dist guit*/, 95, 1.45f /*note duration*/);
  }
  public void drawShip(float scale1) {
    float beamthickness = 2400.0f; /*beam thickness*/
    GL11.glPushMatrix();

    //draw attack beam if it's on draw counter. abs world coords, not relative to ship.
    if (beam_atk_draw_counter_frames > 0
            && Missilemada2.gimmeRandDouble() < 0.6 //flicker the beam
            && current_target != null
            && !isDestroyed()) {
      Missilemada2.setOpenGLMaterial("BEAM");
      Missilemada2.setOpenGLTextureBeam();
      //faction color
      java.awt.Color co = parentFaction.getShipColor();
      GL11.glColor4f(co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 1.0f);

      //draw atk beam quad
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(0, 0);
      GL11.glVertex3f((float)xcoord, (float)ycoord, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(0, 1f);
      GL11.glVertex3f((float)xcoord+beamthickness, (float)ycoord+beamthickness, (float)zcoord);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(1f, 0);
      GL11.glVertex3f((float)ene_closest.getX()+beamthickness, (float)ene_closest.getY()+beamthickness, (float)ene_closest.getZ());   GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(1f, 1f);
      GL11.glVertex3f((float)ene_closest.getX(), (float)ene_closest.getY(), (float)ene_closest.getZ());   GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glEnd();
      Missilemada2.setOpenGLMaterial("SHIP");
    }
    //draw defe beam if it's on draw counter
    if (beam_def_draw_counter_frames > 0 && stored_tryShootDownMissile != null && !isDestroyed()) {
      double dist = calcDistanceMTMT(stored_tryShootDownMissile, this);
      if (dist < getDefenseBeamRange()) {
        Missilemada2.setOpenGLMaterial("BEAM");
        Missilemada2.setOpenGLTextureBeam();
        //blue
        GL11.glColor4f(0.1f, 0.2f, 1.0f, 0.8f);
        //draw defe beam quad
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f((float)xcoord, (float)ycoord, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
        GL11.glVertex3f((float)xcoord+beamthickness, (float)ycoord+beamthickness, (float)zcoord);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
        GL11.glVertex3f((float)stored_tryShootDownMissile.getX()+beamthickness, (float)stored_tryShootDownMissile.getY()+beamthickness, (float)stored_tryShootDownMissile.getZ());   GL11.glNormal3f(0.0f, 0.0f, 1.0f);
        GL11.glVertex3f((float)stored_tryShootDownMissile.getX(), (float)stored_tryShootDownMissile.getY(), (float)stored_tryShootDownMissile.getZ());   GL11.glNormal3f(0.0f, 0.0f, 1.0f);
        GL11.glEnd();
        Missilemada2.setOpenGLMaterial("SHIP");
      }
    }

    //draw line indicator how full of missiles I am.
    if (getHullPerc() > 0.02) { //type.equals("MISSILEDRONE") || type.equals("AC")
      Missilemada2.setOpenGLMaterial("LINE");
      Missilemada2.setOpenGLTextureGUILine();
      double x1 = xcoord - (260*radius);
      double y1 = ycoord;
      double z1 = zcoord;
      double width = 2700.0;
      double tall = (max_buildcredits / 10.0) * 200.0 * (curr_buildcredits/max_buildcredits);

      GL11.glColor4f(0.9f, 0.9f, 0.3f, 0.6f);
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glVertex3f((float) x1, (float) y1, (float) z1);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glVertex3f((float) (x1+width), (float) y1, (float) z1);   GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glVertex3f((float) (x1+width), (float) (y1+tall), (float) (z1));   GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glVertex3f((float) x1, (float) (y1+tall), (float) z1);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glEnd();
      Missilemada2.setOpenGLMaterial("SHIP");
    }

    //BORING: draw line to current target
//    if (current_target != null && !isDestroyed() && isInPlayerFaction() ) {
//      Missilemada2.setOpenGLMaterial("LINE");
//      Missilemada2.setOpenGLTextureGUILine();
//      GL11.glColor4f(0.99f, 0.4f, 0.3f, 0.9f);
//      FlatSprite.drawFlatLineVecVec(this.getXYZ(), current_target.getXYZ(), 5.5*this.getRadius());
//      Missilemada2.setOpenGLMaterial("SHIP");
//    }

        /*
        //draw line to buddy if stance formation bool.
        if (might_stay_in_formation_OBSO && see_friend_count > 1 && buddy_mil != null && !isDestroyed()) {
          Missilemada2.setOpenGLMaterial("LINE");
          Missilemada2.setOpenGLTextureGUILine();
          GL11.glColor4f(0.9f, 0.2f, 0.9f, 0.7f);
          FlatSprite.drawFlatLineVecVec(this.getXYZ(), buddy_mil.getXYZ(), 20.5*this.getRadius());
          Missilemada2.setOpenGLMaterial("SHIP");
        }*/


    //draw line to milbuddy
//    if (buddy_mil != null && isInPlayerFaction()) {
//      Missilemada2.setOpenGLMaterial("LINE");
//      Missilemada2.setOpenGLTextureGUILine();
//      GL11.glColor4f(0.99f, 0.0f, 0.0f, 0.9f);
//      FlatSprite.drawFlatLineVecVec(this.getXYZ(), buddy_mil.getXYZ(), 10.5*this.getRadius());
//    }

    //draw line to civbuddy
//    if (buddy_civilian != null && isInPlayerFaction()) {
//      Missilemada2.setOpenGLMaterial("LINE");
//      Missilemada2.setOpenGLTextureGUILine();
//      GL11.glColor4f(0.0f, 0.5f, 1.0f, 0.5f);
//      FlatSprite.drawFlatLineVecVec(this.getXYZ(), buddy_civilian.getXYZ(), 39.5*this.getRadius());
//    }

    //draw line to buddy_derelict, if we are tractoring.
    if (buddy_derelict != null && tractormode /*xxxxtemp  && isInPlayerFaction() */) {
      Missilemada2.setOpenGLMaterial("LINE");
      Missilemada2.setOpenGLTextureGUILine();
      GL11.glColor4f(0.05f, 1.0f, 0.3f, 0.32f); //green
      FlatSprite.drawFlatLineVecVec(buddy_derelict.getXYZ(), this.getXYZ(), 20*this.getRadius());
      Missilemada2.setOpenGLMaterial("SHIP");
    }

    //non-civilian: draw line to move destination.
    if (!isMiner() && current_destinationXYZ != null && !isDestroyed()) {
      if (calcDistanceVecVec(this.getXYZ(), current_destinationXYZ) > 1.1*Missilemada2.getArrivedDistance()) {
        Missilemada2.setOpenGLMaterial("LINE");
        Missilemada2.setOpenGLTextureGUILine();
        GL11.glColor4f(0.99f, 0.99f, 0.2f, 0.2f); //yellow
        FlatSprite.drawFlatLineVecVec(this.getXYZ(), current_destinationXYZ, 60.0*this.getRadius());
        Missilemada2.setOpenGLMaterial("SHIP");
      }
    }
    //civilian move destination
//    if (isMiner() && current_destinationXYZ != null && !isDestroyed()) {
//      Missilemada2.setOpenGLMaterial("LINE");
//      Missilemada2.setOpenGLTextureGUILine();
//      GL11.glColor4f(0.4f, 0.4f, 0.4f, 0.25f); //grey
//      FlatSprite.drawFlatLineVecVec(this.getXYZ(), current_destinationXYZ, 30.0*this.getRadius());
//      Missilemada2.setOpenGLMaterial("SHIP");
//    }
    //civilian desti aste
//    if (isMiner() && destinationAsteroid != null && !isDestroyed()) {
//      Missilemada2.setOpenGLMaterial("LINE");
//      Missilemada2.setOpenGLTextureGUILine();
//      GL11.glColor4f(0.0f, 0.9f, 0.9f, 0.99f);
//      FlatSprite.drawFlatLineVecVec(this.getXYZ(), destinationAsteroid.getXYZ(), 20.5*this.getRadius());
//      Missilemada2.setOpenGLMaterial("SHIP");
//    }
    //civilian closest aste
//    if (isMiner() && closestAsteroid != null && !isDestroyed()) {
//      Missilemada2.setOpenGLMaterial("LINE");
//      Missilemada2.setOpenGLTextureGUILine();
//      GL11.glColor4f(0.9f, 0.2f, 0.9f, 0.3f);
//      FlatSprite.drawFlatLineVecVec(this.getXYZ(), closestAsteroid.getXYZ(), 190.5*this.getRadius());
//      Missilemada2.setOpenGLMaterial("SHIP");
//    }




    //DEBUG: sphere, draw sensors, before ship's-shape-deform-scale.
    if (parentFaction != null) {
      if (parentFaction.show_sensors)
        drawSensorRange();
    }

    //draw shields if have them.
    if (max_shields > 1000.0 && getShieldPerc() > 0.06) {
      double pwr = 10.0 * (curr_shields / max_shields); //xx curr div (parentFaction.getMaxShieldNumber() / 2.0), might confuse player, shields visibly weaken when build an AC.
      if (pwr > 10.0)
        pwr = 10.0;
      //color: blue. emissive.
      java.awt.Color shieldcolor = new java.awt.Color((int)Math.round(19.9*pwr),(int)Math.round(19.9*pwr),(int)Math.round(25.4*pwr));
      if (shield_flash) { //if recent shield damage, other color.
        shieldcolor = new java.awt.Color((int)Math.round(22.9*pwr),(int)Math.round(21.9*pwr),(int)Math.round(10.2*pwr));
      }

      //set emission
      GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_EMISSION);
      GL11.glColor4f(0.4f*shieldcolor.getRed() / 255.0f, 0.4f*shieldcolor.getGreen() / 255.0f, 0.4f*shieldcolor.getBlue() / 255.0f, 0.8f);
      //set bounced light
      GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
      GL11.glColor4f(0.92f, 0.02f, 0.02f, 0.6f);

      FlatSprite shie_sprite = new FlatSprite(13.0*scale1, 13.0*scale1, xcoord, ycoord, zcoord, "shields1.png", 1.0, 1.0f/*transp*/);
      GL11.glPushMatrix();
      shie_sprite.drawFlatSpriteSHIP((float) (37.0*radius), 0.0f);
      GL11.glPopMatrix();
      //extra thick shield visual if AC or base.
      if (type.equals("STARBASE") || type.equals("AC")) {
        GL11.glPushMatrix();
        shie_sprite.drawFlatSpriteSHIP((float) (33.6*radius), 0.0f);
        GL11.glPopMatrix();
      }

      //reset emission and bounced
      Missilemada2.setOpenGLMaterial("SHIP");
    }

    if (type.equals("STARBASE")) {
      //reset emission and bounced
      Missilemada2.setOpenGLMaterial("SHIP");
      //move coords to this
      //GL11.glTranslatef((float) xcoord, (float) ycoord, (float) zcoord);
      drawStarBase(5.9f*scale1);
    } else { //normal ship

//      Sphere s = new Sphere();
//      s.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."
//      s.setNormals(GLU.GLU_NONE);
//
//      s.setNormals(GLU.GLU_FLAT);
//      s.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."

      //if derelict, dull color.
      try {
      if (isDestroyed()) {
        java.awt.Color co = new java.awt.Color(115,115,115);
        //GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        //set emission
        GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_EMISSION);
        GL11.glColor4f(0.4f*co.getRed() / 255.0f, 0.4f*co.getGreen() / 255.0f, 0.4f*co.getBlue() / 255.0f, 1.0f);
        //set bounced light
        GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        GL11.glColor4f(co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 1.0f);
      } else {
        //faction color
        java.awt.Color co;
        if (parentFaction != null)
          co = parentFaction.getShipColor();
        else //old derelict
          co = Missilemada2.getPlayerFaction().getShipColor();
        if (isMiner()) {
          co = new java.awt.Color(co.getRed()-19, co.getGreen()+9, co.getBlue());
        }
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        //set emission to faction color.
        GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_EMISSION);
        GL11.glColor4f(0.4f*co.getRed() / 255.0f, 0.4f*co.getGreen() / 255.0f, 0.4f*co.getBlue() / 255.0f, 1.0f);
        //set bounced light to faction color.
        GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
    //GL11.glColor4f(co.getRed() / 255.0f, co.getGreen() / 255.0f, co.getBlue() / 255.0f, 1.0f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
      }
      } catch (Exception e) {
        e.printStackTrace();
      }

      float rota = (float) ((180.0/Math.PI) * (getBearingXYfromSpeed() - Math.PI / 2.0)); //okay at  (float) ((180.0/Math.PI) * (getBearingXYfromSpeed() - Math.PI / 2.0))
      float siz = (float) (27.0*radius);
      FlatSprite ship_sprite = new FlatSprite(13.0*scale1, 13.0*scale1, xcoord, ycoord, zcoord, ship_sprite_filename, 1.0, 1.0f/*transp*/);
      //GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_ADD);

      //hmm seems okay GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
      ship_sprite.drawFlatSpriteSHIP(siz, rota);
//      if (dodge_mode) {
//        Missilemada2.addVfx2(getX(), getY()-10.0*radius, getZ()+4.0*radius, "DODGE_MODE", 900, 480.0, 0.5/*transp*/, null, "32texture_arrived.png", 1.0, "");
//      }
      //back to default mode
      //GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
    } //end normal ship

    GL11.glPopMatrix();
  }
  public void drawShipInfoText() {
    //draw faction flag, before ship's-shape-deform-scale.
    //GL11.glDisable(GL11.GL_TEXTURE_2D);
    //java.awt.Color faccol = parentFaction.getShipColor();
    //this.drawFactionFlag(faccol, 5f, 900f, 1500f);
    //GL11.glEnable(GL11.GL_TEXTURE_2D);

    String tx;
    if (isMiner()) {
      tx = "" //+parentFaction.getFactionId()
              + " "+(int)(100.0*getHullPerc()) + "%";
    } else if (max_shields < 1.0) { //no shields, shorter text
      tx = ""//+parentFaction.getFactionId()
              + " na"
              + " "+(int)(100.0*getHullPerc())
              + "% "+(int)(100*getSystemsStatusAvg())
              + "% "+(int)(buildcost);
              //+ " "+curr_crew_count;
    } else {
      tx = "" //+parentFaction.getFactionId()
              + " "+(int)(100.0*getShieldPerc())
              + "% "+(int)(100.0*getHullPerc())
              + "% "+(int)(100*getSystemsStatusAvg())
              + "% "+(int)(buildcost);
              //+ " "+curr_crew_count;
    }


    tx = tx + " dan" + (int) (10*danger_meter);
    tx = tx + " merit" + (int) (10* merit_badges_curr);

    if (type.equals("MINER") || type.equals("TINYMINER") ) {
      //perc:::  tx = tx + " cargo " + (int) (100*cargo_carried/cargo_capacity);
      tx = tx + " cargo" + (int) (cargo_carried);
//      if (closestAsteroid != null) {
//        tx = tx + " Yclosest ";// + (int) (MobileThing.calcDistanceMTMT(this, closestAsteroid));
//      }
//      if (destinationAsteroid != null) {
//        tx = tx + " YdesAs ";
//      }

    }

    //+ " "+(int)(100*curr_buildcredits/max_buildcredits)

    //the following DOESNT set material to FONT. draws with faction color.
    Missilemada2.drawTextXYZ(Missilemada2.getFont60fornowwww(0), 210f, (float) xcoord, (float) (ycoord-6000.0), (float) zcoord, tx, Color.white);
  }
  public void drawStarBase(float scale1) {
    Sphere s = new Sphere();
    Disk di = new Disk();
    GL11.glPushMatrix();
    if (textureMy != null){
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.
      textureMy.bind(); //GL11.glBindTexture(GL11.GL_TEXTURE_2D, 1);
      s.setNormals(GLU.GLU_FLAT);
      s.setTextureFlag(true); // "specifies if texture coordinates should be generated for quadrics rendered with qobj."
      di.setTextureFlag(true);
    }

    GL11.glTranslatef((float) xcoord, (float) ycoord, (float) zcoord);
    float rot = 0.003f * Missilemada2.getWorldTime()%360;
    GL11.glRotatef(rot, 0.0f, 0.0f, 1.0f);
    GL11.glRotatef(45, 0.0f, 1.0f, 0.0f);


    float siz = scale1*(float)radius*17.0f;
    s.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point
    di.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point
    //disc, then sphere.
    if (isDestroyed())
      GL11.glColor4f(0.4f, 0.4f, 0.4f, 1.0f);
    else
      GL11.glColor4f(0.9f, 0.9f, 0.45f, 0.6f); //yellow
    di.draw(0.9f*siz, 1.3f*siz, 7, 7);

    //sphere
    if (isDestroyed())
      GL11.glColor4f(0.4f, 0.4f, 0.4f, 1.0f);
    else
      GL11.glColor4f(0.3f, 0.3f, 1.0f, 1.0f);
    s.draw(siz, 7, 7);

    GL11.glPopMatrix();
  }
  public void drawSensorRange() {
    Sphere s = new Sphere();
    Disk di = new Disk();
    GL11.glPushMatrix();
    GL11.glTranslatef((float) xcoord, (float) ycoord, (float) zcoord);

    float siz = (float) getSensorRange();
    s.setDrawStyle(GLU.GLU_LINE); //fill, line, silhouette, point
    di.setDrawStyle(GLU.GLU_LINE); //fill, line, silhouette, point

    GL11.glColor4f(0.9f, 0.9f, 0.45f, 0.31f); //yellow
    if (see_enemy_count > 0)
      GL11.glColor4f(0.9f, 0.2f, 0.25f, 0.61f); //red
    di.draw(siz/1.0001f, siz, 32, 1);
    //sphere
    //s.draw(siz, 8, 5);
    GL11.glPopMatrix();
  }

  public double getStanceNearFar() {
    //personality_nearfar : 0far .. 1nearother
    double ret = ((personality_aggro + 3.0)/4.0) * getSystemsStatusAvg() * getShieldPerc();
    //System.out.println("ship id "+getId()+" stance_nearfar = "+ret); //ok, verified.
    return ret; //if hurt, more far away.
  }
  public double getStanceLR() {
    //double personality_LR /*0..1 with 0.5=no deviation */,
    return LR_stance;
  }
  private Vector decideEvadeLocation(MobileThing evade1, MobileThing evade2) { //eg. closest foe, closest (tryshootdown)missile
    Vector ret = null;
    if (evade1 == null || evade2 == null)
      return parentFaction.getXYZ_starbase_safer_side();
    double dist = calcDistanceMTMT(this, evade1);
    for (int loopbreaker = 0; loopbreaker < 100; loopbreaker++) {
      //pick spot away from evade1
      ret = Missilemada2.getRandomLocationNear(evade1.getXYZ(), 2.0*dist, 0.2);
      //if that spot is also away from evade2, AND inside play area, approve.
      if (calcDistanceVecVec(ret, evade2.getXYZ()) > 2.5*dist
              && Missilemada2.areCoordsWithinPlayarea(ret) )
        break;
    }
    return ret;
  }
  public void advance_time_derelict_drift(double sec) {
    //just float along trajectory.
    xcoord = xcoord + (xspeed * (sec));
    ycoord = ycoord + (yspeed * (sec));
    zcoord = zcoord + (zspeed * (sec));
    prev_xcoord = xcoord;
    prev_ycoord = ycoord;
    prev_zcoord = zcoord;

    //reduce speed, for more fun????
    /////reduceSpeed(0.9999); //xxx need polish, relative to seconds.

    //xxx apply current gravity well[not yet implemented] into speed.
  }
  public Ship getRandomAllyBADFUNC() {
    Vector li = Missilemada2.getShipsOfFaction(parentFaction);
    Ship candi = null;
    for (int j = 0; j < li.size(); j++) {
      Ship bla = (Ship) li.elementAt(j);
      if (bla != null) {
        if (bla.getFaction() == parentFaction) {
          if (candi == null) { //first candidate, to memory.
            candi = bla;
          } else { //rand chance of other
            if (Missilemada2.gimmeRandDouble() < 0.1) {
              candi = bla;
            }
          }
        }
      }
    }
    return candi;
  }
  public Ship getMinerToProtect() {
    Vector li = Missilemada2.getShipsOfFaction(parentFaction);
    Ship candi = null;
    for (int j = 0; j < li.size(); j++) {
      Ship s = (Ship) li.elementAt(j);
      if (s != null) {
        if (s.getFaction() == this.parentFaction) {
          if (candi == null) { //first MINER candidate, to memory.
            if (s.getType().equals("MINER"))
              candi = s;
          } else { // already have a candidate. rand chance of other as we go down the list
            if (Missilemada2.gimmeRandDouble() < 0.5 && s.getType().equals("MINER")) {
              candi = s;
            }
          }
        }
      }
    }
    return candi;
  }

  public double getKineticEnergy() { //ship's version includes cargo. Drones in cargo weigh nothing, so far.
    return 0.5 * (mass_kg + cargo_carried) * (1000.0* getSpeedCurrent()) * (1000.0* getSpeedCurrent()); //km/s to m/s. return joules.
  }
  public void debugVFXText(Vector xyz, String tx) {
    //if (this.isInPlayerFaction())
    //  Missilemada2.addVfx2(xyz, "TEXT", 15900, 100.0, 0.6/*transp*/, "", 1.0, tx);
  }
  public void clearVisitedDesList() {
    visited_destinations_XYZs_fooooooo = new Vector(50,50);
  }
  public boolean isTooDamagedToObeyFrontline() {
    if (getHullPerc() < 0.82 || getSystemsStatusAvg() < 0.85)
      return true;
    else
      return false;
  }
  public boolean hasDesti() {
    return have_destination;
  }
  public double getMissileStorePerc() {
    if (max_buildcredits < 0.001)
      return 0.0;
    return (curr_buildcredits / (max_buildcredits+0.001));
  }
  private void tryDeploySenSat() {
    double dist_from_base = calcDistanceVecVec(this.getXYZ(), parentFaction.getXYZ());

    //if no sensat near this spot. Otherwise AC drops 3 in one spot.
    //Missilemada2.getShips_XYNearest()
    Vector SPOT_seenships = Missilemada2.getShips_XYNearest(getX(), getY(), 1.15*getSensorRange(), 4);
    if (Missilemada2.doesShipVectorContainType(SPOT_seenships, "SENSAT"))
      return;

    if (carried_sensats > 0 && dist_from_base > 0.4 * Missilemada2.getBaseDistance() ) { //don't deploy too close to base.
      Vector xyz = this.getXYZ();
      Ship ss = new Ship("SENSAT", parentFaction, xyz, "a", "starship.png", false/*needs_crew*/);
      Missilemada2.addToShipList_withcrewcheck(ss);
      carried_sensats = carried_sensats - 1;
      if (isInPlayerFaction()) {
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "our " + type +" "+unique_id + " deployed a sensor satellite.");
        playDeployedSENSAT();
      }
      curr_buildcredits = curr_buildcredits * 0.94; //irrelevant but logical?
    }
  }
  private void getMoreDroneCargoFromBase() {
    if (type.equals("MINER") || type.equals("DEFENDER") || type.equals("SCOUT") ) {
      if (carried_sensats < 1) {
        //get more sensats
        carried_sensats = 1;
        //base minerals down a little
        parentFaction.resource_metal1 = parentFaction.resource_metal1 - 1.5;
        parentFaction.resource_metal2 = parentFaction.resource_metal2 - 0.8;
      }
    }

    if (type.equals("AC") && (carried_sensats < 3) ) {
        carried_sensats = 3;
      //base minerals down a little
      parentFaction.resource_metal1 = parentFaction.resource_metal1 - 9.5;
      parentFaction.resource_metal2 = parentFaction.resource_metal2 - 3.5;
    }
    if (type.equals("AC") && (carried_beamdrones < 2) ) {
      carried_beamdrones = 2;
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + toStrTypeNName() + " picked up two beamdrones at base.", 3);
      //base minerals down a littlexxxxx
      parentFaction.resource_metal1 = parentFaction.resource_metal1 - 30.5;
      parentFaction.resource_metal2 = parentFaction.resource_metal2 - 20.5;
//xxx

    }
    if (type.equals("AC") && (carried_missiledrones < 3) ) {
      carried_missiledrones = 1;
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + toStrTypeNName() + " picked up a missiledrone at base.", 3);
      //base minerals down a little
      parentFaction.resource_metal1 = parentFaction.resource_metal1 - 20.5;
      parentFaction.resource_metal2 = parentFaction.resource_metal2 - 20.5;

 //xxx  calcShipCostFromDNA("MISSILEDRONE", "qqqqqqqq");
//      if (haveResources(tryship.getResourcesRequired())) {
//        removeResources(tryship.getResourcesRequired());



      }

  }
  private boolean isXYZNearFrontline(Vector xyz) {
    if (calcDistanceVecVec(xyz, parentFaction.getFrontlineXYZ()) < 0.2 * Missilemada2.getBaseDistance()) {
      return true;
    }
    return false;
  }
  private void tryDeployCombatDrones() {
    if (carried_beamdrones == 2 && carried_missiledrones == 1) {
      Ship ss;
      double dronetotalcost = 0.0;
      ss = new Ship("BEAMDRONE", parentFaction, getXYZ(), Missilemada2.getRandomDNA(), "starship.png", false/*needs_crew*/); parentFaction.shipCountUp(ss, "AC drone deploy");
      Missilemada2.addToShipList_withcrewcheck(ss); dronetotalcost = dronetotalcost + ss.getCost();
      ss = new Ship("BEAMDRONE", parentFaction, getXYZ(), Missilemada2.getRandomDNA(), "starship.png", false/*needs_crew*/); parentFaction.shipCountUp(ss, "AC drone deploy");
      Missilemada2.addToShipList_withcrewcheck(ss); dronetotalcost = dronetotalcost + ss.getCost();
      ss = new Ship("MISSILEDRONE", parentFaction, getXYZ(), Missilemada2.getRandomDNA(), "starship.png", false/*needs_crew*/); parentFaction.shipCountUp(ss, "AC drone deploy");
      Missilemada2.addToShipList_withcrewcheck(ss); dronetotalcost = dronetotalcost + ss.getCost();



      carried_beamdrones = carried_beamdrones - 2;
      carried_missiledrones = carried_missiledrones - 1;
      curr_buildcredits = curr_buildcredits * 0.90; //irrelevant but logical?

      if (isInPlayerFaction()) {
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + toStrTypeNName() + " deployed two beamdrones, one missiledrone. Value:"+(int)dronetotalcost, 3);
        playDeployedCarriedDrones();
      }
    }
  }
  public int getCrewCount() {
    return curr_crew_count;
  }
  public double getSpeedMax() {
    return max_speed;
  }
  public double getMassTons() {
    return mass_kg / 1000.0;
  }
  public double getDistFromBase_perc_of_basedist() {
    return calcDistanceVecVec(this.getXYZ(), parentFaction.getXYZ()) / Missilemada2.getBaseDistance();
  }
  public boolean amIBeyond_ScoutDist() {
    return (calcDistanceVecVec(this.getXYZ(), parentFaction.getXYZ()) > parentFaction.getScouting_distance_avg());
  }
  public boolean amIBeyond_HalfScoutDist() {
    return (calcDistanceVecVec(this.getXYZ(), parentFaction.getXYZ()) > 0.5*parentFaction.getScouting_distance_avg());
  }
  private void checkIfBuddiesDead() {
    if (buddy_civilian != null) {
      if (buddy_civilian.isDestroyed()) {
        buddy_civilian = null;
      }
    }
    if (buddy_mil != null) {
      if (buddy_mil.isDestroyed()) {
        buddy_mil = null;
      }
    }
  }
  private boolean canTractorThisDerelict(Ship derelict) {
    if ( ((isScout() || type.equals("MINER") || type.equals("DEFENDER") || type.equals("AC")) || isStarbase() ) && curr_crew_count > 0) { //now base can tractor too, for easier time.
      return (1.8 * this.mass_kg > derelict.getMass() );
    } else {
      return false;
    }
  }
  public double getTractorRange() {
    if (isScout() || type.equals("MINER") || type.equals("DEFENDER") || type.equals("AC")) {
      return sensor_range / 3.0;
    } else {
      return 0.0;
    }
  }
  public double getRangeForSetDerebuddy() {
    return sensor_range;
  }
  public void changeDerelictsSpeedTowardsVec(double amt, Vector desti_xyz, double seconds) { //for derelicts that are being towed.
    //System.out.print("changeDerelictsSpeedTowardsVec(): spd before nudge: "+this.getSpeedCurrent());
    double des_x = (Double)desti_xyz.get(0);
    double des_y = (Double)desti_xyz.get(1);
    double des_z = (Double)desti_xyz.get(2);

    double bear_to_desti_xy = Missilemada2.calcBearingXY(xcoord, ycoord, des_x, des_y); //cpu-intensive
    double bear_to_desti_xz = Missilemada2.calcBearingXY(xcoord, zcoord, des_x, des_z); //cpu-intensive

    xspeed = xspeed + (amt * seconds) * Math.cos(bear_to_desti_xy);
    yspeed = yspeed + (amt * seconds) * Math.sin(bear_to_desti_xy);
    zspeed = zspeed + (amt * seconds) * Math.sin(bear_to_desti_xz); //z is like y but in other dim, so SIN().
    //maxspeed is a ceiling.
    //cap the speed so max_speed has meaning. (ships must deflect debris, otherwise die of high travel speed.)
    //System.out.println(" after nudge: "+this.getSpeedCurrent());

    ///////was badddd coz max speed was zero for being dead.   reduceSpeedToThisCap(0.4*max_speed); //new 2013-12-22. Prev was one-time reduction, not capping.
    reduceSpeedToThisCap(0.5*max_speed);
    curr_kinectic_energy = getKineticEnergy();

  }
  private boolean haveShieldSystem() {
    return (max_shields > 10.0 && shield_regen_per_min > 0.2);
  }
  private String getMyTypesMode() {
    if (isScout())
      return parentFaction.getMode("SCOUT");
    if (isMiner())
      return parentFaction.getMode("MINER");
    if (isMil())
      return parentFaction.getMode("MIL");
    return "error, are you a base?";
  }
  private boolean amIinNEARFARandPEACE() { //then you can goto seen derelict.
    if ( (getMyTypesMode().equals("NEAR") || getMyTypesMode().equals("FAR"))
            && !am_under_fire
            && !areMyBuddiesInCombat() ) {
      return true;
    } else {
      return false;
    }
  }
  private boolean areMyBuddiesInCombat() {
    boolean ret = false;
    if (buddy_civilian != null) {
      if (buddy_civilian.isInCombat())
        ret = true;
    }
    if (buddy_mil != null) {
      if (buddy_mil.isInCombat())
        ret = true;
    }
    //buddy_derelict doesn't count
    return ret;
  }
  public Vector getXYZofShip_shifted_away_from_FL() { //crude but should work for "cower behind the base"
    double fl_x = (Double) parentFaction.getFrontlineXYZ().get(0);
    double fl_y = (Double) parentFaction.getFrontlineXYZ().get(1);
    double xshift, yshift;
    double is_at_base_distance = 0.65/*ish*/ * Missilemada2.getArrivedDistance();

    if (fl_x > xcoord) {
      xshift = -is_at_base_distance;
    } else {
      xshift = is_at_base_distance;
    }
    if (fl_y > ycoord) {
      yshift = -is_at_base_distance;
    } else {
      yshift = is_at_base_distance;
    }
    return changeXYZ(getXYZ(), xshift, yshift, 0.0);
  }
  private boolean isInPlayerFaction() {
    return (parentFaction == Missilemada2.getPlayerFaction());
  }
  public void addVfxOfStr() {
    Missilemada2.addVfxOnMT(0, -5 * this.getRadius(), 0, "TEXT", 19000, 900.0, 1.0, this, "", 1.0, "str:" + this.getBattleStrIntDisp());
  }
  public void addVfxOfCargoCarried() {
    Missilemada2.addVfxOnMT(0,-5*this.getRadius(), 0, "TEXT", 29000, 900.0, 1.0, this, "", 1.0, "cargo:"+(int)cargo_carried);
  }
  static class ShipComparator implements Comparator<Ship> {
    @Override
    public int compare(Ship a, Ship b) {
      return (a.getMaxBattleStr() < b.getMaxBattleStr()) ? -1 : ((a.getMaxBattleStr() == b.getMaxBattleStr()) ? 0 : 1);
    }
  }

}
