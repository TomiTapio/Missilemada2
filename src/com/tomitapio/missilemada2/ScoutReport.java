package com.tomitapio.missilemada2;

public class ScoutReport {
  public long timestamp;
  public MobileThing item;

  public ScoutReport(long worldtimestamp, MobileThing sighted_item) {
    timestamp = worldtimestamp;
    item = sighted_item;

  }
}
