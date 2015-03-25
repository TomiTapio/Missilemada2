package com.tomitapio.missilemada2;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

/* date class was created: 13.10.2014 */
public class Commander {
  private HashMap<String, Double> xpMap;
  private double totalXP = 0.0;

  Faction parentFaction; //null means game is broken.
  //name? if totalxp = 0 then new random name.

  public Commander(String foo, Faction f) {
    parentFaction = f;
    xpMap = new HashMap<String, Double>(); // 0.0 to 1000.0 xp in each ship type.
    //init
    xpMap.put("SCOUT", 0.0);
    xpMap.put("MINER", 0.0);
    xpMap.put("TINYMINER", 0.0);

    xpMap.put("DEFENDER", 0.0);
    xpMap.put("AC", 0.0);

    xpMap.put("BEAMDRONE", 0.0);
    xpMap.put("MISSILEDRONE", 0.0);
    //not SENSAT, they don't have pricerange levels.
  }
  public Faction getFaction() {
    return parentFaction;
  }
  public int getTechLevel(String type) { //what pricerange this Commander is allowed to build.
    //0..1000 xp in each type. 0->1 200->2 400->3 600->4 800->5
    //xxxxx unkn bonus, in some class, if 1000xp.

    //convert to pricerange levels, 0-5
    double types_xp = xpMap.get(type);
    int level = 0; //that type is locked for player.
    if (types_xp > 800)
      level = 5;
    else if (types_xp > 600)
      level = 4;
    else if (types_xp > 400)
      level = 3;
    else if (types_xp > 200)
      level = 2;
    else if (types_xp > 0.1)
      level = 1;

    //scout and miner, always minimum pricerange 1, otherwise faction is useless.
    if (type.equals("SCOUT") || type.equals("MINER")) {
      if (level < 1) {
        level = 1;
      }
    }
    return level; //0 means can't this type, too junior to know the tech plans.
  }
  public void handleXP_scenariowon() {
    addXP("SCOUT", 50);
    addXP("MINER", 50);
    addXP("TINYMINER", 50);
    //XXXX XP TO others only if they been unlocked.




    saveToFile();

  }
  public void handleXP_scenariolost() {
    addXP("SCOUT", 50);
    //xxxxxx what




    saveToFile();

  }

