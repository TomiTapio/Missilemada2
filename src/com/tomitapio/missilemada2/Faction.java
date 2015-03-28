package com.tomitapio.missilemada2;
import java.awt.Color;
import java.util.Vector;

public class Faction {
  Vector frontlineLocation;
  Vector starBaseLocation;
  StarBase base;
  Commander cmdr;

  Vector scoutreports_ships; //removed from list when N ticks not seen. //last_seen_stamp
  Vector scoutreports_bases; //base forgetting VERY slow. ie. never.
  Vector scoutreports_asteroids;
  Vector scoutingCandidateSpots;
  private double scouting_distance_avg = 0.9 * Missilemada2.getScoutingDistance_crude();

  private static final long ASTE_FORGET_SECONDS = 25 * 24 * 3600; //gameplay
  private static final long SHIP_FORGET_SECONDS = 580 * 60; //gameplay
  private static final long RESUPPLY_INTERVAL_SECONDS = 3*24*3600; //gameplay, was 2 days, too often
  private static final long RESUPPLY_DOCKDURATION_SECONDS = 4*3600; //gameplay
  private static final long SAVEXP_EVERY_SECONDS = 8/*days*/*24*3600; //anti-boosting, must play instead of start_new_scenario constantly.

  int crew_alive = 0; //assigned and unassigned
  int crew_idle = 0; //unassigned
  int crew_lost = 0; //dead count
  int crew_shortage_upon_buildtry = 0; //to help with build-which-ship logic.
  //xxx prisoners gained from surrender (& brainwash)

  int scout_count = 0;
  int miner_count = 0;
  int tinyminer_count = 0;
  int def_count = 0;
  int ac_count = 0;
  int md_count = 0;
  int bd_count = 0;
  //do not count our SENSAT sensor satellites, they are not really ships, more like utility missiles.

  int curr_wanted_count_scout = 0;
  int curr_wanted_count_missiledrone = 0;
  int curr_wanted_count_beamdrone = 0;
  int curr_wanted_count_tinyminer = 0;
  int curr_wanted_count_miner = 0;
  int curr_wanted_count_defender = 0;
  int curr_wanted_count_ac = 0;

  //resources at starbase. gameplay important.
  double DIFFICULTYADJ_AI_RESOGAIN = 0.0;
  double resource_fuel = 0.0;
  double resource_metal1 = 0.0;
  double resource_metal2 = 0.0;
  double resource_metal3 = 0.0;
  private double FUELMIN = 450.0; //xxx these should depend on price of cheapest? assault cruiser.
  private double M1MIN = 400.0;
  private double M2MIN = 250.0;
  private double M3MIN = 120.0;
  private String lacking_resource = "FUEL"; //default aste type to look for. Use this before have hit any "lack X to build ship".

  private String name;
  private int factionId = 0; //0,1(player),2,3,4...
  private int curr_shipcount = 0;
  private double max_known_ourshields = 30500500 * (0.98 + 0.10); //in MJ //copy default from scout.
  Vector personalityVec; // Vector of N Doubles
  public boolean show_sensors = false;

  //default faction AI mode:
  private String scoutmode = "FAR";
  private String milmode = "FAR";
  private String minermode = "GO";


  int num_unique_foes_seen = 0;
  Ship strongest_seen_enemy = null;
  private double total_ene_battlerstr_in_SRs = 0.0;
  private double total_battlestr = 0.0;
  private double total_active_ships_buildcost = 0.0;
  private double total_mining_cargocapa = 0.0;

  private double spent_on_ships = 0.0;
  private double spent_on_missiles = 0.0;
  private double score = 0.0;
  private double resupply_arrivetimer = 0.0;
  private double savexp_timer = 0.0;
  private double resupply_departtimer = 4*3600;
  double fac_ship_prod_timer_secs = 0.0; //delayer of production


  double perc20 = 0.0;
  double scout_cost0 = 0.0;
  double scout_cost20 = 0.0;
  double scout_cost40 = 0.0;
  double scout_cost60 = 0.0;
  double scout_cost80 = 0.0;
  double scout_cost100 = 0.0;

  double m_cost0 = 0.0;
  double m_cost20 = 0.0;
  double m_cost40 = 0.0;
  double m_cost60 = 0.0;
  double m_cost80 = 0.0;
  double m_cost100 = 0.0;

  double tm_cost0 = 0.0;
  double tm_cost20 = 0.0;
  double tm_cost40 = 0.0;
  double tm_cost60 = 0.0;
  double tm_cost80 = 0.0;
  double tm_cost100 = 0.0;

  double md_cost0 = 0.0;
  double md_cost20 = 0.0;
  double md_cost40 = 0.0;
  double md_cost60 = 0.0;
  double md_cost80 = 0.0;
  double md_cost100 = 0.0;

  double bd_cost0 = 0.0;
  double bd_cost20 = 0.0;
  double bd_cost40 = 0.0;
  double bd_cost60 = 0.0;
  double bd_cost80 = 0.0;
  double bd_cost100 = 0.0;

  double de_cost0 = 0.0;
  double de_cost20 = 0.0;
  double de_cost40 = 0.0;
  double de_cost60 = 0.0;
  double de_cost80 = 0.0;
  double de_cost100 = 0.0;

  double ac_cost0 = 0.0;
  double ac_cost20 = 0.0;
  double ac_cost40 = 0.0;
  double ac_cost60 = 0.0;
  double ac_cost80 = 0.0;
  double ac_cost100 = 0.0;

