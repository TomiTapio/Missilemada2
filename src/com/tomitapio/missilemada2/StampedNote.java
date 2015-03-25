package com.tomitapio.missilemada2;

/* Date: 4.9.2013  */
public class StampedNote {

  public long worldtimestamp;
  public int instrument;
  public int actualnote;
  public int vol;
  public double dur_ticks;
  public boolean isNoteOff;

  public StampedNote(long in_stamp /*what worldtime to play at*/, int in_instrument, int in_actualnote, int in_vol, double in_dur_ticks, boolean off) {
    worldtimestamp = in_stamp;
    instrument = in_instrument;
    actualnote = in_actualnote;
    vol = in_vol;
    dur_ticks = in_dur_ticks;
    isNoteOff = off;
  }
  public String toString() {
    if (isNoteOff) {
      return new String("Stamp "+ worldtimestamp + " ins "+instrument + " NOTEOFF key "+actualnote);
    } else {
      return new String("Stamp "+ worldtimestamp + " ins "+instrument + " note key "+actualnote);
    }
  }
}
