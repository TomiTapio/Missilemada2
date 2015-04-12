package com.tomitapio.missilemada2;

//LWJGL for OpenGL
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.glu.GLU;

//Slick2D for font stuff
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Color;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;

import javax.sound.midi.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

//RTree / SpatialIndex library (jsi-1.0.0)
//import org.slf4j.*;
//import gnu.trove.*;
import gnu.trove.TIntProcedure;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.rtree.RTree;

public class Missilemada2 {
  //constants that define the world
  public static double world_x_max =  5100500.0; //km.  player base at 0 x 0. basedist like 700 000
  public static double world_x_min = -5100500.0;
  public static double world_y_max =  5100500.0;
  public static double world_y_min = -5100500.0;
  private static boolean FLATWORLD = true; //2.15d is fine. 3d world too hard with my camera, and very little added fun.
  public static double SECONDS_TO_MAX_SPEED = 1.7 * 3600.0;

  //per-scenario things, read from scenario files.
  private static double BASEDIST = 680500.0; //1100500.0; //km.
  private static double BASEMOVESPEED = 0.0;
  //private static int ASTEROIDS_PER_FACTION = 92;
  private static int OLD_DERELICT_COUNT = 0;
  //public static double DIFFICULTYADJUSTMENT_AI_RESOGAIN = 0.00001; //0.000 090

  private static boolean VSYNC = false;
  private static int VIEWHEIGHT = 1080; //from ini
  private static int VIEWWIDTH = (int) Math.round((13.0/9.0)*VIEWHEIGHT); //from ini
  private static String FONTNAME = "xx"; //from ini
  private static int FONTSIZE1 = 10; //from ini
  private static int FONTSIZE2 = 10; //from ini
  private static int FONTSIZE3 = 10; //from ini

  private static float INIT_CAMERA_Z_DIST = 425000.0f;
  private static float visualscaling; //for drawing objects when very zoomed out.
  private static boolean FULLSCREEN = false;
  protected static boolean running = false;
  protected static TextureLoaderMy textureLoader;
  static TextureMy texture_panel;

  static boolean show_splashscreen = false; //unused so far
  static boolean show_leaderboard = true;
  static boolean show_help = true;

  //static String splashscreen = "-- Missilemada 2 --\nA Java + OpenGL game by TomiTapio.\nVersion 2014-06-xx.\n\nESC to quit.";
  static String pause_and_infotext = ""; //if very short text, do not be in pause mode.

  static long worldTimeElapsed = 0; //seconds. usually around 60 fps * 25 seconds in a player second.
  static int FPS = 60; //from ini
  static double measured_FPS = 0;
  static int worldTimeIncrement = 48; // (int)(1000.0/((double) FPS)); //in seconds. maybe 5min in one player sec.
  static int worldTimeIncrement_min = 24 /*32*/; //slow down time when playerfaction in combat! //from ini
  static int worldTimeIncrement_max = /*5x*/ 4*32; //speed up time when no playerfaction combat! //from ini
  //static long sleeptime;
  static int prev_wti = worldTimeIncrement; //remember wti, when set wti=0 for pausing.
  static double renderTimeMeasuredms = 0;
  static double logicTimeMeasuredms = 0;
  static long seen_wti_nano = 0;
  static int max_flatsprites = 6000; //this gets auto-reduced if FPS problems from 3d-rendering. //from ini

  static Properties mainProps;
  static Properties scenarioProps;
  static int current_scenario_id; //starts at 1.
  //xxx highscores as a props
  int player_abouttolose_counter = 0;

  static Random randgen;
  static String randCharsAllowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; // q=high, g&i=low, a=avg so qqqqqqqqq is best and priciest ship.
  //static String randCharsAllowed = "qag"; // q=high, g&i=low, a=avg so qqqqqqqqq is best and priciest ship.

  static Vector factionList; //1-10 factions
  static Vector shipList; //20-60ish ships per faction (tinyminer and missiledrone are super cheap)
  static Vector deadShipList;
  static Vector missileList; //roughly 4-50 missiles per combat-capable ship
  static Vector asteroidList;
  static Vector starBaseList; //1 per faction
  //floating bonus resource list eh?

  private static TextureMy texture_beams;
  static Vector vfxList; //explosions, state indicators
  static Vector flatsprite_temporary_List; //metal and rock debris from combat and mining, and asteroids dropping flakes.
  static Vector flatsprite_permanent_List; //clouds, fields
  static Vector hazardList;
  static Vector hudMsgList = new Vector (30, 30);
  private static int MAXLINES_hudMsgList = 18; //11 on 750px tall, 24 on 1080
  private static Vector GUIButtonList = new Vector (20, 10);

  //RTree stuff
  static SpatialIndex si_misl;
  static SpatialIndex si_ship;
  //static SpatialIndex si_aste;
  static Vector getMislNearestN_ret; //because a mini helper-class
  static Vector getShipNearestN_ret; //because a mini helper-class
  //static Vector getAsteNearestN_ret; //because a mini helper-class
  static Missile[] missileArr;
  static Ship[] shipArr;
  //static Asteroid[] asteArr;

  //dynamic lights from explosions, also missiles can travel through curr_explosion and get damaged.
  private static Vector current_explosion_location; //sort of hazard that exists within single time-tick. Cause by missiles and exploding ships.
  private static double current_explosion_range;
  private static long LightExpiryTimestamp_currMisExpl;
  private static long LightExpiryTimestamp_currShipExpl;

  //MIDI things:
  static Sequencer system_MIDISequencer;
  static MidiDevice system_MIDIDevice;
  //static Track MIDI_global_track;
  static double toomuchnotescounter = 0.0;
  static Vector soundNotesPile;
  static long micros_per_quarternote = 0;
  static long millis_per_quarternote = 0;

  private static int mouse_x = 0;
  private static int mouse_y = 0;
  private static boolean mousedrag_camera = false;
  private static double camera_x = 0;
  private static double camera_y = 0;
  private static double camera_z = 2900;
  private static Vector camera_xyz;

  private static UnicodeFont font60;
  private static UnicodeFont font20;
  private static UnicodeFont font30;
  private static String[] helppaneltext = {"Missilemada2 by TomiTapio, made 2013-11 to 2015-04",
        "Yo Commander. Zaibatsu's local resources are at your command. Stop our rivals' asteroid mining operations.",
        "A stealth shuttle will resupply you with crewmembers and the resource you lack most urgently.",
        "",
        "Leftclick on GUI buttons. Rightclick-drag to pan camera.",
        "Mousewheel to zoom, wheelclick or SPACE to toggle pause.",
        "Keyboard camera controls: INSERT/HOME to zoom, cursor keys pan.",
        "ESC quit, SPACE pause, F1 toggle help, F2 toggle ship list, F5 restart scenario.",
        "Commander's experience is saved every N days. Category xp unlocks better pricebrackets."
  };