  public void addXP(String type, double xp) {
    assert (xp > -0.0000001);
    assert (xp < 1001.0);

    double types_xp = xpMap.get(type);
    xpMap.put(type, (types_xp + xp));
    System.out.println("Commander: "+xp+" xp to "+type+".");

    //xxxxxx if reach N in Y, unlock new type! by putting 1 xp in that type.

    //scout 2 or miner 2 unlocks mining drone.
    if (type.equals("SCOUT") || type.equals("MINER")) {
      if (types_xp > 200) {
        //unlock tinyminer
        double tiny_xp = xpMap.get("TINYMINER");
        if (tiny_xp < 1.0) { //if it is locked.
          xpMap.put("TINYMINER", (tiny_xp + 1.0));
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "__Commander, we can now build low-end mining drones!__",3);
        }
      }
    }
    //mining drone level 2 unlocks missiledrone.
    if (type.equals("TINYMINER")) {
      if (types_xp > 200) {
        //unlock missiledrone
        double md_xp = xpMap.get("MISSILEDRONE");
        if (md_xp < 1.0) { //if it is locked.
          xpMap.put("MISSILEDRONE", (md_xp + 1.0));
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "__Commander, we can now build minimal missile drones!__",3);
        }
      }
    }
    //scout 3 unlocks defender.
    if (type.equals("SCOUT")) {
      if (types_xp > 400) {
        //unlock defender
        double def_xp = xpMap.get("DEFENDER");
        if (def_xp < 1.0) { //if it is locked.
          xpMap.put("DEFENDER", (def_xp + 1.0));
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "__Commander, we can now build shieldy DEFENDER warships!__",3);
        }
      }
    }
    //defender 2 unlocks beamdrone.
    //defender 3 unlocks assault cruser.
    if (type.equals("DEFENDER")) {
      if (types_xp > 200) {
        double bd_xp = xpMap.get("BEAMDRONE");
        if (bd_xp < 1.0) { //if it is locked.
          xpMap.put("BEAMDRONE", (bd_xp + 1.0));
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "__Commander, we can now build missile-nullifying beam drones!__",3);
        }
      }
      if (types_xp > 400) {
        double ac_xp = xpMap.get("AC");
        if (ac_xp < 1.0) { //if it is locked.
          xpMap.put("AC", (ac_xp + 1.0));
          Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + "__Commander, we can now build mighty ASSAULT CRUISERS!__",3);
        }
      }

    }
    //xxxxxx if reach N in Y, msg to player!



  }
  public void addXP(Ship s, String why_xp) {
    String type = s.getType();
    double xp = 0.0;
    if (type.equals("SCOUT")) {
      //xxxtemp math logic, SHOULD USE COST SOMEHOW
      xp = 45.0;
    }
    if (type.equals("MINER")) {
      xp = 48.0;
    }
    if (type.equals("TINYMINER")) {
      xp = 16.0; //low coz plentiful cheap
    }
    if (type.equals("DEFENDER")) {
      xp = 47.0;
    }
    if (type.equals("AC")) {
      xp = 77.0; //build 13 of these to max out xp.
    }
    if (type.equals("BEAMDRONE")) {
      xp = 22.0;
    }
    if (type.equals("MISSILEDRONE")) {
      xp = 14.0; //low coz plentiful cheap
    }

    if (why_xp.equals("BUILT")) { //the usual xp gain
      //xp not multiplied
    }
    if (why_xp.equals("DERELICT")) { //badly damaged, less xp
      xp = 0.6 * xp;
    }
    if (why_xp.equals("SURRENDERED")) { //more xp than building one. should include "hacked"
      xp = 1.7 * xp;
    }
    if (why_xp.equals("HULLREPAIR")) { //tiny xp
      xp = 0.02 * xp;
    }

    addXP(type, xp);
  }
  public void addTotalXP(double xp) {
    totalXP = totalXP + xp;
  }
  public double getTotalXP() {
    return totalXP;
  }
  public void loadFromFile() {
    Properties p = new Properties();
    String userdir = System.getProperty("user.dir");
    String a = userdir + "\\Missilemada2_commandersave.txt";
    try {
      System.out.println("Trying to read "+a);
      FileInputStream fis = new FileInputStream(a);
      p.load(fis);

      totalXP = Integer.parseInt(p.getProperty("TOTALXP", "0")); //key, default.

      xpMap.put("SCOUT", (double) (Integer.parseInt(p.getProperty("SCOUTXP", "0"))));
      xpMap.put("MINER", (double) (Integer.parseInt(p.getProperty("MINERXP", "0"))));
      xpMap.put("TINYMINER", (double) (Integer.parseInt(p.getProperty("TINYMINERXP", "0"))));

      xpMap.put("DEFENDER", (double) (Integer.parseInt(p.getProperty("DEFENDERXP", "0"))));
      xpMap.put("AC", (double) (Integer.parseInt(p.getProperty("ACXP", "0"))));

      xpMap.put("BEAMDRONE", (double) (Integer.parseInt(p.getProperty("BEAMDRONEXP", "0"))));
      xpMap.put("MISSILEDRONE", (double) (Integer.parseInt(p.getProperty("MISSILEDRONEXP", "0"))));

      //FONTNAME = p.getProperty("FONTNAME", "Times New Roman");

      fis.close();
    } catch (FileNotFoundException ao) {
      System.out.println("Error: can't read "+a+". Proceeding anyway. Saving a new zero xp savegame.");
      this.saveToFile();


      //xx addto hudmsg, BUT that exists only after createworld...
    } catch (IOException bo) {
      System.out.println("Error: IOException when accessing "+a+". Proceeding anyway.");


      //xx addto hudmsg, BUT that exists only after createworld...
    } finally {

    }
  }
  public void saveToFile() {
    Properties p = new Properties();
    String userdir = System.getProperty("user.dir");
    String a = userdir + "\\Missilemada2_commandersave.txt";


    try {
      //xx rename old one to .bak, but rename might fail

      System.out.println("Trying to rename old savefile "+a);
      File foo = new File(a);
      if (foo.exists()) {
        foo.renameTo(new File(a+".bak"+Missilemada2.gimmeRandInt(8)));
      }

      System.out.println("Trying to write "+a);
      FileOutputStream fos = new FileOutputStream(a);

      p.setProperty("TOTALXP", dTOSTR(totalXP));

      p.setProperty("SCOUTXP",        dTOSTR(xpMap.get("SCOUT")));
      p.setProperty("MINERXP",        dTOSTR(xpMap.get("MINER")));
      p.setProperty("TINYMINERXP",    dTOSTR(xpMap.get("TINYMINER")));

      p.setProperty("DEFENDERXP",     dTOSTR(xpMap.get("DEFENDER")));
      p.setProperty("ACXP",           dTOSTR(xpMap.get("AC")));
      p.setProperty("BEAMDRONEXP",    dTOSTR(xpMap.get("BEAMDRONE")));
      p.setProperty("MISSILEDRONEXP", dTOSTR(xpMap.get("MISSILEDRONE")));

      p.store(fos, null);
      fos.close();
    } catch (FileNotFoundException ao) {
      System.out.println("Error: can't read "+a+". Proceeding anyway.");


      //xx addto hudmsg, BUT that exists only after createworld...
    } catch (IOException bo) {
      System.out.println("Error: IOException when accessing "+a+". Proceeding anyway.");


      //xx addto hudmsg, BUT that exists only after createworld...
    } finally {

    }


  }
  private String dTOSTR(double a) {
    return new Integer((int)a).toString();
  }
  public void addBonusXPFromScenarioWin() {
    //two shipbuild worth of xp to each category.

  }
}