  public Faction(Vector XYZ, int id_in, String name_in, Vector perso /*various 0-1 doubles TOTALLYNOTDONE*/, double ai_reso_boost) {
    starBaseLocation = XYZ;
    frontlineLocation = XYZ;
    name = id_in + ": " + Missilemada2.getRandomJapaneseName();
    factionId = id_in;
    personalityVec = perso;
    DIFFICULTYADJ_AI_RESOGAIN = ai_reso_boost;

    scoutreports_asteroids = new Vector(20,20);
    scoutreports_ships = new Vector(20,20);
    scoutreports_bases = new Vector(4,4);
    scoutingCandidateSpots = new Vector(30,30);

    //init: random wants!
    int binaryfactor = 1; //rand if don't want any additional over default.

    if (Missilemada2.gimmeRandDouble() < 0.3) { binaryfactor = 0; } else { binaryfactor = 1; }
    curr_wanted_count_scout        = 2 + binaryfactor * Missilemada2.gimmeRandInt(14);if (Missilemada2.gimmeRandDouble() < 0.3) { binaryfactor = 0; } else { binaryfactor = 1; }
    curr_wanted_count_missiledrone = 0 + binaryfactor * Missilemada2.gimmeRandInt(14);if (Missilemada2.gimmeRandDouble() < 0.3) { binaryfactor = 0; } else { binaryfactor = 1; }
    curr_wanted_count_beamdrone    = 0 + binaryfactor * Missilemada2.gimmeRandInt(14);if (Missilemada2.gimmeRandDouble() < 0.3) { binaryfactor = 0; } else { binaryfactor = 1; }
    curr_wanted_count_tinyminer    = 2 + binaryfactor * Missilemada2.gimmeRandInt(8); if (Missilemada2.gimmeRandDouble() < 0.3) { binaryfactor = 0; } else { binaryfactor = 1; }
    curr_wanted_count_miner        = 3 + binaryfactor * Missilemada2.gimmeRandInt(5); if (Missilemada2.gimmeRandDouble() < 0.3) { binaryfactor = 0; } else { binaryfactor = 1; }
    curr_wanted_count_defender     = 2 + binaryfactor * Missilemada2.gimmeRandInt(10);if (Missilemada2.gimmeRandDouble() < 0.3) { binaryfactor = 0; } else { binaryfactor = 1; }
    curr_wanted_count_ac           = 0 + binaryfactor * Missilemada2.gimmeRandInt(3);

    //if player's faction, load saved commander.
    if (factionId == 1) {
      System.out.println("Faction: loading Commander's personnel file...");
      cmdr = new Commander("", this);
      cmdr.loadFromFile();
      //xxxxx do hudmsg? "Welcome back to the asteroid zone, Commander X. Your tech levels are: X X X X X X "
    }
  }
  public void saveCommanderToFile() {
    cmdr.saveToFile();
  }
  public void setBase(StarBase sb) {
    base = sb;
  }
  public String getName() {
    return name;
  }
  private void tryMakeFormation() { //xx later might have multiple formations(fleets), 1 2 3
    //if not enough combat ships, fail

    //who takes point(defender), who middle(AC), who rear(scout, drones).

    //if all ships are of same type, fail? nah.

  }
  private void sendFormationTo(Vector xyz) {
    //xxxx use fac personality, very aggressive or not, just harass or not, or NEVER_ATTACK or ALWAYS_FALL_BACK(defensive)...


  }
  public int volleyTowardsCoords(Vector volley_xyz) {
    //for each our ship, request single missile firing.
    int misl_fired_total = 0;
    Vector ourships = Missilemada2.getShipsOfFaction(this);
    int listsize = ourships.size();
    for (int j = 0; j < listsize; j++) {
      Ship s = (Ship) ourships.elementAt(j);
      if (s != null) {
        if (!s.isDrone()) { //gameplay decision: drones shall not volley.
          if (Missilemada2.gimmeRandDouble() < 0.3) { //randomly, ship is too awkward facing or mining-busy to launch.
            if (MobileThing.calcDistanceVecVec(s.getXYZ(), volley_xyz)  < 1.2 * s.getMyMissileDistKnown())
              misl_fired_total = misl_fired_total + s.requestFireMissile(volley_xyz, null);
          }
        }
      }
    }
    if (isPlayerFaction()) {
      //Missilemada2.addToHUDMsgList("Volley: " + misl_fired_total + " missiles launched.");
      Missilemada2.addVfx2(frontlineLocation, "FRONTLINEFLAG", 960 * 45, 3150.0, 0.7/*transp*/, "frontline3.png", 1.0, "");
    }
    return misl_fired_total;
  }
  public void shiftFrontLine(Vector XYZ, double weighting, Ship reportingShip) {
    //if (isPlayerFaction())
    //  Missilemada2.changeWorldTimeIncrement(-1); //slow down world

    //System.out.print("shiftFL: before " + MobileThing.xyzToIntString(frontlineLocation));
    frontlineLocation = MobileThing.changeXYZTowards(frontlineLocation, XYZ, weighting * 7500.0);
    //System.out.println("  after  " + MobileThing.xyzToIntString(frontlineLocation));
    drawFrontLineIndicator(4510.0 /*size*/, 0.08/*chance*/);
  }
  public void drawFrontLineIndicator(double size, double chance) {
    if (Missilemada2.gimmeRandDouble() < chance
        && isPlayerFaction())
      Missilemada2.addVfx2(frontlineLocation, "FRONTLINEFLAG", 2000, size, 0.13/*transp*/, "frontline3.png", 1.0, "");
  }
  private double getFactionTotalBStr() {
    Vector li = Missilemada2.getShipsOfFaction(this);


    //xxxxxxxxx
    return 100.0;


  }
  public int countNumUniqueEnemiesInSRe() {
    //int count of how many uniqueid enemies fac has in sr list
    Ship ene;
    num_unique_foes_seen = 0;
    if (scoutreports_ships.size() > 0) {

      int siz = scoutreports_ships.size();
      for (int i = 0; i < siz; i++) {
        ScoutReport sr = (ScoutReport) scoutreports_ships.elementAt(i);
        if (sr != null) {
          ene = (Ship) sr.item;
          //xxx uniques onleeeeeeee
          total_ene_battlerstr_in_SRs = total_ene_battlerstr_in_SRs + ene.getBattleStr();
          ///xxxxxx



          num_unique_foes_seen++;
          if (strongest_seen_enemy != null) {
            if (ene.getBattleStr() > strongest_seen_enemy.getBattleStr()) {
              strongest_seen_enemy = ene;
            }
          } else {
            strongest_seen_enemy = ene;
          }
        }
      }
    }
    return num_unique_foes_seen;
  }
  public void tryShipProduction(String plr_req_type, int plr_req_pricebracket) {
    //player choice: cost bracket 1 2 3 4 5. (20-percentiles)
    //tryProduceShipOfType("AC", 1/*cheapest 20%*/);
    //tryProduceShipOfType("SCOUT", 5/*priciest 20%*/);

    if (plr_req_pricebracket > 0) {
      //PLAYER FACTION: mouse click orders.
      Ship plr_try_ship = tryProduceShipOfType(plr_req_type, plr_req_pricebracket, false);
      if (plr_try_ship != null) {
        //can afford it okay.
            //if on delay, return.
            if (fac_ship_prod_timer_secs > 0.1) {
              if (isPlayerFaction()) {
                Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Build: have materials, but constructing previous! Time left "+(int)(fac_ship_prod_timer_secs/3600.0)+"h",0);
                if (plr_try_ship.getCrewCount() > crew_idle)
                  Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Build: ALSO, not enough idle crew.", 0);
                playCantBuildCozDelay();
              }
              return;
            }
        if (Missilemada2.addToShipList_withcrewcheck(plr_try_ship)) {
          removeResources(plr_try_ship.getResourcesRequired()); //better to remove here than in tryBuild.
          addShipSpending(plr_try_ship.getCost());
          shipCountUp(plr_try_ship, "built"); //send HUD msg of success, too.
          Missilemada2.updateBuildButtonColours(); //update player coz can build LESS stuff now.
          addXPToCommander(plr_try_ship, "BUILT");
        } else {
          //not enough idle crew! Ship was not added to world.
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours()+"Build: have materials, but not enough crew. Needs "+plr_try_ship.getCrewCount()+", have "+crew_idle+".",0);
        }

        //vfx, sfx        
        plr_try_ship.addVfxOfStr();
        playFactionBuiltShip(plr_req_type, plr_try_ship.getCost());
        return;
      } else {
        //player could not afford.
        playFactionBuildNoAfford(plr_req_type);
        if (isPlayerFaction())
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Build: Cannot, lack resources.");
      }

    } else { //AI decision, miners first because lack of them causes stagnation.
      String ty = "";
      //for seven types, try build.
      for (int i = 1; i <= 7; i++) {
        if (i == 1) ty = "MINER";
        if (i == 2) ty = "SCOUT";
        if (i == 3) ty = "TINYMINER";
        if (i == 4) ty = "DEFENDER";
        if (i == 5) ty = "AC";
        if (i == 6) ty = "MISSILEDRONE";
        if (i == 7) ty = "BEAMDRONE";
        if (!haveEnoughShipType(ty)) {
          //xxx if crew shortage then make tinyminer instead
          Ship try1 = tryProduceShipOfType(ty, pricebracketFromFacPersonalityXXXX(ty), false);
          if (try1 != null) {
            //vfx, sfx
            if (Missilemada2.addToShipList_withcrewcheck(try1)) {
              //success, enough crew. Crew got assigned in addtoshiplist.
              removeResources(try1.getResourcesRequired()); //better to remove here than in tryBuild.
              addShipSpending(try1.getCost());
              shipCountUp(try1, "built");
              //yyy here could be AI-commander gains xp.
              return;
            } else {
              //fail, not enough idlers.
              //xxxxxx branch on type and try a drone ship instead.

            }
          }
        }

      }

      //xxx incr wanted count of YYYYYY ?
      increaseTypeWantCount();
    }//end AI decision
  }
  private int pricebracketFromFacPersonalityXXXX(String type) {
    //xxxxxxx
    return 1+Missilemada2.gimmeRandInt(5);
    //return 3;
  }
  private boolean haveEnoughShipType(String ty) {
    int count_scout = 0;
    int count_md = 0;
    int count_bd = 0;
    int count_tinyminer = 0;
    int count_miner = 0;
    int count_defender = 0;
    int count_ac = 0;
    Vector myShips = Missilemada2.getShipsOfFaction(this);
    int siz = myShips.size();
    for (int i = 0; i < siz; i++) {
      Ship s = (Ship) myShips.elementAt(i);
      if (s != null) {
        if (s.getType().equals("SCOUT"))
          count_scout++;
        if (s.getType().equals("MISSILEDRONE"))
          count_md++;
        if (s.getType().equals("BEAMDRONE"))
          count_bd++;
        if (s.getType().equals("TINYMINER"))
          count_tinyminer++;
        if (s.getType().equals("MINER"))
          count_miner++;
        if (s.getType().equals("DEFENDER"))
          count_defender++;
        if (s.getType().equals("AC"))
          count_ac++;
      }
    }

    if (ty.equals("SCOUT") && count_scout >= curr_wanted_count_scout)
      return true;
    if (ty.equals("MISSILEDRONE") && count_md >= curr_wanted_count_missiledrone)
      return true;
    if (ty.equals("BEAMDRONE") && count_bd >= curr_wanted_count_beamdrone)
      return true;
    if (ty.equals("TINYMINER") && count_tinyminer >= curr_wanted_count_tinyminer)
      return true;
    if (ty.equals("MINER") && count_miner >= curr_wanted_count_miner)
      return true;
    if (ty.equals("DEFENDER") && count_defender >= curr_wanted_count_defender)
      return true;
    if (ty.equals("AC") && count_ac >= curr_wanted_count_ac)
      return true;
    return false;
  }

  //do I need this func at all or does the repair funcs suffice? dna shuffle? resource reclaim?
  public void refitDefectedOrCapturedShipAtBase(Ship s) {
    //xx verify that it is at base?



    //xx doesn't ship get hullrepair when touches base?



    //ship already in shiplist! and has our faction ptr.







    //empty its cargo, already done

    //generate new ship of same type and DNA.

    //used some faction resources on refit!
    //removeResources(foo.getResourcesRequired(0.1)); //10% materials required.

    //time delay to ship, much smaller than build_new delay.

  }
  public Ship/*null if can't afford or lack techlevel */ tryProduceShipOfType(String whichtype, int price_bracket, boolean only_affordcheck) {
    //System.out.println(Missilemada2.getWorldTime() + ": Faction " + getFactionId() + " tries to make a new " + whichtype);
    //make ship. it doesn't properly exist until it's in shipList.

    if (this.getStarbase() == null) //if we dead, no producing try.
      return null;

    //if commander can't do this tech level, return null.
    //OR return lower level ship, for AI simplicity.
    if (isPlayerFaction()) {
      if (cmdr.getTechLevel(whichtype) >= price_bracket) {
        //ok
      } else {
        //problem
        if (!only_affordcheck)
          System.out.println("Fac"+factionId+" "+whichtype+" try pricebracket "+price_bracket+" realtry failed, not enough tech level xp ("+cmdr.getTechLevel(whichtype)+")");
        return null;
      }
    } else {
      //AI doesn't worry about tech levels.
    }

    //try rand dna until in right price range.
    Ship tryship = null;
    String try_dna;
    boolean loop = true;
    int i = 0;
    while (loop) {
      try_dna = Missilemada2.getRandomDNA();
      tryship = new Ship(whichtype, this, this.getStarbase().getDockingXYZ(), try_dna, "starship.png", true/*needs_crew*/);
      //System.out.println(whichtype + " try cost "+tryship.getCost() + " dna "+try_dna);
      if (isInPriceBracket(whichtype, tryship.getCost(), price_bracket)) {
        loop = false;
      }
      i++;
      if (i > 9000)
        loop = false;
    }
    //check if have materials
    if (haveResources(tryship.getResourcesRequired())) { //sets Faction variable lacking_resource.
      if (only_affordcheck) {
        //if would have enough crew, report can_do.
        if (tryship.getCrewCount() <= crew_idle) {
          return tryship; //check successful.
        } else {
          return null; //can't build coz not enough crew.
        }

      }

      //remove_resources was STUPIDLY HERE. better to remove after add to existing_ships_list.

      lacking_resource = predictResourceLack_Str(); //starbase's initiative, as cargo holds of ships probably have the thing we lacked earlier.

      //time delay to ship --it'll sit fully functional next to base for N time, then it's "built" and ready to go.
      tryship.setTimeDelay(tryship.getBuildTimeDelay()); //it may not move while being constructed. Alas, it is full HP upon creation.
      //its weapons work immediately, but movement not allowed (coz it's still "being built")

      //if player, vfx and setvisible.
      if (isPlayerFaction()) {
        tryship.setIsSeenByPlayer(true);

        //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("Ship built,yay", 13, "") /*Vector of pitches*/, 75 /*core note*/, 72 /*piccolo*/, 65, 1.0F /*note duration*/);
        //System.out.println("Faction "+getFactionId()+" built a new "+tryship.getType() +" of cost "+(int)tryship.getCost()+"  " + tryship.toString());
        Missilemada2.addVfxOnMT(6.0 * tryship.getRadius(), 0, 0, "SHIPBUILT", 70000, 3500.0, 0.9/*transp*/, tryship, "yellowcross.png", 1.0, "");
      }
      //MOVED to after properly exist: addShipSpending(tryship.getCost());
      if (tryship.getMaxShields() > max_known_ourshields)
        max_known_ourshields = tryship.getMaxShields();  //xx should move to after-actually-built

      return tryship;
    } else {
      if (only_affordcheck)
        return null; //check successful, no Vfx no HUDmsg.

      //System.out.println("Faction "+getFactionId()+" couldn't afford to build "+whichtype+" cost " + tryship.getCost());
      if (isPlayerFaction()) {
        Missilemada2.addVfx2(getStarbase().getTextXYZ(), "TEXT", 15000, 20.0, 0.8/*transp*/, "", 1.0, "lack " + lacking_resource);
        if (Missilemada2.gimmeRandDouble() < 0.06)
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Build: No resources for a "+whichtype+".",0);
      }
      return null;
    }

  }
  public void initShipPriceBrackets() { //call once for each faction, in world gen.
    perc20 = 0.0;

    scout_cost0 = calcShipCostFromDNA("SCOUT", "ggggggggg");
    scout_cost100 = calcShipCostFromDNA("SCOUT", "qqqqqqqqq");
    perc20 = 0.2 * (scout_cost100 - scout_cost0);
    scout_cost20 = scout_cost0 + perc20;
    scout_cost40 = scout_cost20 + perc20;
    scout_cost60 = scout_cost40 + perc20;
    scout_cost80 = scout_cost60 + perc20;

    md_cost0 = calcShipCostFromDNA("MISSILEDRONE", "ggggggggg");
    md_cost100 = calcShipCostFromDNA("MISSILEDRONE", "qqqqqqqqq");
    perc20 = 0.2 * (md_cost100 - md_cost0);
    md_cost20 = md_cost0 + perc20;
    md_cost40 = md_cost20 + perc20;
    md_cost60 = md_cost40 + perc20;
    md_cost80 = md_cost60 + perc20;

    bd_cost0 = calcShipCostFromDNA("BEAMDRONE", "ggggggggg");
    bd_cost100 = calcShipCostFromDNA("BEAMDRONE", "qqqqqqqqq");
    perc20 = 0.2 * (bd_cost100 - bd_cost0);
    bd_cost20 = bd_cost0 + perc20;
    bd_cost40 = bd_cost20 + perc20;
    bd_cost60 = bd_cost40 + perc20;
    bd_cost80 = bd_cost60 + perc20;

    m_cost0 = calcShipCostFromDNA("MINER", "ggggggggg");
    m_cost100 = calcShipCostFromDNA("MINER", "qqqqqqqqq");
    perc20 = 0.2 * (m_cost100 - m_cost0);
    m_cost20 = m_cost0 + perc20;
    m_cost40 = m_cost20 + perc20;
    m_cost60 = m_cost40 + perc20;
    m_cost80 = m_cost60 + perc20;

    tm_cost0 = calcShipCostFromDNA("TINYMINER", "ggggggggg");
    tm_cost100 = calcShipCostFromDNA("TINYMINER", "qqqqqqqqq");
    perc20 = 0.2 * (tm_cost100 - tm_cost0);
    tm_cost20 = tm_cost0 + perc20;
    tm_cost40 = tm_cost20 + perc20;
    tm_cost60 = tm_cost40 + perc20;
    tm_cost80 = tm_cost60 + perc20;

    de_cost0 = calcShipCostFromDNA("DEFENDER", "ggggggggg");
    de_cost100 = calcShipCostFromDNA("DEFENDER", "qqqqqqqqq");
    perc20 = 0.2 * (de_cost100 - de_cost0);
    de_cost20 = de_cost0 + perc20;
    de_cost40 = de_cost20 + perc20;
    de_cost60 = de_cost40 + perc20;
    de_cost80 = de_cost60 + perc20;

    ac_cost0 = calcShipCostFromDNA("AC", "ggggggggg");
    ac_cost100 = calcShipCostFromDNA("AC", "qqqqqqqqq");
    perc20 = 0.2 * (ac_cost100 - ac_cost0);
    ac_cost20 = ac_cost0 + perc20;
    ac_cost40 = ac_cost20 + perc20;
    ac_cost60 = ac_cost40 + perc20;
    ac_cost80 = ac_cost60 + perc20;
  }
  private boolean isInPriceBracket(String whichtype, double cost, int price_bracket) {
    if (whichtype.equals("SCOUT")) {
      if (price_bracket == 1) {
        if (cost > scout_cost0 && cost < scout_cost20)
          return true;
      }
      if (price_bracket == 2) {
        if (cost > scout_cost20 && cost < scout_cost40)
          return true;
      }
      if (price_bracket == 3) {
        if (cost > scout_cost40 && cost < scout_cost60)
          return true;
      }
      if (price_bracket == 4) {
        if (cost > scout_cost60 && cost < scout_cost80)
          return true;
      }
      if (price_bracket == 5) {
        if (cost > scout_cost80 && cost < scout_cost100)
          return true;
      }
    }
    if (whichtype.equals("MISSILEDRONE")) {
      if (price_bracket == 1) {
        if (cost > md_cost0 && cost < md_cost20)
          return true;
      }
      if (price_bracket == 2) {
        if (cost > md_cost20 && cost < md_cost40)
          return true;
      }
      if (price_bracket == 3) {
        if (cost > md_cost40 && cost < md_cost60)
          return true;
      }
      if (price_bracket == 4) {
        if (cost > md_cost60 && cost < md_cost80)
          return true;
      }
      if (price_bracket == 5) {
        if (cost > md_cost80 && cost < md_cost100)
          return true;
      }
    }
    if (whichtype.equals("BEAMDRONE")) {
      if (price_bracket == 1) {
        if (cost > bd_cost0 && cost < bd_cost20)
          return true;
      }
      if (price_bracket == 2) {
        if (cost > bd_cost20 && cost < bd_cost40)
          return true;
      }
      if (price_bracket == 3) {
        if (cost > bd_cost40 && cost < bd_cost60)
          return true;
      }
      if (price_bracket == 4) {
        if (cost > bd_cost60 && cost < bd_cost80)
          return true;
      }
      if (price_bracket == 5) {
        if (cost > bd_cost80 && cost < bd_cost100)
          return true;
      }
    }
    if (whichtype.equals("MINER")) {
      if (price_bracket == 1) {
        if (cost > m_cost0 && cost < m_cost20)
          return true;
      }
      if (price_bracket == 2) {
        if (cost > m_cost20 && cost < m_cost40)
          return true;
      }
      if (price_bracket == 3) {
        if (cost > m_cost40 && cost < m_cost60)
          return true;
      }
      if (price_bracket == 4) {
        if (cost > m_cost60 && cost < m_cost80)
          return true;
      }
      if (price_bracket == 5) {
        if (cost > m_cost80 && cost < m_cost100)
          return true;
      }
    }
    if (whichtype.equals("TINYMINER")) {
      if (price_bracket == 1) {
        if (cost > tm_cost0 && cost < tm_cost20)
          return true;
      }
      if (price_bracket == 2) {
        if (cost > tm_cost20 && cost < tm_cost40)
          return true;
      }
      if (price_bracket == 3) {
        if (cost > tm_cost40 && cost < tm_cost60)
          return true;
      }
      if (price_bracket == 4) {
        if (cost > tm_cost60 && cost < tm_cost80)
          return true;
      }
      if (price_bracket == 5) {
        if (cost > tm_cost80 && cost < tm_cost100)
          return true;
      }
    }
    if (whichtype.equals("DEFENDER")) {
      if (price_bracket == 1) {
        if (cost > de_cost0 && cost < de_cost20)
          return true;
      }
      if (price_bracket == 2) {
        if (cost > de_cost20 && cost < de_cost40)
          return true;
      }
      if (price_bracket == 3) {
        if (cost > de_cost40 && cost < de_cost60)
          return true;
      }
      if (price_bracket == 4) {
        if (cost > de_cost60 && cost < de_cost80)
          return true;
      }
      if (price_bracket == 5) {
        if (cost > de_cost80 && cost < de_cost100)
          return true;
      }
    }
    if (whichtype.equals("AC")) {
      if (price_bracket == 1) {
        if (cost > ac_cost0 && cost < ac_cost20)
          return true;
      }
      if (price_bracket == 2) {
        if (cost > ac_cost20 && cost < ac_cost40)
          return true;
      }
      if (price_bracket == 3) {
        if (cost > ac_cost40 && cost < ac_cost60)
          return true;
      }
      if (price_bracket == 4) {
        if (cost > ac_cost60 && cost < ac_cost80)
          return true;
      }
      if (price_bracket == 5) {
        if (cost > ac_cost80 && cost < ac_cost100)
          return true;
      }
    }

    //xxxxxx starbase -- not gonna get built-using-resources ever
    return false;
  }
  private double calcShipCostFromDNA(String whichtype, String min_or_max_dna) {
    Ship tryship = new Ship(whichtype, this, this.getStarbase().getDockingXYZ(), min_or_max_dna, "starship.png", false/*needs_crew*/);
    return tryship.getCost();
  }
  private void removeResources(Vector reso) {
    resource_fuel = resource_fuel - ((Double)reso.get(0));
    resource_metal1 = resource_metal1 - ((Double)reso.get(1));
    resource_metal2 = resource_metal2 - ((Double)reso.get(2));
    resource_metal3 = resource_metal3 - ((Double)reso.get(3));
  }
  private boolean haveResourcesOLD(Vector reso_required) {
    if (resource_fuel > ((Double)reso_required.get(0))
     && resource_metal1 > ((Double)reso_required.get(1))
     && resource_metal2 > ((Double)reso_required.get(2))
     && resource_metal3 > ((Double)reso_required.get(3)) )
      return true;
    else
      return false;
  }
  private boolean haveResources(Vector reso_required) {
    if (resource_fuel < ((Double)reso_required.get(0))) {
      lacking_resource = "FUEL";
      return false;
    }
    if (resource_metal1 < ((Double)reso_required.get(1))) {
      lacking_resource = "METAL1";
      return false;
    }
    if (resource_metal2 < ((Double)reso_required.get(2))) {
      lacking_resource = "METAL2";
      return false;
    }
    if (resource_metal3 < ((Double)reso_required.get(3))) {
      lacking_resource = "METAL3";
      return false;
    }

    lacking_resource = ""; //we good
    return true;
  }
  public boolean haveMiningCapability() {
    return (getMiningCapaTons() > 20.0);
  }
  public double getMiningCapaTons() {
    //for each miner and tinyminer, add cargo capacity to sum.
    int uselessvar = getShipCount("MINER"); //recalc
    return total_mining_cargocapa;
  }
  public void changeScoutingDist(double factor) {
    scouting_distance_avg = scouting_distance_avg * factor;
    if (scouting_distance_avg > Missilemada2.getScoutingDistanceMax())
      scouting_distance_avg = Missilemada2.getScoutingDistanceMax();
  }
  public void addScoutingDist(double a) {
    scouting_distance_avg = scouting_distance_avg + a;
    if (scouting_distance_avg > Missilemada2.getScoutingDistanceMax())
      scouting_distance_avg = Missilemada2.getScoutingDistanceMax();
  }
  public void addScore(double a) {
    score = score + a;
  }
  public int getCrewAlive() {
    return crew_alive;
  }
  public int getCrewIdleCount() { //ones not yet assigned to a ship or base.
    return crew_idle;
  }
  public int getCrewDeadCount() {
    return crew_lost;
  }
  public boolean hasEnoughIdleCrew(int a) {
    if (a <= crew_idle)
      return true;
    else
      return false;
  }
  public void assignCrewToShip(Ship s, int a) {
    if (a < 1) {
      System.out.println("error, assigned zero or negative crewmen to ship "+s.toStringShort());
      throw new NullPointerException("foo88");
    }
    // error if not enough
    if (hasEnoughIdleCrew(a)) {
      if (isPlayerFaction())
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Assigned "+a+" crew to freshly built "+s.toStrTypeNName()+".",2);
      crew_idle = crew_idle - a;
    } else {
      System.out.println("error, not enough idle crew at base.");
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + " Faction "+name+" error: not enough idle crew to assign. "+s.toStrTypeNName()+".",2);
    }
  }
  public void addCrew(int p) {
    crew_alive = crew_alive + p;
    crew_idle = crew_idle + p;
  }
  public void addScoreShipKill(double a, Ship victim) {
    if (isPlayerFaction())
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "We destroyed an enemy "+victim.getType()+"! It had "+victim.getCrewCount()+" crew inside.",2);
    score = score + a;

    //xxhack: AI volley.
    if (this != Missilemada2.getPlayerFaction())
      this.volleyTowardsCoords(getFrontlineXYZ());
  }
  public double getScore() {
    return score;
  }
  private void addShipSpending(double bc) {
    spent_on_ships = spent_on_ships + bc;
  }
  public void addMissileSpending(double bc) {
    spent_on_missiles = spent_on_missiles + bc;
  }
  public void lostCrewmen(int a) {
    if (a < 1) {
      System.out.println("error, lost zero or negative crewmen.");
    } else {
      crew_lost = crew_lost + a;
      crew_alive = crew_alive - a; //reduceCrewAliveCount(a);
    }
  }
  public void shipCountDown(Ship s, String reason) {
    if (s.getType().equals("SCOUT"))
      scout_count--;
    if (s.getType().equals("MISSILEDRONE"))
      md_count--;
    if (s.getType().equals("BEAMDRONE"))
      bd_count--;
    if (s.getType().equals("TINYMINER"))
      tinyminer_count--;
    if (s.getType().equals("MINER"))
      miner_count--;
    if (s.getType().equals("DEFENDER"))
      def_count--;
    if (s.getType().equals("AC"))
      ac_count--;

    curr_shipcount--;

    if (isPlayerFaction()) {
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "__LOST__ "+s.toStrTypeNName()
        +". Cause: "+reason+", "+s.getCountMislHurts()+" misl hits total",1);
    }

    //AI behaviour //xxgameplay important
    if (this != Missilemada2.getPlayerFaction()) {
      this.volleyTowardsCoords(getFrontlineXYZ()); //xxhack: AI volley.

      //rand upon ship lost: back to default modes //this one first
      if (Missilemada2.gimmeRandDouble() < 0.3) {
        this.setMode("SCOUT", "FAR");
        this.setMode("MIL", "FAR");
        this.setMode("MINER", "GO");
      }

      //rand upon ship lost: go on the offensive
      if (Missilemada2.gimmeRandDouble() < 0.2) {
        this.setMode("SCOUT", "FLAG");
        this.setMode("MIL", "FLAG");
        if (s.getType().equals("MINER"))
          this.setMode("MINER", "BASE");
      }
      //rand upon ship lost: escort mode
      if (Missilemada2.gimmeRandDouble() < 0.11) {
        this.setMode("SCOUT", "MINERS");
        this.setMode("MIL", "MINERS");
        this.setMode("MINER", "GO");
      }


    }
    //check if we are vanquished (no more resource getting via miners)
    if (getMiningCapaTons() < 90.0) {
      //try to make tinyminer, if fails, gameover for this faction.
      Ship foo = tryProduceShipOfType("TINYMINER", 5, true); //was price bracket 1, but want gameover sooner.
      if (foo == null) {
        Missilemada2.factionHasLost(this);
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Faction " + factionId + " has lost the race! No serious mining capacity left.",3);
      }
    }
    if (base.isDestroyed()) {
      Missilemada2.factionHasLost(this);
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Faction " + factionId + " has lost the race! Their starbase is ashes.",3);
    }
  }
  public void shipCountUp(Ship s, String reason) {
    drawFrontLineIndicator(3510.0 /*size*/, 1.001/*chance*/);

    //SENSAT doesn't count, but AC's drone deployment does.
    if (s.getType().equals("SCOUT"))
      scout_count++;
    if (s.getType().equals("MISSILEDRONE"))
      md_count++;
    if (s.getType().equals("BEAMDRONE"))
      bd_count++;
    if (s.getType().equals("TINYMINER"))
      tinyminer_count++;
    if (s.getType().equals("MINER"))
      miner_count++;
    if (s.getType().equals("DEFENDER"))
      def_count++;
    if (s.getType().equals("AC"))
      ac_count++;

    curr_shipcount++;

    if (isPlayerFaction()) {
      if (reason.equals("built")) {
        fac_ship_prod_timer_secs = s.getBuildTimeDelay(); //NOTE: delay only on BUILD.

        int aa_hrs = (int)((s.gettimestamp_next_allowed_accel() - Missilemada2.getWorldTime()) / 3600.0);
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Building a "+s.getType()+" "+s.getId()+", cost "+(int)s.getCost()
            +", str "+s.getBattleStrIntDisp()+", str/cost "+(int)(0.01*s.getBattleStr()/s.getCost())+". -- "+aa_hrs+"h until ready.",2);
      }
      if (reason.equals("hacked")) {
        s.setTimeDelay(2*3600); //two hours delay from "enemy lost control of ship" to "we remote-control ship".
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Captured an enemy "+s.getType()+"(str "+s.getBattleStrIntDisp()+") by hacking!",2);
      }
      if (reason.equals("surrendered")) {
        s.setTimeDelay(800); //short delay coz defecting ones cooperate.
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "An enemy "+s.getType()+"(str "+s.getBattleStrIntDisp()+") surrendered! We will refit it.",2);
      }
    } else { //enemy debug print
          if (reason.equals("built")) {
            fac_ship_prod_timer_secs = s.getBuildTimeDelay(); //NOTE: delay only on BUILD.

            System.out.println("Enemy fac"+factionId+" built a "+s.getType()+" "+s.getId()+", cost "+(int)s.getCost()+", (str "+s.getBattleStrIntDisp()+")");

            //rand upon mil-ship built: go on the offensive
            if (s.isMil() && def_count >= 3 && Missilemada2.gimmeRandDouble() < 0.13) {
              setMode("SCOUT", "FLAG");
              setMode("MIL", "FLAG");
            }

          }
      if (reason.equals("hacked")) {
        s.setTimeDelay(2*3600); //two hours delay
        System.out.println("Enemy fac"+factionId+" Captured an enemy "+s.getType()+"(str "+s.getBattleStrIntDisp()+") by hacking!");
      }
      if (reason.equals("surrendered")) {
        s.setTimeDelay(800); //short delay coz defecting ones cooperate.
        System.out.println("Enemy fac"+factionId+" An enemy "+s.getType()+"(str "+s.getBattleStrIntDisp()+") surrendered! We will refit it.");
      }


    }
  }
  public int getFactionId() {
    return factionId;
  }
  public Color getShipColor() {
    if (factionId == 1) {
      return Color.getHSBColor(0.281f, 0.61f, 0.92f); //green
    } else if (factionId == 2) {
      return Color.getHSBColor(0.105f, 0.97f, 0.94f); //orange
    } else if (factionId == 3) {
      return Color.getHSBColor(0.16f, 0.73f, 0.91f); //yellow
    } else if (factionId == 4) {
      return Color.getHSBColor(0.48f, 0.70f, 0.95f); //greencyan
    } else if (factionId == 5) {
      return Color.getHSBColor(0.56f, 0.73f, 0.55f); //cyan
    } else if (factionId == 6) {
      return Color.getHSBColor(0.62f, 0.73f, 0.91f); //blue
    } else if (factionId == 7) {
      return Color.getHSBColor(0.715f, 0.73f, 0.91f); //purp
    } else if (factionId == 8) {
      return Color.getHSBColor(0.88f, 0.73f, 0.91f); //otherpurp
    } else {
      return Color.getHSBColor(0.97f, 0.1f, 0.25f); //red for 0 and >8
    }
  }
  public Color getMissileColor() {
    return getShipColor();
  }
  public int getMIDIInstrument() {
    //faction 0 is neutrals like asteroids
    if (factionId == 1) //player faction
      return 16; //drawbar organ
    if (factionId == 2)
      return 77; //shakuhachi
    if (factionId == 3)
      return 67; //baritone sax
    return 1;
  }
  public int num_scouted_aste() {
    return scoutreports_asteroids.size();
  }
  public void resupplyShuttleArrives() { //every 2 days. very stealth so enemy can't hurt it.
    //new crew came IF HAVE UNDER 21 IDLERS.
    if (crew_idle < 21) {
      this.addCrew(6);
      Missilemada2.updateBuildButtonColours();
      //vfx and HUD string if plr
      if (isPlayerFaction()) {
        Missilemada2.addVfx2(getXYZ_starbase_safer_side(), "RESUPPLYSHUTTLE", 460 * 45, 2910.0, 0.9/*transp*/, "resupplyshuttle.png", 1.0, "");
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "---RESUPPLY SHUTTLE brought 6 crew, some metals.---",2);
      }
    } else {
      //didn't bring crew
      if (isPlayerFaction()) {
        Missilemada2.addVfx2(getXYZ_starbase_safer_side(), "RESUPPLYSHUTTLE", 460 * 45, 2910.0, 0.9/*transp*/, "resupplyshuttle.png", 1.0, "");
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "---RESUPPLY SHUTTLE visited, didn't bring crew. (we have many)---",2);
      }
    }

    //shuttle brought some (25 tons) of the missing minerals (to prevent production stalling)
    if (lacking_resource.equals("FUEL")) //rare case
      resource_fuel = resource_fuel + 29.0;
    if (lacking_resource.equals("METAL1"))
      resource_metal1 = resource_metal1 + 30.0;
    if (lacking_resource.equals("METAL2"))
      resource_metal2 = resource_metal2 + 25.0;
    if (lacking_resource.equals("METAL3"))
      resource_metal3 = resource_metal3 + 25.0;
  }
  public void resupplyShuttleDeparts() { //every 2 days (172 800 sec), 4*3600 sec after it arrived.
    //shuttle hauls away 60 tons of the most plentiful resource
    //what is most plentiful?

    double avg_reso = resource_fuel + resource_metal1 + resource_metal2 + resource_metal3;
    if (resource_metal1 > 1.2 * avg_reso) {
      resource_metal1 = resource_metal1 - 60.0;
      return;
    }
    if (resource_metal2 > 1.2 * avg_reso) {
      resource_metal2 = resource_metal2 - 60.0;
      return;
    }
    if (resource_metal3 > 1.2 * avg_reso) {
      resource_metal3 = resource_metal3 - 60.0;
      return;
    }
    if (resource_fuel > 1.2 * avg_reso) {
      resource_fuel = resource_fuel - 60.0;
      return;
    }

  }

  public void advance_time(double seconds) {
    //every N (4 days?) time, save commander's xp stuff to file.
    savexp_timer = savexp_timer + seconds;
    if (savexp_timer > SAVEXP_EVERY_SECONDS) {
      if (isPlayerFaction()) {
        cmdr.saveToFile();
        savexp_timer = 0.0;
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "____Commander's progress has been noted. (saved to disk)____",0);
      }
    }
    //if don't know where to mine most needed mineral, nag msg.
    if (Missilemada2.gimmeRandDouble() < 0.002) {
      if (!knowWhereToMine(lacking_resource)) {
        Missilemada2.addToHUDMsgList("We don't know where to find that lacking resource!",0);
      }
    }

    //update timers of the undetectable resupply shuttle.
    resupply_arrivetimer = resupply_arrivetimer + seconds;
    resupply_departtimer = resupply_departtimer + seconds;
    if (resupply_arrivetimer > RESUPPLY_INTERVAL_SECONDS /*2 days*/) {
      resupplyShuttleArrives();
      resupply_arrivetimer = 0.0;
    }
    if (resupply_departtimer > RESUPPLY_INTERVAL_SECONDS /*2 days*/ + RESUPPLY_DOCKDURATION_SECONDS/*4h*/) {
      resupplyShuttleDeparts();
      resupply_departtimer = RESUPPLY_DOCKDURATION_SECONDS;
    }

    //starbase uses some resources
    resource_fuel = resource_fuel - 0.00002 * seconds;
    resource_metal1 = resource_metal1 - 0.000002 * seconds;

    // difficulty of AI, adjust! Can be negative adjustment too.
    if (this != Missilemada2.getPlayerFaction()) {
      //AI gets stuff from their zaibatsu
      resource_fuel = resource_fuel + seconds* DIFFICULTYADJ_AI_RESOGAIN;
      resource_metal1 = resource_metal1 + seconds* DIFFICULTYADJ_AI_RESOGAIN;
      resource_metal2 = resource_metal2 + seconds* DIFFICULTYADJ_AI_RESOGAIN;
      resource_metal3 = resource_metal3 + seconds* DIFFICULTYADJ_AI_RESOGAIN;
    }

    double prev_blaa = fac_ship_prod_timer_secs;
    fac_ship_prod_timer_secs = fac_ship_prod_timer_secs - seconds;
    if (fac_ship_prod_timer_secs < 1.0) {
      fac_ship_prod_timer_secs = 0.0;
      if (isPlayerFaction()
              && (prev_blaa - fac_ship_prod_timer_secs) > 2.0) {
        playCanBuildNow();
      }
    }

    //sporadically show where frontline is. if frontline is not at base.
    if (Missilemada2.gimmeRandDouble() < 0.00009 * seconds && isPlayerFaction()) {
      //if (MobileThing.calcDistanceVecVec(frontlineLocation, base.getXYZ()) > 0.1*Missilemada2.getSensorRangeMin())
        Missilemada2.addVfx2(frontlineLocation, "FRONTLINEFLAG", 460 * 45, 2910.0, 0.3/*transp*/, "frontline3.png", 1.0, "");
    }

    //if low on resources, scout wider? xxxx
    if (scouting_distance_avg < Missilemada2.getArrivedDistance())
      scouting_distance_avg = Missilemada2.getArrivedDistance();

    //expire/remove too-old aste scout reports
    int siz = scoutreports_asteroids.size();
    for (int i = 0; i < siz; i++) {
      ScoutReport sr = (ScoutReport) scoutreports_asteroids.elementAt(i);
      if (sr != null) {
        if (sr.timestamp + ASTE_FORGET_SECONDS < Missilemada2.getWorldTime()) {
          scoutreports_asteroids.remove(sr);
          Asteroid macc = (Asteroid)sr.item;
          siz = scoutreports_asteroids.size();
          //scouting_distance_avg = 0.98 * scouting_distance_avg; //need to RESCOUT some asteroids!
          addScoutingCandidateSpot(macc.getXYZ());
        }
      }
    }

    siz = scoutreports_ships.size();
    if (siz < 3 && isPlayerFaction()) {
      Missilemada2.changeWorldTimeIncrement(1); //speed up world coz plr sees not many foes.
    }
    boolean removed = false;
    for (int i = 0; i < siz; i++) {
      ScoutReport sr = (ScoutReport) scoutreports_ships.elementAt(i);
      if (sr != null) {
        if ((sr.timestamp + SHIP_FORGET_SECONDS) < Missilemada2.getWorldTime()) { //should expire very quickly.
          if (  ((Ship)sr.item).isStarbase() ) {
            //don't expire base spotting ever

            if (isPlayerFaction())
              ((Ship)sr.item).setIsSeenByPlayer(true);
          } else {
            scoutreports_ships.remove(sr);
            removed = true;
            siz = scoutreports_ships.size();
            //System.out.println("Faction "+factionId+" expired a ship_scoutreport.");
            if (isPlayerFaction()) {
              Missilemada2.changeWorldTimeIncrement(1); //speed up world
            }
          }
        }
      }
    }
    //if no more knowledge of enemy locations, are sort of in peace mode. play melody.
    if (removed && scoutreports_ships.size() < 1) { //xxxbuggy
      //if (isPlayerFaction())
      //  playFactionNoLongerInCombat();
    }

    //decide stuff, send orders to ships -- actually the ships ask for destination when they lack one.
    //Vector list = Missilemada2.getShipsOfFaction(this);

    //if friendly ship very near, repair and resupply it. (ship's code does that.)


    //maybe build ships, pretty rarely since resource getting is slow.
    if (Missilemada2.gimmeRandDouble() < (0.0001 * seconds) ) {
      if (this != Missilemada2.getPlayerFaction()) {
        tryShipProduction("", -1); //empty values to indicate AI mode
      } else {
        // player faction
        //no autobuild, that's for testing. tryShipProduction("", -1); //empty values to indicate AI mode
      }
    }
  }
  private boolean knowWhereToMine(String lacking_resource1) {
    Asteroid a = chooseKnownAsteroidToMine(lacking_resource1, getStarbase()); //ret rand aste or a known-good
    if (a == null) {
      Missilemada2.addToHUDMsgList("knowWhereToMine got a null asteroid", 0);
    } else {
      if (a.hasResource(lacking_resource1))
        return true;
    }
    return false;
  }
  public int getShipCount() {
  return curr_shipcount;
  }
  public String toString() {
    return "Faction "+getFactionId()+" has "+ curr_shipcount +" ships, "+scoutreports_asteroids.size()+" asteroid reports. Reso: " + resource_fuel + "   "+ resource_metal1+ "   "+ resource_metal2+ "   "+ resource_metal3;
  }
  public String factionResourcesAsString() {
    //xxxxxprettify.
    return "Fuel:" + (int)resource_fuel + "   M:"+ (int)resource_metal1+ "   Mx:"+ (int)resource_metal2+ "   Mq:"+ (int)resource_metal3;
  }
  public Vector getXYZ() {
    //works even if base is destroyed. coz we keep "base" pointer.
    return MobileThing.changeXYZ(base.getXYZ(), 0.0, 0.0, 0.0);
  }
  public Vector getFrontlineXYZ() { //exact center of frontline
    return frontlineLocation;
  }
  public Vector getFrontlineXYZ(String whichflank) { //no randomness.
    Vector baseloca = getXYZ();
    //right is minus radians, line from base to FL is d dist, a radi. SAME, but less radians.
    double dist_to_FL = MobileThing.calcDistanceVecVec(baseloca, getFrontlineXYZ());
    Asteroid tempAste = new Asteroid(100.0, frontlineLocation);
    double bearing_from_base_to_FL = Missilemada2.calcBearingXY(getStarbase(), tempAste);
    double flankbearing;
    if (whichflank.equals("LEFT")) { //xxx if close to base, should be wider separation.
      flankbearing = bearing_from_base_to_FL + 3.14 / 6.3;
    } else { // "RIGHT"
      flankbearing = bearing_from_base_to_FL - 3.14 / 6.3;
    }
    double bear_XZ = 0.0; //xx
    Vector flankspot = new Vector(4,3);
    flankspot.add(0, new Double((Double) (baseloca.get(0)) + (dist_to_FL * Math.cos(flankbearing))));
    flankspot.add(1, new Double((Double) (baseloca.get(1)) + (dist_to_FL * Math.sin(flankbearing))));
    flankspot.add(2, new Double((Double) (baseloca.get(2)) + (0.1 * dist_to_FL * Math.sin(bear_XZ)))); //note low desire of Z-axis change.
    return flankspot;
  }

  public Vector getXYZ_starbase_safer_side() { //no chance of null.
    //getXYZofShip_shifted_away_from_FL
    if (base.getHullPerc() > 0.01) { //if base alive
      return base.getXYZofShip_shifted_away_from_FL();
    } else {
      //base dead, exact coords will do.
      return getXYZ();
    }
  }
  public StarBase getStarbase() { //ret null if destroyed.
    //Faction will remember the base object pointer even if it is destroyed. But that obj gets no more advance_time calls from core loop. And is not a valid ship target.
    if (base.getHullPerc() > 0.01)
      return base;
    else
      return null;
  }
  public void playFactionEntersCombat() {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("battle", 5, "") /*Vector of pitches*/, 25 /*core note*/, 2 /**/, 70, 2.1F /*note duration*/);
  }
  public void playFactionNoLongerInCombat() {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("zilipatipippan", 5, "") /*Vector of pitches*/, 42 /*core note*/, 2 /**/, 40, 2.9F /*note duration*/);
  }
  public void playFactionOrderSound() {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("zot yay", 2, "") /*Vector of pitches*/, 28 /*core note*/, 47 /*timpani*/, 103, 4.9F /*note duration*/);
  }
  private void playFactionBuiltShip(String type, double cost) {
    if (cost > 9200) {
      //bonus celebratory notes for HEAVY SHIP.
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("a"+type, 6, "") /*Vector of pitches*/, 34 /*core note*/, 114 /*steeldrum*/, 104, 5.4F /*note duration*/);
    } else {
      //regular puny ship (elite scout or cheaper)
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("q"+type, 2, "") /*Vector of pitches*/, 36 /*core note*/, 114 /*steeldrum*/, 90, 4.3F /*note duration*/);
    }
  }
  private void playFactionBuildNoAfford(String type) {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("a"+type, 2, "") /*Vector of pitches*/, 49 /*core note*/, 114 /*steeldrum*/, 110, 4.0F /*note duration*/);
  }
  private void playCantBuildCozDelay() {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("zabusy", 2, "") /*Vector of pitches*/, 46 /*core note*/, 114 /*steeldrum*/, 80, 1.9F /*note duration*/);
  }
  private void playCanBuildNow() {
    //Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("canbuildnow", 2, "") /*Vector of pitches*/, 50 /*core note*/, 12 /*marimba*/, 72, 2.8F /*note duration*/);
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody(Missilemada2.getRandomDNA(), 3, "") /*Vector of pitches*/, 53 /*core note*/, 12 /*marimba*/, 72, 2.8F /*note duration*/);
  }
  private void playFactionScrappedYay() {
    Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("scrap get", 7, "") /*Vector of pitches*/, 34 /*core note*/, 113 /*agogo*/, 120, 2.9F /*note duration*/);
  }
  public Vector getFrontlineXYZ_vary(String a) {
    return getFrontlineXYZ_vary(a, 0.013);
  }
  public Vector getFrontlineXYZ_vary(String a, double vary) {
    if (getStarbase() == null)
      return null;
    Vector baseloca = getXYZ();
    Vector ret = null;

    if (a.equals("CENTER")) { //exact spot, then shift towards random spot.
      ret = MobileThing.changeXYZTowards(frontlineLocation,
            Missilemada2.getRandomLocationNear(frontlineLocation, 0.5 * Missilemada2.getMissileRangeMax(), vary),
            0.12 * Missilemada2.getBaseDistance());
      if (isPlayerFaction())
        Missilemada2.addVfx2(ret, "aaa", (int) (5.7 * 60 * 60), 200, 0.35/*transp*/, "qm_green.png", 1.0, "");
    }
    if (a.equals("LEFT") || a.equals("RIGHT")) {
      Vector flankspot = getFrontlineXYZ(a);
      ret = MobileThing.changeXYZTowards(/*from*/ flankspot,
              /*to*/ Missilemada2.getRandomLocationNear(flankspot, 0.4 * Missilemada2.getMissileRangeMax(), vary),
              0.18 * Missilemada2.getBaseDistance());
      if (isPlayerFaction())
        Missilemada2.addVfx2(ret, "aaa", (int) (5.7 * 60 * 60), 150, 0.35/*transp*/, "qm_blue.png", 1.0, "");
    }
    return ret;
  }
  public void OBSOLETE_orderShipsTo(Vector to_xyz, String who, double in_scatter_amount) { //NONCOMBAT, COMBAT ships.
    //pls scattered, not stacked.
    Vector list = Missilemada2.getShipsOfFaction(this);
    Ship s = null;
      for (int i = 0; i < list.size(); i++) {
        double rand_dist_varix = 0.3 * 40000.0 * (Missilemada2.gimmeRandDouble() - 0.5);
        double rand_dist_variy = 0.3 * 40000.0 * (Missilemada2.gimmeRandDouble() - 0.5);
        double rand_dist_variz = 0.3 * 40000.0 * (Missilemada2.gimmeRandDouble() - 0.5);
        s = (Ship) list.elementAt(i);
        if (s != null) {
          if (s.isStarbase())
            continue;

          if (who.equals("COMBAT")) {
            if (s.getType().equals("SCOUT") || s.getType().equals("DEFENDER") || s.getType().equals("AC")
                    || s.getType().equals("MISSILEDRONE") || s.getType().equals("BEAMDRONE")) {

              if (s.isTooDamagedToObeyFrontline()  /*not the highly damaged ones to frontline!*/
                      || (s.getType().equals("MISSILEDRONE") && s.getMissileStorePerc() < 0.5) ) { //not empty missiledrone to frontline!

                if (isPlayerFaction())
                  Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + s.getType() + " " + s.getId() + " refused order, need repairs or resupply.",0);
                continue; //skip to next for_iteration.
              }
              if (in_scatter_amount < 0.05) {
                s.forceDestination(to_xyz, "");
              } else {
                double temp_scatter_amount = in_scatter_amount; //temp ver of variable was needed otherwise 15 SCOUTs boosted next ones' shit a lot.
                //if scout, wider place in frontline.
                if (s.getType().equals("SCOUT"))
                  temp_scatter_amount = in_scatter_amount * 1.6;

                s.forceDestination(MobileThing.changeXYZ(to_xyz, temp_scatter_amount * rand_dist_varix, temp_scatter_amount * rand_dist_variy, temp_scatter_amount * rand_dist_variz), "");
              }
            }
          } else {
            //NONCOMBAT ships order.
            if (s.getType().equals("MINER") || s.getType().equals("TINYMINER")) {
              //if (s.isTooDamagedToObeyFrontline())
              //  continue;
              if (in_scatter_amount < 0.05)
                s.forceDestination(to_xyz, "");
              else
                s.forceDestination(MobileThing.changeXYZ(to_xyz, in_scatter_amount * rand_dist_varix, in_scatter_amount * rand_dist_variy, in_scatter_amount * rand_dist_variz), "");

            }
          }
        }
      }
  }
  public void receiveCargo(String reso, Double amt) {
    if (getStarbase() == null)
      return;

    drawFrontLineIndicator(1710.0 /*size*/, 0.09/*chance*/);

    //System.out.println("Faction " + factionId + " received "+amt+" tons of "+reso);
    if (reso != null && amt != null) {
      if (amt < 0.01)
        throw new NullPointerException("hhh");

      if (isPlayerFaction()) {
        Missilemada2.updateBuildButtonColours(); //update coz can build more stuff now.
      }

      //scouting_distance_avg = scouting_distance_avg - 150.0; //we got some probably-wanted stuff, no need to scout wider and get discovered.


      if (reso.equals("FUEL"))
        resource_fuel = resource_fuel + amt;
      if (reso.equals("METAL1"))
        resource_metal1 = resource_metal1 + amt;
      if (reso.equals("METAL2"))
        resource_metal2 = resource_metal2 + amt;
      if (reso.equals("METAL3"))
        resource_metal3 = resource_metal3 + amt;
    } else {
      //
    }
  }

  public double getScouting_distance_avg() {
    return scouting_distance_avg;
  }
  private boolean isXYZNearFrontline(Vector xyz) {
    if (MobileThing.calcDistanceVecVec(xyz, frontlineLocation) < 0.15 * Missilemada2.getBaseDistance()) {
      return true;
    }
    return false;
  }
  public Vector shipRequestsDestination(Ship asker, String why_ask /*for debug*/) {
    //----ships' NEAR and FAR modes ask for scouting spots/asteroids/faction_remembered_enemies.


    Vector ret = Missilemada2.getRandomLocationNear(base.getXYZ(), scouting_distance_avg, 0.09); //BASE vicinity and gradually incr range.
    //scouting_distance_avg = scouting_distance_avg + 100.0;
    //null default was a bit shit

    //if base been destroyed, give default spot near the destroyed base.
    if (getStarbase() == null)
      return ret;

    if (asker.isStarbase())
      return null;

    //xxBRANCH ON SHIP TYPE AND HEALTH and known enemy locations... safe mining thataway. combat ships thataway.

    //repairs use getDockingXYZ()
    //when cowering, use base's core xyz.

    if (asker.getType().equals("SCOUT")) {
      String scoutmode = asker.parentFaction.getMode("SCOUT");
      if (asker.cargo_full) {
        ret = base.getDockingXYZ();
      } else {
        //have cargo space, MAYBE do a mining.
        if (Missilemada2.gimmeRandDouble() < 0.10 && scoutmode.equals("NEAR")) {
                  Asteroid as = this.chooseKnownAsteroidToMine("", asker);
                  if (as != null) {
                    ret = as.getMiningXYZ();
                    debugVFX_Text(ret, "scout2mining");
                    //System.out.println("scout-ship "+ asker.unique_id + " got goto_asteroid_for_mining desti from faction.");
                  } else {
                    //no good aste, go to .
                    ret = getScoutingCandidateSpot();
                    //ret = Missilemada2.getRandomLocationNear(base.getDockingXYZ(), scouting_distance_avg, 0.5); //base vicinity
                    debugVFX_Text(ret, "sc2sc_candi_couldntmine");
                    scouting_distance_avg = scouting_distance_avg + 2.0;
                  }
        } else {
          //default, non-mining destination for scout.
          ret = getScoutingCandidateSpot();//xxxxxx use NEAR/FAR somehow...

          //if ret is too far from base, X.
          //chekc for nulls     //double dist = MobileThing.calcDistanceVecVec(getXYZ(), ret);

          //debugVFX_Text(ret, "sc2sc_candi");
          //System.out.println("scout-ship "+ asker.unique_id + " got candi scouting spot from faction.");
          scouting_distance_avg = scouting_distance_avg + 4.0;
        }
      }
    }

    if (asker.getType().equals("MINER") || asker.getType().equals("TINYMINER")) {
      //minermode irrelevant here. not staying at base if miner called this func.
      Asteroid as = this.chooseKnownAsteroidToMine(""/*not ANY*/, asker);
      if (as != null) {
        ret = as.getMiningXYZ();

        //is as near frontline, DON'T.
        if ((!isFrontlineNearBase()) && isXYZNearFrontline(as.getXYZ()))
          ret = null;
        debugVFX_Text(ret, "mi2as");
      } else { //wanted as was null, try grab unwanted stuff then.
        as = this.chooseKnownAsteroidToMine("ANY", asker);
        if (as != null) {
          //goto unwanted-minerals aste. IF NOT SUPER FAR.

          ret = as.getXYZ();
          //is as near frontline, DON'T.
          if ((!isFrontlineNearBase()) && isXYZNearFrontline(as.getXYZ()))
            ret = null;
          //if gotten ANY_as far from asker-ship, DON'T. Rather wait for a desired match or nearby-ANYas.
          if (MobileThing.calcDistanceVecVec(asker.getXYZ(), ret) > 1.5 * asker.getSensorRange())
            ret = null;

          if (ret != null) {
            debugVFX_Text(ret, "mi2ANYas");
            scouting_distance_avg = 1.000002*scouting_distance_avg + 100.0;
          }

        } else {
          // no resource aste known, trouble!
          scouting_distance_avg = 1.00002*scouting_distance_avg + 300.0;
          //ret = getStarbase().getDockingXYZ(); //xxxx bad coz forever arrive at base!
          if (asker.getType().equals("MINER") && !asker.hasDesti()) {
            ret = Missilemada2.getRandomLocationNear(base.getDockingXYZ(), 0.4*scouting_distance_avg, 0.4); //base vicinity
            debugVFX_Text(ret, "mi2nearbasecozhasno_astedesti");
          } else { //tinyminer, don't scout, you're a dumb drone.
            ret = getXYZ_starbase_safer_side();
          //scouting_distance_avg = scouting_distance_avg + 20.0;
          }


/*
          Ship buddy = ship.getBuddy();
          if (buddy != null) {
            ret = null;  //MobileThing.changeXYZ(buddy.getXYZ(), 2.5*scouting_distance_avg, 0.0, 0.0); //east, right side of buddy.
          } else {//not even a buddy near at start??
            ret = Missilemada2.getRandomLocationNear(this.getXYZ(), scouting_distance_avg, 0.1); //base vicinity
            scouting_distance_avg = 1.000005*scouting_distance_avg + 100.0;

          }
*/
        }
      }
//      if (ret == null) { //miner default
//        //XXXXX was very spammy sometimes. at start.
//
//        ret = getScoutingCandidateSpot();
//        //WAS 50% of cpu timeeeeeeee
//        addScoutingCandidateSpot(ret); //tinyminer not exhaust the list pls
//      }
    }

    //----mil-ships (not scouts). is non-combat time now.
    if (asker.getType().equals("DEFENDER")
     || asker.getType().equals("AC")
     || asker.getType().equals("MISSILEDRONE")
     || asker.getType().equals("BEAMDRONE")) {
      String milmode = asker.parentFaction.getMode("MIL");
      if (milmode.equals("BASE")) {
        ret = Missilemada2.getRandomLocationNear(this.getXYZ(), 0.06 * Missilemada2.getBaseDistance(), 0.3);
        return ret;
      }
      if (milmode.equals("MINERS")) {
        Ship a_miner = asker.getMinerToProtect();
        if (a_miner != null) {
          ret = a_miner.getXYZPredicted(1400);
          return ret;
        }
      }
      if (milmode.equals("FLAG") || milmode.equals("FLAGLEFT") || milmode.equals("FLAGRIGHT")) {
        if (asker.isDrone()) {
          ret = null; //drone should follow milbuddy
        } else {
          //manned mil-ships
          //xxxxxx code in progress
          if (milmode.equals("FLAG"))
            ret = Missilemada2.getRandomLocationNear(frontlineLocation, 0.06 * Missilemada2.getBaseDistance(), 0.3);
          if (milmode.equals("FLAGLEFT"))
            ret = Missilemada2.getRandomLocationNear(getFrontlineXYZ("LEFT"), 0.06 * Missilemada2.getBaseDistance(), 0.3);
          if (milmode.equals("FLAGRIGHT"))
            ret = Missilemada2.getRandomLocationNear(getFrontlineXYZ("RIGHT"), 0.06 * Missilemada2.getBaseDistance(), 0.3);
          //  ret = MobileThing.changeXYZTowards(ret, getXYZ(), 900.0);
          return ret;
        }
      }
      if (milmode.equals("NEAR") || milmode.equals("FAR")) { //the old logic, to known foe locations or scout.
        if (asker.isDrone()) {
          ret = null; //drone should follow milbuddy
        } else {
          //manned mil-ships
          //Do: goto ene ship reported location. xxxxxx alone is bad
          Ship ene = null;
          if (scoutreports_ships.size() > 0) { //if know foe locations

            //for loop, nearest OR deadliest OR weakest... or miner-botheringest



            int siz = scoutreports_ships.size();
            for (int i = 0; i < siz; i++) {
              ScoutReport sr = (ScoutReport) scoutreports_ships.elementAt(i);
              if (sr != null) {
                ene = (Ship) sr.item;
                //if break for, oldest report. if not break, newest report.
              }
            }
            double randsign = Math.signum((Missilemada2.gimmeRandDouble() - 0.5));
            double dist = Missilemada2.getCombatDistMin(asker);
            //System.out.println("MIL-ship "+ asker.unique_id + " got goto_enemy desti from faction.");
            //ret = MobileThing.changeXYZ(ene.getXYZ(), randsign*dist, randsign*dist, randsign*100.0);
            if (ene != null && asker != null) {
              if (asker.getBattleStrPerc() > 0.9) {
                ret = MobileThing.calcRelativeStanceXYZ(asker.getXYZ(), ene.getXYZ(),
                        asker.getMyMissileDistKnown(), asker.getAttackBeamRange(),
                        asker.getStanceLR(), asker.getStanceNearFar());
                //if (Missilemada2.gimmeRandDouble() < 0.5)
                debugVFX_Text(ret, "mil2ene_stanced_fac");
              } else {
                //would go to foe, but too damaged.
              }
            }
          } else { // faction knows no enemy locations. scout NEAR or FAR.




            //if frontline is at base, meh no fun. scout or escort.
            if (isFrontlineNearBase()) {
              //Do: scout like a scout. but less variation.
              ret = getScoutingCandidateSpot(); //for mil
              debugVFX_Text(ret, "mil2candi");
              //System.out.println("MIL-ship "+ asker.unique_id + " got scout candi desti from faction.");

              //pick escorteeship to follow, from list.
              //xxxxxxxxxxxxxx may be shit when escortee is docking!!!!!!!
//          ret = null;
//          Vector list = Missilemada2.getShipsOfFaction(this);
//
//          Ship mil_buddy;
//          for (int i = 0; i < list.size(); i++) {
//            mil_buddy = (Ship) list.elementAt(i);
//            if (mil_buddy != null) {
//              //follow a strong one. not a puny one. DONT FOLLOW STARBASE!
//              //if (mil_buddy.getType().equals("DEFENDER") || mil_buddy.getType().equals("AC")) {
//              if (!mil_buddy.xxxxgetType().equals("STARBASE")
//                      && mil_buddy.getBattleStr() > 1.2 * asker.getBattleStr()
//                      && !mil_buddy.isAtFactionBase()) {
//                //System.out.println("MIL-ship "+ asker.unique_id + " got goto_strong_mil_buddy desti from faction.");
//                //ret = MobileThing.changeXYZ(mil_buddy.getXYZ(), Missilemada2.getArrivedDistance() + 1.1 * Missilemada2.getMiningDistance() * (0.1+Missilemada2.gimmeRandDouble()), 0.0, 0.0);
//                ret = MobileThing.calcRelativeStanceXYZ(asker.getXYZ(), mil_buddy.getXYZ(),
//                        0.35*asker.getSensorRange(), 0.4*asker.getDefenseBeamRange(),
//                        asker.getStanceLR(), 0.95/*near buddy*/   /*asker.getStanceNearFar()*/);
//              }
//            }
//          }
//          //if not return, you might be the strongest one of whole faction. go to... random.
//          if (ret == null) {
//            ret = getScoutingCandidateSpot(); //for mil
//          }


            } else {
              //go to frontline, which is not near base.

              //spot near frontline, so don't stupidly SPIN AT FRONTLINE SPOT.

              //xxxxx stupidly going ALONE to deadly frontline...
              //ret = MobileThing.changeXYZTowards(getXYZ(), getFrontlineXYZ(), 0.3*Missilemada2.getBaseDistance());
              ret = getFrontlineXYZ_vary("CENTER");

              //add randomness, so not in queue formation to frontline...
              double scatter_amount = (2.1) * (Missilemada2.gimmeRandDouble() - 0.5);
              double rand_dist_vari = 0.4 * Missilemada2.getCombatDistMin_Generic();
              ret = MobileThing.changeXYZ(ret, scatter_amount*rand_dist_vari, scatter_amount*rand_dist_vari, scatter_amount*rand_dist_vari);
              debugVFX_Text(ret, "mil2FL");

              //ret = Missilemada2.getRandomLocationNear(getFrontlineXYZ(), 3.8*Missilemada2.getArrivedDistance(), 2.0);

              //System.out.println("MIL-ship "+ asker.unique_id + " got goto_frontline desti from faction.");
            }


            //  ret = Missilemada2.getRandomLocationNear(base.getDockingXYZ(), scouting_distance_avg, 0.5); //base vicinity


            //Do: scout like a scout. but less variation.
            //ret = getScoutingCandidateSpot();

//        if (anyScout != null)
//          ret = Missilemada2.getRandomLocationNear(anyScout.getXYZ(), scouting_distance_avg, 0.1); //a scout's vicinity
//        else
//          ret = Missilemada2.getRandomLocationNear(base.getDockingXYZ(), scouting_distance_avg, 0.5); //base vicinity


          }//end no-spotted-enemies
        }//end manned
      }//end nearfar
    } //end mil-type

    if (ret == null) {
      if (isPlayerFaction())
        System.out.println("Faction "+name+" DIDN'T GIVE "+asker.toStringShort()+ " a destination. Wobble near base now.");

      ret = Missilemada2.getRandomLocationNear(getStarbase().getDockingXYZ(), 0.4 * base.getSensorRange(), 0.001);
      debugVFX_Text(ret, "any2dock");
    } else {
      //if (Missilemada2.getPlayerFaction() == asker.getFaction() && ret != null)
      //  Missilemada2.addVfx2(ret, "DESTINATION_GIVEN", 120*90, 510.0, 0.5/*transp*/, null, "32texturecyanx.png", 1.0, "");
    }
    return ret;
  }
  private boolean isFrontlineNearBase() {
    return (MobileThing.calcDistanceVecVec(getFrontlineXYZ(), base.getXYZ()) < 6.0 * Missilemada2.getNearlyArrivedDistance());
  }
  public Asteroid chooseKnownAsteroidToMine(String whatkind, Ship asker) { //took 33% of cpu time duing big battle! may be fixed now.
    lacking_resource = predictResourceLack_Str(); //xxxhack

    Asteroid ret_as = null;
    //for known aste list
    int siz = scoutreports_asteroids.size();
    if (siz > 0) {
    for (int i = siz-1; i > 0; i--) { //xxxx try reverse order, older is better---
      ScoutReport sr = (ScoutReport) scoutreports_asteroids.elementAt(i);
      if (sr != null) {
        Asteroid as = (Asteroid) sr.item;


        //param "ANY": if we get whatever is available: rarest first
        if (whatkind.equals("ANY")) {
              //System.out.println("Faction "+factionId+ " don't know where to find wanted resource, so getting any old reso by rarity choice.");
              //TOO FAR scouting_distance_avg = scouting_distance_avg + 20.0;

              if (as.hasWantedResource("METAL3")) {
                ret_as = as;
              } else if (as.hasWantedResource("METAL2")) {
                ret_as = as;
              } else if (as.hasWantedResource("METAL1")) {
                ret_as = as;
              } else if (as.hasWantedResource("FUEL")) {
                ret_as = as;
              }
        } else { //not ANY, want specific.
          //if we want stuff: inspect for rarest first
          if (as.hasWantedResource("METAL3") && this.wantsResource_lacking("METAL3")) {
            if (ret_as == null) {
              ret_as = as;
            } else { //check if closer approved asteroid, so don't go to farthest/recent-reportest one.
              if (MobileThing.calcDistanceMTMT(base, as) < MobileThing.calcDistanceMTMT(base, ret_as) && !as.isNearBattle())
                ret_as = as;
            }
          }
          else if (as.hasWantedResource("METAL2") && this.wantsResource_lacking("METAL2")) {
            if (ret_as == null) {
              ret_as = as;
            } else { //check if closer approved asteroid, so don't go to farthest/recent-reportest one.
              if (MobileThing.calcDistanceMTMT(base, as) < MobileThing.calcDistanceMTMT(base, ret_as) && !as.isNearBattle())
                ret_as = as;
            }
          }
          else if (as.hasWantedResource("METAL1") && this.wantsResource_lacking("METAL1")) {
            if (ret_as == null) {
              ret_as = as;
            } else { //check if closer approved asteroid, so don't go to farthest/recent-reportest one.
              if (MobileThing.calcDistanceMTMT(base, as) < MobileThing.calcDistanceMTMT(base, ret_as) && !as.isNearBattle())
                ret_as = as;
            }
          }
          else if (as.hasWantedResource("FUEL") && this.wantsResource_lacking("FUEL")) {
            if (ret_as == null) {
              ret_as = as;
            } else { //check if closer approved asteroid, so don't go to farthest/recent-reportest one.
              if (MobileThing.calcDistanceMTMT(base, as) < MobileThing.calcDistanceMTMT(base, ret_as) && !as.isNearBattle())
                ret_as = as;
            }
          }
          //if have okay one, curb scouting.
          //and add text vfx on that aste.
          if (ret_as != null) {
            scouting_distance_avg = scouting_distance_avg - 1.0;
            Missilemada2.addVfxOnMT(0, -5 * ret_as.getRadius(), 0, "TEXT", 19000, 900.0, 1.0, ret_as, "", 1.0, "has the wanted");
          }
        }

        //if asteroid is in combat zone, do not send miner there!
        if (ret_as != null) {
          //if as close to frontline, try some other aste.
          double dist_aste_to_frontline = MobileThing.calcDistanceVecVec(ret_as.getXYZ(), frontlineLocation);
          double dist_aste_to_base      = MobileThing.calcDistanceVecVec(ret_as.getXYZ(), getXYZ());
          double dist_base_to_frontline = MobileThing.calcDistanceVecVec(this.getXYZ(), frontlineLocation);
          double dist_aste_to_asker     = MobileThing.calcDistanceVecVec(ret_as.getXYZ(), asker.getXYZ());

          //if FL near base, meh.
          if (dist_base_to_frontline < 0.08 * Missilemada2.getBaseDistance()) {
            //meh, don't know a proper frontline
            //thus, don't reject this aste.
          } else {
            //let's not return an aste that is near frontline.
            if (dist_aste_to_frontline < 0.5 * (1.5*Missilemada2.getSensorRangeMinShip()) ) { //use standardised range, not from asking_ship.
              ret_as = null; //try some other aste, on next iteration of loop.
            }
          }

          //xxmaybebad random: occasionally reject for no reason, to give other asteroids a chance.
          //if (Missilemada2.gimmeRandDouble() < 0.07) {
          //  ret_as = null;
          //}

          //xxmaybebad random: occasionally reject coz too far.
          //if (Missilemada2.gimmeRandDouble() < 0.09 && dist_aste_to_base > 0.3 * Missilemada2.getBaseDistance()) {
            //ret_as = null;
          //}

          //if aste tagged as combat zone, try some other aste.
          if (ret_as != null) {
            if (ret_as.isNearBattle()
                || isLocationHostile(ret_as.getXYZ(), 0.9*Missilemada2.getSensorRangeMaxShip_something()) ) {
              //scouting_distance_avg = scouting_distance_avg - 5.0;
              //System.out.println("Faction "+factionId + " not send a miner to asteroid "+ret_as.getId()+", because it's in a combat zone.");
              if (isPlayerFaction()) {
                if (Missilemada2.gimmeRandDouble() < 0.02) { //curb spam
                  Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + " Asteroid "+ret_as.getId()+", too contested location for miners.");
                  Missilemada2.addVfx2(ret_as.getXYZ(), "ASTE IS BATTLEZONE, REJECTED", 16000, 3150.0, 0.7/*transp*/, "battlestart.png", 1.0, "");
                }
              }
              ret_as = null; //try some other aste, on next iteration of loop.
            }
          }
        }
      } //else report is null, error.
    } //end for

    } else {
      //no asteroid reports! rare!
      if (isPlayerFaction()) {
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + " Zero analysed asteroids! Sending "+asker.toStringShort()+" to whichever asteroid.",0);
      }
      changeScoutingDist(1.005);
      return Missilemada2.getRandomAsteroid();
    }

    //what, no wanted-reso aste nearby? Scout harder.
    if (siz > 12 && ret_as == null)
      changeScoutingDist(1.009);

    //xxhack: don't go to super far one.