  public static void main(String[] args) throws LWJGLException {
    new com.tomitapio.missilemada2.Missilemada2().start();
  }
  public static String randomDNAFromStr(String a) {
    StringBuilder sb = new StringBuilder();
    Random tmprand = new Random(strToLongSeed(a)); tmprand.nextBoolean();
    for (int i = 0; i < 9; i++) {
      sb.append(randCharsAllowed.charAt(tmprand.nextInt(randCharsAllowed.length())));
    }
    return sb.toString();
  }
  public static String getRandomDNA() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 9; i++) {
      sb.append(randCharsAllowed.charAt(randgen.nextInt(randCharsAllowed.length())));
    }
    return sb.toString();
  }
  public static double gimmeRandDouble() {
    return randgen.nextDouble();
  }
  public static int gimmeRandInt(int a) { //ret 0 .. a-1
    return randgen.nextInt(a);
  }
  public static String getRandomJapaneseName() {
    String first[] = {"aka", "shiro", "hige", "kata", "tokeru",        "toko", "tooi", "tsui", "uchuu", "uki",
                      "ureshi", "urutora", "wakai", "wata", "yaba",    "ya" };
    String second[] = {"toka", "kuma", "risu", "toishi", "tooboe",     "toorima", "tsuta", "uchiwa", "uketori", "ukimi",
                       "uma", "umeki", "umou" ,"urame"," no uranari",  "uri", "usei", "ushio", "uten","wadome",
                       "waihon", "wani" , "waregachi", "waza", "chou", "yagai"};
    return first[gimmeRandInt(16)]+second[gimmeRandInt(26)];
  }
  public static String getRandomVulcanName() {
    String first[] = { "Yar", "Sele", "Tal'", "Kir", "Droy",          "Kae", "Kapra", "Krup", "Kumi", "Lahso",
                       "Lakht", "Mene", "Nikh", "Orkika", "Ram",      "Sbah", "Shi'kar", "T'Kuhati", "Tor", "Viproy",
                       "Faruwa", "Futisha", "Shaxa", "Taiya", "Tara" };
    String second[] = { " D'Vahl", " T'Plana", "ahla", "mora", "ya",    "kir", "kyr", "shara", "formaji", "khiori",
                        "lasha", "ran", "tesmur", "tikh", "ba'tak",     "dvatai", "kau", "korsovaya", "sahrafel", "vokau",
                        "yehvaru", "ne'hou", "kah'hir", "ahn", "kli",   "kie'dush", "muhd", "senepa", "tilek", "trillpa",
                        "wun", "yon-kliton", "zud", "zalu", "gad-shen", "tevun", "menal", "tsoraya"};
    return first[gimmeRandInt(25)]+second[gimmeRandInt(38)];
  }

  public static int getWorldTimeIncrement() {
    return worldTimeIncrement; //milliseconds
  }
  public static void changeWorldTimeIncrement(int change) {
    worldTimeIncrement = worldTimeIncrement + change;
    if (worldTimeIncrement > worldTimeIncrement_max)
      worldTimeIncrement = worldTimeIncrement_max;
    if (worldTimeIncrement < worldTimeIncrement_min)
      worldTimeIncrement = worldTimeIncrement_min;
  }
  public static void gameScenarioLost() {
    putNotes(strIntoMelody("ga-over", 15, "") /*Vector of pitches*/, 57 /*core note*/, 1 /*instrument*/, 90, 2.4F /*note duration*/);
    //xxx doesn't play, coz we pause the game...
    getPlayerFaction().commanderScenarioLost();
    //xxx store msg to player in some new var


      //worldTimeIncrement = 0;
      pause_and_infotext = "--- DEFEAT! ---\n";
      try { Thread.sleep(4300); } catch (Exception e) {}


    createWorldFromScenarioFile(""+current_scenario_id); //xx? to such scenario that the Commander class is in.




    createGUIButtons();
  }
  public static void gameScenarioWon() {
    putNotes(strIntoMelody("we-WON", 15, "") /*Vector of pitches*/, 57 /*core note*/, 1 /*instrument*/, 90, 2.4F /*note duration*/);
    getPlayerFaction().commanderScenarioWon();
    //xxx store msg to player in some new var

    pause_and_infotext = "--- VICTORIOUS! ---\n";
    try { Thread.sleep(2300); } catch (Exception e) {}

    //player gets to next level!
    //xxxxxxxxxxxxx figure out what is next scenario.
    createWorldFromScenarioFile(""+(current_scenario_id+1) ); //xxxxxxxx seems to work?!?


    createGUIButtons();


  }
  public static long strToLongSeed(String b) {
    return (long) b.hashCode();
  }
  public static Vector stringNumberlistToMelodyVector(String numbers) { //for hardcoding a nice melody.
    Vector ret = new Vector(10,10);
    String[] strArray = numbers.split(",");

    for(int i = 0; i < strArray.length; i++) {
      ret.add(Integer.parseInt(strArray[i]));
    }
    return ret;
  }
  public static Vector strIntoMelody(String definingstr, int len, String enforce_key /*not yet, pretty complex, but increase number until it is in key's numbers*/) {
    //vector of Integers, 0 is pause, 1-127 is midi note pitch but we have relative to corenote(50ish).
    Vector ret = new Vector(len, 15);
    Random tmprand = new Random(strToLongSeed(definingstr)); tmprand.nextBoolean(); tmprand.nextBoolean();
    for (int i = 0; i < len; i++) {
      int note = tmprand.nextInt(20+12); //0..19

      if (note < 12) {
        ret.add(new Integer(0));
      } else {
        ret.add(new Integer(note-12));
      }
    }
    return ret;
  }
  public static String consonantsIntoVowels(String s) {
    //b into ii
    //c into ee
    //d into aa
    //f into yy
    //g into uu
    //h into oe
    //j into ie
    //k into ia
    //l into ay
    //m into au
    //n into ay
    //p into ae
    //q into ea
    //r into eu
    //s into ue
    //t into ui
    //v into oa
    //w into oy
    //x into ou
    //z into oo

    return "DUMMY";
  }
  public static Vector VowelsIntoNotes(String vo) {
    // low to high: o u y a e i.
    //xxxtodo someday
    return new Vector(20,20);
  }

  public static void soundNotesPileAdd(StampedNote s) {
    soundNotesPile.add(s);
  }
  private boolean checkAndPlayNoteFromQue() { //checks, plays, puts noteoff into que
    if (worldTimeIncrement < 1)
      return false; // the pause.

    boolean ret = false;
    int siz = soundNotesPile.size();
    StampedNote sn = null;
    if ( siz > 0) {
      for (int i = 0; i < siz; i++) {
        try {
          sn = (StampedNote) soundNotesPile.elementAt(i);
        } catch (ArrayIndexOutOfBoundsException e) {
          e.printStackTrace();
        }
        if (sn.worldtimestamp < worldTimeElapsed) {
          if (!sn.isNoteOff) {
            //System.out.println("World time "+worldTimeElapsed+", note_on "+sn.toString());
            putMIDINote_crapnoteoff(sn.instrument, sn.actualnote, false, sn.vol, /*sn.dur_ticks, 0.0,*/ false/*noteoff req*/, true /*override spamming counter*/);

            ret = true;
            //no break, may want chords.

            //put a noteoff into Que
            long noteoff_timestamp = Math.round(sn.worldtimestamp /*WORLD SECONDS*/ +(worldTimeIncrement/FPS)*(sn.dur_ticks * millis_per_quarternote));
            soundNotesPile.add(new StampedNote(noteoff_timestamp, sn.instrument, sn.actualnote, 0, 0, true/*noteoff type*/));

            soundNotesPile.remove(sn);
            siz = soundNotesPile.size();
          } else { //noteoff request from pile -- do not generate child noteoff from noteoff
            /* try {
              ShortMessage m = new ShortMessage(); m.setMessage(ShortMessage.NOTE_OFF, 0, sn.actualnote, 0);
              system_MIDIDevice.getReceiver().send(m, -1);
            } catch (Exception e) { e.printStackTrace(); }
            */

            //System.out.println("World time "+worldTimeElapsed+", note_off "+sn.toString());
            putMIDINote_crapnoteoff(sn.instrument, sn.actualnote/*which key to release*/, true/*noteoff*/, 127, /*sn.dur_ticks, 0.0,*/ false/*noteoff right after*/, true /*override spamming counter*/);
            soundNotesPile.remove(sn);
            siz = soundNotesPile.size();
          }
        }
      }
    }
    return ret;
  }
  public static void playRadioChatter(int a, int ins /*70 bassoon*/, int vol, int offset) {
    StampedNote s;
    if (a == 1) { // pa-poo-pa!
      s = new StampedNote(worldTimeElapsed,     ins, 37+offset, vol, 0.90, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+2500, ins, 32+offset, vol, 0.90, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+6000, ins, 36+offset, vol, 0.70, false/*noteoff*/); soundNotesPile.add(s);
    }
    if (a == 2) { // dun dah, dun doh. //actual: du daa do daa.
      s = new StampedNote(worldTimeElapsed /*what worldtime ms to play at*/, ins, 33+offset, vol, 0.65, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+2000 /*what worldtime ms to play at*/, ins, 38+offset, vol, 0.78, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+6000 /*what worldtime ms to play at*/, ins, 33+offset, vol, 0.65, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+800 /*what worldtime ms to play at*/, ins, 34+offset, vol, 0.65, false/*noteoff*/); soundNotesPile.add(s);
    }
    if (a == 3) { // dyy daa-de. LONG for peacetime aste_scoutreport
      s = new StampedNote(worldTimeElapsed,     ins, 38+offset, vol, 2.80, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+7500, ins, 33+offset, vol, 2.40, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+9500, ins, 29+offset, vol, 2.25, false/*noteoff*/); soundNotesPile.add(s);
    }
    if (a == 4) { // dyy daa-de pok pok.
      s = new StampedNote(worldTimeElapsed,     ins, 39+offset, vol, 0.60, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+4200, ins, 33+offset, vol, 0.40, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+6500, ins, 30+offset, vol, 0.25, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+10500,ins, 29+offset, vol, 0.25, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+12900,ins, 29+offset, vol, 0.29, false/*noteoff*/); soundNotesPile.add(s);
    }
    if (a == 5) { // bass babble
      s = new StampedNote(worldTimeElapsed,     ins, 27+offset, vol, 0.15, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+1700, ins, 26+offset, vol, 0.15, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+3000, ins, 28+offset, vol, 0.25, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+4700, ins, 27+offset, vol, 0.25, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+5900, ins, 27+offset, vol, 0.29, false/*noteoff*/); soundNotesPile.add(s);
    }
    if (a == 6) { // LONG for peacetime spotted_a_derelict
      s = new StampedNote(worldTimeElapsed,     ins, 35+offset, vol, 2.10, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+6500, ins, 33+offset, vol, 2.40, false/*noteoff*/); soundNotesPile.add(s);
      s = new StampedNote(worldTimeElapsed+8500, ins, 27+offset, vol, 2.25, false/*noteoff*/); soundNotesPile.add(s);
    }

  }
  public static void putNotes(Vector v /*Vector of pitches and pauses*/, int corenote, int instrument /*instrument*/, int vol, double dur_ticks /*note duration*/) {
    //double worldseconds_per_quarternote = worldTimeIncrement * (millis_per_quarternote/* is 500*/ / 1000.0);
    double worldseconds_per_quarternote = worldTimeIncrement * ( (19*500) / 1000.0);
    double playerseconds_per_quarternote = worldseconds_per_quarternote / (FPS * worldTimeIncrement);   // plr sec has (FPS * worldTimeIncrement) world sec
    //130+ms seems okay quarternote.

    Integer note;
    int actualnote;
    StringBuilder notes_as_string = new StringBuilder(3000);
    try {
      if ( v.size() > 0) {
        for (int i = 0; i < v.size(); i++) {
          note = (Integer) v.elementAt(i);
          actualnote = corenote + note.intValue();
          if (note.intValue() > 0) {
            notes_as_string.append(actualnote).append(",");
          } else {
            notes_as_string.append("  ,");
          }

          if (note.intValue() > 0) { //if not a pause, put note.
            //put into my own queue... because Java Sound device does not timestamps, it's more of a live MIDI cable.
            long worldtimestamp = worldTimeElapsed + Math.round(worldseconds_per_quarternote * dur_ticks * (i));
            StampedNote s = new StampedNote(worldtimestamp /*what worldtime to play at*/, instrument, actualnote, vol,  dur_ticks, false/*not noteoff*/);
            soundNotesPile.add(s);
            //System.out.println("putMelody: inserted " + s.toString());
          }
        }
        //System.out.println("playMelody: inst " + instrument + ", dur " + dur_ticks + ", core " + corenote + " at world time " + worldTimeElapsed + ": " + notes_as_string.toString());
      }
    } catch (Exception e) {
      System.out.println("playMelody exception... " +  e.toString());
    }
  }
  private static void allNotesOff() {
    ShortMessage m = new ShortMessage();
    try {
      m.setMessage(ShortMessage.CONTROL_CHANGE, 0, (byte)123, (byte)0);
      system_MIDIDevice.getReceiver().send(m, -1);
    } catch (Exception e) { e.printStackTrace(); }
  }
  public static void putMIDINote_crapnoteoff(int instrument, int notepitch/*16=20Hz, 32=52Hz, 69=440Hz*/, boolean noteoff_pls,
                                             int volume, /*double dur, double beforewait,*/ boolean followup_noteoff_right_away, boolean override /*override spamming counter*/) {
    int hard_offset = 2;
    int hard_offset_micros = 500;
    //from ticks to microseconds
    float micros_per_quarternote = system_MIDISequencer.getTempoInMPQ();
    //long dur_micros = new Double(dur).longValue() * new Double(micros_per_quarternote).longValue();
    //long beforewait_micros = new Double(beforewait).longValue() * new Double(micros_per_quarternote).longValue();
    /*ticksPerSecond = resolution * (currentTempoInBeatsPerMinute / 60.0);   tickSize = 1.0 / ticksPerSecond;*/


    if (toomuchnotescounter > 5 && !override) {
      System.out.println("putMIDINote_crapnoteoff: too spammy, inst=" +instrument + " counter="+toomuchnotescounter);
      return;
    }
    //if things are set up okay
    if (system_MIDISequencer != null && system_MIDIDevice != null) {
      try {
        //long tipos = system_MIDISequencer.getTickPosition();
        long tipos = 0;
        long upos = system_MIDISequencer.getMicrosecondPosition();
        long dev_us_pos = system_MIDIDevice.getMicrosecondPosition();

        ShortMessage m = new ShortMessage();
        m.setMessage(ShortMessage.NOTE_ON, 0, notepitch, volume); //xxxx WHEN to noteoff??

        ShortMessage msg_instchange = new ShortMessage();
        msg_instchange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);

        //MidiEvent e = new MidiEvent(m,  tipos+2);         //e.setTick();        //MIDI_global_track.add(e);        //system_MIDISequencer.getReceiver().send(m,tipos+2);

        system_MIDIDevice.getReceiver().send(msg_instchange, -1 /*dev_us_pos + beforewait_micros*/);
        //if (override) {//melody testing
        if (!noteoff_pls) //if not a noteoff, send noteon message.
          system_MIDIDevice.getReceiver().send(m, -1 /*dev_us_pos + beforewait_micros + hard_offset_micros*/);
        //}
        if (!override)
          toomuchnotescounter = toomuchnotescounter + 1.0;

        //System.out.println("playNote: inst " + instrument + ", pitch "+notepitch+" at world time " + worldTimeElapsed + " and micros requested="+(dev_us_pos + hard_offset_micros));

        //xxx or put a noteoff into que.

        if (followup_noteoff_right_away || noteoff_pls) {
          m = new ShortMessage(); m.setMessage(ShortMessage.NOTE_OFF, 0/*channel*/, notepitch/*which key was released*/, volume);
          //m = new ShortMessage(); m.setMessage(ShortMessage.NOTE_ON, 0/*channel*/, notepitch/*which key was released*/, 0);
          system_MIDIDevice.getReceiver().send(m, -1 /*dev_us_pos + beforewait_micros + hard_offset_micros + dur_micros*/);
        }

        //e = new MidiEvent(m,  tipos+64+200);       //MIDI_global_track.add(e);
      } catch (Exception e) {
        System.out.println("putMIDINote_crapnoteoff exception: " + e.getMessage());
      }
    } else {
      System.out.println("putMIDINote_crapnoteoff: global MIDI things aren't set up.");
    }
    //else nothing

  }
  public static MidiDevice chooseMIDIDevice() throws MidiUnavailableException { //borrowed func
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    MidiDevice device = null;
    MidiDevice ret = null;
    for (int i = 0; i < infos.length; i++) {
      device = MidiSystem.getMidiDevice(infos[i]); //have four in my Win7: Microsoft MIDI Mapper, Microsoft GS Wavetable Synth, Real Time Sequencer, Java Sound Synthesizer
      System.out.println("MIDI found: " + device.getDeviceInfo().getName().toString()      + "");
      if (device.getDeviceInfo().getName().contains("Java Sound")) {
        ret = device;
      }
      if (device.getDeviceInfo().getName().contains("Gervill")) { //new since Java runtime update whatever...
        ret = device;
      }
    }
    if (ret != null) {
      System.out.println("MIDI now using: "+ret.getDeviceInfo().getName().toString());
      return ret;
    } else {
      System.out.println("MIDI reluctantly trying to use: "+device.getDeviceInfo().getName().toString());
      return device;
    }
  }
  public Missilemada2(){
  }
  public void start() throws LWJGLException {
    System.setProperty("org.lwjgl.util.Debug","false"); //org.lwjgl.util.Debug=<true|false>

    //load main params (not scenario-specific) from file:
    mainProps = new Properties();
    try {
      String userdir = System.getProperty("user.dir");
      String a = userdir + "\\Missilemada2_main_config.txt";
      System.out.println("Trying to read config "+a);
      FileInputStream fis = new FileInputStream(a);
      mainProps.load(fis);

      VIEWHEIGHT = StrToInt(mainProps.getProperty("VIEWHEIGHT", "900")); //key, default.
      VIEWWIDTH  = StrToInt(mainProps.getProperty("VIEWWIDTH", "1300"));
        //MAXLINES_hudMsgList = 18; //11 on 750px tall, 24 on 1080
        if (VIEWHEIGHT < 790)
          MAXLINES_hudMsgList = 11;
        if (VIEWHEIGHT > 950)
          MAXLINES_hudMsgList = 24;

      FPS        = StrToInt(mainProps.getProperty("FPS", "60"));
      //vsync? xx maybe.
      //flatworld? nah.
      worldTimeIncrement_min = StrToInt(mainProps.getProperty("TIME_INCREMENT_MINIMUM", "22"));
      worldTimeIncrement_max = StrToInt(mainProps.getProperty("TIME_INCREMENT_MAXIMUM", "88"));
      max_flatsprites = StrToInt(mainProps.getProperty("MAXIMUM_DEBRIS_FLATSPRITES", "6000"));
      FONTSIZE1 = StrToInt(mainProps.getProperty("FONTSIZE1", "21")); //bad defaults on purpose
      FONTSIZE2 = StrToInt(mainProps.getProperty("FONTSIZE2", "28"));
      FONTSIZE3 = StrToInt(mainProps.getProperty("FONTSIZE3", "60"));
      FONTNAME = mainProps.getProperty("FONTNAME", "Times New Roman");

      fis.close();
    } catch (FileNotFoundException a) {
      System.out.println("Error: can't read Missilemada2_main_config.txt. Proceeding anyway.");
      //xx addto hudmsg, BUT that exists only after createworld...
    } catch (IOException b) {
      System.out.println("Error: IOException when accessing Missilemada2_main_config.txt. Proceeding anyway.");
      //xx addto hudmsg, BUT that exists only after createworld...
    }

    //init MIDI system
    try {
      MidiDevice md = chooseMIDIDevice();
      md.open();
      system_MIDIDevice = md;
      system_MIDISequencer = MidiSystem.getSequencer(); //but which of the four devices is this?
      if (system_MIDISequencer == null || system_MIDIDevice == null) {
        System.out.println("MIDI sequencer getting failed.");
      } else {
        if (!(system_MIDISequencer.isOpen())) {
          system_MIDISequencer.open();
          Sequence mySeq = new Sequence(Sequence.SMPTE_30, 10/*ticks per video frame*/); //MidiSystem.getSequence(myMidiFile);
          system_MIDISequencer.setSequence(mySeq);
          if (system_MIDIDevice.getMicrosecondPosition() == -1) {
            System.out.println("MIDI device does not support timestamps, behaves like a live cable.");
          } else {
            System.out.println("MIDI device supports timestamps; current microseconds "+system_MIDIDevice.getMicrosecondPosition());
          }
        }
      }
    } catch (Exception e) {
      System.out.println("MIDI setup failed, " + e.toString());
      e.printStackTrace();
    }

    // Set up our display
    //PixelFormat pf = new PixelFormat(24, 24, 24, 24, 1); //public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples) { /* compiled code */ }
    PixelFormat pf = new PixelFormat().withDepthBits(32).withSamples(2);
    ContextAttribs contex = new ContextAttribs(3,2)
            .withForwardCompatible(true)
            .withProfileCore(true);

    Display.setTitle("Missilemada 2 by TomiTapio");
    Display.setResizable(false); //whether our window is resizable
    DisplayMode dm = new DisplayMode(VIEWWIDTH, VIEWHEIGHT);

    Display.setDisplayMode(dm); //resolution of our display
    //glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA | GLUT_STENCIL); //multisample eh?

    Display.setVSyncEnabled(VSYNC); //whether hardware VSync is enabled
    Display.setFullscreen(FULLSCREEN); //whether fullscreen is enabled
    Display.setLocation(0,0);
    pf = new PixelFormat(32, 0, 24, 0, 0/*multisampling*/);
    Display.create(pf);

    System.out.println("Display vendor: "+glGetString(GL_VENDOR) +" version: " +glGetString(GL_VERSION) + " using adapter: "+Display.getAdapter());
    initContextAndResources(); //OpenGL and textures
    initSlickUtilFonts();
    show_splashscreen = true;


    current_scenario_id = 1; //xxxxxxxxxxxxxxxxxxxxxxxx load from Commander class persistence.


    createWorldFromScenarioFile(""+current_scenario_id);
    createGUIButtons();
    resize();
    running = true;
    int inputsleep = 0; //to prevent keypress-is-20-presses.

    //core loop
    while (running && !Display.isCloseRequested()) {
      long nanotime_startframe = System.nanoTime();
      camera_xyz = new Vector(4,3);
      camera_xyz.add(0, new Double(camera_x));
      camera_xyz.add(1, new Double(camera_y));
      camera_xyz.add(2, new Double(camera_z));

      // if window resized, need to update projection
      if (Display.wasResized())
        resize();

      inputsleep = inputsleep - 1;
      if (inputsleep < 1) { //prevent accidental N-clicks. Input system sleeps for M frames.
        inputsleep = 0;

        //read mouse inputs
        mouse_x = Mouse.getX(); // will return the X coordinate on the Display.
        mouse_y = Mouse.getY();
        mouse_y = VIEWHEIGHT - mouse_y; //make 0 x 0 upper left as far as mouse is concerned.
        if (Mouse.isButtonDown(0)) {
          playerClicked(worldTimeIncrement);
          inputsleep = 18;
        }

        if (Mouse.isButtonDown(1)) { //rightclick: begin drag
          //mouse change into camera xy change:
          if (!mousedrag_camera) { //if not in mode, begin tracking drag



            mousedrag_camera = true;
          }
          //System.out.println("mouse dx = "+ Mouse.getDX());
          //System.out.println("mouse dy = "+ Mouse.getDY());
          //dx, dy are -8 to +8 ish PER FRAME

          camera_x = camera_x - 0.15 * Mouse.getDX() * (1990 + Math.abs(0.02*camera_z));
          camera_y = camera_y - 0.15 * Mouse.getDY() * (1990 + Math.abs(0.02*camera_z));

          //inputsleep = 22;
        } else {
          mousedrag_camera = false; //end drag
        }

        int deltaWheel = Mouse.getDWheel();
        if (deltaWheel > 0) { //zoom in
          if (camera_z > 0.0)
            camera_z = 0.93*camera_z - 3100.0;
          if (camera_z < -2000.0)
            camera_z = -2000.0; //max zoom in.
          //inputsleep = 1;
        } else if (deltaWheel < 0) { //zoom out
          if (camera_z > 0.0)
            camera_z = 1.08*camera_z + 3100.0;
          else
            camera_z = camera_z + 3100.0;
          if (camera_z > world_x_max) //max zoom out.
            camera_z = world_x_max;
          //inputsleep = 1;
        }

        //read keyboard
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
          running = false;

        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE) || Mouse.isButtonDown(2)/*wheelclick*/) {
          if (worldTimeIncrement == 0) {
            unpauseGame();
          } else {
            pauseGame();
          }
          inputsleep = 21;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
          camera_x = camera_x + 5400f + Math.abs(0.04*camera_z);
          inputsleep = 2;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
          camera_x = camera_x - 5400f - Math.abs(0.04*camera_z);
          inputsleep = 2;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
          camera_y = camera_y + 5400f + Math.abs(0.04*camera_z);
          inputsleep = 2;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
          camera_y = camera_y - 5400f - Math.abs(0.04*camera_z);
          inputsleep = 2;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_INSERT)) { //zoom in
          if (camera_z > 0.0)
            camera_z = 0.95*camera_z - 2500.0;

          if (camera_z < 0.0)
            camera_z = 100.0;
          inputsleep = 1;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_HOME)) { // zoom out
          if (camera_z > 0.0)
            camera_z = 1.05*camera_z + 2500.0;
          else
            camera_z = camera_z + 2500.0;
          inputsleep = 1;
          if (camera_z > world_x_max) //max zoom out.
            camera_z = world_x_max;
        }
        //F1 toggle help panel
        if (Keyboard.isKeyDown(Keyboard.KEY_F1)) {
          show_help = !show_help;
          inputsleep = 21;
        }
        //F2 toggle ship leaderboard
        if (Keyboard.isKeyDown(Keyboard.KEY_F2)) {
          show_leaderboard = !show_leaderboard;
          inputsleep = 21;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F3)) {
          playRadioChatter(2, 109, 90, -9);
          inputsleep = 17;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F4)) {
          playRadioChatter(3, 109, 90, -6);
          inputsleep = 17;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F5)) {
          FULLSCREEN = !FULLSCREEN;
          Display.setFullscreen(FULLSCREEN);
          createWorldFromScenarioFile("1");
          createGUIButtons();
          //resize();
          inputsleep = 50;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F6)) {
          playRadioChatter(5, 109, 90, 0);
          inputsleep = 17;
        }
      }

      long na = System.nanoTime();
      //long na2 = System.nanoTime();

      if (false) { //debug print times
        clear_for_render();
          if (worldTimeIncrement > 0)
            System.out.println("nanotime before logic: "+System.nanoTime());
        advance_time();
        advance_time();
          if (worldTimeIncrement > 0)
            System.out.println("nanotime after logic: "+System.nanoTime() + ", elapsed "+ (System.nanoTime() - na) / 1000000);
          na = System.nanoTime();
          render();
          if (worldTimeIncrement > 0)
            System.out.println("nanotime after render: "+System.nanoTime() + ", elapsed "+ (System.nanoTime() - na) / 1000000);
          // Flip the buffers and sync to X FPS
          na = System.nanoTime();
          Display.update();
          Display.sync(FPS);
          double frame_elapsed_ms = (System.nanoTime() - nanotime_startframe) / 1000000.0;
          measured_FPS = 1000.0 / frame_elapsed_ms;
          if (worldTimeIncrement > 0)
            System.out.println("nanotime after update and sync-wait: "+System.nanoTime() + ", elapsed "+ (System.nanoTime() - na) / 1000000);
      } else { //without debug print time
        clear_for_render();
        advance_time();
        advance_time();

        na = System.nanoTime();
        render();

        Display.update(); // Flip the buffers
        renderTimeMeasuredms = (System.nanoTime() - na) / 1000000.0;

        //if graphics card is struggling, ALLOW LESS DECORATIVE FLATSPRITES

//        if (renderTimeMeasuredms > (1000.0/FPS) - logicTimeMeasuredms)
//          max_flatsprites = max_flatsprites - 1;
//        else
//          max_flatsprites = max_flatsprites + 1;

//        if (renderTimeMeasuredms > ((1000.0/FPS) - logicTimeMeasuredms)) {
//          try {
//            for (int i = 0; i < 30; i++) {
//              flatsprite_temporary_List.remove(i);
//            }
//          } catch (Exception e) {
//
//          }
//        }
//        if (max_flatsprites < 1200) //was 120
//          max_flatsprites = 1200;

        //int error = GL11.glGetError();
        //if (error != GL11.GL_NO_ERROR)
        //  System.out.println(GLU.gluErrorString(error));

        seen_wti_nano = System.nanoTime() -na;
        //System.out.println("seen wti "+ seen_wti_nano + " ns, or " + (seen_wti_nano / 1000000.0) + " ms."); // (8.6-11.4 before textures) - 15.7 - 46 ms
        //worldTimeIncrement = -3 + (int) Math.round((seen_wti_nano / 1000000.0));
        Display.sync(FPS); // sync to target_FPS
        double frame_elapsed_ms = (System.nanoTime() - nanotime_startframe) / 1000000.0;
        measured_FPS = 1000.0 / frame_elapsed_ms;
      }
    } //end main loop

    // Dispose any resources and destroy our window
    disposeResources();
    Display.destroy();
  }
  public static void pauseGame() {
    prev_wti = worldTimeIncrement;
    worldTimeIncrement = 0; //pauses
  }
  public static void unpauseGame() {
    worldTimeIncrement = prev_wti; //unpauses
    pause_and_infotext = ""; //clears pause-causing infotext.
    show_splashscreen = false;
  }
  private static void playerClicked(double wti_sec) {
    //xxxx disallow click when paused(wti 0) ??


    Faction pf = getPlayerFaction();
    //check against every GUIBUTTON.
    int buttonId = 0;
    int listsize = GUIButtonList.size();
    GUIButton butn = null;
    for (int j = 0; j < listsize; j++) {
      butn = (GUIButton) GUIButtonList.elementAt(j);
      if (butn != null) {
        if (butn.areMouseCoordsInsideYou(mouse_x, mouse_y)) { //<-- global vars
          //System.out.println("GUIButton match: "+butn.getButtonId());
          buttonId = butn.getButtonId();
          butn.setVisualState(true); //unwanted on some commands.
          butn.setClickFxOn(worldTimeElapsed+3500 /*expirytime*/);
          //vfx of command success?
          break; //current button is the one, keep this buttonid.
        }
      }
    }
    if (buttonId == 0) {
      System.out.println("No GUIButton matched click. Mouse "+mouse_x+","+mouse_y+".");
      return;
    }
    if (buttonId == 1) { //scout mode: base
      pf.setMode("SCOUT", "BASE");
      //other SCOUT modes' button visual to false! The click detection already set the clicked one to true.
      setGUIButtonVisualState(2, false);
      setGUIButtonVisualState(3, false);
      setGUIButtonVisualState(4, false);
      setGUIButtonVisualState(5, false);
      setGUIButtonVisualState(6, false);
      setGUIButtonVisualState(7, false);
      pf.changeScoutingDist(0.95); //reduce faction scouting distance!
    }
    if (buttonId == 2) { //scout mode:
      pf.setMode("SCOUT", "MINERS");
      setGUIButtonVisualState(1, false);
      setGUIButtonVisualState(3, false);
      setGUIButtonVisualState(4, false);
      setGUIButtonVisualState(5, false);
      setGUIButtonVisualState(6, false);
      setGUIButtonVisualState(7, false);
    }
    if (buttonId == 3) { //scout mode:
      pf.setMode("SCOUT", "NEAR");
      setGUIButtonVisualState(1, false);
      setGUIButtonVisualState(2, false);
      setGUIButtonVisualState(4, false);
      setGUIButtonVisualState(5, false);
      setGUIButtonVisualState(6, false);
      setGUIButtonVisualState(7, false);
      pf.changeScoutingDist(0.95); //reduce faction scouting distance!
    }
    if (buttonId == 4) { //scout mode:
      pf.setMode("SCOUT", "FAR");
      setGUIButtonVisualState(1, false);
      setGUIButtonVisualState(2, false);
      setGUIButtonVisualState(3, false);
      setGUIButtonVisualState(5, false);
      setGUIButtonVisualState(6, false);
      setGUIButtonVisualState(7, false);
      pf.changeScoutingDist(1.08);
    }
    if (buttonId == 5) { //scout mode:
      pf.setMode("SCOUT", "FLAG");
      setGUIButtonVisualState(1, false);
      setGUIButtonVisualState(2, false);
      setGUIButtonVisualState(3, false);
      setGUIButtonVisualState(4, false);
      setGUIButtonVisualState(6, false);
      setGUIButtonVisualState(7, false);
    }
    if (buttonId == 6) { //scout mode:
      pf.setMode("SCOUT", "FLAGLEFT");
      setGUIButtonVisualState(1, false);
      setGUIButtonVisualState(2, false);
      setGUIButtonVisualState(3, false);
      setGUIButtonVisualState(4, false);
      setGUIButtonVisualState(5, false);
      setGUIButtonVisualState(7, false);
    }
    if (buttonId == 7) { //scout mode:
      pf.setMode("SCOUT", "FLAGRIGHT");
      setGUIButtonVisualState(1, false);
      setGUIButtonVisualState(2, false);
      setGUIButtonVisualState(3, false);
      setGUIButtonVisualState(4, false);
      setGUIButtonVisualState(5, false);
      setGUIButtonVisualState(6, false);
    }

    if (buttonId == 11) { //mil mode:
      pf.setMode("MIL", "BASE");
      setGUIButtonVisualState(12, false);
      setGUIButtonVisualState(13, false);
      setGUIButtonVisualState(14, false);
      setGUIButtonVisualState(15, false);
      setGUIButtonVisualState(16, false);
      setGUIButtonVisualState(17, false);
    }
    if (buttonId == 12) {
      pf.setMode("MIL", "MINERS");
      setGUIButtonVisualState(11, false);
      setGUIButtonVisualState(13, false);
      setGUIButtonVisualState(14, false);
      setGUIButtonVisualState(15, false);
      setGUIButtonVisualState(16, false);
      setGUIButtonVisualState(17, false);
    }
    if (buttonId == 13) {
      pf.setMode("MIL", "NEAR");
      setGUIButtonVisualState(11, false);
      setGUIButtonVisualState(12, false);
      setGUIButtonVisualState(14, false);
      setGUIButtonVisualState(15, false);
      setGUIButtonVisualState(16, false);
      setGUIButtonVisualState(17, false);
    }
    if (buttonId == 14) {
      pf.setMode("MIL", "FAR");
      setGUIButtonVisualState(11, false);
      setGUIButtonVisualState(12, false);
      setGUIButtonVisualState(13, false);
      setGUIButtonVisualState(15, false);
      setGUIButtonVisualState(16, false);
      setGUIButtonVisualState(17, false);
    }
    if (buttonId == 15) {
      pf.setMode("MIL", "FLAG");
      setGUIButtonVisualState(11, false);
      setGUIButtonVisualState(12, false);
      setGUIButtonVisualState(13, false);
      setGUIButtonVisualState(14, false);
      setGUIButtonVisualState(16, false);
      setGUIButtonVisualState(17, false);
    }
    if (buttonId == 16) {
      pf.setMode("MIL", "FLAGLEFT");
      setGUIButtonVisualState(11, false);
      setGUIButtonVisualState(12, false);
      setGUIButtonVisualState(13, false);
      setGUIButtonVisualState(14, false);
      setGUIButtonVisualState(15, false);
      setGUIButtonVisualState(17, false);
    }
    if (buttonId == 17) {
      pf.setMode("MIL", "FLAGRIGHT");
      setGUIButtonVisualState(11, false);
      setGUIButtonVisualState(12, false);
      setGUIButtonVisualState(13, false);
      setGUIButtonVisualState(14, false);
      setGUIButtonVisualState(15, false);
      setGUIButtonVisualState(16, false);
    }
    if (buttonId == 21) { //miner mode: base
      pf.setMode("MINER", "BASE");
      setGUIButtonVisualState(22, false);
    }
    if (buttonId == 22) { //miner mode: go mine
      pf.setMode("MINER", "GO");
      setGUIButtonVisualState(21, false);
    }
    //end radio button section, begin build-commands (no visual state)
    if (buttonId < 31) {
      return;
    } else { //the rest of the buttons. they get visualstate_false.
      butn.setVisualState(false);
      if (buttonId == 1000) { pf.volleyTowardsCoords(pf.getFrontlineXYZ()); }
      if (buttonId == 1001) { pf.toggleShowSensors(); }
      if (buttonId == 1002) { show_help = !show_help; }
      if (buttonId == 1003) { show_leaderboard = !show_leaderboard; }
      if (buttonId == 1004) {
        if (worldTimeIncrement == 0) {
          unpauseGame();
        } else {
          pauseGame();
        }
      }
    if (buttonId == 1005) { running = false; /*quit/exit game*/ }

    if (buttonId == 31) { pf.tryShipProduction("SCOUT", 1); }
    if (buttonId == 32) { pf.tryShipProduction("SCOUT", 2); }
    if (buttonId == 33) { pf.tryShipProduction("SCOUT", 3); }
    if (buttonId == 34) { pf.tryShipProduction("SCOUT", 4); }
    if (buttonId == 35) { pf.tryShipProduction("SCOUT", 5); }

    if (buttonId == 41) { pf.tryShipProduction("TINYMINER", 1); }
    if (buttonId == 42) { pf.tryShipProduction("TINYMINER", 2); }
    if (buttonId == 43) { pf.tryShipProduction("TINYMINER", 3); }
    if (buttonId == 44) { pf.tryShipProduction("TINYMINER", 4); }
    if (buttonId == 45) { pf.tryShipProduction("TINYMINER", 5); }

    if (buttonId == 51) { pf.tryShipProduction("MINER", 1); }
    if (buttonId == 52) { pf.tryShipProduction("MINER", 2); }
    if (buttonId == 53) { pf.tryShipProduction("MINER", 3); }
    if (buttonId == 54) { pf.tryShipProduction("MINER", 4); }
    if (buttonId == 55) { pf.tryShipProduction("MINER", 5); }

    if (buttonId == 61) { pf.tryShipProduction("DEFENDER", 1); }
    if (buttonId == 62) { pf.tryShipProduction("DEFENDER", 2); }
    if (buttonId == 63) { pf.tryShipProduction("DEFENDER", 3); }
    if (buttonId == 64) { pf.tryShipProduction("DEFENDER", 4); }
    if (buttonId == 65) { pf.tryShipProduction("DEFENDER", 5); }

    if (buttonId == 71) { pf.tryShipProduction("AC", 1); }
    if (buttonId == 72) { pf.tryShipProduction("AC", 2); }
    if (buttonId == 73) { pf.tryShipProduction("AC", 3); }
    if (buttonId == 74) { pf.tryShipProduction("AC", 4); }
    if (buttonId == 75) { pf.tryShipProduction("AC", 5); }

    if (buttonId == 81) { pf.tryShipProduction("BEAMDRONE", 1); }
    if (buttonId == 82) { pf.tryShipProduction("BEAMDRONE", 2); }
    if (buttonId == 83) { pf.tryShipProduction("BEAMDRONE", 3); }
    if (buttonId == 84) { pf.tryShipProduction("BEAMDRONE", 4); }
    if (buttonId == 85) { pf.tryShipProduction("BEAMDRONE", 5); }

    if (buttonId == 91) { pf.tryShipProduction("MISSILEDRONE", 1); }
    if (buttonId == 92) { pf.tryShipProduction("MISSILEDRONE", 2); }
    if (buttonId == 93) { pf.tryShipProduction("MISSILEDRONE", 3); }
    if (buttonId == 94) { pf.tryShipProduction("MISSILEDRONE", 4); }
    if (buttonId == 95) { pf.tryShipProduction("MISSILEDRONE", 5); }
    }
  }
  private static void setGUIButtonVisualState(int buttonid, boolean b) {
    int listsize = GUIButtonList.size();
    GUIButton butn;
    for (int j = 0; j < listsize; j++) {
      butn = (GUIButton) GUIButtonList.elementAt(j);
      if (butn != null) {
        if (butn.getButtonId() == buttonid) { //if found the desired button
          butn.setVisualState(b);
          return;
        }
      }
    }
  }
  private static void createGUIButtons() { //create GUIButtons into buttonlist.
    GUIButtonList = new Vector (20, 10);
    //assume at least 700x700 px display. FOR HUD AND GUI: 0 x 0 coords are upper left.
    GUIButton bu;
    float guirowheight = 32.0f;
    //first an informative text. as a button, because this func can't draw gfx.
    float lmargin = 4.0f;
    float topmargin = 7.0f;
    float buwid = 60.5f;
    float buhei = 23.0f; //was 29
    //scouts to: base, miners, near, far, flag
    float bla = 140f;
    bu = new GUIButton(9001 , "Scouts ", lmargin, topmargin,   10.0f, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(1/*id*/, "Base",       0*(buwid+4.0)+bla, topmargin,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(2/*id*/, "Miners",     1*(buwid+4.0)+bla, topmargin,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(3/*id*/, "Near", 2*(buwid+4.0)+bla, topmargin,   buwid-6, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(4/*id*/, "Far",  3*(buwid+4.0)+bla, topmargin,   buwid-11, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);


    bu = new GUIButton(6/*id*/, "Lflank",     4*(buwid+4.0)+bla, topmargin,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(5/*id*/, "Frontline",  5*(buwid+4.0)+bla, topmargin,   75, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(7/*id*/, "Rflank",     5*(buwid+4.0)+bla+87f, topmargin,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);

    //def & ac & mildrones to: base, miners, near, far, flag

    bu = new GUIButton(9002 , "Warships ", lmargin, topmargin +guirowheight,   10.0f, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(11/*id*/, "Base",      0*(buwid+4.0)+bla, topmargin +guirowheight,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(12/*id*/, "Miners",    1*(buwid+4.0)+bla, topmargin +guirowheight,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(13/*id*/, "Near",      2*(buwid+4.0)+bla, topmargin +guirowheight,   buwid-6, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(14/*id*/, "Far",       3*(buwid+4.0)+bla, topmargin +guirowheight,   buwid-11, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);

    bu = new GUIButton(16/*id*/, "Lflank",    4*(buwid+4.0)+bla, topmargin +guirowheight,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(15/*id*/, "Frontline", 5*(buwid+4.0)+bla, topmargin +guirowheight,   75f, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(17/*id*/, "Rflank",    5*(buwid+4.0)+bla+87f, topmargin +guirowheight,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);

    //miners to: base, aste
    //buwid = 64.0f;
    buwid = 85.0f;
    bu = new GUIButton(9003 , "Miners ", lmargin, topmargin+2*guirowheight,   10.0f, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(21/*id*/, "At base",       bla, topmargin +2*guirowheight,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(22/*id*/, "Go mine",        1*(buwid+4.0)+bla, topmargin +2*guirowheight,   buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);

    //maybe: drone mode buttons
    //everyone volley to attack flag vicinity!
    bu = new GUIButton(1000/*id*/, "VOLLEY to frontline",       lmargin, topmargin + 3*guirowheight,   2.9*buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(1001/*id*/, "Show sensors",       lmargin+2.93*buwid, topmargin + 3*guirowheight,   1.7*buwid, buhei,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);

    buwid = 64.0f;
    bu = new GUIButton(1002/*id*/, " Help panel",  7.2*(buwid+5.0)+bla, topmargin,   120f, buhei-2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(1003/*id*/, " Ships panel", 9.2*(buwid+5.0)+bla, topmargin,   120f, buhei-2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(1004/*id*/, " Pause",       11.2*(buwid+5.0)+bla, topmargin,   100f, buhei-2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(1005/*id*/, " Quit",        15.2*(buwid+5.0)+bla, topmargin,   100f, buhei-2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);

    //build scout:
    buwid = 57.0f;
    float buhei2 = 27.0f;
    ///can't draw text in this func. So, info texts as no-action buttons.
    float introwidth = lmargin+buwid+75.0f;
    bu = new GUIButton(9031/*id*/, "Build scout: ", lmargin, 5*guirowheight,   introwidth/10.0f, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(31/*id*/, "Dingy", 0*(buwid+4.0)+introwidth, 5*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(32/*id*/, "Cheap", 1*(buwid+4.0)+introwidth, 5*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(33/*id*/, "Avg",   2*(buwid+4.0)+introwidth, 5*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(34/*id*/, "Nice",  3*(buwid+4.0)+introwidth, 5*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(35/*id*/, "Elite", 4*(buwid+4.0)+introwidth, 5*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    //build tinyminer:
    bu = new GUIButton(9041/*id*/, "Mining drone: ", lmargin, 6*guirowheight,   introwidth/10.0f, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(41/*id*/, "Dingy", 0*(buwid+4.0)+introwidth, 6*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(42/*id*/, "Cheap", 1*(buwid+4.0)+introwidth, 6*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(43/*id*/, "Avg",   2*(buwid+4.0)+introwidth, 6*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(44/*id*/, "Nice",  3*(buwid+4.0)+introwidth, 6*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(45/*id*/, "Elite", 4*(buwid+4.0)+introwidth, 6*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    //build miner:
    bu = new GUIButton(9051/*id*/, "Build miner: ", lmargin, 7*guirowheight,   introwidth/10.0f, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(51/*id*/, "Dingy", 0*(buwid+4.0)+introwidth, 7*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(52/*id*/, "Cheap", 1*(buwid+4.0)+introwidth, 7*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(53/*id*/, "Avg",   2*(buwid+4.0)+introwidth, 7*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(54/*id*/, "Nice",  3*(buwid+4.0)+introwidth, 7*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(55/*id*/, "Elite", 4*(buwid+4.0)+introwidth, 7*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    //build defender:
    bu = new GUIButton(9061/*id*/, "Defender: ",           lmargin, 8*guirowheight,   introwidth/10.0f, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(61/*id*/, "Dingy", 0*(buwid+4.0)+introwidth, 8*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(62/*id*/, "Cheap", 1*(buwid+4.0)+introwidth, 8*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(63/*id*/, "Avg",   2*(buwid+4.0)+introwidth, 8*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(64/*id*/, "Nice",  3*(buwid+4.0)+introwidth, 8*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(65/*id*/, "Elite", 4*(buwid+4.0)+introwidth, 8*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    //build AC:
    bu = new GUIButton(9071/*id*/, "Asslt Cruiser: ",       lmargin, 9*guirowheight,   introwidth/10.0f, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(71/*id*/, "Dingy", 0*(buwid+4.0)+introwidth, 9*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(72/*id*/, "Cheap", 1*(buwid+4.0)+introwidth, 9*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(73/*id*/, "Avg",   2*(buwid+4.0)+introwidth, 9*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(74/*id*/, "Nice",  3*(buwid+4.0)+introwidth, 9*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(75/*id*/, "Elite", 4*(buwid+4.0)+introwidth, 9*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    //build beamdrone:
    bu = new GUIButton(9081/*id*/, "Beam drone: ",         lmargin, 10*guirowheight,   introwidth/10.0f, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(81/*id*/, "Dingy", 0*(buwid+4.0)+introwidth, 10*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(82/*id*/, "Cheap", 1*(buwid+4.0)+introwidth, 10*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(83/*id*/, "Avg",   2*(buwid+4.0)+introwidth, 10*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(84/*id*/, "Nice",  3*(buwid+4.0)+introwidth, 10*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(85/*id*/, "Elite", 4*(buwid+4.0)+introwidth, 10*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    //build missiledrone:
    bu = new GUIButton(9091/*id*/, "Missile drone: ",      lmargin, 11*guirowheight,   introwidth/10.0f, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(91/*id*/, "Dingy", 0*(buwid+4.0)+introwidth, 11*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(92/*id*/, "Cheap", 1*(buwid+4.0)+introwidth, 11*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(93/*id*/, "Avg",   2*(buwid+4.0)+introwidth, 11*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(94/*id*/, "Nice",  3*(buwid+4.0)+introwidth, 11*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
    bu = new GUIButton(95/*id*/, "Elite", 4*(buwid+4.0)+introwidth, 11*guirowheight,   buwid, buhei2,   0.0f/*z*/, 0.38f, 0.0f); GUIButtonList.add(bu);
  }
  public static void updateBuildButtonColours() {
    //if can't afford, grey(default). if can afford, light green.
    //UPDATE COLORS WHEN PLR FAC RESO GAIN and build, not every frame. way less cpu that way.

    Faction pf = getPlayerFaction();

    //getGUIButton(91/*misdrone pricebracket 1*/);

    //if trying returns null Ship, set gray(no afford).
    if (pf.tryProduceShipOfType("SCOUT", 1, true) == null) { buttonSetAfford(31, false); } else { buttonSetAfford(31, true); }
    if (pf.tryProduceShipOfType("SCOUT", 2, true) == null) { buttonSetAfford(32, false); } else { buttonSetAfford(32, true); }
    if (pf.tryProduceShipOfType("SCOUT", 3, true) == null) { buttonSetAfford(33, false); } else { buttonSetAfford(33, true); }
    if (pf.tryProduceShipOfType("SCOUT", 4, true) == null) { buttonSetAfford(34, false); } else { buttonSetAfford(34, true); }
    if (pf.tryProduceShipOfType("SCOUT", 5, true) == null) { buttonSetAfford(35, false); } else { buttonSetAfford(35, true); }

    if (pf.tryProduceShipOfType("TINYMINER", 1, true) == null) { buttonSetAfford(41, false); } else { buttonSetAfford(41, true); }
    if (pf.tryProduceShipOfType("TINYMINER", 2, true) == null) { buttonSetAfford(42, false); } else { buttonSetAfford(42, true); }
    if (pf.tryProduceShipOfType("TINYMINER", 3, true) == null) { buttonSetAfford(43, false); } else { buttonSetAfford(43, true); }
    if (pf.tryProduceShipOfType("TINYMINER", 4, true) == null) { buttonSetAfford(44, false); } else { buttonSetAfford(44, true); }
    if (pf.tryProduceShipOfType("TINYMINER", 5, true) == null) { buttonSetAfford(45, false); } else { buttonSetAfford(45, true); }

    if (pf.tryProduceShipOfType("MINER", 1, true) == null) { buttonSetAfford(51, false); } else { buttonSetAfford(51, true); }
    if (pf.tryProduceShipOfType("MINER", 2, true) == null) { buttonSetAfford(52, false); } else { buttonSetAfford(52, true); }
    if (pf.tryProduceShipOfType("MINER", 3, true) == null) { buttonSetAfford(53, false); } else { buttonSetAfford(53, true); }
    if (pf.tryProduceShipOfType("MINER", 4, true) == null) { buttonSetAfford(54, false); } else { buttonSetAfford(54, true); }
    if (pf.tryProduceShipOfType("MINER", 5, true) == null) { buttonSetAfford(55, false); } else { buttonSetAfford(55, true); }

    if (pf.tryProduceShipOfType("DEFENDER", 1, true) == null) { buttonSetAfford(61, false); } else { buttonSetAfford(61, true); }
    if (pf.tryProduceShipOfType("DEFENDER", 2, true) == null) { buttonSetAfford(62, false); } else { buttonSetAfford(62, true); }
    if (pf.tryProduceShipOfType("DEFENDER", 3, true) == null) { buttonSetAfford(63, false); } else { buttonSetAfford(63, true); }
    if (pf.tryProduceShipOfType("DEFENDER", 4, true) == null) { buttonSetAfford(64, false); } else { buttonSetAfford(64, true); }
    if (pf.tryProduceShipOfType("DEFENDER", 5, true) == null) { buttonSetAfford(65, false); } else { buttonSetAfford(65, true); }

    if (pf.tryProduceShipOfType("AC", 1, true) == null) { buttonSetAfford(71, false); } else { buttonSetAfford(71, true); }
    if (pf.tryProduceShipOfType("AC", 2, true) == null) { buttonSetAfford(72, false); } else { buttonSetAfford(72, true); }
    if (pf.tryProduceShipOfType("AC", 3, true) == null) { buttonSetAfford(73, false); } else { buttonSetAfford(73, true); }
    if (pf.tryProduceShipOfType("AC", 4, true) == null) { buttonSetAfford(74, false); } else { buttonSetAfford(74, true); }
    if (pf.tryProduceShipOfType("AC", 5, true) == null) { buttonSetAfford(75, false); } else { buttonSetAfford(75, true); }

    if (pf.tryProduceShipOfType("BEAMDRONE", 1, true) == null) { buttonSetAfford(81, false); } else { buttonSetAfford(81, true); }
    if (pf.tryProduceShipOfType("BEAMDRONE", 2, true) == null) { buttonSetAfford(82, false); } else { buttonSetAfford(82, true); }
    if (pf.tryProduceShipOfType("BEAMDRONE", 3, true) == null) { buttonSetAfford(83, false); } else { buttonSetAfford(83, true); }
    if (pf.tryProduceShipOfType("BEAMDRONE", 4, true) == null) { buttonSetAfford(84, false); } else { buttonSetAfford(84, true); }
    if (pf.tryProduceShipOfType("BEAMDRONE", 5, true) == null) { buttonSetAfford(85, false); } else { buttonSetAfford(85, true); }

    if (pf.tryProduceShipOfType("MISSILEDRONE", 1, true) == null) { buttonSetAfford(91, false); } else { buttonSetAfford(91, true); }
    if (pf.tryProduceShipOfType("MISSILEDRONE", 2, true) == null) { buttonSetAfford(92, false); } else { buttonSetAfford(92, true); }
    if (pf.tryProduceShipOfType("MISSILEDRONE", 3, true) == null) { buttonSetAfford(93, false); } else { buttonSetAfford(93, true); }
    if (pf.tryProduceShipOfType("MISSILEDRONE", 4, true) == null) { buttonSetAfford(94, false); } else { buttonSetAfford(94, true); }
    if (pf.tryProduceShipOfType("MISSILEDRONE", 5, true) == null) { buttonSetAfford(95, false); } else { buttonSetAfford(95, true); }
  }
  private static void buttonSetAfford(int id, boolean aff) {
    int listsize = GUIButtonList.size();
    for (int j = 0; j < listsize; j++) {
      GUIButton p = (GUIButton) GUIButtonList.elementAt(j);
      if (p != null && id == p.getButtonId()) {
        p.setCanAfford(aff);
        return;
      }
    }
  }
  private static void createWorldFromScenarioFile(String which_scenario /*"1"*/) {
    //dfeaults in case of exception
    int facnum = 2;
    int astenum = 20;
    double basedi = BASEDIST;
    double start_reso_AI = 140.0; //sets fuel, the rest are relative to this.
    double start_reso_plr = 140.0; //sets fuel, the rest are relative to this.
    double aiboost = 0.0;
    //double BASEMOVESPEED = ;

    //load scenario params from file
    scenarioProps = new Properties();
    String userdir = System.getProperty("user.dir");
    String fullpath = userdir + "\\Missilemada2_scenario"+which_scenario+".txt";
    System.out.println("Trying to read scenario file: "+fullpath);
    try {
      FileInputStream fis = new FileInputStream(fullpath);
      scenarioProps.load(fis);

      //we have just ints and negative ints in config file.
      facnum = StrToInt(scenarioProps.getProperty("NUMBER_FACTIONS", "15")); //key, default.
      astenum = StrToInt(scenarioProps.getProperty("ASTEROIDS", "3"));
      OLD_DERELICT_COUNT = StrToInt(scenarioProps.getProperty("OLD_DERELICT_COUNT", "1000"));
      start_reso_AI = StrToInt(scenarioProps.getProperty("AI_INITIAL_RESOURCES", "70"));
      start_reso_plr = StrToInt(scenarioProps.getProperty("PLAYER_INITIAL_RESOURCES", "70"));
      aiboost = 0.000001 * StrToInt(scenarioProps.getProperty("AI_RESOURCE_CHEATING_AMOUNT_MICRONS", "0"));

      BASEDIST = StrToInt(scenarioProps.getProperty("DISTANCE_BETWEEN_STARBASES", "20500"));
      BASEMOVESPEED = 0.001 * StrToInt(scenarioProps.getProperty("STARBASE_SPEED_MILLIS", "9000"));

      fis.close();
      scenarioProps.clear(); //no longer needed. xxxscoresaving?
    } catch (FileNotFoundException ex1) {
      System.out.println("Error: can't read scenario: "+fullpath+". Proceeding anyway.");
      //xx addto hudmsg, BUT that exists only after createworld...
    } catch (IOException ex2) {
      System.out.println("Error: exception when accessing scenario file: "+fullpath+" Proceeding anyway.");
      //xx addto hudmsg, BUT that exists only after createworld...
    }

    createWorld(facnum, astenum, basedi, start_reso_plr, start_reso_AI, aiboost);
  }
  private static void createWorld(int num_factions/*2..10*/, int aste_per_world, double basedi, double start_reso_plr, double start_reso_AI, double boost_or_nerf_foe_reso) {
    camera_x = 0.0;    camera_y = 0.0;    camera_z = 13000.0; //reset user camera shifts
    worldTimeElapsed = 0; //new world.
    randgen = new Random(); randgen.nextBoolean(); randgen.nextBoolean();

    //empty and init all lists of existing things.
    factionList = new Vector (5, 5);
    starBaseList = new Vector (5, 5);
    shipList = new Vector (80, 15);
    deadShipList = new Vector (10, 30);
    missileList = new Vector (300, 80);
    asteroidList = new Vector (350, 100);

    flatsprite_temporary_List = new Vector (900, 200);
    flatsprite_permanent_List = new Vector (100, 200);
    hazardList = new Vector (6, 6); //xxnotyet
    vfxList = new Vector (150, 200);
    hudMsgList = new Vector (30, 30);

    allNotesOff();
    soundNotesPile = new Vector(60,60);

    System.out.println("Resetting the world... "+num_factions+" factions, "+aste_per_world+" asteroids. "+basedi+" km distance between faction bases.");
    Missilemada2.addToHUDMsgList("Welcome, Commander. Zaibatsu's local resources are at your command. Stop our "+(num_factions-1)+" rivals' mining operations.");
    Missilemada2.addToHUDMsgList("ESC quit, SPACE pause, F5 restart. INS/HOME zoom, cursor keys pan.");
    Missilemada2.addToHUDMsgList("Mousewheel zoom, rightbutton drag pan. Wheelclick pause.");

    //place factions
    double rand_dist = basedi * (0.9 + 0.2*gimmeRandDouble());
    //place first faction (player) near origin; camera starts there too.
    Vector xyz_fac1 = getRandomLocationNear(MobileThing.createXYZ(0.0,0.0,0.0), basedi/300.0, 0.04); //first faction near origin
    Faction f1 = new Faction(xyz_fac1, 1, "Plr fac", new Vector()/*personality*/, 0.0/*no boost coz player*/);
    factionList.add(f1);
        StarBase sb = new StarBase(xyz_fac1, f1);
        //melody on init? let player's starbase sing.
        putNotes(strIntoMelody("kla4", 12, "") /*Vector of pitches*/, 30 /*core note*/, 67 /*tenor sax*/, 80, 3.9F /*note duration*/);
        sb.setIsSeenByPlayer(true);
        sb.setFaction(f1);
        addToBaseList(sb);
        addToShipList_withcrewcheck(sb);
        f1.setBase(sb);
    f1.initShipPriceBrackets();

    f1.addStarterShips(); //first must have base set.
    f1.addStarterResources(start_reso_plr/*note*/);
    updateBuildButtonColours(); //update GUI, after resources.

    num_factions--;

    //the other factions, AIs, must be N dist away from any faction base.
    int fi = 0;
    Faction n = null;
    while (num_factions > 0) {
      fi++;
      Vector loca = MobileThing.createXYZ(num_factions * 9000.0, 0.0, 0.0);
      boolean accept_loca = false;

      while (!accept_loca) {
        loca = getRandomLocationNear(getRandomFaction().getXYZ(), basedi+rand_dist, 0.34);
        if (far_from_factions(loca, basedi+rand_dist) ){
          accept_loca = true;
        }
      }

      n = new Faction(loca, 1+fi, getRandomJapaneseName(), new Vector()/*personality*/, boost_or_nerf_foe_reso );
      factionList.add(n);
          StarBase sb1 = new StarBase(loca, n);
          sb1.setFaction(n);
          addToBaseList(sb1);
          addToShipList_withcrewcheck(sb1);
          n.setBase(sb1);
      n.initShipPriceBrackets();
      n.addStarterShips();  //first must have base set.
      n.addStarterResources(start_reso_AI);
      num_factions--;
    }
    //foe_test = new Ship()
    //Vector xyz = Missilemada2.getRandomLocationNear(xyz_fac1, 1000.0, 0.54);
    //Missilemada2.addToShipList_withcrewcheck(new Ship("AC",    0.2, n, xyz, "q", "starship.png"));

    //background -- is in render func.
    //set up lights
    try {
      initAmbientLight();
      initLight(GL11.GL_LIGHT0, 1.0f/*diffuse*/, 0.5f/*spec*/, plusminusrandcoordFORLIGHT(), plusminusrandcoordFORLIGHT(), 0.1f * plusminusrandcoordFORLIGHT());
      initLight(GL11.GL_LIGHT1 , 1.0f/*diffuse*/,  0.1f/*spec*/, plusminusrandcoordFORLIGHT(), plusminusrandcoordFORLIGHT(), (float)(0.6 * world_x_max)); //high z to be near camera-ish.
      initLight(GL11.GL_LIGHT2 , 0.95f/*diffuse*/, 0.9f/*spec*/, plusminusrandcoordFORLIGHT(), plusminusrandcoordFORLIGHT(), 0.1f * plusminusrandcoordFORLIGHT());
      //initLight(GL11.GL_LIGHT3 , 1.0f/*diffuse*/, 0.5f/*spec*/, (float)getPlayerFaction().getStarbase().getX(),
      //        (float)getPlayerFaction().getStarbase().getY(), (float)(0.2*basedi)); //player starbase!
    } catch (Exception e) {
      e.printStackTrace();
    }

    //place asteroids, N per world.
    //double nearbydist = 16.0* getScoutingDistance_crude();
    double nearbydist = 0.45*basedi;
    //double fardist = basedi * 2.5;
    boolean world_approved = false;
    int loopbreaker = 0;
    while (!world_approved && loopbreaker < 200) {
      //start new try.
      asteroidList = new Vector (350, 120);
      flatsprite_permanent_List = new Vector (100, 200);

      for (int i = 0; i < aste_per_world; i++) {
        Asteroid as = new Asteroid(randomAsteSize(), getRandomLocationInPlayarea());
        as.setZ( 0.08 * ((Double)getRandomLocationInPlayarea().get(0)) );
        asteroidList.add(as);
        //yyymaybe if isAsteTooCloseToABase(as)
            //rand chance of accompanied by a decorative cloud permanent-flatsprite.
            if (gimmeRandDouble() < 0.07) {
              int rand1to10 = 1+gimmeRandInt(9);
              String cloudfilename = "cloud"+rand1to10+".png";
              double siz = 590000.0*(0.15+gimmeRandDouble());
              FlatSprite fs = new FlatSprite(siz, siz, as.getX(), as.getY(), as.getZ() - 9000.0, cloudfilename, 1.0, 0.30f/*transp*/);
              fs.setPermRotation((float)(173.0f*gimmeRandDouble()));
              addToFSPermList(fs);

              //also a far-away cloud, not near asteroids and the action area:
              rand1to10 = 1+gimmeRandInt(9);
              cloudfilename = "cloud"+rand1to10+".png";
              siz = 740000.0*(0.25+gimmeRandDouble());
              fs = new FlatSprite(siz, siz, 4.0*as.getX(), 4.0*as.getY(), 1.5*as.getZ(), cloudfilename, 1.0, 0.28f/*transp*/);
              fs.setPermRotation((float)(173.0f*gimmeRandDouble()));
              addToFSPermList(fs);
            }
      }

      if (aste_per_world < 70) //if scenario calls for very few aste, approve.
        world_approved = true;
      else
        world_approved = areAllResourcesAvailableNearPlayerBase(nearbydist); //try new rand placement until good for player.
      loopbreaker++;
    }

    //xxhack:WORKS! : sort the list so highest z coord aste are last in drawing order...
    Comparator<Asteroid> comparator = new Asteroid.AsteroidComparator();
    Collections.sort(asteroidList, comparator);
    //sort by shoving into tree and reading in order.
    TreeSet asSet = new TreeSet();
    asSet.addAll(asteroidList);
    Vector asLi2 = new Vector(500,250);
    Iterator it = asSet.descendingIterator();
    while (it.hasNext()) {
      Asteroid s = (Asteroid) it.next();
      asLi2.add(s);
    }
    asteroidList = asLi2;

    //place derelict ships from earlier years. scouts, miners, mining drones. low end. near asteroids (gravity has pulled 'em to a stop)
    String rand_cheap_dna = "gggggggg"; //older tech, lowest cost these days.
    for (int a = 0; a < OLD_DERELICT_COUNT /*from scenario file*/; a++) {
      String randtype = "";
      double rand = Missilemada2.gimmeRandDouble();
      if (rand < 0.18) {
        randtype = "SCOUT";
      } else if (rand > 0.18 && rand < 0.66) {
        randtype = "TINYMINER";
      } else if (rand > 0.66) {
        randtype = "MINER";
      }
      Ship dead_old_dere = new Ship(randtype, null, getRandomLocation_using_asteroidlist(), rand_cheap_dna, "starship.png", false/*needs crew*/);
      dead_old_dere.setDead_oncreation();
      dead_old_dere.setSpeed(0.0, 0.0, 0.0); //asteroids have gravitied/bumped them into stillness during the years.
      deadShipList.add(dead_old_dere);
    }
  }
  private static boolean areAllResourcesAvailableNearPlayerBase(double nearbydist /*should be under 0.70 basedist?? */) {
    StarBase plrbase = getPlayerFaction().getStarbase();
    boolean yesfuel = false;
    boolean yesm1 = false;
    boolean yesm2 = false;
    boolean yesm3 = false;
    //foreach in asteroidlist, update the booleans.
    try {
      Asteroid as;
      int siz = asteroidList.size();
      for (int i = 0; i < siz; i++) {
        as = (Asteroid) asteroidList.elementAt(i);
        if (as != null && MobileThing.calcDistanceMTMT(plrbase, as) < nearbydist) { //if within nearbydist
          if (as.hasResource("FUEL"))
            yesfuel = true;
          if (as.hasResource("METAL1"))
            yesm1 = true;
          if (as.hasResource("METAL2"))
            yesm2 = true;
          if (as.hasResource("METAL3"))
            yesm3 = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return (yesfuel && yesm1 && yesm2 && yesm3);
  }
  private static Vector getRandomLocationInPlayarea() { //for asteroid placement.
    Vector ret = new Vector(4,3);
    double worldwidth = world_x_max - world_x_min;
    double worldhei = world_y_max - world_y_min;
    double worlddepth = world_x_max / 20.0;

    double x = world_x_min + worldwidth * (0.06 + 0.9*gimmeRandDouble()); //xxnot fully at the edges.
    double y = world_y_min + worldhei * (0.06 + 0.9*gimmeRandDouble());
    double z;
    if (FLATWORLD)
      z = 0.04 /*not fully flat.*/ * (world_y_min/20.0 + worlddepth * gimmeRandDouble());
    else
      z = world_y_min/20.0 + worlddepth * gimmeRandDouble();
    ret.add(0, new Double(x));
    ret.add(1, new Double(y));
    ret.add(2, new Double(z));
    return ret;
  }
  private static float plusminusrandcoordfloat() {
    return (float)(1.7*world_x_max * (gimmeRandDouble() -0.5));
  }
  private static float plusminusrandcoordFORLIGHT() {
    return (float)(0.3*world_x_max * (gimmeRandDouble() + 0.8));
  }
  private static boolean far_from_factions(Vector xyz, double dist) {
    for (int a = 0; a < factionList.size(); a++) {
      Faction fa = (Faction) factionList.get(a);
      if (MobileThing.calcDistanceVecVec(fa.getXYZ(), xyz) < dist) { //if candidate spot is smaller dist from a faction, reject.
        return false;
      }
    }
    return true; //spot was not tooclose to any faction.
  }
  public static double randomAsteSize() {
    return (1900.0 + 3800.0 * gimmeRandDouble()); //should be in km but bugger that in favor of visuals.
  }
  public static Faction getRandomFaction() {
    int a = gimmeRandInt(factionList.size());
    Faction ret = (Faction) factionList.get(a);
    if (ret == null)
      System.out.println("blagggg, null faction from rand faction.");
    return ret;
  }
  public static Vector getRandomLocationNear(Vector asked_origin, double dist, double distvariance/*0..1..3*/){
    //start with dist, then choose a rand direction!
    double mindist = dist* (1 - 0.97*gimmeRandDouble()); //xx,was bad, mindist always too large.
    double maxdist = mindist * (1 + distvariance*(0.06+gimmeRandDouble()));
    double finaldist = mindist + gimmeRandDouble() * (maxdist-mindist);

    double bearXZ;
    if (FLATWORLD) {
      //bearXZ = (Math.PI/2.0); //gameplay: cosine to zero, stay on same z.
      bearXZ = 2 * Math.PI * gimmeRandDouble();
    } else {
      bearXZ = 2 * Math.PI * gimmeRandDouble();
    }

    Vector vec = getLocationDistBear(asked_origin, finaldist,    2 * Math.PI * gimmeRandDouble() /*bearingXY*/,   bearXZ);
    //debug:
    //Missilemada2.addVfx2(vec, "ARGLEFARRIGHT?", 9500, 3000.0, 0.9/*transp*/, "qm_red.png", 1.0, ""); //fuck, always at far edge of scoutdist.
    return vec;
  }
  private static Vector getLocationDistBear(Vector xyz, double dist, double bear_xy, double bear_xz) {
    Vector ret = new Vector(4,3);
    double x = ((Double) xyz.get(0)).doubleValue() + dist * Math.cos(bear_xy);
    double y = ((Double) xyz.get(1)).doubleValue() + dist * Math.sin(bear_xy);
    double z;
    if (FLATWORLD)
      z = 0.12/*not fully flat.*/ * ( ((Double) xyz.get(2)).doubleValue() + dist * Math.sin(bear_xz) );
    else
      z = ((Double) xyz.get(2)).doubleValue() + dist * Math.sin(bear_xz);

    ret.add(0, new Double(x));
    ret.add(1, new Double(y));
    ret.add(2, new Double(z));
    return ret;
  }
  public static Vector getRandomLocationNearOLD(Vector xyz, double dist, double distvariance/*0.08ish*/){
    Vector ret = new Vector(3,3);
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    double mindist = dist - dist*(0.1*gimmeRandDouble()); //xxxxxxxxxxxxxxx
    //double maxdist = dist + distvariance*dist*(3.0*gimmeRandDouble());
    double maxdist = 1.1*mindist + distvariance*dist*(gimmeRandDouble());
    boolean accept = false;
    int loopbreaker = 0; int sign; int sign2;
    while (!accept) {
      if (gimmeRandDouble() < 0.5)
        sign = -1;
      else
        sign = 1;
      if (gimmeRandDouble() < 0.5)
        sign2 = -1;
      else
        sign2 = 1;

      x =  ((Double) xyz.get(0)).doubleValue() + sign*mindist * (1.0+1.0*gimmeRandDouble());
      y =  ((Double) xyz.get(1)).doubleValue() + sign2*mindist * (1.0+1.0*gimmeRandDouble());
      if (FLATWORLD)
        z =  0.0;
      else
        z = ((Double) xyz.get(2)).doubleValue() + 0.05*mindist * (gimmeRandDouble()-0.5);
      double try_dist = MobileThing.calcDistance(((Double) xyz.get(0)).doubleValue(), ((Double) xyz.get(1)).doubleValue(), ((Double) xyz.get(2)).doubleValue(), x, y, z);
      if ((try_dist > mindist) && (try_dist < maxdist))
        accept = true;
      else
        loopbreaker++;

      if (loopbreaker > 800)
        accept = true;

    }

    ret.add(0, new Double(x));
    ret.add(1, new Double(y));
    ret.add(2, new Double(z));
    return ret;
  }
  public static Ship getNearestShipAtXXXXXX(double in_x, double in_y, double in_z) {
    int listsize = shipList.size();
    for (int j = 0; j < listsize; j++) {
      Ship p = (Ship) shipList.elementAt(j);
      if (p != null) {
        Vector vuu = MobileThing.createXYZ(in_x, in_y, in_z);
        if (MobileThing.calcDistanceVecVec(vuu, p.getXYZ()) < BASEDIST / 25.0) //xxxxxxxxxxxxxxxxxxxxx
          return p;
      }
    }
    return null;
  }
  public static Missile getwithinrange_fooNearestMissileAt(double in_x, double in_y, double in_z, double range) {
    int listsize = missileList.size();
    for (int j = 0; j < listsize; j++) {
      Missile mi = (Missile) missileList.elementAt(j);
      if (mi != null) {
        if (MobileThing.calcDistanceVecVec(MobileThing.createXYZ(in_x, in_y, in_z), mi.getXYZ()) < range)
          return mi;
      }
    }
    return null;
  }
  /*private static void gameStopRequest() {
    running = false;
  }*/
  protected static void initContextAndResources() {
    Keyboard.enableRepeatEvents(true);
    if (system_MIDISequencer != null) {
      micros_per_quarternote = new Long(Math.round(system_MIDISequencer.getTempoInMPQ())).longValue();
      millis_per_quarternote = new Long(Math.round(system_MIDISequencer.getTempoInMPQ() / 1000.0)).longValue(); //273 ms per quarternote.
    }
    //glEnable(GL_COLOR_SUM);
    //GL11.glEnableClientState(GL_VERTEX_ARRAY);
    //GL11.glEnableClientState(GL_COLOR_ARRAY);
    //GL11.glEnableClientState(GL_SECONDARY_COLOR_ARRAY);
    //GL11.glEnableClientState(GL_TEXTURE_COORD_ARRAY);

    GL11.glEnable(GL11.GL_TEXTURE_2D); //disabling this is very slow they say
    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
    GL11.glShadeModel(GL11.GL_SMOOTH);
    GL11.glEnable(GL_BLEND);
    GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    //antialiasing
    GL11.glDisable(GL_LINE_SMOOTH);
    GL11.glHint(GL_LINE_SMOOTH_HINT, GL_FASTEST);
    //GL11.glEnable(GL_POLYGON_SMOOTH); //big effect! //NOT RECOMMENDED
    GL11.glHint(GL_POLYGON_SMOOTH_HINT, GL_FASTEST);
    GL11.glEnable(GL_DITHER);

    //glDisable(GL11.GL_MULTISAMPLE );
    //??? glUniform3f(offsetUniform, 0.0f, 0.0f, -0.75f);
    GL11.glPointSize(8.5f); // "call glEnable(GL_POINT_SMOOTH) or glEnable(GL_LINE_SMOOTH) and you use shaders at the same time, your rendering speed goes down to 0.1 FPS" software renderer?

    //fog code
 /*   glEnable(GL_FOG);
    glFogi (GL_FOG_MODE, GL_EXP2);
      float[] fogcolor = {0.1f, 0.25f, 0.1f, 0.5f};
      ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    glFog(GL_FOG_COLOR, (FloatBuffer)buffer.asFloatBuffer().put(fogcolor).flip());  //glFogfv (GL_FOG_COLOR, fogColor);
    glFogf (GL_FOG_DENSITY, 0.000004f);
    glHint(GL_FOG_HINT, GL_NICEST);
*/

    glEnable(GL_NORMALIZE);
    //GL11.glEnable(GL_STENCIL_TEST); //nope, shadows too hard to code.

    //depth stuff
    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glDepthMask(true);
    GL11.glDepthFunc(GL11.GL_LEQUAL);
    GL11.glDepthRange(0.0, 1.0); //use full range
    GL11.glClearDepth(1.0f);

    //http://www.felixgers.de/teaching/jogl/polygonOffset.html
    //There are three different ways to turn on polygon offset, one for each type of polygon rasterization mode: GL_FILL, GL_LINE, or GL_POINT.
    // You enable the polygon offset by passing the appropriate parameter to glEnable(), either GL_POLYGON_OFFSET_FILL, GL_POLYGON_OFFSET_LINE,
    // or GL_POLYGON_OFFSET_POINT. You must also call glPolygonMode() to set the current polygon rasterization method.
    GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
    GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    GL11.glPolygonOffset(11.0f /*factor*/, 100.1f/*units*/);

    GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
    GL11.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    GL11.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    GL11.glClearColor(0f, 0f, 0f, 1.0f);
    GL11.glEnable(GL_COLOR_MATERIAL);								// enables opengl to use glColor3f to define material color
    GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE); //set mode of color commands: tell opengl glColor3f effects the ambient and diffuse properties of material

    //camera was here

    //lights setup was here

    //set up textureloader
    try {
      textureLoader = new TextureLoaderMy();
      texture_beams = Missilemada2.getTextureLoader().getTexture("beam.png");
      texture_panel = Missilemada2.getTextureLoader().getTexture("opaque_dgray.png");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static void setCamera(Vector xyz, float dist) {
    float CLIP_ZNEAR = 0.2f;
    float CLIP_ZFAR = 2.3f * (float)(camera_z + INIT_CAMERA_Z_DIST + BASEDIST);

    GL11.glMatrixMode(GL11.GL_PROJECTION); //camera mode: are changing projection.
    GL11.glLoadIdentity();
    float voo = (float)VIEWWIDTH/(float)VIEWHEIGHT;
    GLU.gluPerspective(70.0f, voo, CLIP_ZNEAR, CLIP_ZFAR);

    GLU.gluLookAt(((Double) xyz.get(0)).floatValue()+(float)camera_x, ((Double) xyz.get(1)).floatValue()+(float)camera_y, dist,
                  ((Double) xyz.get(0)).floatValue()+(float)camera_x, ((Double) xyz.get(1)).floatValue()+(float)camera_y, -dist/*helped somewhat*/,
            0.0f, 100.0f, 0.0f);
    //2D CAD measurements view:   GL11.glOrtho(0, VIEWWIDTH, 0, VIEWHEIGHT, 9000, -9000); //clip distance -1 to 1 is for 2d games...
    GL11.glMatrixMode(GL11.GL_MODELVIEW); //regular mode: are changing models.
  }
  public static void removeShipFromActives(Ship just_destr_ship) {
    //already done in getDest()  just_destr_ship.getFaction().shipCountDown(just_destr_ship, "destroyed");
    shipList.remove(just_destr_ship);
    deadShipList.add(just_destr_ship);
    just_destr_ship.reduceSpeed(0.6); //more fun gameplay if they are catchable.
    if (just_destr_ship.isStarbase()) {
      //dead base keeps its parentfaction pointer.
    } else {
      just_destr_ship.setFaction(null); //no owner.
    }
  }
  public static void removeDerelictShipFromGame(Ship de) {
    deadShipList.remove(de);
    if (de.isStarbase()) {
      System.out.println("major error: some base tried to scrap another base " + de.toStringShort());
    }
  }
  public static double gimmeRandomDisplayCoord() {
    return Display.getHeight() * randgen.nextDouble();
  }
  protected static void clear_for_render() {
    // Clear the screen and depth buffer
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
    GL11.glLoadIdentity();
  }
  protected static void render() {
    // draw starry background quad, far below play-area
    //if (FLATWORLD)
//      drawBackground(1);
    // draw second, transparent starry background quad, closer than solid_bg

    //automatic camera move code
//    float camera_x_shift = 0.0f; //1.0f*mouse_x - VIEWWIDTH/2.0f;
//    float camera_y_shift = 0.0f; //1.0f*mouse_y - VIEWHEIGHT/2.0f;
//    camera_x_shift = (float) camera_x;
//    camera_y_shift = (float) camera_y;
//    float camera_z_shift = (float) camera_z;

    Vector re = new Vector(3,3);    re.add(0, new Double(0.0));    re.add(1, new Double(0.0));    re.add(2, new Double(0.0));
    Vector fac_xyz = re; //getDefaultCameraXYZ();
    Faction fa = getPlayerFaction();
    if (fa != null) {
      if (fa.getStarbase() != null) //fix nullptr
        fac_xyz = fa.getStarbase().getXYZ();
    }

    //"fun" camera_z = camera_z - 10.0f;
    double cam_oo = INIT_CAMERA_Z_DIST + (float)camera_z;
    setCamera(fac_xyz, (float)cam_oo); //camera AFTER factions exist.
    //setLightPosition(GL_LIGHT2, (float)camera_x, (float)camera_y, (float)camera_z);
    double cam_isnearcam_dist = cam_oo * 0.89; //for isNearCamera_2d
    visualscaling = 1.3f; //for close camera
    float aste_visualscaling = 1.4f; //for close?? camera
//    if (cam_foo > 70000.0) {
//      visualscaling = (float) (cam_foo / 100000.0);
//      aste_visualscaling = 2.3f;  //(float) (0.95*cam_foo / 100000.0);
//    }

    //if (gimmeRandDouble() < 0.02)
    //  System.out.println(worldTimeElapsed + ": Camera: showing range-ish "+cam_foo);

    // draw all within-view asteroids
    MobileThing.textureUnknAste.bind();
    GL11.glEnable(GL_CULL_FACE); //---fixes the sphere drawing problem!
    Missilemada2.setOpenGLMaterial("ASTEROID");
    int aco = 0;
    int listsize = asteroidList.size();
    for (int aa = 0; aa < listsize; aa++) {
      Asteroid as = (Asteroid) asteroidList.elementAt(aa);
      if (as != null && as.isNearCamera_2d(camera_xyz, 1.3*cam_isnearcam_dist) ) { //xx speedup
        if (isAsteroidKnownToPlayerFaction(as)) {
          as.drawAsteroid(aste_visualscaling);
          aco++;
        } else {
          //as.drawYourFakeSprite(aste_visualscaling);
          as.drawAsteroid(aste_visualscaling);
        }
      }
    }
    //if (gimmeRandDouble() < 0.02)
    //  System.out.println("Camera: drew "+aco+" asteroids.");

    // draw all ships within-view, alive. incl starbases.
    //Faction pf = getPlayerFaction();
    Missilemada2.setOpenGLMaterial("SHIP");
    GL11.glEnable(GL_DEPTH_TEST);
    listsize = shipList.size();
    int j = 0;
    int shipdrawcount = 0;
    for (; j < listsize; j++) {
      Ship p = (Ship) shipList.elementAt(j);
      if (p != null /*&& p.isNearCamera_2d(camera_xyz, cam_isnearcam_dist)*/) {
        //draw only if belongs to player faction or has been spotted by pf recently.
        if (p.isSeenByPlayer()) {
          p.drawShip(0.97f*visualscaling, (camera_z > 0.18*world_x_max) ); /*draw health bar if far zoom*/
          shipdrawcount++;
        }
      }
    }
    //player ships INFOTEXT which is different OPENGL MATERIAL.
    //drawtext sets: Missilemada2.setOpenGLMaterial("FONT"); ,not costly op.
    j = 0;
    for (; j < listsize; j++) {
      Ship p = (Ship) shipList.elementAt(j);
      if (p != null && p.isNearCamera_2d(camera_xyz, cam_isnearcam_dist) && camera_z < 0.07*world_x_max) { //only when close zoom.
        if (p.getFaction() == getPlayerFaction())
          p.drawShipInfoText();
      }
    }
    // draw all derelict ships within-view, no infotext ever, dull colour
    Missilemada2.setOpenGLMaterial("SHIP");
    listsize = deadShipList.size();
    for (j = 0; j < listsize; j++) {
      Ship p = (Ship) deadShipList.elementAt(j);
      if (p != null /*&& p.isNearCamera_2d(camera_xyz, cam_isnearcam_dist)*/) {
        //draw only if usesensors tagged it.
        //xxxtemp if (p.isSeenByPlayer()) {
          p.drawShip(0.97f*visualscaling, false);
        //}
      }
    }

    //---sphere drawing over, let's not cull all the flats.---
    GL11.glDisable(GL_CULL_FACE);


    //----draw all within-view 2D sprites, assume all have texture.
    Missilemada2.setOpenGLMaterial("BASIC");
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    //GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.
    //set emission
    GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_EMISSION);
    GL11.glColor4f(0.33f, 0.33f, 0.39f, 1.0f);
    //back to default:set bounced light
    GL11.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);

    //temp ones
    listsize = flatsprite_temporary_List.size();
    for (j = 0; j < listsize; j++) {
      FlatSprite p = (FlatSprite) flatsprite_temporary_List.elementAt(j);
      if (p != null && p.isNearCamera_2d(camera_xyz, cam_isnearcam_dist))
        if (p.isSeenByPlayer())
          p.drawFlatSprite(2.0f*visualscaling, 0.0f, true);
    }
    //perm ones (clouds, fields)
    listsize = flatsprite_permanent_List.size();
    for (j = 0; j < listsize; j++) {
      FlatSprite p = (FlatSprite) flatsprite_permanent_List.elementAt(j);
      //if (p != null && p.isNearCamera_2d(camera_xyz, cam_isnearcam_dist))
      //  if (p.isSeenByPlayer())
      p.drawFlatSprite(2.0f*visualscaling, 0.0f, true);
    }

    //----draw all within-view VFX, atop stuff, then missiles atop vfx.
    Missilemada2.setOpenGLMaterial("VFX");
    GL11.glEnable(GL_DEPTH_TEST);
    int vsize = vfxList.size(); //sorta heavy operation, so outside loop
    for (int i = 0; i < vsize; i++) {
      Vfx v = (Vfx) vfxList.elementAt(i);
      if (v != null) {
        if (v.getExpirationTime() < worldTimeElapsed) {
          vfxList.removeElementAt(i); //dangerous coz...
          vsize--; //coz moved vsize outside loop.
          //if (gimmeRandDouble() < 0.01)
          //  System.out.println("DEBUG: vfx list size is "+vsize);
        } else {
          if (v.isNearCamera(camera_xyz, cam_isnearcam_dist))
            v.drawVfx(12.0f*visualscaling, worldTimeElapsed);
        }
      }
    }

    //draw all missiles within-view
    Missilemada2.setOpenGLMaterial("MISSILE");
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    //xxxxx bind missile texture? speeds up a little if here.
    listsize = missileList.size();
    j = 0;
    for (; j < listsize; j++) {
      Missile p = (Missile) missileList.elementAt(j);
      if (p != null /*&& p.isNearCamera_2d(camera_xyz, cam_isnearcam_dist)*/) {
        //draw only if belongs to player faction or has been spotted by pf recently.
        if (p.isSeenByPlayer()) {
          p.drawMissile(0.4f + 0.81f * visualscaling);
        }
      }
    }

    //draw HUD on top
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    drawHUDAndControls();
  }
  private static void drawHUDAndControls() {
    glPushMatrix();
    glDisable(GL_DEPTH_TEST); //HUD is on top on other stuff

    //swap to ortho mode, default projection, for the 2D HUD.
    glMatrixMode(GL_PROJECTION); //into camera changing.
    glPushMatrix();
    glLoadIdentity();
    glOrtho(0.0/*left*/, VIEWWIDTH/*right*/, VIEWHEIGHT/*bottom*/, 0.0/*top*/, 900.0, -900.0);
    glMatrixMode(GL_MODELVIEW); //out of camera changing.
    glLoadIdentity(); //at screen origin

    //glDisable(GL_CULL_FACE);
    //no need coz disabled depth test: glClear(GL_DEPTH_BUFFER_BIT);

    //material:
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] emission = {0.9f, 0.9f, 0.9f, 1.0f};
    float[] amb_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());


/*
    //tessssssstxxx;
    Sphere s = new Sphere();
    s.setTextureFlag(false);
    s.setDrawStyle(GLU.GLU_FILL); //fill, line, silhouette, point
    GL11.glColor4f(0.1f, 0.2f, 0.4f, 1.0f); //change alpha for ghostly orbs.
    s.draw(10000.0f*(float)9000.0f*10.0f, 9,9);
*/

    //NEW: draw all GUIButtons -STAGE1 bg- from list.
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    Missilemada2.setOpenGLMaterial("BEAM");
    Missilemada2.setOpenGLTextureBeam();

    GL11.glColor4f(0.9f, 0.9f, 0.9f, 1.0f); // ?
    int listsize = GUIButtonList.size();
    for (int j = 0; j < listsize; j++) {
      GUIButton p = (GUIButton) GUIButtonList.elementAt(j);
      if (p != null) {
        p.draw_parts();
      }
    }
    //NEW: draw all GUIButtons -STAGE2 text- from list.
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    Missilemada2.setOpenGLMaterial("FONT");
    GL11.glColor4f(0.9f, 0.9f, 0.9f, 1.0f);
    //listsize = GUIButtonList.size();
    for (int j = 0; j < listsize; j++) {
      GUIButton p = (GUIButton) GUIButtonList.elementAt(j);
      if (p != null) {
        p.drawText(Color.yellow);
      }
    }

    float hud_x = 0.0f;
    float hud_y = 0.0f; //from top left
    //float hud_z = 0.0f;
    float sizing = 1.0f;
    float rowheight = 45.0f * sizing;

    GL11.glColor4f(0.9f, 0.9f, 0.9f, 1.0f);
    //FlatSprite bg = new FlatSprite(2000f, -2000f, 2000f, -2000f, 1.0, "gneiss.png", 12.0);
    //bg.drawFlatSprite(1.0f);

    String tx;

    //draw debug/HUD texts
    Faction pf = getPlayerFaction();
    hud_x = 5.0f;
    hud_y = 500.0f; //from top left
    rowheight = 23.0f;

    //put scout/miner/milship counts up there near the command buttons
    tx = "(" + getPlayerFaction().getScoutCount() + ")"; font20.drawString(99, 14, tx, new Color(250, 50, 50));
    tx = "(" + getPlayerFaction().getMilCount() + ")";   font20.drawString(99, 14+29, tx, new Color(250, 50, 50));
    tx = "(" + getPlayerFaction().getMinerCount() + ")"; font20.drawString(99, 14+2*29, tx, new Color(250, 50, 50));

    tx = getPlayerFaction().factionResourcesAsString();
    font30.drawString(hud_x, hud_y - 2*rowheight, tx, new Color(250, 50, 50));

      double basestr;
      Ship base = pf.getStarbase();
      if (base == null) {
        basestr = 0.0;
      } else {
        basestr = base.getBattleStr();
      }

    double hours = worldTimeElapsed / (3600.0);
    int days = (int)(worldTimeElapsed / (3600.0 * 24.0));
    int hours_left_over = (int) Math.round(hours - (days * 24.0));
    tx = "Time: "+ days + " d "+hours_left_over+" h. Our battle rating: "+(int)((pf.getBattleStrengthTotal()-basestr) / 1000000.0)+""; //important number for player -- too low relative to foe, causes gameover!;
    font20.drawString(hud_x, hud_y - 0.8f*rowheight, tx, new Color(250, 50, 50));

    tx = "World: ships="+shipList.size() + " misl="+missileList.size() + " vfx="+vfxList.size();
    font20.drawString(hud_x, VIEWHEIGHT-20f, tx, new Color(250, 50, 50));
    tx = "tempFlatSprites="+ flatsprite_temporary_List.size() + " meas.FPS="+(int)measured_FPS;
    font20.drawString(hud_x, VIEWHEIGHT-42f, tx, new Color(250, 50, 50, 255));

    tx = /*"Our base str "+(int)(basestr / 1000000.0)
            +", " + */
          "Mining capacity: "+(int)((pf.getMiningCapaTons()) / 10.0)
            +" Scout dist:"+(int)(pf.getScouting_distance_avg()/ 100000.0);
    font20.drawString(hud_x, hud_y, tx, new Color(250, 50, 50, 255));

    tx = "Crew available:"+pf.getCrewIdleCount()+" Total:"+pf.getCrewAlive()+" Lost:"+pf.getCrewDeadCount();
    font30.drawString(hud_x, hud_y - (3.11f * rowheight), tx, new Color(250, 50, 50, 255));

    //double scora = getPlayerFaction().getScouting_distance_avg();
    //tx = "player scoutrange "+(int)scora + " km ("+(int)(scora/BASEDIST)+"x basedist)";
    //font20.drawString(hud_x, hud_y + 4 * rowheight, tx, new Color(250, 50, 50));


    //tx = "mx="+mouse_x + " my="+mouse_y;
    //font20.drawString(hud_x, hud_y + 6 * rowheight, tx, new Color(250, 50, 50));

    //draw message log panel.
    drawMessageLog();

    //draw my ships leaderboard (toggleable)
      //draw scoutreport_ship count?
    if (show_leaderboard)
      drawLeaderBShipsPanel();
    if (show_help) {
      GL11.glColor4f(0.2f, 0.35f, 0.2f, 1.0f); //green background of panel
      drawTextPanel(helppaneltext, 5.0f/*x*/, 527.0f/*y*/, 780.0f/*width*/, 16.0f* 9f + 25.0f/*total hei*/, 16.0f/*rowhei*/);
    }

    //draw faction score (total battle str plus ship_cost destroyed?)


    //DONT: draw camera move buttons, six

    //get out of ortho camera mode.
    glEnable(GL_CULL_FACE);
    glMatrixMode(GL_PROJECTION);
    glPopMatrix(); //back to previous camera mode.
    glMatrixMode(GL_MODELVIEW);
    glEnable(GL_DEPTH_TEST); //HUD is on top on other stuff, so it had no depth test.
    glPopMatrix();
  }
    private static void drawMessageLog() {
      float hud_x;
      float hud_y;
      float rowheight;
      hud_x = 5.0f;
      hud_y = 527.0f; //from top left
      rowheight = 19.0f;

      String tx;
        //material:
            ByteBuffer buffer2 = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
            float[] emission2 = {0.4f, 0.4f, 0.4f, 0.5f};
            float[] amb_diffuse2 = {0f, 0f, 0f, 0f};
            GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer2.asFloatBuffer().put(emission2).flip());
            GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer2.asFloatBuffer().put(amb_diffuse2).flip());

        int colorcode = 0; //default 0
        Color colo = new Color(170, 170, 190, 255/*this matters*/);
        int sizz = hudMsgList.size();
        for (int i = 0; i < (MAXLINES_hudMsgList); i++) {
          tx = "";
          try {
            if (i < sizz) {
              tx = (String) hudMsgList.get(i);
              colorcode = new Integer(tx.substring(0,1));
              tx = tx.substring(1); //cut colorcode away from start of string.
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          if (colorcode == 0) {
            colo = new Color(140, 150, 150, 225/*this matters*/); //gray, low importance reports
          } else if (colorcode == 1) {
            colo = new Color(230, 80, 80, 255/*this matters*/); //red bad news
          } else if (colorcode == 2) {
            colo = new Color(70, 210, 80, 255/*this matters*/); //green good news
          } else if (colorcode == 3) {
            colo = new Color(170, 170, 30, 255/*this matters*/); //yellow important
          }

          font20.drawString(hud_x, hud_y + i * rowheight, tx, colo);
        }
    }
  private static void drawLeaderBShipsPanel() {
    float hud_x = 633.0f; //from top left
    float hud_y = 28.0f;
    float rowheight = 16.0f;

    String tx;

    //draw background rectangle
    float zcoord = 0.0f;
    float xplace = hud_x;
    float yplace = hud_y+4.0f;
    float wid = VIEWWIDTH - hud_x;
    float hei = rowheight * 14 + 5.0f;
    //we are in HUD-drawing coords.
    GL11.glColor4f(0.2f, 0.2f, 0.35f, 0.5f); //dark gray
    //set texture
    texture_panel.bind();
    //draw quad
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glTexCoord2f(0, 0);
    GL11.glVertex3f((float)xplace, (float)yplace, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glTexCoord2f(0, 1);
    GL11.glVertex3f((float)xplace+wid, (float)yplace, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glTexCoord2f(1,1);
    GL11.glVertex3f((float)xplace+wid, (float)yplace+hei, (float)zcoord);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glTexCoord2f(0,1);
    GL11.glVertex3f((float)xplace, (float)yplace+hei, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glEnd();

    //material:
    ByteBuffer buffer2 = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] emission2 = {0.4f, 0.4f, 0.4f, 0.5f};
    float[] amb_diffuse2 = {0f, 0f, 0f, 0f};
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer2.asFloatBuffer().put(emission2).flip());
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer2.asFloatBuffer().put(amb_diffuse2).flip());

  //for player faction:
    Color color_unhurt = new Color(170, 170, 190, 255/*this matters*/);
    Color color_hurt = new Color(190, 80, 110, 255/*this matters*/);
    int numships = getPlayerFaction().getShipCount();

    //sort by shoving from master alive shiplist into tree and reading in order.
    //also shove spotted foes into 2nd tree.
    Comparator<Ship> comparator = new Ship.ShipComparator();
    TreeSet plrshipstoleaderboard = new TreeSet(comparator);
    TreeSet seenfoes = new TreeSet(comparator);
    Faction pf = getPlayerFaction();
    int listsize = shipList.size();
    for (int j = 0; j < listsize; j++) {
      Ship tryship = (Ship) shipList.elementAt(j);
      if (tryship != null) {
        if (tryship.getFaction() == pf && !tryship.isSenSat() ) { //ignore immobile SENSATs
          plrshipstoleaderboard.add(tryship);
        } else {
          if (tryship.isSeenByPlayer() && !tryship.isSenSat() ) { //ignore immobile SENSATs
            seenfoes.add(tryship);
          }
        }
      }
    }
    //read from TreeSet in order, draw text.
    int i = 0;
    float off1 = 320.0f; //2nd column for aligning
    float off2 = 118.0f; //3rd column for aligning
    Color usecolor;
    try {
      Iterator it = plrshipstoleaderboard.descendingIterator();
      while (it.hasNext() && i < 15) { //max 20 rows
        Ship s = (Ship) it.next();
        tx = s.toStringLeaderboard();
        //draw as columns, chop string off at _ separator.
        String[] foo = tx.split("_");

        //if hull dama, diff color.
        if (s.getShieldPerc() < 0.25 || s.getHullPerc() < 0.94) {
          usecolor = color_hurt;
        } else {
          usecolor = color_unhurt;
        }
        font20.drawString(hud_x, hud_y + i * rowheight, foo[0], usecolor);
        font20.drawString(hud_x + off1, hud_y + i * rowheight, foo[1], usecolor);
        font20.drawString(hud_x + off1+off2, hud_y + i * rowheight, foo[2], usecolor);

        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    //for plr seen foes: already have them in a Tree.
    Iterator it2 = seenfoes.descendingIterator();
    int j = 0; //rows written
    usecolor = color_unhurt;
    float hud_y_next = hud_y + (i) * rowheight;
    font20.drawString(hud_x, hud_y_next + j * rowheight, "----spotted competitor ships----", color_unhurt);
    j++;
    while (it2.hasNext() && j < 13) { //max 12 rows
      Ship s = (Ship) it2.next();
      tx = s.toStringLeaderboard();
      //draw as columns, chop string off at _ separator.
      String[] foo = tx.split("_");
      font20.drawString(hud_x, hud_y_next + j * rowheight, foo[0], usecolor);
      //foes: not show seecount,danger,cargo.
      //font20.drawString(hud_x + off1, hud_y_next + j * rowheight, foo[1], usecolor);
      font20.drawString(hud_x + off1+off2, hud_y_next + j * rowheight, foo[2], usecolor);
      j++;
    }
  }
  private static void drawTextPanel(String[] textrows, float hud_x, float hud_y, float wid, float hei, float rowhei) {
    //help panel on top of HUDmsglog older msgs! yay!
    //draw background rectangle
    float zcoord = 0.0f;
    float xplace = hud_x;
    float yplace = hud_y - 8.0f;
    //we are in HUD-drawing coords.
    texture_panel.bind(); //set texture
    //material:
    ByteBuffer buffer2 = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] emission2 = {0.4f, 0.4f, 0.4f, 0.5f};
    float[] amb_diffuse2 = {0f, 0f, 0f, 0f};
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer2.asFloatBuffer().put(emission2).flip());
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer2.asFloatBuffer().put(amb_diffuse2).flip());

    //draw quad
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glTexCoord2f(0, 0);
    GL11.glVertex3f((float) xplace, (float) yplace, (float)zcoord);
    GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glTexCoord2f(0, 1);
    GL11.glVertex3f((float) xplace + wid, (float) yplace, (float)zcoord);
    GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glTexCoord2f(1,1);
    GL11.glVertex3f((float) xplace + wid, (float) yplace+hei, (float)zcoord);
    GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glTexCoord2f(0,1);
    GL11.glVertex3f((float) xplace, (float) yplace+hei, (float)zcoord);
    GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glEnd();

    Color color_textpaneltext = new Color(220, 220, 220, 255/*this matters*/);
    int i = 0;
    while (i < 10) {
      if (i < textrows.length)
        font20.drawString(hud_x + 15.0f, hud_y + i * rowhei, textrows[i], color_textpaneltext);
      i++;
    }
  }
    public static void addVfxOnMT(double x/*is offset, if attached.*/, double y, double z, String in_fx_str, int dur /*ms*/, double siz, double tra,
                                MobileThing attachedto, String texture_filename, double in_texturescaling, String in_text) {
    //xxverify inputs...
    Vfx v = new Vfx(x, y, z, in_fx_str, dur, siz, tra, attachedto, texture_filename, in_texturescaling, in_text);
    if (v != null && vfxList.size() < 2000) {
      vfxList.add(v);
    } else {
      //oh crap, way too many vfx. probably from debug vfxs.
      int debugfoo = 0;
    }
  }

  public static void addVfx2(Vector xyz, String in_fx_str, int dur /*world sec*/, double siz, double tra,
                             String texture_filename, double in_texturescaling, String in_text) {
    if (xyz == null) {
      System.out.println("addVfx2: location was null. "+in_fx_str + " "+in_text);
    } else {
      addVfxOnMT(((Double) xyz.get(0)).doubleValue(), ((Double) xyz.get(1)).doubleValue(), ((Double) xyz.get(2)).doubleValue(),
              in_fx_str, dur, siz, tra, null/*not attached to a mobilething!*/, texture_filename, in_texturescaling, in_text);
    }
  }
  public void advance_time() {
    double time_a_ms = System.nanoTime() / 1000000.0;
//    if (show_splashscreen && worldTimeElapsed > 10) {
//      pause_and_infotext = splashscreen;
//    }
//    //if have infotext, we are paused and draw infotext.
//    if (pause_and_infotext.length() > 2) {
//      if (worldTimeIncrement > 0) {
//        prev_wti = worldTimeIncrement;
//        worldTimeIncrement = 0; //pauses like spacebar.
//      } //else are already paused.
//
//      drawTextXYZ(font60, VIEWWIDTH*0.3f, VIEWHEIGHT*0.45f, 30.0f, pause_and_infotext, Color.white);
//    }

    // generic game engine: move the lights. EVEN WHEN PAUSED?

    if (worldTimeIncrement < 1) {
      return; // the pause.
    }
    //camera_x = camera_x + (0.07*Math.sqrt(camera_z)); //drift the camera, to look more 3D.

    //world goes x seconds forward.
    worldTimeElapsed += worldTimeIncrement;

    toomuchnotescounter = toomuchnotescounter - (1.0/8.0); //cut down on sound spamming. // 8 timeticks for new note?
    //play 0-N notes from from melody queue
    if (checkAndPlayNoteFromQue()) {
      toomuchnotescounter = toomuchnotescounter - (1.0/8.0);
    }

    //xxxxxxx----ambient vfx:


    checkPlayAmbientSounds();

    //clear "remember single explosion, for X seconds(aka timetick), for misl hitting the explosion"
    Missilemada2.setCurrentExplosion(null, 0.0);
    //expire dynamic lights if their time is up.
    updateDynamicLights();

    //-------------------------------------
    //stuff missiles into RTree for spatial indexing. Added 2013-08-03 in Missilemada1.
    si_misl = new RTree();
    si_misl.init(null);
    int mlistsize = missileList.size();
    missileArr = new Missile[mlistsize];
    Missile m1;
    float lee_mis = (float) (getMissileCollisionRangeMin()); //leeway to convert point-like missiles into rectangles.
    if (mlistsize > 0) {
      for (int i = 0; i < mlistsize; i++) { //for each misl, shove it into unchanging-list and rect into spatialindex.
        try {
          missileArr[i] = (Missile) missileList.get(i); //populate global unchanging-this-timetick array... *sigh*
        } catch (Exception e) {
          System.out.println("Exception in getting from mis list: " + e.toString()) ;
        }
        m1 = missileArr[i];
        if (m1 != null) {
          Rectangle rect = new Rectangle((float) (m1.getX()-lee_mis), (float) (m1.getY()-lee_mis), (float) (m1.getX()+lee_mis), (float) (m1.getY()+lee_mis));
          si_misl.add(rect, i);
        }
      }
    }
    //-------------------------------------
    //stuff ships (30-55 per faction) into RTree for spatial indexing. This speedup was added 2013-12-27.
    si_ship = new RTree();
    si_ship.init(null);
    int slistsize = shipList.size();
    shipArr = new Ship[slistsize];
    Ship s1;
    float lee_sh = 5.0f; //leeway to convert point-like ships into rectangles.
    if (slistsize > 0) {
      for (int i = 0; i < slistsize; i++) { //for each ship, shove it into unchanging-list and corresponding id rect into spatialindex.
        try {
          shipArr[i] = (Ship) shipList.get(i); //populate global unchanging-this-timetick array... *sigh*
        } catch (Exception e) {
          System.out.println("Exception in getting from ship RTree list: " + e.toString());
        }
        s1 = shipArr[i];
        if (s1 != null) {
          Rectangle rect = new Rectangle((float) (s1.getX()-lee_sh), (float) (s1.getY()-lee_sh), (float) (s1.getX()+lee_sh), (float) (s1.getY()+lee_sh));
          si_ship.add(rect, i);
        }
      }
    }
    //-------------------------------------

    //----time-tick for all Factions, dead or alive
    try {
      Faction fa;
      double biggest_bs = 0.0;
      double player_bs = 0.0;
      for (int i = 0; i < factionList.size(); i++) {
        fa = (Faction) factionList.elementAt(i);
        if (fa != null && worldTimeIncrement > 0) {
          fa.advance_time(worldTimeIncrement);

          //compare total faction battlestrength.
          double bs = fa.getBattleStrengthTotal();
          if (fa.equals(getPlayerFaction())) {
            player_bs = bs;
          }
          if (bs > biggest_bs) {
            biggest_bs = bs;
          }
        }
      }
      if (biggest_bs > 1.5 * player_bs) {
        if (gimmeRandDouble() < 0.002)
          addToHUDMsgList("Reports indicate we are losing the race! President is about to call a withdrawal!", 1);
        player_abouttolose_counter++; //120 times per player second??
        if (player_abouttolose_counter > 70*120) {
          gameScenarioLost();
        }
      }
      if ((int) biggest_bs == (int) player_bs) {
        if (gimmeRandDouble() < 0.0012)
          addToHUDMsgList("Reports indicate we are leading!");
      }

    } catch (Exception e) {
      System.out.println("Faction advance_time() exception:");
      e.printStackTrace();
    }
    //time-tick for all alive ships
    try {
      Ship sh;
      int shsiz = shipList.size();
      if ( shsiz > 0) {
        for (int i = 0; i < shsiz; i++) {
          sh = (Ship) shipList.elementAt(i);
          if (sh != null  && worldTimeIncrement > 0) {
            if (!sh.isDestroyed()) //if not destroyed, it gets time.
              sh.advance_time(worldTimeIncrement);
          }
          shsiz = shipList.size();
        }
      }
    } catch (Exception e) {
      System.out.println("Ship advance_time() exception:");
      e.printStackTrace();
    }
    //time-tick for all derelict Ships - they just float along.
    try {
      Ship ded;
      int shsiz = deadShipList.size();
      if ( shsiz > 0) {
        for (int i = 0; i < shsiz; i++) {
          ded = (Ship) deadShipList.elementAt(i);
          if (ded != null) {
              ded.advance_time_derelict_drift(worldTimeIncrement);
          }
          shsiz = deadShipList.size();
        }
      }
    } catch (Exception e) {
      System.out.println("DERELICT ship advance_time() exception:");
      e.printStackTrace();
    }
    //time-tick for all Missiles
    try {
      Missile b;
      int msiz = missileList.size();
      if ( msiz > 0) {
        for (int i = 0; i < msiz; i++) {
          b = (Missile) missileList.elementAt(i);
          if (b != null)
            b.advance_time(worldTimeIncrement);
          msiz = missileList.size();
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }

    //time-tick for all Asteroids, they float around aimlessly.
    try {
      Asteroid as;
      int siz = asteroidList.size();
      if ( siz > 0) {
        for (int i = 0; i < siz; i++) {
          as = (Asteroid) asteroidList.elementAt(i);
          if (as != null)
            as.advance_time(worldTimeIncrement);
          siz = asteroidList.size();
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }

    //time-tick for all temp decorative sprites
    try {
      FlatSprite tempdecor;
      for (int i = 0; i < flatsprite_temporary_List.size(); i++) {
        tempdecor = (FlatSprite) flatsprite_temporary_List.elementAt(i);
        if (tempdecor != null) {
          tempdecor.advance_time(worldTimeIncrement);
          //xxx should delete insted of hide from player, coz they're non-interactive, BUTTTTTT they can indicate combat and mining operations...
          //if (gimmeRandInt(400) < 5)
          //  tempdecor.setIsSeenByPlayer(false);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
    //time-tick for all perm decorative sprites
    try {
      FlatSprite perm;
      for (int i = 0; i < flatsprite_permanent_List.size(); i++) {
        perm = (FlatSprite) flatsprite_permanent_List.elementAt(i);
        if (perm != null) {
          perm.advance_time(worldTimeIncrement);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }

    logicTimeMeasuredms = (System.nanoTime() / 1000000.0) - time_a_ms;
  }

  private void checkPlayAmbientSounds() { //called on every time-tick.
    //----ambient sounds: low chance, bass note.
    if (gimmeRandDouble() < 0.0004) {

      double rand = gimmeRandDouble();
      if (rand > 0.0 && rand < 0.2) {
        putNotes(strIntoMelody("01abc", 3, ""), 39 /*core note*/, 94 /**/, 55/*vol*/, 15.3F /*note duration*/);
      }
      if (rand > 0.2 && rand < 0.4) {
        putNotes(strIntoMelody("Dbc", 3, ""), 19 /*core note*/, 98 /*soundtrack*/, 95/*vol*/, 15.3F /*note duration*/);
      }
      if (rand > 0.4 && rand < 0.6) {
        putNotes(strIntoMelody("zerportr", 2, ""), 39 /*core note*/, 97 /*rain*/, 85/*vol*/, 11.9F /*note duration*/);
      }
      if (rand > 0.6 && rand < 0.8) {
        putNotes(strIntoMelody("1kritoks", 4, ""), 41 /*core note*/, 101 /*brightness*/, 115/*vol*/, 18.3F /*note duration*/);
      }
      if (rand > 0.8 && rand < 0.90) {
        putNotes(strIntoMelody("maxtyox", 6, ""), 15 /*core note*/, 85 /*charang*/, 77/*vol*/, 16.3F /*note duration*/);
      }
      if (rand > 0.9 && rand < 0.999) {
        putNotes(strIntoMelody("kurax", 16, ""), 16 /*core note*/, 85 /*charang*/, 67/*vol*/, 19.3F /*note duration*/);
      }

      //addToHUDMsgList("rand ambient now");


    }

  }

  public static Vector getShipList() {
    return shipList;
  }
  public static Vector getBaseList() {
    return starBaseList;
  }
  public static boolean addToShipList_withcrewcheck(Ship s) { //ship _really_ gets added to the world, the "new Ship()" was just 'trying'.
    if (s != null) {
      if (s.isStarbase() || s.getCrewCount() == 0) {
        shipList.add(s); //ship becomes "real" from this
        return true;
      } else { // need to check if crew available.
        int req_crewcount = s.getCrewCount();
        int fac_avail_idlecount = s.getFaction().getCrewIdleCount();

        if (s.parentFaction.hasEnoughIdleCrew(req_crewcount)) { //if enough idlers, realify ship.
          s.parentFaction.assignCrewToShip(s, req_crewcount); //N idlers into assigned ones
          shipList.add(s); //ship becomes "real" from this
          return true;
          //Missilemada2.addToHUDMsgList("new ship: yeah fac had enough crew.");
        } else { //error not enough crew.
          //Missilemada2.addToHUDMsgList("new ship: faction doesn't have enough idle crew.");
          //xxxxx
          //faction notify not enuf crew
          s.getFaction().messageNotEnoughCrewIdle(req_crewcount - fac_avail_idlecount);
          return false;
        }
      }
    } else {
      throw new NullPointerException("tried to add nullptr ship to shiplist.");
      //return false;
    }
  }
  public static void addToBaseList(StarBase s) {
    if (s != null)
      starBaseList.add(s);
  }
  public static TextureLoaderMy getTextureLoader() {
    if (textureLoader != null)
      return textureLoader;
    else throw new NullPointerException("argh no textureloader");
  }
  private static void drawBackground(int which) {
    //material:
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] emission = {0.2f, 0.0f, 0.2f, 1.0f};
    float[] amb_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
    GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());

    if (which == 1) {
      GL11.glColor4f(0.9f, 0.9f, 0.9f, 1.0f);
      FlatSprite bg = new FlatSprite(2.0*world_x_max, 2.0*world_y_max, world_x_min, world_y_min, -8000.0, "stars_bg.png", 12.0, 1.0f/*transp*/);
      bg.setSpeed(0.0, 0.0, 0.0); //no-one updates this flatsprite unless it's in a list.
      bg.drawFlatSprite(1.0f, 0.0f, true);
    }
    if (which == 2) {
      FlatSprite bg = new FlatSprite(1.0*world_x_max, 1.0*world_y_max, world_x_min, world_y_min, -getSensorRangeMinShip(), "stars_transparent_bg.png", 6.0, 1.0f/*transp*/);
      GL11.glColor4f(0.9f, 0.9f, 0.9f, 1.0f);
      bg.setSpeed(0.0, 0.0, 0.0); //no-one updates this flatsprite unless it's in a list.
      bg.drawFlatSprite(1.0f, 0.0f, true);
    }
  }
  public static void addToFSTempList(FlatSprite fs) {
    if (fs != null)
      flatsprite_temporary_List.add(fs);
  }
  public static void addToFSPermList(FlatSprite fs) {
    if (fs != null)
      flatsprite_permanent_List.add(fs);
  }
  public static int getMouseX() {
    return mouse_x;
  }
  public static int getMouseY() {
    return mouse_y;
  }
  public static double calcBearingXY(MobileThing a, MobileThing b) { //asker, target
    double bx = b.getX();
    double by = b.getY();
    float dx = (float)(bx - a.getX());
    float dy = (float)(- (by - a.getY()));
    double ra = ATAN2Lookup.atan2(dy, dx); //out comes -pi to +pi
    if (ra < 0)
      ra = Math.abs(ra);
    else //WTF huh
      ra = 2*Math.PI - ra;
    //System.out.println("calcBearingXY2D: dx="+dx+ " dy="+dy+" and bearingXY="+ra );
    return ra;
  }
  public static double calcBearingXZ(MobileThing a, MobileThing b) { //asker, target
    double bx = b.getX();
    double bz = b.getZ();
    float dx = (float)(bx - a.getX());
    float dz = (float)(- (bz - a.getZ()));
    double ra = ATAN2Lookup.atan2(dz, dx); //out comes -pi to +pi
    if (ra < 0)
      ra = Math.abs(ra);
    else //WTF huh
      ra = 2*Math.PI - ra;
    //System.out.println("calcBearingXZ: dx="+dx+ " dz="+dz+" and bearingXZ="+ra );
    return ra;
  }
  public static double calcBearingXY2D(double ax, double ay, double bx, double by) { //asker, target //depending what numbers you give, can calc bearingXZ too.
    float dx = (float)(bx - ax);
    float dy = (float)(- (by - ay));
    double ra = ATAN2Lookup.atan2(dy, dx); //out comes -pi to +pi
    if (ra < 0.0)
      ra = Math.abs(ra);
    else
      ra = 2*Math.PI - ra;
    return ra;
  }
  public static double calcDistanceXY2D(double ax, double ay, double bx, double by) { //asker, target
    return Math.sqrt( (ax - bx)*(ax - bx)  +  (ay - by)*(ay - by) );
  }
  public static void initAmbientLight() {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] ambientglobal = {0.07f, 0.07f, 0.07f, 1.0f};
    GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, buffer.asFloatBuffer().put(ambientglobal));
    GL11.glEnable(GL11.GL_LIGHTING);
    GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, GL11.GL_TRUE);
  }
  public static void initLight(int light_id, float diffuse_str, float specularmain, float x, float y, float z) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] ambient = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] diffuse = {diffuse_str, diffuse_str, diffuse_str, 1.0f};
    float[] specular = {specularmain +0.3f, specularmain +0.3f, specularmain, 1.0f}; //yellow specular
    float[] position = {x,y,z, 1.0f};

    GL11.glLight(light_id, GL11.GL_AMBIENT, (FloatBuffer) buffer.asFloatBuffer().put(ambient).flip());
    GL11.glLight(light_id, GL11.GL_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(diffuse).flip());
    GL11.glLight(light_id, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
    GL11.glLight(light_id, GL11.GL_POSITION, (FloatBuffer) buffer.asFloatBuffer().put(position).flip());

    glLighti(light_id, GL_LINEAR_ATTENUATION, 0);

    // Disable all spot settings
    glLighti(light_id, GL_SPOT_CUTOFF, 180); // no cutoff
    glLighti(light_id, GL_SPOT_EXPONENT, 0); // no focussing

    GL11.glEnable(light_id);
  }
  private static void setLightPosition(int light_id, float xi, float yi, float zi) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    float[] position = {xi, yi, zi, 1.0f};
    GL11.glLight(light_id, GL11.GL_POSITION, (FloatBuffer) buffer.asFloatBuffer().put(position).flip());
  }
  public static void setDynamicLight(String which, float intensity, long timestamp, Vector xyz) {
    if (which.equals("MISSILE_EXP")) {
      initLight(GL11.GL_LIGHT6, intensity/*diffuse*/,  0.5f/*spec*/, 0f,0f,0f);
      GL11.glEnable(GL_LIGHT6);
      setLightPosition(GL_LIGHT6, ((Double)xyz.get(0)).floatValue(), ((Double)xyz.get(1)).floatValue(), ((Double)xyz.get(2)).floatValue() );
      LightExpiryTimestamp_currMisExpl = timestamp;
    } else if (which.equals("SHIP_EXP")) {
      initLight(GL11.GL_LIGHT7, intensity/*diffuse*/,  0.5f/*spec*/, 0f,0f,0f);
      GL11.glEnable(GL_LIGHT7);
      setLightPosition(GL_LIGHT7, ((Double)xyz.get(0)).floatValue(), ((Double)xyz.get(1)).floatValue(), ((Double)xyz.get(2)).floatValue() );
      LightExpiryTimestamp_currShipExpl = timestamp;
    }
  }
  private void updateDynamicLights() {
    //disable light after timestamp past.
    if (LightExpiryTimestamp_currMisExpl < worldTimeElapsed) {
      GL11.glDisable(GL_LIGHT6);
    }
    if (LightExpiryTimestamp_currShipExpl < worldTimeElapsed) {
      GL11.glDisable(GL_LIGHT7);
    }
  }
  public static void initSlickUtilFonts() {
    Font awtFont = new Font(FONTNAME, Font.BOLD, 200);
    font60 = new UnicodeFont(awtFont, FONTSIZE3, false, false);
    try {
      font60.addAsciiGlyphs();
      font60.addGlyphs(400, 600);
      font60.getEffects().add(new ColorEffect(java.awt.Color.WHITE)); //ColorEffect, FilterEffect, GradientEffect, OutlineEffect, OutlineWobbleEffect, OutlineZigzagEffect, ShadowEffect
      font60.loadGlyphs();
    } catch (SlickException e) {
      e.printStackTrace();
    }

    awtFont = new Font(FONTNAME, Font.BOLD, 200);
    font20 = new UnicodeFont(awtFont, FONTSIZE1, false, false);
    try {
      font20.addAsciiGlyphs();
      font20.addGlyphs(400, 600);
      font20.getEffects().add(new ColorEffect(java.awt.Color.WHITE)); //ColorEffect, FilterEffect, GradientEffect, OutlineEffect, OutlineWobbleEffect, OutlineZigzagEffect, ShadowEffect
      font20.loadGlyphs();
    } catch (SlickException e) {
      e.printStackTrace();
    }

    awtFont = new Font(FONTNAME, Font.BOLD, 200);
    font30 = new UnicodeFont(awtFont, FONTSIZE2, false, false);
    try {
      font30.addAsciiGlyphs();
      font30.addGlyphs(400, 600);
      font30.getEffects().add(new ColorEffect(java.awt.Color.WHITE)); //ColorEffect, FilterEffect, GradientEffect, OutlineEffect, OutlineWobbleEffect, OutlineZigzagEffect, ShadowEffect
      font30.loadGlyphs();
    } catch (SlickException e) {
      e.printStackTrace();
    }

    // load font60ttf from a .ttf file
/*
    new UnicodeFont(java.lang.String ttfFileRef, int size, boolean bold, boolean italic)
    try {
      InputStream inputStream	= ResourceLoader.getResourceAsStream("wood sticks.ttf");

      Font awtFont2 = Font.createFont(Font.TRUETYPE_FONT, inputStream);
      awtFont2 = awtFont2.deriveFont(24f); // set font60 size
      font2 = new UnicodeFont(awtFont2);

    } catch (Exception e) {
      e.printStackTrace();
    }
*/
  }
  public static void drawTextXYZ(UnicodeFont fon, float scal, float x, float y, float z, String s, Color c) {
    GL11.glPushMatrix();
    GL11.glLoadIdentity();//must load identity, or remove scaling from vfx.  //not load identity means, use Vfx's scaling.
    // enable texture
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    //GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE); //modulate surface color? GL_DECAL seems worse.

    //TOO CPU-COSTLY: Missilemada2.setOpenGLMaterial("FONT");

    GL11.glRotatef(180, 1.0f,0.0f,0.0f); //x 180deg makes correct side up. compared to normal PNG graphic.
    GL11.glTranslatef(x, -y, -z); //minuses coz rotated.
    GL11.glScalef(scal, scal, scal);
    if (fon != null)
      fon.drawString(0.0f, 0.0f, s, c); //always z = 0 because func is from 2D library.

    //GL11.glDisable(GL11.GL_TEXTURE_2D); //slow
    GL11.glPopMatrix();
  }
  public static void drawTextHUD(UnicodeFont fon, float scal, float x, float y, String s, Color c) {
    GL11.glPushMatrix();
    GL11.glLoadIdentity();//must load identity, or remove scaling from vfx.  //not load identity means, use Vfx's scaling.

    //GL11.glRotatef(180, 1.0f,0.0f,0.0f); //x 180deg makes correct side up. compared to normal PNG graphic.
    GL11.glTranslatef(x, y, 0.0f); //minuses coz rotated.
    GL11.glScalef(scal, scal, scal);
    if (fon != null)
      fon.drawString(0.0f, 0.0f, s, c); //always z = 0 because func is from 2D library.

    GL11.glPopMatrix();
  }
  public static void setOpenGLMaterial(String s) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
    if (s.equals("BASIC") || s.equals("ASTEROID")) {
      float[] emission = {0.01f, 0.01f, 0.01f, 1.0f};
      float[] amb_diffuse = {0.8f, 0.8f, 0.8f, 1.0f};
      float[] specular = {0.12f, 0.12f, 0.07f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 7); //0 to 128 //some stones should have zero specular.
    }
    if (s.equals("SHIP")) {
      //xxxget ship color from shiptype or faction...
      float[] emission = {0.1f, 0.1f, 0.1f, 1.0f};
      float[] amb_diffuse = {0.9f, 0.9f, 0.9f, 1.0f};
      float[] specular = {0.22f, 0.22f, 0.22f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 40); //0 to 128 .
    }
    if (s.equals("NOSPECULAR")) {
      float[] emission = {0.05f, 0.05f, 0.05f, 1.0f};
      float[] amb_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
      float[] specular = {0.02f, 0.02f, 0.02f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 0); //0 to 128
    }
    if (s.equals("UNSCOUTED_ASTEROID")) {//curr almost same as NOSPECULAR
      float[] emission = {0.2f, 0.0f, 0.0f, 1.0f};
      float[] amb_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
      float[] specular = {0.12f, 0.02f, 0.02f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 0); //0 to 128
    }
    if (s.equals("FONT")) {
      float[] emission    = {0.9f, 0.9f, 0.9f, 1.0f};
      float[] amb_diffuse = {0f, 0f, 0f, 1.0f};
      float[] specular    = {0f, 0f, 0f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 120); //0 to 128
    }
    if (s.equals("BEAM")) {
      float[] emission    = {0.3f, 0.3f, 0.3f, 1.0f};
      float[] amb_diffuse = {0.5f, 0.5f, 0.5f, 1.0f};
      float[] specular    = {0f, 0f, 0f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 120); //0 to 128
    }
    if (s.equals("LINE")) { //like beam but a GUI overlay
      float[] emission    = {0.7f, 0.7f, 0.7f, 1.0f};
      float[] amb_diffuse = {0.0f, 0.0f, 0.0f, 1.0f};
      float[] specular    = {0f, 0f, 0f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 120); //0 to 128
    }
    if (s.equals("VFX")) {
      float[] emission = {0.35f, 0.35f, 0.35f, 1.0f};
      float[] amb_diffuse = {1f, 1f, 1f, 1.0f};
      float[] specular = {0.0f, 0.0f, 0.0f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 0); //0 to 128 //some stones should have zero specular.
    }
    if (s.equals("MISSILE")) {
      float[] emission = {0.05f, 0.05f, 0.05f, 1.0f};
      float[] amb_diffuse = {0.9f, 0.9f, 0.9f, 1.0f};
      float[] specular = {0.0f, 0.0f, 0.0f, 1.0f};
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, (FloatBuffer) buffer.asFloatBuffer().put(amb_diffuse).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, (FloatBuffer) buffer.asFloatBuffer().put(emission).flip());
      GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, (FloatBuffer) buffer.asFloatBuffer().put(specular).flip());
      //size of specular highlight:
      GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 0); //0 to 128 //some stones should have zero specular.
    }

  }
  public static double getMiningDistance() {
    return 1.15 * getArrivedDistance();
    //return 59500.0;
    //return 9.3*getArrivedDistance();
    //return 7900.0 * (worldTimeIncrement / 100.0);
  }
  public static double getArrivedDistance() {
    //xxxx PROBLEMATIC.
    return 5300.0 /*xxxx 4200.0*/ * (worldTimeIncrement / 20.0); //distance precision is greater, when 60sec in 60 frames. Lower precision when 90*60sec in 60 frames.
  }
  public static double getNearlyArrivedDistance() {
    return 17.5 * getArrivedDistance();
  }
  public static double getDockingDistance() {
    return 1.27*Missilemada2.getArrivedDistance();
  }
  public static double getScoutingDistance_crude() {
    return 0.25 * BASEDIST;
  }
  public static double getScoutingDistanceMax() {
    return 1.4 * BASEDIST;
  }
  public static double getBaseDistance() {
    return BASEDIST;
  }
  public static double getBaseSpeed() {
    return BASEMOVESPEED;
  }
  public static Vector getShipsOfFaction(Faction fa) { //xxxxis base included?
    Vector ret = new Vector(20,10);
    int siz = shipList.size();
    for (int i = 0; i < siz; i++) {
      Ship s = (Ship) shipList.elementAt(i);
      if (s != null) {
        if (s.getFaction() == fa) {
          ret.add(s);
        }
      }
    }
    return ret;
  }
  public static Vector getShipsOfFactionNearXYZ(Faction fa, Vector xyz, double dist) {
    Vector ret = new Vector(10,10);
    int siz = shipList.size();
    double trydist = 0.0;
    for (int i = 0; i < siz; i++) {
      Ship s = (Ship) shipList.elementAt(i);
      if (s != null) {
        trydist = MobileThing.calcDistanceVecVec(s.getXYZ(), xyz);
        if (s.getFaction() == fa && trydist < dist) {
          ret.add(s);
        }
      }
    }
    return ret;
  }
  public static Vector getShipsWithinOLDB(double x, double y, double z, double range) { //non-RTree version, simple, but bad at 200 misl look at 200 ships.
    Vector ret = new Vector(15,15);
    int siz = shipList.size();
    for (int i = 0; i < siz; i++) {
      Ship s = (Ship) shipList.elementAt(i);
      if (s != null) {
        if (MobileThing.calcDistance(x, y, z, s.getX(), s.getY(), s.getZ()) < range ) {
          ret.add(s);
        }
      }
    }
    return ret;
  }
  public static Vector getDeadShipsWithin(double x, double y, double z, double range) { //don't switch to RTree, so few ships??
    Vector ret = new Vector(5,8);
    int siz = deadShipList.size();
    for (int i = 0; i < siz; i++) {
      Ship s = (Ship) deadShipList.elementAt(i);
      if (s != null) {
        if (MobileThing.calcDistance(x, y, z, s.getX(), s.getY(), s.getZ()) < range ) {
          ret.add(s);
        }
      }
    }
    return ret;
  }
  public static Vector getMissilesWithinOLD(Vector xyz, double range) { //xxxx needs a speedup, RTree again.
    Vector ret = new Vector(4,4);
    int siz = missileList.size();
    for (int i = 0; i < siz; i++) {
      Missile s = (Missile) missileList.elementAt(i);
      if (s != null) {
        if (MobileThing.calcDistanceVecVec(xyz, s.getXYZ()) < range ) {
          ret.add(s); //3% of cpu time when combat. or 10%!
        }
      }
    }
    return ret;
  }
  public static Vector getMissiles_XYNearest_ones(double in_xcoord, double in_ycoord, double range/*sensor radius or misl collision radius*/, int howMany) {
    getMislNearestN_ret = new Vector(howMany+1,20); //reset this global static vector.
    final Point p = new Point((float)in_xcoord, (float)in_ycoord);

    si_misl.nearestN(/*querypoint*/ p, new TIntProcedure() {                           //NullProc_for_misl_idx
      public boolean execute(int i) { //id i matches query, do something on those ids.
        //log.info("Rectangle " + i + " " + rects[i] + ", distance=" + rects[i].distance(p));
        getMislNearestN_ret.add(missileArr[i]); //on hit, shove into global static vector.
        //getMislNearestN_ret.add(missileList.get(i)); //BAD: false result coz missileList lives constantly, expires and explodes.
        return true;
      }
    }, howMany, (float)range /*Float.MAX_VALUE*/);
    /*if (getMislNearestN_ret.size() > 2) {
      System.out.println("getMissiles_XYNearest_ones: returning "+getMislNearestN_ret.size()+" misl in vec.");
    }*/
    return getMislNearestN_ret;
  }
  public static Vector getShips_XYNearest(double in_xcoord, double in_ycoord, double range/*sensor radius or misl collision radius*/, int howMany) {
    getShipNearestN_ret = new Vector(howMany+1,15); //reset this global static vector.
    final Point p = new Point((float)in_xcoord, (float)in_ycoord);

    si_ship.nearestN(/*querypoint*/ p, new TIntProcedure() { //NullProc_for_ship_idx
      public boolean execute(int i) { //id i matches query, do something on those ids.
        //log.info("Rectangle " + i + " " + rects[i] + ", distance=" + rects[i].distance(p));
        getShipNearestN_ret.add(shipArr[i]); //on hit, shove into global static vector.
        return true;
      }
    }, howMany, (float)range /*Float.MAX_VALUE*/);
    /*if (getShipNearestN_ret.size() > 2) {
      System.out.println("getShip_XYNearest_ones: returning "+getShipNearestN_ret.size()+" ships in vec.");
    }*/
    return getShipNearestN_ret;
  }
  public static Vector getAsteroidsWithin(double x, double y, double z, double range) {
    Vector ret = new Vector(15,25);
    int siz = asteroidList.size();
    for (int i = 0; i < siz; i++) {
      Asteroid s = (Asteroid) asteroidList.elementAt(i);
      if (s != null) {
        if (MobileThing.calcDistance(x, y, z, s.getX(), s.getY(), s.getZ()) < range ) {
          ret.add(s);
        }
      }
    }
    return ret;
  }
  public static boolean isAsteroidKnownToFaction(Asteroid asteroid, Faction fa) {
    if (fa.isAsteroidScouted(asteroid))
      return true;
    else
      return false;
  }
  public static boolean isAsteroidKnownToPlayerFaction(Asteroid asteroid) {
    Faction fac = getPlayerFaction();
    if (fac == null) {
     return false;
    } else {
      if (fac.isAsteroidScouted(asteroid))
        return true;
      else
        return false;
    }
  }
  public static Faction getPlayerFaction() {
    Faction ret = null;
    try {
      ret = (Faction) factionList.get(0);
    } catch (ArrayIndexOutOfBoundsException e) {
      addToHUDMsgList("ERROR, Excep in getPlayerFaction",3);
    }
    return ret;
  }
  public static double getCurrVisualScaling() {
    return visualscaling;
  }
  public static double getSensorRangeMinShip() { //gameplay important
    //crap ships may have worse sensors than above-avg missile??
    return 1.0 /*for testing, production at 1.0*/ * 1.40*(250500.0);
  }
  public static double getSensorRangeMaxShip_something() {
    return 1.40*(250500.0)     * 1.58 * (1.0 + 0.47); //elite scout's sensorrange
  }
  public static double getSensorRangeMinMissile() {
    return (0.66) * (240500.0); //xx gameplay important. Might be lots of wasted missiles if they very nearsighted.
  }
  public static double getAsteroidScanningRange() {
    return 0.52 * getSensorRangeMinShip();
  }
  public static double getCombatDistMin_Generic() {
    return 2.0*getArrivedDistance();
  }
  public static double getCombatDistMin(Ship aggressor) {
    //if has beams
    if (aggressor.shield_regen_per_min > 0.05)
      return 0.7 * aggressor.getAttackBeamRange();
    else
      return 2.5*getArrivedDistance(); //relative to getSensorRangeMinMissile() was probably crap.
  }
  public static double getCombatDistMax(Ship aggressor) {
    //if has missiles
    if (aggressor.max_buildcredits > 100.0)
      return 0.8*aggressor.getMyMissileDistKnown();
    else
      return 0.9*aggressor.getAttackBeamRange();
  }
  public static double getMissileRangeMin_Generic() {
    //return 0.20 * BASEDIST;
    return 0.95 * getSensorRangeMinShip();
  }
  public static double getMissileRangeMax() {
    return 0.59 * BASEDIST; //xxxxgameplay. 0.8 might be different gameplay. 1.0x and 2.2x is SHITE.
  }
  public static double getMissileCollisionRangeMin() { //scale to world tick, coz large tick is VERY IMPRECISE. //precision improved by 4x adv time per framedraw.
    return 17300.0 * (worldTimeIncrement / 60.0) ; //26x gives about 14 300 km. when wti is 60.
    //xxone must test at both 15 sec tick and 160 sec tick... well, combat is viewed with slow time..
  }
  public static double getMissileTurnMargin() { //scale to world tick, coz large tick is VERY IMPRECISE. //precision improved by 4x adv time per framedraw.
    return 0.035; //radians. was 0.04 for a long time. was 0.12 then.
  }
  public static double getMissileSpeedMin() {
    //calc from ship speeds.
    return 1.17 * Ship.getAvgScoutSpeed(); //crappiest missiles should catch up to mid-low scouts, barely.
  }
  public static double getMissileSpeedMax() {
    return 1.46 * Ship.getMaxScoutSpeed(); //elite missiles should catch up to average scouts most excellently.
  }
  public static double getAvgMislYield() {
    return Ship.getAvgScoutHullHP() / 19.3; //gameplay: more missiles means more fun! (scout/12: not that fun.)
  }
  public static double getAsteDriftSpeed() {
    return 0.09318; //0.00118; //gameplay
  }
  public static Asteroid getRandomAsteroid() {
    try {
      int i = gimmeRandInt(asteroidList.size());
      return (Asteroid) asteroidList.elementAt(i);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  public static UnicodeFont getFont60fornowwww(int i) {
    //xxxx
    return font60;
  }
  public static void removeMissile(Missile m) {
    missileList.remove(m);
  }
  public static void addToMissileList(Missile m) {
    if (m != null)
      missileList.add(m);
  }
  public static void addToHUDMsgList(String a) {
    addToHUDMsgList(a, 0);
  }
  public static void addToHUDMsgList(String a, int colorcode/*0 default textcolor*/) {
    if (a != null ) {
      if (!doesHUDMsgListContainString(a)) { //a aint duplicate of prev. this reduces spammy.

        hudMsgList.add("" + colorcode + a);
        if (hudMsgList.size() > MAXLINES_hudMsgList)
          hudMsgList.remove(0); //oldest away
        hudMsgList.trimToSize(); //xxcan be omitted?
        System.out.println("HUD: " + a);
      }

    } else {
      System.out.println("HUD: DUPLICATE omitted: "+a);
    }
    if (gimmeRandDouble() < 0.009) { //rare occurrence
      randomRumorEvent();
      changeWorldTimeIncrement(-15); //so player notices better?
    }
  }
  private static boolean doesHUDMsgListContainString(String a) {
    String msg;
    int sizz = hudMsgList.size();
    for (int i = 0; i < sizz; i++) {
      if (a.contentEquals((String) hudMsgList.get(i))) {
        return true;
      }
    }
    return false;
  }
  private static void randomRumorEvent() {
    Faction pf = getPlayerFaction();
    if (pf == null)
      return;
    Ship sh = pf.gimmeRandMannedShip();
    if (sh == null)
      return;
    String[] rumor = { /*0*/"Nav array design discounts this week!",
          "The pilot of "+sh.getName()+" complains of insects.",
          "Professor Ka'thur thinks she's found a new mineral. Again.",
          "The crew of "+sh.getName()+" are glad to be heading back to base.",
          "The crew of "+sh.getName()+" have eaten too many bean rations.",
          "The crew of "+sh.getName()+" are arguing about taxes.",
          "The crew of "+sh.getName()+" are arguing about thruster configurations.",
          /*7*/"The crew of "+sh.getName()+" are testing thruster configurations.",
      /*8*/"The crew of "+sh.getName()+" got a surprise donation from the public! Upgrades!",
          "The crew of "+sh.getName()+" are playing chicken with their own missiles.",
          "The crew of "+sh.getName()+" are playing mahjong.",
          "The "+sh.getName()+" had a micrometeorite accident.",
          "The "+sh.getName()+" lost a retro stabiliser.",
          "The resupply shuttle brought extra noodle rations!",
          "The resupply shuttle brought some purring hairballs.",
          "The resupply shuttle brought contraband retro nudie magazines."
           };
    int eventid = gimmeRandInt(rumor.length);
    //xxtodo: if (eventid == 7 /*thruster*/)
    //  sh.setSpeed_KE_dirXY(xxxx);
    Missilemada2.addToHUDMsgList(Missilemada2.strCurrDaysHours() + rumor[eventid],0);
  }
  public static void removeStarBaseFromPlay(StarBase sb) {
    starBaseList.remove(sb);
    factionList.remove(sb.getFaction());
    //xxxxxxx for all ships of this faction, surrender-join other fac or self-destruct, or fly faaaaaaar south....



    System.out.println("A BASE WAS DESTROYED: "+sb.toString());

    if (sb.getFaction().getFactionId() == getPlayerFaction().getFactionId()) {
      addToHUDMsgList("ALL IS LOST! OUR BASE IS GONE! The President is very disappointed in your operation.",1);
      gameScenarioLost();
    } else {
      //xxfork msg based on if we did it (we in its vicinity) or someone else did it.
      addToHUDMsgList("Commander! An enemy starbase (str "+sb.getBattleStr()+") has been destroyed!",3);
    }
  }
  public static int getFPS() {
    return FPS;
  }
  public static boolean isLocationACombatPlaceBAADDD(Faction askingfaction, Vector xyz) {
    double combatdistance = 0.7 * getSensorRangeMinShip();
    Vector list = getShipsOfFactionNearXYZ(askingfaction, xyz, combatdistance);
    int siz = list.size();
    for (int i = 0; i < siz; i++) {
      Ship s = (Ship) list.elementAt(i);
      if (s != null) {
        if (s.isInCombat())
          return true;
      }
    }
    return false; //no ships of ours in that vicinity, or they haven't reported combat there,  seems safe.

    //xxx faction check its scoutreports too.!
  }
  public static void setCurrentExplosion(Vector xyz, double range_km) {
    current_explosion_location = xyz;
    current_explosion_range = range_km;
  }
  public static Vector getCurrentExplosionLocation() {
    return current_explosion_location;
  }
  public static double getCurrentExplosionRange() {
    return current_explosion_range;
  }
  public static void sendDebrisTowardsCamera(String cause, MobileThing created_by_MT/*gives start location*/) {
    if (cause.equals("BATTLE"))
      Missilemada2.createDebrisFlatSprite("hull_bits.png", 6.88*(0.10+Missilemada2.gimmeRandDouble()),350.0*(1.0+Missilemada2.gimmeRandDouble()),
        350.0*(1.0+Missilemada2.gimmeRandDouble()), created_by_MT, false, true/*to camera*/);
    if (cause.equals("MINING"))
      Missilemada2.createDebrisFlatSprite("mining_debris.png", 6.9*(0.10+Missilemada2.gimmeRandDouble()),350.0*(1.0+Missilemada2.gimmeRandDouble()),
        350.0*(1.0+Missilemada2.gimmeRandDouble()), created_by_MT, false, true/*to camera*/);
  }
  public static void createDebrisFlatSprite(String texturefilename, double spd/*ignored if copy bearing from creator*/,
                                            double sizex, double sizey, MobileThing created_by_MT, boolean bearing_from_MT, boolean towards_camera) {
    if (created_by_MT == null) {
      //err, don't know where to create.
      //throw new NullPointerException("");
      return;
    }
    //visual fun: create a flying piece of debris.
    double hostradius = created_by_MT.getPixelRadius();
    Vector randXYZ = getRandomLocationNear(created_by_MT.getXYZ(), 0.7*hostradius, 0.62);
      double x = (Double)randXYZ.get(0);
      double y = (Double)randXYZ.get(1);
      double z = (Double)randXYZ.get(2);
      FlatSprite debris = new FlatSprite(sizex, sizey, x, y, z, texturefilename, 1.0, 1.0f/*transp*/);
    if (bearing_from_MT) { //for missile-expired-debris.
      //shall float in direction the creator was XY-heading.
            //xx not obey spd
            //debris.setSpeed_mag_dirXY(0.9*created_by_MT.getSpeedCurrent(), created_by_MT.getBearingXYfromSpeed());
            //give it some z-speed
            //debris.setSpeed(debris.getXSpeed(), debris.getYSpeed(), spd * 0.05*(Missilemada2.gimmeRandDouble() - 0.5) );
      //exact bearing from creator:
      debris.setSpeed(created_by_MT.getXSpeed(), created_by_MT.getYSpeed(), created_by_MT.getZSpeed());
      debris.reduceSpeed(0.95);
    } else {
      if (towards_camera) { //fun debris
        //maybe debris.setPermRotation(0.5f);
        double rand = (0.25*camera_z) * (gimmeRandDouble()-0.5);
        MobileThing camera_tempMT = new MobileThing(); camera_tempMT.setX(camera_x+rand); camera_tempMT.setY(camera_y+rand); camera_tempMT.setZ(camera_z);
        //xyz sin cos math copied from missile turning.
        double desired_bearingXY = Missilemada2.calcBearingXY(debris, camera_tempMT);
        double desired_bearingXZ = Missilemada2.calcBearingXZ(debris, camera_tempMT);
        debris.setSpeed(Math.sqrt(spd) * Math.cos(desired_bearingXY),
                        Math.sqrt(spd) * Math.sin(desired_bearingXY),
                        spd * Math.sin(desired_bearingXZ)); //difficult to grasp and to get right.

        //DEBUG: from camera to MT
        //first make a temp thing at camer... wait.
//        FlatSprite bebugdebris = new FlatSprite(sizex, sizey, x, y, z, texturefilename, 1.0, 1.0f/*transp*/);
//        desired_bearingXY = Missilemada2.calcBearingXY(camera_tempMT, bebugdebris);
//        desired_bearingXZ = Missilemada2.calcBearingXZ(camera_tempMT, bebugdebris);
//        bebugdebris.setSpeed(spd * Math.cos(desired_bearingXY),
//              spd * Math.sin(desired_bearingXY),
//              spd * Math.sin(desired_bearingXZ));
//        addToFSTempList(bebugdebris);


      } else { //not towards camera, not towards originator's heading.
        //default: shall float in random direction.
        debris.setSpeed(spd * (gimmeRandDouble() - 0.5), spd * (gimmeRandDouble() - 0.5), spd * (gimmeRandDouble() - 0.5) );
      }
    }
    //no-one updates this flatsprite unless it's in The List.
    addToFSTempList(debris);
    //possibly expire the oldest (first in fslist) ones
    if (flatsprite_temporary_List.size() > max_flatsprites) {
      int randint = gimmeRandInt(17);
      try {
        flatsprite_temporary_List.remove(randint);
        flatsprite_temporary_List.remove(randint+4); //fukkin clever
        flatsprite_temporary_List.remove(randint+9);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  public static void setOpenGLTextureBeam() {
    texture_beams.bind();
  }
  public static void setOpenGLTextureGUILine() {
    texture_beams.bind();
  }
  public static void factionHasLost(Faction destroyedfaction) {
    //xxxxxxxxxxxxx;
    if (destroyedfaction == getPlayerFaction()) {
      gameScenarioLost(); //xxx duplicate?
    } else {
      if (factionList.size() == 2) { //if two fac, and player fac survives, win.
        gameScenarioWon();

      }
      //xxxxxx

    }

  }
  public static String strCurrDaysHours() {
    double hours = worldTimeElapsed / (3600.0);
    int days = (int)(hours/24.0);
    int hours_left_over = (int) Math.round(hours - (days * 24.0));
    return "" + days + " d " + hours_left_over + " h ";
  }
  public static boolean areCoordsWithinPlayarea(Vector xyz) {
    if (xyz == null)
      return false;
    Vector originvec = MobileThing.createXYZ(0.0, 0.0, 0.0);
    double dist_from_origin = MobileThing.calcDistanceVecVec(originvec, xyz);
    if (dist_from_origin > 1.49*world_x_max /*5.0 * BASEDIST*/) {
      System.out.println("someone asked areCoordsWithinPlayarea() and it was way far from origin.");
      return false;
    } else {
      return true;
    }
  }
  public static Vector getRandomLocation_using_asteroidlist() {
    Asteroid a = null;
    while (a == null) {
     a = getRandomAsteroid();
    }
    return a.getXYZ();
  }
  public static long getWorldTime() {
    return worldTimeElapsed;
  }
  private static int StrToInt(String a) {
    return Integer.parseInt(a);
  }
  protected void resize() {
    glViewport(0, 0, Display.getWidth(), Display.getHeight());
    // ??
  }
  public long getTimeMS() {
    return System.nanoTime() / 1000000;
  }
  public long getTime() { //in ms
    return (Sys.getTime() * 1000) / Sys.getTimerResolution();
  }
  protected void disposeResources() {
    try {
      allNotesOff();
      system_MIDISequencer.close();
      system_MIDIDevice.close(); //otherwise program stays sort-of running in background
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static boolean isAsteTooCloseToABase(Asteroid as) { //shouldn't be called very often. Called by aste adv_time.
    double tooclose = 2.05 * getAsteroidScanningRange();
    Faction fa;
    for (int a = 0; a < factionList.size(); a++) {
      fa = (Faction) factionList.get(a);
      StarBase ba = fa.getStarbase();
      if (ba != null) {
        if (MobileThing.calcDistanceMTMT(as, ba) < tooclose) {
          return true;
        }
      }
    }
    return false;
  }
  public static boolean doesShipVectorContainType(Vector shipList, String querytype) {
    int listsize = shipList.size();
    for (int j = 0; j < listsize; j++) {
      Ship p = (Ship) shipList.elementAt(j);
      if (p != null) {
        if (p.getType().equals(querytype))
          return true;
      }
    }
    return false;
  }


}