//    if (ret_as != null) {
//      if (MobileThing.calcDistanceVecVec(getXYZ(), ret_as.getXYZ()) > 1.8*Missilemada2.getBaseDistance())
//        ret_as = null;
//    } else {
//      //ret_as is null, faction has a lack-of-scouting problem.
//
//    }

    if (ret_as != null) {
      //double cal = MobileThing.calcDistanceVecVec(getXYZ(), ret_as.getXYZ());
      if (isPlayerFaction()) {
        //Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + " sending a miner to aste"+ret_as.getId()+", distfrombase="+cal);
        Missilemada2.addVfx2(ret_as.getXYZ(), "ASTE IS OKAY FOR MINING", 16000, 1150.0, 0.1/*transp*/, "round_green.png", 1.0, "");
      }
    }
    return ret_as;
  }
  private boolean wantsResourceOLD(String res) {
    if (res.equals("METAL1") && resource_metal1 < M1MIN)
      return true;
    if (res.equals("METAL2") && resource_metal2 < M2MIN)
      return true;
    if (res.equals("METAL3") && resource_metal3 < M3MIN)
      return true;
    if (res.equals("FUEL") && resource_fuel < FUELMIN)
      return true;

    //System.out.println("Whoa! Faction "+factionId + " doesn't want "+ res);
    return false;
  }
  public boolean wantsResource_lacking(String res) {
    if (res.equals(lacking_resource))
      return true;
    else
      return false;
  }
  public void addScoutReportEnemyShip(Ship reportingscout, ScoutReport sr) { //called by ship.usesensors and defecting.
  //xxx if sensat, less merit, less commotion.
    if (!isShipScouted( (Ship)sr.item )) {
      scoutreports_ships.add(sr);
      reportingscout.addMerit(0.057); //0.166 WAY TOO MUCH

      if (((Ship) sr.item).isStarbase()) {
        addScoutReportBase(reportingscout, sr);
        reportingscout.addMerit(0.110); //0.166 WAY TOO MUCH
      } else { //regular ship
        //melody of enemy sighted. CONSTANTLY? maybe add a timer.
//      if (this.isPlayerFaction())
//        Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("enemy spotted", 2, "") /*Vector of pitches*/, 49 /*core note*/, 30 /*dist guit*/, 70, 2.3F /*note duration*/);

      }

      //if first sighting, melody of battle.
      if (scoutreports_ships.size() == 1) {
        if (isPlayerFaction())
          playFactionEntersCombat();
      }
      //if few sighting and FL near base, shiftFrontLine() FL lots.
      if (scoutreports_ships.size() < 4 && getDistanceBetwBaseAndFrontline() < 0.1 * Missilemada2.getBaseDistance()) {
        shiftFrontLine( ((Ship)sr.item).getXYZ(), 20.0, reportingscout);
      }

      scouting_distance_avg = scouting_distance_avg - 0.01 * Missilemada2.getSensorRangeMinShip(); //increase scouting's caution.
      if (scouting_distance_avg < Missilemada2.getNearlyArrivedDistance())
        scouting_distance_avg = Missilemada2.getNearlyArrivedDistance();

      if (isPlayerFaction()) {
        Missilemada2.changeWorldTimeIncrement(-3); //slow down world, because combat begins.
      }
    } else {
      //was already in reports, just a location update(???), no merit.

      ///scoutreports_ships.add(sr); //YYYY BAD, causes much cpu load on superlarge SR_list

      if (isPlayerFaction())
        Missilemada2.changeWorldTimeIncrement(-1); //slow down world
    }
  }
  public void addScoutReportAste(Ship reportingscout, ScoutReport sr, Asteroid as) {
    drawFrontLineIndicator(2910.0 /*size*/, 1.001/*chance*/);
    //if (this.isPlayerFaction() && !((Asteroid)sr.item).isResourceless()) {
    //  Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("scanner"/*sr.item.toString()*/, 4, "") /*Vector of pitches*/, 57 /*core note*/, 30 /*dist guit*/, 40, 1.71F /*note duration*/);
    //}

    //scouting_distance_avg = 1.00004 * scouting_distance_avg; //increasing on each scout_wantdestination
    //System.out.println("SR of Aste: Faction "+getFactionId()+" incr scoutdist to "+ scouting_distance_avg);

    //WILL have duplicates.

    //get merit if aste was not known.
    if (!Missilemada2.isAsteroidKnownToFaction(as, this))
      reportingscout.addMerit(0.12);

    if (scoutreports_asteroids.size() < 200)
      scoutreports_asteroids.add(sr);
  }
  public void addScoutReportBase(Ship reportingscout, ScoutReport sr) {
    scoutreports_bases.add(sr);
    reportingscout.addMerit(2);
    //melody of enemy BASE sighted.
    //xxxx big-ass vfx.
    if (this.isPlayerFaction())
      Missilemada2.putMelodyNotes(Missilemada2.strIntoMelody("enemy base spotted", 16, "") /*Vector of pitches*/, 50 /*core note*/, 56 /*instrument*/, 90, 0.9F /*note duration*/);
  }
  public boolean isAsteroidScouted(Asteroid a) {
    int siz = scoutreports_asteroids.size();
    for (int i = 0; i < siz; i++) {
      ScoutReport sr = (ScoutReport) scoutreports_asteroids.elementAt(i);
      if (sr != null) {
        Asteroid as = (Asteroid) sr.item;
        if (as.getId() == a.getId())
          return true;
      }
    }
    return false;
  }
  public boolean isShipScouted(Ship a) {
    if (a.getFaction() == this) //own faction ships are always known.
      return true;

    int siz = scoutreports_ships.size();
    for (int i = 0; i < siz; i++) {
      ScoutReport sr = (ScoutReport) scoutreports_ships.elementAt(i);
      if (sr != null) {
        Ship as = (Ship) sr.item;
        if (as.getId() == a.getId())
          return true;
      }
    }
    return false;
  }
  public boolean isLocationHostile(Vector xyz, double radius_from_foe_ship) {
  //xxxxxxxxxxxx use knowledge of known foe base locations

  //avg sensor range versus spotted foe ship locations.
    int siz = scoutreports_ships.size();
    for (int i = 0; i < siz; i++) {
      ScoutReport sr = (ScoutReport) scoutreports_ships.elementAt(i);
      if (sr != null) {
        Ship foe = (Ship) sr.item;
        //if tested ship is too near xyz, then xyz is hostile spot.
        if (MobileThing.calcDistanceVecVec(foe.getXYZ(), xyz) < radius_from_foe_ship)
          return true;
      }
    }

    return false;
  }
  public boolean areAllResourceMinimumsSatisfied() {
    if ((resource_fuel > FUELMIN) && (resource_metal1 > M1MIN)
     && (resource_metal2 > M2MIN) && (resource_metal3 > M3MIN)) {
      return true;
    }
    return false;
  }
  private void increaseTypeWantCount() {
    //random or personality, xxxxxxxx
    if (curr_wanted_count_defender < 6)
      curr_wanted_count_defender++;

    if (curr_wanted_count_scout < 11)
      curr_wanted_count_scout++;

    if (curr_wanted_count_tinyminer < 16)
      curr_wanted_count_tinyminer++;

  }
  public void addStarterResources(double amt_fuel) {
    this.addCrew(4+2+2); //enough for miner, scout, scout.
    resource_fuel = amt_fuel;
    resource_metal1 = 0.55*amt_fuel;
    resource_metal2 = 0.4*amt_fuel;
    resource_metal3 = 0.29*amt_fuel;
  }
  public void addStarterShips() { //first must have starbase set, for getXYZ().
    //MUST HAVE enough idle crew for this func to work okay.
    this.addCrew(4 + 5 + 2 + 2);

    if (isPlayerFaction()) {
      Missilemada2.addToHUDMsgList("addStarterShips: before ship adding, "+crew_idle+" crew idle, "+crew_alive+" crew alive. ",0);
    }

      //MUST HAVE MINING SHIPS or enough resources, otherwise you start with gameover!
    Vector xyz;
    boolean added_ok = false;

    System.out.println("Adding starter ships for faction "+name+". crew_alive: "+ crew_alive);
    xyz = Missilemada2.getRandomLocationNear(this.getXYZ(), 99000.0, 0.54);
    added_ok = Missilemada2.addToShipList_withcrewcheck(new Ship("MINER", this, xyz, "a", "starship.png", true/*needs_crew*/)); miner_count++;

    //xyz = Missilemada2.getRandomLocationNear(this.getXYZ(), 99000.0, 0.54);
    //added_ok = Missilemada2.addToShipList_withcrewcheck(new Ship("AC", this, xyz, "g", "starship.png", true/*needs_crew*/)); ac_count++;
    xyz = Missilemada2.getRandomLocationNear(this.getXYZ(), 99000.0, 0.54);
    added_ok = Missilemada2.addToShipList_withcrewcheck(new Ship("DEFENDER", this, xyz, "a", "starship.png", true/*needs_crew*/)); def_count++;

    xyz = Missilemada2.getRandomLocationNear(this.getXYZ(), 99000.0, 0.54);
    added_ok = Missilemada2.addToShipList_withcrewcheck(new Ship("SCOUT", this, xyz, "q", "starship.png", true/*needs_crew*/)); scout_count++;

    xyz = Missilemada2.getRandomLocationNear(this.getXYZ(), 99000.0, 0.54);
    added_ok = Missilemada2.addToShipList_withcrewcheck(new Ship("SCOUT", this, xyz, "a", "starship.png", true/*needs_crew*/)); scout_count++;

    if (added_ok == false) {
      throw new NullPointerException("addstarterships: not enough crew, catastrophe.");
    }

    curr_shipcount = Missilemada2.getShipsOfFaction(this).size();
    if (isPlayerFaction()) {
      Missilemada2.addToHUDMsgList("addStarterShips: after ship adding, "+crew_idle+" crew idle, "+crew_alive+" crew alive. ",0);
      Missilemada2.updateBuildButtonColours();
    }

  }
  private boolean isPlayerFaction() {
    return (this == Missilemada2.getPlayerFaction());
  }
  public void addScoutingCandidateSpot(Vector xyz) {
    if (scoutingCandidateSpots.size() < 25)
      scoutingCandidateSpots.add(xyz); //was super cpu intensive without (if size)!
  }
  public Vector getScoutingSpotMIL(String nearfar, Ship asker) {
    if (asker.isDrone())
      throw new NullPointerException("yreyu");

    Vector ret = null;
    int loopbreaker = 0;
    double toofar;
    while (ret == null && loopbreaker < 500) {
      loopbreaker++;
      if (nearfar.equals("FAR")) {
          //if know any ene ship loca, scout there! Can be very far.
          if (hasEnemySightings()) {  //far mode, to enemy sighting.
            ret = Missilemada2.getRandomLocationNear(getXYZFromAnyShipScoutReport(), 0.3*asker.getSensorRange(), 0.05);
            toofar = 1.35*scouting_distance_avg; //i dont understand xxx
          } else { //scout, rand spot at scoutdist, centre is base
            ret = Missilemada2.getRandomLocationNear(base.getXYZ(), scouting_distance_avg, 0.15); //far
            toofar = 1.1*scouting_distance_avg;
          }
      } else { //NEAR
              //if know any ene ship loca, scout there!
              if (hasEnemySightings()) {  //near mode, to enemy sighting.
                ret = Missilemada2.getRandomLocationNear(getXYZFromAnyShipScoutReport(), 0.2*asker.getSensorRange(), 0.09);
                toofar = 0.60*scouting_distance_avg;
                //if foe too far from base, DONT GO.
                if (MobileThing.calcDistanceVecVec(getXYZ(), ret) > toofar) {
                  ret = null; //try some other spot
                }
              } else { //no sightings, scout near
                ret = Missilemada2.getRandomLocationNear(base.getXYZ(), 0.5*scouting_distance_avg, 0.12); //near
                toofar = 0.55*scouting_distance_avg;
              }
      }
      //exclude too far & too near base
      if (ret != null && MobileThing.calcDistanceVecVec(ret, getXYZ()) > toofar) { //too far
        ret = null;
        continue;
      }
      if (ret != null && MobileThing.calcDistanceVecVec(ret, getXYZ()) < 1.2*asker.getSensorRange()) { //too close to base
        ret = null;
        continue;
      }
      //exclude near FL
      if (ret != null && getDistanceBetwBaseAndFrontline() > 0.7*asker.getSensorRange() ) { //if frontline not at base
        if (MobileThing.calcDistanceVecVec(ret, getFrontlineXYZ()) < 0.9*asker.getSensorRange()) { //too close to frontline
          ret = null;
          continue;
        }
      }

      //xxxx exclude miner & SENSAT monitored spots, no sense in scouting next to those



      //optional: exclude where any ally ship. if FAR.
      //OR use getShips_XYNearest
      if (nearfar.equals("FAR")) {
        Vector someallies = Missilemada2.getShipsOfFactionNearXYZ(this, ret, 0.5*asker.getSensorRange());
        if (someallies.size() > 0) {
          ret = null;
          continue;
        }
      } //else NEAR: don't want such check??xxx
    }
    return ret;
  }
  private Vector getXYZFromAnyShipScoutReport() {
    if (hasEnemySightings()) {
      int siz = scoutreports_ships.size();
      for (int i = 0; i < siz; i++) {
        ScoutReport sr = (ScoutReport) scoutreports_ships.elementAt(i);
        if (sr != null) {
          return sr.item.getXYZ();
        } else {
          throw new NullPointerException("lka");
        }
      }
      return null;
    } else {
      return null;
    }
  }
  public Vector getScoutingCandidateSpot() {
    if (scouting_distance_avg < 2.0 * Missilemada2.getNearlyArrivedDistance()) {
      scouting_distance_avg = 2.0 * Missilemada2.getNearlyArrivedDistance();
    }
    Vector ret = null;
    //rand (previous logic) or from spotted (not scanned) asteroids list.
    if (Missilemada2.gimmeRandDouble() < 0.2) {
      ret = Missilemada2.getRandomLocationNear(base.getXYZ(), scouting_distance_avg, 0.05); //BASE vicinity and gradually incr range.
      //System.out.println("1goto_near_base desti from faction.");
      //scouting_distance_avg = scouting_distance_avg + 1.0;
    } else if (scoutingCandidateSpots.size() > 0) {
      int rand = Missilemada2.gimmeRandInt(scoutingCandidateSpots.size());
      ret = (Vector) scoutingCandidateSpots.get(rand);
      //System.out.println("goto_CANDISPOT desti from faction.");
      scoutingCandidateSpots.remove(ret);
      if (ret == null) {
        System.out.println("error, getScoutingCandidateSpot list gave null.");
        ret = Missilemada2.getRandomLocationNear(base.getXYZ(), scouting_distance_avg, 0.01); //BASE vicinity and gradually incr range.
      }
    } else {
      //not random, and not from candi spots.

      ret = Missilemada2.getRandomLocationNear(base.getXYZ(), scouting_distance_avg, 0.20); //BASE vicinity and gradually incr range.

      if (Missilemada2.areCoordsWithinPlayarea(ret)) {
        //okay
      } else {
        System.out.println("debug: error, getRandomLocationNear(base.getXYZ(), gave out of play area.");
        ret = Missilemada2.getRandomLocationNear(base.getXYZ(), base.getSensorRange(), 1); //BASE vicinity and gradually incr range.
      }
      //System.out.println("2goto_near_base desti from faction.");

    }
    return ret;
  }
  public Vector getSafeLocation(Ship asker, Ship closestEnemy) {
    if (asker == null || closestEnemy == null) {
      System.out.println("getSafeLocation() called with null arg");
      return getXYZ();
      //return Missilemada2.getRandomAsteroid().getXYZ();
    }

    scouting_distance_avg = scouting_distance_avg - 5.0;
    if (scouting_distance_avg < Missilemada2.getNearlyArrivedDistance())
      scouting_distance_avg = Missilemada2.getNearlyArrivedDistance();


    double safedist = 1.5*Missilemada2.getCombatDistMax(asker);
    Vector tryspot = Missilemada2.getRandomLocationNear(asker.getXYZ(), safedist, 0.2);
    int bailout = 0;

    while (MobileThing.calcDistanceVecVec(tryspot, closestEnemy.getXYZ()) < safedist && bailout < 200) {
      tryspot = Missilemada2.getRandomLocationNear(closestEnemy.getXYZ(), safedist, 0.2);
      safedist = safedist * 1.03;
      bailout++;
    }
    return tryspot;
  }
  public Vector getSafeLocation() {
    if (base.isInCombat()) {
      //xxx calc safe spot, maybe from one of the scouted asteroids... nah.
      //maybe rand maxmissiledist away from base.
      return Missilemada2.getRandomLocationNear(base.getXYZ(), Missilemada2.getMissileRangeMax(), 0.04);
    } else {
      return base.getXYZ(); //behind base, safer than docking spot.
    }
  }
  public double getBattleStrengthTotal() {
    int foo = getShipCount(""); //recalcs total_battlestr.
    return total_battlestr;
  }
  public double getActiveShipsCostsTotal() {
    int foo = getShipCount(""); //recalcs total_battlestr.
    return total_active_ships_buildcost;
  }

  public int getShipCount(String t) {
    total_battlestr = 0.0; //reset for recalc
    total_active_ships_buildcost = 0.0;
    total_mining_cargocapa = 0.0;
    int count_scout = 0;
    int count_md = 0;
    int count_bd = 0;
    int count_tinyminer = 0;
    int count_miner = 0;
    int count_defender = 0;
    int count_ac = 0;
    Vector myShips = Missilemada2.getShipsOfFaction(this);
    int siz = myShips.size();
    for (int i = 0; i < siz; i++) {
      Ship s = (Ship) myShips.elementAt(i);
      if (s != null) {
        total_battlestr = total_battlestr + s.getBattleStr();
        total_active_ships_buildcost = total_active_ships_buildcost + s.getCost();
        if (s.getType().equals("SCOUT"))
          count_scout++;
        if (s.getType().equals("MISSILEDRONE"))
          count_md++;
        if (s.getType().equals("BEAMDRONE"))
          count_bd++;
        if (s.getType().equals("TINYMINER")) {
          count_tinyminer++;
          total_mining_cargocapa = total_mining_cargocapa + s.getCargoCapa();
        }
        if (s.getType().equals("MINER")) {
          count_miner++;
          total_mining_cargocapa = total_mining_cargocapa + s.getCargoCapa();
        }
        if (s.getType().equals("DEFENDER"))
          count_defender++;
        if (s.getType().equals("AC"))
          count_ac++;
      }
    }

    if (t.equals("SCOUT"))
      return count_scout;
    if (t.equals("MISSILEDRONE"))
      return count_md;
    if (t.equals("BEAMDRONE"))
      return count_bd;
    if (t.equals("TINYMINER"))
      return count_tinyminer;
    if (t.equals("MINER"))
      return count_miner;
    if (t.equals("DEFENDER"))
      return count_defender;
    if (t.equals("AC"))
      return count_ac;
    return 0;
  }
  public boolean hasEnemySightings() {
    scoutreports_ships.trimToSize();
    if (scoutreports_ships.size() > 1)
      return true;
    else
      return false;
  }
  public boolean isAnyOfOurShipsInBattle() {






    if (scoutreports_ships.size() > 0)
      return true;
    else
      return false;
  }
  public double getMaxShieldNumber() {
    return max_known_ourshields;
  }
  public boolean isBaseAlive() {
    if (base.isDestroyed()) {
      return false;
    } else {
      return true;
    }
  }
  public void debugVFX_Text(Vector xyz, String tx) {
    if (isPlayerFaction())
      Missilemada2.addVfx2(xyz, "TEXT", 8900, 100.0, 0.6/*transp*/, "", 1.0, tx);
  }
  private String predictResourceLack_Str() {
    //xx could try cheap AC, see what is lacking that way...

    //which reso is lower than avg, that should do it.
    double avg = (resource_fuel + resource_metal1 + resource_metal2 + resource_metal3) / 4.0;
    if (resource_fuel < avg)
      return "FUEL";
    if (resource_metal1 < avg)
      return "METAL1";
    if (resource_metal2 < avg)
      return "METAL2";
    if (resource_metal3 < avg)
      return "METAL3";

    return "FUEL";
  }
  public void setMode(String who, String where) { //called on user issuing command from button.
    //xxx check for valid mode command combinations! BASE MINERS NEAR FAR FLAG. miners: BASE, GO.

    if (isPlayerFaction()) {
      playFactionOrderSound();
      Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "Order set: "+ who +","+where,0);
      drawFrontLineIndicator(4510.0 /*size*/, 1.01/*chance*/);
    }
    Vector ourships = Missilemada2.getShipsOfFaction(this);

    if (who.equals("SCOUT")) {
      scoutmode = where;
      //for each scout of ours, force new desti_decision
      //for each X of ours, (force new desti_decision ??)
      int listsize = ourships.size();
      for (int j = 0; j < listsize; j++) {
        Ship s = (Ship) ourships.elementAt(j);
        if (s != null) {
          //if type
          if (s.getType().equals("SCOUT")) {
            s.clearDestination();

            if (where.equals("BASE")) {
              //special case: force desti base, easily known location.
              s.forceDestination(getStarbase().getDockingXYZ(), "scouts to base");
            }
            if (where.equals("MINERS")) {
              //?
              s.clearDestination();
              //if (s.hasForcedDesti())
            }
            if (where.equals("NEAR")) {
              //?
              s.clearDestination();
            }
            if (where.equals("FAR")) {
              //?
              s.clearDestination();
            }
            if (where.equals("FLAG")) {
              //special case: force desti flag, easily known location.
              s.forceDestination(getFrontlineXYZ_vary("CENTER"), "scouts to FL");
              drawFrontLineIndicator(3150.0, 1.01);
              tryShiftFrontlineTowardsFoes(8, 2.0);
            }
            if (where.equals("FLAGLEFT")) {
              s.forceDestination(getFrontlineXYZ_vary("LEFT"), "scouts to FLLE");
              drawFrontLineIndicator(3150.0, 1.01);
              tryShiftFrontlineTowardsFoes(8, 2.0);
            }
            if (where.equals("FLAGRIGHT")) {
              s.forceDestination(getFrontlineXYZ_vary("RIGHT"), "scouts to FLRI");
              drawFrontLineIndicator(3150.0, 1.01);
              tryShiftFrontlineTowardsFoes(8, 2.0);
            }
          }
        }
      }
    }
    if (who.equals("MIL")) {
      milmode = where;
      //for each X of ours, (force new desti_decision ??)
      int listsize = ourships.size();
      for (int j = 0; j < listsize; j++) {
        Ship s = (Ship) ourships.elementAt(j);
        if (s != null) {
          String type = s.getType();
          if (type.equals("DEFENDER") || type.equals("AC")  || type.equals("MISSILEDRONE") || type.equals("BEAMDRONE") ) {
            s.clearDestination();

            if (where.equals("BASE")) {
              //special case: force desti base, easily known location.
              if (!s.isDrone())
                s.forceDestination(getStarbase().getDockingXYZ(), "mil to base");
            }
            if (where.equals("MINERS")) {
              //?
              s.clearDestination();
            }
            if (where.equals("NEAR")) {
              //?
              s.clearDestination();
            }
            if (where.equals("FAR")) {
              //?
              s.clearDestination();
            }
            if (where.equals("FLAG")) {
              if (!s.isDrone())
                s.forceDestination(getFrontlineXYZ_vary("CENTER"), "mil to FL");
              //else drone, and just follows milbuddies.
              drawFrontLineIndicator(3150.0, 1.01);
              tryShiftFrontlineTowardsFoes(8, 2.0);
            }
            if (where.equals("FLAGLEFT")) {
              if (!s.isDrone())
                s.forceDestination(getFrontlineXYZ_vary("LEFT"), "mil to FLLE");
              //else drone, and just follows milbuddies.
              drawFrontLineIndicator(3150.0, 1.01);
              tryShiftFrontlineTowardsFoes(8, 2.0);
            }
            if (where.equals("FLAGRIGHT")) {
              if (!s.isDrone())
                s.forceDestination(getFrontlineXYZ_vary("RIGHT"), "mil to FLRI");
              //else drone, and just follows milbuddies.
              drawFrontLineIndicator(3150.0, 1.01);
              tryShiftFrontlineTowardsFoes(8, 2.0);
            }
          }
        }
      }
    }
    if (who.equals("MINER")) {
      minermode = where;
      //for each miner and tinyminer of ours, (force new desti_decision ??)
      int listsize = ourships.size();
      for (int j = 0; j < listsize; j++) {
        Ship s = (Ship) ourships.elementAt(j);
        if (s != null) {
          if (s.isMiner()) {
            if (where.equals("BASE")) {
              //special case: force desti base, easily known location.
              s.clearDestination();
              s.forceDestination(getXYZ_starbase_safer_side(), "miners to base");
            }
            if (where.equals("GO")) {
              s.clearDestination();
              //force desti asteroid, unless cargo full...
              //the old miner code does all this. No action needed.
            }
          }
        }
      } //end for each aste
    }
  }
  public String getMode(String who) {
    if (who.equals("SCOUT")) {
      return scoutmode;
    }
    if (who.equals("MIL")) {
      return milmode;
    }
    if (who.equals("MINER")) {
      return minermode;
    }
    throw new NullPointerException("faction.getmode");
    //return "error";
  }
  public void forgetAsteroidUNUSED(Asteroid as) { //to prevent miners from stupidly trying verydanger spot. Just scout it again with fast scouts.
    for (int i = 0; i < scoutreports_asteroids.size(); i++) {
      ScoutReport sr = (ScoutReport) scoutreports_asteroids.elementAt(i);
      if (sr != null) {
        Asteroid candi = (Asteroid) sr.item;
        if (candi.equals(as)) {
          scoutreports_asteroids.remove(sr); //list can have duplicate reports filed of same asteroid. thus no BREAK command.
        }
      }
    }



  }
  public double getDistanceBetwBaseAndFrontline() {
    return MobileThing.calcDistanceVecVec(getXYZ(), frontlineLocation);
  }
  public void convertDerelictIntoResources(Ship derelict) {
    if (getStarbase() == null)
      return; //base is dead.
    try {
      //remove from deadshipslist
      Missilemada2.removeDerelictShipFromGame(derelict);

      //calc cost and reso
      Vector a = derelict.getResourcesRequired();
      resource_fuel = resource_fuel + 0.1 * (Double) a.get(0);
      resource_metal1 = resource_metal1 + 0.6 * (Double) a.get(1);
      resource_metal2 = resource_metal2 + 0.7 * (Double) a.get(2);
      resource_metal3 = resource_metal3 + 0.7 * (Double) a.get(3);
      if (isPlayerFaction()) {
        addXPToCommander(derelict, "DERELICT");
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "_scrapped_ a derelict " + derelict.getType() + " at our base.",2);
        playFactionScrappedYay();

        Missilemada2.updateBuildButtonColours(); //update coz can build more stuff now.
      } else { //AI
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "DEBUG: FOE scrapped a derelict " + derelict.getType() + ".",3);
      }
    } catch (Exception e) {
      e.printStackTrace();
      Missilemada2.pauseGame();
    }
  }
  public int getScoutCount() {
    return scout_count;
  }
  public int getMinerCount() {
    return miner_count + tinyminer_count;
  }
  public int getMilCount() {
    return def_count + ac_count + bd_count + md_count;
  }

  public int gimmeCrewmanFromBase(Ship s) { //ship asks base for replacement person.
    if (crew_idle > 0) {
      crew_idle = crew_idle - 1;
      return 1;
    } else { //no idlers!
      if (isPlayerFaction()) {
        //xxx spammy!
        Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + s.toStrTypeNName() + " requested crewman to replace dead one, but don't have anyone unassigned!",0);
      }
      return 0;
    }
  }
  private void tryShiftFrontlineTowardsFoes(int howmanytoask, double weighting) {
    for (int i = 0; i < howmanytoask; i++) {
      //get random ally
      Ship s = chooseRandomMannedAlly();
      //xx ask them who is biggest threat
      Ship foe = s.getTarget();
      if (foe != null) {
        //shift frontline towards foe.
        frontlineLocation = MobileThing.changeXYZTowards(frontlineLocation, foe.getXYZ(), weighting * 8000.0);
      }
    }
  }
  private Ship chooseRandomMannedAlly() {
    Vector v = Missilemada2.getShipsOfFaction(this);
    if (v.size() < 3) {
      return base;
    }
    Ship ret = null;
    for (int i = 0; i < 30; i++) {
      ret = (Ship) v.get(Missilemada2.gimmeRandInt(v.size()));
      if (ret == null) {
        System.out.println("chooseRandomMannedAlly: null entry in getShipsOfFaction result.");
        return base;
      } else {
          if (ret.isSenSat() || ret.isDrone()) {
          //continue;
        } else { //ret is acceptable
          break;
        }
      }
    }
    System.out.println("chooseRandomMannedAlly gave "+ret.toString());
    return ret;
  }
  public void toggleShowSensors() {
    show_sensors = !show_sensors;
  }
  public String getLackingResource() {
    return lacking_resource;
  }

  public void removeNresourcesForMislRefill(double buildcredits) {
    //xxxx
  }

  public void messageNotEnoughCrewIdle(int a /*positive, 1..16ish*/) {
    crew_shortage_upon_buildtry = a;
  }
  public void addXPToCommander(Ship s, String why) {
    if (isPlayerFaction())
      cmdr.addXP(s, why);
    //AI not have xp across scenarios.
  }
  public void commanderScenarioWon() {
    cmdr.handleXP_scenariowon();
  }
  public void commanderScenarioLost() {
    cmdr.handleXP_scenariolost();
  }

  public Ship gimmeRandMannedShip() { //for random rumors
    Ship ret = null;
    Vector ourships = Missilemada2.getShipsOfFaction(this);
    int listsize = ourships.size();
    if (listsize < 3)
      return null;
    int trycount = 0;
    while (trycount < 150) {
      //try rand, see if manned
      ret = (Ship) ourships.elementAt(Missilemada2.gimmeRandInt(listsize));
      if (ret.getCrewCount() > 0) {
        trycount++;
        break;
      }
      trycount++;
    }
    return ret;
  }
}
